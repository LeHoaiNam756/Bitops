package core.SymbolicExecution.AstNode.Expression.Operation;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;

public class OperationExpressionNode extends ExpressionNode {
    public static AstNode executeOperation(Expression expression, MemoryModel memoryModel) {
        if (expression instanceof ParenthesizedExpression) {
            return ParenthesizedExpressionNode.executeParenthesizedExpression(
                    (ParenthesizedExpression) expression, memoryModel);
        }
        else if (expression instanceof InfixExpression) {
            return InfixExpressionNode.executeInfixExpression((InfixExpression) expression, memoryModel);
        } else if (expression instanceof PrefixExpression) {
            return PrefixExpressionNode.executePrefixExpression((PrefixExpression) expression, memoryModel);
        } else if (expression instanceof PostfixExpression) {
            return PostfixExpressionNode.executePostfixExpression((PostfixExpression) expression, memoryModel);
        } else {
            throw new RuntimeException("Unknown operation expression type: " + expression.getClass());
        }
    }


}
