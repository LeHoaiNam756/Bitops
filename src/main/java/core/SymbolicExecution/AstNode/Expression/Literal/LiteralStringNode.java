package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.StringLiteral;
import core.SymbolicExecution.MemoryModel;

@Getter
@Setter
public final class LiteralStringNode extends LiteralNode {

    /*String Pool for comparison??*/
    private String value;

    private LiteralStringNode(String value) {
        this.value = value;
    }

    public static LiteralStringNode of(String value) {
        return new LiteralStringNode(value);
    }

    public static LiteralStringNode from(StringLiteral stringLiteral) {
        return new LiteralStringNode(stringLiteral.getLiteralValue());
    }

    public static Expr<?> convertLiteralStringToZ3Expr(LiteralStringNode literalAstNode, Context ctx,
                                                       MemoryModel memoryModel) {
        throw new UnsupportedOperationException("String type conversion to Z3 Expr is not supported yet.");
    }

    @Override
    public String toString() {
        return value;
    }
}
