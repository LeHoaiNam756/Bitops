package core.SymbolicExecution.AstNode;

import core.SymbolicExecution.AstNode.Expression.Array.ArrayCreationNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayInitializerNode;
import core.SymbolicExecution.SymbolicExecution;
import core.SymbolicExecution.Variable.ArrayVariable;
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
        ArrayVariable arrayVariable = new ArrayVariable((ArrayType) arrayType, name);
        AstNode initValue = null;

        if (initializer != null) {
            if (initializer instanceof ArrayCreation) {
                ArrayCreationNode creationNode =
                        ArrayCreationNode.executeArrayCreation((ArrayCreation) initializer, memoryModel);
                initValue = creationNode.getArraySymbolicRepresent();

            } else if (initializer instanceof ArrayInitializer) {
                initValue = ArrayInitializerNode.executeArrayInitializer(
                        (ArrayInitializer) initializer, memoryModel);

            } else {
                initValue = ExpressionNode.executeExpression(initializer, memoryModel);
            }
            arrayVariable.setParameter(SymbolicExecution.isRelatedToParameter);
        }

        memoryModel.declareVariable(arrayVariable, initValue);
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
        PrimitiveVariable variable = new PrimitiveVariable((PrimitiveType) baseType, name);
        AstNode initValue = null;

        if (initializer != null) {
            initValue = ExpressionNode.executeExpression(initializer, memoryModel);
            variable.setParameter(SymbolicExecution.isRelatedToParameter);
        }

        memoryModel.declareVariable(variable, initValue);
    }

    private static void executeVariableDeclarationFragment(VariableDeclarationFragment fragment,
                                                           Type baseType,
                                                           MemoryModel memoryModel) {
        String     name = fragment.getName().getIdentifier();
        Expression init = fragment.getInitializer();
        declarePrimitiveVariable(baseType, name, init, memoryModel);
    }
}