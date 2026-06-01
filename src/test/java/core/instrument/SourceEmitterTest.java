package core.instrument;

import core.cfg.CfgBuilder;
import core.cfg.ControlFlowGraph;
import core.cfg.Coverage;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SourceEmitterTest {

    @Test
    public void emitsMarkerForCfgAstContainedByStatement() {
        CompilationUnit cu = parse("""
                package sample;

                class Example {
                    int value(int x) {
                        return x + 1;
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.STATEMENT);

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.STATEMENT, "cloned");

        int marker = emitted.indexOf("TraceRecorder.mark(3, TraceKind.NODE);");
        int statement = emitted.indexOf("return x + 1");
        assertTrue("mark call for return expression should be emitted", marker >= 0);
        assertTrue("mark call should appear before containing statement", marker < statement);
        assertFalse("emitter should not emit annotation sentinels", emitted.contains("@Ct4jTrace"));
    }

    @Test
    public void emitsBothBranchProbeOutcomesForSameConditionPosition() {
        CompilationUnit cu = parse("""
                package sample;

                class Example {
                    int value(int x) {
                        if (x > 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.BRANCH);

        TracePoint trueProbe = plan.points().stream()
                .filter(tp -> tp.kind() == TraceKind.COND_T)
                .findFirst()
                .orElseThrow();
        TracePoint falseProbe = plan.points().stream()
                .filter(tp -> tp.kind() == TraceKind.COND_F)
                .findFirst()
                .orElseThrow();

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.BRANCH, "cloned");

        assertTrue("true branch probe should be emitted",
                emitted.contains("TraceRecorder.mark(" + trueProbe.cfgNodeId() + ", TraceKind.COND_T)"));
        assertTrue("false branch probe should be emitted",
                emitted.contains("TraceRecorder.mark(" + falseProbe.cfgNodeId() + ", TraceKind.COND_F)"));
    }

    @Test
    public void branchCoverageEmitsConditionProbesWithoutStatementMarkers() {
        CompilationUnit cu = parse("""
                package sample;

                class Example {
                    int value(int x) {
                        if (x > 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.BRANCH);

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.BRANCH, "cloned");

        assertTrue("branch coverage should mark the if condition true path",
                emitted.contains("TraceKind.COND_T"));
        assertTrue("branch coverage should mark the if condition false path",
                emitted.contains("TraceKind.COND_F"));
        assertFalse("branch coverage should not emit statement node markers",
                emitted.contains("TraceKind.NODE"));
    }

    @Test
    public void mcdcCoverageEmitsConditionProbesWithoutStatementMarkers() {
        CompilationUnit cu = parse("""
                package sample;

                class Example {
                    int value(int x) {
                        if (x > 0) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.MCDC);

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.MCDC, "cloned");

        assertTrue("MCDC coverage should mark condition true paths",
                emitted.contains("TraceKind.COND_T"));
        assertTrue("MCDC coverage should mark condition false paths",
                emitted.contains("TraceKind.COND_F"));
        assertFalse("MCDC coverage should not emit statement node markers",
                emitted.contains("TraceKind.NODE"));
    }

    private static CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }
}
