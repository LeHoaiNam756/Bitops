package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgBuilder;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Before;
import org.junit.Test;
import utils.Parser;

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

    @Test
    public void coverageAwareFindPath_prefersPathThatCoversMoreUncoveredNodes() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int target = addStmt(cfg, "target");
        int extraUncovered = addStmt(cfg, "extra");
        int exit = addExit(cfg);
        normal(cfg, entry, target);
        falseEdge(cfg, target, exit);
        trueEdge(cfg, target, extraUncovered);
        normal(cfg, extraUncovered, exit);
        CoverageTracker tracker = new CoverageTracker(cfg);
        tracker.markCovered(entry);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPath(cfg, target, null, tracker);

        assertFalse(paths.isEmpty());
        assertTrue(nodeIds(paths.get(0)).contains(target));
        assertTrue(nodeIds(paths.get(0)).contains(extraUncovered));
    }

    @Test
    public void findPathsForUncovered_returnsOneBatchCoveringCurrentUncoveredNodes() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        int entry = addEntry(cfg);
        int branch = addBranch(cfg, "cond");
        int trueNode = addStmt(cfg, "true");
        int falseNode = addStmt(cfg, "false");
        int exit = addExit(cfg);
        normal(cfg, entry, branch);
        trueEdge(cfg, branch, trueNode);
        falseEdge(cfg, branch, falseNode);
        normal(cfg, trueNode, exit);
        normal(cfg, falseNode, exit);
        CoverageTracker tracker = new CoverageTracker(cfg);
        tracker.markCovered(entry);
        tracker.markCovered(branch);
        tracker.markCovered(exit);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathsForUncovered(cfg, tracker);

        assertEquals(2, paths.size());
        assertEquals(Set.of(trueNode, falseNode), coveredNodes(paths).stream()
                .filter(tracker::isUncovered)
                .collect(Collectors.toSet()));
        for (List<ControlFlowGraph.Edge> path : paths) {
            assertTrue(pathContainsNode(path, trueNode) || pathContainsNode(path, falseNode));
        }
    }

    @Test
    public void tn1BenchmarkSeq_pathCoverCoversEveryReachableBasicBlock() {
        String src = """
                public static int TN1_benchmarkSeq(
                        int examScore,
                        int projectScore,
                        int attendanceDays,
                        int applicationTier) {
                    int scholarshipPoints = 0;

                    if (applicationTier >= 1) {
                        if (examScore > 10) {
                            scholarshipPoints += 1;
                        }
                        if (projectScore > 20) {
                            scholarshipPoints += 2;
                        }
                        if (attendanceDays < 30) {
                            scholarshipPoints += 4;
                        }
                    } else {
                        if (examScore <= 10) {
                            scholarshipPoints -= 1;
                        }
                    }

                    if (examScore > 0 && projectScore > 0) {
                        if (attendanceDays > 0) {
                            scholarshipPoints += 10;
                        } else {
                            scholarshipPoints += 20;
                        }
                    } else if (examScore > 0) {
                        scholarshipPoints += 30;
                    }

                    return scholarshipPoints;
                }
                """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));
        int entry = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exit = findNodeByKind(cfg, CfgNodeKind.EXIT);

        List<List<ControlFlowGraph.Edge>> paths = finder.findPathCover(cfg);

        assertEquals(4, paths.size());
        assertAllPathsBounded(paths, entry, exit);
        assertEquals(relevantNodes(cfg, entry, exit), coveredNodes(paths));
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=1");
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=2");
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=4");
        assertPathCoverContains(cfg, paths, "scholarshipPoints-=1");
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=10");
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=20");
        assertPathCoverContains(cfg, paths, "scholarshipPoints+=30");
        assertPathCoverContains(cfg, paths, "return scholarshipPoints;");
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

    private boolean pathContainsNode(List<ControlFlowGraph.Edge> path, int node) {
        return nodeIds(path).contains(node);
    }

    private Set<Integer> relevantNodes(ControlFlowGraph cfg, int entry, int exit) {
        Set<Integer> fromEntry = reachableForward(cfg, entry);
        Set<Integer> toExit = reachableBackward(cfg, exit);
        fromEntry.retainAll(toExit);
        return fromEntry;
    }

    private Set<Integer> reachableForward(ControlFlowGraph cfg, int start) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
                if (visited.add(edge.getTo())) {
                    queue.addLast(edge.getTo());
                }
            }
        }
        return visited;
    }

    private Set<Integer> reachableBackward(ControlFlowGraph cfg, int start) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (ControlFlowGraph.Edge edge : cfg.incoming(current)) {
                if (visited.add(edge.getFrom())) {
                    queue.addLast(edge.getFrom());
                }
            }
        }
        return visited;
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

    private int findNodeByKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int id : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getKind() == kind) {
                return id;
            }
        }
        fail("Missing node kind: " + kind);
        return -1;
    }

    private void assertPathCoverContains(
            ControlFlowGraph cfg,
            List<List<ControlFlowGraph.Edge>> paths,
            String content) {

        Set<Integer> covered = coveredNodes(paths);
        for (int id : covered) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getContent() != null
                    && node.getContent().replace(" ", "").contains(content.replace(" ", ""))) {
                return;
            }
        }
        fail("Path cover did not include CFG node containing: " + content);
    }
}
