package SymbolicExecution.Expression.Literal;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.junit.Test;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralStringNode;
import static org.junit.Assert.*;


public class TestLiteralStringNode {

    private StringLiteral newStringLiteral(String value) {
        AST ast = AST.newAST(AST.JLS8);
        StringLiteral stringLiteral = ast.newStringLiteral();
        stringLiteral.setLiteralValue(value);
        return stringLiteral;
    }

    @Test
    public void from_createsLiteralStringNodeWithSimpleValue() {
        StringLiteral stringLiteral = newStringLiteral("hello");

        LiteralStringNode node = LiteralStringNode.from(stringLiteral);

        assertEquals("hello", node.getValue());
        assertEquals("hello", node.toString());
    }

    @Test
    public void from_createsLiteralStringNodeWithEmptyValue() {
        StringLiteral stringLiteral = newStringLiteral("");

        LiteralStringNode node = LiteralStringNode.from(stringLiteral);

        assertEquals("", node.getValue());
        assertEquals("", node.toString());
    }

    @Test
    public void from_createsLiteralStringNodeWithEscapedCharacters() {
        StringLiteral stringLiteral = newStringLiteral("line1\nline2");

        LiteralStringNode node = LiteralStringNode.from(stringLiteral);

        assertEquals("line1\nline2", node.getValue());
        assertEquals("line1\nline2", node.toString());
    }
}



























