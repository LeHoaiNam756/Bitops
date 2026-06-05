package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;

/**
 * Strategy: Constant Folding
 *
 * Evaluates operations whose operands are all {@link SymLiteral} at
 * analysis time, collapsing the node into a single literal.
 *
 * Examples:
 *   BinaryOp(Lit(3), ADD, Lit(4))              → Lit(7)
 *   UnaryOp(NEG, Lit(5))                       → Lit(-5)
 *   ITE(Lit(true), t, e)                       → t
 *   UnaryOp(NOT, UnaryOp(NOT, x))              → x        (double-negation)
 *   StringOp(Lit("hi"), LENGTH,      [])       → Lit(2)
 *   StringOp(Lit("hi"), IS_EMPTY,    [])       → Lit(false)
 *   StringOp(Lit("Hi"), TO_LOWER_CASE, [])     → Lit("hi")
 *   StringOp(Lit("hi"), CONTAINS,   [Lit("i")]) → Lit(true)
 *   StringOp(Lit("hi"), SUBSTRING,  [Lit(1)])  → Lit("i")
 *   StringOp(Lit("hi"), INDEX_OF,   [Lit("i"), Lit(0)]) → Lit(1)
 */
public final class ConstantFoldingStrategy implements SimplificationStrategy {

    public static final ConstantFoldingStrategy INSTANCE = new ConstantFoldingStrategy();

    private ConstantFoldingStrategy() {}

    @Override
    public SymbolicValue apply(SymbolicValue node) {
        if (node instanceof SymBinaryOp b)  { return foldBinary(b); }
        if (node instanceof SymUnaryOp u)   { return foldUnary(u); }
        if (node instanceof SymITE ie)      { return foldITE(ie); }
        if (node instanceof SymStringOp so) { return foldStringOp(so); }
        return node;
    }

    private SymbolicValue foldBinary(SymBinaryOp b) {
        if (!(b.left() instanceof SymLiteral ll) || !(b.right() instanceof SymLiteral rl))
            return b;

        Object l = ll.value();
        Object r = rl.value();

        if (l instanceof String ls && r instanceof String rs) {
            return switch (b.op()) {
                case ADD -> SymLiteral.of(ls + rs);
                case EQ  -> SymLiteral.of(ls.equals(rs));
                case NEQ -> SymLiteral.of(!ls.equals(rs));
                default  -> b;
            };
        }

        if (l instanceof Boolean lb && r instanceof Boolean rb) {
            return switch (b.op()) {
                case AND -> SymLiteral.of(lb && rb);
                case OR  -> SymLiteral.of(lb || rb);
                case EQ  -> SymLiteral.of(lb == rb);
                case NEQ -> SymLiteral.of(lb != rb);
                default  -> b;
            };
        }

        if (isFloating(l) || isFloating(r)) {
            double ld = toDouble(l), rd = toDouble(r);
            return switch (b.op()) {
                case ADD -> narrowDouble(ld + rd, l, r);
                case SUB -> narrowDouble(ld - rd, l, r);
                case MUL -> narrowDouble(ld * rd, l, r);
                case DIV -> narrowDouble(ld / rd, l, r);
                case EQ  -> SymLiteral.of(ld == rd);
                case NEQ -> SymLiteral.of(ld != rd);
                case SGT -> SymLiteral.of(ld >  rd);
                case SLT -> SymLiteral.of(ld <  rd);
                case SGE -> SymLiteral.of(ld >= rd);
                case SLE -> SymLiteral.of(ld <= rd);
                default  -> b;
            };
        }

        long lv = toLong(l), rv = toLong(r);
        return switch (b.op()) {
            case ADD  -> narrowLong(lv + rv, l, r);
            case SUB  -> narrowLong(lv - rv, l, r);
            case MUL  -> narrowLong(lv * rv, l, r);
            case DIV  -> rv == 0 ? b : narrowLong(lv / rv, l, r);
            case MOD  -> rv == 0 ? b : narrowLong(lv % rv, l, r);
            case BAND -> narrowLong(lv & rv,   l, r);
            case BOR  -> narrowLong(lv | rv,   l, r);
            case BXOR -> narrowLong(lv ^ rv,   l, r);
            case BLS  -> narrowLong(lv << rv,  l, r);
            case BRS  -> narrowLong(lv >> rv,  l, r);
            case BURS -> narrowLong(lv >>> rv, l, r);
            case EQ   -> SymLiteral.of(lv == rv);
            case NEQ  -> SymLiteral.of(lv != rv);
            case SGT  -> SymLiteral.of(lv >  rv);
            case SLT  -> SymLiteral.of(lv <  rv);
            case SGE  -> SymLiteral.of(lv >= rv);
            case SLE  -> SymLiteral.of(lv <= rv);
            case UGT  -> SymLiteral.of(Long.compareUnsigned(lv, rv) >  0);
            case UGE  -> SymLiteral.of(Long.compareUnsigned(lv, rv) >= 0);
            case ULT  -> SymLiteral.of(Long.compareUnsigned(lv, rv) <  0);
            case ULE  -> SymLiteral.of(Long.compareUnsigned(lv, rv) <= 0);
            case AND  -> SymLiteral.of(lv != 0 && rv != 0);
            case OR   -> SymLiteral.of(lv != 0 || rv != 0);
        };
    }

