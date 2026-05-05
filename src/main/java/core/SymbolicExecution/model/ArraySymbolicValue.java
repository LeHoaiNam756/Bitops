package core.SymbolicExecution.model;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;

public final class ArraySymbolicValue extends SymbolicValue {
    private final TypedExpr.JavaType elementType;
    private final int dimensions;

    private ArraySymbolicValue(String name,
                               TypedExpr.JavaType elementType,
                               Expr<?> expr,
                               boolean parameter,
                               int dimensions) {
        super(name, TypedExpr.JavaType.OTHER, expr, parameter);
        this.elementType = elementType;
        this.dimensions = dimensions;
    }

    public static ArraySymbolicValue of(String name,
                                        TypedExpr.JavaType elementType,
                                        Expr<?> expr,
                                        boolean parameter,
                                        int dimensions) {
        return new ArraySymbolicValue(name, elementType, expr, parameter, dimensions);
    }

    public TypedExpr.JavaType getElementType() {
        return elementType;
    }

    public int getDimensions() {
        return dimensions;
    }
}
