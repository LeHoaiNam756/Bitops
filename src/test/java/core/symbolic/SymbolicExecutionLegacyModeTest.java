package core.symbolic;

import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import core.cfg.CfgBuilder;
import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SymbolicExecutionLegacyModeTest {

    @Test
    public void executePath_legacyOriginalUsesIntRealArithmetic() {
        MethodDeclaration method = parseMethod("""
                class T {
                    int score(int x) {
                        if (x + 1 > 10) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        int branch = nodeIdContaining(cfg, "x + 1 > 10");
        ControlFlowGraph.Edge trueEdge = outgoingEdge(cfg, branch, CfgEdgeKind.TRUE);

        SolverResult result = new SymbolicExecution().executePath(
                cfg,
                List.of(trueEdge),
                new ArrayList<ASTNode>(method.parameters()),
                Map.<String, SymType>of("x", PrimitiveSymType.INT),
                Z3EncodingMode.LEGACY_INT_REAL);

        assertTrue(result.isSat());
        Object value = ((SolverResult.Sat) result).model().lookup("x").orElseThrow().value();
        assertTrue(value instanceof Integer);
        assertTrue((Integer) value > 9);
    }

    @Test
    public void executePath_legacyOriginalRejectsBitwiseInsteadOfUsingBitVector() {
        MethodDeclaration method = parseMethod("""
                class T {
                    int odd(int x) {
                        if ((x & 1) == 1) {
                            return 1;
                        }
                        return 0;
                    }
                }
                """);
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        int branch = nodeIdContaining(cfg, "(x & 1) == 1");
        ControlFlowGraph.Edge trueEdge = outgoingEdge(cfg, branch, CfgEdgeKind.TRUE);

        SolverResult result = new SymbolicExecution().executePath(
                cfg,
                List.of(trueEdge),
                new ArrayList<ASTNode>(method.parameters()),
                Map.<String, SymType>of("x", PrimitiveSymType.INT),
                Z3EncodingMode.LEGACY_INT_REAL);

        assertTrue(result.isUnknown());
        assertTrue(((SolverResult.Unknown) result).reason().contains("bitwise or shift"));
    }

    private static MethodDeclaration parseMethod(String source) {
        return (MethodDeclaration) Parser.parseSourceToAstFuncList(source).get(0);
    }

    private static int nodeIdContaining(ControlFlowGraph cfg, String content) {
        for (int nodeId : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(nodeId);
            if (node.getKind() == CfgNodeKind.BRANCH
                    && node.getContent() != null
                    && node.getContent().contains(content)) {
                return nodeId;
            }
        }
        throw new AssertionError("Missing branch CFG node containing: " + content);
    }

    private static ControlFlowGraph.Edge outgoingEdge(ControlFlowGraph cfg, int from, CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(from)) {
            if (edge.getKind() == kind) return edge;
        }
        throw new AssertionError("Missing outgoing edge from " + from + " [" + kind + "]");
    }
}
