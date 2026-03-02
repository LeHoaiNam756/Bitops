package OldConcolic;

import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.MemoryModel;

public class OldAstNode {
    public static AstNode executeASTNode(ASTNode ast, MemoryModel memoryModel) {
        if (ast instanceof Expression) {
            return OldExpressionNode.executeExpression((Expression) ast, memoryModel);
        } else if (ast instanceof Statement) {
            return OldStatementNode.executeStatement((Statement) ast, memoryModel);
        } else if (ast instanceof VariableDeclaration) {
            return OldVariableDeclaration.executeVariableDeclaration((VariableDeclaration) ast, memoryModel);
        } else {
            throw new RuntimeException("Unknown ASTNode type: " + ast.getClass());
        }
    }
}
