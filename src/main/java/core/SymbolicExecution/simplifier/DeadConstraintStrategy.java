package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Strategy 3 – Dead Constraint Elimination
 *
 * Scans a path-condition (a list of atomic constraints) and removes or
 * short-circuits constraints that are statically decidable, avoiding
 * unnecessary Z3 calls.
 *
 * Two outcomes per atomic constraint:
 *
 *   TAUTOLOGY  (always true)  → drop from the path condition silently.
 *              Examples: x == x,  x + 1 > x,  true,  x >= x
 *
 *   CONTRADICTION (always false) → the whole path is infeasible.
 *              Examples: false,  x != x,  x < x,  x + 1 <= x
 *              Callers should treat PathFeasibility.INFEASIBLE as UNSAT
 *              without ever querying Z3.
 *
 * This class is also usable as a {@link SimplificationStrategy} for single
 * nodes: a tautology node becomes Lit(true); a contradiction becomes Lit(false).
 *
 * Thread-safe (stateless).
 */
public final class DeadConstraintStrategy implements SimplificationStrategy {

    public static final DeadConstraintStrategy INSTANCE = new DeadConstraintStrategy();

    private DeadConstraintStrategy() {}

    @Override
    public SymbolicValue apply(SymbolicValue node) {
        Tristate t = classify(node);
        return switch (t) {
            case TRUE  -> SymLiteral.of(true);
            case FALSE -> SymLiteral.of(false);
            case UNKNOWN -> node;
        };
    }

    /**
     * Result of scanning a full path condition.
     *
     * @param feasible     false when a contradicting constraint was found
     * @param liveConstraints constraints that survive after tautologies are dropped
     */
    public record ScanResult(boolean feasible, List<SymbolicValue> liveConstraints) {
        /** Convenience: UNSAT – the path is dead. */
        static ScanResult infeasible() {
            return new ScanResult(false, List.of());
        }
    }

    /**
     * Scan a list of conjunctive path-condition constraints and eliminate
     * those that are statically trivial.
     *
     * @param pathCondition the current accumulated constraints (AND-list)
     * @return a ScanResult; if {@code !result.feasible()} the path is dead
     */
    public ScanResult scan(List<SymbolicValue> pathCondition) {
        List<SymbolicValue> live = new ArrayList<>(pathCondition.size());

        for (SymbolicValue constraint : pathCondition) {
            Tristate t = classify(constraint);
            switch (t) {
                case TRUE    -> { /* tautology – silently drop */ }
                case FALSE   -> { return ScanResult.infeasible(); }   // path is dead
                case UNKNOWN -> live.add(constraint);
            }
        }

        return new ScanResult(true, List.copyOf(live));
    }

    /** Three-valued logic result. */
    public enum Tristate { TRUE, FALSE, UNKNOWN }

    /**
     * Attempt to classify a single boolean-typed SymbolicValue without Z3.
     *
     * Returns TRUE / FALSE when the answer is certain;
     * UNKNOWN when Z3 or further analysis is required.
     */
    public Tristate classify(SymbolicValue node) {

    // ── Literal booleans ────────────────────────────────────────────
       if (node instanceof SymLiteral lit &&
            lit.value() instanceof Boolean b) {
            return b ? Tristate.TRUE : Tristate.FALSE;
        }

        if (node instanceof SymUnaryOp u && u.op() == SymUnaryOp.Op.NOT) {
            return switch (classify(u.operand())) {
                case TRUE -> Tristate.FALSE;
                case FALSE -> Tristate.TRUE;
                case UNKNOWN -> Tristate.UNKNOWN;
            };
        }

        if (node instanceof SymBinaryOp b && b.op() == SymBinaryOp.Op.AND) {
            Tristate l = classify(b.left());
            Tristate r = classify(b.right());
            if (l == Tristate.FALSE || r == Tristate.FALSE) {
                return Tristate.FALSE;
            }

            if (l == Tristate.TRUE && r == Tristate.TRUE) {
                return Tristate.TRUE;
            }

            return Tristate.UNKNOWN;
        }

        if (node instanceof SymBinaryOp b && b.op() == SymBinaryOp.Op.OR) {
            Tristate l = classify(b.left());
            Tristate r = classify(b.right());

            if (l == Tristate.TRUE || r == Tristate.TRUE) {
                return Tristate.TRUE;
            }

            if (l == Tristate.FALSE && r == Tristate.FALSE) {
                return Tristate.FALSE;
            }
            return Tristate.UNKNOWN;
        }

        if (node instanceof SymBinaryOp b &&
            b.left().equals(b.right())) {
            return switch (b.op()) {
                case EQ, SGE, SLE, UGE, ULE -> Tristate.TRUE;
                case NEQ, SGT, SLT, UGT, ULT -> Tristate.FALSE;
                default -> Tristate.UNKNOWN;
            };
    }

        if (node instanceof SymBinaryOp b && isOffsetComparison(b)) {
            return classifyOffsetComparison(b);
        }

        if (node instanceof SymITE ite) {
            return switch (classify(ite.cond())) {
                case TRUE -> classify(ite.thenBranch());
                case FALSE -> classify(ite.elseBranch());
                case UNKNOWN -> Tristate.UNKNOWN;
            };
        }
        return Tristate.UNKNOWN;
    }

