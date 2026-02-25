package SymbolicExecution.Expression.Literal;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import core.SymbolicExecution.AstNode.Expression.Literal.*;

import static org.junit.Assert.*;

public class TestLiteralNode {

    private Expression createExpr(Object expr) {
        AST ast = AST.newAST(AST.JLS8);

        if (expr instanceof Number) {
            String token;
            Number number = (Number) expr;

            if (expr instanceof Integer || expr instanceof Short || expr instanceof Byte) {
                token = String.valueOf(number.longValue());
            } else if (expr instanceof Long) {
                token = number.longValue() + "L";
            } else if (expr instanceof Float) {
                token = number.floatValue() + "f";
            } else if (expr instanceof Double) {
                token = String.valueOf(number.doubleValue());
            } else {
                token = expr.toString();
            }

            return ast.newNumberLiteral(token);
        }

        if (expr instanceof Boolean) {
            return ast.newBooleanLiteral((Boolean) expr);
        }

        if (expr instanceof Character) {
            CharacterLiteral characterLiteral = ast.newCharacterLiteral();
            characterLiteral.setCharValue((Character) expr);
            return characterLiteral;
        }

        if (expr instanceof String) {
            StringLiteral stringLiteral = ast.newStringLiteral();
            stringLiteral.setLiteralValue((String) expr);
            return stringLiteral;
        }

        if (expr == null) {
            return ast.newNullLiteral();
        }

        throw new IllegalArgumentException("Unsupported expression value: " + expr);
    }

    @Test
    public void executeLiteral_createsLiteralNumberNodeFromInteger() {
        Expression expr = createExpr(42);

        LiteralNode node = LiteralNode.executeLiteral(expr);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode numberNode = (LiteralNumberNode) node;
        assertTrue(numberNode.isInteger());
        assertEquals(42L, numberNode.getIntegerValue());
    }

    @Test
    public void executeLiteral_createsLiteralBooleanNode() {
        Expression expr = createExpr(true);

        LiteralNode node = LiteralNode.executeLiteral(expr);

        assertTrue(node instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanNode = (LiteralBooleanNode) node;
        assertTrue(booleanNode.isValue());
        assertEquals("true", booleanNode.toString());
    }

    @Test
    public void executeLiteral_createsLiteralCharacterNode() {
        Expression expr = createExpr('a');

        LiteralNode node = LiteralNode.executeLiteral(expr);

        assertTrue(node instanceof LiteralCharacterNode);
        LiteralCharacterNode characterNode = (LiteralCharacterNode) node;
        assertEquals('a', characterNode.getValue());
        assertEquals("a", characterNode.toString());
    }

    @Test
    public void executeLiteral_createsLiteralStringNode() {
        Expression expr = createExpr("hello");

        LiteralNode node = LiteralNode.executeLiteral(expr);

        assertTrue(node instanceof LiteralStringNode);
        LiteralStringNode stringNode = (LiteralStringNode) node;
        assertEquals("hello", stringNode.getValue());
        assertEquals("hello", stringNode.toString());
    }

    @Test
    public void executeLiteral_createsLiteralNullNode() {
        Expression expr = createExpr(null);

        LiteralNode node = LiteralNode.executeLiteral(expr);

        assertTrue(node instanceof LiteralNullNode);
        assertEquals("null", node.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeLiteral_throwsOnTypeLiteral() {
        AST ast = AST.newAST(AST.JLS8);
        TypeLiteral typeLiteral = ast.newTypeLiteral();

        LiteralNode.executeLiteral(typeLiteral);
    }

    @Test
    public void calculateOnePostfixNumberLiteral_incrementsIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(5);
        PostfixExpression.Operator operator = PostfixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(6L, resultNode.getIntegerValue());
    }

    @Test
    public void calculateOnePostfixCharacterLiteral_decrementsCharacterLiteral() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of('d');
        PostfixExpression.Operator operator = PostfixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralCharacterNode);
        LiteralCharacterNode resultNode = (LiteralCharacterNode) node;
        assertEquals('c', resultNode.getValue());
    }

