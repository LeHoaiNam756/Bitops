package core.SymbolicExecution.AstNode;

import core.SymbolicExecution.SymbolicExecution;
import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.PrimitiveVariable;

import java.util.List;

public class VariableDeclarationNode extends ExpressionNode {

    public static AstNode executeVariableDeclarationExpression(VariableDeclarationExpression expr,
                                                               MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = expr.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            VariableDeclarationNode.executeVariableDeclarationFragment(fragment, expr.getType(), memoryModel);
        }
        return null;
    }

    public static AstNode executeVariableDeclarationStatement(VariableDeclarationStatement stmt,
                                                               MemoryModel memoryModel) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = stmt.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            VariableDeclarationNode.executeVariableDeclarationFragment(fragment, stmt.getType(), memoryModel);
        }
        return null;
    }

    public static AstNode executeVariableDeclaration(VariableDeclaration variableDeclaration,
                                                   MemoryModel memoryModel) {
        if (variableDeclaration instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) variableDeclaration;
            String name = svd.getName().getIdentifier();
            Type type = svd.getType();
            declarePrimitiveVariable(type, name, svd.getInitializer(), memoryModel);
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

        PrimitiveVariable variable = new PrimitiveVariable((PrimitiveType) baseType, name);
        AstNode initValue = null;

        if (initializer != null) {
            initValue = ExpressionNode.executeExpression(initializer, memoryModel);
            //TODO: temporary check if the initializer is parameter, change isParameter to true
            variable.setParameter(SymbolicExecution.isRelatedToParameter);
        }

        memoryModel.declareVariable(variable, initValue);
    }

    private static void executeVariableDeclarationFragment(VariableDeclarationFragment fragment,
                                                           Type baseType, MemoryModel memoryModel) {
        String name = fragment.getName().getIdentifier();
        Expression initializer = fragment.getInitializer();
        declarePrimitiveVariable(baseType, name, initializer, memoryModel);
    }
}
