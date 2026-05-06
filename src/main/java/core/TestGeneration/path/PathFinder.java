package core.TestGeneration.path;

import core.CFG.graph.CfgEdgeKind;
import core.CFG.graph.CfgGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class PathFinder {
    public List<PathStep> findPathThrough(CfgGraph graph, int start, int mid, int end) {
        List<PathStep> first = bfs(graph, start, mid);
        if (first.isEmpty()) {
            return Collections.emptyList();
        }
        List<PathStep> second = bfs(graph, mid, end);
        if (second.isEmpty()) {
            return Collections.emptyList();
        }
        List<PathStep> combined = new ArrayList<>(first);
        combined.addAll(second.subList(1, second.size()));
        return combined;
    }

    private List<PathStep> bfs(CfgGraph graph, int start, int goal) {
        Map<Integer, Integer> parents = new HashMap<>();
        Map<Integer, Boolean> decisions = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        parents.put(start, null);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == goal) {
                return buildPath(parents, decisions, goal);
            }
            for (CfgGraph.Edge edge : graph.outgoing(current)) {
                int next = edge.getTo();
                if (!parents.containsKey(next)) {
                    Boolean decision = edge.getKind() == CfgEdgeKind.TRUE
                            ? Boolean.TRUE
                            : edge.getKind() == CfgEdgeKind.FALSE ? Boolean.FALSE : null;
                    parents.put(next, current);
                    decisions.put(next, decision);
                    queue.add(next);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<PathStep> buildPath(Map<Integer, Integer> parents,
                                     Map<Integer, Boolean> decisions,
                                     int goal) {
        List<Integer> nodeIds = new ArrayList<>();
        Integer current = goal;
        while (current != null) {
            nodeIds.add(current);
            current = parents.get(current);
        }
        Collections.reverse(nodeIds);

        List<PathStep> path = new ArrayList<>(nodeIds.size());
        for (Integer nodeId : nodeIds) {
            path.add(new PathStep(nodeId, decisions.get(nodeId)));
        }
        return path;
    }
}
