package core.SymbolicExecution;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPSort;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValueTemp;
import org.eclipse.jdt.core.dom.PrimitiveType;

import java.util.HashMap;
import java.util.Map;

public class MemoryModel {
    private final SymbolicStore store = new SymbolicStore();
    private final Map<String, AstNode> values = new HashMap<>();
    private final Context ctx;

    public MemoryModel() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        this.ctx = new Context(cfg);
    }

    public Context getContext() {
        return ctx;
    }

    public void clear() {
        values.clear();
    }

    public int size() {
        return store.size();
    }

    public AstNode accessVariable(String name) {
        return values.get(name);
    }

    public void declareVariable(SymbolicValueTemp variable, AstNode node) {
        store.declare(variable.toString(), variable);
        values.put(variable.toString(), node);
    }

    public void assignVariable(String variable, AstNode node) {
        values.put(variable, node);
    }

    public boolean containsVariable(String name) {
        return store.contains(name);
    }

    public SymbolicValueTemp getVariable(String name) {
        return null;
    }

    public SymbolicValueTemp getVariableByValue(AstNode value) {
        for (Map.Entry<String, AstNode> entry : values.entrySet()) {
            if (entry.getValue() == value) {
                return null;
            }
        }
        return null;
    }

    public static Expr<?> createZ3ExprFromType(String name, TypedExpr.JavaType type, Context context) {
        switch (type) {
            case BYTE:
                return context.mkBVConst(name, 8);
            case CHAR:
                return context.mkBVConst(name, 16);
            case SHORT:
                return context.mkBVConst(name, 16);
            case INT:
                return context.mkBVConst(name, 32);
            case LONG:
                return context.mkBVConst(name, 64);
            case FLOAT:
                FPSort f32 = context.mkFPSort32();
                return context.mkConst(name, f32);
            case DOUBLE:
                FPSort f64 = context.mkFPSort64();
                return context.mkConst(name, f64);
            case BOOLEAN:
                return context.mkBoolConst(name);
            default:
                throw new IllegalArgumentException("Unsupported type for Z3 expression: " + type);
        }
    }

    public static TypedExpr.JavaType mapPrimitiveType(PrimitiveType primitiveType) {
        PrimitiveType.Code code = primitiveType.getPrimitiveTypeCode();
        if (code.equals(PrimitiveType.BYTE)) {
            return TypedExpr.JavaType.BYTE;
        } else if (code.equals(PrimitiveType.CHAR)) {
            return TypedExpr.JavaType.CHAR;
        } else if (code.equals(PrimitiveType.SHORT)) {
            return TypedExpr.JavaType.SHORT;
        } else if (code.equals(PrimitiveType.INT)) {
            return TypedExpr.JavaType.INT;
        } else if (code.equals(PrimitiveType.LONG)) {
            return TypedExpr.JavaType.LONG;
        } else if (code.equals(PrimitiveType.FLOAT)) {
            return TypedExpr.JavaType.FLOAT;
        } else if (code.equals(PrimitiveType.DOUBLE)) {
            return TypedExpr.JavaType.DOUBLE;
        } else if (code.equals(PrimitiveType.BOOLEAN)) {
            return TypedExpr.JavaType.BOOLEAN;
        }
        throw new IllegalArgumentException("Invalid primitive type: " + code);
    }
}
