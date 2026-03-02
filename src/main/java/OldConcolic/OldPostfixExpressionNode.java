package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PostfixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

public class OldPostfixExpressionNode {
    public static AstNode executePostfixExpression(PostfixExpression postfixExpression,
                                                   MemoryModel memoryModel) {
        PostfixExpressionNode postfixExpressionNode = new PostfixExpressionNode();
        postfixExpressionNode.setOperator(postfixExpression.getOperator());
        ASTNode originalOperand = postfixExpression.getOperand();
        if (originalOperand instanceof SimpleName) {
            postfixExpressionNode.setVarName(((SimpleName) originalOperand).getIdentifier());
        }

        postfixExpressionNode.setOperand(OldAstNode.executeASTNode(postfixExpression.getOperand(), memoryModel));
        return executePostfixExpressionNode(postfixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executePostfixExpressionNode(PostfixExpressionNode postfixExpressionNode,
                                                       MemoryModel memoryModel) {
        AstNode operand = postfixExpressionNode.getOperand();
        PostfixExpression.Operator operator = postfixExpressionNode.getOperator();
        if (operand instanceof LiteralNode) {
            LiteralNode operandLiteral = (LiteralNode) operand;
            AstNode result = LiteralNode.analyzeOnePostfixLiteral(operandLiteral, operator);

            if (postfixExpressionNode.getVarName() != null) {
                memoryModel.assignVariable(postfixExpressionNode.getVarName(), result);
            }

            return operandLiteral;
        }

        if (postfixExpressionNode.getVarName() != null) {
            PostfixExpressionNode newPostfixNode = new PostfixExpressionNode();
            newPostfixNode.setOperator(operator);
            newPostfixNode.setOperand(operand);
            newPostfixNode.setVarName(postfixExpressionNode.getVarName());
            memoryModel.assignVariable(postfixExpressionNode.getVarName(), newPostfixNode);
        }

        return operand;
    }
    public static Expr<?> convertPostfixExpressionToZ3ExprOld(PostfixExpressionNode postfixExpressionNode, Context ctx,
                                                          MemoryModel memoryModel) {
        AstNode operand = postfixExpressionNode.getOperand();

        return OldExpressionNode.convertAstNodeToZ3ExprOld(operand, ctx, memoryModel);
    }
}
