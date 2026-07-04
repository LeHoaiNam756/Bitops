package core.testpath;

import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import lombok.Setter;

import java.util.*;

/**
 * Finds all complete paths:  ENTRY ──(all paths, cycle-limited)──► target ──(shortest)──► EXIT.
 * When a requested branch outcome cannot reach the modelled EXIT, it returns
 * the constraint prefix through that outcome; this still suffices to generate
 * a concrete input for coverage of partially modelled control-flow constructs.
 *
 * <h3>Cycle / loop handling</h3>
 * {@link #MAX_LOOP_ITERATIONS} controls how many full loop iterations are
 * unrolled on any single path.  Each node may appear on a path at most
 * {@code MAX_LOOP_ITERATIONS + 1} times.  If the target node is inside a
 * loop, the DFS will produce separate paths for "target reached on iteration
 * 1", "target reached on iteration 2", … up to the limit.
 *
 * <h3>Path-explosion guards</h3>
 * <ol>
 *   <li><b>MAX_PATHS</b>          – hard cap on distinct results collected.</li>
 *   <li><b>MAX_PATH_EDGES</b>     – prune any branch whose current edge count
 *                                    exceeds this threshold.</li>
 *   <li><b>Deduplication</b>      – identical edge sequences (same path found
 *                                    via two DFS routes) are discarded.</li>
 * </ol>
 */
public class AllPathsFinder implements PathFinder {

    // ------------------------------------------------------------------
    // Tuneable limits
    // ------------------------------------------------------------------

    /** Number of full loop iterations to unroll (k=1 → visit each node at most twice). */
    private int MAX_LOOP_ITERATIONS = 2;

    /** Maximum number of distinct ENTRY→target paths to collect before stopping. */
    private int MAX_PATHS = 256;

    /** Maximum number of edges allowed on a single path before pruning that branch. */
    private int MAX_PATH_EDGES = 512;

    // Derived: a node may appear on one path at most this many times.
    private int MAX_NODE_VISITS = MAX_LOOP_ITERATIONS + 1;


    public void setMAX_LOOP_ITERATIONS(int MAX_LOOP_ITERATIONS) {
        if (MAX_LOOP_ITERATIONS < 0) {
            throw new IllegalArgumentException("MAX_LOOP_ITERATIONS must be greater than 0");
        }
        this.MAX_LOOP_ITERATIONS = MAX_LOOP_ITERATIONS;
        this.MAX_NODE_VISITS = MAX_LOOP_ITERATIONS + 1;
    }

    public void setMAX_PATHS(int MAX_PATHS) {
        if (MAX_PATHS < 1) {
            throw new IllegalArgumentException("MAX_PATHS must be greater than 0");
        }
        this.MAX_PATHS = MAX_PATHS;
    }

    public void setMAX_PATH_EDGES(int MAX_PATH_EDGES) {
        if (MAX_PATH_EDGES < 1) {
            throw new IllegalArgumentException("MAX_PATH_EDGES must be greater than 0");
        }
        this.MAX_PATH_EDGES = MAX_PATH_EDGES;
    }

    // ------------------------------------------------------------------
    // PathFinder entry point
    // ------------------------------------------------------------------

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(ControlFlowGraph cfg, int target) {
        return findPath(cfg, target, null);
    }

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(
            ControlFlowGraph cfg,
            int target,
            core.cfg.CfgEdgeKind requiredExit) {

        int entryId = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exitId  = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entryId == -1 || exitId == -1) {
            return Collections.emptyList();
        }

        // Phase 1 — single BFS path: target → EXIT
        List<ControlFlowGraph.Edge> targetToExit = requiredExit == null
                ? bfsPath(cfg, target, exitId)
                : pathViaRequiredExit(cfg, target, exitId, requiredExit);
        if (targetToExit == null) {
            // A coverage constraint only needs the prefix through the requested
            // branch outcome. Some partially modelled constructs (currently
            // switch statements) can hide a real return from the CFG, making
            // EXIT appear unreachable even though the concrete method exits.
            // Preserve the required branch edge so symbolic execution can still
            // solve an input for that outcome.
            targetToExit = requiredExit == null
                    ? null
                    : requiredExitOnly(cfg, target, requiredExit);
            if (targetToExit == null) return Collections.emptyList();
        }

        // Phase 2 — all DFS paths: ENTRY → target  (explosion-safe)
        DfsState state = new DfsState();
        boolean targetIsCyclic = isOnCycle(cfg, target);
        dfs(cfg, entryId, target, targetIsCyclic,
                new ArrayDeque<>(), new HashMap<>(), state);

