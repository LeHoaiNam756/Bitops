package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class BranchCoverageTrackerTest {

    @Test
    public void tracksTrueAndFalseOutcomesForBranchAndLoopNodesOnly() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int statement = cfg.addNode(CfgNodeKind.STMT, null, "x = 1");
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "x > 0");
        int loop = cfg.addNode(CfgNodeKind.LOOP, null, "i < 10");

        BranchCoverageTracker tracker = new BranchCoverageTracker(cfg);

        assertEquals(Set.of(branch * 2, branch * 2 + 1, loop * 2, loop * 2 + 1), tracker.getUncovered());
        assertFalse(tracker.isUncovered(entry));
        assertFalse(tracker.isUncovered(statement));
        assertTrue(tracker.isUncovered(branch * 2));
        assertTrue(tracker.isUncovered(branch * 2 + 1));
        assertTrue(tracker.isUncovered(loop * 2));
        assertTrue(tracker.isUncovered(loop * 2 + 1));
    }

    @Test
    public void movesOutcomesBetweenUncoveredCoveredAndSkipped() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "x > 0");
        int trueOutcome = branch * 2;
        int falseOutcome = branch * 2 + 1;

        BranchCoverageTracker tracker = new BranchCoverageTracker(cfg);

        tracker.markCovered(trueOutcome);
        tracker.markSkipped(falseOutcome);

        assertEquals(Set.of(), tracker.getUncovered());
        assertEquals(Set.of(trueOutcome), tracker.getCovered());
        assertEquals(Set.of(falseOutcome), tracker.getSkipped());
        assertTrue(tracker.isCovered(trueOutcome));
        assertTrue(tracker.isSkipped(falseOutcome));
        assertFalse(tracker.isUncovered(trueOutcome));
        assertTrue(tracker.isComplete());

        tracker.markCovered(falseOutcome);

        assertEquals(Set.of(trueOutcome, falseOutcome), tracker.getCovered());
        assertEquals(Set.of(), tracker.getSkipped());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void coverageSetsAreReadOnly() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "x > 0");

        new BranchCoverageTracker(cfg).getUncovered().remove(branch * 2);
    }

    @Test
    public void mapsBranchOutcomeIdsBackToCfgNodeTargetsForPathFinding() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "x > 0");

        BranchCoverageTracker tracker = new BranchCoverageTracker(cfg);

        assertEquals(branch, tracker.pathTargetFor(branch * 2));
        assertEquals(branch, tracker.pathTargetFor(branch * 2 + 1));
    }

    @Test
    public void findsPathsThroughTheRequestedBranchOutcome() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int branch = cfg.addNode(CfgNodeKind.BRANCH, null, "x > 0");
        int trueNode = cfg.addNode(CfgNodeKind.STMT, null, "true");
        int falseNode = cfg.addNode(CfgNodeKind.STMT, null, "false");
        int exit = cfg.addNode(CfgNodeKind.EXIT, null, "exit");
        cfg.addEdge(entry, branch, CfgEdgeKind.NORMAL);
        cfg.addEdge(branch, trueNode, CfgEdgeKind.TRUE);
        cfg.addEdge(branch, falseNode, CfgEdgeKind.FALSE);
        cfg.addEdge(trueNode, exit, CfgEdgeKind.NORMAL);
        cfg.addEdge(falseNode, exit, CfgEdgeKind.NORMAL);

        BranchCoverageTracker tracker = new BranchCoverageTracker(cfg);
        int falseOutcome = branch * 2 + 1;

        List<List<ControlFlowGraph.Edge>> paths = new AllPathsFinder()
                .findPath(cfg, tracker.pathTargetFor(falseOutcome), tracker.requiredExitFor(falseOutcome));

        assertFalse(paths.isEmpty());
        for (List<ControlFlowGraph.Edge> path : paths) {
            assertTrue(path.stream().anyMatch(edge -> edge.getFrom() == branch && edge.getKind() == CfgEdgeKind.FALSE));
            assertFalse(path.stream().anyMatch(edge -> edge.getFrom() == branch && edge.getKind() == CfgEdgeKind.TRUE));
        }
    }
}
