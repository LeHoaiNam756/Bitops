package SymbolicExecution.Expression.Literal;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.junit.Test;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;

import static org.junit.Assert.*;

public class TestLiteralBooleanNode {

    private BooleanLiteral newBooleanLiteral(boolean value) {
        AST ast = AST.newAST(AST.JLS8);
        return ast.newBooleanLiteral(value);
    }

    @Test
    public void from_createsLiteralBooleanNodeWithTrueValue() {
        BooleanLiteral booleanLiteral = newBooleanLiteral(true);

        LiteralBooleanNode node = LiteralBooleanNode.from(booleanLiteral);

        assertEquals(true, node.isValue());
        assertEquals("true", node.toString());
    }

    @Test
    public void from_createsLiteralBooleanNodeWithFalseValue() {
        BooleanLiteral booleanLiteral = newBooleanLiteral(false);

        LiteralBooleanNode node = LiteralBooleanNode.from(booleanLiteral);

        assertEquals(false, node.isValue());
        assertEquals("false", node.toString());
    }
}
