package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;

/**
 * Strategy – Identity / Absorbing-Element Elimination
 *
 * Applies algebraic laws when exactly one operand is a known literal.
 * The other operand is assumed already simplified (children-first traversal).
 *
 * Rules covered:
 * ┌────────────┬──────────────────────────────────────────────────┐
 * │ Arithmetic │ x+0=x  x-0=x  x*1=x  x*0=0  x/1=x  x/x=1      │
 * │            │ x-x=0  x%1=0  x%x=0  0/x=0  0%x=0              │
 * │ Bitwise    │ x&0=0  x|0=x  x^x=0  x<<0=x  x>>0=x  0&x=0    │
 * │ Logical    │ x&&true=x  x&&false=false  x||true=true         │
 * │            │ x||false=x                                       │
 * │ Comparison │ x==x → true   x!=x → false                      │
 * │ ITE        │ ITE(c,x,x) → x                                   │
 * └────────────┴──────────────────────────────────────────────────┘
 */
public final class IdentityEliminationStrategy implements SimplificationStrategy {

    public static final IdentityEliminationStrategy INSTANCE = new IdentityEliminationStrategy();

    private IdentityEliminationStrategy() {}

    @Override
    public SymbolicValue apply(SymbolicValue node) {
        if (node instanceof SymBinaryOp b) return simplifyBinary(b);
        if (node instanceof SymUnaryOp unary) return simplifyUnary(unary);
        if (node instanceof SymITE ite) return simplifyITE(ite);
        return node;
    }

    private SymbolicValue simplifyBinary(SymBinaryOp b) {
        SymbolicValue left  = b.left();
        SymbolicValue right = b.right();

        boolean sameOperand = left.equals(right);

        return switch (b.op()) {

            case ADD -> {
                if (isIntegralLiteral(left,  0)) yield right;
                if (isIntegralLiteral(right, 0)) yield left;
                yield b;
            }
            case SUB -> {
                if (isIntegralLiteral(right, 0)) yield left;
                if (sameOperand)                 yield SymLiteral.of(0);
                yield b;
            }
            case MUL -> {
                if (isIntegralLiteral(left,  0) || isIntegralLiteral(right, 0)) yield SymLiteral.of(0);
                if (isIntegralLiteral(left,  1)) yield right;
                if (isIntegralLiteral(right, 1)) yield left;
                yield b;
            }
            case DIV -> {
                if (isIntegralLiteral(left,  0)) yield SymLiteral.of(0);
                if (isIntegralLiteral(right, 1)) yield left;
                if (sameOperand)                 yield SymLiteral.of(1);
                yield b;
            }
            case MOD -> {
                if (isIntegralLiteral(right, 1)) yield SymLiteral.of(0);
                if (isIntegralLiteral(left,  0)) yield SymLiteral.of(0);
                if (sameOperand)                 yield SymLiteral.of(0);
                yield b;
            }

            case BAND -> {
                if (isIntegralLiteral(left,  0) || isIntegralLiteral(right, 0)) yield SymLiteral.of(0);
                if (isIntegralLiteral(left, -1)) yield right;
                if (isIntegralLiteral(right,-1)) yield left;
                if (sameOperand)                 yield left;
                yield b;
            }
            case BOR -> {
                if (isIntegralLiteral(left,  0)) yield right;
                if (isIntegralLiteral(right, 0)) yield left;
                if (isIntegralLiteral(left, -1) || isIntegralLiteral(right, -1)) yield SymLiteral.of(-1);
                if (sameOperand)                 yield left;
                yield b;
            }
            case BXOR -> {
                if (isIntegralLiteral(left,  0)) yield right;
                if (isIntegralLiteral(right, 0)) yield left;
                if (sameOperand)                 yield SymLiteral.of(0);
                yield b;
            }
            case BLS, BRS, BURS -> {
                if (isIntegralLiteral(right, 0)) yield left;
                if (isIntegralLiteral(left,  0)) yield SymLiteral.of(0);
                yield b;
            }

            case AND -> {
                if (isBoolLiteral(left,  false) || isBoolLiteral(right, false)) yield SymLiteral.of(false);
                if (isBoolLiteral(left,  true))  yield right;
                if (isBoolLiteral(right, true))  yield left;
                if (sameOperand)                 yield left;   // x && x = x
                yield b;
            }
            case OR -> {
                if (isBoolLiteral(left,  true)  || isBoolLiteral(right, true))  yield SymLiteral.of(true);
                if (isBoolLiteral(left,  false)) yield right;
                if (isBoolLiteral(right, false)) yield left;
                if (sameOperand)                 yield left;   // x || x = x
                yield b;
            }

            case EQ  -> sameOperand ? SymLiteral.of(true)  : b;
            case NEQ -> sameOperand ? SymLiteral.of(false) : b;

            case SGE, SLE, UGE, ULE -> sameOperand ? SymLiteral.of(true)  : b;
            case SGT, SLT, UGT, ULT -> sameOperand ? SymLiteral.of(false) : b;

            default -> b;
        };
    }

    private SymbolicValue simplifyUnary(SymUnaryOp u) {
        if (u.op() == SymUnaryOp.Op.PLUS) return u.operand();
        return u;
    }

    private SymbolicValue simplifyITE(SymITE ite) {
        if (ite.thenBranch().equals(ite.elseBranch())) return ite.thenBranch();
        return ite;
    }

    private static boolean isIntegralLiteral(SymbolicValue v, long expected) {
        return v instanceof SymLiteral lit
               && !(lit.value() instanceof Boolean)
               && !(lit.value() instanceof Float)
               && !(lit.value() instanceof Double)
               && ConstantFoldingStrategy.toLong(lit.value()) == expected;
    }

    private static boolean isBoolLiteral(SymbolicValue v, boolean expected) {
        return v instanceof SymLiteral lit
               && lit.value() instanceof Boolean b
               && b == expected;
    }
}