package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BranchFlippingPathExplorerTest {

    @Test
    public void flippedPathsNegateOneObservedBranchAtATime() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int firstBranch = cfg.addNode(CfgNodeKind.BRANCH, null, "a > 0");
        int secondBranch = cfg.addNode(CfgNodeKind.BRANCH, null, "b > 0");
        int firstFalse = cfg.addNode(CfgNodeKind.STMT, null, "first false");
        int secondTrue = cfg.addNode(CfgNodeKind.STMT, null, "second true");
        int secondFalse = cfg.addNode(CfgNodeKind.STMT, null, "second false");
        int exit = cfg.addNode(CfgNodeKind.EXIT, null, "exit");

        cfg.addEdge(entry, firstBranch, CfgEdgeKind.NORMAL);
        cfg.addEdge(firstBranch, secondBranch, CfgEdgeKind.TRUE);
        cfg.addEdge(firstBranch, firstFalse, CfgEdgeKind.FALSE);
        cfg.addEdge(secondBranch, secondTrue, CfgEdgeKind.TRUE);
        cfg.addEdge(secondBranch, secondFalse, CfgEdgeKind.FALSE);
        cfg.addEdge(firstFalse, exit, CfgEdgeKind.NORMAL);
        cfg.addEdge(secondTrue, exit, CfgEdgeKind.NORMAL);
        cfg.addEdge(secondFalse, exit, CfgEdgeKind.NORMAL);

        BranchFlippingPathExplorer explorer = new BranchFlippingPathExplorer();
        List<List<ControlFlowGraph.Edge>> paths = explorer.flippedPaths(cfg, List.of(
                new BranchFlippingPathExplorer.BranchDecision(firstBranch, CfgEdgeKind.TRUE),
                new BranchFlippingPathExplorer.BranchDecision(secondBranch, CfgEdgeKind.TRUE)
        ));

        assertEquals(2, paths.size());

        List<ControlFlowGraph.Edge> flipFirst = paths.get(0);
        assertTrue(containsEdge(flipFirst, firstBranch, CfgEdgeKind.FALSE));
        assertFalse(containsEdge(flipFirst, secondBranch, CfgEdgeKind.TRUE));
        assertFalse(containsEdge(flipFirst, secondBranch, CfgEdgeKind.FALSE));

        List<ControlFlowGraph.Edge> flipSecond = paths.get(1);
        assertTrue(containsEdge(flipSecond, firstBranch, CfgEdgeKind.TRUE));
        assertTrue(containsEdge(flipSecond, secondBranch, CfgEdgeKind.FALSE));
        assertFalse(containsEdge(flipSecond, secondBranch, CfgEdgeKind.TRUE));
    }

    @Test
    public void shortestEntryToExitReturnsBootstrapPath() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int stmt = cfg.addNode(CfgNodeKind.STMT, null, "stmt");
        int exit = cfg.addNode(CfgNodeKind.EXIT, null, "exit");
        cfg.addEdge(entry, stmt, CfgEdgeKind.NORMAL);
        cfg.addEdge(stmt, exit, CfgEdgeKind.NORMAL);

        List<ControlFlowGraph.Edge> path =
                new BranchFlippingPathExplorer().shortestEntryToExit(cfg);

        assertEquals(2, path.size());
        assertEquals(entry, path.get(0).getFrom());
        assertEquals(exit, path.get(1).getTo());
    }

    @Test
    public void shortestPathThroughUsesRequestedBranchOutcome() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "a > 0");
        int trueNode = cfg.addNode(CfgNodeKind.STMT, null, "true");
        int falseNode = cfg.addNode(CfgNodeKind.STMT, null, "false");
        int exit = cfg.addNode(CfgNodeKind.EXIT, null, "exit");
        cfg.addEdge(entry, branch, CfgEdgeKind.NORMAL);
        cfg.addEdge(branch, trueNode, CfgEdgeKind.TRUE);
        cfg.addEdge(branch, falseNode, CfgEdgeKind.FALSE);
        cfg.addEdge(trueNode, exit, CfgEdgeKind.NORMAL);
        cfg.addEdge(falseNode, exit, CfgEdgeKind.NORMAL);

        List<ControlFlowGraph.Edge> path =
                new BranchFlippingPathExplorer().shortestPathThrough(
                        cfg, branch, CfgEdgeKind.FALSE);

        assertTrue(containsEdge(path, branch, CfgEdgeKind.FALSE));
        assertFalse(containsEdge(path, branch, CfgEdgeKind.TRUE));
        assertEquals(exit, path.get(path.size() - 1).getTo());
    }

    private static boolean containsEdge(
            List<ControlFlowGraph.Edge> path,
            int from,
            CfgEdgeKind kind) {
        return path.stream().anyMatch(edge ->
                edge.getFrom() == from && edge.getKind() == kind);
    }
}
