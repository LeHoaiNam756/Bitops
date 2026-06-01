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

import static org.junit.Assert.*;

public class SymbolicExecutionLoopSsaTest {

    @Test
    public void loopBranchAfterFourIncrementsIsSat() throws Exception {
        String src = """
                class T {
                    void foo(int n) {
                        int i = 0;
                        int cnt = 0;
                        while (i < n) {
                            if (i % 2 == 0) { cnt++; }
                            if (i == 4) { cnt += 4; }
                            i++;
                        }
                    }
                }
                """;
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(src).get(0);
        ControlFlowGraph cfg = new CfgBuilder().build(method);
        int entry = nodeIdWithKind(cfg, CfgNodeKind.ENTRY);
        int exit = nodeIdWithKind(cfg, CfgNodeKind.EXIT);
        int initI = nodeIdContaining(cfg, "i=0");
        int initCnt = nodeIdContaining(cfg, "cnt=0");
        int loop = nodeIdContaining(cfg, "i < n");
        int evenBranch = nodeIdContaining(cfg, "i % 2 == 0");
        int cntIncrement = nodeIdContaining(cfg, "cnt++");
        int target = nodeIdContaining(cfg, "i == 4");
        int cntAdd = nodeIdContaining(cfg, "cnt+=4");
        int increment = nodeIdContaining(cfg, "i++");

        List<ControlFlowGraph.Edge> fifthIterationPath = new ArrayList<>();
        fifthIterationPath.add(edge(cfg, entry, initI, CfgEdgeKind.NORMAL));
        fifthIterationPath.add(edge(cfg, initI, initCnt, CfgEdgeKind.NORMAL));
        fifthIterationPath.add(edge(cfg, initCnt, loop, CfgEdgeKind.NORMAL));
        appendIteration(cfg, fifthIterationPath, loop, evenBranch, cntIncrement, target, increment, true, false);
        appendIteration(cfg, fifthIterationPath, loop, evenBranch, cntIncrement, target, increment, false, false);
        appendIteration(cfg, fifthIterationPath, loop, evenBranch, cntIncrement, target, increment, true, false);
        appendIteration(cfg, fifthIterationPath, loop, evenBranch, cntIncrement, target, increment, false, false);
        appendIteration(cfg, fifthIterationPath, loop, evenBranch, cntIncrement, target, increment, true, true);
        fifthIterationPath.add(edge(cfg, target, cntAdd, CfgEdgeKind.TRUE));
        fifthIterationPath.add(edge(cfg, cntAdd, increment, CfgEdgeKind.NORMAL));
        fifthIterationPath.add(edge(cfg, increment, loop, CfgEdgeKind.NORMAL));
        fifthIterationPath.add(edge(cfg, loop, exit, CfgEdgeKind.FALSE));

        SolverResult result = new SymbolicExecution().executePath(
                cfg,
                fifthIterationPath,
                new ArrayList<ASTNode>(method.parameters()),
                Map.<String, SymType>of("n", PrimitiveSymType.INT)
        );

        assertTrue("i == 4 should be reachable after four increments", result.isSat());
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

    private static int nodeIdWithKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int nodeId : cfg.getNodes()) {
            if (cfg.getNode(nodeId).getKind() == kind) return nodeId;
        }
        throw new AssertionError("Missing CFG node with kind: " + kind);
    }

    private static void appendIteration(
            ControlFlowGraph cfg,
            List<ControlFlowGraph.Edge> path,
            int loop,
            int evenBranch,
            int cntIncrement,
            int target,
            int increment,
            boolean even,
            boolean targetTrue) {
        path.add(edge(cfg, loop, evenBranch, CfgEdgeKind.TRUE));
        if (even) {
            path.add(edge(cfg, evenBranch, cntIncrement, CfgEdgeKind.TRUE));
            path.add(edge(cfg, cntIncrement, target, CfgEdgeKind.NORMAL));
        } else {
            path.add(edge(cfg, evenBranch, target, CfgEdgeKind.FALSE));
        }
        if (!targetTrue) {
            path.add(edge(cfg, target, increment, CfgEdgeKind.FALSE));
            path.add(edge(cfg, increment, loop, CfgEdgeKind.NORMAL));
        }
    }

    private static ControlFlowGraph.Edge edge(ControlFlowGraph cfg, int from, int to, CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(from)) {
            if (edge.getTo() == to && edge.getKind() == kind) return edge;
        }
        throw new AssertionError("Missing edge " + from + " -> " + to + " [" + kind + "]");
    }
}
