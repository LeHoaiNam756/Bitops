package core.testpath;

import core.cfg.ControlFlowGraph;

import java.util.*;

/**
 * Wraps {@link AllPathsFinder} and ranks results by a coverage-aware score:
 *
 * <pre>
 *   score = (covered nodes on path) - SKIP_PENALTY × (skipped nodes on path)
 * </pre>
 *
 * The path with the highest score is returned first.  Ties are broken by
 * shorter path length (fewer edges) so that two equally-scoring paths prefer
 * the simpler one.
 *
 * <h3>Scoring rationale</h3>
 * <ul>
 *   <li>A <b>covered</b> node on the path means the test exercises known-good
 *       ground — valuable for regression confidence.</li>
 *   <li>A <b>skipped</b> node means the path deliberately avoids unreachable
 *       or irrelevant code — having many skipped nodes on a path is a sign the
 *       path is "going the wrong way".</li>
 *   <li><b>Uncovered</b> nodes are neutral: they are the normal intermediate
 *       nodes every path must pass through.</li>
 * </ul>
 */
public class IntelligentPathFinder implements PathFinder {

    /**
     * How much each skipped node subtracts from the score.
     * A value of 1 means skipped nodes cancel out covered nodes 1-for-1.
     * Increase to make the finder avoid skipped nodes more aggressively.
     */
    private static final int SKIP_PENALTY = 2;

    private final AllPathsFinder delegate = new AllPathsFinder();

    // ------------------------------------------------------------------
    // PathFinder entry point
    // ------------------------------------------------------------------

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(ControlFlowGraph cfg, int target) {
        throw new UnsupportedOperationException(
            "Use findPath(cfg, target, tracker) for coverage-aware ranking.");
    }

    /**
     * Returns all ENTRY→target→EXIT paths sorted by coverage score (best first).
     *
     * @param cfg     the control-flow graph
     * @param target  destination node id
     * @param tracker current coverage state
     * @return ranked paths, best score first; empty if target is unreachable
     */
    public List<List<ControlFlowGraph.Edge>> findPath(
            ControlFlowGraph cfg,
            int target,
            CoverageTracker tracker) {

        // 1. Collect all candidate paths from the delegate
        List<List<ControlFlowGraph.Edge>> allPaths = delegate.findPath(cfg, target);
        if (allPaths.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Score every path
        List<ScoredPath> scored = new ArrayList<>(allPaths.size());
        for (List<ControlFlowGraph.Edge> path : allPaths) {
            scored.add(new ScoredPath(path, score(path, cfg, tracker)));
        }

        // 3. Sort: highest score first; ties broken by shorter path
        scored.sort(Comparator
            .comparingInt(ScoredPath::score).reversed()
            .thenComparingInt(sp -> sp.path().size()));

        // 4. Unwrap back to plain edge lists
        List<List<ControlFlowGraph.Edge>> results = new ArrayList<>(scored.size());
        for (ScoredPath sp : scored) {
            results.add(sp.path());
        }
        return Collections.unmodifiableList(results);
    }

    // ------------------------------------------------------------------
    // Scoring
    // ------------------------------------------------------------------

    /**
     * Walks every node touched by the path and computes:
     * <pre>  +1 per covered node,  -SKIP_PENALTY per skipped node  </pre>
     *
     * Both the FROM and TO node of each edge are considered, but each node
     * is counted only once per path (a node appearing twice in a loop is
     * still counted once for scoring purposes — its coverage value doesn't
     * double just because the loop unrolled it).
     */
    private int score(
            List<ControlFlowGraph.Edge> path,
            ControlFlowGraph cfg,
            CoverageTracker tracker) {

        Set<Integer> seen = new HashSet<>();
        int s = 0;

        for (ControlFlowGraph.Edge edge : path) {
            s += scoreNode(edge.getFrom(), cfg, tracker, seen);
            s += scoreNode(edge.getTo(),   cfg, tracker, seen);
        }
        return s;
    }

    private int scoreNode(
            int nodeId,
            ControlFlowGraph cfg,
            CoverageTracker tracker,
            Set<Integer> seen) {

        if (!seen.add(nodeId)) return 0; // already counted

        if (tracker.isCovered(nodeId)) return  1;
        if (tracker.isSkipped(nodeId)) return -SKIP_PENALTY;
        return 0; // uncovered — neutral
    }

    // ------------------------------------------------------------------
    // Value type — path + its score
    // ------------------------------------------------------------------

    private record ScoredPath(List<ControlFlowGraph.Edge> path, int score) {}
}