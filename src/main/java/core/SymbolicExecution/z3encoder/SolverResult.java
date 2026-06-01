package core.SymbolicExecution.z3encoder;

/**
 * Sealed result type returned by {@link ConstraintSolver#check}.
 *
 * Three variants:
 *   {@link Sat}     – path is feasible; carries a concrete model
 *   {@link Unsat}   – path is infeasible (dead branch)
 *   {@link Unknown} – Z3 timed out or hit a resource limit
 *
 * Usage:
 * <pre>
 *   switch (solver.check(pathConstraints)) {
 *       case SolverResult.Sat    sat     -> handleSat(sat.model());
 *       case SolverResult.Unsat  ignored -> pruneBranch();
 *       case SolverResult.Unknown ignored -> recordTimeout();
 *   }
 * </pre>
 */
public sealed interface SolverResult
        permits SolverResult.Sat, SolverResult.Unsat, SolverResult.Unknown {

    // =========================================================================
    // Variants
    // =========================================================================

    /**
     * SAT: the path condition is satisfiable.
     *
     * @param model concrete variable assignments from Z3;
     *              fed to {@link core.SymbolicExecution.simplifier.ModelBasedSimplifier}
     *              to simplify sibling branches
     */
    record Sat(Z3ModelBindings model) implements SolverResult {}

    /**
     * UNSAT: the path condition is unsatisfiable — the branch is unreachable.
     *
     * @param source how we determined UNSAT
     */
    record Unsat(UnsatSource source) implements SolverResult {}

    /**
     * UNKNOWN: Z3 could not determine satisfiability within constraints
     * (timeout, memory limit, or incompleteness).
     *
     * @param reason Z3's reason-unknown string
     */
    record Unknown(String reason) implements SolverResult {}

    // =========================================================================
    // UnsatSource – distinguishes free UNSAT from Z3-confirmed UNSAT
    // =========================================================================

    /**
     * Indicates how UNSAT was determined.
     * Useful for telemetry: how often do we avoid Z3?
     */
    enum UnsatSource {
        /**
         * Detected by {@link core.SymbolicExecution.simplifier.DeadConstraintStrategy}
         * before Z3 was invoked — a structural contradiction in the path condition
         * (e.g., {@code false} literal, or {@code x != x}).
         * Z3 was never called.
         */
        STATIC_DEAD_CONSTRAINT,

        /**
         * Confirmed by Z3's SMT solver after encoding.
         */
        Z3_SOLVER
    }

    // =========================================================================
    // Convenience predicates
    // =========================================================================

    default boolean isSat()     { return this instanceof Sat;     }
    default boolean isUnsat()   { return this instanceof Unsat;   }
    default boolean isUnknown() { return this instanceof Unknown;  }

    // =========================================================================
    // Static constructors (reduce verbosity at call sites)
    // =========================================================================

    static SolverResult sat(Z3ModelBindings model) {
        return new Sat(model);
    }

    static SolverResult unsat(UnsatSource source) {
        return new Unsat(source);
    }

    static SolverResult unknown(String reason) {
        return new Unknown(reason);
    }
}