        // Phase 3 — stitch prefix + suffix
        List<List<ControlFlowGraph.Edge>> results = new ArrayList<>(state.paths.size());
        for (List<ControlFlowGraph.Edge> prefix : state.paths) {
            List<ControlFlowGraph.Edge> full = new ArrayList<>(prefix.size() + targetToExit.size());
            full.addAll(prefix);
            full.addAll(targetToExit);
            results.add(Collections.unmodifiableList(full));
        }

        results.sort(Comparator.comparingInt(List::size));
        return Collections.unmodifiableList(results);
    }

    @Override
    public List<List<ControlFlowGraph.Edge>> findAlternativePaths(
            ControlFlowGraph cfg,
            int target,
            core.cfg.CfgEdgeKind requiredExit) {
        int entryId = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exitId = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entryId == -1 || exitId == -1) return Collections.emptyList();

        DfsState prefixes = new DfsState();
        boolean targetIsCyclic = isOnCycle(cfg, target);
        dfs(cfg, entryId, target, targetIsCyclic,
                new ArrayDeque<>(), new HashMap<>(), prefixes);

        List<List<ControlFlowGraph.Edge>> results = new ArrayList<>();
        Set<List<ControlFlowGraph.Edge>> seen = new HashSet<>();
        for (List<ControlFlowGraph.Edge> prefix : prefixes.paths) {
            Map<Integer, Integer> visits = visitCounts(prefix);
            ArrayDeque<ControlFlowGraph.Edge> path = new ArrayDeque<>(prefix);
            dfsToExit(cfg, target, target, exitId, requiredExit, false,
                    path, visits, results, seen);
            if (results.size() >= MAX_PATHS) break;
        }
        results.sort(Comparator.comparingInt(List::size));
        return Collections.unmodifiableList(results);
    }

    private Map<Integer, Integer> visitCounts(List<ControlFlowGraph.Edge> path) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (path.isEmpty()) return counts;
        counts.merge(path.get(0).getFrom(), 1, Integer::sum);
        for (ControlFlowGraph.Edge edge : path) {
            counts.merge(edge.getTo(), 1, Integer::sum);
        }
        return counts;
    }

    private void dfsToExit(
            ControlFlowGraph cfg,
            int current,
            int target,
            int exit,
            core.cfg.CfgEdgeKind requiredExit,
            boolean requiredExitTaken,
            Deque<ControlFlowGraph.Edge> path,
            Map<Integer, Integer> visits,
            List<List<ControlFlowGraph.Edge>> results,
            Set<List<ControlFlowGraph.Edge>> seen) {
        if (results.size() >= MAX_PATHS || path.size() >= MAX_PATH_EDGES) return;

        for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
            boolean exitTaken = requiredExitTaken
                    || (current == target
                    && (requiredExit == null || edge.getKind() == requiredExit));
            int next = edge.getTo();
            int nextVisits = visits.getOrDefault(next, 0);
            if (next != exit && nextVisits >= MAX_NODE_VISITS) continue;

            path.addLast(edge);
            if (next == exit) {
                if (requiredExit == null || exitTaken) {
                    List<ControlFlowGraph.Edge> candidate = List.copyOf(path);
                    if (seen.add(candidate)) results.add(candidate);
                }
            } else {
                visits.put(next, nextVisits + 1);
                dfsToExit(cfg, next, target, exit, requiredExit, exitTaken,
                        path, visits, results, seen);
                if (nextVisits == 0) visits.remove(next);
                else visits.put(next, nextVisits);
            }
            path.removeLast();
        }
    }

    private List<ControlFlowGraph.Edge> pathViaRequiredExit(
            ControlFlowGraph cfg,
            int target,
            int exitId,
            core.cfg.CfgEdgeKind requiredExit) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(target)) {
            if (edge.getKind() == requiredExit) {
                List<ControlFlowGraph.Edge> rest = bfsPath(cfg, edge.getTo(), exitId);
                if (rest == null) continue;
                List<ControlFlowGraph.Edge> path = new ArrayList<>(rest.size() + 1);
                path.add(edge);
                path.addAll(rest);
                return path;
            }
        }
        return null;
    }

    private List<ControlFlowGraph.Edge> requiredExitOnly(
            ControlFlowGraph cfg,
            int target,
            core.cfg.CfgEdgeKind requiredExit) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(target)) {
            if (edge.getKind() == requiredExit) return List.of(edge);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Mutable DFS bookkeeping (passed by reference to avoid extra params)
    // ------------------------------------------------------------------

    private static final class DfsState {
        final List<List<ControlFlowGraph.Edge>> paths    = new ArrayList<>();
        final Set<List<ControlFlowGraph.Edge>>  seen     = new HashSet<>();
        boolean limitReached = false; // set when MAX_PATHS is hit
    }

    // ------------------------------------------------------------------
    // Phase 2 — DFS: ENTRY → target, all paths
    // ------------------------------------------------------------------

    private void dfs(
            ControlFlowGraph cfg,
            int current,
            int target,
            boolean targetIsCyclic,
            Deque<ControlFlowGraph.Edge> pathEdges,
            Map<Integer, Integer> visitCount,
            DfsState state) {

        // Guard: path-explosion limit already hit — abort entire DFS subtree
        if (state.limitReached) return;

        // Guard: path length exceeded — prune this branch only
        if (pathEdges.size() > MAX_PATH_EDGES) return;

        // Guard: node visit limit (loop unrolling)
        int visits = visitCount.getOrDefault(current, 0);
        if (visits >= MAX_NODE_VISITS) return;
        visitCount.put(current, visits + 1);

        if (current == target) {
            // Record this ENTRY→target path (deduplicated)
            List<ControlFlowGraph.Edge> snapshot = new ArrayList<>(pathEdges);
            if (state.seen.add(snapshot)) {          // add() returns false if already present
                state.paths.add(snapshot);
                if (state.paths.size() >= MAX_PATHS) {
                    state.limitReached = true;
                }
            }
            // An acyclic target cannot be reached again. Traversing its suffix
            // would enumerate every later branch without producing another
            // prefix, which is exponential wasted work for long if-chains.
            if (!targetIsCyclic) {
                restoreVisitCount(visitCount, current);
                return;
            }
        }

        // Explore outgoing edges regardless of whether current == target,
        // so loops containing the target node are unrolled correctly.
        if (!state.limitReached) {
            for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
                pathEdges.addLast(edge);
                dfs(cfg, edge.getTo(), target, targetIsCyclic,
                        pathEdges, visitCount, state);
                pathEdges.removeLast();
            }
        }

        // Backtrack: restore visit count for this node
        restoreVisitCount(visitCount, current);
    }

    private void restoreVisitCount(Map<Integer, Integer> visitCount, int node) {
        int after = visitCount.get(node) - 1;
        if (after == 0) visitCount.remove(node);
        else            visitCount.put(node, after);
    }

    /** Returns true when a non-empty path leads from {@code node} back to itself. */
    private boolean isOnCycle(ControlFlowGraph cfg, int node) {
        Deque<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        for (ControlFlowGraph.Edge edge : cfg.outgoing(node)) {
            if (edge.getTo() == node) return true;
            if (visited.add(edge.getTo())) queue.addLast(edge.getTo());
        }

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
                if (edge.getTo() == node) return true;
                if (visited.add(edge.getTo())) queue.addLast(edge.getTo());
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Phase 1 — BFS: shortest path target → EXIT
    // ------------------------------------------------------------------

    private List<ControlFlowGraph.Edge> bfsPath(ControlFlowGraph cfg, int start, int exitId) {
        if (start == exitId) {
            return Collections.emptyList();
        }

        // Parallel queues keep path-so-far aligned with the frontier node
        Queue<List<ControlFlowGraph.Edge>> pathQueue = new ArrayDeque<>();
        Queue<Integer>                     nodeQueue = new ArrayDeque<>();
        Set<Integer>                       visited   = new HashSet<>();

        pathQueue.add(new ArrayList<>());
        nodeQueue.add(start);
        visited.add(start);

        while (!pathQueue.isEmpty()) {
            List<ControlFlowGraph.Edge> currentPath = pathQueue.poll();
            int currentNode = nodeQueue.poll();

            for (ControlFlowGraph.Edge edge : cfg.outgoing(currentNode)) {
                int next = edge.getTo();

                List<ControlFlowGraph.Edge> nextPath = new ArrayList<>(currentPath);
                nextPath.add(edge);

                if (next == exitId) {
                    return nextPath;
                }

                if (visited.add(next)) {   // add() returns false if already present
                    pathQueue.add(nextPath);
                    nodeQueue.add(next);
                }
            }
        }

        return null; // EXIT unreachable
    }

    // ------------------------------------------------------------------
    // Helper — locate a unique node by CfgNodeKind
    // ------------------------------------------------------------------

    private int findNodeByKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int id : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getKind() == kind) {
                return id;
            }
        }
        return -1;
    }
}
