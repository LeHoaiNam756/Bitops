package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralCharacterNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNullNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralStringNode;
import core.SymbolicExecution.MemoryModel;

public class OldLiteralNode {
    public static Expr<?> convertLiteralToZ3ExprOld(LiteralNode literalAstNode, Context ctx, MemoryModel memoryModel) {
        Expr<?> result;
        if (literalAstNode instanceof LiteralNumberNode) {
            result = OldNumberNode.convertLiteralNumberToZ3Expr((LiteralNumberNode) literalAstNode, ctx);
        } else if (literalAstNode instanceof LiteralBooleanNode) {
            result = LiteralBooleanNode.convertLiteralBooleanToZ3Expr((LiteralBooleanNode) literalAstNode, ctx);
        } else if (literalAstNode instanceof LiteralCharacterNode) {
            // Old version uses mkInt instead of mkBV
            LiteralCharacterNode charNode = (LiteralCharacterNode) literalAstNode;
            result = ctx.mkInt(charNode.getValue());
        } else if (literalAstNode instanceof LiteralStringNode) {
            result = LiteralStringNode.convertLiteralStringToZ3Expr((LiteralStringNode) literalAstNode, ctx,
                    memoryModel);
        } else if (literalAstNode instanceof LiteralNullNode) {
            result = LiteralNullNode.convertLiteralNullToZ3Expr(ctx);
        } else {
            throw new RuntimeException("Unsupported literal type for Z3 conversion: " + literalAstNode.getClass());
        }
        return result;
    }
}
