package core.SymbolicExecution.AstNode.Expression;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.ArrayZ3Wrapper;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayAccessNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayCreationNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayInitializerNode;
import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Name.NameNode;
import core.SymbolicExecution.AstNode.Expression.Name.QualifiedNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.*;
import core.SymbolicExecution.AstNode.VariableDeclarationNode;
import core.SymbolicExecution.MemoryModel;

public abstract class ExpressionNode extends AstNode {
    public static AstNode executeExpression(Expression expression, MemoryModel memoryModel) {
        if (isLiteral(expression)) {
            return LiteralNode.executeLiteral(expression);
        } else if (expression instanceof Assignment) {
            return AssignmentNode.executeAssignment((Assignment) expression, memoryModel);
        } else if (expression instanceof Name) {
            return NameNode.executeName((Name) expression, memoryModel);
        } else if (expression instanceof FieldAccess) {
            return NameNode.executeFieldAccess((FieldAccess) expression, memoryModel);
        } else if (expression instanceof VariableDeclarationExpression) {
            return VariableDeclarationNode.executeVariableDeclarationExpression(
                    (VariableDeclarationExpression) expression, memoryModel);
        } else if (isOperationExpression(expression)) {
            return OperationExpressionNode.executeOperation(expression, memoryModel);
        } else if (expression instanceof MethodInvocation) {
            return MethodInvocationNode.executeMethodInvocation((MethodInvocation) expression, memoryModel);
        } else if (expression instanceof CastExpression) {
            return CastExpressionNode.executeCastExpression((CastExpression) expression, memoryModel);
        } else if (expression instanceof ArrayAccess) {
            return ArrayAccessNode.executeArrayAccess((ArrayAccess) expression, memoryModel);
        } else if (expression instanceof ArrayCreation) {
            return ArrayCreationNode.executeArrayCreation((ArrayCreation) expression, memoryModel);
        } else if (expression instanceof ArrayInitializer) {
            return ArrayInitializerNode.executeArrayInitializer((ArrayInitializer) expression, memoryModel);
        }
         else {
            throw new RuntimeException("Unknown expression type: " + expression.getClass());
        }
    }

    public static boolean isLiteral(Expression expression) {
        return (expression instanceof NumberLiteral) ||
                (expression instanceof CharacterLiteral) ||
                (expression instanceof TypeLiteral) ||
                (expression instanceof NullLiteral) ||
                (expression instanceof StringLiteral) ||
                (expression instanceof BooleanLiteral);

    }

    public static boolean isOperationExpression(Expression expression) {
        return (expression instanceof InfixExpression) ||
                (expression instanceof PostfixExpression) ||
                (expression instanceof PrefixExpression) ||
                (expression instanceof ParenthesizedExpression);
    }

    public static Expr<?> convertAstNodeToZ3Expr(AstNode astNode, Context ctx, MemoryModel memoryModel) {
        if (astNode instanceof InfixExpressionNode) {
            return InfixExpressionNode.convertInfixExpressionToZ3Expr((InfixExpressionNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof PrefixExpressionNode) {
            return PrefixExpressionNode.convertPrefixExpressionToZ3Expr((PrefixExpressionNode) astNode, ctx,
                    memoryModel);
        } else if (astNode instanceof PostfixExpressionNode) {
            return PostfixExpressionNode.convertPostfixExpressionToZ3Expr((PostfixExpressionNode) astNode, ctx,
                    memoryModel);
        } else if (astNode instanceof ParenthesizedExpressionNode) {
            return ParenthesizedExpressionNode.convertParenthesizedExpressionToZ3Expr(
                    (ParenthesizedExpressionNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof QualifiedNameNode) {
            return QualifiedNameNode.convertQualifiedNameToZ3Expr((QualifiedNameNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof NameNode) {
            return NameNode.convertNameToZ3Expr((NameNode) astNode, ctx, memoryModel);
        }
        else if (astNode instanceof LiteralNode) {
            return LiteralNode.convertLiteralToZ3Expr((LiteralNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof CastExpressionNode) {
            return CastExpressionNode.convertCastExpressionToZ3Expr((CastExpressionNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof ArrayAccessNode) {
            return ArrayZ3Wrapper.convertArrayAccessToZ3((ArrayAccessNode) astNode, ctx, memoryModel);
        } else if (astNode instanceof ConditionalExpressionNode) {
            return ConditionalExpressionNode.convertConditionalExpressionToZ3Expr(
                    (ConditionalExpressionNode) astNode, ctx, memoryModel);
        }
        else {
            throw new RuntimeException("Cannot convert to Z3 Expr: " + astNode.getClass());
        }
    }
}
