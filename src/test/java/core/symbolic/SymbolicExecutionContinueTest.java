package core.symbolic;

import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.cfg.CfgBuilder;
import core.cfg.CfgEdgeKind;
import core.cfg.ControlFlowGraph;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class SymbolicExecutionContinueTest {

    @Test
    public void executePath_ignoresContinueStatement() {
        String src = """
                class T {
                    void foo() {
                        while (true) {
                            continue;
                        }
                    }
                }
                """;
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(src).get(0);
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        int loop = nodeIdContaining(cfg, "true");
        int continueNode = nodeIdContaining(cfg, "continue");

        List<ControlFlowGraph.Edge> path = new ArrayList<>();
        path.add(edge(cfg, loop, continueNode, CfgEdgeKind.TRUE));
        path.add(edge(cfg, continueNode, loop, CfgEdgeKind.NORMAL));

        SolverResult result = new SymbolicExecution().executePath(
                cfg,
                path,
                new ArrayList<ASTNode>(method.parameters()),
                Map.<String, SymType>of()
        );

        assertNotNull(result);
    }

    private static int nodeIdContaining(ControlFlowGraph cfg, String content) {
        for (int nodeId : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(nodeId);
            if (node.getContent() != null && node.getContent().contains(content)) {
                return nodeId;
            }
        }
        throw new AssertionError("Missing CFG node containing: " + content);
    }

    private static ControlFlowGraph.Edge edge(ControlFlowGraph cfg, int from, int to, CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(from)) {
            if (edge.getTo() == to && edge.getKind() == kind) return edge;
        }
        throw new AssertionError("Missing edge " + from + " -> " + to + " [" + kind + "]");
    }
}
