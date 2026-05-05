package core.SymbolicExecution.AstNode.Expression;

import com.microsoft.z3.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.TypedExpr;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;


@Setter
public class CastExpressionNode extends ExpressionNode {
    private Type targetType;   // the AST Type node (e.g. "int", "double", …)
    private AstNode expression; // the value being cast

    // -----------------------------------------------------------------------
    // Concrete-value path
    // -----------------------------------------------------------------------

    public static AstNode executeCastExpression(CastExpression castExpression,
                                                MemoryModel memoryModel) {
        CastExpressionNode node = new CastExpressionNode();
        node.targetType = castExpression.getType();
        node.expression = AstNode.executeASTNode(castExpression.getExpression(), memoryModel);
        return executeCastExpressionNode(node, memoryModel);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeCastExpressionNode(CastExpressionNode castNode,
                                                    MemoryModel memoryModel) {
        AstNode expression = castNode.expression;

        if (expression instanceof LiteralNode) {
            LiteralNode literalNode = (LiteralNode) expression;
            return LiteralNode.analyzeCastLiteral(literalNode, castNode.targetType);
        }

        // Symbolic / unknown – keep as a cast node for later Z3 processing
        return castNode;
    }

    // -----------------------------------------------------------------------
    // Z3 / symbolic path
    // -----------------------------------------------------------------------

    public static Expr<?> convertCastExpressionToZ3Expr(CastExpressionNode astNode,
                                                        Context ctx,
                                                        MemoryModel memoryModel) {
        AstNode expression = astNode.expression;
        Type targetType = astNode.targetType;

        Expr<?> sourceExpr = ExpressionNode.convertAstNodeToZ3Expr(expression, ctx, memoryModel);
        TypedExpr typedExpr = TypedExpr.getInstance();
        TypedExpr.JavaType sourceType = typedExpr.getType(sourceExpr);
        TypedExpr.JavaType destType = resolveJavaType(targetType);

        return analyzeCastZ3Expr(sourceExpr, sourceType, destType, ctx);
    }

    // -----------------------------------------------------------------------
    // Core cast logic
    // -----------------------------------------------------------------------

    private static Expr<?> analyzeCastZ3Expr(Expr<?> sourceExpr,
                                             TypedExpr.JavaType sourceType,
                                             TypedExpr.JavaType destType,
                                             Context ctx) {
        TypedExpr typedExpr = TypedExpr.getInstance();

        // No-op cast
        if (sourceType == destType) {
            return sourceExpr;
        }

        Expr<?> result;

        // ── Floating-point source ──────────────────────────────────────────
        if (sourceExpr instanceof FPExpr) {
            FPExpr fpVal = (FPExpr) sourceExpr;
            FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();
            result = castFromFP(fpVal, rm, destType, sourceType, ctx);

            // ── BitVector source ───────────────────────────────────────────────
        } else if (sourceExpr instanceof BitVecExpr) {
            BitVecExpr bvVal = (BitVecExpr) sourceExpr;
            result = castFromBV(bvVal, sourceType, destType, ctx);

            // ── Boolean source ─────────────────────────────────────────────────
        } else if (sourceExpr instanceof BoolExpr) {
            // Java doesn't allow casting boolean to numeric, but keep the value
            if (destType == TypedExpr.JavaType.BOOLEAN) {
                result = sourceExpr;
            } else {
                throw new RuntimeException(
                        "Cannot cast boolean to " + destType);
            }

        } else {
            throw new RuntimeException(
                    "Unsupported source type for cast: " + sourceExpr.getSort());
        }

        typedExpr.put(result, destType);
        return result;
    }

    // -----------------------------------------------------------------------
    // FP → * casts
    // -----------------------------------------------------------------------

    private static Expr<?> castFromFP(FPExpr fpVal,
                                      FPRMExpr rm,
                                      TypedExpr.JavaType destType,
                                      TypedExpr.JavaType sourceType,
                                      Context ctx) {
        switch (destType) {
            case FLOAT:
                return ctx.mkFPToFP(rm, fpVal, ctx.mkFPSort32());
            case DOUBLE:
                return ctx.mkFPToFP(rm, fpVal, ctx.mkFPSort64());
            case LONG: {
                BitVecExpr bv64 = (BitVecExpr) ctx.mkFPToBV(rm, fpVal, 64, true);
                return bv64;
            }
            case INT: {
                BitVecExpr bv32 = (BitVecExpr) ctx.mkFPToBV(rm, fpVal, 32, true);
                return bv32;
            }
            case SHORT: {
                BitVecExpr bv32 = (BitVecExpr) ctx.mkFPToBV(rm, fpVal, 32, true);
                return ctx.mkExtract(15, 0, bv32);
            }
            case CHAR: {
                BitVecExpr bv32 = (BitVecExpr) ctx.mkFPToBV(rm, fpVal, 32, true);
                return ctx.mkExtract(15, 0, bv32);
            }
            case BYTE: {
                BitVecExpr bv32 = (BitVecExpr) ctx.mkFPToBV(rm, fpVal, 32, true);
                return ctx.mkExtract(7, 0, bv32);
            }
            default:
                throw new RuntimeException("Unsupported FP cast target: " + destType);
        }
    }

    // -----------------------------------------------------------------------
    // BV → * casts
    // -----------------------------------------------------------------------

    private static Expr<?> castFromBV(BitVecExpr bvVal,
                                      TypedExpr.JavaType sourceType,
                                      TypedExpr.JavaType destType,
                                      Context ctx) {
        int srcSize = bvVal.getSortSize();
        FPRMExpr rm = ctx.mkFPRoundNearestTiesToEven();

        // Promote small integer types to int32 first (Java numeric promotion)
        BitVecExpr promoted = promoteToBV32(bvVal, sourceType, srcSize, ctx);
        int promotedSize = promoted.getSortSize(); // 8,16,32, or 64

        switch (destType) {
            case DOUBLE: {
                boolean signed = (sourceType != TypedExpr.JavaType.CHAR);
                return ctx.mkFPToFP(rm, promoted, ctx.mkFPSort64(), signed);
            }
            case FLOAT: {
                boolean signed = (sourceType != TypedExpr.JavaType.CHAR);
                return ctx.mkFPToFP(rm, promoted, ctx.mkFPSort32(), signed);
            }
            case LONG: {
                if (promotedSize == 64) return promoted;
                if (sourceType == TypedExpr.JavaType.CHAR) {
                    return ctx.mkZeroExt(64 - promotedSize, promoted);
                }
                return ctx.mkSignExt(64 - promotedSize, promoted);
            }
            case INT: {
                if (promotedSize == 64) return ctx.mkExtract(31, 0, promoted);
                if (promotedSize == 32) return promoted;
                // byte/short/char already promoted to 32
                return promoted;
            }
            case SHORT: {
                if (promotedSize > 16) return ctx.mkExtract(15, 0, promoted);
                if (promotedSize == 16) return promoted;
                // 8-bit → sign/zero extend to 16, then truncate not needed
                if (sourceType == TypedExpr.JavaType.CHAR) {
                    return ctx.mkZeroExt(16 - promotedSize, bvVal);
                }
                return ctx.mkSignExt(16 - promotedSize, bvVal);
            }
            case CHAR: {
                // Narrowing: take low 16 bits (treat as unsigned char)
                if (promotedSize > 16) return ctx.mkExtract(15, 0, promoted);
                if (promotedSize == 16) return promoted;
                return ctx.mkZeroExt(16 - promotedSize, bvVal);
            }
            case BYTE: {
                if (promotedSize > 8) return ctx.mkExtract(7, 0, promoted);
                if (promotedSize == 8) return promoted;
                return ctx.mkSignExt(8 - promotedSize, bvVal);
            }
            case BOOLEAN:
                throw new RuntimeException("Cannot cast integer to boolean");
            default:
                throw new RuntimeException("Unsupported BV cast target: " + destType);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Promotes byte/char/short to int32 to match Java's numeric promotion rules.
     * Long (64-bit) and int (32-bit) are returned as-is.
     */
    private static BitVecExpr promoteToBV32(BitVecExpr bvVal,
                                            TypedExpr.JavaType sourceType,
                                            int srcSize,
                                            Context ctx) {
        switch (sourceType) {
            case CHAR:
                return ctx.mkZeroExt(32 - srcSize, bvVal);
            case BYTE:
            case SHORT:
                return ctx.mkSignExt(32 - srcSize, bvVal);
            default:
                return bvVal; // INT or LONG already correct size
        }
    }

    /**
     * Maps an AST {@link Type} to the corresponding {@link TypedExpr.JavaType}.
     */
    private static TypedExpr.JavaType resolveJavaType(Type type) {
        if (type.isPrimitiveType()) {
            PrimitiveType pt = (PrimitiveType) type;
            PrimitiveType.Code code = pt.getPrimitiveTypeCode();
            if (code == PrimitiveType.BOOLEAN) return TypedExpr.JavaType.BOOLEAN;
            if (code == PrimitiveType.BYTE)    return TypedExpr.JavaType.BYTE;
            if (code == PrimitiveType.CHAR)    return TypedExpr.JavaType.CHAR;
            if (code == PrimitiveType.SHORT)   return TypedExpr.JavaType.SHORT;
            if (code == PrimitiveType.INT)     return TypedExpr.JavaType.INT;
            if (code == PrimitiveType.LONG)    return TypedExpr.JavaType.LONG;
            if (code == PrimitiveType.FLOAT)   return TypedExpr.JavaType.FLOAT;
            if (code == PrimitiveType.DOUBLE)  return TypedExpr.JavaType.DOUBLE;
        }
        throw new RuntimeException("Unsupported cast target type: " + type);
    }
}
