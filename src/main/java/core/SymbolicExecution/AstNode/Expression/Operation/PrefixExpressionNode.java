package core.SymbolicExecution.AstNode.Expression.Operation;

import com.microsoft.z3.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.TypedExpr;

@Getter
@Setter
public class PrefixExpressionNode extends OperationExpressionNode{
    private PrefixExpression.Operator operator;
    private AstNode operand;
    private String varName;


    public static AstNode executePrefixExpression(PrefixExpression prefixExpression, MemoryModel memoryModel) {
        PrefixExpressionNode prefixExpressionNode = new PrefixExpressionNode();
        prefixExpressionNode.operator = prefixExpression.getOperator();
        ASTNode originalOperand = prefixExpression.getOperand();
        if (originalOperand instanceof SimpleName) {
            prefixExpressionNode.varName = ((SimpleName) originalOperand).getIdentifier();
        }

        prefixExpressionNode.operand = AstNode.executeASTNode(prefixExpression.getOperand(), memoryModel);
        return executePrefixExpressionNode(prefixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executePrefixExpressionNode(PrefixExpressionNode prefixExpressionNode,
                                                      MemoryModel memoryModel) {
        AstNode operand = prefixExpressionNode.operand;
        PrefixExpression.Operator operator = prefixExpressionNode.operator;
        if (operand instanceof LiteralNode) {
            LiteralNode operandLiteral = (LiteralNode) operand;
            AstNode result = LiteralNode.analyzeOnePrefixLiteral(operandLiteral, operator);

            if (prefixExpressionNode.varName != null) {
                memoryModel.assignVariable(prefixExpressionNode.varName, result);
            }

            return result;
        }

        if (prefixExpressionNode.varName != null) {
            PrefixExpressionNode newPrefixNode = new PrefixExpressionNode();
            newPrefixNode.operator = operator;
            newPrefixNode.operand = operand;
            memoryModel.assignVariable(prefixExpressionNode.varName, newPrefixNode);
        }

        return prefixExpressionNode;
    }

    public static Expr<?> convertPrefixExpressionToZ3Expr(PrefixExpressionNode astNode,
                                                          Context ctx,
                                                          MemoryModel memoryModel) {
        AstNode operand = astNode.operand;
        PrefixExpression.Operator operator = astNode.operator;

        Expr<?> operandExpr = ExpressionNode.convertAstNodeToZ3Expr(operand, ctx, memoryModel);
        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType operandType = typedExpr.getType(operandExpr);

        return analyzePrefixZ3Expr(operandExpr, operator, operandType, ctx);
    }

    private static Expr<?> analyzePrefixZ3Expr(Expr<?> operandExpr,
                                               PrefixExpression.Operator operator,
                                               TypedExpr.JavaType operandType,
                                               Context ctx) {
        TypedExpr typedExpr = TypedExpr.getInstance();
        Expr<?> result;
        TypedExpr.JavaType resultType;

        if (operator == PrefixExpression.Operator.NOT) {
            if (!(operandExpr instanceof BoolExpr)) {
                throw new RuntimeException("NOT operator requires boolean operand");
            }
            result = ctx.mkNot((BoolExpr) operandExpr);
            resultType = TypedExpr.JavaType.BOOLEAN;
            typedExpr.put(result, resultType);
            return result;
        }

        boolean isFP = operandExpr instanceof FPExpr;
        if (isFP) {
            FPExpr fpVal = (FPExpr) operandExpr;
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();

            if (operator == PrefixExpression.Operator.INCREMENT) {
                FPExpr one = ctx.mkFP(1.0, fpVal.getSort());
                result = ctx.mkFPAdd(rm, fpVal, one);
                resultType = operandType;
            } else if (operator == PrefixExpression.Operator.DECREMENT) {
                FPExpr one = ctx.mkFP(1.0, fpVal.getSort());
                result = ctx.mkFPSub(rm, fpVal, one);
                resultType = operandType;
            } else if (operator == PrefixExpression.Operator.PLUS) {
                result = fpVal;
                resultType = operandType;
            } else if (operator == PrefixExpression.Operator.MINUS) {
                result = ctx.mkFPNeg(fpVal);
                resultType = operandType;
            } else {
                throw new RuntimeException("Invalid operator for floating-point operand: " + operator);
            }
            typedExpr.put(result, resultType);
            return result;
        }

        if (!(operandExpr instanceof BitVecExpr)) {
            throw new RuntimeException("Unsupported prefix operand type: " + operandExpr.getSort());
        }

        @SuppressWarnings("PatternVariableCanBeUsed")
        BitVecExpr bvVal = (BitVecExpr) operandExpr;
        int size = bvVal.getSortSize();

        if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
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

                if (operator == PrefixExpression.Operator.INCREMENT) {
                    result = ctx.mkBVAdd(promoted, one);
                } else {
                    result = ctx.mkBVSub(promoted, one);
                }

                result = ctx.mkExtract(size - 1, 0, (BitVecExpr) result);
                resultType = operandType;
            } else {
                BitVecExpr one = ctx.mkBV(1, size);
                if (operator == PrefixExpression.Operator.INCREMENT) {
                    result = ctx.mkBVAdd(bvVal, one);
                } else {
                    result = ctx.mkBVSub(bvVal, one);
                }
                resultType = operandType;
            }
        } else if (operator == PrefixExpression.Operator.PLUS) {
            if (operandType == TypedExpr.JavaType.BYTE
                    || operandType == TypedExpr.JavaType.CHAR
                    || operandType == TypedExpr.JavaType.SHORT) {
                if (operandType == TypedExpr.JavaType.CHAR) {
                    result = ctx.mkZeroExt(32 - size, bvVal);
                } else {
                    result = ctx.mkSignExt(32 - size, bvVal);
                }
                resultType = TypedExpr.JavaType.INT;
            } else {
                result = bvVal;
                resultType = operandType;
            }
        } else if (operator == PrefixExpression.Operator.MINUS) {
            if (operandType == TypedExpr.JavaType.BYTE
                    || operandType == TypedExpr.JavaType.CHAR
                    || operandType == TypedExpr.JavaType.SHORT) {
                BitVecExpr promoted;
                if (operandType == TypedExpr.JavaType.CHAR) {
                    promoted = ctx.mkZeroExt(32 - size, bvVal);
                } else {
                    promoted = ctx.mkSignExt(32 - size, bvVal);
                }
                result = ctx.mkBVNeg(promoted);
                resultType = TypedExpr.JavaType.INT;
            } else {
                result = ctx.mkBVNeg(bvVal);
                resultType = operandType;
            }
        } else if (operator == PrefixExpression.Operator.COMPLEMENT) {
            if (operandType == TypedExpr.JavaType.BYTE
                    || operandType == TypedExpr.JavaType.CHAR
                    || operandType == TypedExpr.JavaType.SHORT) {
                BitVecExpr promoted;
                if (operandType == TypedExpr.JavaType.CHAR) {
                    promoted = ctx.mkZeroExt(32 - size, bvVal);
                } else {
                    promoted = ctx.mkSignExt(32 - size, bvVal);
                }
                result = ctx.mkBVNot(promoted);
                resultType = TypedExpr.JavaType.INT;
            } else {
                result = ctx.mkBVNot(bvVal);
                resultType = operandType;
            }
        } else {
            throw new RuntimeException("Unsupported prefix operator: " + operator);
        }

        typedExpr.put(result, resultType);
        return result;
    }


    @Override
    public String toString() {
        return operator.toString() + "(" + operand.toString() + ")";
    }
}
