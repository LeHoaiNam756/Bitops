package core.SymbolicExecution.AstNode;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayCreationNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayInitializerNode;
import core.SymbolicExecution.SymbolicExecution;
import core.SymbolicExecution.TypedExpr;
import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.model.SymbolicValueTemp;

import java.util.List;

public class VariableDeclarationNode extends ExpressionNode {

    public static AstNode executeVariableDeclarationExpression(VariableDeclarationExpression expr,
                                                               MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = expr.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            Type type = expr.getType();
            if (type instanceof ArrayType) {
                executeArrayDeclarationFragment(fragment, (ArrayType) type, memoryModel);
            } else {
                executeVariableDeclarationFragment(fragment, type, memoryModel);
            }
        }
        return null;
    }

    public static AstNode executeVariableDeclarationStatement(VariableDeclarationStatement stmt,
                                                              MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = stmt.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            Type type = stmt.getType();
            if (type instanceof ArrayType) {
                executeArrayDeclarationFragment(fragment, (ArrayType) type, memoryModel);
            } else {
                executeVariableDeclarationFragment(fragment, type, memoryModel);
            }
        }
        return null;
    }

    public static AstNode executeVariableDeclaration(VariableDeclaration variableDeclaration,
                                                     MemoryModel memoryModel) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) variableDeclaration;
            String name         = svd.getName().getIdentifier();
            Type   type         = svd.getType();
            Expression init     = svd.getInitializer();
            if (type instanceof PrimitiveType) {
                declarePrimitiveVariable(type, name, init, memoryModel);
            } else if (type instanceof ArrayType) {
                declareArrayVariable(type, name, init, memoryModel);
            } else {
                throw new RuntimeException("Unsupported variable declaration type: " + type);
            }
        } else if (variableDeclaration instanceof VariableDeclarationFragment) {
            throw new RuntimeException("Not implemented yet for VariableDeclarationFragment");
        } else {
            throw new RuntimeException(variableDeclaration.getClass() + " is not a VariableDeclaration");
        }
        return null;
    }

    private static void declareArrayVariable(Type arrayType,
                                             String name,
                                             Expression initializer,
                                             MemoryModel memoryModel) {
//        ArrayType arrType = (ArrayType) arrayType;
//        TypedExpr.JavaType elementType = MemoryModel.mapPrimitiveType((PrimitiveType) arrType.getElementType());
//        Expr<?> arrayExpr = createArrayZ3Expr(name, arrType, memoryModel.getContext());
//        ArraySymbolicValueTemp arrayValue = ArraySymbolicValueTemp.of(name, elementType, arrayExpr,
//                SymbolicExecution.isRelatedToParameter, arrType.getDimensions());
//        AstNode initValue = null;
//
//        if (initializer != null) {
//            if (initializer instanceof ArrayCreation) {
//                ArrayCreationNode creationNode =
//                        ArrayCreationNode.executeArrayCreation((ArrayCreation) initializer, memoryModel);
//                initValue = creationNode.getArraySymbolicRepresent();
//
//            } else if (initializer instanceof ArrayInitializer) {
//                initValue = ArrayInitializerNode.executeArrayInitializer(
//                        (ArrayInitializer) initializer, memoryModel);
//
//            } else {
//                initValue = ExpressionNode.executeExpression(initializer, memoryModel);
//            }
//        }
//
//        memoryModel.declareVariable(arrayValue, initValue);
    }

    private static com.microsoft.z3.Expr<?> createArrayZ3Expr(String name, ArrayType arrayType, com.microsoft.z3.Context ctx) {
        com.microsoft.z3.Sort domain = ctx.mkBitVecSort(32);
        com.microsoft.z3.Sort currentSort = getBaseSort(arrayType.getElementType(), ctx);

        for (int i = 0; i < arrayType.getDimensions(); i++) {
            currentSort = ctx.mkArraySort(domain, currentSort);
        }

        return ctx.mkConst(name, currentSort);
    }

    private static com.microsoft.z3.Sort getBaseSort(Type elementType, com.microsoft.z3.Context ctx) {
        if (elementType.isPrimitiveType()) {
            PrimitiveType pt = (PrimitiveType) elementType;
            PrimitiveType.Code code = pt.getPrimitiveTypeCode();
            if (code.equals(PrimitiveType.BYTE)) {
                return ctx.mkBitVecSort(8);
            } else if (code.equals(PrimitiveType.CHAR)) {
                return ctx.mkBitVecSort(16);
            } else if (code.equals(PrimitiveType.SHORT)) {
                return ctx.mkBitVecSort(16);
            } else if (code.equals(PrimitiveType.INT)) {
                return ctx.mkBitVecSort(32);
            } else if (code.equals(PrimitiveType.LONG)) {
                return ctx.mkBitVecSort(64);
            } else if (code.equals(PrimitiveType.FLOAT)) {
                return ctx.mkFPSort32();
            } else if (code.equals(PrimitiveType.DOUBLE)) {
                return ctx.mkFPSort64();
            } else if (code.equals(PrimitiveType.BOOLEAN)) {
                return ctx.mkBoolSort();
            }
        }
        throw new IllegalArgumentException("Unsupported array element type: " + elementType);
    }

    private static void executeArrayDeclarationFragment(VariableDeclarationFragment fragment,
                                                        ArrayType arrayType,
                                                        MemoryModel memoryModel) {
        String     name = fragment.getName().getIdentifier();
        Expression init = fragment.getInitializer();
        declareArrayVariable(arrayType, name, init, memoryModel);
    }

    public static void declarePrimitiveVariable(Type baseType,
                                                String name,
                                                Expression initializer,
                                                MemoryModel memoryModel) {
//        PrimitiveType primitiveType = (PrimitiveType) baseType;
//        TypedExpr.JavaType javaType = MemoryModel.mapPrimitiveType(primitiveType);
//        Expr<?> expr = MemoryModel.createZ3ExprFromType(name, javaType, memoryModel.getContext());
//        SymbolicValueTemp value = SymbolicValueTemp.of(name, javaType, expr, SymbolicExecution.isRelatedToParameter);
//        AstNode initValue = null;
//
//        if (initializer != null) {
//            initValue = ExpressionNode.executeExpression(initializer, memoryModel);
//        }
//
//        memoryModel.declareVariable(value, initValue);
    }

    private static void executeVariableDeclarationFragment(VariableDeclarationFragment fragment,
                                                           Type baseType,
                                                           MemoryModel memoryModel) {
        String     name = fragment.getName().getIdentifier();
        Expression init = fragment.getInitializer();
        declarePrimitiveVariable(baseType, name, init, memoryModel);
    }
}
