package OldConcolic;

import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;

public class OldStatementNode {
    @SuppressWarnings("PatternVariableCanBeUsed")
    public static AstNode executeStatement(Statement statement, MemoryModel memoryModel) {
        if (statement instanceof ExpressionStatement) {
            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            return OldExpressionNode.executeExpression(expressionStatement.getExpression(), memoryModel);
        } else if (statement instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement varDeclStatement = (VariableDeclarationStatement) statement;
            return OldVariableDeclaration.executeVariableDeclarationStatement(varDeclStatement, memoryModel);
        } else if (statement instanceof ReturnStatement) {
            ReturnStatement returnStatement = (ReturnStatement) statement;
            return OldExpressionNode.executeExpression(returnStatement.getExpression(), memoryModel);
        } else if (statement instanceof BreakStatement) {
            return null;
        } else if (statement instanceof ContinueStatement) {
            return null;
        } else {
            throw new RuntimeException("Unknown statement type: " + statement.getClass());
        }
    }
}

