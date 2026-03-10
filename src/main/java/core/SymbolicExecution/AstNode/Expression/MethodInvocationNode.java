package core.SymbolicExecution.AstNode.Expression;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.PrimitiveVariable;
import core.SymbolicExecution.Variable.Variable;
import core.TestGeneration.ConcolicTesting;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class MethodInvocationNode extends ExpressionNode {
    public static AstNode executeMethodInvocation(MethodInvocation methodInvocation, MemoryModel memoryModel) {
//        if (methodInvocation.getExpression() == null) {
//            // method invocation in the same class
//            MethodDeclaration methodDeclaration = getInvokedMethodAST(methodInvocation);
//            return declareStubVariable(methodInvocation, methodDeclaration, memoryModel);
//        } else {
//            // method invocation outside the class or in libs
//            Class<?> invokedMethodReturnClass = getInvokedMethodReturnClass(methodInvocation);
//            return declareStubVariable(methodInvocation, invokedMethodReturnClass, memoryModel);
//        }
        return null;
    }


    private static MethodDeclaration getInvokedMethodAST(MethodInvocation methodInvocation) {
       return getInvokedMethodAST(methodInvocation, ConcolicTesting.unitsASTNodeList);
    }

    public static MethodDeclaration getInvokedMethodAST(MethodInvocation methodInvocation,
                                                        List<ASTNode> listOfMethods) {
        String methodName = methodInvocation.getName().getIdentifier();
        @SuppressWarnings("unchecked")
        List<ASTNode> argumentNodes = methodInvocation.arguments();
        int argumentCount = argumentNodes.size();

        for (ASTNode node : listOfMethods) {
            if (node instanceof MethodDeclaration) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                MethodDeclaration methodDecl = (MethodDeclaration) node;
                IMethodBinding methodBinding = methodDecl.resolveBinding();

                if (methodBinding == null) {
                    continue;
                }

                if (methodBinding.getName().equals(methodName)) {
                    ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();

                    if (parameterTypes.length == argumentCount) {
                        boolean allMatch = true;

                        for (int i = 0; i < argumentNodes.size(); i++) {
                            ITypeBinding argBinding = resolveExpressionBinding(argumentNodes.get(i));

                            if (argBinding == null || !argBinding.isAssignmentCompatible(parameterTypes[i])) {
                                allMatch = false;
                                break;
                            }
                        }

                        if (allMatch) return methodDecl;
                    }
                }
            }
        }

        for (ASTNode node : listOfMethods) {
            if (node instanceof MethodDeclaration) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                MethodDeclaration methodDecl = (MethodDeclaration) node;
                
                IMethodBinding methodBinding = methodDecl.resolveBinding();
                if (methodBinding != null) {
                    continue;
                }

                if (!methodDecl.getName().getIdentifier().equals(methodName)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<SingleVariableDeclaration> methodParams = methodDecl.parameters();
                if (methodParams.size() == argumentCount) {
                    return methodDecl;
                }
            }
        }

        throw new RuntimeException("Could not find a matching method for: " + methodName + 
                " with " + argumentCount + " argument(s)");
    }

    private static ITypeBinding resolveExpressionBinding(ASTNode node) {
        if (node instanceof Expression) {
            return ((Expression) node).resolveTypeBinding();
        }
        return null;
    }

    /**
     * return the return type of the method invocation in fully qualified name
     * (e.g., "java.lang.String" or "com.app.User"), fallback to "Object"
     * @param methodInvocation AST node of the method invocation
     * @return String representing the return type
     */
    public static String getInvokedMethodReturnTypeName(MethodInvocation methodInvocation) {
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();

        if (methodBinding != null) {
            ITypeBinding returnType = methodBinding.getReturnType();

            return returnType.getQualifiedName();
        }

        // Fallback: use Java reflection to resolve the return type when binding is unavailable
        // (e.g., when the parser was not configured with the JRE classpath)
        return resolveReturnTypeViaReflection(methodInvocation);
    }

    /**
     * Attempts to resolve the return type of a method invocation using Java reflection.
     * Handles cases like Math.min(x, y) where the expression is a simple class name.
     *
     * @param methodInvocation the method invocation node
     * @return the fully qualified return type name, or "Object" if resolution fails
     */
    private static String resolveReturnTypeViaReflection(MethodInvocation methodInvocation) {
        Expression expr = methodInvocation.getExpression();
        if (expr == null) {
            return "Object";
        }

        String className = expr.toString();
        String methodName = methodInvocation.getName().getIdentifier();
        @SuppressWarnings("unchecked")
        List<ASTNode> arguments = methodInvocation.arguments();
        int argCount = arguments.size();

        // Try common packages to resolve the class
        String[] candidatePackages = { "java.lang.", "java.util.", "java.io.", "java.math.", "" };
        for (String pkg : candidatePackages) {
            try {
                Class<?> clazz = Class.forName(pkg + className);
                for (java.lang.reflect.Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                        Class<?> returnType = method.getReturnType();
                        if (returnType.isPrimitive()) {
                            return returnType.getName();
                        }
                        return returnType.getCanonicalName();
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // try next package
            }
        }

        return "Object";
    }

    /**
     * using java reflection to get the return type class of the method invocation, fallback to Object.class
     * @param methodInvocation AST node of the method invocation
     * @return Class<?> representing the return type
     */
    public static Class<?> getInvokedMethodReturnClass(MethodInvocation methodInvocation) {
        String returnTypeName = getInvokedMethodReturnTypeName(methodInvocation);

        switch (returnTypeName) {
            case "int":     return int.class;
            case "long":    return long.class;
            case "double":  return double.class;
            case "float":   return float.class;
            case "boolean": return boolean.class;
            case "char":    return char.class;
            case "byte":    return byte.class;
            case "short":   return short.class;
            case "void":    return void.class;
            default:
                try {
                    return Class.forName(returnTypeName);
                } catch (ClassNotFoundException e) {
                    return Object.class;
                }
        }
    }

    private static AstNode declareStubVariable(MethodInvocation methodInvocation,
                                               MethodDeclaration methodDeclaration,
                                               MemoryModel memoryModel) {
        Type returnType = methodDeclaration.getReturnType2();
        String stubName = buildStubName(methodInvocation);
        SimpleNameNode stubVariableAstNode= SimpleNameNode.of(stubName);
        replaceMethodInvocationWithStub(methodInvocation, stubName);
        if (returnType instanceof PrimitiveType) {
            if (returnType.toString().equals("void")) {
                return null;
            }
            Variable stubVariable = new PrimitiveVariable( (PrimitiveType) returnType, stubName);
            memoryModel.declareVariable(stubVariable, stubVariableAstNode);
            stubVariable.setParameter(true);
            addStubVariableToParameterList(stubName, returnType);
            return stubVariableAstNode;
        } else if (returnType instanceof ArrayType) {
            throw new RuntimeException("Unexpected array type");
        } else {
            throw new RuntimeException("Invalid type");
        }
    }

    public static AstNode declareStubVariable(MethodInvocation methodInvocation,
                                               Class<?> returnTypeClass,
                                               MemoryModel memoryModel) {
        String stubName = buildStubName(methodInvocation);
        SimpleNameNode stubVariableAstNode= SimpleNameNode.of(stubName);
        replaceMethodInvocationWithStub(methodInvocation, stubName);
        if (returnTypeClass.isPrimitive()) {
            if (returnTypeClass == void.class) {
                return null;
            }
            PrimitiveType primitiveType = getPrimitiveTypeFromClass(returnTypeClass, methodInvocation.getAST());
            Variable stubVariable = new PrimitiveVariable(primitiveType, stubName);
            memoryModel.declareVariable(stubVariable, stubVariableAstNode);
            stubVariable.setParameter(true);
            addStubVariableToParameterList(stubName, primitiveType);
            return stubVariableAstNode;
        } else if (returnTypeClass.isArray()) {
            throw new UnsupportedOperationException("Stub not supported array type yet!");
        } else {
            throw new UnsupportedOperationException("Stub not supported non-primitive type yet!");
        }
    }

    public static PrimitiveType getPrimitiveTypeFromClass(Class<?> clazz, AST ast) {
        if (clazz == int.class) {
            return ast.newPrimitiveType(PrimitiveType.INT);
        } else if (clazz == boolean.class) {
            return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        } else if (clazz == byte.class) {
            return ast.newPrimitiveType(PrimitiveType.BYTE);
        } else if (clazz == short.class) {
            return ast.newPrimitiveType(PrimitiveType.SHORT);
        } else if (clazz == char.class) {
            return ast.newPrimitiveType(PrimitiveType.CHAR);
        } else if (clazz == long.class) {
            return ast.newPrimitiveType(PrimitiveType.LONG);
        } else if (clazz == float.class) {
            return ast.newPrimitiveType(PrimitiveType.FLOAT);
        } else if (clazz == double.class) {
            return ast.newPrimitiveType(PrimitiveType.DOUBLE);
        } else if (clazz == void.class) {
            return ast.newPrimitiveType(PrimitiveType.VOID);
        } else {
            throw new RuntimeException("Unsupported primitive type: " + clazz.getName());
        }
    }

    public static void replaceMethodInvocationWithStub(MethodInvocation methodInvocation, String stubName) {
        AST ast = methodInvocation.getAST();
        SimpleName stubNode = ast.newSimpleName(stubName);

        ASTNode parent = methodInvocation.getParent();
        StructuralPropertyDescriptor location = methodInvocation.getLocationInParent();

        if (location.isChildProperty()) {
            parent.setStructuralProperty(location, stubNode);
        } else if (location.isChildListProperty()) {
            @SuppressWarnings("unchecked")
            List<ASTNode> list = (List<ASTNode>) parent.getStructuralProperty(location);
            int index = list.indexOf(methodInvocation);
            list.set(index, stubNode);
        }
    }

    private static void addStubVariableToParameterList(String stubName, Type returnType) {
        addStubVariableToParameterList(stubName, returnType, (MethodDeclaration) ConcolicTesting.testUnit);
    }

    public static void addStubVariableToParameterList(String stubName, Type returnType,
                                                      MethodDeclaration methodInvokedStub) {
        if (hasParameter(methodInvokedStub, stubName)) {
            return;
        }
        AST ast = methodInvokedStub.getAST();
        SingleVariableDeclaration singleVariableDeclaration = ast.newSingleVariableDeclaration();
        singleVariableDeclaration.setName(ast.newSimpleName(stubName));
        singleVariableDeclaration.setType((Type) ASTNode.copySubtree(ast, returnType));
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = methodInvokedStub.parameters();
        parameters.add(singleVariableDeclaration);
    }

    private static String buildStubName(MethodInvocation mi) {
        String methodName = mi.getName().getIdentifier();
        int pos = mi.getStartPosition();
        return methodName + "_call_at_" + pos;
    }

    private static boolean hasParameter(MethodDeclaration method, String name) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = method.parameters();
        for (SingleVariableDeclaration p : params) {
            if (p.getName().getIdentifier().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
