package core.SymbolicExecution.AstNode.Expression;

import com.microsoft.z3.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.TypedExpr;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ConditionalExpression;

@Setter
public class ConditionalExpressionNode extends ExpressionNode {
    private AstNode condition;
    private AstNode thenExpression;
    private AstNode elseExpression;

    // -----------------------------------------------------------------------
    // Concrete-value path
    // -----------------------------------------------------------------------

    public static AstNode executeConditionalExpression(ConditionalExpression conditionalExpression,
                                                       MemoryModel memoryModel) {
        ConditionalExpressionNode node = new ConditionalExpressionNode();
        node.condition      = AstNode.executeASTNode(conditionalExpression.getExpression(), memoryModel);
        node.thenExpression = AstNode.executeASTNode(conditionalExpression.getThenExpression(), memoryModel);
        node.elseExpression = AstNode.executeASTNode(conditionalExpression.getElseExpression(), memoryModel);
        return executeConditionalExpressionNode(node, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeConditionalExpressionNode(ConditionalExpressionNode node,
                                                           MemoryModel memoryModel) {
        AstNode condition = node.condition;

        // Condition resolved to a concrete boolean — pick the branch
        if (condition instanceof LiteralBooleanNode) {
            LiteralBooleanNode boolCondition = (LiteralBooleanNode) condition;
            return boolCondition.isValue() ? node.thenExpression : node.elseExpression;
        }

        // Condition is still symbolic — keep the whole node for Z3
        return node;
    }

    // -----------------------------------------------------------------------
    // Z3 / symbolic path
    // -----------------------------------------------------------------------

    public static Expr<?> convertConditionalExpressionToZ3Expr(ConditionalExpressionNode astNode,
                                                               Context ctx,
                                                               MemoryModel memoryModel) {
        Expr<?> conditionExpr    = ExpressionNode.convertAstNodeToZ3Expr(astNode.condition,      ctx, memoryModel);
        Expr<?> thenExpr         = ExpressionNode.convertAstNodeToZ3Expr(astNode.thenExpression, ctx, memoryModel);
        Expr<?> elseExpr         = ExpressionNode.convertAstNodeToZ3Expr(astNode.elseExpression, ctx, memoryModel);

        if (!(conditionExpr instanceof BoolExpr)) {
            throw new RuntimeException("Condition of ternary expression must be boolean");
        }

        // Align branch types before building the ITE node
        Expr<?>[] aligned = alignBranchTypes(thenExpr, elseExpr, ctx);
        Expr<?> alignedThen = aligned[0];
        Expr<?> alignedElse = aligned[1];

        Expr<?> result = ctx.mkITE((BoolExpr) conditionExpr, alignedThen, alignedElse);

        // Result type follows the wider of the two branches (same as Java)
        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType thenType = typedExpr.getType(alignedThen);
        TypedExpr.JavaType elseType = typedExpr.getType(alignedElse);
        typedExpr.put(result, widerType(thenType, elseType));

        return result;
    }

    // -----------------------------------------------------------------------
    // Branch type alignment  (mirrors Java numeric promotion for ternary)
    // -----------------------------------------------------------------------

    /**
     * Returns a two-element array [alignedThen, alignedElse] whose Z3 sorts
     * match so that mkITE accepts them.  Follows JLS §15.25 numeric promotion.
     */
    private static Expr<?>[] alignBranchTypes(Expr<?> thenExpr,
                                              Expr<?> elseExpr,
                                              Context ctx) {
        // Same sort — nothing to do
        if (thenExpr.getSort().equals(elseExpr.getSort())) {
            return new Expr<?>[]{ thenExpr, elseExpr };
        }

        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType thenType = typedExpr.getType(thenExpr);
        TypedExpr.JavaType elseType = typedExpr.getType(elseExpr);

        // FP involved — promote the non-FP branch or widen to the wider FP sort
        boolean thenFP = thenExpr instanceof FPExpr;
        boolean elseFP = elseExpr instanceof FPExpr;

        if (thenFP || elseFP) {
            FPSort targetSort = widenFPSort(thenExpr, elseExpr, thenType, elseType, ctx);
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();

            Expr<?> newThen = toFP(thenExpr, thenType, targetSort, rm, ctx);
            Expr<?> newElse = toFP(elseExpr, elseType, targetSort, rm, ctx);

            TypedExpr.JavaType targetJavaType =
                    targetSort.equals(ctx.mkFPSort64()) ? TypedExpr.JavaType.DOUBLE : TypedExpr.JavaType.FLOAT;
            typedExpr.put(newThen, targetJavaType);
            typedExpr.put(newElse, targetJavaType);

            return new Expr<?>[]{ newThen, newElse };
        }

        // Both BV — sign/zero-extend the narrower side
        if (thenExpr instanceof BitVecExpr && elseExpr instanceof BitVecExpr) {
            BitVecExpr bvThen = (BitVecExpr) thenExpr;
            BitVecExpr bvElse = (BitVecExpr) elseExpr;
            int thenSize = bvThen.getSortSize();
            int elseSize = bvElse.getSortSize();
            int targetSize = Math.max(thenSize, elseSize);

            Expr<?> newThen = extendBV(bvThen, thenType, thenSize, targetSize, ctx);
            Expr<?> newElse = extendBV(bvElse, elseType, elseSize, targetSize, ctx);

            TypedExpr.JavaType targetType = widerType(thenType, elseType);
            typedExpr.put(newThen, targetType);
            typedExpr.put(newElse, targetType);

            return new Expr<?>[]{ newThen, newElse };
        }

        // Bool / Bool mismatch or unsupported combination
        throw new RuntimeException(
                "Cannot align branch types for ternary: " + thenType + " vs " + elseType);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Widen a BV expression to targetSize bits. */
    private static BitVecExpr extendBV(BitVecExpr bv,
                                       TypedExpr.JavaType type,
                                       int currentSize,
                                       int targetSize,
                                       Context ctx) {
        if (currentSize == targetSize) return bv;
        int extension = targetSize - currentSize;
        if (type == TypedExpr.JavaType.CHAR) {
            return ctx.mkZeroExt(extension, bv);
        }
        return ctx.mkSignExt(extension, bv);
    }

    /** Convert any numeric expr to the given FP sort. */
    private static Expr<?> toFP(Expr<?> expr,
                                TypedExpr.JavaType type,
                                FPSort sort,
                                FPRMExpr rm,
                                Context ctx) {
        if (expr instanceof FPExpr) {
            return ctx.mkFPToFP(rm, (FPExpr) expr, sort);
        }
        if (expr instanceof BitVecExpr) {
            boolean signed = (type != TypedExpr.JavaType.CHAR);
            return ctx.mkFPToFP(rm, (BitVecExpr) expr, sort, signed);
        }
        throw new RuntimeException("Cannot convert to FP: " + expr.getSort());
    }

    /**
     * Picks the wider FP sort between the two branches.
     * If one branch is BV, the other branch's FP sort is used.
     */
    private static FPSort widenFPSort(Expr<?> thenExpr,
                                      Expr<?> elseExpr,
                                      TypedExpr.JavaType thenType,
                                      TypedExpr.JavaType elseType,
                                      Context ctx) {
        FPSort sort64 = ctx.mkFPSort64();
        FPSort sort32 = ctx.mkFPSort32();

        boolean thenDouble = (thenType == TypedExpr.JavaType.DOUBLE);
        boolean elseDouble = (elseType == TypedExpr.JavaType.DOUBLE);

        if (thenDouble || elseDouble) return sort64;
        return sort32;
    }

    /**
     * Returns the wider of two JavaTypes following JLS numeric promotion order:
     * double > float > long > int > short > char > byte > boolean
     */
    private static TypedExpr.JavaType widerType(TypedExpr.JavaType a, TypedExpr.JavaType b) {
        int rankA = typeRank(a);
        int rankB = typeRank(b);
        return (rankA >= rankB) ? a : b;
    }

    private static int typeRank(TypedExpr.JavaType type) {
        switch (type) {
            case DOUBLE:  return 7;
            case FLOAT:   return 6;
            case LONG:    return 5;
            case INT:     return 4;
            case SHORT:   return 3;
            case CHAR:    return 2;
            case BYTE:    return 1;
            case BOOLEAN: return 0;
            default: throw new RuntimeException("Unknown JavaType: " + type);
        }
    }
}
