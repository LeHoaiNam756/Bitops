package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;

import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.simplifier.DeadConstraintStrategy;
import core.SymbolicExecution.simplifier.SymValueFactory;

import java.util.List;
import java.util.Map;

/**
 * Outermost facade for the symbolic execution constraint pipeline.
 *
 * Full pipeline per check() call:
 *
 *   List&lt;SymbolicValue&gt; pathCondition
 *          │
 *          ▼  DeadConstraintStrategy.INSTANCE.scan()
 *   Dead-constraint scan  (tautologies dropped, contradiction → free UNSAT)
 *          │
 *          ▼  Z3Encoder.encodeAll()
 *   List&lt;BoolExpr&gt;  (shared sub-expressions encoded once via IdentityHashMap)
 *          │
 *          ▼  z3Solver.add() + z3Solver.check()
 *   Z3 Status (SATISFIABLE | UNSATISFIABLE | UNKNOWN)
 *          │
 *      ┌───┴──────────────────┐
 *    SAT                    UNSAT / UNKNOWN
 *      │                      │
 *      ▼                      ▼
 *  ModelExtractor         SolverResult.unsat / .unknown
 *  → Z3ModelBindings
 *  → SolverResult.sat(bindings)
 *         │
 *         └──► ModelBasedSimplifier (inter-query, called by the analysis engine,
 *                                   not here — ConstraintSolver just returns bindings)
 *
 * Z3 Context lifecycle:
 * One Z3 Context per ConstraintSolver instance.  The Context is NOT thread-safe;
 * create one ConstraintSolver per analysis thread (or per top-level method).
 * Call {@link #close()} when done to release native Z3 memory.
 *
 *Incremental solving:
 * Uses Z3's push()/pop() stack so the solver can be reused across branches
 * without rebuilding from scratch:
 *   - {@link #push()}  checkpoint before exploring a branch
 *   - {@link #pop()}   restore when backtracking
 *   - {@link #check(List)} adds constraints at current stack level
 *
 * Usage:
 * <pre>
 *   try (ConstraintSolver solver = ConstraintSolver.create(typeMap)) {
 *
 *       solver.push();
 *       SolverResult result = solver.check(branchConstraints);
 *       switch (result) {
 *           case SolverResult.Sat    sat  -> explore(sat.model());
 *           case SolverResult.Unsat  u    -> prune();
 *           case SolverResult.Unknown unk -> recordTimeout(unk.reason());
 *       }
 *       solver.pop();
 *   }
 * </pre>
 */
public final class ConstraintSolver implements AutoCloseable {

    // =========================================================================
    // Z3 infrastructure
    // =========================================================================

    private final Context          ctx;
    private final Solver           z3Solver;
    private final SortResolver sortResolver;
    private final Z3Encoder encoder;
    private final ModelExtractor extractor;
    private final LegacySortResolver legacySortResolver;
    private final LegacyZ3Encoder legacyEncoder;
    private final LegacyModelExtractor legacyExtractor;
    private final Z3EncodingMode encodingMode;

    // =========================================================================
    // Construction
    // =========================================================================

    /**
     * @param varTypes  map of variable name → SymType, built from TypeContext
     *                  at the analysis callsite
     * @param factory   shared SymValueFactory (intern table for the analysis session)
     */
    public ConstraintSolver(Map<String, SymType> varTypes, SymValueFactory factory) {
        this(varTypes, factory, Z3EncodingMode.BITVECTOR);
    }

    public ConstraintSolver(Map<String, SymType> varTypes,
                            SymValueFactory factory,
                            Z3EncodingMode encodingMode) {
        // One Z3 Context per solver instance (not thread-safe; one per thread)
        this.ctx = new Context();
        this.encodingMode = encodingMode == null ? Z3EncodingMode.BITVECTOR : encodingMode;

        // Z3 solver with incremental push/pop support
        this.z3Solver = ctx.mkSolver();

        // Set a default timeout (5 seconds) to prevent runaway queries
        Params params = ctx.mkParams();
        params.add("timeout", 5_000);  // milliseconds
        z3Solver.setParameters(params);

        // Build both pipelines from the caller's SymType map. Only one is used
        // per solver instance, but keeping both initialized preserves the
        // existing constructor shape and keeps mode selection localized here.
        this.sortResolver = new SortResolver(ctx, buildSortMap(varTypes), varTypes);
        this.encoder      = new Z3Encoder(sortResolver);
        this.extractor    = new ModelExtractor(sortResolver);
        this.legacySortResolver = new LegacySortResolver(ctx, varTypes);
        this.legacyEncoder      = new LegacyZ3Encoder(legacySortResolver);
        this.legacyExtractor    = new LegacyModelExtractor(legacySortResolver);
    }

    /**
     * Convenience factory with global SymValueFactory.
     */
    public static ConstraintSolver create(Map<String, SymType> varTypes) {
        return new ConstraintSolver(varTypes, SymValueFactory.global());
    }

    public static ConstraintSolver create(Map<String, SymType> varTypes,
                                          Z3EncodingMode encodingMode) {
        return new ConstraintSolver(varTypes, SymValueFactory.global(), encodingMode);
    }

