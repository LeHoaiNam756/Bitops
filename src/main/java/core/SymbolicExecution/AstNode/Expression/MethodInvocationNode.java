package core.SymbolicExecution.AstNode.Expression;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.PrimitiveVariable;
import core.SymbolicExecution.Variable.Variable;
import core.TestGeneration.ConcolicTesting;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.ArrayList;
import java.util.List;

public class MethodInvocationNode extends ExpressionNode {
    private static int numberOfFunctionsCall = 1;

    public static AstNode executeMethodInvocation(MethodInvocation methodInvocation, MemoryModel memoryModel) {
        if (methodInvocation.getExpression() == null) {
            // method invocation in the same class
            MethodDeclaration methodDeclaration = getInvokedMethodAST(methodInvocation);
            return declareStubVariable(methodInvocation, methodDeclaration, memoryModel);
        } else {
            // method invocation outside the class or in libs
//            Class<?> invokedMethodReturnClass = getInvokedMethodReturnClass(methodInvocation, memoryModel);
//            return declareStubVariable(methodName, invokedMethodReturnClass, memoryModel, methodInvocation);
            return null;
        }
    }


    private static MethodDeclaration getInvokedMethodAST(MethodInvocation methodInvocation) {
       return getInvokedMethodAST(methodInvocation, ConcolicTesting.unitsASTNodeList);
    }

    public static MethodDeclaration getInvokedMethodAST(MethodInvocation methodInvocation,
                                                        List<ASTNode> listOfMethods) {
        String methodName = methodInvocation.getName().getIdentifier();
        @SuppressWarnings("unchecked")
        List<ASTNode> argumentNodes = methodInvocation.arguments();

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

                    if (parameterTypes.length == argumentNodes.size()) {
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
        throw new RuntimeException("Could not find a matching binding for: " + methodName);
    }

    private static ITypeBinding resolveExpressionBinding(ASTNode node) {
        if (node instanceof Expression) {
            return ((Expression) node).resolveTypeBinding();
        }
        return null;
    }

    private static AstNode declareStubVariable(MethodInvocation methodInvocation,
                                               MethodDeclaration methodDeclaration,
                                               MemoryModel memoryModel) {
        Type returnType = methodDeclaration.getReturnType2();
        String methodName = methodInvocation.getName().getIdentifier();
        String stubName = methodName + "_call_" + numberOfFunctionsCall;
        numberOfFunctionsCall++;
        SimpleNameNode stubVariableAstNode= SimpleNameNode.of(stubName);
        replaceMethodInvocationWithStub(methodInvocation, stubName);
        if (returnType instanceof PrimitiveType) {
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
        AST ast = methodInvokedStub.getAST();
        SingleVariableDeclaration singleVariableDeclaration = ast.newSingleVariableDeclaration();
        singleVariableDeclaration.setName(ast.newSimpleName(stubName));
        singleVariableDeclaration.setType((Type) ASTNode.copySubtree(ast, returnType));
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = methodInvokedStub.parameters();
        parameters.add(singleVariableDeclaration);
    }
}
