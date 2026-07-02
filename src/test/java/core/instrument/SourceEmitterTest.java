package core.instrument;

import core.cfg.CfgBuilder;
import core.cfg.ControlFlowGraph;
import core.cfg.Coverage;
import core.utils.Compiler;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    public void branchCoveragePreservesForLoopWithOmittedCondition() {
        assertForLoopWithOmittedConditionIsPreserved(Coverage.BRANCH);
    }

    @Test
    public void mcdcCoveragePreservesForLoopWithOmittedCondition() {
        assertForLoopWithOmittedConditionIsPreserved(Coverage.MCDC);
    }

    @Test
    public void preservesStaticInitializersThatAssignBlankFinalFields() throws Exception {
        CompilationUnit cu = parse("""
                package com.xk72.charles.gui;

                public final class P {
                    private static final String[] n;

                    static {
                        n = new String[] {"x"};
                    }

                    private String lookup(int i) {
                        return n[i];
                    }
                }

                class LicenceException extends Exception {
                    LicenceException(String message) {
                        super(message);
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.STATEMENT);

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.STATEMENT, "core.output.clone");

        assertTrue("static initializer should be preserved", emitted.contains("static"));
        assertTrue("blank final field assignment should be preserved", emitted.contains("new String[]"));

        Path tempDir = Files.createTempDirectory("ct4j-p-like-clone-");
        Path emittedFile = tempDir.resolve("P.java");
        Files.writeString(emittedFile, emitted, StandardCharsets.UTF_8);
        Compiler.getInstance().compileJavaFile(
                emittedFile.toString(),
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
    }

    @Test
    public void statementCoverageEmitsMarkersInsideTryCatchBodies() {
        CompilationUnit cu = parse("""
                package sample;

                class Example {
                    int value(int x) {
                        try {
                            if (x < 0) {
                                throw new IllegalArgumentException();
                            }
                            return 1;
                        } catch (IllegalArgumentException e) {
                            return -1;
                        }
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, Coverage.STATEMENT);

        String emitted = new SourceEmitter().emit(cu, plan, Coverage.STATEMENT, "cloned");

        int catchStart = emitted.indexOf("catch (IllegalArgumentException e)");
        int marker = emitted.indexOf("TraceRecorder.mark(", catchStart);
        int catchReturn = emitted.indexOf("return -1", catchStart);

        assertTrue("catch clause should be preserved", catchStart >= 0);
        assertTrue("catch body should receive a statement marker", marker >= 0);
        assertTrue("marker should appear before catch return", marker < catchReturn);
    }

    private static CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    private static void assertForLoopWithOmittedConditionIsPreserved(Coverage coverage) {
        CompilationUnit cu = parse("""
                class Example {
                    int value(int x) {
                        for (;;) {
                            if (x > 0) {
                                break;
                            }
                            x++;
                        }
                        return x;
                    }
                }
                """);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        InstrumentationPlan plan = new InstrumentationPlanner().plan(cu, cfg, coverage);

        String emitted = new SourceEmitter().emit(cu, plan, coverage, "cloned");

        assertTrue("for-loop should retain an empty condition", emitted.contains("for (; ; )"));
        assertTrue("nested condition should still be instrumented", emitted.contains("TraceKind.COND_T"));
        assertTrue("nested condition should still be instrumented", emitted.contains("TraceKind.COND_F"));
    }
}
