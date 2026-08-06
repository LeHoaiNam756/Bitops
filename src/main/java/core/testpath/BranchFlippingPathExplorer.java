package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Builds concolic follow-up paths by negating one branch from an observed run.
 *
 * <p>This is deliberately not an all-paths finder. For a trace containing
 * decisions {@code b0, b1, b2}, it emits candidates equivalent to:
 * {@code !b0}, {@code b0 && !b1}, {@code b0 && b1 && !b2}, then lets the
 * next concrete run reveal more branch decisions.</p>
 */
public final class BranchFlippingPathExplorer {
    private static final int MAX_CANDIDATES_PER_TRACE = 256;

    public List<List<ControlFlowGraph.Edge>> flippedPaths(
            ControlFlowGraph cfg,
            List<BranchDecision> observedBranches) {
        if (observedBranches == null || observedBranches.isEmpty()) {
            return List.of();
        }

        int entry = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exit = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entry < 0 || exit < 0) {
            return List.of();
        }

        List<List<ControlFlowGraph.Edge>> candidates = new ArrayList<>();
        Set<List<ControlFlowGraph.Edge>> seen = new HashSet<>();
        for (int flipIndex = observedBranches.size() - 1;
             flipIndex >= 0 && candidates.size() < MAX_CANDIDATES_PER_TRACE;
             flipIndex--) {
            List<ControlFlowGraph.Edge> candidate =
                    pathWithFlippedBranch(cfg, entry, exit, observedBranches, flipIndex);
            if (candidate != null && seen.add(candidate)) {
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingInt(List::size));
        return List.copyOf(candidates);
    }

    public List<ControlFlowGraph.Edge> shortestEntryToExit(ControlFlowGraph cfg) {
        int entry = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exit = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entry < 0 || exit < 0) {
            return List.of();
        }
        List<ControlFlowGraph.Edge> path = shortestPath(cfg, entry, exit);
        return path == null ? List.of() : path;
    }

    public List<ControlFlowGraph.Edge> shortestPathThrough(
            ControlFlowGraph cfg,
            int targetNodeId,
            CfgEdgeKind requiredExit) {
        int entry = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exit = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entry < 0 || exit < 0 || cfg.getNode(targetNodeId) == null) {
            return List.of();
        }

        List<ControlFlowGraph.Edge> result = new ArrayList<>();
        if (!appendShortestPath(cfg, entry, targetNodeId, result)) {
            return List.of();
        }

        int suffixStart = targetNodeId;
        if (requiredExit != null) {
            ControlFlowGraph.Edge requiredEdge =
                    outgoingEdge(cfg, targetNodeId, requiredExit);
            if (requiredEdge == null) {
                return List.of();
            }
            result.add(requiredEdge);
            suffixStart = requiredEdge.getTo();
        }

        if (!appendShortestPath(cfg, suffixStart, exit, result)) {
            return List.of();
        }
        return List.copyOf(result);
    }

    public List<ControlFlowGraph.Edge> shortestPrefixThrough(
            ControlFlowGraph cfg,
            int targetNodeId,
            CfgEdgeKind requiredExit) {
        int entry = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        if (entry < 0 || cfg.getNode(targetNodeId) == null) {
            return List.of();
        }

        List<ControlFlowGraph.Edge> result = new ArrayList<>();
        if (!appendShortestPath(cfg, entry, targetNodeId, result)) {
            return List.of();
        }

        if (requiredExit != null) {
            ControlFlowGraph.Edge requiredEdge =
                    outgoingEdge(cfg, targetNodeId, requiredExit);
            if (requiredEdge == null) {
                return List.of();
            }
            result.add(requiredEdge);
        }
        return List.copyOf(result);
    }

    private List<ControlFlowGraph.Edge> pathWithFlippedBranch(
            ControlFlowGraph cfg,
            int entry,
            int exit,
            List<BranchDecision> observedBranches,
            int flipIndex) {
        List<ControlFlowGraph.Edge> result = new ArrayList<>();
        int current = entry;

        for (int i = 0; i < flipIndex; i++) {
            BranchDecision decision = observedBranches.get(i);
            if (!appendShortestPath(cfg, current, decision.nodeId(), result)) {
                return null;
            }
            ControlFlowGraph.Edge observedEdge =
                    outgoingEdge(cfg, decision.nodeId(), decision.edgeKind());
            if (observedEdge == null) {
                return null;
            }
            result.add(observedEdge);
            current = observedEdge.getTo();
        }

        BranchDecision flippedDecision = observedBranches.get(flipIndex);
        if (!appendShortestPath(cfg, current, flippedDecision.nodeId(), result)) {
            return null;
        }
        ControlFlowGraph.Edge flippedEdge = outgoingEdge(
                cfg, flippedDecision.nodeId(), opposite(flippedDecision.edgeKind()));
        if (flippedEdge == null) {
            return null;
        }
        result.add(flippedEdge);
        return List.copyOf(result);
    }

    private boolean appendShortestPath(
            ControlFlowGraph cfg,
            int from,
            int to,
            List<ControlFlowGraph.Edge> destination) {
        if (from == to) {
            return true;
        }
        List<ControlFlowGraph.Edge> path = shortestPath(cfg, from, to);
        if (path == null) {
            return false;
        }
        destination.addAll(path);
        return true;
    }

    private List<ControlFlowGraph.Edge> shortestPath(
            ControlFlowGraph cfg,
            int start,
            int target) {
        if (start == target) {
            return List.of();
        }

        Queue<PathState> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(new PathState(start, List.of()));
        visited.add(start);

        while (!queue.isEmpty()) {
            PathState state = queue.remove();
            for (ControlFlowGraph.Edge edge : cfg.outgoing(state.node())) {
                List<ControlFlowGraph.Edge> nextPath = new ArrayList<>(state.path());
                nextPath.add(edge);
                if (edge.getTo() == target) {
                    return List.copyOf(nextPath);
                }
                if (visited.add(edge.getTo())) {
                    queue.add(new PathState(edge.getTo(), nextPath));
                }
            }
        }
        return null;
    }

    private ControlFlowGraph.Edge outgoingEdge(
            ControlFlowGraph cfg,
            int nodeId,
            CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(nodeId)) {
            if (edge.getKind() == kind) {
                return edge;
            }
        }
        return null;
    }

    private CfgEdgeKind opposite(CfgEdgeKind kind) {
        return kind == CfgEdgeKind.TRUE ? CfgEdgeKind.FALSE : CfgEdgeKind.TRUE;
    }

    private int findNodeByKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int id : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getKind() == kind) {
                return id;
            }
        }
        return -1;
    }

    private record PathState(int node, List<ControlFlowGraph.Edge> path) {}

    public record BranchDecision(int nodeId, CfgEdgeKind edgeKind) {}
}
