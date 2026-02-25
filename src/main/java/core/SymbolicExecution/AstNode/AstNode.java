package core.SymbolicExecution.AstNode;

import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Statement.StatementNode;
import core.SymbolicExecution.MemoryModel;

public abstract class AstNode {
    public static AstNode executeASTNode(ASTNode ast, MemoryModel memoryModel) {
        if (ast instanceof Expression) {
            return ExpressionNode.executeExpression((Expression) ast, memoryModel);
        } else if (ast instanceof Statement) {
            return StatementNode.executeStatement((Statement) ast, memoryModel);
        } else if (ast instanceof VariableDeclaration) {
            return VariableDeclarationNode.executeVariableDeclaration((VariableDeclaration) ast, memoryModel);
        } else {
            throw new RuntimeException("Unknown ASTNode type: " + ast.getClass());
        }
    }

    public static void replaceMethodInvocationWithStub(ASTNode originStatement, MethodInvocation originMethodInvocation,
                                                       ASTNode replacement) {
        return;
    }

}
