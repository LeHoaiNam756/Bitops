package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.*;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a simplified {@link SymbolicValue} tree into a Z3 {@link Expr}.
 *
 * ── Encoding decisions (from design review) ───────────────────────────────────
 *
 *  Integers   → IntSort (unbounded Z3 integer)
 *  float      → FPSort32  (IEEE 754 single, via Z3 FP theory)
 *  double     → FPSort64  (IEEE 754 double, via Z3 FP theory)
 *  boolean    → BoolSort
 *  Bitwise    → IntSort default; auto-switched to BitVecSort(32|64) when a
 *               bitwise op (BAND BOR BXOR BLS BRS BURS) is present
 *  SymVariable → mkConst(name, sort) where sort comes from caller's varSorts map
 *  SymFieldAccess → encoded as fresh variable "receiver.field" (IntSort)
 *
 * ── Cache ────────────────────────────────────────────────────────────────────
 * IdentityHashMap<SymbolicValue, Expr<?>> keyed on interned references.
 * Because SymValueFactory interns all nodes, pointer equality == structural
 * equality → lookup is O(1) pointer compare, shared sub-trees encoded once.
 *
 * ── FP rounding mode ─────────────────────────────────────────────────────────
 * All FP operations use RNE (round-nearest-ties-to-even), matching Java's
 * default IEEE 754 behaviour.
 *
 * ── BitVec promotion ─────────────────────────────────────────────────────────
 * When a bitwise op is detected, operands that are IntSort are sign-extended
 * to BitVecSort(32) or (64) via mkInt2BV before the operation is applied.
 *
 * ── Usage ────────────────────────────────────────────────────────────────────
 * <pre>
 *   SortResolver resolver = new SortResolver(ctx, varSorts);
 *   Z3Encoder    encoder  = new Z3Encoder(resolver);
 *
 *   Expr<?> z3expr = encoder.encode(symValue);
 *   BoolExpr constraint = (BoolExpr) encoder.encode(boolSymValue);
 * </pre>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Z3Encoder {

    private enum EncodingMode { DEFAULT, BITVEC }

    private final Context      ctx;
    private final SortResolver sorts;

    /** FP rounding mode: round-nearest-ties-to-even (matches Java). */
    private final FPRMExpr RNE;

    // =========================================================================
    // Construction
    // =========================================================================

    public Z3Encoder(SortResolver sortResolver) {
        this.sorts = sortResolver;
        this.ctx   = sortResolver.ctx();
        this.RNE   = ctx.mkFPRoundNearestTiesToEven();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Encode a single {@link SymbolicValue} into a Z3 {@link Expr}.
     * Creates a fresh per-call cache; for encoding many constraints together
     * use {@link #encodeAll}.
     */
    public Expr<?> encode(SymbolicValue node) {
        return visit(node, new IdentityHashMap<>(),
                     new IdentityHashMap<>(), EncodingMode.DEFAULT);
    }

    /**
     * Encode a list of constraints sharing one cache.
     * Common sub-expressions across constraints are encoded exactly once.
     *
     * @param nodes list of boolean-typed SymbolicValues (path conditions)
     * @return list of Z3 BoolExprs in the same order
     */
    public List<BoolExpr> encodeAll(List<SymbolicValue> nodes) {
        IdentityHashMap<SymbolicValue, Sort>    sortCache = new IdentityHashMap<>();
        return nodes.stream()
                .map(n -> {
                    EncodingMode mode = requiresBitVecMode(n, sortCache)
                            ? EncodingMode.BITVEC
                            : EncodingMode.DEFAULT;
                    return (BoolExpr) visit(n, new IdentityHashMap<>(), sortCache, mode);
                })
                .toList();
    }

    // =========================================================================
    // Visitor – bottom-up with IdentityHashMap cache
    // =========================================================================

    private Expr<?> visit(SymbolicValue node,
                          IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                          IdentityHashMap<SymbolicValue, Sort>    sortCache,
                          EncodingMode mode) {

        Expr<?> cached = exprCache.get(node);
        if (cached != null) return cached;

        Expr<?> result = encode(node, exprCache, sortCache, mode);
        exprCache.put(node, result);
        return result;
    }

    // =========================================================================
    // Dispatch
    // =========================================================================

    private Expr<?> encode(SymbolicValue node,
                           IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                           IdentityHashMap<SymbolicValue, Sort> sortCache,
                           EncodingMode mode) {

        if (node instanceof SymLiteral lit) {
            return encodeLiteral(lit, mode);
        }

        if (node instanceof SymVariable var) {
            return encodeVariable(var, sortCache, mode);
        }

        if (node instanceof SymUnaryOp u) {
            return encodeUnary(u, exprCache, sortCache, mode);
        }

        if (node instanceof SymBinaryOp b) {
            return encodeBinary(b, exprCache, sortCache, mode);
        }

        if (node instanceof SymITE ite) {
            return encodeITE(ite, exprCache, sortCache, mode);
        }

        if (node instanceof SymArraySelect s) {
            return encodeArraySelect(s, exprCache, sortCache, mode);
        }

        if (node instanceof SymArrayStore st) {
            return encodeArrayStore(st, exprCache, sortCache, mode);
        }

        if (node instanceof SymFieldAccess f) {
            return encodeFieldAccess(f, exprCache, sortCache, mode);
        }

        throw new IllegalArgumentException(
                "Unsupported SymbolicValue: " + node);
    }


    // =========================================================================
    // SymLiteral
    // =========================================================================

   private Expr<?> encodeLiteral(SymLiteral lit, EncodingMode mode) {

    Object value = lit.value();

    // ── Integers ──────────────────────────────────────────────────────
    if (value instanceof Integer i) {
        if (mode == EncodingMode.BITVEC) return ctx.mkBV(i, 32);
        return ctx.mkInt(i);
    }

    if (value instanceof Long l) {
        if (mode == EncodingMode.BITVEC) return ctx.mkBV(l, 32);
        return ctx.mkInt(l);
    }

    if (value instanceof Short s) {
        if (mode == EncodingMode.BITVEC) return ctx.mkBV(s, 32);
        return ctx.mkInt(s);
    }

    if (value instanceof Byte b) {
        if (mode == EncodingMode.BITVEC) return ctx.mkBV(b, 32);
        return ctx.mkInt(b);
    }

    if (value instanceof Character c) {
        if (mode == EncodingMode.BITVEC) return ctx.mkBV(c, 32);
        return ctx.mkInt(c); // char as Unicode code point
    }

    // ── Booleans ──────────────────────────────────────────────────────
    if (value instanceof Boolean b) {
        return ctx.mkBool(b);
    }

    // ── Floats (IEEE 754 single) ─────────────────────────────────────
    if (value instanceof Float f) {

        if (Float.isNaN(f)) {
            return ctx.mkFPNaN(sorts.fp32Sort());
        }

        if (f == Float.POSITIVE_INFINITY) {
            return ctx.mkFPInf(sorts.fp32Sort(), false);
        }

        if (f == Float.NEGATIVE_INFINITY) {
            return ctx.mkFPInf(sorts.fp32Sort(), true);
        }

        // Decompose to sign + biased exponent + significand bits
        int bits = Float.floatToRawIntBits(f);

        boolean sign = (bits >>> 31) != 0;
        long exp = (bits >>> 23) & 0xFF;
        long sig = bits & 0x7F_FFFF;
        BitVecExpr signBV = ctx.mkBV(sign ? 1 : 0, 1);
        BitVecExpr expBV  = ctx.mkBV(exp, 8);
        BitVecExpr sigBV  = ctx.mkBV(sig, 23);
        return ctx.mkFP(signBV, expBV, sigBV);
    }

    // ── Doubles (IEEE 754 double) ────────────────────────────────────
    if (value instanceof Double) {
        Double d = (Double) value;

        if (Double.isNaN(d)) {
            return ctx.mkFPNaN(sorts.fp64Sort());
        }

        if (d == Double.POSITIVE_INFINITY) {
            return ctx.mkFPInf(sorts.fp64Sort(), false);
        }

        if (d == Double.NEGATIVE_INFINITY) {
            return ctx.mkFPInf(sorts.fp64Sort(), true);
        }

        long bits = Double.doubleToRawLongBits(d);

        boolean sign = (bits >>> 63) != 0;
        long exp = (bits >>> 52) & 0x7FFL;
        long sig = bits & 0x000F_FFFF_FFFF_FFFFL;
        BitVecExpr signBV = ctx.mkBV(sign ? 1 : 0, 1);
        BitVecExpr expBV  = ctx.mkBV(exp, 11);
        BitVecExpr sigBV  = ctx.mkBV(sig, 52);
        return ctx.mkFP(signBV, expBV, sigBV);
    }

    throw new EncodingException(
            "Unsupported literal type: " + value.getClass(), lit);
}

    // =========================================================================
    // SymVariable
    // =========================================================================

    private Expr<?> encodeVariable(SymVariable var,
                                   IdentityHashMap<SymbolicValue, Sort> sortCache,
                                   EncodingMode mode) {
        Sort sort = sorts.resolve(var, sortCache);
        if (mode == EncodingMode.BITVEC && sort.equals(sorts.intSort())) {
            return ctx.mkBVConst(var.name(), 32);
        }
        return ctx.mkConst(var.name(), sort);
    }

    // =========================================================================
    // SymUnaryOp
    // =========================================================================

    private Expr<?> encodeUnary(SymUnaryOp u,
                                 IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                 IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                 EncodingMode mode) {
        Expr<?> operand = visit(u.operand(), exprCache, sortCache, mode);
        Sort    opSort  = sorts.resolve(u.operand(), sortCache);

        return switch (u.op()) {

            case NOT -> ctx.mkNot((BoolExpr) operand);

            case NEG -> {
                if (opSort.equals(sorts.fp32Sort()) || opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPNeg((FPExpr) operand);
                if (opSort instanceof BitVecSort)
                    yield ctx.mkBVNeg((BitVecExpr) operand);
                yield ctx.mkUnaryMinus((ArithExpr) operand);
            }

            case PLUS -> operand;   // unary-plus is identity

            case COMPLIMENT -> {
                // Bitwise complement (~x) — requires BitVec
                BitVecExpr bv = promoteToIntBV(operand, opSort, sorts.bv32Sort());
                yield ctx.mkBVNot(bv);
            }

            // INC / DEC: x++ → x + 1  (analysis sees post-increment as new SSA write,
            // so this branch is reached only for prefix ++/-- in expressions)
            case INC -> {
                if (opSort.equals(sorts.fp32Sort()))
                    yield ctx.mkFPAdd(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0f), EncodingMode.DEFAULT));
                if (opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPAdd(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0), EncodingMode.DEFAULT));
                if (operand instanceof BitVecExpr bv)
                    yield ctx.mkBVAdd(bv, ctx.mkBV(1, bv.getSortSize()));
                yield ctx.mkAdd((ArithExpr<IntSort>) operand, ctx.mkInt(1));
            }

            case DEC -> {
                if (opSort.equals(sorts.fp32Sort()))
                    yield ctx.mkFPSub(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0f), EncodingMode.DEFAULT));
                if (opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPSub(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0), EncodingMode.DEFAULT));
                if (operand instanceof BitVecExpr bv)
                    yield ctx.mkBVSub(bv, ctx.mkBV(1, bv.getSortSize()));
                yield ctx.mkSub((ArithExpr<IntSort>) operand, ctx.mkInt(1));
            }
        };
    }

    // =========================================================================
    // SymBinaryOp
    // =========================================================================

    private Expr<?> encodeBinary(SymBinaryOp b,
                                  IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                  IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                  EncodingMode mode) {

        Sort resultSort = sorts.resolve(b, sortCache);
        Sort leftSort   = sorts.resolve(b.left(),  sortCache);
        Sort rightSort  = sorts.resolve(b.right(), sortCache);

        Expr<?> left  = visit(b.left(),  exprCache, sortCache, mode);
        Expr<?> right = visit(b.right(), exprCache, sortCache, mode);

        // ── Bitwise ops → BitVec domain ───────────────────────────────────────
        if (isBitwiseOp(b.op())) {
            BitVecSort targetBV = resultSort instanceof BitVecSort bvs
                                  ? bvs : sorts.bv32Sort();
            BitVecExpr bvLeft  = promoteToIntBV(left,  leftSort,  targetBV);
            BitVecExpr bvRight = promoteToIntBV(right, rightSort, targetBV);
            return encodeBitwise(b.op(), bvLeft, bvRight);
        }

        if (mode == EncodingMode.BITVEC && resultSort.equals(sorts.intSort())) {
            BitVecExpr[] operands = coerceBitVecOperands(left, right, leftSort, rightSort);
            return encodeBitVecArithmetic(b.op(), operands[0], operands[1], b);
        }

        // ── FP domain (at least one FP operand) ───────────────────────────────
        if (isFPSort(resultSort) || isFPSort(leftSort) || isFPSort(rightSort)) {
            FPSort   targetFP = resultSort.equals(sorts.fp64Sort())
                                ? sorts.fp64Sort() : sorts.fp32Sort();
            FPExpr   fpLeft   = coerceToFP(left,  leftSort,  targetFP);
            FPExpr   fpRight  = coerceToFP(right, rightSort, targetFP);
            return encodeFPBinary(b.op(), fpLeft, fpRight, b);
        }

        // ── Bool domain (logical / comparison) ────────────────────────────────
        if (resultSort.equals(sorts.boolSort())) {
            return encodeBoolBinary(b.op(), left, right, leftSort, rightSort, mode, b);
        }

        // ── Integer domain ────────────────────────────────────────────────────
        return encodeIntBinary(b.op(), (ArithExpr<?>) left, (ArithExpr<?>) right, b);
    }

    // ── Integer binary ──────────────────────────────────────────────────────

    private Expr<?> encodeIntBinary(SymBinaryOp.Op op,
                                    ArithExpr<?> l, ArithExpr<?> r,
                                    SymBinaryOp node) {
        return switch (op) {
            case ADD -> ctx.mkAdd(l, r);
            case SUB -> ctx.mkSub(l, r);
            case MUL -> ctx.mkMul(l, r);
            case DIV -> ctx.mkDiv(l, r);
            case MOD -> ctx.mkMod((IntExpr) l, (IntExpr) r);
            default  -> throw new EncodingException(
                "Unexpected op in integer binary encoding: " + op, node);
        };
    }

    // ── FP binary ───────────────────────────────────────────────────────────

    private Expr<?> encodeFPBinary(SymBinaryOp.Op op,
                                   FPExpr l, FPExpr r,
                                   SymBinaryOp node) {
        return switch (op) {
            case ADD -> ctx.mkFPAdd(RNE, l, r);
            case SUB -> ctx.mkFPSub(RNE, l, r);
            case MUL -> ctx.mkFPMul(RNE, l, r);
            case DIV -> ctx.mkFPDiv(RNE, l, r);
            case MOD -> ctx.mkFPRem(l, r);            // IEEE 754 remainder (no rounding)
            case EQ  -> ctx.mkFPEq(l, r);
            case NEQ -> ctx.mkNot(ctx.mkFPEq(l, r));
            case SGT -> ctx.mkFPGt(l, r);
            case SLT -> ctx.mkFPLt(l, r);
            case SGE -> ctx.mkFPGEq(l, r);
            case SLE -> ctx.mkFPLEq(l, r);
            // Unsigned comparisons on FP: treat as signed (FP has no unsigned)
            case UGT -> ctx.mkFPGt(l, r);
            case UGE -> ctx.mkFPGEq(l, r);
            case ULT -> ctx.mkFPLt(l, r);
            case ULE -> ctx.mkFPLEq(l, r);
            default  -> throw new EncodingException(
                "Op '" + op + "' not supported in FP domain", node);
        };
    }

    // ── Bool / comparison binary ─────────────────────────────────────────────

    private BoolExpr encodeBoolBinary(SymBinaryOp.Op op,
                                      Expr<?> l, Expr<?> r,
                                      Sort leftSort,
                                      Sort rightSort,
                                      EncodingMode mode,
                                      SymBinaryOp node) {
        if (mode == EncodingMode.BITVEC && isNumericComparison(op)) {
            BitVecExpr[] operands = coerceBitVecOperands(l, r, leftSort, rightSort);
            return encodeBitVecComparison(op, operands[0], operands[1], node);
        }

        return switch (op) {
            case EQ -> {
                Expr<?>[] coerced = coerceEqualityOperands(l, r, leftSort, rightSort);
                yield ctx.mkEq(coerced[0], coerced[1]);
            }
            case NEQ -> {
                Expr<?>[] coerced = coerceEqualityOperands(l, r, leftSort, rightSort);
                yield ctx.mkNot(ctx.mkEq(coerced[0], coerced[1]));
            }

            // Arithmetic comparisons
            case SGT -> ctx.mkGt((ArithExpr<?>) l, (ArithExpr<?>) r);
            case SLT -> ctx.mkLt((ArithExpr<?>) l, (ArithExpr<?>) r);
            case SGE -> ctx.mkGe((ArithExpr<?>) l, (ArithExpr<?>) r);
            case SLE -> ctx.mkLe((ArithExpr<?>) l, (ArithExpr<?>) r);

            // Unsigned comparisons on IntSort: Z3 integers are infinite-precision,
            // no unsigned concept → treat as signed (sound for non-negative values)
            case UGT -> ctx.mkGt((ArithExpr<?>) l, (ArithExpr<?>) r);
            case UGE -> ctx.mkGe((ArithExpr<?>) l, (ArithExpr<?>) r);
            case ULT -> ctx.mkLt((ArithExpr<?>) l, (ArithExpr<?>) r);
            case ULE -> ctx.mkLe((ArithExpr<?>) l, (ArithExpr<?>) r);

            // Logical
            case AND -> ctx.mkAnd((BoolExpr) l, (BoolExpr) r);
            case OR  -> ctx.mkOr((BoolExpr) l,  (BoolExpr) r);

            default  -> throw new EncodingException(
                "Op '" + op + "' not a bool-result binary op", node);
        };
    }

    // ── Bitwise ─────────────────────────────────────────────────────────────

    private BitVecExpr encodeBitwise(SymBinaryOp.Op op,
                                     BitVecExpr l, BitVecExpr r) {
        return switch (op) {
            case BAND -> ctx.mkBVAND(l, r);
            case BOR  -> ctx.mkBVOR(l, r);
            case BXOR -> ctx.mkBVXOR(l, r);
            case BLS  -> ctx.mkBVSHL(l, r);
            case BRS  -> ctx.mkBVASHR(l, r);  // arithmetic (signed) shift right
            case BURS -> ctx.mkBVLSHR(l, r);  // logical (unsigned) shift right
            default   -> throw new IllegalStateException("Not a bitwise op: " + op);
        };
    }

    private Expr<?> encodeBitVecArithmetic(SymBinaryOp.Op op,
                                           BitVecExpr l, BitVecExpr r,
                                           SymBinaryOp node) {
        return switch (op) {
            case ADD -> ctx.mkBVAdd(l, r);
            case SUB -> ctx.mkBVSub(l, r);
            case MUL -> ctx.mkBVMul(l, r);
            case DIV -> ctx.mkBVSDiv(l, r);
            case MOD -> ctx.mkBVSRem(l, r);
            default -> throw new EncodingException(
                    "Unexpected op in bitvector arithmetic encoding: " + op, node);
        };
    }

    private BoolExpr encodeBitVecComparison(SymBinaryOp.Op op,
                                            BitVecExpr l, BitVecExpr r,
                                            SymBinaryOp node) {
        return switch (op) {
            case EQ  -> ctx.mkEq(l, r);
            case NEQ -> ctx.mkNot(ctx.mkEq(l, r));
            case SGT -> ctx.mkBVSGT(l, r);
            case SLT -> ctx.mkBVSLT(l, r);
            case SGE -> ctx.mkBVSGE(l, r);
            case SLE -> ctx.mkBVSLE(l, r);
            case UGT -> ctx.mkBVUGT(l, r);
            case UGE -> ctx.mkBVUGE(l, r);
            case ULT -> ctx.mkBVULT(l, r);
            case ULE -> ctx.mkBVULE(l, r);
            default -> throw new EncodingException(
                    "Op '" + op + "' not a bitvector comparison op", node);
        };
    }

    private Expr<?>[] coerceEqualityOperands(Expr<?> left, Expr<?> right,
                                             Sort leftSort, Sort rightSort) {
        if (leftSort.equals(rightSort)) return new Expr<?>[] { left, right };

        if (leftSort instanceof BitVecSort leftBV && rightSort.equals(sorts.intSort())) {
            return new Expr<?>[] { left, promoteToIntBV(right, rightSort, leftBV) };
        }

        if (leftSort.equals(sorts.intSort()) && rightSort instanceof BitVecSort rightBV) {
            return new Expr<?>[] { promoteToIntBV(left, leftSort, rightBV), right };
        }

        return new Expr<?>[] { left, right };
    }

    // =========================================================================
    // SymITE
    // =========================================================================

    private Expr<?> encodeITE(SymITE ite,
                                IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                EncodingMode mode) {
        BoolExpr cond  = (BoolExpr) visit(ite.cond(),       exprCache, sortCache, mode);
        Expr<?>  then  =            visit(ite.thenBranch(), exprCache, sortCache, mode);
        Expr<?>  else_ =            visit(ite.elseBranch(), exprCache, sortCache, mode);
        return ctx.mkITE(cond, then, else_);
    }

    // =========================================================================
    // SymArraySelect / SymArrayStore
    // =========================================================================

    private Expr<?> encodeArraySelect(SymArraySelect s,
                                       IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                       IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                       EncodingMode mode) {
        Expr<?> arr   = visit(s.arr(),   exprCache, sortCache, EncodingMode.DEFAULT);
        Expr<?> index = visit(s.index(), exprCache, sortCache, EncodingMode.DEFAULT);
        Expr<?> selected = ctx.mkSelect((ArrayExpr) arr, index);
        Sort selectSort = sorts.resolve(s, sortCache);
        if (mode == EncodingMode.BITVEC && selectSort.equals(sorts.intSort())) {
            return promoteToIntBV(selected, selectSort, sorts.bv32Sort());
        }
        return selected;
    }

    private Expr<?> encodeArrayStore(SymArrayStore st,
                                      IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                      IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                      EncodingMode mode) {
        Expr<?> arr   = visit(st.arr(),   exprCache, sortCache, EncodingMode.DEFAULT);
        Expr<?> index = visit(st.index(), exprCache, sortCache, EncodingMode.DEFAULT);
        Expr<?> val   = visit(st.value(), exprCache, sortCache, mode);
        return ctx.mkStore((ArrayExpr) arr, index, val);
    }

    // =========================================================================
    // SymFieldAccess  →  fresh variable  "receiver_field"
    // =========================================================================

    private Expr<?> encodeFieldAccess(SymFieldAccess f,
                                       IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                       IdentityHashMap<SymbolicValue, Sort>    sortCache,
                                       EncodingMode mode) {
        // Encode the receiver to get a stable name fragment
        Expr<?> recv = visit(f.receiver(), exprCache, sortCache, mode);

        // Build a fresh variable name: "recv_toString__fieldName"
        // This loses the structural relationship (per design decision) but gives
        // Z3 a stable free variable it can assign a value to.
        String freshName = receiverKey(recv) + "__" + f.fieldName();
        if (mode == EncodingMode.BITVEC) return ctx.mkBVConst(freshName, 32);
        return ctx.mkConst(freshName, sorts.intSort());
    }

    /** Derive a string key from an encoded receiver expression. */
    private static String receiverKey(Expr<?> recv) {
        // Z3 Expr.toString() is stable and unique for constants
        String raw = recv.toString();
        // Sanitise: Z3 SMT-LIB names cannot contain spaces/parens
        return raw.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // =========================================================================
    // Type coercion helpers
    // =========================================================================

    /**
     * Promote an IntSort or FPSort expression to a BitVecExpr of {@code target} width.
     * Used when entering the bitwise op domain.
     *
     * IntSort  → mkInt2BV (sign-extends value to BV width)
     * FPSort   → mkFPToSBV (round-to-zero, then sign-extend if needed)
     * BitVecSort (same width) → identity
     * BitVecSort (different width) → sign-extend or truncate
     */
    private BitVecExpr promoteToIntBV(Expr<?> expr, Sort sort, BitVecSort target) {
        int targetWidth = target.getSize();

        if (expr instanceof BitVecExpr bv) {
            int w = bv.getSortSize();
            if (w == targetWidth) return bv;
            if (w < targetWidth)  return ctx.mkSignExt(targetWidth - w, bv);
            return ctx.mkExtract(targetWidth - 1, 0, bv);
        }

        if (sort instanceof BitVecSort bvs) {
            int w = bvs.getSize();
            if (w == targetWidth) return (BitVecExpr) expr;
            if (w < targetWidth)  return ctx.mkSignExt(targetWidth - w, (BitVecExpr) expr);
            return ctx.mkExtract(targetWidth - 1, 0, (BitVecExpr) expr); // truncate
        }

        if (sort.equals(sorts.intSort())) {
            // mkInt2BV: converts a Z3 integer to a BV of given width
            return ctx.mkInt2BV(targetWidth, (IntExpr) expr);
        }

        if (sort instanceof FPSort) {
            // FP → BV via signed conversion (round-to-zero)
            return ctx.mkFPToBV(ctx.mkFPRoundTowardZero(),
                                 (FPExpr) expr, targetWidth, true);
        }

        throw new IllegalArgumentException(
            "Cannot promote sort " + sort + " to BitVecSort");
    }

    private BitVecExpr[] coerceBitVecOperands(Expr<?> left, Expr<?> right,
                                              Sort leftSort, Sort rightSort) {
        BitVecSort target = commonBitVecSort(left, right, leftSort, rightSort);
        return new BitVecExpr[] {
                promoteToIntBV(left, left.getSort(), target),
                promoteToIntBV(right, right.getSort(), target)
        };
    }

    private BitVecSort commonBitVecSort(Expr<?> left, Expr<?> right,
                                        Sort leftSort, Sort rightSort) {
        int width = 32;
        if (left.getSort() instanceof BitVecSort leftBV) width = Math.max(width, leftBV.getSize());
        if (right.getSort() instanceof BitVecSort rightBV) width = Math.max(width, rightBV.getSize());
        if (leftSort instanceof BitVecSort leftBV) width = Math.max(width, leftBV.getSize());
        if (rightSort instanceof BitVecSort rightBV) width = Math.max(width, rightBV.getSize());
        return width > 32 ? sorts.bv64Sort() : sorts.bv32Sort();
    }

    /**
     * Coerce any numeric expression to an FP expression of {@code target} sort.
     *
     * IntSort  → mkInt2Real → mkRealToFP  (exact for integers fitting in mantissa)
     * FP32     → mkFPToFP   (widen to FP64 if needed)
     * FP64     → identity or narrow to FP32
     */
    private FPExpr coerceToFP(Expr<?> expr, Sort sort, FPSort target) {
        if (sort.equals(target)) return (FPExpr) expr;

        if (sort.equals(sorts.intSort())) {
            // int → real → fp  (two-step; Z3 has no direct int→fp)
            RealExpr real = ctx.mkInt2Real((IntExpr) expr);
            return ctx.mkFPToFP(RNE, real, target);
        }

        if (sort instanceof FPSort) {
            // FP widening or narrowing
            return ctx.mkFPToFP(RNE, (FPExpr) expr, target);
        }

        if (sort instanceof BitVecSort) {
            return ctx.mkFPToFP(RNE, (BitVecExpr) expr, target, true);
}

        throw new IllegalArgumentException(
            "Cannot coerce sort " + sort + " to FPSort");
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private static boolean isBitwiseOp(SymBinaryOp.Op op) {
        return switch (op) {
            case BAND, BOR, BXOR, BLS, BRS, BURS -> true;
            default -> false;
        };
    }

    private boolean requiresBitVecMode(SymbolicValue node,
                                       IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Sort sort = sorts.resolve(node, sortCache);
        if (sort instanceof BitVecSort) return true;

        if (node instanceof SymBinaryOp b) {
            return isBitwiseOp(b.op())
                    || requiresBitVecMode(b.left(), sortCache)
                    || requiresBitVecMode(b.right(), sortCache);
        }
        if (node instanceof SymUnaryOp u) return requiresBitVecMode(u.operand(), sortCache);
        if (node instanceof SymITE ite) {
            return requiresBitVecMode(ite.cond(), sortCache)
                    || requiresBitVecMode(ite.thenBranch(), sortCache)
                    || requiresBitVecMode(ite.elseBranch(), sortCache);
        }
        if (node instanceof SymArraySelect s) {
            return requiresBitVecMode(s.arr(), sortCache)
                    || requiresBitVecMode(s.index(), sortCache);
        }
        if (node instanceof SymArrayStore st) {
            return requiresBitVecMode(st.arr(), sortCache)
                    || requiresBitVecMode(st.index(), sortCache)
                    || requiresBitVecMode(st.value(), sortCache);
        }
        if (node instanceof SymFieldAccess f) return requiresBitVecMode(f.receiver(), sortCache);
        return false;
    }

    private static boolean isNumericComparison(SymBinaryOp.Op op) {
        return switch (op) {
            case EQ, NEQ, SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE -> true;
            default -> false;
        };
    }

    private static boolean isFPSort(Sort s) {
        return s instanceof FPSort;
    }
}
