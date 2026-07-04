package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AllPathsFinder}.
 *
 * <p>Every test builds a {@link ControlFlowGraph} directly (no mocks required –
 * {@code ASTNode} fields are passed as {@code null}, which the graph accepts).
 * Mockito is therefore not a dependency.
 *
 * <p>Test-naming convention: {@code <scenario>_<condition>_<expected>}
 *
 * <h3>Groups</h3>
 * <ol>
 *   <li>Degenerate / error cases</li>
 *   <li>Linear graph (no branches)</li>
 *   <li>Branching (if / else)</li>
 *   <li>Loops</li>
 *   <li>Target = ENTRY or EXIT</li>
 *   <li>Deduplication</li>
 *   <li>Explosion guards (MAX_PATHS, MAX_PATH_EDGES, MAX_NODE_VISITS)</li>
 *   <li>Edge-kind preservation</li>
 *   <li>do-while style back-edge</li>
 * </ol>
 */
public class AllPathsFinderTest {

    // -----------------------------------------------------------------------
    // Fixture
    // -----------------------------------------------------------------------

    private AllPathsFinder finder;

    @Before
    public void setUp() {
        finder = new AllPathsFinder();
        finder.setMAX_LOOP_ITERATIONS(1);
    }

    // -----------------------------------------------------------------------
    // CFG builder helpers
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Path inspection helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the ordered list of node IDs visited by a path.
     * Element 0 is the FROM of the first edge; every subsequent element is the TO.
     */
    private List<Integer> nodeIds(List<ControlFlowGraph.Edge> path) {
        if (path.isEmpty()) return Collections.emptyList();
        List<Integer> ids = new ArrayList<>();
        ids.add(path.get(0).getFrom());
        for (ControlFlowGraph.Edge e : path) ids.add(e.getTo());
        return ids;
    }

    private Set<List<Integer>> nodeIdSet(List<List<ControlFlowGraph.Edge>> paths) {
        return paths.stream()
                .map(this::nodeIds)
                .collect(Collectors.toSet());
    }

    private boolean pathContainsNode(List<ControlFlowGraph.Edge> path, int nodeId) {
        return nodeIds(path).contains(nodeId);
    }

    private long nodeVisitCount(List<ControlFlowGraph.Edge> path, int nodeId) {
        return nodeIds(path).stream().filter(id -> id == nodeId).count();
    }

    /** Asserts every path starts at {@code entryId} and ends at {@code exitId}. */
    private void assertAllPathsBounded(
            List<List<ControlFlowGraph.Edge>> results, int entryId, int exitId) {
        for (List<ControlFlowGraph.Edge> path : results) {
            assertFalse("Path must not be empty", path.isEmpty());
            assertEquals("Path must start at ENTRY",
                    entryId, path.get(0).getFrom());
            assertEquals("Path must end at EXIT",
                    exitId, path.get(path.size() - 1).getTo());
        }
    }

    /** Asserts every path visits the target node at least once. */
    private void assertAllPathsContainTarget(
            List<List<ControlFlowGraph.Edge>> results, int targetId) {
        for (List<ControlFlowGraph.Edge> path : results) {
            assertTrue("Each path must visit target node " + targetId,
                    pathContainsNode(path, targetId));
        }
    }

    private void assertResultIsUnmodifiable(List<List<ControlFlowGraph.Edge>> results) {
        try {
            results.add(Collections.emptyList());
            fail("Result list must be unmodifiable");
        } catch (UnsupportedOperationException ignored) { /* expected */ }
    }

    // -----------------------------------------------------------------------
    // Reusable CFG factories
    // -----------------------------------------------------------------------

    /**
     * Linear: ENTRY → A → B(target) → C → EXIT
     * Returns [entryId, aId, bId, cId, exitId]
     */
    private int[] buildLinearGraph(ControlFlowGraph cfg) {
        int entry  = addEntry(cfg);
        int a      = addStmt(cfg, "A");
        int b      = addStmt(cfg, "B");   // typical target
        int c      = addStmt(cfg, "C");
        int exit   = addExit(cfg);
        normal(cfg, entry, a);
        normal(cfg, a, b);
        normal(cfg, b, c);
        normal(cfg, c, exit);
        return new int[]{entry, a, b, c, exit};
    }

