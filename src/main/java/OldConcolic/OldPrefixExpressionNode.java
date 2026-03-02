package OldConcolic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PrefixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;

public class OldPrefixExpressionNode {
    public static AstNode executePrefixExpression(PrefixExpression prefixExpression, MemoryModel memoryModel) {
        PrefixExpressionNode prefixExpressionNode = new PrefixExpressionNode();
        prefixExpressionNode.setOperator(prefixExpression.getOperator());
        ASTNode originalOperand = prefixExpression.getOperand();
        if (originalOperand instanceof SimpleName) {
            prefixExpressionNode.setVarName(((SimpleName) originalOperand).getIdentifier());
        }

        prefixExpressionNode.setOperand(OldAstNode.executeASTNode(prefixExpression.getOperand(), memoryModel));
        return executePrefixExpressionNode(prefixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executePrefixExpressionNode(PrefixExpressionNode prefixExpressionNode,
                                                      MemoryModel memoryModel) {
        AstNode operand = prefixExpressionNode.getOperand();
        PrefixExpression.Operator operator = prefixExpressionNode.getOperator();
        if (operand instanceof LiteralNode) {
            LiteralNode operandLiteral = (LiteralNode) operand;
            AstNode result = LiteralNode.analyzeOnePrefixLiteral(operandLiteral, operator);

            if (prefixExpressionNode.getVarName() != null) {
                memoryModel.assignVariable(prefixExpressionNode.getVarName(), result);
            }

            return result;
        }

        if (prefixExpressionNode.getVarName() != null) {
            PrefixExpressionNode newPrefixNode = new PrefixExpressionNode();
            newPrefixNode.setOperator(operator);
            newPrefixNode.setOperand(operand);
            memoryModel.assignVariable(prefixExpressionNode.getVarName(), newPrefixNode);
        }

        return prefixExpressionNode;
    }
    public static Expr<?> convertPrefixExpressionToZ3ExprOld(PrefixExpressionNode astNode,
                                                             Context ctx,
                                                             MemoryModel memoryModel) {
        AstNode operand = astNode.getOperand();
        PrefixExpression.Operator operator = astNode.getOperator();

        Expr<?> operandExpr = OldExpressionNode.convertAstNodeToZ3ExprOld(operand, ctx, memoryModel);

        if (operator.equals(PrefixExpression.Operator.INCREMENT)) {
            if (operandExpr instanceof ArithExpr) {
                return ctx.mkAdd((ArithExpr) operandExpr, ctx.mkInt(1));
            } else {
                throw new RuntimeException("Increment operation requires arithmetic operand, got: " + operandExpr.getClass());
            }
        } else if (operator.equals(PrefixExpression.Operator.DECREMENT)) {
            if (operandExpr instanceof ArithExpr) {
                return ctx.mkSub((ArithExpr) operandExpr, ctx.mkInt(1));
            } else {
                throw new RuntimeException("Decrement operation requires arithmetic operand, got: " + operandExpr.getClass());
            }
        } else if (operator.equals(PrefixExpression.Operator.PLUS)) {
            return operandExpr; // Multiplying by 1 is redundant in Z3 logic
        } else if (operator.equals(PrefixExpression.Operator.MINUS)) {
            if (operandExpr instanceof ArithExpr) {
                return ctx.mkUnaryMinus((ArithExpr) operandExpr);
            } else {
                throw new RuntimeException("Unary minus operation requires arithmetic operand, got: " + operandExpr.getClass());
            }
        } else if (operator.equals(PrefixExpression.Operator.NOT)) {
            return ctx.mkNot((BoolExpr) operandExpr);
        } else if (operator.equals(PrefixExpression.Operator.COMPLEMENT)) {
            if (operandExpr instanceof ArithExpr) {
                return ctx.mkUnaryMinus(ctx.mkAdd((ArithExpr) operandExpr, ctx.mkInt(1)));
            } else {
                throw new RuntimeException("Complement operation requires arithmetic operand, got: " + operandExpr.getClass());
            }
        } else {
            throw new RuntimeException("Invalid operator: " + operator);
        }
    }
}
