package OldConcolic;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.VariableDeclarationNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class OldVariableDeclaration {

    public static AstNode executeVariableDeclarationExpression(VariableDeclarationExpression expr,
                                                               MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = expr.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            OldVariableDeclaration.executeVariableDeclarationFragment(fragment, expr.getType(), memoryModel);
        }
        return null;
    }

    public static AstNode executeVariableDeclarationStatement(VariableDeclarationStatement stmt,
                                                               MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = stmt.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            OldVariableDeclaration.executeVariableDeclarationFragment(fragment, stmt.getType(), memoryModel);
        }
        return null;
    }

    public static AstNode executeVariableDeclaration(VariableDeclaration variableDeclaration,
                                                   MemoryModel memoryModel) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) variableDeclaration;
            String name = svd.getName().getIdentifier();
            Type type = svd.getType();
            OldVariableDeclaration.declarePrimitiveVariable(type, name, svd.getInitializer(), memoryModel);
        } else if (variableDeclaration instanceof VariableDeclarationFragment) {
            throw new RuntimeException("Not implemented yet for VariableDeclarationFragment");
        } else {
            throw new RuntimeException(variableDeclaration.getClass() + " is not a VariableDeclaration");
        }
        return null;
    }

    public static void declarePrimitiveVariable(Type baseType,
                                                String name,
                                                Expression initializer,
                                                MemoryModel memoryModel) {
        if (!(baseType instanceof PrimitiveType)) {
            if (baseType instanceof ArrayType) {
                throw new Error("unexpected array type");
            }
            throw new RuntimeException(baseType.getClass() + " is invalid!!");
        }

        OldPrimitiveVariable variable = new OldPrimitiveVariable((PrimitiveType) baseType, name);
        AstNode initValue = null;

        if (initializer != null) {
            initValue = ExpressionNode.executeExpression(initializer, memoryModel);
            //TODO: temporary check if the initializer is parameter, change isParameter to true
            if (OldSymbolicExecution.isRelatedToParameter) {
                variable.setParameter(true);
            }
        }

        memoryModel.declareVariable(variable, initValue);
    }

    private static void executeVariableDeclarationFragment(VariableDeclarationFragment fragment,
                                                           Type baseType, MemoryModel memoryModel) {
        String name = fragment.getName().getIdentifier();
        Expression initializer = fragment.getInitializer();
        OldVariableDeclaration.declarePrimitiveVariable(baseType, name, initializer, memoryModel);
    }
}
