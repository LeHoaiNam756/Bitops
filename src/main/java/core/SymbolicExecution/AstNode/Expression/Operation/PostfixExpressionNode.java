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
        PostfixExpression.Operator operator = postfixExpressionNode.operator;

        Expr<?> operandExpr = ExpressionNode.convertAstNodeToZ3Expr(operand, ctx, memoryModel);
        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType operandType = typedExpr.getType(operandExpr);

        return analyzePostfixZ3Expr(operandExpr, operator, operandType, ctx);
    }

    private static Expr<?> analyzePostfixZ3Expr(Expr<?> operandExpr,
                                                 PostfixExpression.Operator operator,
                                                 TypedExpr.JavaType operandType,
                                                 Context ctx) {
        TypedExpr typedExpr = TypedExpr.getInstance();
        Expr<?> result;
        TypedExpr.JavaType resultType;

        boolean isFP = operandExpr instanceof FPExpr;
        if (isFP) {
            FPExpr fpVal = (FPExpr) operandExpr;
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();

            if (operator == PostfixExpression.Operator.INCREMENT) {
                FPExpr one = ctx.mkFP(1.0, fpVal.getSort());
                result = ctx.mkFPAdd(rm, fpVal, one);
                resultType = operandType;
            } else if (operator == PostfixExpression.Operator.DECREMENT) {
                FPExpr one = ctx.mkFP(1.0, fpVal.getSort());
                result = ctx.mkFPSub(rm, fpVal, one);
                resultType = operandType;
            } else {
                throw new RuntimeException("Invalid operator for floating-point operand: " + operator);
            }
            typedExpr.put(result, resultType);
            return result;
        }

        if (!(operandExpr instanceof BitVecExpr)) {
            throw new RuntimeException("Unsupported postfix operand type: " + operandExpr.getSort());
        }

        @SuppressWarnings("PatternVariableCanBeUsed")
        BitVecExpr bvVal = (BitVecExpr) operandExpr;
        int size = bvVal.getSortSize();

        if (operator == PostfixExpression.Operator.INCREMENT || operator == PostfixExpression.Operator.DECREMENT) {
            if (operandType == TypedExpr.JavaType.BYTE
                    || operandType == TypedExpr.JavaType.CHAR
                    || operandType == TypedExpr.JavaType.SHORT) {
                BitVecExpr promoted;
                if (operandType == TypedExpr.JavaType.CHAR) {
                    promoted = ctx.mkZeroExt(32 - size, bvVal);
                } else {
                    promoted = ctx.mkSignExt(32 - size, bvVal);
                }
                BitVecExpr one = ctx.mkBV(1, 32);

                if (operator == PostfixExpression.Operator.INCREMENT) {
                    result = ctx.mkBVAdd(promoted, one);
                } else {
                    result = ctx.mkBVSub(promoted, one);
                }

                result = ctx.mkExtract(size - 1, 0, (BitVecExpr) result);
                resultType = operandType;
            } else {
                BitVecExpr one = ctx.mkBV(1, size);
                if (operator == PostfixExpression.Operator.INCREMENT) {
                    result = ctx.mkBVAdd(bvVal, one);
                } else {
                    result = ctx.mkBVSub(bvVal, one);
                }
                resultType = operandType;
            }
        } else {
            throw new RuntimeException("Unsupported postfix operator: " + operator);
        }

        typedExpr.put(result, resultType);
        return result;
    }

    @Override
    public String toString() {
        return "(" + operand.toString() + ")"  + operator.toString();
    }
}
