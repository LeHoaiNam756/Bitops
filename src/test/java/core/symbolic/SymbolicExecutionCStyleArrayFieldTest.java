package core.symbolic;

import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.z3encoder.SolverResult;
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

public class SymbolicExecutionCStyleArrayFieldTest {

    @Test
    public void executePath_arrayBracketsAfterFieldName_encodesArraySelect() {
        MethodDeclaration method = parseMethod("""
                class T {
                    private static int values[] = { 40, 41 };

                    int find(int ch) {
                        if (ch >= values[0]) {
                            return 1;
                        }
                        return -1;
                    }
                }
                """);
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        int branch = branchContaining(cfg, "ch >= values[0]");

        SolverResult result = new SymbolicExecution().executePath(
                cfg,
                List.of(outgoingEdge(cfg, branch, CfgEdgeKind.TRUE)),
                new ArrayList<ASTNode>(method.parameters()),
                Map.<String, SymType>of("ch", PrimitiveSymType.INT));

        assertTrue(result.isSat());
    }

    private static MethodDeclaration parseMethod(String source) {
        return (MethodDeclaration) Parser.parseSourceToAstFuncList(source).get(0);
    }

    private static int branchContaining(ControlFlowGraph cfg, String content) {
        for (int nodeId : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(nodeId);
            if (node.getKind() == CfgNodeKind.BRANCH
                    && node.getContent() != null
                    && node.getContent().contains(content)) {
                return nodeId;
            }
        }
        throw new AssertionError("Missing branch containing: " + content);
    }

    private static ControlFlowGraph.Edge outgoingEdge(
            ControlFlowGraph cfg, int from, CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(from)) {
            if (edge.getKind() == kind) return edge;
        }
        throw new AssertionError("Missing outgoing edge from " + from + " [" + kind + "]");
    }
}
