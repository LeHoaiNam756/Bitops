package core.SymbolicExecution.execution;

import com.microsoft.z3.*;

public class ModelExtractor {
    private final Context ctx;

    public ModelExtractor(Context ctx) {
        this.ctx = ctx;
    }

    public Object extractPrimitive(Model model, Expr<?> expr, Class<?> type) {
        Expr<?> evaluated = model.evaluate(expr, true);
        if (evaluated instanceof BitVecNum) {
            BitVecNum num = (BitVecNum) evaluated;
            if (type == int.class) {
                return num.getBigInteger().intValue();
            }
            if (type == long.class) {
                return num.getBigInteger().longValue();
            }
            if (type == short.class) {
                return num.getBigInteger().shortValue();
            }
            if (type == byte.class) {
                return num.getBigInteger().byteValue();
            }
            if (type == char.class) {
                return (char) num.getBigInteger().intValue();
            }
        }
        if (evaluated instanceof BoolExpr && type == boolean.class) {
            return Boolean.parseBoolean(evaluated.toString());
        }
        throw new IllegalArgumentException("Unsupported type extraction: " + type);
    }
}
