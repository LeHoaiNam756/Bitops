package core.SymbolicExecution.AstNode.Expression.Operation;

import com.microsoft.z3.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.TypedExpr;

@Getter
@Setter
public class PostfixExpressionNode extends OperationExpressionNode{
    private PostfixExpression.Operator operator;
    private AstNode operand;
    private String varName;

    public static AstNode executePostfixExpression(PostfixExpression postfixExpression,
                                                   MemoryModel memoryModel) {
        PostfixExpressionNode postfixExpressionNode = new PostfixExpressionNode();
        postfixExpressionNode.operator = postfixExpression.getOperator();
        ASTNode originalOperand = postfixExpression.getOperand();
        if (originalOperand instanceof SimpleName) {
            postfixExpressionNode.varName = ((SimpleName) originalOperand).getIdentifier();
        }

        postfixExpressionNode.operand = AstNode.executeASTNode(postfixExpression.getOperand(), memoryModel);
        return executePostfixExpressionNode(postfixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executePostfixExpressionNode(PostfixExpressionNode postfixExpressionNode,
                                                        MemoryModel memoryModel) {
        AstNode operand = postfixExpressionNode.operand;
        PostfixExpression.Operator operator = postfixExpressionNode.operator;
        if (operand instanceof LiteralNode) {
            LiteralNode operandLiteral = (LiteralNode) operand;
            AstNode result = LiteralNode.analyzeOnePostfixLiteral(operandLiteral, operator);

            if (postfixExpressionNode.varName != null) {
                memoryModel.assignVariable(postfixExpressionNode.varName, result);
            }

            return operandLiteral;
        }

        if (postfixExpressionNode.varName != null) {
            PostfixExpressionNode newPostfixNode = new PostfixExpressionNode();
            newPostfixNode.operator = operator;
            newPostfixNode.operand = operand;
            newPostfixNode.varName = postfixExpressionNode.varName;
            memoryModel.assignVariable(postfixExpressionNode.varName, newPostfixNode);
        }

        return operand;
    }

    public static Expr<?> convertPostfixExpressionToZ3Expr(PostfixExpressionNode postfixExpressionNode, Context ctx,
                                                           MemoryModel memoryModel) {
        AstNode operand = postfixExpressionNode.operand;

        // For postfix expressions (e.g., a++), the expression evaluates to the original value
        // The increment/decrement is a side effect that happens after the value is returned
        // So we convert the operand to Z3 expression and return it directly
        return ExpressionNode.convertAstNodeToZ3Expr(operand, ctx, memoryModel);
    }

    @Override
    public String toString() {
        return "(" + operand.toString() + ")"  + operator.toString();
    }
}