    private SymbolicValue foldUnary(SymUnaryOp u) {
        if (u.operand() instanceof SymUnaryOp inner) {
            if (u.op() == SymUnaryOp.Op.NOT  && inner.op() == SymUnaryOp.Op.NOT)  return inner.operand();
            if (u.op() == SymUnaryOp.Op.NEG  && inner.op() == SymUnaryOp.Op.NEG)  return inner.operand();
            if (u.op() == SymUnaryOp.Op.PLUS && inner.op() == SymUnaryOp.Op.PLUS) return inner.operand();
        }

        if (!(u.operand() instanceof SymLiteral lit)) return u;
        Object val = lit.value();

        return switch (u.op()) {
            case NEG        -> foldNeg(val);
            case NOT        -> foldNot(val);
            case COMPLIMENT -> foldComplement(val);
            case PLUS       -> lit;
            case INC        -> foldAddInt(val,  1);
            case DEC        -> foldAddInt(val, -1);
        };
    }

       private SymbolicValue foldITE(SymITE ite) {
        if (ite.cond() instanceof SymLiteral lit && lit.value() instanceof Boolean b)
            return b ? ite.thenBranch() : ite.elseBranch();
        return ite;
    }

    /**
     * Folds a {@link SymStringOp} when the receiver (and any required args) are
     * all {@link SymLiteral} values. Returns the original node if folding is not
     * possible (e.g. symbolic receiver, wrong arg types, or out-of-bounds index).
     */
    private SymbolicValue foldStringOp(SymStringOp so) {
        // Receiver must be a literal String.
        if (!(so.receiver() instanceof SymLiteral recvLit)) return so;
        if (!(recvLit.value() instanceof String s))         return so;

        // Helper: extract a literal String arg at position i, or return null.
        java.util.List<SymbolicValue> args = so.args();

        try {
            return switch (so.op()) {
                // ---- zero-arg ops ------------------------------------------------
                case LENGTH       -> SymLiteral.of(s.length());
                case IS_EMPTY     -> SymLiteral.of(s.isEmpty());
                case TO_LOWER_CASE -> SymLiteral.of(s.toLowerCase());
                case TO_UPPER_CASE -> SymLiteral.of(s.toUpperCase());
                case TRIM          -> SymLiteral.of(s.trim());

                // ---- one-arg String ops -----------------------------------------
                case EQUALS      -> {
                    if (!isLitString(args, 0)) yield so;
                    yield SymLiteral.of(s.equals(litString(args, 0)));
                }
                case CONTAINS    -> {
                    if (!isLitString(args, 0)) yield so;
                    yield SymLiteral.of(s.contains(litString(args, 0)));
                }
                case STARTS_WITH -> {
                    if (!isLitString(args, 0)) yield so;
                    yield SymLiteral.of(s.startsWith(litString(args, 0)));
                }
                case ENDS_WITH   -> {
                    if (!isLitString(args, 0)) yield so;
                    yield SymLiteral.of(s.endsWith(litString(args, 0)));
                }

                // ---- SUBSTRING(begin) or SUBSTRING(begin, end) ------------------
                case SUBSTRING   -> {
                    if (!isLitInt(args, 0)) yield so;
                    int begin = litInt(args, 0);
                    if (args.size() == 1) {
                        yield SymLiteral.of(s.substring(begin));
                    }
                    if (!isLitInt(args, 1)) yield so;
                    yield SymLiteral.of(s.substring(begin, litInt(args, 1)));
                }

                // ---- INDEX_OF(str) or INDEX_OF(str, fromIndex) ------------------
                case INDEX_OF    -> {
                    if (!isLitString(args, 0)) yield so;
                    String needle = litString(args, 0);
                    if (args.size() == 1) {
                        yield SymLiteral.of(s.indexOf(needle));
                    }
                    if (!isLitInt(args, 1)) yield so;
                    yield SymLiteral.of(s.indexOf(needle, litInt(args, 1)));
                }

                // ---- REPLACE(target, replacement) --------------------------------
                case REPLACE     -> {
                    if (!isLitString(args, 0) || !isLitString(args, 1)) yield so;
                    yield SymLiteral.of(s.replace(litString(args, 0), litString(args, 1)));
                }
            };
        } catch (IndexOutOfBoundsException e) {
            // Out-of-bounds indices at analysis time — leave node symbolic.
            return so;
        }
    }

