package core.SymbolicExecution;

import com.microsoft.z3.*;
import java.lang.reflect.Array;

public class ArrayZ3Parser {

   public static Object parseArrayFromModel(Context ctx, Model model, Expr<?> arrayExpr,
                                         int length, Sort elementSort) {
    if (length < 0) {
        throw new IllegalArgumentException("Array length must be non-negative, got: " + length);
    }

    ArraySort arraySort = (ArraySort) arrayExpr.getSort();
    int idxBits = ((BitVecSort) arraySort.getDomain()).getSize();

    Object result = Array.newInstance(sortToJavaClass(elementSort), length);

    for (int i = 0; i < length; i++) {
        BitVecExpr idx = ctx.mkBV(i, idxBits);
        Expr<?> selectExpr = ctx.mkSelect((ArrayExpr) arrayExpr, idx);
        Expr<?> elemValue = model.evaluate(selectExpr, true);
        Object javaValue = convertElementToJava(elemValue, elementSort);
        Array.set(result, i, javaValue);
    }
    return result;
}

    public static int parseArrayLength(Model model, Expr<?> lengthExpr) {
        Expr<?> evaluated = model.evaluate(lengthExpr, true);
        if (evaluated instanceof BitVecNum) {
            return ((BitVecNum) evaluated).getBigInteger().intValue();
        }
        throw new RuntimeException("Cannot evaluate array length expression: " + lengthExpr);
    }

    private static Object convertElementToJava(Expr<?> elem, Sort sort) {
        if (elem instanceof BitVecNum) {
            BitVecNum bvNum = (BitVecNum) elem;
            int size = ((BitVecSort) sort).getSize();
            switch (size) {
                case 8:  return bvNum.getBigInteger().byteValue();
                case 16: return bvNum.getBigInteger().shortValue();
                case 32: return bvNum.getBigInteger().intValue();
                case 64: return bvNum.getBigInteger().longValue();
                default: throw new RuntimeException("Unsupported bitvector size: " + size);
            }
        } else if (elem instanceof FPNum) {
            FPNum fpNum = (FPNum) elem;
            FPSort fpSort = (FPSort) sort;
            if (fpSort.getEBits() == 8 && fpSort.getSBits() == 24) {
                if (fpNum.isNaN()) return Float.NaN;
                if (fpNum.isInf()) return fpNum.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                if (fpNum.toString().contains("/")) {
                    String[] split = fpNum.toString().split("/");
                    Float numerator = Float.parseFloat(split[0]);
                    Float denominator = Float.parseFloat(split[1]);
                    return numerator / denominator;
                }
                return Float.parseFloat(fpNum.toString());
            } else {
                if (fpNum.isNaN()) return Double.NaN;
                if (fpNum.isInf()) return fpNum.isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                if (fpNum.toString().contains("/")) {
                    String[] split = fpNum.toString().split("/");
                    Double numerator = Double.parseDouble(split[0]);
                    Double denominator = Double.parseDouble(split[1]);
                    return numerator / denominator;
                }
                return Double.parseDouble(fpNum.toString());
            }
        } else if (elem instanceof BoolExpr) {
            return "true".equals(elem.toString());
        } else {
            throw new RuntimeException("Unsupported element type in array model: " + elem.getClass());
        }
    }

    private static Class<?> sortToJavaClass(Sort sort) {
        if (sort instanceof BitVecSort) {
            int size = ((BitVecSort) sort).getSize();
            switch (size) {
                case 8:  return byte.class;
                case 16: return short.class;
                case 32: return int.class;
                case 64: return long.class;
                default: throw new RuntimeException("Unsupported bitvector size: " + size);
            }
        } else if (sort instanceof FPSort) {
            FPSort fpSort = (FPSort) sort;
            if (fpSort.getEBits() == 8 && fpSort.getSBits() == 24) return float.class;
            return double.class;
        } else if (sort instanceof BoolSort) {
            return boolean.class;
        } else {
            throw new RuntimeException("Unsupported array element sort: " + sort);
        }
    }
}
