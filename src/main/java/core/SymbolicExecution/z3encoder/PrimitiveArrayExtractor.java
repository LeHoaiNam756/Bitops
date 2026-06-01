package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
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

final class PrimitiveArrayExtractor {

    private final SortResolver sorts;

    PrimitiveArrayExtractor(SortResolver sorts) {
        this.sorts = sorts;
    }

    Optional<SymLiteral> extract(Model model, FuncDecl<?> decl, Map<String, SymLiteral> scalarBindings) {
        if (decl.getArity() != 0) return Optional.empty();

        String name = decl.getName().toString();
        if (!(decl.getRange() instanceof ArraySort<?, ?> arraySort)) return Optional.empty();
        if (!arraySort.getDomain().equals(sorts.intSort())) return Optional.empty();
        if (arraySort.getRange() instanceof ArraySort<?, ?>) return Optional.empty();

        Optional<Integer> length = arrayLength(name, scalarBindings);
        if (length.isEmpty()) return Optional.empty();

        Sort range = arraySort.getRange();
        Expr<?> array = sorts.ctx().mkConst(name, decl.getRange());

        if (range.equals(sorts.intSort())) return extractLongArray(model, array, length.get());
        if (range.equals(sorts.boolSort())) return extractBooleanArray(model, array, length.get());
        if (range.equals(sorts.fp32Sort())) return extractFloatArray(model, array, length.get(), range);
        if (range.equals(sorts.fp64Sort())) return extractDoubleArray(model, array, length.get(), range);

        return Optional.empty();
    }

    private Optional<Integer> arrayLength(String name, Map<String, SymLiteral> scalarBindings) {
        SymLiteral lengthLiteral = scalarBindings.get(name + "__length");
        if (lengthLiteral == null || !(lengthLiteral.value() instanceof Number number)) return Optional.empty();

        long length = number.longValue();
        if (length < 0 || length > Integer.MAX_VALUE) return Optional.empty();
        return Optional.of((int) length);
    }

    private Optional<SymLiteral> extractLongArray(Model model, Expr<?> array, int length) {
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            Expr<?> value = evalElement(model, array, i);
            if (!(value instanceof IntNum intNum)) return Optional.empty();
            Optional<Long> converted = toLong(intNum);
            if (converted.isEmpty()) return Optional.empty();
            values[i] = converted.get();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Optional<Long> toLong(IntNum intNum) {
        BigInteger value = intNum.getBigInteger();
        if (value.bitLength() >= Long.SIZE) return Optional.empty();
        return Optional.of(value.longValue());
    }

    private Optional<SymLiteral> extractBooleanArray(Model model, Expr<?> array, int length) {
        boolean[] values = new boolean[length];
        for (int i = 0; i < length; i++) {
            Expr<?> value = evalElement(model, array, i);
            if (value == null) return Optional.empty();
            if (value.isTrue()) values[i] = true;
            else if (value.isFalse()) values[i] = false;
            else return Optional.empty();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Optional<SymLiteral> extractFloatArray(Model model, Expr<?> array, int length, Sort range) {
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            Expr<?> value = evalElement(model, array, i);
            if (!(value instanceof FPNum fp)) return Optional.empty();
            Optional<Float> converted = extractFloat(fp, range);
            if (converted.isEmpty()) return Optional.empty();
            values[i] = converted.get();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Optional<SymLiteral> extractDoubleArray(Model model, Expr<?> array, int length, Sort range) {
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            Expr<?> value = evalElement(model, array, i);
            if (!(value instanceof FPNum fp)) return Optional.empty();
            Optional<Double> converted = extractDouble(fp, range);
            if (converted.isEmpty()) return Optional.empty();
            values[i] = converted.get();
        }
        return Optional.of(new SymLiteral(values));
    }

    private Expr<?> evalElement(Model model, Expr<?> array, int index) {
        return model.eval(sorts.ctx().mkSelect((ArrayExpr) array, sorts.ctx().mkInt(index)), true);
    }

    private Optional<Float> extractFloat(FPNum fp, Sort sort) {
        if (!sort.equals(sorts.fp32Sort())) return Optional.empty();
        try {
            boolean positive = fp.isPositive();
            long expBits = Long.parseLong(fp.getExponent(false));
            long sigBits = Long.parseLong(fp.getSignificand());
            int bits = (positive ? 0 : (1 << 31))
                    | ((int) (expBits & 0xFF) << 23)
                    | (int) (sigBits & 0x7F_FFFF);
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
                double numerator = Double.parseDouble(text.substring(0, slash));
                double denominator = Double.parseDouble(text.substring(slash + 1));
                return Optional.of(numerator / denominator);
            }
            return Optional.of(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
