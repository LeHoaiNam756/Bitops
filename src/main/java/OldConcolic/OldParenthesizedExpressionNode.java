package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Operation.ParenthesizedExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;

public class OldParenthesizedExpressionNode {
    public static AstNode executeParenthesizedExpression(ParenthesizedExpression parenthesizedExpr,
                                                             MemoryModel memoryModel) {
        Expression innerExpression = parenthesizedExpr.getExpression();
        return OldExpressionNode.executeExpression(innerExpression, memoryModel);
    }


    public static Expr<?> convertParenthesizedExpressionToZ3ExprOld(ParenthesizedExpressionNode astNode, Context ctx,
                                                                 MemoryModel memoryModel) {
        AstNode innerAstNode = astNode.getInnerAstNode();
        return OldExpressionNode.convertAstNodeToZ3ExprOld(innerAstNode, ctx, memoryModel);
    }
}
