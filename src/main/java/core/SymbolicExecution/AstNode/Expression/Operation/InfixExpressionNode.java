package core.SymbolicExecution.AstNode.Expression.Operation;

import com.microsoft.z3.*;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.TypedExpr;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class InfixExpressionNode extends OperationExpressionNode {
    private AstNode left;
    private AstNode right;
    private InfixExpression.Operator operator;
    private List<AstNode> extendedOperands;

    public static AstNode executeInfixExpression(InfixExpression infixExpression, MemoryModel memoryModel) {
        InfixExpressionNode infixExpressionNode = new InfixExpressionNode();
        infixExpressionNode.left =
                 AstNode.executeASTNode(infixExpression.getLeftOperand(), memoryModel);
        infixExpressionNode.right =
                AstNode.executeASTNode(infixExpression.getRightOperand(), memoryModel);
        infixExpressionNode.operator = infixExpression.getOperator();

        List<AstNode> extendedOperands = new LinkedList<>();
        for (Object operand : infixExpression.extendedOperands()) {
            ExpressionNode executedOperand =
                    (ExpressionNode) ExpressionNode.executeExpression((Expression) operand, memoryModel);
            extendedOperands.add(executedOperand);
        }

        infixExpressionNode.extendedOperands = extendedOperands;

        return executeInfixExpressionNode(infixExpressionNode, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeInfixExpressionNode(InfixExpressionNode infixExpressionNode, MemoryModel memoryModel) {
        AstNode left = infixExpressionNode.left;
        AstNode right = infixExpressionNode.right;
        InfixExpression.Operator operator = infixExpressionNode.operator;
        List<AstNode> extendedOperands = infixExpressionNode.extendedOperands;

        if (!(left instanceof LiteralNode) || !(right instanceof LiteralNode)) {
            return infixExpressionNode;
        } else {
            LiteralNode leftLiteral = (LiteralNode) left;
            LiteralNode rightLiteral = (LiteralNode) right;
            LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(leftLiteral, operator, rightLiteral);
            if (extendedOperands.isEmpty()) {
                return result;
            } else {
                infixExpressionNode.left = result;
                infixExpressionNode.right = extendedOperands.get(0);
                extendedOperands.remove(0);
                return executeInfixExpressionNode(infixExpressionNode, memoryModel);
            }
        }
    }

    public static Expr<?> convertInfixExpressionToZ3Expr(InfixExpressionNode infixExpressionNode, Context ctx,
                                                         MemoryModel memoryModel) {
        AstNode left = infixExpressionNode.left;
        AstNode right = infixExpressionNode.right;
        InfixExpression.Operator operator = infixExpressionNode.operator;
        List<AstNode> extendedOperands = infixExpressionNode.extendedOperands;

        Expr<?> leftExpr = ExpressionNode.convertAstNodeToZ3Expr(left, ctx, memoryModel);
        Expr<?> rightExpr = ExpressionNode.convertAstNodeToZ3Expr(right, ctx, memoryModel);

        Expr<?> resultExpr = analyzeTwoInfixZ3Expr(leftExpr, operator, rightExpr, ctx);

        if (extendedOperands == null || extendedOperands.isEmpty()) {
            return resultExpr;
        }

        for (AstNode extendedOperandNode : extendedOperands) {
            Expr<?> extendedExpr = ExpressionNode.convertAstNodeToZ3Expr(extendedOperandNode, ctx, memoryModel);
            resultExpr = analyzeTwoInfixZ3Expr(resultExpr, operator, extendedExpr, ctx);
        }

        return resultExpr;
    }

    private static Expr<?> analyzeTwoInfixZ3Expr(Expr<?> leftExpr,
                                                 InfixExpression.Operator operator,
                                                 Expr<?> rightExpr,
                                                 Context ctx) {
        if (leftExpr instanceof BoolExpr || rightExpr instanceof BoolExpr) {
            BoolExpr l = (BoolExpr) leftExpr;
            BoolExpr r = (BoolExpr) rightExpr;

            if (operator == InfixExpression.Operator.CONDITIONAL_AND
                    || operator == InfixExpression.Operator.AND) {
                return ctx.mkAnd(l, r);
            } else if (operator == InfixExpression.Operator.CONDITIONAL_OR
                    || operator == InfixExpression.Operator.OR) {
                return ctx.mkOr(l, r);
            } else if (operator == InfixExpression.Operator.XOR) {
                return ctx.mkXor(l, r);
            } else if (operator == InfixExpression.Operator.EQUALS) {
                return ctx.mkEq(l, r);
            } else if (operator == InfixExpression.Operator.NOT_EQUALS) {
                return ctx.mkDistinct(l, r);
            } else {
                throw new RuntimeException("Invalid boolean infix operator: " + operator);
            }
        }

        boolean fpLeft = isFP(leftExpr);
        boolean fpRight = isFP(rightExpr);
        boolean isFPExpr = fpLeft || fpRight;

        if (isFPExpr) {
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();
            FPSort fpSort = pickFpSort(leftExpr, rightExpr, ctx);
            FPExpr l = coerceToFP(ctx, leftExpr, fpSort, rm);
            FPExpr r = coerceToFP(ctx, rightExpr, fpSort, rm);

            if (operator == InfixExpression.Operator.PLUS) {
                return ctx.mkFPAdd(rm, l, r);
            } else if (operator == InfixExpression.Operator.MINUS) {
                return ctx.mkFPSub(rm, l, r);
            } else if (operator == InfixExpression.Operator.TIMES) {
                return ctx.mkFPMul(rm, l, r);
            } else if (operator == InfixExpression.Operator.DIVIDE) {
                return ctx.mkFPDiv(rm, l, r);
            } else if (operator == InfixExpression.Operator.REMAINDER) {
                return ctx.mkFPRem(l, r);
            } else if (operator == InfixExpression.Operator.EQUALS) {
                return ctx.mkFPEq(l, r);
            } else if (operator == InfixExpression.Operator.NOT_EQUALS) {
                return ctx.mkDistinct(l, r);
            } else if (operator == InfixExpression.Operator.LESS) {
                return ctx.mkFPLt(l, r);
            } else if (operator == InfixExpression.Operator.GREATER) {
                return ctx.mkFPGt(l, r);
            } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
                return ctx.mkFPLEq(l, r);
            } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
                return ctx.mkFPGEq(l, r);
            } else {
                throw new RuntimeException("Invalid operator for floating-point operands: " + operator);
            }
        }

        if (!(leftExpr instanceof BitVecExpr) || !(rightExpr instanceof BitVecExpr)) {
            throw new RuntimeException("Unsupported infix operand types for Z3 conversion: "
                    + leftExpr.getSort() + " and " + rightExpr.getSort());
        }

        BitVecExpr l = (BitVecExpr) leftExpr;
        BitVecExpr r = (BitVecExpr) rightExpr;

        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType leftType = typedExpr.getType(leftExpr);
        TypedExpr.JavaType rightType = typedExpr.getType(rightExpr);
        boolean leftIsChar = leftType == TypedExpr.JavaType.CHAR;
        boolean rightIsChar = rightType == TypedExpr.JavaType.CHAR;
        boolean hasChar = leftIsChar || rightIsChar;

        boolean isShift = operator == InfixExpression.Operator.LEFT_SHIFT
                || operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED
                || operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;

        if (isShift) {
            int targetSize = l.getSortSize();
            r = fixBvWidth(ctx, r, targetSize, false);
            BitVecExpr mask = ctx.mkBV(targetSize - 1, targetSize);
            r = ctx.mkBVAND(r, mask);
        } else {
            int maxSize = Math.max(l.getSortSize(), r.getSortSize());
            l = fixBvWidth(ctx, l, maxSize, !leftIsChar);
            r = fixBvWidth(ctx, r, maxSize, !rightIsChar);
        }

        Expr<?> result;
        TypedExpr.JavaType resultType;
        
        if (operator == InfixExpression.Operator.PLUS) {
            result = ctx.mkBVAdd(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.MINUS) {
            result = ctx.mkBVSub(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.TIMES) {
            result = ctx.mkBVMul(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.DIVIDE) {
            result = hasChar ? ctx.mkBVUDiv(l, r) : ctx.mkBVSDiv(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.REMAINDER) {
            result = hasChar ? ctx.mkBVURem(l, r) : ctx.mkBVSRem(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.LESS) {
            result = hasChar ? ctx.mkBVULT(l, r) : ctx.mkBVSLT(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.GREATER) {
            result = hasChar ? ctx.mkBVUGT(l, r) : ctx.mkBVSGT(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
            result = hasChar ? ctx.mkBVULE(l, r) : ctx.mkBVSLE(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
            result = hasChar ? ctx.mkBVUGE(l, r) : ctx.mkBVSGE(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.EQUALS) {
            result = ctx.mkEq(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.NOT_EQUALS) {
            result = ctx.mkDistinct(l, r);
            resultType = TypedExpr.JavaType.BOOLEAN;
        } else if (operator == InfixExpression.Operator.AND) {
            result = ctx.mkBVAND(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.OR) {
            result = ctx.mkBVOR(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.XOR) {
            result = ctx.mkBVXOR(l, r);
            resultType = determineResultType(leftType, rightType);
        } else if (operator == InfixExpression.Operator.LEFT_SHIFT) {
            result = ctx.mkBVSHL(l, r);
            resultType = leftType != null ? leftType : TypedExpr.JavaType.INT;
        } else if (operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
            result = ctx.mkBVASHR(l, r);
            resultType = leftType != null ? leftType : TypedExpr.JavaType.INT;
        } else if (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
            result = ctx.mkBVLSHR(l, r);
            resultType = leftType != null ? leftType : TypedExpr.JavaType.INT;
        } else {
            throw new RuntimeException("Unsupported infix operator for bit‑vectors: " + operator);
        }
        
        if (resultType != null) {
            typedExpr.put(result, resultType);
        }
        return result;
    }

    private static TypedExpr.JavaType determineResultType(TypedExpr.JavaType leftType, TypedExpr.JavaType rightType) {
        if (leftType == TypedExpr.JavaType.DOUBLE || rightType == TypedExpr.JavaType.DOUBLE) {
            return TypedExpr.JavaType.DOUBLE;
        } else if (leftType == TypedExpr.JavaType.FLOAT || rightType == TypedExpr.JavaType.FLOAT) {
            return TypedExpr.JavaType.FLOAT;
        } else if (leftType == TypedExpr.JavaType.LONG || rightType == TypedExpr.JavaType.LONG) {
            return TypedExpr.JavaType.LONG;
        } else if (leftType == TypedExpr.JavaType.INT || rightType == TypedExpr.JavaType.INT) {
            return TypedExpr.JavaType.INT;
        } else if (leftType == TypedExpr.JavaType.SHORT || rightType == TypedExpr.JavaType.SHORT) {
            return TypedExpr.JavaType.INT;
        } else if (leftType == TypedExpr.JavaType.CHAR || rightType == TypedExpr.JavaType.CHAR) {
            return TypedExpr.JavaType.INT;
        } else if (leftType == TypedExpr.JavaType.BYTE || rightType == TypedExpr.JavaType.BYTE) {
            return TypedExpr.JavaType.INT;
        } else {
            return TypedExpr.JavaType.INT;
        }
    }

    private static boolean isFP(Expr<?> e) {
        return e.getSort() instanceof FPSort;
    }

    private static FPSort pickFpSort(Expr<?> a, Expr<?> b, Context ctx) {
        Sort sa = a.getSort();
        Sort sb = b.getSort();
        FPSort fa = sa instanceof FPSort ? (FPSort) sa : null;
        FPSort fb = sb instanceof FPSort ? (FPSort) sb : null;

        if (fa != null && fb != null) {
            if (fa.getEBits() == fb.getEBits() && fa.getSBits() == fb.getSBits()) {
                return fa;
            }
            return ctx.mkFPSort64();
        } else if (fa != null) {
            return fa;
        } else if (fb != null) {
            return fb;
        } else {
            throw new IllegalArgumentException("No FP sort in operands");
        }
    }

    private static FPExpr coerceToFP(Context ctx, Expr<?> e, FPSort target, FPRMExpr rm) {
        if (e instanceof FPExpr) {
            FPSort s = (FPSort) e.getSort();
            if (s.getEBits() == target.getEBits() && s.getSBits() == target.getSBits()) {
                return (FPExpr) e;
            }
            return ctx.mkFPToFP(rm, (FPExpr) e, target);
        }
        if (e instanceof BitVecExpr) {
            return ctx.mkFPToFP(rm, (BitVecExpr) e, target, true);
        }
        throw new IllegalArgumentException("Cannot coerce non-numeric expr " + e);
    }

    private static BitVecExpr fixBvWidth(Context ctx, BitVecExpr e, int targetSz, boolean signExt) {
        int curSz = e.getSortSize();
        if (curSz == targetSz) return e;
        return curSz < targetSz
                ? (signExt ? ctx.mkSignExt(targetSz - curSz, e) : ctx.mkZeroExt(targetSz - curSz, e))
                : ctx.mkExtract(targetSz - 1, 0, e);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(left.toString()).append(operator.toString()).append(right.toString());
        if (extendedOperands == null) {
            return sb.toString();
        }
        for (AstNode operand : extendedOperands) {
            sb.append(operator.toString()).append(operand.toString());
        }
        return sb.toString();
    }

}
