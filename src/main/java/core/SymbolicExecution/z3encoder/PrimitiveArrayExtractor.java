package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPNum;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Sort;
import core.SymbolicExecution.model.SymLiteral;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

/**
 * Extracts primitive array values from a Z3 model after SAT.
 *
 * Arrays are declared with IntSort index (see {@link SortResolver#symTypeToSort})
 * so element evaluation still uses {@code ctx.mkInt(i)} as the index expression.
 * Scalar (length) bindings now arrive as {@link BitVecNum} values because all
 * Java integer variables are encoded as BitVec.
 */
final class PrimitiveArrayExtractor {

    private final SortResolver sorts;

    PrimitiveArrayExtractor(SortResolver sorts) {
        this.sorts = sorts;
    }

    Optional<SymLiteral> extract(Model model, FuncDecl<?> decl,
                                  Map<String, SymLiteral> scalarBindings) {
        if (decl.getArity() != 0) return Optional.empty();

        String name = decl.getName().toString();
        if (!(decl.getRange() instanceof ArraySort<?, ?> arraySort)) return Optional.empty();
        // Index must be IntSort (how we declare arrays)
        if (!arraySort.getDomain().equals(sorts.intSort())) return Optional.empty();
        // Skip nested/multi-dim arrays
        if (arraySort.getRange() instanceof ArraySort<?, ?>) return Optional.empty();

        Optional<Integer> length = arrayLength(name, scalarBindings);
        if (length.isEmpty()) return Optional.empty();

        Sort  range = arraySort.getRange();
        Expr<?> array = model.getConstInterp(decl);
        if (array == null) {
            array = sorts.ctx().mkConst(name, decl.getRange());
        }

        if (range.equals(sorts.bv32Sort()) || range.equals(sorts.bv64Sort()))
            return extractBVArray(model, array, length.get(), range);
        // IntSort element range: array was declared with IntSort elements instead of
        // bv32Sort (happens when the varSorts builder uses ctx.getIntSort() for int[]).
        // We extract IntNum elements and return them as int[] / long[] depending on value.
        if (range.equals(sorts.intSort()))
            return extractIntSortArray(model, array, length.get());
        if (range.equals(sorts.boolSort()))
            return extractBooleanArray(model, array, length.get());
        if (range.equals(sorts.fp32Sort()))
            return extractFloatArray(model, array, length.get(), range);
        if (range.equals(sorts.fp64Sort()))
            return extractDoubleArray(model, array, length.get(), range);

        return Optional.empty();
    }

    // =========================================================================
    // Length lookup
    // =========================================================================

    private Optional<Integer> arrayLength(String name, Map<String, SymLiteral> scalarBindings) {
        SymLiteral lengthLiteral = scalarBindings.get(name + "__length");
        if (lengthLiteral == null || !(lengthLiteral.value() instanceof Number number))
            return Optional.empty();

        long length = number.longValue();
        if (length < 0 || length > Integer.MAX_VALUE) return Optional.empty();
        return Optional.of((int) length);
    }

    // =========================================================================
    // Element extraction per sort
    // =========================================================================

    /** Handles both bv32 (→ int[]) and bv64 (→ long[]) element sorts. */
    private Optional<SymLiteral> extractBVArray(Model model, Expr<?> array,
                                                 int length, Sort range) {
        boolean is64 = range.equals(sorts.bv64Sort());
        if (is64) {
            long[] values = new long[length];
            for (int i = 0; i < length; i++) {
                Expr<?> val = evalElement(model, array, i);
                if (!(val instanceof BitVecNum bvn)) return Optional.empty();
                values[i] = bvn.getBigInteger().longValue();
            }
            return Optional.of(new SymLiteral(values));
        } else {
            // BitVecNum is unsigned; truncating its low bits restores the signed
            // two's-complement Java int value.
            int[] ints = new int[length];
            for (int i = 0; i < length; i++) {
                Expr<?> val = evalElement(model, array, i);
                if (!(val instanceof BitVecNum bvn)) return Optional.empty();
                ints[i] = bvn.getBigInteger().intValue();
            }
            return Optional.of(new SymLiteral(ints));
        }
    }

