package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

public final class LiteralNullNode extends LiteralNode {
    public static Expr<?> convertLiteralNullToZ3Expr(Context ctx) {
        throw new UnsupportedOperationException("null convert to Z3 Expr is not supported yet.");
    }

    @Override
    public String toString() {
        return "null";
    }
}
