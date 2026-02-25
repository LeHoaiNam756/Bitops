package core.SymbolicExecution.Variable;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPSort;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import core.SymbolicExecution.TypedExpr;

import static org.eclipse.jdt.core.dom.PrimitiveType.*;

public class PrimitiveVariable extends Variable{
    private final PrimitiveType primitiveType;

    public PrimitiveVariable(PrimitiveType primitiveType, String name) {
        this.setName(name);
        this.primitiveType = primitiveType;
    }

    public Code getCode() {
        return primitiveType.getPrimitiveTypeCode();
    }

    @Override
    public Type getType() {
        return primitiveType;
    }

    @Override
    public Expr<?> createZ3Expr(Context context) {
        Code code = this.getCode();
        String name = this.getName();
        Expr<?> expr;
        if (code.equals(BYTE)) {
            expr = context.mkBVConst(name, 8);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.BYTE);
        } else if (code.equals(CHAR)) {
            expr = context.mkBVConst(name, 16);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.CHAR);
        } else if (code.equals(SHORT)) {
            expr = context.mkBVConst(name, 16);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.SHORT);
        } else if (code.equals(INT)) {
            expr = context.mkBVConst(name, 32);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.INT);
        } else if (code.equals(LONG)) {
            expr = context.mkBVConst(name, 64);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.LONG);
        } else if (code.equals(FLOAT)) {
            FPSort f32 = context.mkFPSort32();
            expr = context.mkConst(name, f32);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.FLOAT);
        } else if (code.equals(DOUBLE)) {
            FPSort f64 = context.mkFPSort64();
            expr = context.mkConst(name, f64);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.DOUBLE);
        } else if (code.equals(BOOLEAN)) {
            expr = context.mkBoolConst(name);
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.BOOLEAN);
        } else {
            throw new IllegalArgumentException("Invalid type: " + code);
        }
        return expr;
    }
}
