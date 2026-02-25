package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CharacterLiteral;

@Getter
@Setter
public final class LiteralCharacterNode extends LiteralNode {

    private char value;

    private LiteralCharacterNode(char value) {
        this.value = value;
    }

    public static LiteralCharacterNode of(char value) {
        return new LiteralCharacterNode(value);
    }

    public static LiteralCharacterNode from(CharacterLiteral characterLiteral) {
        return new LiteralCharacterNode(characterLiteral.charValue());
    }

    public static Expr<?> convertLiteralCharacterToZ3Expr(LiteralCharacterNode literalAstNode, Context ctx) {
        return ctx.mkBV(literalAstNode.getValue(), 16);
    }

    @Override
    public String toString() {
        return Character.toString(value);
    }
}
