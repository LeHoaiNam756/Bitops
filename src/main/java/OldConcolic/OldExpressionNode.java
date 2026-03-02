package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.NameNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Operation.ParenthesizedExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PostfixExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PrefixExpressionNode;
import core.SymbolicExecution.AstNode.Expression.AssignmentNode;
import core.SymbolicExecution.AstNode.Expression.Operation.OperationExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.*;

public class OldExpressionNode {
    public static AstNode executeExpression(Expression expression, MemoryModel memoryModel) {
        if (ExpressionNode.isLiteral(expression)) {
            return LiteralNode.executeLiteral(expression);
        } else if (expression instanceof Assignment) {
            return AssignmentNode.executeAssignment((Assignment) expression, memoryModel);
        } else if (expression instanceof Name) {
            return OldNameNode.executeName((Name) expression, memoryModel);
        } else if (expression instanceof VariableDeclarationExpression) {
            return OldVariableDeclaration.executeVariableDeclarationExpression(
                    (VariableDeclarationExpression) expression, memoryModel);
        } else if (ExpressionNode.isOperationExpression(expression)) {
            return OldOperationExpressionNode.executeOperation(expression, memoryModel);
        } else if (expression instanceof MethodInvocation) {
            return null;
        }
        else {
            throw new RuntimeException("Unknown expression type: " + expression.getClass());
        }
    }
    public static Expr<?> convertAstNodeToZ3ExprOld(AstNode astNode, Context ctx, MemoryModel memoryModel) {
        if (astNode instanceof InfixExpressionNode) {
            return OldInfixExpressionNode.convertInfixExpressionToZ3ExprOld((InfixExpressionNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof PrefixExpressionNode) {
            return OldPrefixExpressionNode.convertPrefixExpressionToZ3ExprOld((PrefixExpressionNode) astNode, ctx,
                    memoryModel);
        } else if (astNode instanceof PostfixExpressionNode) {
            return OldPostfixExpressionNode.convertPostfixExpressionToZ3ExprOld((PostfixExpressionNode) astNode, ctx,
                    memoryModel);
        } else if (astNode instanceof ParenthesizedExpressionNode) {
            return OldParenthesizedExpressionNode.convertParenthesizedExpressionToZ3ExprOld(
                    (ParenthesizedExpressionNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof NameNode) {
            return OldNameNode.convertNameToZ3ExprOld((NameNode) astNode, ctx, memoryModel);
        }
        else if (astNode instanceof LiteralNode) {
            return OldLiteralNode.convertLiteralToZ3ExprOld((LiteralNode) astNode, ctx, memoryModel);
        } else {
            throw new RuntimeException("Cannot convert to Z3 Expr: " + astNode.getClass());
        }
    }
}
