package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;

import java.util.List;

/**
 * Inter-query optimisation: Model-Based Simplification.
 *
 * ── Problem it solves ────────────────────────────────────────────────────────
 * When Z3 returns SAT for a path it produces a concrete model — an assignment
 * of values to every symbolic variable (e.g. x=42, flag=false).  On its own
 * that model is used to generate a test case or a bug report.
 *
 * But the analysis still has sibling branches to explore that share many of
 * the same variables.  Without feedback, those sibling constraints are
 * submitted to Z3 as fully symbolic expressions.  With feedback, we can
 * substitute the model's concrete values into the sibling constraints
 * *before* calling Z3, collapsing large parts of the formula to literals
 * that constant-folding eliminates entirely.
 *
 * ── Pipeline ─────────────────────────────────────────────────────────────────
 *
 *   Z3 SAT result
 *        │
 *        ▼
 *   Z3ModelBindings  { x→42, flag→false, … }
 *        │
 *        ▼  ModelSubstitutor.substituteAll()
 *   Partially concrete constraint list
 *        │
 *        ▼  SymbolicSimplifier.simplifyAll()
 *   Constant-folded / identity-eliminated constraints
 *        │
 *        ▼  DeadConstraintStrategy.scan()
 *   Feasibility check (may short-circuit as UNSAT without Z3)
 *        │
 *        ▼
 *   PropagationResult
 *        ├── feasible = false  → skip Z3, mark branch UNSAT
 *        └── liveConstraints  → submit to Z3 (smaller formula)
 *
 * ── Soundness note ───────────────────────────────────────────────────────────
 * Substituting a SAT model into sibling branch constraints is NOT sound for
 * proving UNSAT — a sibling branch may be SAT with different variable values.
 * This is used purely as an optimistic pre-simplification: the resulting
 * liveConstraints are still submitted to Z3 for final judgment.
 * The only case where we skip Z3 is when the static dead-constraint scan
 * detects a structural contradiction (always UNSAT regardless of values).
 *
 * ── Usage ────────────────────────────────────────────────────────────────────
 * <pre>
 *   // After Z3 returns SAT on branch A:
 *   Z3ModelBindings model = modelExtractor.extract(z3Solver.getModel());
 *   ModelBasedSimplifier mbs = new ModelBasedSimplifier(factory, simplifier);
 *
 *   // For each sibling branch B:
 *   PropagationResult result = mbs.propagate(siblingConstraints, model);
 *   if (!result.feasible()) {
 *       // skip branch B — statically UNSAT
 *   } else {
 *       // submit result.liveConstraints() to Z3 (smaller / cheaper query)
 *       z3Solver.check(result.liveConstraints());
 *   }
 * </pre>
 */
public final class ModelBasedSimplifier {

    private final ModelSubstitutor  substitutor;
    private final SymbolicSimplifier simplifier;

    // =========================================================================
    // Construction
    // =========================================================================

    public ModelBasedSimplifier(SymValueFactory factory, SymbolicSimplifier simplifier) {
        this.substitutor = new ModelSubstitutor(factory);
        this.simplifier  = simplifier;
    }

    /** Convenience: build from factory alone (uses default simplifier pipeline). */
    public static ModelBasedSimplifier create(SymValueFactory factory) {
        return new ModelBasedSimplifier(factory,
                SymbolicSimplifier.withDefaultPipeline(factory));
    }

    // =========================================================================
    // Core operation
    // =========================================================================

    /**
     * Propagate a Z3 SAT model into a sibling branch's path condition.
     *
     * Steps:
     *   1. Substitute model bindings into every constraint (x → 42, etc.)
     *   2. Run the full simplification pipeline (constant-fold, identity, …)
     *   3. Scan for dead constraints (tautologies dropped, contradictions → UNSAT)
     *
     * @param siblingConstraints the path condition of a sibling branch (AND-list)
     * @param model              bindings extracted from Z3's SAT model
     * @return a PropagationResult the caller uses to decide whether to call Z3
     */
    public PropagationResult propagate(List<SymbolicValue> siblingConstraints,
                                       Z3ModelBindings model) {

        // Fast path: empty model — no substitution possible
        if (model.isEmpty()) {
            DeadConstraintStrategy.ScanResult scan = simplifier.scanPathCondition(siblingConstraints);
            return new PropagationResult(scan.feasible(), scan.liveConstraints(), model);
        }

        // Step 1: Substitute concrete values into the sibling constraints
        List<SymbolicValue> substituted = substitutor.substituteAll(siblingConstraints, model);

        // Step 2: Simplify (constant-fold the now-partially-concrete expressions)
        //         simplifyAll uses one shared memo across all constraints for
        //         maximum sharing — common sub-expressions simplified once.
        List<SymbolicValue> simplified = simplifier.simplifyAll(substituted);

        // Step 3: Dead-constraint scan on the simplified list
        DeadConstraintStrategy.ScanResult scan =
                DeadConstraintStrategy.INSTANCE.scan(simplified);

        return new PropagationResult(scan.feasible(), scan.liveConstraints(), model);
    }

    /**
     * Convenience overload: propagate to multiple sibling branches at once,
     * returning one PropagationResult per branch.
     *
     * Each branch gets its own substitution + simplification pass, but they
     * share the same SymValueFactory intern table, so cross-branch common
     * sub-expressions are deduplicated in memory.
     *
     * @param siblingBranches list of per-branch path conditions
     * @param model           bindings from Z3's SAT model
     * @return per-branch PropagationResult, same order as input
     */
    public List<PropagationResult> propagateAll(List<List<SymbolicValue>> siblingBranches,
                                                Z3ModelBindings model) {
        return siblingBranches.stream()
                .map(branch -> propagate(branch, model))
                .toList();
    }

    // =========================================================================
    // Result type
    // =========================================================================

    /**
     * Outcome of model-based propagation for one sibling branch.
     *
     * @param feasible        true if no static contradiction was found;
     *                        false means the branch is dead — skip Z3
     * @param liveConstraints the simplified, tautology-pruned constraints
     *                        to submit to Z3 (empty when !feasible)
     * @param appliedModel    the model that was used — for audit / debugging
     */
    public record PropagationResult(
            boolean feasible,
            List<SymbolicValue> liveConstraints,
            Z3ModelBindings appliedModel
    ) {
        /**
         * How many constraints were eliminated by model substitution +
         * simplification (tautologies + newly-folded constraints).
         * Useful for telemetry.
         *
         * @param originalSize the size of the input constraint list
         */
        public int eliminated(int originalSize) {
            return originalSize - liveConstraints.size();
        }
    }
}