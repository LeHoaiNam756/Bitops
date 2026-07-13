package core.instrument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable result of {@link InstrumentationPlanner}: the complete set of
 * probe points for one file, indexed by CFG node ID.
 *
 * <h3>Relationship to {@code CoverageTracker}</h3>
 * <pre>{@code
 * InstrumentationPlan plan = planner.plan(cu, cfg, coverage);
 *
 * // Initialise tracker from the SAME node IDs — no translation needed
 * CoverageTracker tracker = new CoverageTracker(plan.nodeIds());
 *
 * // After execution, map a fired probe back to AST context
 * plan.pointFor(nodeId).ifPresent(p -> {
 *     ASTNode  ast  = p.astNode();
 *     TraceKind k   = p.kind();
 * });
 * }</pre>
 *
 * <p>For large batch runs the plan can be persisted via {@link PlanSerializer}
 * (only {@code cfgNodeId}, {@code kind}, and source position are written —
 * no AST object serialisation) and rehydrated later by re-parsing the same
 * {@link org.eclipse.jdt.core.dom.CompilationUnit}.
 */
public final class InstrumentationPlan {

    private final List<TracePoint>         points;
    private final Map<Integer, TracePoint> byNodeId;
    private final int                      statementCount;
    private final int                      branchCount;

    /**
     * Package-private: constructed only by {@link InstrumentationPlanner}.
     */
    InstrumentationPlan(List<TracePoint> points) {
        this.points    = List.copyOf(points);
        this.byNodeId  = points.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TracePoint::cfgNodeId,
                        tp -> tp,
                        // Statement and branch-outcome probe IDs can overlap
                        // when statement mode also records branch traces.
                        // Direct coverage tracking filters by TraceKind.
                        (a, b) -> a));
        this.statementCount = (int) points.stream()
                .filter(tp -> tp.kind() == TraceKind.NODE).count();
        this.branchCount = (int) points.stream()
                .filter(tp -> tp.kind() == TraceKind.COND_T || tp.kind() == TraceKind.COND_F)
                .count();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** All trace points in walk order. */
    public List<TracePoint> points() {
        return points;
    }

    /**
     * Look up the {@link TracePoint} for a given CFG node ID.
     * Returns empty if the node was marked "untrackable" by the planner.
     */
    public Optional<TracePoint> pointFor(int cfgNodeId) {
        return Optional.ofNullable(byNodeId.get(cfgNodeId));
    }

    /**
     * The set of CFG node IDs covered by this plan.
     * Pass directly to {@code CoverageTracker(Set<Integer>)} constructor.
     */
    public Set<Integer> nodeIds() {
        return byNodeId.keySet();
    }

    /** Number of {@link TraceKind#NODE} points (plain statements). */
    public int statementCount() {
        return statementCount;
    }

    /**
     * Number of branch probe points (each condition contributes two:
     * {@link TraceKind#COND_T} + {@link TraceKind#COND_F}).
     */
    public int branchCount() {
        return branchCount;
    }

    /** Total probe points in this plan. */
    public int size() {
        return points.size();
    }

    @Override
    public String toString() {
        return "InstrumentationPlan{statements=" + statementCount
                + ", branches=" + branchCount + "}";
    }
}
