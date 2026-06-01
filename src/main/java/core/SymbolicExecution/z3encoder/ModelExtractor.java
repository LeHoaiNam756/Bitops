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
 * {@link SymLiteral} values that {@link core.SymbolicExecution.simplifier.ModelBasedSimplifier}
 * can substitute into sibling-branch constraints.
 *
 * Type mapping:
 *  Z3 IntNum            →  SymLiteral(int)   (or long if value overflows int)
 *  Z3 RatNum            →  SymLiteral(double) (rational → closest double)
 *  Z3 BoolVal (true)    →  SymLiteral(true)
 *  Z3 BoolVal (false)   →  SymLiteral(false)
 *  Z3 FPNum (fp32)      →  SymLiteral(float)
 *  Z3 FPNum (fp64)      →  SymLiteral(double)
 *  Z3 BitVecNum         →  SymLiteral(int) or SymLiteral(long)
 *  anything else        →  skipped (not representable as a Java literal)
 *
 * Only zero-arity declarations (plain constants, no arguments) are extracted.
 * Primitive 1-D arrays are reconstructed from their length binding and element
 * selections; unsupported arrays and function interpretations are skipped.
 *
 * Usage:
 * <pre>
 *   Status status = solver.check();
 *   if (status == Status.SATISFIABLE) {
 *       Model           model    = solver.getModel();
 *       Z3ModelBindings bindings = extractor.extract(model);
 *       // feed to ModelBasedSimplifier for sibling branches
 *   }
 * </pre>
 */
public final class ModelExtractor {

    private final SortResolver sorts;
    private final PrimitiveArrayExtractor primitiveArrayExtractor;

    public ModelExtractor(SortResolver sorts) {
        this.sorts = sorts;
        this.primitiveArrayExtractor = new PrimitiveArrayExtractor(sorts);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Extract all constant assignments from {@code model} and return them
     * as a {@link Z3ModelBindings} snapshot.
     *
     * @param model a Z3 Model (only valid while the Solver's stack is unchanged)
     * @return immutable bindings; may be empty if no constants were declared
     */
    public Z3ModelBindings extract(Model model) {
        Map<String, SymLiteral> bindings = new HashMap<>();

        FuncDecl<?>[] decls = model.getConstDecls();
        if (decls == null) return Z3ModelBindings.empty();

        for (FuncDecl<?> decl : decls) {
            // Only plain constants (arity == 0); skip function symbols
            if (decl.getArity() != 0) continue;

            String  name  = decl.getName().toString();
            Expr<?> interp = model.getConstInterp(decl);
            if (interp == null) continue;

            toSymLiteral(interp, decl.getRange())
                    .ifPresent(lit -> bindings.put(name, lit));
        }

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

    /**
     * Convert a Z3 model value to a SymLiteral.
     * Returns empty if the value cannot be represented as a Java primitive.
     */
    private java.util.Optional<SymLiteral> toSymLiteral(Expr<?> expr, Sort sort) {

        // ── Boolean ───────────────────────────────────────────────────────────
        if (expr.isBool()) {
            if (expr.isTrue())  return java.util.Optional.of(SymLiteral.of(true));
            if (expr.isFalse()) return java.util.Optional.of(SymLiteral.of(false));
        }

        // ── Integer ───────────────────────────────────────────────────────────
        if (expr instanceof IntNum intNum) {
            long val = intNum.getInt64();   // Z3 integers are unbounded; clamp to long
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE)
                return java.util.Optional.of(SymLiteral.of((int) val));
            return java.util.Optional.of(SymLiteral.of(val));
        }

        // ── Rational (RealSort) ───────────────────────────────────────────────
        if (expr instanceof RatNum rat) {
            // Convert p/q to double (may lose precision for irrational-like rationals)
            double numerator   = rat.getNumerator().getInt64();
            double denominator = rat.getDenominator().getInt64();
            return java.util.Optional.of(SymLiteral.of(numerator / denominator));
        }

        // ── Floating-point ────────────────────────────────────────────────────
        if (expr instanceof FPNum fp) {
            return extractFP(fp, sort);
        }

        // ── BitVector ─────────────────────────────────────────────────────────
        if (expr instanceof BitVecNum bv) {
            int width = ((BitVecSort) sort).getSize();
            long val  = bv.getLong();    // signed interpretation
            if (width <= 32)
                return java.util.Optional.of(SymLiteral.of((int) val));
            return java.util.Optional.of(SymLiteral.of(val));
        }

        // Anything else (arrays, UFs, algebraic numbers…) → skip
        return java.util.Optional.empty();
    }

    /**
     * Convert a Z3 FPNum to float or double depending on the FP sort width.
     *
     * Z3's FPNum.toString() produces SMT-LIB format; we reconstruct the
     * IEEE 754 bit pattern directly from sign + exponent + significand.
     */
    private java.util.Optional<SymLiteral> extractFP(FPNum fp, Sort sort) {
        try {
            boolean positive = !fp.getSign();     // Z3: true = negative
            long    expBits  = Long.parseLong(fp.getExponent(false));   // biased exponent
            long    sigBits  = Long.parseLong(fp.getSignificand()); // significand (no hidden bit)

            if (sort.equals(sorts.fp32Sort())) {
                // Reconstruct 32-bit float from components
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
            // FP extraction failure: log and skip
            System.err.println("[ModelExtractor] Could not extract FP value: " + e.getMessage());
        }
        return java.util.Optional.empty();
    }
}
