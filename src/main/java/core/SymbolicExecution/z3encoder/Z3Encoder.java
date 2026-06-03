package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.*;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * Converts a simplified {@link SymbolicValue} tree into a Z3 {@link Expr}.
 *
 * ── Encoding decisions ────────────────────────────────────────────────────────
 *
 *  int / short / byte / char  →  BitVecSort(32)
 *  long                       →  BitVecSort(64)
 *  float                      →  FPSort(32)   (IEEE 754 single, RNE rounding)
 *  double                     →  FPSort(64)   (IEEE 754 double, RNE rounding)
 *  boolean                    →  BoolSort
 *  SymVariable                →  mkConst(name, sort) where sort from varSorts map
 *  SymFieldAccess             →  fresh BV32 constant "receiver__field"
 *
 * IntSort is never used for Java integral values.  All integral arithmetic,
 * comparisons, and bitwise ops operate uniformly in the BitVec domain, which
 * means:
 *   • No Int↔BV coercions are ever needed.
 *   • Unsigned comparisons (UGT/UGE/ULT/ULE) are correct by construction.
 *   • Overflow wraps as Java specifies (BV arithmetic wraps mod 2^width).
 *
 * IntSort is retained only as the index sort for array theories (required by Z3).
 *
 * ── Cache ────────────────────────────────────────────────────────────────────
 * IdentityHashMap<SymbolicValue, Expr<?>> keyed on interned references.
 * Because SymValueFactory interns all nodes, pointer equality == structural
 * equality → lookup is O(1) pointer compare; shared sub-trees encoded once.
 *
 * ── FP rounding mode ─────────────────────────────────────────────────────────
 * All FP operations use RNE (round-nearest-ties-to-even), matching Java's
 * default IEEE 754 behaviour.
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
        return visit(node, new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    /**
     * Encode a list of constraints sharing one cache.
     * Common sub-expressions across constraints are encoded exactly once.
     *
     * @param nodes list of boolean-typed SymbolicValues (path conditions)
     * @return list of Z3 BoolExprs in the same order
     */
    public List<BoolExpr> encodeAll(List<SymbolicValue> nodes) {
        IdentityHashMap<SymbolicValue, Expr<?>> exprCache = new IdentityHashMap<>();
        IdentityHashMap<SymbolicValue, Sort>    sortCache = new IdentityHashMap<>();
        return nodes.stream()
                .map(n -> (BoolExpr) visit(n, exprCache, sortCache))
                .toList();
    }

    // =========================================================================
    // Visitor – bottom-up with IdentityHashMap cache
    // =========================================================================

    private Expr<?> visit(SymbolicValue node,
                          IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                          IdentityHashMap<SymbolicValue, Sort>    sortCache) {

        Expr<?> cached = exprCache.get(node);
        if (cached != null) return cached;

        Expr<?> result = encodeNode(node, exprCache, sortCache);
        exprCache.put(node, result);
        return result;
    }

    // =========================================================================
    // Dispatch
    // =========================================================================

    private Expr<?> encodeNode(SymbolicValue node,
                                IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                IdentityHashMap<SymbolicValue, Sort>    sortCache) {

        if (node instanceof SymLiteral lit)     return encodeLiteral(lit);
        if (node instanceof SymVariable var)    return encodeVariable(var, sortCache);
        if (node instanceof SymUnaryOp u)       return encodeUnary(u,  exprCache, sortCache);
        if (node instanceof SymBinaryOp b)      return encodeBinary(b, exprCache, sortCache);
        if (node instanceof SymITE ite)         return encodeITE(ite,  exprCache, sortCache);
        if (node instanceof SymArraySelect s)   return encodeArraySelect(s,  exprCache, sortCache);
        if (node instanceof SymArrayStore st)   return encodeArrayStore(st,  exprCache, sortCache);
        if (node instanceof SymFieldAccess f)   return encodeFieldAccess(f,  exprCache, sortCache);

        throw new IllegalArgumentException("Unsupported SymbolicValue: " + node);
    }

    // =========================================================================
    // SymLiteral
    // =========================================================================

    private Expr<?> encodeLiteral(SymLiteral lit) {
        Object value = lit.value();

        // ── Integral types → BitVec ───────────────────────────────────────
        if (value instanceof Integer i)   return ctx.mkBV(i, 32);
        if (value instanceof Long    l)   return ctx.mkBV(l, 64);
        if (value instanceof Short   s)   return ctx.mkBV(s, 32);
        if (value instanceof Byte    b)   return ctx.mkBV(b, 32);
        if (value instanceof Character c) return ctx.mkBV(c, 32);

        // ── Boolean ───────────────────────────────────────────────────────
        if (value instanceof Boolean b) return ctx.mkBool(b);

        // ── Float (IEEE 754 single) ───────────────────────────────────────
        if (value instanceof Float f) {
            if (Float.isNaN(f))                     return ctx.mkFPNaN(sorts.fp32Sort());
            if (f == Float.POSITIVE_INFINITY)       return ctx.mkFPInf(sorts.fp32Sort(), false);
            if (f == Float.NEGATIVE_INFINITY)       return ctx.mkFPInf(sorts.fp32Sort(), true);
            int bits = Float.floatToRawIntBits(f);
            BitVecExpr signBV = ctx.mkBV((bits >>> 31) & 1, 1);
            BitVecExpr expBV  = ctx.mkBV((bits >>> 23) & 0xFF, 8);
            BitVecExpr sigBV  = ctx.mkBV(bits & 0x7F_FFFF, 23);
            return ctx.mkFP(signBV, expBV, sigBV);
        }

        // ── Double (IEEE 754 double) ──────────────────────────────────────
        if (value instanceof Double d) {
            if (Double.isNaN(d))                    return ctx.mkFPNaN(sorts.fp64Sort());
            if (d == Double.POSITIVE_INFINITY)      return ctx.mkFPInf(sorts.fp64Sort(), false);
            if (d == Double.NEGATIVE_INFINITY)      return ctx.mkFPInf(sorts.fp64Sort(), true);
            long bits = Double.doubleToRawLongBits(d);
            BitVecExpr signBV = ctx.mkBV((bits >>> 63) & 1, 1);
            BitVecExpr expBV  = ctx.mkBV((bits >>> 52) & 0x7FFL, 11);
            BitVecExpr sigBV  = ctx.mkBV(bits & 0x000F_FFFF_FFFF_FFFFL, 52);
            return ctx.mkFP(signBV, expBV, sigBV);
        }

        throw new EncodingException("Unsupported literal type: " + value.getClass(), lit);
    }

    // =========================================================================
    // SymVariable
    // =========================================================================

    private Expr<?> encodeVariable(SymVariable var,
                                   IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Sort sort = sorts.resolve(var, sortCache);
        return ctx.mkConst(var.name(), sort);
    }

    // =========================================================================
    // SymUnaryOp
    // =========================================================================

    private Expr<?> encodeUnary(SymUnaryOp u,
                                 IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                 IdentityHashMap<SymbolicValue, Sort>    sortCache) {
        Expr<?> operand = visit(u.operand(), exprCache, sortCache);
        Sort    opSort  = sorts.resolve(u.operand(), sortCache);

        return switch (u.op()) {

            case NOT -> ctx.mkNot((BoolExpr) operand);

            case NEG -> {
                if (opSort.equals(sorts.fp32Sort()) || opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPNeg((FPExpr) operand);
                yield ctx.mkBVNeg((BitVecExpr) operand);
            }

            case PLUS -> operand;   // unary-plus is identity

            case COMPLIMENT -> ctx.mkBVNot((BitVecExpr) operand);

            case INC -> {
                if (opSort.equals(sorts.fp32Sort()))
                    yield ctx.mkFPAdd(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0f)));
                if (opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPAdd(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0)));
                BitVecExpr bv = (BitVecExpr) operand;
                yield ctx.mkBVAdd(bv, ctx.mkBV(1, bv.getSortSize()));
            }

            case DEC -> {
                if (opSort.equals(sorts.fp32Sort()))
                    yield ctx.mkFPSub(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0f)));
                if (opSort.equals(sorts.fp64Sort()))
                    yield ctx.mkFPSub(RNE, (FPExpr) operand,
                                     (FPExpr) encodeLiteral(SymLiteral.of(1.0)));
                BitVecExpr bv = (BitVecExpr) operand;
                yield ctx.mkBVSub(bv, ctx.mkBV(1, bv.getSortSize()));
            }
        };
    }

    // =========================================================================
    // SymBinaryOp
    // =========================================================================

    private Expr<?> encodeBinary(SymBinaryOp b,
                                  IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                  IdentityHashMap<SymbolicValue, Sort>    sortCache) {

        Sort resultSort = sorts.resolve(b, sortCache);
        Sort leftSort   = sorts.resolve(b.left(),  sortCache);
        Sort rightSort  = sorts.resolve(b.right(), sortCache);

        Expr<?> left  = visit(b.left(),  exprCache, sortCache);
        Expr<?> right = visit(b.right(), exprCache, sortCache);

        // ── FP domain ─────────────────────────────────────────────────────────
        if (isFPSort(resultSort) || isFPSort(leftSort) || isFPSort(rightSort)) {
            FPSort  targetFP = resultSort.equals(sorts.fp64Sort())
                               ? sorts.fp64Sort() : sorts.fp32Sort();
            FPExpr  fpLeft   = coerceToFP(left,  leftSort,  targetFP);
            FPExpr  fpRight  = coerceToFP(right, rightSort, targetFP);
            return encodeFPBinary(b.op(), fpLeft, fpRight, b);
        }

        // ── Bool domain (logical / comparison on BV operands) ─────────────────
        if (resultSort.equals(sorts.boolSort())) {
            return encodeBoolBinary(b.op(), left, right, leftSort, rightSort, b);
        }

        // ── BitVec domain (arithmetic + bitwise) ──────────────────────────────
        BitVecSort targetBV = resultSort instanceof BitVecSort bvs ? bvs : sorts.bv32Sort();
        BitVecExpr bvLeft  = coerceToBV(left,  leftSort,  targetBV);
        BitVecExpr bvRight = coerceToBV(right, rightSort, targetBV);
        return encodeBVBinary(b.op(), bvLeft, bvRight, b);
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
            case MOD -> ctx.mkFPRem(l, r);
            case EQ  -> ctx.mkFPEq(l, r);
            case NEQ -> ctx.mkNot(ctx.mkFPEq(l, r));
            case SGT -> ctx.mkFPGt(l, r);
            case SLT -> ctx.mkFPLt(l, r);
            case SGE -> ctx.mkFPGEq(l, r);
            case SLE -> ctx.mkFPLEq(l, r);
            case UGT -> ctx.mkFPGt(l, r);   // FP has no unsigned; treat as signed
            case UGE -> ctx.mkFPGEq(l, r);
            case ULT -> ctx.mkFPLt(l, r);
            case ULE -> ctx.mkFPLEq(l, r);
            default  -> throw new EncodingException(
                    "Op '" + op + "' not supported in FP domain", node);
        };
    }

    // ── BV arithmetic + bitwise ─────────────────────────────────────────────

    private Expr<?> encodeBVBinary(SymBinaryOp.Op op,
                                   BitVecExpr l, BitVecExpr r,
                                   SymBinaryOp node) {
        return switch (op) {
            // Arithmetic
            case ADD  -> ctx.mkBVAdd(l, r);
            case SUB  -> ctx.mkBVSub(l, r);
            case MUL  -> ctx.mkBVMul(l, r);
            case DIV  -> ctx.mkBVSDiv(l, r);
            case MOD  -> ctx.mkBVSRem(l, r);
            // Bitwise
            case BAND -> ctx.mkBVAND(l, r);
            case BOR  -> ctx.mkBVOR(l, r);
            case BXOR -> ctx.mkBVXOR(l, r);
            case BLS  -> ctx.mkBVSHL(l, r);
            case BRS  -> ctx.mkBVASHR(l, r);   // arithmetic (signed) shift right
            case BURS -> ctx.mkBVLSHR(l, r);   // logical (unsigned) shift right
            default -> throw new EncodingException(
                    "Unexpected op in BV encoding: " + op, node);
        };
    }

    // ── Bool / comparison ───────────────────────────────────────────────────

    private BoolExpr encodeBoolBinary(SymBinaryOp.Op op,
                                      Expr<?> l, Expr<?> r,
                                      Sort leftSort, Sort rightSort,
                                      SymBinaryOp node) {
        // For numeric comparisons coerce operands to a common BV width first.
        if (isNumericComparison(op)) {
            BitVecSort target = commonBVSort(leftSort, rightSort);
            BitVecExpr bvL = coerceToBV(l, leftSort,  target);
            BitVecExpr bvR = coerceToBV(r, rightSort, target);
            return encodeBVComparison(op, bvL, bvR, node);
        }

        return switch (op) {
            case AND -> ctx.mkAnd((BoolExpr) l, (BoolExpr) r);
            case OR  -> ctx.mkOr((BoolExpr)  l, (BoolExpr) r);
            default  -> throw new EncodingException(
                    "Op '" + op + "' not a bool-result binary op", node);
        };
    }

    private BoolExpr encodeBVComparison(SymBinaryOp.Op op,
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
            default  -> throw new EncodingException(
                    "Op '" + op + "' not a BV comparison op", node);
        };
    }

    // =========================================================================
    // SymITE
    // =========================================================================

    private Expr<?> encodeITE(SymITE ite,
                               IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                               IdentityHashMap<SymbolicValue, Sort>    sortCache) {
        BoolExpr cond  = (BoolExpr) visit(ite.cond(),       exprCache, sortCache);
        Expr<?>  then  =            visit(ite.thenBranch(), exprCache, sortCache);
        Expr<?>  else_ =            visit(ite.elseBranch(), exprCache, sortCache);

        // Ensure branch sorts agree (e.g. bv32 vs bv64 edge case)
        Sort thenSort = then.getSort();
        Sort elseSort = else_.getSort();
        if (!thenSort.equals(elseSort) && thenSort instanceof BitVecSort bt
                && elseSort instanceof BitVecSort be) {
            BitVecSort wider = bt.getSize() >= be.getSize() ? bt : be;
            then  = coerceToBV(then,  thenSort,  wider);
            else_ = coerceToBV(else_, elseSort, wider);
        }
        return ctx.mkITE(cond, then, else_);
    }

    // =========================================================================
    // SymArraySelect / SymArrayStore
    // =========================================================================

    private Expr<?> encodeArraySelect(SymArraySelect s,
                                      IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                      IdentityHashMap<SymbolicValue, Sort>    sortCache) {
        Expr<?> arr   = visit(s.arr(),   exprCache, sortCache);
        // Array index sort in Z3 array theory must match the declared index sort.
        // Our arrays are declared with IntSort index (see SortResolver.symTypeToSort),
        // so convert the BV index to IntSort via bv2int (unsigned).
        Expr<?> index = visit(s.index(), exprCache, sortCache);
        Expr<?> intIndex = bvToArrayIndex(index);
        return ctx.mkSelect((ArrayExpr) arr, intIndex);
    }

    private Expr<?> encodeArrayStore(SymArrayStore st,
                                      IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                      IdentityHashMap<SymbolicValue, Sort>    sortCache) {
        Expr<?> arr   = visit(st.arr(),   exprCache, sortCache);
        Expr<?> index = visit(st.index(), exprCache, sortCache);
        Expr<?> val   = visit(st.value(), exprCache, sortCache);
        Expr<?> intIndex = bvToArrayIndex(index);
        return ctx.mkStore((ArrayExpr) arr, intIndex, val);
    }

    /**
     * Convert a BV index to the IntSort index expected by array theory.
     * If the index is already IntSort (shouldn't happen post-refactor, but
     * defensive), pass through unchanged.
     */
    private Expr<?> bvToArrayIndex(Expr<?> index) {
        if (index.getSort() instanceof BitVecSort) {
            return ctx.mkBV2Int((BitVecExpr) index, false); // unsigned interpretation
        }
        return index; // already IntSort (legacy / reference type)
    }

    // =========================================================================
    // SymFieldAccess → fresh BV32 variable "receiver__field"
    // =========================================================================

    private Expr<?> encodeFieldAccess(SymFieldAccess f,
                                       IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                       IdentityHashMap<SymbolicValue, Sort>    sortCache) {
        Expr<?> recv     = visit(f.receiver(), exprCache, sortCache);
        String freshName = receiverKey(recv) + "__" + f.fieldName();
        return ctx.mkBVConst(freshName, 32);
    }

    /** Derive a stable, SMT-LIB-safe string key from an encoded receiver expression. */
    private static String receiverKey(Expr<?> recv) {
        return recv.toString().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // =========================================================================
    // Coercion helpers  (BV ↔ BV width, BV → FP)
    // =========================================================================

    /**
     * Coerce any integral/FP expression to a {@link BitVecExpr} of {@code target} width.
     *
     * BitVecSort (same width)  → identity
     * BitVecSort (narrower)    → sign-extend
     * BitVecSort (wider)       → extract low bits
     * FPSort                   → mkFPToBV (round-to-zero signed)
     */
    private BitVecExpr coerceToBV(Expr<?> expr, Sort sort, BitVecSort target) {
        int w = target.getSize();

        if (expr instanceof BitVecExpr bv) {
            int bw = bv.getSortSize();
            if (bw == w) return bv;
            if (bw < w)  return ctx.mkSignExt(w - bw, bv);
            return ctx.mkExtract(w - 1, 0, bv);
        }

        if (sort instanceof BitVecSort bvs) {
            int bw = bvs.getSize();
            BitVecExpr bv = (BitVecExpr) expr;
            if (bw == w) return bv;
            if (bw < w)  return ctx.mkSignExt(w - bw, bv);
            return ctx.mkExtract(w - 1, 0, bv);
        }

        if (sort instanceof FPSort) {
            return ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), (FPExpr) expr, w, true);
        }

        throw new IllegalArgumentException("Cannot coerce sort " + sort + " to BitVecSort");
    }

    /**
     * Coerce any numeric expression to an FP expression of {@code target} sort.
     *
     * FP (same sort)  → identity
     * FP (other sort) → mkFPToFP (widen or narrow)
     * BitVec          → mkFPToFP (signed BV → FP)
     */
    private FPExpr coerceToFP(Expr<?> expr, Sort sort, FPSort target) {
        if (sort.equals(target)) return (FPExpr) expr;

        if (sort instanceof FPSort) {
            return ctx.mkFPToFP(RNE, (FPExpr) expr, target);
        }

        if (sort instanceof BitVecSort) {
            // Signed BV → FP via mkFPToFP(rounding, bv, sort, signed=true)
            return ctx.mkFPToFP(RNE, (BitVecExpr) expr, target, true);
        }

        throw new IllegalArgumentException("Cannot coerce sort " + sort + " to FPSort");
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /** Widest BV sort given two operand sorts (minimum bv32). */
    private BitVecSort commonBVSort(Sort a, Sort b) {
        int w = 32;
        if (a instanceof BitVecSort bva) w = Math.max(w, bva.getSize());
        if (b instanceof BitVecSort bvb) w = Math.max(w, bvb.getSize());
        return w > 32 ? sorts.bv64Sort() : sorts.bv32Sort();
    }

    private static boolean isFPSort(Sort s) {
        return s instanceof FPSort;
    }

    private static boolean isNumericComparison(SymBinaryOp.Op op) {
        return switch (op) {
            case EQ, NEQ, SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE -> true;
            default -> false;
        };
    }
}