    // ---- SymStringOp arg-extraction helpers ------------------------------------

    /** Returns true if args[i] is a SymLiteral whose value is a String. */
    private static boolean isLitString(java.util.List<SymbolicValue> args, int i) {
        return i < args.size()
                && args.get(i) instanceof SymLiteral lit
                && lit.value() instanceof String;
    }

    /** Returns true if args[i] is a SymLiteral whose value is an integral number. */
    private static boolean isLitInt(java.util.List<SymbolicValue> args, int i) {
        if (i >= args.size() || !(args.get(i) instanceof SymLiteral lit)) return false;
        Object v = lit.value();
        return v instanceof Integer || v instanceof Long
                || v instanceof Short || v instanceof Byte;
    }

    private static String litString(java.util.List<SymbolicValue> args, int i) {
        return (String) ((SymLiteral) args.get(i)).value();
    }

    private static int litInt(java.util.List<SymbolicValue> args, int i) {
        return (int) toLong(((SymLiteral) args.get(i)).value());
    }

     private static SymbolicValue foldNeg(Object v) {
        if (v instanceof Integer i) return SymLiteral.of(-i);
        if (v instanceof Long    l) return SymLiteral.of(-l);
        if (v instanceof Float   f) return SymLiteral.of(-f);
        if (v instanceof Double  d) return SymLiteral.of(-d);
        if (v instanceof Short   s) return SymLiteral.of((short) -s);
        if (v instanceof Byte    b) return SymLiteral.of((byte)  -b);
        throw new IllegalArgumentException("NEG: unsupported type " + v.getClass());
    }

    private static SymbolicValue foldNot(Object v) {
        if (v instanceof Boolean b) return SymLiteral.of(!b);
        throw new IllegalArgumentException("NOT: requires boolean, got " + v.getClass());
    }

    private static SymbolicValue foldComplement(Object v) {
        if (v instanceof Integer i) return SymLiteral.of(~i);
        if (v instanceof Long    l) return SymLiteral.of(~l);
        throw new IllegalArgumentException("~: requires integral, got " + v.getClass());
    }

    private static SymbolicValue foldAddInt(Object v, int delta) {
        if (v instanceof Integer  i) return SymLiteral.of(i + delta);
        if (v instanceof Long     l) return SymLiteral.of(l + delta);
        if (v instanceof Short    s) return SymLiteral.of((short)(s + delta));
        if (v instanceof Byte     b) return SymLiteral.of((byte) (b + delta));
        if (v instanceof Character c) return SymLiteral.of((char)(c + delta));
        throw new IllegalArgumentException("INC/DEC: unsupported type " + v.getClass());
    }

    static boolean isFloating(Object o) { return o instanceof Float || o instanceof Double; }

    static long toLong(Object o) {
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return (long) i;
        if (o instanceof Short    s) return (long) s;
        if (o instanceof Byte     b) return (long) b;
        if (o instanceof Character c) return (long) c;
        throw new IllegalArgumentException("INC/DEC: unsupported type " + o.getClass());
    }

    static double toDouble(Object o) {
        if (o instanceof Double d) return d;
        if (o instanceof Float f) return (double) f;
        return (double) toLong(o);
    }

    /** Re-narrow result to int unless either operand was long. */
    static SymbolicValue narrowLong(long result, Object l, Object r) {
        if (l instanceof Long || r instanceof Long) return SymLiteral.of(result);
        return SymLiteral.of((int) result);
    }

    /** Re-narrow result to float unless either operand was double. */
    static SymbolicValue narrowDouble(double result, Object l, Object r) {
        if (l instanceof Double || r instanceof Double) return SymLiteral.of(result);
        return SymLiteral.of((float) result);
    }
}