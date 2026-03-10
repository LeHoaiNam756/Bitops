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

        public BFSState(CfgNode node, LinkedList<PathNode> currentPath, Map<CfgNode, Integer> currentCounts) {
            this.currentNode = node;
            this.path = new LinkedList<>(currentPath);
            this.visitCounts = new HashMap<>(currentCounts);
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

        TestPath res1 = finder.bfs(start, mid, new HashMap<>());
        if (res1 == null) return null;

        TestPath res2 = finder.bfs(mid, end, res1.finalCounts);
        if (res2 == null) return null;

        LinkedList<PathNode> totalPath = res1.path;
        if (!totalPath.isEmpty()) totalPath.removeLast();
        totalPath.addAll(res2.path);

        return totalPath;
    }

    private TestPath bfs(CfgNode startNode, CfgNode targetNode, Map<CfgNode, Integer> initialCounts) {
        Queue<BFSState> queue = new LinkedList<>();
        int limit = Setup.nodeVisitLimit;

        queue.add(new BFSState(startNode, new LinkedList<>(), initialCounts));

        while (!queue.isEmpty()) {
            BFSState state = queue.poll();
            CfgNode curr = state.currentNode;

            int currentCount = state.visitCounts.getOrDefault(curr, 0) + 1;
            Map<CfgNode, Integer> nextCounts = new HashMap<>(state.visitCounts);
            nextCounts.put(curr, currentCount);

            if (curr.equals(targetNode)) {
                LinkedList<PathNode> finalPath = new LinkedList<>(state.path);
                finalPath.add(new PathNode(curr, null));
                return new TestPath(finalPath, nextCounts);
            }

            if (currentCount > limit) continue;

            if (curr instanceof CfgBoolExprNode) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                CfgBoolExprNode boolNode = (CfgBoolExprNode) curr;

                if (boolNode.getTrueNode() != null) {
                    queue.add(createNewState(state, boolNode.getTrueNode(), nextCounts, true));
                }
                if (boolNode.getFalseNode() != null) {
                    queue.add(createNewState(state, boolNode.getFalseNode(), nextCounts, false));
                }
            } else if (curr.getAfterNode() != null) {
                queue.add(createNewState(state, curr.getAfterNode(), nextCounts, null));
            }
        }
        return null;
    }

    private BFSState createNewState(BFSState oldState, CfgNode nextNode, Map<CfgNode, Integer> nextCounts,
                                    Boolean decision) {
        LinkedList<PathNode> nextPath = new LinkedList<>(oldState.path);
        nextPath.add(new PathNode(oldState.currentNode, decision));
        return new BFSState(nextNode, nextPath, nextCounts);
    }

}