    // =========================================================================
    // Core API
    // =========================================================================

    /**
     * Check whether the given path condition is satisfiable.
     *
     * Constraints are added at the current stack level (use push/pop for
     * incremental solving across branches).
     *
     * @param pathCondition AND-list of boolean-typed symbolic constraints
     * @return SAT (+ model) | UNSAT (+ source) | UNKNOWN (+ reason)
     */
    public SolverResult check(List<SymbolicValue> pathCondition) {

        // ── Step 1: Static dead-constraint scan (no Z3) ───────────────────────
        DeadConstraintStrategy.ScanResult scan =
                DeadConstraintStrategy.INSTANCE.scan(pathCondition);

        if (!scan.feasible()) {
            // Structural contradiction found — skip Z3 entirely
            return SolverResult.unsat(SolverResult.UnsatSource.STATIC_DEAD_CONSTRAINT);
        }

        List<SymbolicValue> live = scan.liveConstraints();

        if (live.isEmpty()) {
            // All constraints were tautologies — trivially SAT with empty model
            return SolverResult.sat(Z3ModelBindings.empty());
        }

        // ── Step 2: Encode simplified constraints to Z3 BoolExprs ─────────────
        List<BoolExpr> z3Constraints;
        try {
            z3Constraints = encodingMode == Z3EncodingMode.LEGACY_INT_REAL
                    ? legacyEncoder.encodeAll(live)
                    : encoder.encodeAll(live);
            System.out.println(z3Constraints);
        } catch (EncodingException e) {
            // Encoding failure: report as UNKNOWN (conservative, not UNSAT)
            System.err.println("[ConstraintSolver] Encoding failed: " + e.getMessage());
            return SolverResult.unknown("encoding-failure: " + e.getMessage());
        }

        // ── Step 3: Push constraints into Z3 at the current stack level ───────
        z3Constraints.forEach(z3Solver::add);

        // ── Step 4: Invoke Z3 ─────────────────────────────────────────────────
        Status status = z3Solver.check();
        Statistics statistics = z3Solver.getStatistics();
        Z3StatisticsRecorder.record(statistics);
//        printZ3Statistics(statistics);

        return switch (status) {

            case SATISFIABLE -> {
                Model           model    = z3Solver.getModel();
                Z3ModelBindings bindings = encodingMode == Z3EncodingMode.LEGACY_INT_REAL
                        ? legacyExtractor.extract(model)
                        : extractor.extract(model);
                yield SolverResult.sat(bindings);
            }

            case UNSATISFIABLE ->
                SolverResult.unsat(SolverResult.UnsatSource.Z3_SOLVER);

            case UNKNOWN ->
                SolverResult.unknown(z3Solver.getReasonUnknown());
        };
    }

    // =========================================================================
    // Incremental stack
    // =========================================================================

    /**
     * Checkpoint the current solver state.
     * Call before entering a branch; pair with {@link #pop()} on backtrack.
     */
    public void push() { z3Solver.push(); }

    /**
     * Restore solver state to the last {@link #push()} checkpoint.
     */
    public void pop()  { z3Solver.pop();  }

    /**
     * Discard all constraints and reset the stack.
     * Use between top-level method analyses.
     */
    public void reset() { z3Solver.reset(); }

    // =========================================================================
    // Diagnostics
    // =========================================================================

    private void printZ3Statistics(Statistics statistics) {
        System.out.println("[ConstraintSolver] Z3 statistics:");
        Statistics.Entry[] entries = statistics.getEntries();
        for (int i = 0; i < entries.length; i++) {
            Statistics.Entry entry = entries[i];
            System.out.println("  [" + i + "] " + entry.Key + " = " + entry.getValueString());
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Release native Z3 resources.  Must be called when the analysis is done.
     * After close(), no further calls are valid.
     */
    @Override
    public void close() {
        ctx.close();
    }

    // =========================================================================
    // Sort map builder
    // =========================================================================

    /**
     * Convert the caller's {@code Map<String, SymType>} into a
     * {@code Map<String, Sort>} by calling {@link SortResolver#symTypeToSort}.
     *
     * This is the coupling point between TypeContext and Z3.
     * The sort map is built once per ConstraintSolver instance.
     */
    private Map<String, Sort> buildSortMap(Map<String, SymType> varTypes) {
        // Temporary resolver with empty varSorts — only used for symTypeToSort(),
        // which doesn't need the map (it only reads the pre-built sort constants).
        SortResolver bootstrap = new SortResolver(ctx, Map.of(),null);
        java.util.HashMap<String, Sort> sortMap = new java.util.HashMap<>();
        varTypes.forEach((name, symType) -> {
            try {
                sortMap.put(name, bootstrap.symTypeToSort(symType));
            } catch (IllegalArgumentException e) {
                // void / unsupported types: skip (they can't appear as constraints)
                System.err.println("[ConstraintSolver] Skipping variable '" + name
                        + "' with unsupported type: " + symType);
            }
        });
        return sortMap;
    }
}
