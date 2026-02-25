package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.PrefixExpression;

@Getter
@Setter
public final class LiteralBooleanNode extends LiteralNode {
    private boolean value;

    private LiteralBooleanNode(boolean value) {
        this.value = value;
    }

    public static LiteralBooleanNode of(boolean value) {
        return new LiteralBooleanNode(value);
    }

    public static LiteralBooleanNode from(BooleanLiteral booleanLiteral) {
        return new LiteralBooleanNode(booleanLiteral.booleanValue());
    }

    public static Expr<?> convertLiteralBooleanToZ3Expr(LiteralBooleanNode literalAstNode, Context ctx) {
        return ctx.mkBool(literalAstNode.value);
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
