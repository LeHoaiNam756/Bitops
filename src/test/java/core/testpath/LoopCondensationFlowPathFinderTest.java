package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class LoopCondensationFlowPathFinderTest {
    private LoopCondensationFlowPathFinder finder;

    @Before
    public void setUp() {
        finder = new LoopCondensationFlowPathFinder();
    }

    @Test
    public void diamondGraph_pathCoverUsesMinimumTwoCompletePaths() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int branch = addBranch(cfg, "x > 0");
        int trueNode = addStmt(cfg, "true");
        int falseNode = addStmt(cfg, "false");
        int merge = addStmt(cfg, "merge");
        int exit = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, trueNode);
        falseEdge(cfg, branch, falseNode);
        normal(cfg, trueNode, merge);
        normal(cfg, falseNode, merge);
        normal(cfg, merge, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathCover(cfg);

        assertEquals(2, paths.size());
        assertAllPathsBounded(paths, entry, exit);
        Set<Integer> covered = coveredNodes(paths);
        assertTrue(covered.containsAll(Set.of(entry, branch, trueNode, falseNode, merge, exit)));
    }

    @Test
    public void whileLoop_pathCoverRemovesBackEdgeForMatchingButCompletesPathThroughLoopExit() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int loop = addLoop(cfg, "i < n");
        int body = addStmt(cfg, "body");
        int exit = addExit(cfg);
        normal(cfg, entry, loop);
        trueEdge(cfg, loop, body);
        normal(cfg, body, loop);
        falseEdge(cfg, loop, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathCover(cfg);

        assertEquals(1, paths.size());
        assertAllPathsBounded(paths, entry, exit);
        assertTrue(coveredNodes(paths).containsAll(Set.of(entry, loop, body, exit)));
        assertTrue(paths.get(0).stream().anyMatch(edge -> edge.getFrom() == body && edge.getTo() == loop));
    }

    @Test
    public void loopWithAfterNode_pathCoverUsesOneIterationToCoverLoopAndAfterNode() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int loop = addLoop(cfg, "i < n");
        int body = addStmt(cfg, "body");
        int after = addStmt(cfg, "after");
        int exit = addExit(cfg);
        normal(cfg, entry, loop);
        trueEdge(cfg, loop, body);
        normal(cfg, body, loop);
        falseEdge(cfg, loop, after);
        normal(cfg, after, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathCover(cfg);

        assertEquals(1, paths.size());
        assertAllPathsBounded(paths, entry, exit);
        assertTrue(coveredNodes(paths).containsAll(Set.of(entry, loop, body, after, exit)));
    }

    @Test
    public void requiredBranchExit_returnsPathUsingRequestedOutcome() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int branch = addBranch(cfg, "x > 0");
        int merge = addStmt(cfg, "merge");
        int exit = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, merge);
        falseEdge(cfg, branch, merge);
        normal(cfg, merge, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPath(cfg, branch, CfgEdgeKind.FALSE);

        assertFalse(paths.isEmpty());
        for (List<ControlFlowGraph.Edge> path : paths) {
            assertTrue(path.stream()
                    .anyMatch(edge -> edge.getFrom() == branch && edge.getKind() == CfgEdgeKind.FALSE));
        }
    }

    @Test
    public void unreachableNodes_areIgnoredByPathCover() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int reachable = addStmt(cfg, "reachable");
        int exit = addExit(cfg);
        int orphan = addStmt(cfg, "orphan");
        normal(cfg, entry, reachable);
        normal(cfg, reachable, exit);
        normal(cfg, orphan, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathCover(cfg);

        assertEquals(1, paths.size());
        assertFalse(coveredNodes(paths).contains(orphan));
    }

    @Test
    public void targetFilter_returnsOnlyCoverPathsContainingTarget() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int branch = addBranch(cfg, "x > 0");
        int trueNode = addStmt(cfg, "true");
        int falseNode = addStmt(cfg, "false");
        int exit = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, trueNode);
        falseEdge(cfg, branch, falseNode);
        normal(cfg, trueNode, exit);
        normal(cfg, falseNode, exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPath(cfg, falseNode);

        assertEquals(1, paths.size());
        assertTrue(nodeIds(paths.get(0)).contains(falseNode));
        assertFalse(nodeIds(paths.get(0)).contains(trueNode));
    }

    private int addEntry(ControlFlowGraph cfg) {
        return cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
    }

    private int addExit(ControlFlowGraph cfg) {
        return cfg.addNode(CfgNodeKind.EXIT, null, "exit");
    }

    private int addStmt(ControlFlowGraph cfg, String label) {
        return cfg.addNode(CfgNodeKind.STMT, null, label);
    }

    private int addBranch(ControlFlowGraph cfg, String label) {
        return cfg.addNode(CfgNodeKind.BRANCH, null, label);
    }

    private int addLoop(ControlFlowGraph cfg, String label) {
        return cfg.addNode(CfgNodeKind.LOOP, null, label);
    }

    private void normal(ControlFlowGraph cfg, int from, int to) {
        cfg.addEdge(from, to, CfgEdgeKind.NORMAL);
    }

    private void trueEdge(ControlFlowGraph cfg, int from, int to) {
        cfg.addEdge(from, to, CfgEdgeKind.TRUE);
    }

    private void falseEdge(ControlFlowGraph cfg, int from, int to) {
        cfg.addEdge(from, to, CfgEdgeKind.FALSE);
    }

    private Set<Integer> coveredNodes(List<List<ControlFlowGraph.Edge>> paths) {
        return paths.stream()
                .flatMap(path -> nodeIds(path).stream())
                .collect(Collectors.toSet());
    }

    private List<Integer> nodeIds(List<ControlFlowGraph.Edge> path) {
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(path.get(0).getFrom());
        for (ControlFlowGraph.Edge edge : path) {
            ids.add(edge.getTo());
        }
        return ids;
    }

    private void assertAllPathsBounded(
            List<List<ControlFlowGraph.Edge>> paths,
            int entry,
            int exit) {

        for (List<ControlFlowGraph.Edge> path : paths) {
            assertFalse(path.isEmpty());
            assertEquals(entry, path.get(0).getFrom());
            assertEquals(exit, path.get(path.size() - 1).getTo());
        }
    }
}
