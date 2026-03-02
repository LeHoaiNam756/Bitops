package core.SymbolicExecution.AstNode.Expression.Operation;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;

@Getter
@Setter
public class ParenthesizedExpressionNode extends OperationExpressionNode {
    private AstNode innerAstNode;

    public static AstNode executeParenthesizedExpression(ParenthesizedExpression parenthesizedExpr,
                                                             MemoryModel memoryModel) {
        ParenthesizedExpressionNode parenthesizedExpressionNode = new ParenthesizedExpressionNode();
        parenthesizedExpressionNode.innerAstNode =
                AstNode.executeASTNode(parenthesizedExpr.getExpression(), memoryModel);
        return analyzeParenthesizedExpressionNode(parenthesizedExpressionNode);
    }

    private static AstNode analyzeParenthesizedExpressionNode(ParenthesizedExpressionNode parenthesizedExpressionNode) {
        AstNode innerAstNode = parenthesizedExpressionNode.innerAstNode;
        if (innerAstNode instanceof LiteralNode) {
            return innerAstNode;
        }
        return parenthesizedExpressionNode;
    }

    public static Expr<?> convertParenthesizedExpressionToZ3Expr(ParenthesizedExpressionNode astNode, Context ctx,
                                                                 MemoryModel memoryModel) {
        AstNode innerAstNode = astNode.innerAstNode;
        return ExpressionNode.convertAstNodeToZ3Expr(innerAstNode, ctx, memoryModel);
    }
}