    private Optional<SymLiteral> extractBooleanArray(Model model, Expr<?> array, int length) {
        boolean[] values = new boolean[length];
        for (int i = 0; i < length; i++) {
            Expr<?> val = evalElement(model, array, i);
            if (val == null)        return Optional.empty();
            if (val.isTrue())       values[i] = true;
            else if (val.isFalse()) values[i] = false;
            else                    return Optional.empty();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Optional<SymLiteral> extractFloatArray(Model model, Expr<?> array,
                                                    int length, Sort range) {
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            Expr<?> val = evalElement(model, array, i);
            if (!(val instanceof FPNum fp)) return Optional.empty();
            Optional<Float> converted = extractFloat(fp, range);
            if (converted.isEmpty()) return Optional.empty();
            values[i] = converted.get();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Optional<SymLiteral> extractDoubleArray(Model model, Expr<?> array,
                                                     int length, Sort range) {
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            Expr<?> val = evalElement(model, array, i);
            if (!(val instanceof FPNum fp)) return Optional.empty();
            Optional<Double> converted = extractDouble(fp, range);
            if (converted.isEmpty()) return Optional.empty();
            values[i] = converted.get();
        }
        return Optional.of(new SymLiteral(values));
    }

    // =========================================================================
    // IntSort element extraction  (legacy / mis-declared arrays)
    // =========================================================================

    /**
     * Extracts elements from an array whose element sort is IntSort rather than
     * the expected bv32Sort.  This happens when the varSorts builder registers
     * int-array elements as {@code ctx.getIntSort()} instead of going through
     * {@link SortResolver#symTypeToSort}.
     *
     * <p>Values that fit in a signed 32-bit range are returned as {@code int[]};
     * values that require more bits are returned as {@code long[]}.
     */
    private Optional<SymLiteral> extractIntSortArray(Model model, Expr<?> array, int length) {
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            Expr<?> val = evalElement(model, array, i);
            if (val instanceof IntNum intNum) {
                BigInteger integer = intNum.getBigInteger();
                if (integer.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                        || integer.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                    return Optional.empty();
                }
                values[i] = integer.longValue();
            } else if (val instanceof BitVecNum bvn) {
                // Defensive: solver may simplify to BV even when sort is Int
                values[i] = bvn.getBigInteger().longValue();
            } else {
                return Optional.empty();
            }
        }
        // Prefer int[] when all values fit in signed 32-bit range
        boolean allInt = true;
        for (long v : values) {
            if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) { allInt = false; break; }
        }
        if (allInt) {
            int[] ints = new int[length];
            for (int i = 0; i < length; i++) ints[i] = (int) values[i];
            return Optional.of(new SymLiteral(ints));
        }
        return Optional.of(new SymLiteral(values));
    }

    // =========================================================================
    // Element evaluation  (array index stays IntSort per Z3 array theory)
    // =========================================================================
    private Expr<?> evalElement(Model model, Expr<?> array, int index) {
        Expr<?> select = sorts.ctx().mkSelect((ArrayExpr) array, sorts.ctx().mkInt(index));
        // Evaluate against the model and explicitly simplify the expression tree
        return model.eval(select, true).simplify();
    }

    // =========================================================================
    // FP extraction helpers
    // =========================================================================

    private Optional<Float> extractFloat(FPNum fp, Sort sort) {
        if (!sort.equals(sorts.fp32Sort())) return Optional.empty();
        try {
            boolean positive = fp.isPositive();
            long expBits = Long.parseLong(fp.getExponent(false));
            long sigBits = Long.parseLong(fp.getSignificand());
            int bits = (positive ? 0 : (1 << 31))
                    | ((int)(expBits & 0xFF) << 23)
                    | (int)(sigBits & 0x7F_FFFF);
            return Optional.of(Float.intBitsToFloat(bits));
        } catch (Exception e) {
            return parseFiniteDecimal(fp).map(Double::floatValue);
        }
    }

    private Optional<Double> extractDouble(FPNum fp, Sort sort) {
        if (!sort.equals(sorts.fp64Sort())) return Optional.empty();
        try {
            boolean positive = fp.isPositive();
            long expBits = Long.parseLong(fp.getExponent(false));
            long sigBits = Long.parseLong(fp.getSignificand());
            long bits = (positive ? 0L : (1L << 63))
                    | ((expBits & 0x7FFL) << 52)
                    | (sigBits & 0x000F_FFFF_FFFF_FFFFL);
            return Optional.of(Double.longBitsToDouble(bits));
        } catch (Exception e) {
            return parseFiniteDecimal(fp);
        }
    }

    private Optional<Double> parseFiniteDecimal(FPNum fp) {
        String text = fp.toString();
        try {
            int slash = text.indexOf('/');
            if (slash >= 0) {
                double num = Double.parseDouble(text.substring(0, slash));
                double den = Double.parseDouble(text.substring(slash + 1));
                return Optional.of(num / den);
            }
            return Optional.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
