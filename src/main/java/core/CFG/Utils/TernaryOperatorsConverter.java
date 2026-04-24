package core.CFG.Utils;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class TernaryOperatorsConverter {
    static ASTNode convertTernaryToIfThenElse(ASTNode statement) {
        AST ast = statement.getAST();
        if (statement instanceof ReturnStatement) {
            @SuppressWarnings("PatternVariableCanBeUsed")
            ReturnStatement returnStatement = (ReturnStatement) statement;
            Expression expression = returnStatement.getExpression();

            if (expression instanceof ConditionalExpression) {
                return convertConditionalExpr((returnStatement));
            }

        } else if (statement instanceof ExpressionStatement) {
            Expression expressionStatement = ((ExpressionStatement) statement).getExpression();
            if (expressionStatement instanceof Assignment) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                Assignment assignment = (Assignment) expressionStatement;
                if (assignment.getRightHandSide() instanceof ConditionalExpression) {
                    return convertConditionalExpr(assignment);
                }
            }
        } else if (statement instanceof VariableDeclarationStatement) {
            @SuppressWarnings("PatternVariableCanBeUsed")
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments = variableDeclarationStatement.fragments();

            for (VariableDeclarationFragment fragment : fragments) {
                Expression initializer = fragment.getInitializer();
                ConditionalExpression conditionalExpression = getConditionalExpression(initializer);

                if (conditionalExpression != null && canSafelySplitTernaryDeclarationFragment(variableDeclarationStatement)) {
                    // int x;
                    VariableDeclarationStatement declarationOnly = createSingleFragmentDeclaration(
                            variableDeclarationStatement,
                            fragment,
                            false);

                    // x = thenExpr;
                    ExpressionStatement thenAssign = createAssignmentStatement(
                            ast,
                            fragment.getName(),
                            Assignment.Operator.ASSIGN,
                            conditionalExpression.getThenExpression());

                    // x = elseExpr;
                    ExpressionStatement elseAssign = createAssignmentStatement(
                            ast,
                            fragment.getName(),
                            Assignment.Operator.ASSIGN,
                            conditionalExpression.getElseExpression());

                    // if (condition) { x = thenExpr; } else { x = elseExpr; }
                    IfStatement ifStatement = ast.newIfStatement();
                    ifStatement.setExpression((Expression) ASTNode.copySubtree(ast, conditionalExpression.getExpression()));

                    Block thenBlock = ast.newBlock();
                    //noinspection unchecked
                    thenBlock.statements().add(thenAssign);
                    ifStatement.setThenStatement(thenBlock);

                    Block elseBlock = ast.newBlock();
                    //noinspection unchecked
                    elseBlock.statements().add(elseAssign);
                    ifStatement.setElseStatement(elseBlock);

                    // Wrap both: int x; + if/else into a Block
                    Block resultBlock = ast.newBlock();
                    //noinspection unchecked
                    resultBlock.statements().add(declarationOnly);
                    //noinspection unchecked
                    resultBlock.statements().add(ifStatement);

                    return resultBlock;

                } else {
                    // Cannot split: keep declaration as-is with its initializer
                    return createSingleFragmentDeclaration(
                            variableDeclarationStatement,
                            fragment,
                            true);
                }
            }
        }
        return statement;
    }

    static ASTNode convertConditionalExpr(ReturnStatement returnStatement) {
        AST ast = returnStatement.getAST();
        ConditionalExpression conditionalExpression = getConditionalExpression(returnStatement.getExpression());
        assert conditionalExpression != null;

        Expression thenExpr = convertNestedTernaries(ast, conditionalExpression.getThenExpression());
        Expression elseExpr = convertNestedTernaries(ast, conditionalExpression.getElseExpression());

        ReturnStatement thenReturn = ast.newReturnStatement();
        thenReturn.setExpression(thenExpr);

        ReturnStatement elseReturn = ast.newReturnStatement();
        elseReturn.setExpression(elseExpr);

        IfStatement ifStatement = ast.newIfStatement();
        ifStatement.setExpression((Expression) ASTNode.copySubtree(ast, conditionalExpression.getExpression()));

        Block thenBlock = ast.newBlock();
        thenBlock.statements().add(thenReturn);
        ifStatement.setThenStatement(thenBlock);

        Block elseBlock = ast.newBlock();
        elseBlock.statements().add(elseReturn);
        ifStatement.setElseStatement(elseBlock);

        return ifStatement;
    }

    static ASTNode convertConditionalExpr(Assignment assignment) {
        AST ast = assignment.getAST();
        ConditionalExpression conditionalExpression = getConditionalExpression(assignment.getRightHandSide());
        assert conditionalExpression != null;

        Expression thenExpr = convertNestedTernaries(ast, conditionalExpression.getThenExpression());
        Expression elseExpr = convertNestedTernaries(ast, conditionalExpression.getElseExpression());

        Assignment thenAssignment = ast.newAssignment();
        thenAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, assignment.getLeftHandSide()));
        thenAssignment.setOperator(assignment.getOperator());
        thenAssignment.setRightHandSide(thenExpr);

        Assignment elseAssignment = ast.newAssignment();
        elseAssignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, assignment.getLeftHandSide()));
        elseAssignment.setOperator(assignment.getOperator());
        elseAssignment.setRightHandSide(elseExpr);

        IfStatement ifStatement = ast.newIfStatement();
        ifStatement.setExpression((Expression) ASTNode.copySubtree(ast, conditionalExpression.getExpression()));

        Block thenBlock = ast.newBlock();
        //noinspection unchecked
        thenBlock.statements().add(ast.newExpressionStatement(thenAssignment));
        ifStatement.setThenStatement(thenBlock);

        Block elseBlock = ast.newBlock();
        //noinspection unchecked
        elseBlock.statements().add(ast.newExpressionStatement(elseAssignment));
        ifStatement.setElseStatement(elseBlock);

        return ifStatement;
    }

    private static ConditionalExpression getConditionalExpression(Expression expression) {
        Expression unwrappedExpression = unwrapParenthesizedExpression(expression);
        if (unwrappedExpression instanceof ConditionalExpression) {
            return (ConditionalExpression) unwrappedExpression;
        }
        return null;
    }

    private static Expression unwrapParenthesizedExpression(Expression expression) {
        Expression unwrappedExpression = expression;
        while (unwrappedExpression instanceof ParenthesizedExpression) {
            unwrappedExpression = ((ParenthesizedExpression) unwrappedExpression).getExpression();
        }
        return unwrappedExpression;
    }

    private static Expression convertNestedTernaries(AST ast, Expression expr) {
        Expression unwrapped = unwrapParenthesizedExpression(expr);
        if (unwrapped instanceof ConditionalExpression) {
            ConditionalExpression nested = (ConditionalExpression) unwrapped;
            Expression convertedThen = convertNestedTernaries(ast, nested.getThenExpression());
            Expression convertedElse = convertNestedTernaries(ast, nested.getElseExpression());

            ConditionalExpression newTernary = ast.newConditionalExpression();
            newTernary.setExpression((Expression) ASTNode.copySubtree(ast, nested.getExpression()));
            newTernary.setThenExpression(convertedThen);
            newTernary.setElseExpression(convertedElse);
            return newTernary;
        }
        return (Expression) ASTNode.copySubtree(ast, expr);
    }

    private static boolean canSafelySplitTernaryDeclarationFragment(VariableDeclarationStatement declarationStatement) {
        if (isFinalDeclaration(declarationStatement)) {
            return false;
        }
        return !isVarType(declarationStatement.getType());
    }

    private static boolean isFinalDeclaration(VariableDeclarationStatement declarationStatement) {
        @SuppressWarnings("unchecked")
        List<IExtendedModifier> modifiers = declarationStatement.modifiers();
        for (IExtendedModifier modifier : modifiers) {
            if (modifier instanceof Modifier && ((Modifier) modifier).isFinal()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVarType(Type type) {
        if (!(type instanceof SimpleType)) {
            return false;
        }
        return "var".equals(((SimpleType) type).getName().getFullyQualifiedName());
    }

    private static VariableDeclarationStatement createSingleFragmentDeclaration(
            VariableDeclarationStatement template,
            VariableDeclarationFragment fragment,
            boolean keepInitializer) {
        AST ast = template.getAST();

        VariableDeclarationFragment fragmentCopy =
                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
        if (!keepInitializer) {
            fragmentCopy.setInitializer(null);
        }

        VariableDeclarationStatement singleDeclaration = ast.newVariableDeclarationStatement(fragmentCopy);
        singleDeclaration.setType((Type) ASTNode.copySubtree(ast, template.getType()));

        @SuppressWarnings("unchecked")
        List<IExtendedModifier> templateModifiers = template.modifiers();
        @SuppressWarnings("unchecked")
        List<IExtendedModifier> singleModifiers = singleDeclaration.modifiers();
        for (IExtendedModifier modifier : templateModifiers) {
            singleModifiers.add((IExtendedModifier) ASTNode.copySubtree(ast, (ASTNode) modifier));
        }

        return singleDeclaration;
    }

    private static ExpressionStatement createAssignmentStatement(
            AST ast,
            Expression leftHandSide,
            Assignment.Operator operator,
            Expression rightHandSide) {
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
        assignment.setOperator(operator);
        assignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, rightHandSide));
        return ast.newExpressionStatement(assignment);
    }

}
