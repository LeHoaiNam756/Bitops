package core.SymbolicExecution;

import com.microsoft.z3.Expr;
import java.util.HashMap;

public class TypedExpr {
    public enum JavaType {
        INT,
        FLOAT,
        DOUBLE,
        BOOLEAN,
        CHAR,
        STRING,
        LONG,
        SHORT,
        BYTE,
        OTHER
    }

    private static TypedExpr instance = null;
    private HashMap<Expr<?>, JavaType> exprTypeMap;

    private TypedExpr() {
        exprTypeMap = new HashMap<>();
    }

    public void put(Expr<?> expr, JavaType type) {
        exprTypeMap.put(expr, type);
    }

    public static JavaType getType(int numOfBits) {
        return getType(numOfBits, false, false);
    }

    public static JavaType getType(int numOfBits, boolean isFP) {
        return getType(numOfBits, isFP, false);
    }

    public static JavaType getType(int numOfBits, boolean isFP, boolean isChar) {
        switch (numOfBits) {
            case 1:
                return JavaType.BOOLEAN;
            case 8:
                return JavaType.BYTE;
            case 16:
                return isChar ? JavaType.CHAR : JavaType.SHORT;
            case 32:
                return isFP ? JavaType.FLOAT : JavaType.INT;
            case 64:
                return isFP ? JavaType.DOUBLE : JavaType.LONG;
            default:
                return JavaType.OTHER;
        }
    }

    public JavaType getType(Expr<?> expr) {
        return exprTypeMap.get(expr);
    }

    public void clear() {
        exprTypeMap.clear();
    }

    public static TypedExpr getInstance() {
        if (instance == null) {
            instance = new TypedExpr();
        }
        return instance;
    }
}