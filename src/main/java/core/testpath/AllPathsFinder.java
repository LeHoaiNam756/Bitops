package core.testpath;

import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;

import java.util.*;

/**
 * Finds all complete paths:  ENTRY ──(all paths, cycle-limited)──► target ──(shortest)──► EXIT
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
    private static final int MAX_LOOP_ITERATIONS = 1;

    /** Maximum number of distinct ENTRY→target paths to collect before stopping. */
    private static final int MAX_PATHS = 256;

    /** Maximum number of edges allowed on a single path before pruning that branch. */
    private static final int MAX_PATH_EDGES = 512;

    // Derived: a node may appear on one path at most this many times.
    private static final int MAX_NODE_VISITS = MAX_LOOP_ITERATIONS + 1;

    // ------------------------------------------------------------------
    // PathFinder entry point
    // ------------------------------------------------------------------

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(ControlFlowGraph cfg, int target) {

        int entryId = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exitId  = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entryId == -1 || exitId == -1) {
            return Collections.emptyList();
        }

        // Phase 1 — single BFS path: target → EXIT
        List<ControlFlowGraph.Edge> targetToExit = bfsPath(cfg, target, exitId);
        if (targetToExit == null) {
            return Collections.emptyList(); // target can never reach EXIT
        }

        // Phase 2 — all DFS paths: ENTRY → target  (explosion-safe)
        DfsState state = new DfsState();
        dfs(cfg, entryId, target, new ArrayDeque<>(), new HashMap<>(), state);

        // Phase 3 — stitch prefix + suffix
        List<List<ControlFlowGraph.Edge>> results = new ArrayList<>(state.paths.size());
        for (List<ControlFlowGraph.Edge> prefix : state.paths) {
            List<ControlFlowGraph.Edge> full = new ArrayList<>(prefix.size() + targetToExit.size());
            full.addAll(prefix);
            full.addAll(targetToExit);
            results.add(Collections.unmodifiableList(full));
        }
        return Collections.unmodifiableList(results);
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
            // ↓ IMPORTANT: do NOT return — keep exploring outgoing edges so that
            // paths that pass *through* target (e.g. target is inside a loop and
            // will be visited again after another iteration) are also discovered.
        }

        // Explore outgoing edges regardless of whether current == target,
        // so loops containing the target node are unrolled correctly.
        if (!state.limitReached) {
            for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
                pathEdges.addLast(edge);
                dfs(cfg, edge.getTo(), target, pathEdges, visitCount, state);
                pathEdges.removeLast();
            }
        }

        // Backtrack: restore visit count for this node
        int after = visitCount.get(current) - 1;
        if (after == 0) visitCount.remove(current);
        else            visitCount.put(current, after);
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
