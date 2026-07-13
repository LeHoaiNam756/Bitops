package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
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
        int branch = cfg.addNode(CfgNodeKind.BRANCH, condition(), "x > 0");
        int loop = cfg.addNode(CfgNodeKind.LOOP, condition(), "i < 10");

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
        int branch = cfg.addNode(CfgNodeKind.BRANCH, condition(), "x > 0");
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
        int branch = cfg.addNode(CfgNodeKind.BRANCH, condition(), "x > 0");

        new BranchCoverageTracker(cfg).getUncovered().remove(branch * 2);
    }

    @Test
    public void distinguishesInfeasibleFromUnknownAndComputesFeasibleCoverage() {
        CoverageTracker tracker = new CoverageTracker(Set.of(1, 2, 3, 4));

        tracker.markCovered(1);
        tracker.markCovered(2);
        tracker.markInfeasible(3, "unsat");
        tracker.markUnknown(4, "timeout");

        assertEquals(Set.of(3), tracker.getInfeasible());
        assertEquals(Set.of(4), tracker.getUnknown());
        assertEquals(Set.of(3, 4), tracker.getSkipped());
        assertEquals("unsat", tracker.getInfeasibleReasons().get(3));
        assertEquals("timeout", tracker.getUnknownReasons().get(4));
        assertEquals(50.0, tracker.rawCoveragePercent(), 0.0001);
        assertEquals(200.0 / 3.0, tracker.feasibleCoveragePercent(), 0.0001);
        assertTrue(tracker.isComplete());
    }

    @Test
    public void mapsBranchOutcomeIdsBackToCfgNodeTargetsForPathFinding() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int branch = cfg.addNode(CfgNodeKind.BRANCH, condition(), "x > 0");

        BranchCoverageTracker tracker = new BranchCoverageTracker(cfg);

        assertEquals(branch, tracker.pathTargetFor(branch * 2));
        assertEquals(branch, tracker.pathTargetFor(branch * 2 + 1));
    }

    @Test
    public void findsPathsThroughTheRequestedBranchOutcome() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = cfg.addNode(CfgNodeKind.ENTRY, null, "entry");
        int branch = cfg.addNode(CfgNodeKind.BRANCH, condition(), "x > 0");
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

    @Test
    public void ignoresSyntheticBranchNodesWithoutSourceAst() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        cfg.addNode(CfgNodeKind.LOOP, null, "true");

        assertTrue(new BranchCoverageTracker(cfg).getUncovered().isEmpty());
    }

    @Test
    public void ignoresBooleanLiteralBranchNodes() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        cfg.addNode(CfgNodeKind.LOOP, booleanLiteral(), "true");

        assertTrue(new BranchCoverageTracker(cfg).getUncovered().isEmpty());
    }

    @SuppressWarnings("deprecation")
    private static Expression condition() {
        AST ast = AST.newAST(AST.JLS8);
        InfixExpression condition = ast.newInfixExpression();
        condition.setLeftOperand(ast.newSimpleName("x"));
        condition.setOperator(InfixExpression.Operator.GREATER);
        condition.setRightOperand(ast.newNumberLiteral("0"));
        return condition;
    }

    @SuppressWarnings("deprecation")
    private static Expression booleanLiteral() {
        return AST.newAST(AST.JLS8).newBooleanLiteral(true);
    }
}
