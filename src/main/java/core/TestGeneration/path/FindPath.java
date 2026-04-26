package core.TestGeneration.path;

import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;
import core.utils.Setup;

import java.util.*;

public class FindPath {

    static class TestPath {
        public final LinkedList<PathNode> path;
        public final Map<CfgNode, Integer> finalCounts;

        public TestPath(LinkedList<PathNode> path, Map<CfgNode, Integer> finalCounts) {
            this.path = path;
            this.finalCounts = finalCounts;
        }
    }

    static class BFSState {
        CfgNode currentNode;
        LinkedList<PathNode> path;
        Map<CfgNode, Integer> visitCounts;
        int cycleCount;

        public BFSState(CfgNode node, LinkedList<PathNode> currentPath, Map<CfgNode, Integer> currentCounts,
                        int cycleCount) {
            this.currentNode = node;
            this.path = new LinkedList<>(currentPath);
            this.visitCounts = new HashMap<>(currentCounts);
            this.cycleCount = cycleCount;
        }
    }

    public static class PathNode {
        public final CfgNode node;
        public final Boolean decision;

        public PathNode(CfgNode node, Boolean decision) {
            this.node = node;
            this.decision = decision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathNode)) return false;
            @SuppressWarnings("PatternVariableCanBeUsed")
            PathNode p = (PathNode) o;
            return Objects.equals(node, p.node) && Objects.equals(decision, p.decision);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, decision);
        }
    }

    public static CfgNode getUncoveredNode(Set<CfgNode> totalCfgNodes, Set<CfgNode> coveredCfgNodes) {
        if (totalCfgNodes == null || coveredCfgNodes == null) {
            return null;
        }

        Set<CfgNode> uncoveredNodes = new HashSet<>(totalCfgNodes);
        uncoveredNodes.removeAll(coveredCfgNodes);
        uncoveredNodes.removeIf(node ->
                node != null && (node.isBeginCfgNode() || node.isEndCfgNode() || node.isFakeVisited())
        );

        if (uncoveredNodes.isEmpty()) {
            return null;
        }

        return uncoveredNodes.iterator().next();
    }

    public static LinkedList<PathNode> findPathThrough(CfgNode start, CfgNode mid, CfgNode end) {
        FindPath finder = new FindPath();

        // 1. Discover all back-edges in this CFG
        List<BackEdge> backEdges = findBackEdges(start);

        // 2. Find which back-edge (if any) guards 'mid', and how many cycles needed
        BackEdge relevantEdge    = null;
        int      requiredCycles  = 0;

        for (BackEdge be : backEdges) {
            int k = requiredCycleCount(start, mid, be, Setup.nodeVisitLimit);
            if (k > 0) {           // mid requires this loop to be traversed
                relevantEdge   = be;
                requiredCycles = k;
                break;
            }
        }

        // 3. Run cycle-aware BFS from start → mid
        TestPath res1 = (relevantEdge == null)
                ? finder.bfs(start, mid, new HashMap<>(), null, null, 0)
                : finder.bfs(start, mid, new HashMap<>(),
                relevantEdge.tail, relevantEdge.head, requiredCycles);

        if (res1 == null) return null;

        // 4. From mid → end, no additional cycles needed (just find exit)
        TestPath res2 = finder.bfs(mid, end, res1.finalCounts, null, null, 0);
        if (res2 == null) return null;

        LinkedList<PathNode> totalPath = res1.path;
        if (!totalPath.isEmpty()) totalPath.removeLast();
        totalPath.addAll(res2.path);
        return totalPath;
    }

    private TestPath bfs(CfgNode startNode, CfgNode targetNode, Map<CfgNode, Integer> initialCounts,
                         CfgNode backEdgeFrom, CfgNode backEdgeTo, int requiredCycles) {
        Queue<BFSState> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(new BFSState(startNode, new LinkedList<>(), initialCounts, 0));

        while (!queue.isEmpty()) {
            BFSState state = queue.poll();
            CfgNode curr = state.currentNode;

            String key = curr.hashCode() + ":" + state.cycleCount;
            if (!visited.add(key)) continue;

            int currentCount = state.visitCounts.getOrDefault(curr, 0) + 1;
            Map<CfgNode, Integer> nextCounts = new HashMap<>(state.visitCounts);
            nextCounts.put(curr, currentCount);

            if (curr.equals(targetNode) && state.cycleCount == requiredCycles) {
                LinkedList<PathNode> finalPath = new LinkedList<>(state.path);
                finalPath.add(new PathNode(curr, null));
                return new TestPath(finalPath, nextCounts);
            }

            if (currentCount > Setup.nodeVisitLimit) continue;

            if (curr instanceof CfgBoolExprNode) {
                CfgBoolExprNode b = (CfgBoolExprNode) curr;
                if (b.getTrueNode()  != null) enqueueSuccessor(state, b.getTrueNode(),  nextCounts, true,
                        requiredCycles, backEdgeFrom, backEdgeTo, queue);
                if (b.getFalseNode() != null) enqueueSuccessor(state, b.getFalseNode(), nextCounts, false,
                        requiredCycles, backEdgeFrom, backEdgeTo, queue);
            } else if (curr.getAfterNode() != null) {
                enqueueSuccessor(state, curr.getAfterNode(), nextCounts, null, requiredCycles,
                        backEdgeFrom, backEdgeTo, queue);
            }
        }
        return null;
    }

    private void enqueueSuccessor(BFSState state, CfgNode next, Map<CfgNode, Integer> nextCounts,
                                  Boolean decision, int requiredCycles,
                                  CfgNode backEdgeFrom, CfgNode backEdgeTo,
                                  Queue<BFSState> queue) {
        int nextCycleCount = state.cycleCount;
        if (state.currentNode.equals(backEdgeFrom) && next.equals(backEdgeTo)) {
            nextCycleCount++;
            if (nextCycleCount > requiredCycles) return; // prune over-loop
        }
        LinkedList<PathNode> nextPath = new LinkedList<>(state.path);
        nextPath.add(new PathNode(state.currentNode, decision));
        queue.add(new BFSState(next, nextPath, nextCounts, nextCycleCount));
    }

    /**
     * A back-edge descriptor: the node that jumps back (tail)
     * and the loop header it returns to (head).
     *
     * @param head loop entry (the CfgBoolExprNode guard)
     * @param tail node whose successor is head (closes the loop)
     */
        public record BackEdge(CfgNode head, CfgNode tail) {
    }

    /**
     * DFS over the CFG from 'start', collecting all back-edges.
     * A back-edge is any edge (u → v) where v is an ancestor of u
     * in the DFS tree (i.e. v is already on the current DFS stack).
     */
    public static List<BackEdge> findBackEdges(CfgNode start) {
        List<BackEdge> backEdges = new ArrayList<>();
        Set<CfgNode> onStack  = new HashSet<>(); // current DFS path
        Set<CfgNode> visited  = new HashSet<>(); // all ever visited

        dfs(start, onStack, visited, backEdges);
        return backEdges;
    }

    private static void dfs(CfgNode node, Set<CfgNode> onStack,
                            Set<CfgNode> visited, List<BackEdge> backEdges) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);
        onStack.add(node);

        List<CfgNode> successors = getSuccessors(node);
        for (CfgNode succ : successors) {
            if (onStack.contains(succ)) {
                // succ is an ancestor → this is a back-edge
                backEdges.add(new BackEdge(succ, node));
            } else {
                dfs(succ, onStack, visited, backEdges);
            }
        }

        onStack.remove(node);
    }

    /** Unified successor list regardless of node type. */
    private static List<CfgNode> getSuccessors(CfgNode node) {
        List<CfgNode> result = new ArrayList<>();
        if (node instanceof CfgBoolExprNode) {
            @SuppressWarnings("PatternVariableCanBeUsed")
            CfgBoolExprNode b = (CfgBoolExprNode) node;
            if (b.getTrueNode()  != null) result.add(b.getTrueNode());
            if (b.getFalseNode() != null) result.add(b.getFalseNode());
        } else if (node.getAfterNode() != null) {
            result.add(node.getAfterNode());
        }
        return result;
    }

    /**
     * Returns how many times the back-edge must be crossed to reach 'target'
     * from 'start'. Returns 0 if target is reachable without any loop.
     * Returns -1 if target is unreachable even with looping.
     */
    public static int requiredCycleCount(CfgNode start, CfgNode target,
                                         BackEdge backEdge, int maxCycles) {
        for (int k = 0; k <= maxCycles; k++) {
            if (canReach(start, target, backEdge, k)) return k;
        }
        return -1; // unreachable within maxCycles
    }

    /**
     * Simple reachability check: can we reach 'target' from 'start'
     * crossing the given back-edge exactly 'cycles' times?
     */
    private static boolean canReach(CfgNode start, CfgNode target,
                                    BackEdge backEdge, int cycles) {
        // State: (node, cyclesCompleted)
        record State(CfgNode node, int k) {}

        Queue<State>  queue   = new LinkedList<>();
        Set<State>    visited = new HashSet<>();

        queue.add(new State(start, 0));

        while (!queue.isEmpty()) {
            State s = queue.poll();
            if (!visited.add(s)) continue;

            if (s.node.equals(target) && s.k == cycles) return true;
            if (s.k > cycles) continue;

            for (CfgNode succ : getSuccessors(s.node)) {
                int nextK = (s.node.equals(backEdge.tail)
                        && succ.equals(backEdge.head))
                        ? s.k + 1 : s.k;
                if (nextK <= cycles) queue.add(new State(succ, nextK));
            }
        }
        return false;
    }
}
