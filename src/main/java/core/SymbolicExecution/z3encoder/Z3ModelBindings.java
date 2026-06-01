package core.SymbolicExecution.z3encoder;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.simplifier.ModelBasedSimplifier;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable snapshot of the concrete variable assignments produced by Z3
 * after a SAT result.
 *
 * ── Where does this come from? ───────────────────────────────────────────────
 * When Z3 returns SAT, its {@code Model} object maps each declared symbolic
 * variable to a concrete value.  The {@link ModelExtractor} (Z3-layer class)
 * reads that model and produces a {@code Z3ModelBindings} instance.
 *
 * ── What is it used for? ─────────────────────────────────────────────────────
 * {@link ModelBasedSimplifier} consumes these bindings to substitute concrete
 * values into sibling-branch constraints, turning symbolic expressions into
 * literals that constant-folding can collapse.
 *
 * Example:
 *   Z3 found: x → 42, flag → false
 *   Constraint on sibling branch: x * 2 + flag
 *   After substitution: 42 * 2 + false  →  folded to Lit(84)
 *
 * ── Design notes ─────────────────────────────────────────────────────────────
 * Kept deliberately thin: just a Map wrapper.  The extraction logic lives in
 * ModelExtractor (Z3-specific) so this class has zero Z3 dependency and is
 * independently testable.
 */
public final class Z3ModelBindings {

    /** Maps SymVariable.name() → concrete SymLiteral from the Z3 model. */
    private final Map<String, SymLiteral> bindings;

    public Z3ModelBindings(Map<String, SymLiteral> bindings) {
        this.bindings = Map.copyOf(bindings);  // defensive immutable copy
    }

    public static Z3ModelBindings empty() {
        return new Z3ModelBindings(Map.of());
    }

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Return the concrete literal assigned to {@code variable} by Z3,
     * or empty if Z3 did not assign a value (unconstrained variable).
     */
    public Optional<SymLiteral> lookup(SymVariable variable) {
        return Optional.ofNullable(bindings.get(variable.name()));
    }

    /**
     * Convenience: look up by raw name (used by ModelSubstitutor).
     */
    public Optional<SymLiteral> lookup(String name) {
        return Optional.ofNullable(bindings.get(name));
    }

    /** All variable names that Z3 assigned concrete values to. */
    public Set<String> boundNames() {
        return Collections.unmodifiableSet(bindings.keySet());
    }

    /** Number of bindings. */
    public int size() { return bindings.size(); }

    public boolean isEmpty() { return bindings.isEmpty(); }

    @Override
    public String toString() {
        return "Z3ModelBindings" + bindings;
    }
}