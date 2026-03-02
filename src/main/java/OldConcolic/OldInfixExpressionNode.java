package OldConcolic;

import com.microsoft.z3.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.LinkedList;
import java.util.List;

public class OldInfixExpressionNode {
    public static AstNode executeInfixExpression(InfixExpression infixExpression, MemoryModel memoryModel) {
        InfixExpressionNode infixExpressionNode = new InfixExpressionNode();
        infixExpressionNode.setLeft(
                 OldAstNode.executeASTNode(infixExpression.getLeftOperand(), memoryModel));
        infixExpressionNode.setRight(
                OldAstNode.executeASTNode(infixExpression.getRightOperand(), memoryModel));
        infixExpressionNode.setOperator(infixExpression.getOperator());

        List<AstNode> extendedOperands = new LinkedList<>();
        for (Object operand : infixExpression.extendedOperands()) {
            ExpressionNode executedOperand =
                    (ExpressionNode) OldExpressionNode.executeExpression((Expression) operand, memoryModel);
            extendedOperands.add(executedOperand);
        }

        infixExpressionNode.setExtendedOperands(extendedOperands);

        return executeInfixExpressionNode(infixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeInfixExpressionNode(InfixExpressionNode infixExpressionNode, MemoryModel memoryModel) {
        AstNode left = infixExpressionNode.getLeft();
        AstNode right = infixExpressionNode.getRight();
        InfixExpression.Operator operator = infixExpressionNode.getOperator();
        List<AstNode> extendedOperands = infixExpressionNode.getExtendedOperands();

        if (!(left instanceof LiteralNode) || !(right instanceof LiteralNode)) {
            return infixExpressionNode;
        } else {
            LiteralNode leftLiteral = (LiteralNode) left;
            LiteralNode rightLiteral = (LiteralNode) right;
            LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(leftLiteral, operator, rightLiteral);
            if (extendedOperands == null || extendedOperands.isEmpty()) {
                return result;
            } else {
                infixExpressionNode.setLeft(result);
                infixExpressionNode.setRight(extendedOperands.get(0));
                extendedOperands.remove(0);
                return executeInfixExpressionNode(infixExpressionNode, memoryModel);
            }
        }
    }
    public static Expr<?> convertInfixExpressionToZ3ExprOld(InfixExpressionNode infixExpressionNode, Context ctx,
                                                         MemoryModel memoryModel) {
        AstNode leftOperand = infixExpressionNode.getLeft();
        AstNode rightOperand = infixExpressionNode.getRight();
        InfixExpression.Operator operator = infixExpressionNode.getOperator();
        List<AstNode> extendedOperands = infixExpressionNode.getExtendedOperands();

        Expr<?> Z3LeftOperand = OldExpressionNode.convertAstNodeToZ3ExprOld(leftOperand, ctx, memoryModel);
        Expr<?> Z3RightOperand = OldExpressionNode.convertAstNodeToZ3ExprOld(rightOperand, ctx, memoryModel);

        Expr<?> result = createInfixZ3Expression(ctx, Z3LeftOperand, operator, Z3RightOperand);

        if (extendedOperands == null) {
            return result;
        }
        for (int i = 0; i < extendedOperands.size(); i++) {
            Expr<?> extendedOperand = OldExpressionNode.convertAstNodeToZ3ExprOld( extendedOperands.get(i), ctx,
                    memoryModel);
            result = createInfixZ3Expression(ctx, result, operator, extendedOperand);
        }

        return result;
    }

    private static Expr<?> createInfixZ3Expression(Context ctx, Expr<?> left,
                                                   InfixExpression.Operator operator, Expr<?> right) {
        if (operator.equals(InfixExpression.Operator.PLUS)) {
            return ctx.mkAdd((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.MINUS)) {
            return ctx.mkSub((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.TIMES)) {
            return ctx.mkMul((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.DIVIDE)) {
            return ctx.mkDiv((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.REMAINDER)) {
            // Remainder/modulo only works with integers
            if (left instanceof IntExpr && right instanceof IntExpr) {
                return ctx.mkMod((IntExpr) left, (IntExpr) right);
            } else {
                throw new RuntimeException("Modulo operation requires integer operands, got: " + 
                        left.getClass() + " and " + right.getClass());
            }
        } else if (operator.equals(InfixExpression.Operator.LESS)) {
            return ctx.mkLt((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.GREATER)) {
            return ctx.mkGt((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.LESS_EQUALS)) {
            return ctx.mkLe((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.GREATER_EQUALS)) {
            return ctx.mkGe((ArithExpr) left, (ArithExpr) right);
        } else if (operator.equals(InfixExpression.Operator.EQUALS)) {
            return ctx.mkEq(left, right);
        } else if (operator.equals(InfixExpression.Operator.NOT_EQUALS)) {
            return ctx.mkDistinct(left, right);
        } else if (operator.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
            return ctx.mkAnd((BoolExpr) left, (BoolExpr) right);
        } else if (operator.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
            return ctx.mkOr((BoolExpr) left, (BoolExpr) right);

        } else if (operator.equals(InfixExpression.Operator.LEFT_SHIFT)) {
            // Left shift only works with integers
            if (left instanceof IntExpr && right instanceof IntExpr) {
                ArithExpr powerOfTwo = ctx.mkPower(ctx.mkInt(2), (ArithExpr) right);
                return ctx.mkMul((ArithExpr) left, powerOfTwo);
            } else {
                throw new RuntimeException("Left shift operation requires integer operands, got: " + 
                        left.getClass() + " and " + right.getClass());
            }
        } else if (operator.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
            // Right shift only works with integers
            if (left instanceof IntExpr && right instanceof IntExpr) {
                ArithExpr powerOfTwo = ctx.mkPower(ctx.mkInt(2), (ArithExpr) right);
                return ctx.mkDiv((ArithExpr) left, powerOfTwo);
            } else {
                throw new RuntimeException("Right shift operation requires integer operands, got: " + 
                        left.getClass() + " and " + right.getClass());
            }
        } else {
            throw new UnsupportedOperationException("Operator " + operator + " not yet implemented or invalid.");
        }
    }
}
