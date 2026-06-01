package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;
import core.SymbolicExecution.simplifier.SimplificationStrategy;

import java.util.*;

/**
 * Strategy 4 – Normalisation
 *
 * Produces a canonical form for expressions so that semantically identical
 * trees have the same structure. This is essential for:
 *   • memoization / constraint caching inside Z3
 *   • structural equality checks in DeadConstraintStrategy
 *   • readable debug output
 *
 * Passes performed:
 *
 *   1. Commutativity canonicalisation
 *      For commutative ops (ADD, MUL, BAND, BOR, BXOR, EQ, NEQ, AND, OR)
 *      ensure left ≤ right in a deterministic term order so that
 *      "a + b" and "b + a" collapse to the same tree.
 *
 *   2. Sum flattening
 *      ADD(ADD(a, b), c)  →  ADD(a, ADD(b, c))   (right-associate)
 *      Literals in a flat sum are merged into one constant at the right end:
 *      ADD(3, ADD(x, ADD(5, y)))  →  ADD(x, ADD(y, Lit(8)))
 *
 *   3. Product flattening – same idea for MUL.
 *
 *   4. Negation normalisation
 *      SUB(a, b)  →  ADD(a, NEG(b))     when b is not a literal
 *      (makes subtraction explicit as "add of negation", simplifying
 *       later algebraic reasoning)
 *
 * Term ordering (deterministic, not semantic):
 *   Literals < Variables (alphabetical) < compound expressions (by toString)
 *   This gives Z3 better sharing opportunities.
 */
public final class NormalisationStrategy implements SimplificationStrategy {

    public static final NormalisationStrategy INSTANCE = new NormalisationStrategy();

    private NormalisationStrategy() {}

     @Override
    public SymbolicValue apply(SymbolicValue node) {
        if (node instanceof SymBinaryOp bin) return normaliseBinary(bin);
        return node;
    }

    private SymbolicValue normaliseBinary(SymBinaryOp b) {

        if (b.op() == SymBinaryOp.Op.SUB && !(b.right() instanceof SymLiteral)) {
            SymbolicValue negRight = new SymUnaryOp(SymUnaryOp.Op.NEG, b.right());
            return normaliseBinary(new SymBinaryOp(b.left(), SymBinaryOp.Op.ADD, negRight));
        }

        if (b.op() == SymBinaryOp.Op.ADD) {
            return flattenAssociative(b, SymBinaryOp.Op.ADD);
        }

        if (b.op() == SymBinaryOp.Op.MUL) {
            return flattenAssociative(b, SymBinaryOp.Op.MUL);
        }

        if (isCommutative(b.op())) {
            if (termOrder(b.left()) > termOrder(b.right())
                    || (termOrder(b.left()) == termOrder(b.right())
                        && termString(b.left()).compareTo(termString(b.right())) > 0)) {
                return new SymBinaryOp(b.right(), b.op(), b.left());
            }
        }

        return b;
    }


    private SymbolicValue flattenAssociative(SymBinaryOp root, SymBinaryOp.Op op) {
        List<SymbolicValue> nonLiterals = new ArrayList<>();
        long   intAcc    = (op == SymBinaryOp.Op.MUL) ? 1 : 0;
        double floatAcc  = (op == SymBinaryOp.Op.MUL) ? 1.0 : 0.0;
        boolean hasFloat = false;
        boolean hasInt   = false;

        Deque<SymbolicValue> work = new ArrayDeque<>();
        work.push(root.right());
        work.push(root.left());

        while (!work.isEmpty()) {
            SymbolicValue v = work.pop();
            if (v instanceof SymBinaryOp bin && bin.op() == op) {
                work.push(bin.right());
                work.push(bin.left());
            } else if (v instanceof SymLiteral lit && isNumericLiteral(lit)) {
                Object val = lit.value();
                if (ConstantFoldingStrategy.isFloating(val)) {
                    floatAcc = (op == SymBinaryOp.Op.MUL)
                            ? floatAcc * ConstantFoldingStrategy.toDouble(val)
                            : floatAcc + ConstantFoldingStrategy.toDouble(val);
                    hasFloat = true;
                } else {
                    intAcc = (op == SymBinaryOp.Op.MUL)
                            ? intAcc * ConstantFoldingStrategy.toLong(val)
                            : intAcc + ConstantFoldingStrategy.toLong(val);
                    hasInt = true;
                }
            } else {
                nonLiterals.add(v);
            }
        }

        nonLiterals.sort(Comparator.comparingInt(NormalisationStrategy::termOrder)
                                   .thenComparing(NormalisationStrategy::termString));

        SymbolicValue accNode = null;
        if (hasFloat) {
            double total = (op == SymBinaryOp.Op.MUL) ? floatAcc * intAcc : floatAcc + intAcc;
            boolean isNeutral = (op == SymBinaryOp.Op.ADD && total == 0.0)
                             || (op == SymBinaryOp.Op.MUL && total == 1.0);
            if (!isNeutral) accNode = SymLiteral.of(total);
        } else if (hasInt) {
            boolean isNeutral = (op == SymBinaryOp.Op.ADD && intAcc == 0)
                             || (op == SymBinaryOp.Op.MUL && intAcc == 1);
            if (!isNeutral) accNode = SymLiteral.of((int) intAcc); // keep as int by default
        }

        if (accNode != null) nonLiterals.add(accNode);

        if (nonLiterals.isEmpty()) {
            return op == SymBinaryOp.Op.ADD ? SymLiteral.of(0) : SymLiteral.of(1);
        }
        if (nonLiterals.size() == 1) return nonLiterals.get(0);

        SymbolicValue result = nonLiterals.get(nonLiterals.size() - 1);
        for (int i = nonLiterals.size() - 2; i >= 0; i--) {
            result = new SymBinaryOp(nonLiterals.get(i), op, result);
        }
        return result;
    }


    private static int termOrder(SymbolicValue v) {
        if (v instanceof SymLiteral) return 0;
        if (v instanceof SymVariable) return 1;
        return 2;
    }


    private static String termString(SymbolicValue v) {
        if (v instanceof SymLiteral lit) return  lit.value().toString();
        if (v instanceof SymVariable var) return var.name();
        return v.toString();
    }

    private static boolean isCommutative(SymBinaryOp.Op op) {
        return switch (op) {
            case ADD, MUL, BAND, BOR, BXOR, EQ, NEQ, AND, OR -> true;
            default -> false;
        };
    }

    private static boolean isNumericLiteral(SymLiteral lit) {
        Object v = lit.value();
        return v instanceof Integer || v instanceof Long    || v instanceof Short
            || v instanceof Byte    || v instanceof Float   || v instanceof Double
            || v instanceof Character;
    }
}