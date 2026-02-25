package core.SymbolicExecution.AstNode.Statement;

import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.VariableDeclarationNode;
import core.SymbolicExecution.MemoryModel;

public class StatementNode extends AstNode {
    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeStatement(Statement statement, MemoryModel memoryModel) {
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            return ExpressionNode.executeExpression(expressionStatement.getExpression(), memoryModel);
        } else if (statement instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement varDeclStatement = (VariableDeclarationStatement) statement;
            return VariableDeclarationNode.executeVariableDeclarationStatement(varDeclStatement, memoryModel);
        } else if (statement instanceof ReturnStatement) {
            ReturnStatement returnStatement = (ReturnStatement) statement;
            return ExpressionNode.executeExpression(returnStatement.getExpression(), memoryModel);
        } else if (statement instanceof BreakStatement) {
            return null;
        } else if (statement instanceof ContinueStatement) {
            return null;
        } else {
            throw new RuntimeException("Unknown statement type: " + statement.getClass());
        }
    }
}
