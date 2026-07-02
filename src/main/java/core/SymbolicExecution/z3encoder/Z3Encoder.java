package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.*;

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
 *  String                     →  StringSort   (Z3 string/sequence theory)
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

    /**
     * Side-constraints emitted while encoding {@code TO_LOWER_CASE} /
     * {@code TO_UPPER_CASE}.  Each call introduces a fresh result variable and
     * asserts two axioms:
     * <ol>
     *   <li>Length equality: {@code len(result) = len(input)}
     *   <li>Character mapping: {@code ∀ i ∈ [0, len(input)).
     *         let c = code(input[i])
     *         if 'A' ≤ c ≤ 'Z' then code(result[i]) = c + 32   [toLower]
     *                           else code(result[i]) = c}
     * </ol>
     * The caller must add these constraints to the solver in the same
     * {@code assert} batch as the main formula.  Retrieve and clear via
     * {@link #drainSideConstraints()}.
     */
    private final java.util.List<BoolExpr> sideConstraints = new java.util.ArrayList<>();

    /** Counter for generating unique result-variable names. */
    private int caseOpCounter = 0;

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
     * Returns any side-constraints accumulated during the most recent
     * {@link #encode} / {@link #encodeAll} call (e.g. from
     * {@code TO_LOWER_CASE} / {@code TO_UPPER_CASE}), then clears the list.
     *
     * <p>These <em>must</em> be asserted into the solver together with the
     * primary formula, otherwise the case-folding result variables are
     * unconstrained:
     * <pre>
     *   List&lt;BoolExpr&gt; constraints = encoder.encodeAll(pathConditions);
     *   solver.add(constraints.toArray(new BoolExpr[0]));
     *   solver.add(encoder.drainSideConstraints().toArray(new BoolExpr[0]));
     * </pre>
     */
    public java.util.List<BoolExpr> drainSideConstraints() {
        var result = java.util.List.copyOf(sideConstraints);
        sideConstraints.clear();
        return result;
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
        if (node instanceof SymCastOp c)        return encodeCast(c,   exprCache, sortCache);
        if (node instanceof SymStringOp s)      return encodeStringOp(s, exprCache, sortCache);
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
        if (value instanceof Short   s)   return ctx.mkBV(s, 16);
        if (value instanceof Byte    b)   return ctx.mkBV(b, 8);
        if (value instanceof Character c) return ctx.mkBV(c, 16);

        // ── Boolean ───────────────────────────────────────────────────────
        if (value instanceof Boolean b) return ctx.mkBool(b);

        // ── String ────────────────────────────────────────────────────────
        if (value instanceof String s) return ctx.mkString(s);

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
    // SymCastOp  –  JLS 26 §5.1 / §5.4 / §5.5 type conversions
    // =========================================================================

    /**
     * Encodes a Java cast expression {@code (T) expr} according to JLS 26 §5.1.
     *
     * <p>Supported conversion categories:
     * <ul>
     *   <li><b>Identity (§5.1.1)</b> – target sort equals operand sort; return as-is.</li>
     *   <li><b>Widening primitive (§5.1.2)</b> – byte/short/char/int → wider integral or FP;
     *       int/long → float/double.  Integral widening uses sign-extension except char which
     *       uses zero-extension (char is unsigned 0–65535).  Integral-to-FP uses
     *       {@code mkFPToFP(RNE, signedBV, targetSort)}.</li>
     *   <li><b>Narrowing primitive (§5.1.3)</b> – wider integral/FP → narrower.  Integral
     *       narrowing discards high-order bits ({@code mkExtract}), <em>not</em> sign-extension.
     *       FP→FP narrowing uses {@code mkFPToFP(RNE)}.  FP→integral uses
     *       {@code mkFPToBV(roundTowardZero)} per §5.1.3 (truncates toward zero).</li>
     *   <li><b>Widening-then-narrowing (§5.1.4)</b> – byte→short is widening, handled above.</li>
     *   <li><b>boolean (§5.1.1)</b> – only identity cast {@code (boolean)} is legal in Java;
     *       returned unchanged.</li>
     *   <li><b>Reference / object (§5.5)</b> – modelled as a bv32 identity (heap address);
     *       the encoder has no heap model so we return the operand coerced to bv32.</li>
     * </ul>
     *
     * <p>The method uses {@link SortResolver#javaTypeOf} to distinguish char (zero-extension)
     * from other sub-int types (sign-extension) during integral widening, matching the
     * {@link #coerceToBV} policy used in binary expressions.
     */
    private Expr<?> encodeCast(SymCastOp cast,
                                IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                IdentityHashMap<SymbolicValue, Sort>    sortCache) {

        Expr<?> operand    = visit(cast.operand(), exprCache, sortCache);
        Sort    fromSort   = sorts.resolve(cast.operand(), sortCache);
        Sort    toSort     = sorts.symTypeToSort(cast.type());

        // ── Identity ──────────────────────────────────────────────────────────
        if (fromSort.equals(toSort)) {
            return operand;
        }

        // ── Boolean → boolean only: identity (Java does not allow other casts) ─
        if (toSort.equals(sorts.boolSort())) {
            if (fromSort.equals(sorts.boolSort())) return operand;
            throw new EncodingException(
                    "Illegal cast to boolean from non-boolean sort " + fromSort, cast);
        }

        // ── Target is a String sort (reference cast — treated as identity) ─────
        if (sorts.isStringSort(toSort)) {
            if (sorts.isStringSort(fromSort)) return operand;
            throw new EncodingException(
                    "Cannot cast non-String expression to String sort", cast);
        }

        // ── Target is FP ──────────────────────────────────────────────────────
        //   BV  → FP : widening integral-to-FP (§5.1.2); char is unsigned so zero-ext
        //   FP  → FP : widening (float→double) or narrowing (double→float), both use RNE (§5.1.3)
        if (toSort instanceof FPSort fpTarget) {
            if (fromSort instanceof FPSort) {
                // float ↔ double: mkFPToFP with RNE handles both directions
                return ctx.mkFPToFP(RNE, (FPExpr) operand, fpTarget);
            }
            if (fromSort instanceof BitVecSort) {
                // Integral → FP (§5.1.2): treat char as unsigned, others as signed
                boolean unsigned = cast.operand() instanceof SymLiteral lit
                        && lit.value() instanceof Character;
                SymType jt = sorts.javaTypeOf(cast.operand());
                boolean isChar = jt == PrimitiveSymType.CHAR || unsigned;
                return ctx.mkFPToFP(RNE, (BitVecExpr) operand, fpTarget, !isChar);
            }
            throw new EncodingException(
                    "Cannot cast sort " + fromSort + " to FP sort " + toSort, cast);
        }

        // ── Target is BV ──────────────────────────────────────────────────────
        if (toSort instanceof BitVecSort bvTarget) {
            int targetWidth = bvTarget.getSize();

            // FP → integral (§5.1.3): truncate toward zero (JLS mandates this)
            if (fromSort instanceof FPSort) {
                return jlsFpToIntegral((FPExpr) operand, (FPSort) fromSort, targetWidth);
            }

            // BV → BV: widening or narrowing
            if (fromSort instanceof BitVecSort bvFrom) {
                int fromWidth = bvFrom.getSize();

                if (fromWidth == targetWidth) {
                    // Same width: identity (e.g. (int)(int) — no-op after promotion)
                    return operand;
                }

                if (fromWidth > targetWidth) {
                    // Narrowing (§5.1.3): discard high-order bits — extract low targetWidth bits.
                    // This is NOT sign-extension; it is a raw bit truncation, matching JVM behaviour:
                    //   (byte) 256  = 0, (short) 65536 = 0, (byte) -129 = 127
                    return ctx.mkExtract(targetWidth - 1, 0, (BitVecExpr) operand);
                }

                // Widening (§5.1.2): char is zero-extended; byte/short/int are sign-extended.
                SymType jt = sorts.javaTypeOf(cast.operand());
                boolean isChar = jt == PrimitiveSymType.CHAR;
                return isChar
                        ? ctx.mkZeroExt(targetWidth - fromWidth, (BitVecExpr) operand)
                        : ctx.mkSignExt(targetWidth - fromWidth, (BitVecExpr) operand);
            }

            // IntSort (array-index / reference) → BV: treat as unsigned int-to-BV
            if (fromSort.equals(sorts.intSort())) {
                return ctx.mkInt2BV(targetWidth, (Expr<IntSort>) operand);
            }

            throw new EncodingException(
                    "Cannot cast sort " + fromSort + " to BV sort " + toSort, cast);
        }

        // ── Reference (object) types: bv32 identity (heap address model) ──────
        // Both fromSort and toSort should already be bv32 for object types (see
        // SortResolver.symTypeToSort), so the identity branch above normally fires.
        // This is a defensive fallback for any exotic sort combination.
        throw new EncodingException(
                "Unsupported cast from sort " + fromSort + " to sort " + toSort, cast);
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

        // Retrieve Java source types for the operands so that coerceToBV can
        // apply zero-extension for char (JLS §5.1.2) vs sign-extension for
        // byte/short/int.  These are null for non-variable sub-expressions
        // (literals, compound expressions) because literals are already
        // bit-exact and compound results are always int-promoted or wider.
        SymType leftJavaType  = sorts.javaTypeOf(b.left());
        SymType rightJavaType = sorts.javaTypeOf(b.right());

        Expr<?> left  = visit(b.left(),  exprCache, sortCache);
        Expr<?> right = visit(b.right(), exprCache, sortCache);

        // ── String domain ─────────────────────────────────────────────────────
        if (sorts.isStringSort(resultSort) || sorts.isStringSort(leftSort)
                || sorts.isStringSort(rightSort)) {
            return encodeStringBinary(b.op(), left, right, leftSort, rightSort, b);
        }

        // ── FP domain ─────────────────────────────────────────────────────────
        if (isFPSort(resultSort) || isFPSort(leftSort) || isFPSort(rightSort)) {
            FPSort  targetFP = resultSort.equals(sorts.fp64Sort())
                               ? sorts.fp64Sort() : sorts.fp32Sort();
            FPExpr  fpLeft   = coerceToFP(left,  leftSort,  targetFP, leftJavaType);
            FPExpr  fpRight  = coerceToFP(right, rightSort, targetFP, rightJavaType);
            return encodeFPBinary(b.op(), fpLeft, fpRight, b);
        }

        // ── Bool domain (logical / comparison on BV operands) ─────────────────
        if (resultSort.equals(sorts.boolSort())) {
            return encodeBoolBinary(b.op(), left, right, leftSort, rightSort, b);
        }

        // ── BitVec domain (arithmetic + bitwise) ──────────────────────────────
        // For shift ops the result sort equals the LEFT operand's sort (JLS §15.19),
        // so we coerce left to targetBV normally but keep the right operand's
        // width independent — matchWidth + maskShiftDist in encodeBVBinary will
        // handle it.  For all other BV ops both operands are coerced to targetBV.
        BitVecSort targetBV = resultSort instanceof BitVecSort bvs ? bvs : sorts.bv32Sort();
        BitVecExpr bvLeft   = coerceToBV(left,  leftSort,  targetBV, leftJavaType);
        BitVecExpr bvRight  = coerceToBV(right, rightSort, targetBV, rightJavaType);
        return encodeBVBinary(b.op(), bvLeft, bvRight, b);
    }

    // ── String concat / equality ───────────────────────────────────────────

    private Expr<?> encodeStringBinary(SymBinaryOp.Op op,
                                       Expr<?> l, Expr<?> r,
                                       Sort leftSort, Sort rightSort,
                                       SymBinaryOp node) {
        Expr<SeqSort<CharSort>> leftString = asStringConversion(l, leftSort, node.left(), node);
        Expr<SeqSort<CharSort>> rightString = asStringConversion(r, rightSort, node.right(), node);

        return switch (op) {
            case ADD -> ctx.mkConcat(leftString, rightString);
            case EQ  -> ctx.mkEq(leftString, rightString);
            case NEQ -> ctx.mkNot(ctx.mkEq(leftString, rightString));
            default -> throw new EncodingException(
                    "Op '" + op + "' not supported in String domain", node);
        };
    }

    // =========================================================================
    // SymStringOp
    // =========================================================================

    private Expr<?> encodeStringOp(SymStringOp op,
                                   IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                   IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Expr<SeqSort<CharSort>> receiver = asStringExpr(
                visit(op.receiver(), exprCache, sortCache),
                sorts.resolve(op.receiver(), sortCache),
                op);

        return switch (op.op()) {
            case EQUALS -> ctx.mkEq(receiver, stringArg(op, 0, exprCache, sortCache));
            case CONTAINS -> ctx.mkContains(receiver, stringArg(op, 0, exprCache, sortCache));
            case STARTS_WITH -> ctx.mkPrefixOf(stringArg(op, 0, exprCache, sortCache), receiver);
            case ENDS_WITH -> ctx.mkSuffixOf(stringArg(op, 0, exprCache, sortCache), receiver);
            case LENGTH -> ctx.mkInt2BV(32, ctx.mkLength(receiver));
            case IS_EMPTY -> ctx.mkEq(ctx.mkLength(receiver), ctx.mkInt(0));
            case SUBSTRING     -> encodeSubstring(op, receiver, exprCache, sortCache);
            case TO_LOWER_CASE -> encodeCaseFold(receiver, true);
            case TO_UPPER_CASE -> encodeCaseFold(receiver, false);
            case TRIM          -> encodeTrim(receiver);
            case REPLACE       -> ctx.mkReplace(
                                      receiver,
                                      stringArg(op, 0, exprCache, sortCache),
                                      stringArg(op, 1, exprCache, sortCache));
            case INDEX_OF      -> encodeIndexOf(op, receiver, exprCache, sortCache);
        };
    }

    private Expr<?> encodeSubstring(SymStringOp op,
                                    Expr<SeqSort<CharSort>> receiver,
                                    IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                    IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (op.args().isEmpty() || op.args().size() > 2) {
            throw new EncodingException("String.substring expects one or two arguments", op);
        }

        IntExpr begin = intIndexArg(op, 0, exprCache, sortCache);
        IntExpr length = op.args().size() == 1
                ? (IntExpr) ctx.mkSub(ctx.mkLength(receiver), begin)
                : (IntExpr) ctx.mkSub(intIndexArg(op, 1, exprCache, sortCache), begin);
        return ctx.mkExtract(receiver, begin, length);
    }

    /**
     * Encodes {@code String.indexOf}.
     * <ul>
     *   <li>One arg  → {@code str.indexof(recv, needle, 0)}
     *   <li>Two args → {@code str.indexof(recv, needle, fromIndex)}
     * </ul>
     * Z3's {@code str.indexof} returns an {@code IntSort} value (−1 when absent).
     * We convert to BV32 to match the declared sort of {@code INDEX_OF}.
     */
    private Expr<?> encodeIndexOf(SymStringOp op,
                                   Expr<SeqSort<CharSort>> receiver,
                                   IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                   IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (op.args().isEmpty() || op.args().size() > 2) {
            throw new EncodingException("String.indexOf expects one or two arguments", op);
        }
        Expr<SeqSort<CharSort>> needle = stringArg(op, 0, exprCache, sortCache);
        IntExpr offset = op.args().size() == 1
                ? ctx.mkInt(0)
                : intIndexArg(op, 1, exprCache, sortCache);
        // mkIndexOf returns IntSort; narrow to BV32 (signed — indexOf returns -1 on miss)
        IntExpr result = (IntExpr) ctx.mkIndexOf(receiver, needle, offset);
        return ctx.mkInt2BV(32, result);
    }

    /**
     * Encodes {@code String.toLowerCase()} / {@code String.toUpperCase()}.
     *
     * <p>Z3's string theory has no native case-folding op, so we model it by
     * introducing a fresh result variable and asserting two axioms as
     * side-constraints (see {@link #drainSideConstraints()}):
     *
     * <ol>
     *   <li><b>Length:</b> {@code len(result) = len(input)}
     *   <li><b>Per-character mapping</b> via a universally quantified formula:
     *       <pre>
     *         ∀ i : Int.  0 ≤ i ∧ i < len(input)  →
     *           let c      = str.to_code(str.at(input,  i))
     *           let r      = str.to_code(str.at(result, i))
     *           let isCase = (lo ≤ c ≤ hi)          -- 'A'-'Z' or 'a'-'z'
     *           in  r = if isCase then c ± 32 else c
     *       </pre>
     *   The quantifier is bounded by a concrete {@code len(input)} guard,
     *   so Z3's string solver can instantiate it on concrete lengths.
     * </ol>
     *
     * @param toLower {@code true} for toLowerCase, {@code false} for toUpperCase
     */
    private Expr<SeqSort<CharSort>> encodeCaseFold(
            Expr<SeqSort<CharSort>> input, boolean toLower) {

        SeqSort<CharSort> strSort = sorts.stringSort();
        String resultName = (toLower ? "str.to_lower#" : "str.to_upper#") + caseOpCounter++;

        // Fresh result variable
        Expr<SeqSort<CharSort>> result =
                (Expr<SeqSort<CharSort>>) ctx.mkConst(resultName, strSort);

        // ── Axiom 1: same length ──────────────────────────────────────────────
        IntExpr inputLen  = (IntExpr) ctx.mkLength(input);
        IntExpr resultLen = (IntExpr) ctx.mkLength(result);
        sideConstraints.add(ctx.mkEq(resultLen, inputLen));

        // ── Axiom 2: per-character case mapping ───────────────────────────────
        //
        // Named integer constant used as the bound variable in mkForall.
        // Z3's Java mkForall takes the same Expr constants in both the
        // bound-vars array and the body — it converts them to de Bruijn
        // indices internally.
        IntExpr i = ctx.mkIntConst("i#" + resultName);

        // c = charToInt(nth(input,  i))   — code point of input char
        // r = charToInt(nth(result, i))   — code point of result char
        // ctx.charToInt() is the correct Java API name (no "mk" prefix).
        IntExpr c = ctx.charToInt((Expr<CharSort>) ctx.mkNth(input,  i));
        IntExpr r = ctx.charToInt((Expr<CharSort>) ctx.mkNth(result, i));

        // Source range: 'A'–'Z' (65–90) for toLower; 'a'–'z' (97–122) for toUpper
        int lo = toLower ? 65  : 97;   // 'A' : 'a'
        int hi = toLower ? 90  : 122;  // 'Z' : 'z'
        int delta = toLower ? 32 : -32;

        BoolExpr inRange = ctx.mkAnd(
                ctx.mkGe(c, ctx.mkInt(lo)),
                ctx.mkLe(c, ctx.mkInt(hi)));

        // r = (inRange ? c ± 32 : c)
        IntExpr mappedCode = (IntExpr) ctx.mkITE(inRange,
                ctx.mkAdd(c, ctx.mkInt(delta)), c);
        BoolExpr charConstraint = ctx.mkEq(r, mappedCode);

        // Guard: 0 ≤ i < len(input)
        BoolExpr inBounds = ctx.mkAnd(
                ctx.mkGe(i, ctx.mkInt(0)),
                ctx.mkLt(i, inputLen));

        // ∀ i. inBounds → charConstraint
        // Pass the same IntExpr constant in both the bound-vars array and the
        // body; Z3 replaces it with the correct de Bruijn index internally.
        BoolExpr body = ctx.mkImplies(inBounds, charConstraint);
        BoolExpr forAll = ctx.mkForall(
                new Expr[]{ i },   // bound variable — same object used in body
                body,
                1,       // weight
                null, null, null, null);

        sideConstraints.add(forAll);

        return result;
    }

    /**
     * Encodes {@code String.trim()} as an uninterpreted function.
     *
     * <p>Z3 has no native trim op and a precise character-level axiom would
     * require existential quantifiers over the leading/trailing whitespace
     * counts (making it hard to solve). We model trim as an uninterpreted
     * function and add two lightweight structural axioms:
     * <ul>
     *   <li>{@code len(trim(s)) ≤ len(s)}
     *   <li>{@code trim(trim(s)) = trim(s)}  (idempotent)
     * </ul>
     * This is sound for path conditions that only test the trimmed result
     * (e.g. {@code s.trim().equals("hello")}); the solver will find a model
     * where the result is the target string.
     */
    private Expr<SeqSort<CharSort>> encodeTrim(Expr<SeqSort<CharSort>> input) {
        SeqSort<CharSort> strSort = sorts.stringSort();
        Sort[]     sig  = { strSort };
        FuncDecl<?> fnTrim = ctx.mkFuncDecl("str.trim", sig, strSort);

        Expr<SeqSort<CharSort>> trimmed =
                (Expr<SeqSort<CharSort>>) ctx.mkApp(fnTrim, input);

        // len(trim(s)) ≤ len(s)
        sideConstraints.add(ctx.mkLe(
                (IntExpr) ctx.mkLength(trimmed),
                (IntExpr) ctx.mkLength(input)));

        // trim(trim(s)) = trim(s)
        Expr<SeqSort<CharSort>> doubleTrimmed =
                (Expr<SeqSort<CharSort>>) ctx.mkApp(fnTrim, trimmed);
        sideConstraints.add(ctx.mkEq(doubleTrimmed, trimmed));

        return trimmed;
    }

    private Expr<SeqSort<CharSort>> stringArg(SymStringOp op,
                                              int index,
                                              IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                              IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (index >= op.args().size()) {
            throw new EncodingException("Missing String argument " + index + " for " + op.op(), op);
        }
        SymbolicValue arg = op.args().get(index);
        return asStringExpr(visit(arg, exprCache, sortCache), sorts.resolve(arg, sortCache), op);
    }

    private IntExpr intIndexArg(SymStringOp op,
                                int index,
                                IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (index >= op.args().size()) {
            throw new EncodingException("Missing index argument " + index + " for " + op.op(), op);
        }
        SymbolicValue arg = op.args().get(index);
        Expr<?> encoded = visit(arg, exprCache, sortCache);
        Sort sort = sorts.resolve(arg, sortCache);
        if (sort.equals(sorts.intSort())) {
            return (IntExpr) encoded;
        }
        if (sort instanceof BitVecSort) {
            return ctx.mkBV2Int((BitVecExpr) encoded, false);
        }
        throw new EncodingException("String index argument must be integral", op);
    }

    private Expr<SeqSort<CharSort>> asStringExpr(Expr<?> expr, Sort sort, SymbolicValue source) {
        if (!sorts.isStringSort(sort)) {
            throw new EncodingException("Expected String sort, got " + sort, source);
        }
        return (Expr<SeqSort<CharSort>>) expr;
    }

    private Expr<SeqSort<CharSort>> asStringConversion(
            Expr<?> expr,
            Sort sort,
            SymbolicValue source,
            SymbolicValue context) {
        if (sorts.isStringSort(sort)) {
            return (Expr<SeqSort<CharSort>>) expr;
        }
        if (source instanceof SymLiteral literal) {
            return ctx.mkString(String.valueOf(literal.value()));
        }
        throw new EncodingException(
                "String conversion for non-literal sort " + sort + " is not supported",
                context);
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
            // JLS §15.19: shift distance is masked to the low 5 bits (int) or 6 bits (long).
            // Z3 BV shifts do NOT do this automatically — a value >= width gives 0 / all-sign-bits,
            // which differs from Java's wrap-around semantics.  We mask the RHS before shifting.
            // The RHS may also have a different BV width than L (e.g. long << int after coercion),
            // so we first truncate/extend it to match L's width, then apply the bit-mask.
            case BLS  -> {
                BitVecExpr dist = matchWidth(r, l.getSortSize());
                yield ctx.mkBVSHL(l, maskShiftDist(dist, l.getSortSize()));
            }
            case BRS  -> {                                          // arithmetic (signed) shift right
                BitVecExpr dist = matchWidth(r, l.getSortSize());
                yield ctx.mkBVASHR(l, maskShiftDist(dist, l.getSortSize()));
            }
            case BURS -> {                                          // logical (unsigned) shift right
                BitVecExpr dist = matchWidth(r, l.getSortSize());
                yield ctx.mkBVLSHR(l, maskShiftDist(dist, l.getSortSize()));
            }
            default -> throw new EncodingException(
                    "Unexpected op in BV encoding: " + op, node);
        };
    }

    // ── Bool / comparison ───────────────────────────────────────────────────

    private BoolExpr encodeBoolBinary(SymBinaryOp.Op op,
                                      Expr<?> l, Expr<?> r,
                                      Sort leftSort, Sort rightSort,
                                      SymBinaryOp node) {
        if (isEqualityComparison(op)) {
            return encodeEquality(op, l, r, leftSort, rightSort, node);
        }

        // For relational numeric comparisons coerce operands to a common BV width first.
        if (isRelationalComparison(op)) {
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

    private BoolExpr encodeEquality(SymBinaryOp.Op op,
                                    Expr<?> l, Expr<?> r,
                                    Sort leftSort, Sort rightSort,
                                    SymBinaryOp node) {
        BoolExpr equality;
        if (leftSort.equals(rightSort)) {
            equality = ctx.mkEq(l, r);
        } else if (canCoerceToBV(leftSort) && canCoerceToBV(rightSort)) {
            BitVecSort target = commonBVSort(leftSort, rightSort);
            equality = ctx.mkEq(
                    coerceToBV(l, leftSort, target),
                    coerceToBV(r, rightSort, target));
        } else {
            throw new EncodingException(
                    "Cannot compare sorts " + leftSort + " and " + rightSort + " with " + op,
                    node);
        }

        return op == SymBinaryOp.Op.NEQ ? ctx.mkNot(equality) : equality;
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
        String freshName = receiverKey(f.receiver(), exprCache, sortCache) + "__" + f.fieldName();
        return ctx.mkConst(freshName, sorts.resolve(f, sortCache));
    }

    private String receiverKey(
            SymbolicValue receiver,
            IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
            IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (receiver instanceof SymVariable variable) return variable.name();
        if (receiver instanceof SymFieldAccess field) {
            return receiverKey(field.receiver(), exprCache, sortCache) + "__" + field.fieldName();
        }
        return visit(receiver, exprCache, sortCache)
                .toString()
                .replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // =========================================================================
    // Coercion helpers  (BV ↔ BV width, BV → FP)
    // =========================================================================

    /**
     * Coerce any integral/FP expression to a {@link BitVecExpr} of {@code target} width,
     * using the Java source type to choose sign-extension vs zero-extension.
     *
     * <p>JLS §5.1.2: widening of {@code char} is zero-extension (it is unsigned 0–65535).
     * All other integral types ({@code byte}, {@code short}, {@code int}) use sign-extension.
     *
     * @param expr      Z3 expression to coerce
     * @param sort      Z3 sort of {@code expr}
     * @param target    desired target BV width
     * @param javaType  Java source type of the operand, or {@code null} if unknown
     *                  (falls back to sign-extension when null)
     */
    private BitVecExpr coerceToBV(Expr<?> expr, Sort sort, BitVecSort target,
                                   SymType javaType) {
        int w = target.getSize();

        if (sort instanceof BitVecSort bvs) {
            int bw = bvs.getSize();
            BitVecExpr bv = (BitVecExpr) expr;
            if (bw == w) return bv;
            if (bw > w)  return ctx.mkExtract(w - 1, 0, bv);
            // Narrower → need to widen; char is zero-extended, everything else sign-extended
            boolean isChar = javaType == PrimitiveSymType.CHAR;
            return isChar
                    ? ctx.mkZeroExt(w - bw, bv)   // JLS §5.1.2 char: unsigned zero-extension
                    : ctx.mkSignExt(w - bw, bv);   // byte / short / int: sign-extension
        }

        if (sort instanceof FPSort) {
            return ctx.mkFPToBV(ctx.mkFPRoundTowardZero(), (FPExpr) expr, w, true);
        }

        if (sort.equals(sorts.intSort())) {
            return ctx.mkInt2BV(w, (Expr<IntSort>) expr);
        }

        throw new IllegalArgumentException("Cannot coerce sort " + sort + " to BitVecSort");
    }

    /**
     * Type-unaware overload retained for call-sites that don't have a Java source type
     * (ITE branch reconciliation, field-access, etc.). Uses sign-extension as a safe
     * default — callers that need char zero-extension must use the four-arg overload.
     */
    private BitVecExpr coerceToBV(Expr<?> expr, Sort sort, BitVecSort target) {
        return coerceToBV(expr, sort, target, null);
    }

    /**
     * Coerce any numeric expression to an FP expression of {@code target} sort.
     *
     * FP (same sort)  → identity
     * FP (other sort) → mkFPToFP (widen or narrow)
     * BitVec          → mkFPToFP (signed BV → FP)
     */
    private FPExpr coerceToFP(Expr<?> expr, Sort sort, FPSort target, SymType javaType) {
        if (sort.equals(target)) return (FPExpr) expr;

        if (sort instanceof FPSort) {
            return ctx.mkFPToFP(RNE, (FPExpr) expr, target);
        }

        if (sort instanceof BitVecSort) {
            // Signed BV → FP via mkFPToFP(rounding, bv, sort, signed=true)
            return ctx.mkFPToFP(RNE, (BitVecExpr) expr, target, javaType != PrimitiveSymType.CHAR);
        }

        throw new IllegalArgumentException("Cannot coerce sort " + sort + " to FPSort");
    }

    private BitVecExpr jlsFpToIntegral(FPExpr fp, FPSort fromSort, int targetWidth) {
        int conversionWidth = targetWidth < 32 ? 32 : targetWidth;
        BitVecExpr converted = ctx.mkFPToBV(
                ctx.mkFPRoundTowardZero(), fp, conversionWidth, true);

        BitVecExpr zero = ctx.mkBV(0, conversionWidth);
        BitVecExpr max = conversionWidth == 64
                ? ctx.mkBV(Long.MAX_VALUE, 64)
                : ctx.mkBV(Integer.MAX_VALUE, 32);
        BitVecExpr min = conversionWidth == 64
                ? ctx.mkBV(Long.MIN_VALUE, 64)
                : ctx.mkBV(Integer.MIN_VALUE, 32);

        BoolExpr isNaN = ctx.mkFPIsNaN(fp);
        BoolExpr tooLarge = ctx.mkFPGt(fp, fpLiteralForSort(
                conversionWidth == 64 ? (double) Long.MAX_VALUE : (double) Integer.MAX_VALUE,
                fromSort));
        BoolExpr tooSmall = ctx.mkFPLt(fp, fpLiteralForSort(
                conversionWidth == 64 ? (double) Long.MIN_VALUE : (double) Integer.MIN_VALUE,
                fromSort));

        BitVecExpr saturated = (BitVecExpr) ctx.mkITE(isNaN, zero,
                ctx.mkITE(tooLarge, max, ctx.mkITE(tooSmall, min, converted)));

        if (targetWidth == conversionWidth) {
            return saturated;
        }
        return ctx.mkExtract(targetWidth - 1, 0, saturated);
    }

    private FPExpr fpLiteralForSort(double value, FPSort sort) {
        if (sort.equals(sorts.fp32Sort())) {
            return (FPExpr) encodeLiteral(SymLiteral.of((float) value));
        }
        return (FPExpr) encodeLiteral(SymLiteral.of(value));
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

    private static boolean isEqualityComparison(SymBinaryOp.Op op) {
        return op == SymBinaryOp.Op.EQ || op == SymBinaryOp.Op.NEQ;
    }

    private static boolean isRelationalComparison(SymBinaryOp.Op op) {
        return switch (op) {
            case SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE -> true;
            default -> false;
        };
    }

    private boolean canCoerceToBV(Sort sort) {
        return sort instanceof BitVecSort
                || sort instanceof FPSort
                || sort.equals(sorts.intSort());
    }

    // =========================================================================
    // Shift-distance helpers  (JLS §15.19)
    // =========================================================================

    /**
     * Adjust {@code dist} to exactly {@code targetWidth} bits so it can be
     * used as the RHS of a Z3 shift on a value of that width.
     *
     * <p>The shift distance operand may arrive with a different BV width than
     * the left operand (e.g. {@code long << int} after generic coercion).  We
     * truncate or zero-extend to match — the subsequent mask makes the high bits
     * irrelevant anyway.
     */
    private BitVecExpr matchWidth(BitVecExpr dist, int targetWidth) {
        int dw = dist.getSortSize();
        if (dw == targetWidth) return dist;
        if (dw > targetWidth)  return ctx.mkExtract(targetWidth - 1, 0, dist);
        return ctx.mkZeroExt(targetWidth - dw, dist);  // zero-extend (distance is unsigned)
    }

    /**
     * Mask a shift-distance BV to the low 5 bits (int, width=32) or 6 bits
     * (long, width=64), implementing JLS §15.19 distance-masking.
     *
     * @param dist   BV already normalised to {@code width} bits by {@link #matchWidth}
     * @param width  width of the value being shifted (32 or 64)
     */
    private BitVecExpr maskShiftDist(BitVecExpr dist, int width) {
        long maskVal = (width == 64) ? 0x3FL : 0x1FL;
        return ctx.mkBVAND(dist, ctx.mkBV(maskVal, width));
    }
}
