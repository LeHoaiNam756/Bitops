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
        Map<Integer, PathStep> prev = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        prev.put(start, new PathStep(start, null));

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == goal) {
                return buildPath(prev, goal);
            }
            for (CfgGraph.Edge edge : graph.outgoing(current)) {
                int next = edge.getTo();
                if (!prev.containsKey(next)) {
                    Boolean decision = edge.getKind() == CfgEdgeKind.TRUE
                            ? Boolean.TRUE
                            : edge.getKind() == CfgEdgeKind.FALSE ? Boolean.FALSE : null;
                    prev.put(next, new PathStep(next, decision));
                    queue.add(next);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<PathStep> buildPath(Map<Integer, PathStep> prev, int goal) {
        List<PathStep> path = new ArrayList<>();
        PathStep step = prev.get(goal);
        while (step != null) {
            path.add(step);
            int nodeId = step.getNodeId();
            PathStep parent = null;
            for (PathStep candidate : prev.values()) {
                if (candidate.getNodeId() == nodeId) {
                    parent = candidate;
                    break;
                }
            }
            if (parent == step) {
                break;
            }
            step = parent;
        }
        Collections.reverse(path);
        return path;
    }
}
