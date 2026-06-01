package core.SymbolicExecution.model;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;
@Deprecated
public class SymbolicValueTemp {
    private final String name;
    private final TypedExpr.JavaType type;
    private final Expr<?> expr;
    private final boolean parameter;

    protected SymbolicValueTemp(String name, TypedExpr.JavaType type, Expr<?> expr, boolean parameter) {
        this.name = name;
        this.type = type;
        this.expr = expr;
        this.parameter = parameter;
    }

    public static SymbolicValueTemp of(String name, TypedExpr.JavaType type, Expr<?> expr, boolean parameter) {
        return new SymbolicValueTemp(name, type, expr, parameter);
    }

    public String getName() {
        return name;
    }

    public TypedExpr.JavaType getType() {
        return type;
    }

    public Expr<?> getExpr() {
        return expr;
    }

    public boolean isParameter() {
        return parameter;
    }
}