    @Test
    public void calculateOnePostfixCharacterLiteral_incrementsCharacterLiteral() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of('x');
        PostfixExpression.Operator operator = PostfixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralCharacterNode);
        LiteralCharacterNode resultNode = (LiteralCharacterNode) node;
        assertEquals('y', resultNode.getValue());
    }

    @Test
    public void calculateOnePostfixNumberLiteral_decrementsIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(10);
        PostfixExpression.Operator operator = PostfixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(9L, resultNode.getIntegerValue());
    }

    @Test
    public void calculateOnePostfixNumberLiteral_incrementsLongLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(100L);
        PostfixExpression.Operator operator = PostfixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(101L, resultNode.getIntegerValue());
    }

    @Test
    public void calculateOnePostfixNumberLiteral_decrementsShortLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of((short) 20);
        PostfixExpression.Operator operator = PostfixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(19L, resultNode.getIntegerValue());
    }

    @Test
    public void calculateOnePostfixNumberLiteral_decrementsDoubleLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(5.5);
        PostfixExpression.Operator operator = PostfixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isDouble());
        assertEquals(4.5, resultNode.getDoubleValue(), 0.0001);
    }

    @Test
    public void calculateOnePostfixNumberLiteral_incrementsDoubleLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(7.8);
        PostfixExpression.Operator operator = PostfixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isDouble());
        assertEquals(8.8, resultNode.getDoubleValue(), 0.0001);
    }

    @Test
    public void calculateOnePostfixNumberLiteral_incrementsFloatLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(3.2f);
        PostfixExpression.Operator operator = PostfixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isDouble());
        assertEquals(4.2, resultNode.getDoubleValue(), 0.0001);
    }

    @Test
    public void calculateOnePostfixNumberLiteral_decrementsFloatLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(6.7f);
        PostfixExpression.Operator operator = PostfixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePostfixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isDouble());
        assertEquals(5.7, resultNode.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeOnePrefixLiteral_incrementsIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(10);
        PrefixExpression.Operator operator = PrefixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(11L, resultNode.getIntegerValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_decrementsIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(10);
        PrefixExpression.Operator operator = PrefixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(9L, resultNode.getIntegerValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_appliesUnaryMinusToIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(5);
        PrefixExpression.Operator operator = PrefixExpression.Operator.MINUS;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(-5L, resultNode.getIntegerValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_appliesUnaryMinusToDoubleLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(3.5);
        PrefixExpression.Operator operator = PrefixExpression.Operator.MINUS;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isDouble());
        assertEquals(-3.5, resultNode.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeOnePrefixLiteral_appliesBitwiseComplementToIntegerLiteral() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(5);
        PrefixExpression.Operator operator = PrefixExpression.Operator.COMPLEMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(~5L, resultNode.getIntegerValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_returnsSameLiteralOnPlusOperator() {
        LiteralNumberNode literalNumberNode = LiteralNumberNode.of(7);
        PrefixExpression.Operator operator = PrefixExpression.Operator.PLUS;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalNumberNode, operator);

        assertSame(literalNumberNode, node);
    }

    @Test
    public void analyzeOnePrefixLiteral_incrementsCharacterLiteralWithinRange() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of('a');
        PrefixExpression.Operator operator = PrefixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralCharacterNode);
        LiteralCharacterNode resultNode = (LiteralCharacterNode) node;
        assertEquals('b', resultNode.getValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_decrementsCharacterLiteralWithinRange() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of('b');
        PrefixExpression.Operator operator = PrefixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralCharacterNode);
        LiteralCharacterNode resultNode = (LiteralCharacterNode) node;
        assertEquals('a', resultNode.getValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_incrementMaxCharReturnsNumberLiteral() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of(Character.MAX_VALUE);
        PrefixExpression.Operator operator = PrefixExpression.Operator.INCREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals((long) Character.MAX_VALUE + 1, resultNode.getIntegerValue());
    }

    @Test
    public void analyzeOnePrefixLiteral_decrementMinCharReturnsNumberLiteral() {
        LiteralCharacterNode literalCharacterNode = LiteralCharacterNode.of(Character.MIN_VALUE);
        PrefixExpression.Operator operator = PrefixExpression.Operator.DECREMENT;

        LiteralNode node = LiteralNode.analyzeOnePrefixLiteral(literalCharacterNode, operator);

        assertTrue(node instanceof LiteralNumberNode);
        LiteralNumberNode resultNode = (LiteralNumberNode) node;
        assertTrue(resultNode.isInteger());
        assertEquals(-1L, resultNode.getIntegerValue());
    }


    @Test
    public void analyzeTwoInfixLiteral_addsIntegerLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(1L);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(3L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_multipliesIntegerLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(2L);
        LiteralNumberNode right = LiteralNumberNode.of(3L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(6L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_dividesIntegerLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(5L);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(2L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_remainderOfIntegerLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(5L);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(1L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_addsDoubleLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(1.5);
        LiteralNumberNode right = LiteralNumberNode.of(2.0);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isDouble());
        assertEquals(3.5, numberResult.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeTwoInfixLiteral_comparesIntegerLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(1L);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.LESS, right);

        assertTrue(result instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanResult = (LiteralBooleanNode) result;
        assertTrue(booleanResult.isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_comparesDoubleLiterals() {
        LiteralNumberNode left = LiteralNumberNode.of(2.0);
        LiteralNumberNode right = LiteralNumberNode.of(1.0);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.GREATER, right);

        assertTrue(result instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanResult = (LiteralBooleanNode) result;
        assertTrue(booleanResult.isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_bitwiseAndOnIntegers() {
        LiteralNumberNode left = LiteralNumberNode.of(1L);
        LiteralNumberNode right = LiteralNumberNode.of(3L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.AND, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(1L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_leftShiftOnInteger() {
        LiteralNumberNode left = LiteralNumberNode.of(1L);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.LEFT_SHIFT, right);

        assertTrue(result instanceof LiteralNumberNode);
        LiteralNumberNode numberResult = (LiteralNumberNode) result;
        assertTrue(numberResult.isInteger());
        assertEquals(4L, numberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_booleanConditionalAnd() {
        LiteralBooleanNode left = LiteralBooleanNode.of(true);
        LiteralBooleanNode right = LiteralBooleanNode.of(false);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.CONDITIONAL_AND, right);

        assertTrue(result instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanResult = (LiteralBooleanNode) result;
        assertFalse(booleanResult.isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_booleanBitwiseOr() {
        LiteralBooleanNode left = LiteralBooleanNode.of(true);
        LiteralBooleanNode right = LiteralBooleanNode.of(false);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.OR, right);

        assertTrue(result instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanResult = (LiteralBooleanNode) result;
        assertTrue(booleanResult.isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_booleanEquals() {
        LiteralBooleanNode left = LiteralBooleanNode.of(true);
        LiteralBooleanNode right = LiteralBooleanNode.of(true);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.EQUALS, right);

        assertTrue(result instanceof LiteralBooleanNode);
        LiteralBooleanNode booleanResult = (LiteralBooleanNode) result;
        assertTrue(booleanResult.isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_concatenatesStringLiterals() {
        LiteralStringNode left = LiteralStringNode.of("a");
        LiteralStringNode right = LiteralStringNode.of("b");

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);

        assertTrue(result instanceof LiteralStringNode);
        LiteralStringNode stringResult = (LiteralStringNode) result;
        assertEquals("ab", stringResult.getValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_concatenatesStringAndNumber() {
        LiteralStringNode left = LiteralStringNode.of("a");
        LiteralNumberNode right = LiteralNumberNode.of(1L);

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);

        assertTrue(result instanceof LiteralStringNode);
        LiteralStringNode stringResult = (LiteralStringNode) result;
        assertEquals("a1", stringResult.getValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithTwoInteger() {
        LiteralNumberNode left = LiteralNumberNode.of(8L);
        LiteralNumberNode right = LiteralNumberNode.of(4L);

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isInteger());
        assertEquals(12L, additionNumberResult.getIntegerValue());
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isInteger());
        assertEquals(4L, subtractionNumberResult.getIntegerValue());
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isInteger());
        assertEquals(32L, multiplicationNumberResult.getIntegerValue());
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isInteger());
        assertEquals(2L, divisionNumberResult.getIntegerValue());
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isInteger());
        assertEquals(0L, remainderNumberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithTwoDoubles() {
        LiteralNumberNode left = LiteralNumberNode.of(7.5);
        LiteralNumberNode right = LiteralNumberNode.of(2.5);

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isDouble());
        assertEquals(10.0, additionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isDouble());
        assertEquals(5.0, subtractionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isDouble());
        assertEquals(18.75, multiplicationNumberResult.getDoubleValue(), 0.0001);
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isDouble());
        assertEquals(3.0, divisionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isDouble());
        assertEquals(0.0, remainderNumberResult.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithMixIntAndDouble() {
        LiteralNumberNode left = LiteralNumberNode.of(7.5);
        LiteralNumberNode right = LiteralNumberNode.of(2L);

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isDouble());
        assertEquals(9.5, additionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isDouble());
        assertEquals(5.5, subtractionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isDouble());
        assertEquals(15.0, multiplicationNumberResult.getDoubleValue(), 0.0001);
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isDouble());
        assertEquals(3.75, divisionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isDouble());
        assertEquals(1.5, remainderNumberResult.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithMixLongAndFloat() {
        LiteralNumberNode left = LiteralNumberNode.of(10L);
        LiteralNumberNode right = LiteralNumberNode.of(3.5f);

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isDouble());
        assertEquals(13.5, additionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isDouble());
        assertEquals(6.5, subtractionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isDouble());
        assertEquals(35.0, multiplicationNumberResult.getDoubleValue(), 0.0001);
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isDouble());
        assertEquals(2.85714, divisionNumberResult.getDoubleValue(), 0.0001);
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isDouble());
        assertEquals(3.0, remainderNumberResult.getDoubleValue(), 0.0001);
    }

    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithCharAndInt() {
        LiteralCharacterNode left = LiteralCharacterNode.of('A');
        LiteralNumberNode right = LiteralNumberNode.of(5L);

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isInteger());
        assertEquals((long) 'A' + 5, additionNumberResult.getIntegerValue());
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isInteger());
        assertEquals((long) 'A' - 5, subtractionNumberResult.getIntegerValue());
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isInteger());
        assertEquals((long) 'A' * 5, multiplicationNumberResult.getIntegerValue());
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isInteger());
        assertEquals((long) 'A' / 5, divisionNumberResult.getIntegerValue());
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isInteger());
        assertEquals((long) 'A' % 5, remainderNumberResult.getIntegerValue());
    }


    @Test
    public void analyzeTwoInfixLiteral_ArithmeticWithChar() {
        LiteralCharacterNode left = LiteralCharacterNode.of('D');
        LiteralCharacterNode right = LiteralCharacterNode.of('A');

        LiteralNode addition = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.PLUS, right);
        LiteralNode subtraction = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.MINUS, right);
        LiteralNode multiplication = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.TIMES, right);
        LiteralNode division = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.DIVIDE, right);
        LiteralNode remainder = LiteralNode.analyzeTwoInfixLiteral(left, InfixExpression.Operator.REMAINDER, right);

        assertTrue(addition instanceof LiteralNumberNode);
        LiteralNumberNode additionNumberResult = (LiteralNumberNode) addition;
        assertTrue(additionNumberResult.isInteger());
        assertEquals((long) 'D' + (long) 'A', additionNumberResult.getIntegerValue());
        assertTrue(subtraction instanceof LiteralNumberNode);
        LiteralNumberNode subtractionNumberResult = (LiteralNumberNode) subtraction;
        assertTrue(subtractionNumberResult.isInteger());
        assertEquals((long) 'D' - (long) 'A', subtractionNumberResult.getIntegerValue());
        assertTrue(multiplication instanceof LiteralNumberNode);
        LiteralNumberNode multiplicationNumberResult = (LiteralNumberNode) multiplication;
        assertTrue(multiplicationNumberResult.isInteger());
        assertEquals((long) 'D' * (long) 'A', multiplicationNumberResult.getIntegerValue());
        assertTrue(division instanceof LiteralNumberNode);
        LiteralNumberNode divisionNumberResult = (LiteralNumberNode) division;
        assertTrue(divisionNumberResult.isInteger());
        assertEquals((long) 'D' / (long) 'A', divisionNumberResult.getIntegerValue());
        assertTrue(remainder instanceof LiteralNumberNode);
        LiteralNumberNode remainderNumberResult = (LiteralNumberNode) remainder;
        assertTrue(remainderNumberResult.isInteger());
        assertEquals((long) 'D' % (long) 'A', remainderNumberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_RelationalWithNumeric() {
        LiteralNumberNode intLiteral = LiteralNumberNode.of(5L);
        LiteralNumberNode doubleLiteral = LiteralNumberNode.of(5.0);

        LiteralNode equalsResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.EQUALS, doubleLiteral);
        LiteralNode notEqualsResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.NOT_EQUALS, doubleLiteral);
        LiteralNode lessResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.LESS, doubleLiteral);
        LiteralNode lessEqualsResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.LESS_EQUALS, doubleLiteral);
        LiteralNode greaterResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.GREATER, doubleLiteral);
        LiteralNode greaterEqualsResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.GREATER_EQUALS, doubleLiteral);

        assertTrue(equalsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) equalsResult).isValue());

        assertTrue(notEqualsResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) notEqualsResult).isValue());

        assertTrue(lessResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) lessResult).isValue());

        assertTrue(lessEqualsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) lessEqualsResult).isValue());

        assertTrue(greaterResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) greaterResult).isValue());

        assertTrue(greaterEqualsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) greaterEqualsResult).isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_RelationalWithCharAndNumeric() {
        LiteralCharacterNode charLiteral = LiteralCharacterNode.of('A');
        LiteralNumberNode intLiteral = LiteralNumberNode.of(65L);

        LiteralNode equalsResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.EQUALS, intLiteral);
        LiteralNode notEqualsResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.NOT_EQUALS, intLiteral);
        LiteralNode lessResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.LESS, intLiteral);
        LiteralNode lessEqualsResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.LESS_EQUALS, intLiteral);
        LiteralNode greaterResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.GREATER, intLiteral);
        LiteralNode greaterEqualsResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.GREATER_EQUALS, intLiteral);

        assertTrue(equalsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) equalsResult).isValue());

        assertTrue(notEqualsResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) notEqualsResult).isValue());

        assertTrue(lessResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) lessResult).isValue());

        assertTrue(lessEqualsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) lessEqualsResult).isValue());

        assertTrue(greaterResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) greaterResult).isValue());

        assertTrue(greaterEqualsResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) greaterEqualsResult).isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_LogicWithBoolean() {
        LiteralBooleanNode left = LiteralBooleanNode.of(true);
        LiteralBooleanNode right = LiteralBooleanNode.of(false);

        LiteralNode andResult = LiteralNode.analyzeTwoInfixLiteral(left,
                InfixExpression.Operator.CONDITIONAL_AND, right);
        LiteralNode orResult = LiteralNode.analyzeTwoInfixLiteral(left,
                InfixExpression.Operator.CONDITIONAL_OR, right);


        assertTrue(andResult instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) andResult).isValue());

        assertTrue(orResult instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) orResult).isValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_BitwiseWithNumericLikeLiteral() {
        LiteralNumberNode intLiteral = LiteralNumberNode.of(1L);
        LiteralNumberNode longLiteral = LiteralNumberNode.of(3L);

        LiteralNode andResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.AND, longLiteral);
        LiteralNode orResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.OR, longLiteral);
        LiteralNode xorResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.XOR, longLiteral);

        assertTrue(andResult instanceof LiteralNumberNode);
        LiteralNumberNode andNumberResult = (LiteralNumberNode) andResult;
        assertTrue(andNumberResult.isInteger());
        assertEquals(1 & 3, andNumberResult.getIntegerValue());
        assertTrue(orResult instanceof LiteralNumberNode);
        LiteralNumberNode orNumberResult = (LiteralNumberNode) orResult;
        assertTrue(orNumberResult.isInteger());
        assertEquals(1 | 3, orNumberResult.getIntegerValue());
        assertTrue(xorResult instanceof LiteralNumberNode);
        LiteralNumberNode xorNumberResult = (LiteralNumberNode) xorResult;
        assertTrue(xorNumberResult.isInteger());
        assertEquals(1 ^ 3, xorNumberResult.getIntegerValue());
    }

    @Test
    public void analyzeTwoInfixLiteral_BitwiseWithNumericLikeLiteral_Char() {
        LiteralCharacterNode charLiteral = LiteralCharacterNode.of('C');
        LiteralNumberNode intLiteral = LiteralNumberNode.of(5L);

        LiteralNode andResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.AND, intLiteral);
        LiteralNode orResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.OR, intLiteral);
        LiteralNode xorResult = LiteralNode.analyzeTwoInfixLiteral(charLiteral,
                InfixExpression.Operator.XOR, intLiteral);

        assertTrue(andResult instanceof LiteralNumberNode);
        LiteralNumberNode andNumberResult = (LiteralNumberNode) andResult;
        assertTrue(andNumberResult.isInteger());
        assertEquals((long) 'C' & 5, andNumberResult.getIntegerValue());
        assertTrue(orResult instanceof LiteralNumberNode);
        LiteralNumberNode orNumberResult = (LiteralNumberNode) orResult;
        assertTrue(orNumberResult.isInteger());
        assertEquals((long) 'C' | 5, orNumberResult.getIntegerValue());
        assertTrue(xorResult instanceof LiteralNumberNode);
        LiteralNumberNode xorNumberResult = (LiteralNumberNode) xorResult;
        assertTrue(xorNumberResult.isInteger());
        assertEquals((long) 'C' ^ 5, xorNumberResult.getIntegerValue());
    }

    @Test(expected = RuntimeException.class)
    public void analyzeTwoInfixLiteral_BitwiseWithNumericLikeLiteral_Double() {
        LiteralNumberNode doubleLiteral1 = LiteralNumberNode.of(1.5);
        LiteralNumberNode doubleLiteral2 = LiteralNumberNode.of(2.5);

        LiteralNode.analyzeTwoInfixLiteral(doubleLiteral1,
                InfixExpression.Operator.AND, doubleLiteral2);
    }

    @Test
    public void analyzeTwoInfixLiteral_BitshiftWithNumericLikeLiteral() {
        LiteralNumberNode intLiteral = LiteralNumberNode.of(1L);
        LiteralNumberNode shiftLiteral = LiteralNumberNode.of(2L);

        LiteralNode leftShiftResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.LEFT_SHIFT, shiftLiteral);
        LiteralNode rightShiftResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.RIGHT_SHIFT_SIGNED, shiftLiteral);
        LiteralNode unsignedRightShiftResult = LiteralNode.analyzeTwoInfixLiteral(intLiteral,
                InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, shiftLiteral);

        assertTrue(leftShiftResult instanceof LiteralNumberNode);
        LiteralNumberNode leftShiftNumberResult = (LiteralNumberNode) leftShiftResult;
        assertTrue(leftShiftNumberResult.isInteger());
        assertEquals(1L << 2, leftShiftNumberResult.getIntegerValue());
        assertTrue(rightShiftResult instanceof LiteralNumberNode);
        LiteralNumberNode rightShiftNumberResult = (LiteralNumberNode) rightShiftResult;
        assertTrue(rightShiftNumberResult.isInteger());
        assertEquals(1L >> 2, rightShiftNumberResult.getIntegerValue());
        assertTrue(unsignedRightShiftResult instanceof LiteralNumberNode);
        LiteralNumberNode unsignedRightShiftNumberResult = (LiteralNumberNode) unsignedRightShiftResult;
        assertTrue(unsignedRightShiftNumberResult.isInteger());
        assertEquals(1L >>> 2, unsignedRightShiftNumberResult.getIntegerValue());
    }

    @Test(expected = RuntimeException.class)
    public void analyzeTwoInfixLiteral_BitshiftWithNumericLikeLiteral_Double() {
        LiteralNumberNode doubleLiteral1 = LiteralNumberNode.of(1.5);
        LiteralNumberNode doubleLiteral2 = LiteralNumberNode.of(2.5);

        LiteralNode.analyzeTwoInfixLiteral(doubleLiteral1,
                InfixExpression.Operator.LEFT_SHIFT, doubleLiteral2);
    }

    @Test
    public void analyzeTwoInfixLiteral_StringConcatenationWithChar() {
        LiteralStringNode stringLiteral = LiteralStringNode.of("Hello ");
        LiteralCharacterNode charLiteral = LiteralCharacterNode.of('A');

        LiteralNode result = LiteralNode.analyzeTwoInfixLiteral(stringLiteral,
                InfixExpression.Operator.PLUS, charLiteral);

        assertTrue(result instanceof LiteralStringNode);
        LiteralStringNode stringResult = (LiteralStringNode) result;
        assertEquals("Hello A", stringResult.getValue());
    }

}
