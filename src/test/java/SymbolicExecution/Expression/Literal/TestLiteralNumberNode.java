package SymbolicExecution.Expression.Literal;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.junit.Test;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;


public class TestLiteralNumberNode {

    private NumberLiteral newNumberLiteral(String token) {
        AST ast = AST.newAST(AST.JLS8);
        NumberLiteral numberLiteral = ast.newNumberLiteral(token);
        numberLiteral.setToken(token);
        return numberLiteral;
    }

    @Test
    public void from_createsIntegerLiteralNumberNode() {
        NumberLiteral numberLiteral = newNumberLiteral("42");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("42", node.getToken());
        assertTrue(node.isInteger());
        assertFalse(node.isDouble());
        assertEquals(42L, node.getIntegerValue());
        assertEquals(42.0, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_createsIntegerLiteralNumberNodeWithUnderscores() {
        NumberLiteral numberLiteral = newNumberLiteral("1_000_000");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("1_000_000", node.getToken());
        assertTrue(node.isInteger());
        assertEquals(1_000_000L, node.getIntegerValue());
        assertEquals(1_000_000.0, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_createsDoubleLiteralNumberNode() {
        NumberLiteral numberLiteral = newNumberLiteral("3.14");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("3.14", node.getToken());
        assertFalse(node.isInteger());
        assertTrue(node.isDouble());
        assertEquals(3.14, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_parsesDecimalIntLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("123456789");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("123456789", node.getToken());
        assertTrue(node.isInteger());
        assertEquals(123456789L, node.getIntegerValue());
    }

    @Test
    public void from_parsesHexIntLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("0xFF");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("0xFF", node.toString());
        assertTrue(node.isInteger());
        assertEquals(255L, node.getIntegerValue());
    }

    @Test
    public void from_parsesOctalIntLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("077");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("077", node.toString());
        assertTrue(node.isInteger());
        assertEquals(63L, node.getIntegerValue());
    }

    @Test
    public void from_parsesLongLiteralWithSuffix() {
        NumberLiteral numberLiteral = newNumberLiteral("2147483648L");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("2147483648L", node.toString());
        assertTrue(node.isInteger());
        assertEquals(2147483648L, node.getIntegerValue());
    }

    @Test(expected = NumberFormatException.class)
    public void from_throwsOnBinaryIntLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("0b1010");

        LiteralNumberNode.from(numberLiteral);
    }

    @Test
    public void from_parsesStandardDoubleLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("0.125");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("0.125", node.getToken());
        assertTrue(node.isDouble());
        assertFalse(node.isInteger());
        assertEquals(0.125, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_parsesFloatLiteralWithSuffixAsDouble() {
        NumberLiteral numberLiteral = newNumberLiteral("1.23f");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertTrue(node.isDouble());
        assertFalse(node.isInteger());
        assertEquals(1.23, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_parsesScientificNotationDoubleLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("1.23e4");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("1.23e4", node.getToken());
        assertTrue(node.isDouble());
        assertFalse(node.isInteger());
        assertEquals(1.23e4, node.getDoubleValue(), 0.0);
    }

    @Test
    public void from_parsesHexFloatingPointDoubleLiteral() {
        NumberLiteral numberLiteral = newNumberLiteral("0x1.0p3");

        LiteralNumberNode node = LiteralNumberNode.from(numberLiteral);

        assertEquals("0x1.0p3", node.getToken());
        assertTrue(node.isDouble());
        assertFalse(node.isInteger());
        assertEquals(8.0, node.getDoubleValue(), 0.0);
    }

    @Test
    public void of_createsIntegerLiteralNumberNode() {
        LiteralNumberNode node = LiteralNumberNode.of(100);

        assertEquals("100", node.getToken());
        assertTrue(node.isInteger());
        assertFalse(node.isDouble());
        assertEquals(100L, node.getIntegerValue());
        assertEquals(100.0, node.getDoubleValue(), 0.0);
    }

    @Test
    public void of_createsDoubleLiteralNumberNode() {
        LiteralNumberNode node = LiteralNumberNode.of(2.718);

        assertEquals("2.718", node.getToken());
        assertFalse(node.isInteger());
        assertTrue(node.isDouble());
        assertEquals(2L, node.getIntegerValue());
        assertEquals(2.718, node.getDoubleValue(), 0.0);
    }
}

