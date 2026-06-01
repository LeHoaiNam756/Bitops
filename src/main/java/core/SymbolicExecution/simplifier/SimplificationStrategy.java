package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.SymbolicValue;

/**
 * Strategy contract for a single simplification concern.
 *
 * Each implementation is stateless and pure: given a node whose children
 * have already been simplified (bottom-up), return either a simpler
 * SymbolicValue or the original node unchanged.
 *
 * Implementations are composed in {@link core.SymbolicExecution.simplifier.SymbolicSimplifier}
 * and applied in order:
 *   1. ConstantFoldingStrategy
 *   2. IdentityEliminationStrategy
 *   3. DeadConstraintStrategy
 *   4. NormalisationStrategy
 *
 * A strategy signals "I changed nothing" by returning the exact same
 * reference it received (identity check, not equals).
 */
public interface SimplificationStrategy {

    /**
     * Attempt to simplify {@code node}.
     *
     * @param node a SymbolicValue whose sub-tree has already been simplified
     * @return a simplified value, or {@code node} itself if this strategy
     *         cannot simplify further
     */
    SymbolicValue apply(SymbolicValue node);
}
