package OldConcolic;

import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.MemoryModel;

public class OldOperationExpressionNode {

    public static AstNode executeOperation(Expression expression, MemoryModel memoryModel) {
        if (expression instanceof ParenthesizedExpression) {
            return OldParenthesizedExpressionNode.executeParenthesizedExpression(
                    (ParenthesizedExpression) expression, memoryModel);
        }
        else if (expression instanceof InfixExpression) {
            return OldInfixExpressionNode.executeInfixExpression((InfixExpression) expression, memoryModel);
        } else if (expression instanceof PrefixExpression) {
            return OldPrefixExpressionNode.executePrefixExpression((PrefixExpression) expression, memoryModel);
        } else if (expression instanceof PostfixExpression) {
            return OldPostfixExpressionNode.executePostfixExpression((PostfixExpression) expression, memoryModel);
        } else {
            throw new RuntimeException("Unknown operation expression type: " + expression.getClass());
        }
    }
}