    private static boolean isOffsetComparison(SymBinaryOp b) {
        if (!isComparisonOp(b.op())) return false;
        return extractOffset(b.left(), b.right()) != null
            || extractOffset(b.right(), b.left()) != null;
    }


    private static Long extractOffset(SymbolicValue expr, SymbolicValue base) {
        if (!(expr instanceof SymBinaryOp bin)) return null;
        if (bin.op() != SymBinaryOp.Op.ADD && bin.op() != SymBinaryOp.Op.SUB) return null;

        if (bin.left().equals(base) && bin.right() instanceof SymLiteral lit && isIntegral(lit)) {
            long k = ConstantFoldingStrategy.toLong(lit.value());
            return bin.op() == SymBinaryOp.Op.SUB ? -k : k;
        }
        if (bin.right().equals(base) && bin.left() instanceof SymLiteral lit && isIntegral(lit)
                && bin.op() == SymBinaryOp.Op.ADD) {
            return ConstantFoldingStrategy.toLong(lit.value());
        }
        return null;
    }


    private Tristate classifyOffsetComparison(SymBinaryOp b) {
        Long kLeft = extractOffset(b.left(), b.right());
        if (kLeft != null) {
            return classifyKCmp(kLeft, b.op(), false);
        }
        Long kRight = extractOffset(b.right(), b.left());
        if (kRight != null) {
            return classifyKCmp(kRight, mirrorOp(b.op()), false);
        }
        return Tristate.UNKNOWN;
    }


    private Tristate classifyKCmp(long k, SymBinaryOp.Op op, boolean unsigned) {
        if (k == 0) return Tristate.UNKNOWN;  // same as x CMP x already handled

        return switch (op) {
            case SGT  -> k > 0 ? Tristate.TRUE  : Tristate.FALSE;
            case SLT  -> k < 0 ? Tristate.TRUE  : Tristate.FALSE;
            case SGE  -> k > 0 ? Tristate.TRUE  : Tristate.UNKNOWN;  // k=0 already handled
            case SLE  -> k < 0 ? Tristate.TRUE  : Tristate.UNKNOWN;
            case EQ   -> Tristate.FALSE;   // x+k == x only if k==0
            case NEQ  -> Tristate.TRUE;    // x+k != x if k!=0
            default   -> Tristate.UNKNOWN;
        };
    }

    private static SymBinaryOp.Op mirrorOp(SymBinaryOp.Op op) {
        return switch (op) {
            case SGT -> SymBinaryOp.Op.SLT;
            case SLT -> SymBinaryOp.Op.SGT;
            case SGE -> SymBinaryOp.Op.SLE;
            case SLE -> SymBinaryOp.Op.SGE;
            case UGT -> SymBinaryOp.Op.ULT;
            case ULT -> SymBinaryOp.Op.UGT;
            case UGE -> SymBinaryOp.Op.ULE;
            case ULE -> SymBinaryOp.Op.UGE;
            default  -> op; // EQ, NEQ are symmetric
        };
    }


    private static boolean isComparisonOp(SymBinaryOp.Op op) {
        return switch (op) {
            case EQ, NEQ, SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE -> true;
            default -> false;
        };
    }

    private static boolean isIntegral(SymLiteral lit) {
        Object v = lit.value();
        return v instanceof Integer || v instanceof Long
            || v instanceof Short   || v instanceof Byte
            || v instanceof Character;
    }
}