    /**
     * Diamond (if/else):
     *   ENTRY → BRANCH
     *     BRANCH --TRUE-->  A → MERGE
     *     BRANCH --FALSE--> B → MERGE
     *   MERGE → TARGET → EXIT
     *
     * Returns [entryId, branchId, aId, bId, mergeId, targetId, exitId]
     */
    private int[] buildDiamondGraph(ControlFlowGraph cfg) {
        int entry  = addEntry(cfg);
        int branch = addBranch(cfg, "cond");
        int a      = addStmt(cfg, "A");
        int b      = addStmt(cfg, "B");
        int merge  = addStmt(cfg, "merge");
        int target = addStmt(cfg, "target");
        int exit   = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, a);
        falseEdge(cfg, branch, b);
        normal(cfg, a, merge);
        normal(cfg, b, merge);
        normal(cfg, merge, target);
        normal(cfg, target, exit);
        return new int[]{entry, branch, a, b, merge, target, exit};
    }

    /**
     * Simple while-loop:
     *   ENTRY → LOOP_COND --TRUE-->  BODY → LOOP_COND (back-edge)
     *                    --FALSE--> EXIT
     *
     * Returns [entryId, loopCondId, bodyId, exitId]
     */
    private int[] buildWhileLoop(ControlFlowGraph cfg) {
        int entry    = addEntry(cfg);
        int loopCond = addLoop(cfg, "while_cond");
        int body     = addStmt(cfg, "body");
        int exit     = addExit(cfg);
        normal(cfg, entry, loopCond);
        trueEdge(cfg, loopCond, body);
        normal(cfg, body, loopCond);        // back-edge
        falseEdge(cfg, loopCond, exit);
        return new int[]{entry, loopCond, body, exit};
    }

    // -----------------------------------------------------------------------
    // Group 1 – Degenerate / error cases
    // -----------------------------------------------------------------------

    @Test
    public void noEntryNode_returnsEmpty() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int exit   = addExit(cfg);
        int target = addStmt(cfg, "A");
        normal(cfg, target, exit);

        assertTrue(finder.findPath(cfg, target).isEmpty());
    }

    @Test
    public void noExitNode_returnsEmpty() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int target = addStmt(cfg, "A");
        normal(cfg, entry, target);

        assertTrue(finder.findPath(cfg, target).isEmpty());
    }

    @Test
    public void targetCannotReachExit_returnsEmpty() {
        // ENTRY → TARGET   EXIT (no edge from TARGET to EXIT)
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int target = addStmt(cfg, "A");
        addExit(cfg);                        // EXIT exists but is unreachable from TARGET
        normal(cfg, entry, target);

        assertTrue(finder.findPath(cfg, target).isEmpty());
    }

    @Test
    public void requiredBranchOutcomeWithUnmodelledDownstream_returnsConstraintPrefix() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int target = addBranch(cfg, "condition");
        int sink = addStmt(cfg, "unmodelled-returning-construct");
        addExit(cfg);
        normal(cfg, entry, target);
        falseEdge(cfg, target, sink);

        List<List<ControlFlowGraph.Edge>> paths =
                finder.findPath(cfg, target, CfgEdgeKind.FALSE);

        assertEquals(1, paths.size());
        assertEquals(List.of(entry, target, sink), nodeIds(paths.get(0)));
        assertEquals(CfgEdgeKind.FALSE,
                paths.get(0).get(paths.get(0).size() - 1).getKind());
    }

    @Test
    public void disconnectedTarget_returnsEmpty() {
        // ENTRY → EXIT   TARGET (isolated – neither reachable from ENTRY nor linked to EXIT)
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int exit   = addExit(cfg);
        int target = addStmt(cfg, "isolated");
        normal(cfg, entry, exit);
        normal(cfg, target, exit);  // target→EXIT exists but ENTRY can't reach target

        assertTrue(finder.findPath(cfg, target).isEmpty());
    }

    @Test
    public void entryLinksDirectlyToExit_targetOrphaned_returnsEmpty() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int exit   = addExit(cfg);
        int target = addStmt(cfg, "orphan");
        normal(cfg, entry, exit);
        // target has an exit edge but is not reachable from entry
        normal(cfg, target, exit);

        assertTrue(finder.findPath(cfg, target).isEmpty());
    }

    // -----------------------------------------------------------------------
    // Group 2 – Linear graph
    // -----------------------------------------------------------------------

    @Test
    public void linearGraph_targetInMiddle_returnsExactlyOnePath() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);
        int entry = ids[0], target = ids[2], exit = ids[4];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, target);
    }

    @Test
    public void linearGraph_targetInMiddle_pathHasExactlyFourEdges() {
        // ENTRY→A→B(target)→C→EXIT = 4 edges
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, ids[2]);

        assertEquals(4, result.get(0).size());
    }

    @Test
    public void linearGraph_targetIsNodeBeforeExit_returnsOnePath() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);
        int entry = ids[0], target = ids[3] /*C*/, exit = ids[4];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
    }

    @Test
    public void linearGraph_resultListIsUnmodifiable() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);

        assertResultIsUnmodifiable(finder.findPath(cfg, ids[2]));
    }

    @Test
    public void linearGraph_noNullPathsInResult() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);

        for (List<ControlFlowGraph.Edge> path : finder.findPath(cfg, ids[2])) {
            assertNotNull(path);
        }
    }

    @Test
    public void linearGraph_edgeNodeSequenceMatchesExpected() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);
        // ids = [entry, a, b(target), c, exit]

        List<Integer> actualNodes = nodeIds(finder.findPath(cfg, ids[2]).get(0));

        assertEquals(Arrays.asList(ids[0], ids[1], ids[2], ids[3], ids[4]), actualNodes);
    }

    // -----------------------------------------------------------------------
    // Group 3 – Branching (if / else)
    // -----------------------------------------------------------------------

    @Test
    public void ifElse_targetAfterMerge_returnsExactlyTwoPaths() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int entry = ids[0], target = ids[5], exit = ids[6];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, target);
    }

    @Test
    public void ifElse_twoPaths_coverBothBranches() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int aId = ids[2], bId = ids[3];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, ids[5]);

        Set<Integer> allNodes = result.stream()
                .flatMap(p -> nodeIds(p).stream())
                .collect(Collectors.toSet());
        assertTrue("TRUE branch node A must be covered", allNodes.contains(aId));
        assertTrue("FALSE branch node B must be covered", allNodes.contains(bId));
    }

    @Test
    public void ifElse_targetInsideTrueBranch_returnsOnePath_withTrueEdge() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int entry = ids[0], aId = ids[2], exit = ids[6];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, aId);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
        boolean hasTrueEdgeToA = result.get(0).stream()
                .anyMatch(e -> e.getTo() == aId && e.getKind() == CfgEdgeKind.TRUE);
        assertTrue("Path to A must arrive via a TRUE edge", hasTrueEdgeToA);
    }

    @Test
    public void ifElse_targetInsideFalseBranch_returnsOnePath_withFalseEdge() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int entry = ids[0], bId = ids[3], exit = ids[6];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, bId);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
        boolean hasFalseEdgeToB = result.get(0).stream()
                .anyMatch(e -> e.getTo() == bId && e.getKind() == CfgEdgeKind.FALSE);
        assertTrue("Path to B must arrive via a FALSE edge", hasFalseEdgeToB);
    }

    @Test
    public void ifElse_targetIsBranchNode_returnsOnePaths() {
        // ENTRY → BRANCH(target) → {TRUE: A→EXIT, FALSE: B→EXIT}
        // prefix = [ENTRY→BRANCH], suffix = 2 routes to EXIT
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int entry = ids[0], branch = ids[1], exit = ids[6];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, branch);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, branch);
    }

    @Test
    public void ifElse_mergeNode_returnsTwoPaths() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        int entry = ids[0], merge = ids[4], exit = ids[6];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, merge);

        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, merge);
    }

    // -----------------------------------------------------------------------
    // Group 4 – Loops
    // -----------------------------------------------------------------------

    @Test
    public void whileLoop_targetIsBody_returnsTwoPaths() {
        // With MAX_LOOP_ITERATIONS=1, body is reached on iteration 1 AND iteration 2.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int entry = ids[0], body = ids[2], exit = ids[3];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, body);
    }

    @Test
    public void whileLoop_targetIsBody_noPathVisitsBodyMoreThanTwice() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int body = ids[2];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        for (List<ControlFlowGraph.Edge> path : result) {
            assertTrue("Body node visited at most 2 times (MAX_NODE_VISITS)",
                    nodeVisitCount(path, body) <= 2);
        }
    }

    @Test
    public void whileLoop_targetIsCondition_returnsTwoPaths() {
        // LOOP_COND hit on first arrival and again after one body iteration.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int entry = ids[0], loopCond = ids[1], exit = ids[3];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, loopCond);

        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, loopCond);
    }

    @Test
    public void whileLoop_targetAfterLoop_returnsTwoPaths() {
        // ENTRY → LOOP --TRUE--> BODY → LOOP (back)
        //              --FALSE-> AFTER_LOOP(target) → EXIT
        // Two paths: zero-iteration path, and one-iteration path.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry     = addEntry(cfg);
        int loopCond  = addLoop(cfg, "cond");
        int body      = addStmt(cfg, "body");
        int afterLoop = addStmt(cfg, "after");   // target
        int exit      = addExit(cfg);
        normal(cfg, entry, loopCond);
        trueEdge(cfg, loopCond, body);
        normal(cfg, body, loopCond);
        falseEdge(cfg, loopCond, afterLoop);
        normal(cfg, afterLoop, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, afterLoop);

        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, afterLoop);
    }

    @Test
    public void whileLoop_allPaths_endAtExit() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int exit = ids[3];

        for (List<ControlFlowGraph.Edge> path : finder.findPath(cfg, ids[2])) {
            assertEquals("Every path must end at EXIT",
                    exit, path.get(path.size() - 1).getTo());
        }
    }

    @Test
    public void alternativePaths_includeLongerLoopSuffix() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int target = addStmt(cfg, "target");
        int loop = addLoop(cfg, "loop");
        int body = addStmt(cfg, "body");
        int exit = addExit(cfg);
        normal(cfg, entry, target);
        normal(cfg, target, loop);
        falseEdge(cfg, loop, exit);
        trueEdge(cfg, loop, body);
        normal(cfg, body, loop);

        List<List<ControlFlowGraph.Edge>> result =
                finder.findAlternativePaths(cfg, target, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(path -> pathContainsNode(path, body)));
        assertAllPathsBounded(result, entry, exit);
    }

    @Test
    public void loopWithInternalBranch_targetInTrueSide_isReachable() {
        // ENTRY → LOOP --TRUE--> BRANCH --TRUE-->  X(target) → LOOP
        //                                 --FALSE--> Y → LOOP
        //              --FALSE--> EXIT
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int loop   = addLoop(cfg, "loop");
        int branch = addBranch(cfg, "branch");
        int x      = addStmt(cfg, "X");   // target
        int y      = addStmt(cfg, "Y");
        int exit   = addExit(cfg);
        normal(cfg, entry, loop);
        trueEdge(cfg, loop, branch);
        trueEdge(cfg, branch, x);
        falseEdge(cfg, branch, y);
        normal(cfg, x, loop);
        normal(cfg, y, loop);
        falseEdge(cfg, loop, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, x);

        assertFalse("Must find at least one path to X", result.isEmpty());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, x);
    }

    @Test
    public void loopWithInternalBranch_noNodeExceedsMaxNodeVisits() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int loop   = addLoop(cfg, "loop");
        int branch = addBranch(cfg, "branch");
        int x      = addStmt(cfg, "X");
        int y      = addStmt(cfg, "Y");
        int exit   = addExit(cfg);
        normal(cfg, entry, loop);
        trueEdge(cfg, loop, branch);
        trueEdge(cfg, branch, x);
        falseEdge(cfg, branch, y);
        normal(cfg, x, loop);
        normal(cfg, y, loop);
        falseEdge(cfg, loop, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, x);
        int i = 0;
        for (List<ControlFlowGraph.Edge> path : result) {
            Map<Integer, Long> freq = nodeIds(path).stream()
                    .collect(Collectors.groupingBy(n -> n, Collectors.counting()));
            for (Map.Entry<Integer, Long> e : freq.entrySet()) {
                assertTrue("Node " + e.getKey() + " in path " + i +" visited " + e.getValue() + " times – exceeds MAX_NODE_VISITS",
                        e.getValue() <= 3);
            }
            i++;
        }
    }

    @Test
    public void higherMaxLoopIterations_containsAllPathsFromLowerMaxLoopIterations() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int body = ids[2];

        AllPathsFinder lowerLimitFinder = new AllPathsFinder();
        lowerLimitFinder.setMAX_LOOP_ITERATIONS(5);
        Set<List<Integer>> lowerLimitPaths = nodeIdSet(lowerLimitFinder.findPath(cfg, body));

        AllPathsFinder higherLimitFinder = new AllPathsFinder();
        higherLimitFinder.setMAX_LOOP_ITERATIONS(10);
        Set<List<Integer>> higherLimitPaths = nodeIdSet(higherLimitFinder.findPath(cfg, body));

        assertFalse("Lower loop limit should find paths", lowerLimitPaths.isEmpty());
        assertTrue(
                "Paths found with loop limit 10 must contain every path found with loop limit 5",
                higherLimitPaths.containsAll(lowerLimitPaths));
        assertTrue(
                "Higher loop limit should permit additional loop-unrolled paths",
                higherLimitPaths.size() > lowerLimitPaths.size());
    }

    // -----------------------------------------------------------------------
    // Group 5 – Target = ENTRY or EXIT
    // -----------------------------------------------------------------------

    @Test
    public void targetIsEntryNode_prefixEmpty_returnsOnePath() {
        // ENTRY(target) → A → EXIT
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int a     = addStmt(cfg, "A");
        int exit  = addExit(cfg);
        normal(cfg, entry, a);
        normal(cfg, a, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, entry);

        assertEquals(1, result.size());
        assertEquals(entry, result.get(0).get(0).getFrom());
        assertEquals(exit,  result.get(0).get(result.get(0).size() - 1).getTo());
    }

    @Test
    public void targetIsExitNode_suffixEmpty_returnsOnePath() {
        // ENTRY → A → EXIT(target)
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int a     = addStmt(cfg, "A");
        int exit  = addExit(cfg);
        normal(cfg, entry, a);
        normal(cfg, a, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, exit);

        assertEquals(1, result.size());
        assertEquals(entry, result.get(0).get(0).getFrom());
        assertEquals(exit,  result.get(0).get(result.get(0).size() - 1).getTo());
    }

    // -----------------------------------------------------------------------
    // Group 6 – Deduplication
    // -----------------------------------------------------------------------

    @Test
    public void twoDistinctPaths_bothPresent_neitherDropped() {
        // ENTRY → BRANCH
        //   --TRUE-->  A(target) → EXIT
        //   --FALSE--> B         → EXIT
        // Only one path reaches A.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int branch = addBranch(cfg, "cond");
        int a      = addStmt(cfg, "A");   // target
        int b      = addStmt(cfg, "B");
        int exit   = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, a);
        falseEdge(cfg, branch, b);
        normal(cfg, a, exit);
        normal(cfg, b, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, a);

        assertEquals(1, result.size());
        assertAllPathsBounded(result, entry, exit);
    }

    @Test
    public void diamondGraph_mergeTarget_noDuplicatePaths() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);
        // Two structurally different routes to MERGE – must not deduplicate them
        // since they traverse different nodes.
        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, ids[4] /* merge */);

        assertEquals(2, result.size());
        // Verify the two paths are actually distinct by comparing their node sequences
        List<Integer> path0nodes = nodeIds(result.get(0));
        List<Integer> path1nodes = nodeIds(result.get(1));
        assertFalse("Two paths must be distinct", path0nodes.equals(path1nodes));
    }

    @Test
    public void resultPathsContainNoNullElements() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, ids[5]);

        for (List<ControlFlowGraph.Edge> path : result) {
            for (ControlFlowGraph.Edge edge : path) {
                assertNotNull("Edge in path must not be null", edge);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Group 7 – Explosion guards
    // -----------------------------------------------------------------------

    @Test
    public void maxPathsGuard_binaryTreeWith9Levels_resultAtMost256() {
        // 2^9 = 512 leaf paths → expect result capped at MAX_PATHS = 256.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int exit   = addExit(cfg);
        int target = addStmt(cfg, "target");
        normal(cfg, target, exit);

        List<Integer> frontier = new ArrayList<>();
        frontier.add(entry);
        for (int level = 0; level < 9; level++) {
            List<Integer> next = new ArrayList<>();
            for (int node : frontier) {
                int left  = addStmt(cfg, "L" + level + "_" + node);
                int right = addStmt(cfg, "R" + level + "_" + node);
                trueEdge(cfg, node, left);
                falseEdge(cfg, node, right);
                next.add(left);
                next.add(right);
            }
            frontier = next;
        }
        for (int leaf : frontier) {
            normal(cfg, leaf, target);
        }

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertFalse("Result must not be empty", result.isEmpty());
        assertTrue("Result must be capped at MAX_PATHS (256)", result.size() <= 256);
        // All returned paths must still be structurally valid
        assertAllPathsBounded(result, entry, exit);
    }

    @Test
    public void maxPathEdgesGuard_chainOf600Stmts_returnsEmpty() {
        // Chain length 600 > MAX_PATH_EDGES 512 → DFS prunes before reaching target.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int exit   = addExit(cfg);
        int target = addStmt(cfg, "target");
        normal(cfg, target, exit);

        int prev = entry;
        for (int i = 0; i < 600; i++) {
            int node = addStmt(cfg, "s" + i);
            normal(cfg, prev, node);
            prev = node;
        }
        normal(cfg, prev, target);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertTrue("Chain of 600 nodes must be pruned – result must be empty", result.isEmpty());
    }

    @Test
    public void maxNodeVisitsGuard_loopBody_neverVisitedMoreThanTwice() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int body = ids[2];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        for (List<ControlFlowGraph.Edge> path : result) {
            Map<Integer, Long> freq = nodeIds(path).stream()
                    .collect(Collectors.groupingBy(n -> n, Collectors.counting()));
            for (Map.Entry<Integer, Long> e : freq.entrySet()) {
                assertTrue(
                        "Node " + e.getKey() + " appears " + e.getValue() + " times, "
                                + "exceeds MAX_NODE_VISITS (3)",
                        e.getValue() <= 3);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Group 8 – Edge-kind preservation
    // -----------------------------------------------------------------------

    @Test
    public void linearGraph_allEdgesAreNormal() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildLinearGraph(cfg);

        List<ControlFlowGraph.Edge> path = finder.findPath(cfg, ids[2]).get(0);

        for (ControlFlowGraph.Edge e : path) {
            assertEquals("Linear graph must use only NORMAL edges",
                    CfgEdgeKind.NORMAL, e.getKind());
        }
    }

    @Test
    public void diamondGraph_trueAndFalseEdgesPreservedInResults() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildDiamondGraph(cfg);

        Set<CfgEdgeKind> kinds = finder.findPath(cfg, ids[5]).stream()
                .flatMap(Collection::stream)
                .map(ControlFlowGraph.Edge::getKind)
                .collect(Collectors.toSet());

        assertTrue("TRUE edge must appear across results", kinds.contains(CfgEdgeKind.TRUE));
        assertTrue("FALSE edge must appear across results", kinds.contains(CfgEdgeKind.FALSE));
    }

    @Test
    public void whileLoop_trueEdgeLeadsToBody_falseEdgeLeadsToExit() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int[] ids = buildWhileLoop(cfg);
        int loopCond = ids[1], body = ids[2], exit = ids[3];

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        for (List<ControlFlowGraph.Edge> path : result) {
            // Every edge entering body must be TRUE; every edge entering exit must be FALSE.
            for (ControlFlowGraph.Edge e : path) {
                if (e.getTo() == body && e.getFrom() == loopCond) {
                    assertEquals("Edge LOOP_COND→BODY must be TRUE",
                            CfgEdgeKind.TRUE, e.getKind());
                }
                if (e.getTo() == exit && e.getFrom() == loopCond) {
                    assertEquals("Edge LOOP_COND→EXIT must be FALSE",
                            CfgEdgeKind.FALSE, e.getKind());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Group 9 – Minimal graph
    // -----------------------------------------------------------------------

    @Test
    public void minimalGraph_entryStmtExit_exactEdgeSequence() {
        // ENTRY → STMT(target) → EXIT
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry  = addEntry(cfg);
        int target = addStmt(cfg, "only");
        int exit   = addExit(cfg);
        normal(cfg, entry, target);
        normal(cfg, target, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, target);

        assertEquals(1, result.size());
        List<ControlFlowGraph.Edge> path = result.get(0);
        assertEquals(2, path.size());
        assertEquals(entry,  path.get(0).getFrom());
        assertEquals(target, path.get(0).getTo());
        assertEquals(target, path.get(1).getFrom());
        assertEquals(exit,   path.get(1).getTo());
    }

    // -----------------------------------------------------------------------
    // Group 10 – do-while style back-edge
    // -----------------------------------------------------------------------

    @Test
    public void doWhileStyle_targetIsBody_returnsTwoPaths() {
        // ENTRY → BODY(target) → COND --TRUE-->  BODY (back-edge)
        //                            --FALSE--> EXIT
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int body  = addStmt(cfg, "body");   // target
        int cond  = addLoop(cfg, "cond");
        int exit  = addExit(cfg);
        normal(cfg, entry, body);
        normal(cfg, body, cond);
        trueEdge(cfg, cond, body);           // back-edge
        falseEdge(cfg, cond, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        // Iteration 0: ENTRY→BODY (body visited once) + BODY→COND→EXIT
        // Iteration 1: ENTRY→BODY→COND→BODY (body visited twice) + COND→EXIT
        assertEquals(2, result.size());
        assertAllPathsBounded(result, entry, exit);
        assertAllPathsContainTarget(result, body);
    }

    @Test
    public void doWhileStyle_targetIsBody_shorterPathComesFirst() {
        // The shorter path (0 extra iterations) should have fewer edges than the longer one.
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int body  = addStmt(cfg, "body");
        int cond  = addLoop(cfg, "cond");
        int exit  = addExit(cfg);
        normal(cfg, entry, body);
        normal(cfg, body, cond);
        trueEdge(cfg, cond, body);
        falseEdge(cfg, cond, exit);

        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, body);

        // Collect sizes – one path must be strictly shorter than the other
        int size0 = result.get(0).size();
        int size1 = result.get(1).size();
        assertFalse("The two do-while paths must have different lengths", size0 == size1);
    }

    @Test
    public void loopNestedITE_targetIsNodeAfterITE_pathsContains2Branches() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int exit = addExit(cfg);
        int statement1 = addStmt(cfg, "int i=0;");
        int statement2 = addStmt(cfg, "int count=0;");
        int statement3 = addStmt(cfg, "int k=4;");
        int loopCond = addLoop(cfg, "i<n");
        int iteCond1 = addBranch(cfg, "i%2==0");
        int trueNode1 = addStmt(cfg, "count+=1;");
        int iteCond2 = addBranch(cfg, "i==k");
        int trueNode2 = addStmt(cfg, "break;");
        int incNode = addStmt(cfg, "i++");
        int returnNode = addStmt(cfg, "return count;");
        normal(cfg, entry, statement1);
        normal(cfg, statement1, statement2);
        normal(cfg, statement2, statement3);
        normal(cfg, statement3, loopCond);
        trueEdge(cfg, loopCond, iteCond1);
        trueEdge(cfg, iteCond1, trueNode1);
        normal(cfg, trueNode1, iteCond2);
        falseEdge(cfg, iteCond1, iteCond2);
        trueEdge(cfg, iteCond2, trueNode2);
        normal(cfg, trueNode2, incNode);
        falseEdge(cfg, iteCond2, incNode);
        normal(cfg, incNode, loopCond);
        falseEdge(cfg, loopCond, returnNode);
        normal(cfg, returnNode, exit);
        int MAX_LOOPS = 10;
        finder.setMAX_LOOP_ITERATIONS(MAX_LOOPS);
        boolean hasTrue = false;
        boolean hasFalse = false;
        List<List<ControlFlowGraph.Edge>> result = finder.findPath(cfg, trueNode2);
        for (List<ControlFlowGraph.Edge> path : result) {
            for (ControlFlowGraph.Edge e : path) {
                if (e.getFrom() == iteCond1) {
                    if (e.getKind() == CfgEdgeKind.TRUE) {
                        hasTrue = true;
                    } else if (e.getKind() == CfgEdgeKind.FALSE) {
                        hasFalse = true;
                    }

                    if (hasTrue && hasFalse) {
                        break;
                    }
                }
            }
            if (hasTrue && hasFalse) {
                break;
            }
        }
        assertTrue(hasTrue);
        assertTrue(hasFalse);
    }
}
