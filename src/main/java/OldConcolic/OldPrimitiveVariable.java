package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.Variable.Variable;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

public class OldPrimitiveVariable extends Variable {
    private final PrimitiveType primitiveType;

    public OldPrimitiveVariable(PrimitiveType primitiveType, String name) {
        this.primitiveType = primitiveType;
        this.setName(name);
    }

    public PrimitiveType.Code getCode() {
        return primitiveType.getPrimitiveTypeCode();
    }

    @Override
    public Type getType() {
        return primitiveType;
    }

    @Override
    public Expr<?> createZ3Expr(Context context) {
        PrimitiveType.Code code = this.getCode();
        String name = this.getName();

        if (code.equals(PrimitiveType.INT) ||
                code.equals(PrimitiveType.LONG) ||
                code.equals(PrimitiveType.SHORT) ||
                code.equals(PrimitiveType.BYTE) ||
                code.equals(PrimitiveType.CHAR)) {
            return context.mkIntConst(name);
        } else if (code.equals(PrimitiveType.DOUBLE) ||
                code.equals(PrimitiveType.FLOAT)) {
            return context.mkRealConst(name);
        } else if (code.equals(PrimitiveType.BOOLEAN)) {
            return context.mkBoolConst(name);
        } else {
            throw new RuntimeException("Invalid type");
        }
    }
}
