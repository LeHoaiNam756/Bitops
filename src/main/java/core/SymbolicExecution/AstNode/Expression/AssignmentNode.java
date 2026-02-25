package core.SymbolicExecution.AstNode.Expression;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNode;
import core.SymbolicExecution.AstNode.Expression.Name.NameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.MemoryModel;


public class AssignmentNode extends ExpressionNode {
    private ExpressionNode left;
    private ExpressionNode right;
    private Assignment.Operator operator;

    public static AstNode executeAssignment(Assignment assignment, MemoryModel memoryModel) {
        AssignmentNode assignmentNode = new AssignmentNode();
        assignmentNode.left = (ExpressionNode) ExpressionNode.executeExpression(assignment.getLeftHandSide(),
                                                                                memoryModel);
        assignmentNode.right = (ExpressionNode) ExpressionNode.executeExpression(assignment.getRightHandSide(),
                                                                                memoryModel);
        assignmentNode.operator = assignment.getOperator();

        AstNode assignValue = analyzeAssignValue(assignmentNode.left, assignmentNode.right, assignmentNode.operator);
        Expression leftHandSide = assignment.getLeftHandSide();
        if (leftHandSide instanceof Name) {
           String varName = NameNode.getStringName((Name) leftHandSide);
           memoryModel.assignVariable(varName, assignValue);
        } else {
            throw new Error("unexpected left hand side");
        }


        return assignmentNode;
    }

    private static AstNode analyzeAssignValue(AstNode left, AstNode right,
                                              Assignment.Operator operator) {
        if (operator == Assignment.Operator.ASSIGN) {
            return right;
        }

        InfixExpressionNode extractedAssignmentNode = new InfixExpressionNode();
        extractedAssignmentNode.setLeft(left);
        extractedAssignmentNode.setRight(right);

        if (operator == Assignment.Operator.PLUS_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.PLUS);
        } else if (operator == Assignment.Operator.MINUS_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.MINUS);
        } else if (operator == Assignment.Operator.DIVIDE_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.DIVIDE);
        } else if (operator == Assignment.Operator.TIMES_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.TIMES);
        } else if (operator == Assignment.Operator.REMAINDER_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.REMAINDER);
        } else if (operator == Assignment.Operator.BIT_OR_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.OR);
        } else if (operator == Assignment.Operator.BIT_AND_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.AND);
        } else if (operator == Assignment.Operator.BIT_XOR_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.XOR);
        } else if (operator == Assignment.Operator.LEFT_SHIFT_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.LEFT_SHIFT);
        } else if (operator == Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED);
        } else if (operator == Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN) {
            extractedAssignmentNode.setOperator(InfixExpression.Operator.RIGHT_SHIFT_SIGNED);
        } else {
            throw new RuntimeException("Invalid assignment operator: " + operator);
        }

        if (left instanceof LiteralNode && right instanceof LiteralNode) {
            return LiteralNode.analyzeTwoInfixLiteral((LiteralNode) left, 
                                                       extractedAssignmentNode.getOperator(), 
                                                       (LiteralNode) right);
        }

        return extractedAssignmentNode;
    }
}
