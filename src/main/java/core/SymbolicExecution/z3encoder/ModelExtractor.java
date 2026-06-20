package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.SymLiteral;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracts a {@link Z3ModelBindings} from a Z3 {@link Model} after SAT.
 *
 * What it does:
 * When Z3 returns SAT, its model assigns a concrete value to every declared
 * constant.  This class reads those assignments and converts them back into
 * {@link SymLiteral} values that
 * {@link core.SymbolicExecution.simplifier.ModelBasedSimplifier}
 * can substitute into sibling-branch constraints.
 *
 * Type mapping (all integers are now BitVec):
 *  Z3 BitVecNum (32-bit)    →  SymLiteral(int)
 *  Z3 BitVecNum (64-bit)    →  SymLiteral(long)
 *  Z3 BitVecNum (other)     →  SymLiteral(long)  (widened)
 *  Z3 RatNum                →  SymLiteral(double) (rational → closest double)
 *  Z3 BoolVal (true)        →  SymLiteral(true)
 *  Z3 BoolVal (false)       →  SymLiteral(false)
 *  Z3 FPNum (fp32)          →  SymLiteral(float)
 *  Z3 FPNum (fp64)          →  SymLiteral(double)
 *  Z3 String value          →  SymLiteral(String)
 *  anything else            →  skipped (not representable as a Java literal)
 *
 * Note: IntNum no longer appears for Java integer variables because SortResolver
 * maps all integral types to BitVec.  RatNum may still appear for intermediate
 * real-sort expressions if any survive into the model.
 *
 * Only zero-arity declarations (plain constants) are extracted.
 * Primitive 1-D arrays are reconstructed by {@link PrimitiveArrayExtractor}.
 *
 * Usage:
 * <pre>
 *   Status status = solver.check();
 *   if (status == Status.SATISFIABLE) {
 *       Model           model    = solver.getModel();
 *       Z3ModelBindings bindings = extractor.extract(model);
 *   }
 * </pre>
 */
public final class ModelExtractor {

    private final SortResolver          sorts;
    private final PrimitiveArrayExtractor primitiveArrayExtractor;

    public ModelExtractor(SortResolver sorts) {
        this.sorts = sorts;
        this.primitiveArrayExtractor = new PrimitiveArrayExtractor(sorts);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Extract all constant assignments from {@code model}.
     *
     * @param model a Z3 Model (only valid while the Solver's stack is unchanged)
     * @return immutable bindings; may be empty if no constants were declared
     */
    public Z3ModelBindings extract(Model model) {
        Map<String, SymLiteral> bindings = new HashMap<>();

        FuncDecl<?>[] decls = model.getConstDecls();
        if (decls == null) return Z3ModelBindings.empty();

        // Pass 1: scalar constants
        for (FuncDecl<?> decl : decls) {
            if (decl.getArity() != 0) continue;

            String  name   = decl.getName().toString();
            Expr<?> interp = model.getConstInterp(decl);
            if (interp == null) continue;

            toSymLiteral(interp, decl.getRange())
                    .ifPresent(lit -> bindings.put(name, lit));
        }

        // Pass 2: primitive arrays (need scalar bindings for length lookup)
        for (FuncDecl<?> decl : decls) {
            if (decl.getArity() != 0) continue;
            primitiveArrayExtractor.extract(model, decl, bindings)
                    .ifPresent(lit -> bindings.put(decl.getName().toString(), lit));
        }

        return new Z3ModelBindings(bindings);
    }

    // =========================================================================
    // Z3 Expr → SymLiteral conversion
    // =========================================================================

    private java.util.Optional<SymLiteral> toSymLiteral(Expr<?> expr, Sort sort) {

        // ── Boolean ───────────────────────────────────────────────────────────
        if (expr.isBool()) {
            if (expr.isTrue())  return java.util.Optional.of(SymLiteral.of(true));
            if (expr.isFalse()) return java.util.Optional.of(SymLiteral.of(false));
        }
        // IntSort constants: these should not appear for Java int/long variables
        // (those must be declared as bv32/bv64 via SortResolver.symTypeToSort), but
        // they DO appear when the varSorts builder uses ctx.getIntSort() directly —
        // e.g. for length variables (nums__length) or any scalar registered outside
        // symTypeToSort.  We extract them here so they are not silently dropped.
        // Note: array-index expressions never appear as model *constants*; this
        // branch handles the scalar mis-declaration case exclusively.
        if (expr instanceof IntNum intNum) {
            long val = intNum.getInt64();
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return java.util.Optional.of(SymLiteral.of((int) val));
            }
            return java.util.Optional.of(SymLiteral.of(val));
        }

        // ── BitVec (all Java integral types) ──────────────────────────────────
        if (expr instanceof BitVecNum bv) {
            int width = ((BitVecSort) sort).getSize();
            long val  = bv.getLong();   // signed two's-complement interpretation
            if (width <= 32)
                return java.util.Optional.of(SymLiteral.of((int) val));
            return java.util.Optional.of(SymLiteral.of(val));
        }

        // ── Rational (may appear for real-sort intermediates) ─────────────────
        if (expr instanceof RatNum rat) {
            double numerator   = rat.getNumerator().getInt64();
            double denominator = rat.getDenominator().getInt64();
            return java.util.Optional.of(SymLiteral.of(numerator / denominator));
        }

        // ── Floating-point ────────────────────────────────────────────────────
        if (expr instanceof FPNum fp) {
            return extractFP(fp, sort);
        }

        // ── String ────────────────────────────────────────────────────────────
        if (expr.isString()) {
            return java.util.Optional.of(SymLiteral.of(expr.getString()));
        }

        // Anything else (arrays, UFs, algebraic numbers…) → skip
        return java.util.Optional.empty();
    }

    /**
     * Convert a Z3 FPNum to float or double depending on the FP sort width.
     * Reconstructs the IEEE 754 bit pattern from sign + exponent + significand.
     */
    private java.util.Optional<SymLiteral> extractFP(FPNum fp, Sort sort) {
        try {
            boolean positive = !fp.getSign();
            long    expBits  = Long.parseLong(fp.getExponent(false));
            long    sigBits  = Long.parseLong(fp.getSignificand());

            if (sort.equals(sorts.fp32Sort())) {
                int bits = (positive ? 0 : (1 << 31))
                         | ((int)(expBits & 0xFF) << 23)
                         | (int)(sigBits & 0x7F_FFFF);
                return java.util.Optional.of(SymLiteral.of(Float.intBitsToFloat(bits)));
            }

            if (sort.equals(sorts.fp64Sort())) {
                long bits = (positive ? 0L : (1L << 63))
                          | ((expBits & 0x7FFL) << 52)
                          | (sigBits & 0x000F_FFFF_FFFF_FFFFL);
                return java.util.Optional.of(SymLiteral.of(Double.longBitsToDouble(bits)));
            }

        } catch (Exception e) {
            System.err.println("[ModelExtractor] Could not extract FP value: " + e.getMessage());
        }
        return java.util.Optional.empty();
    }
}