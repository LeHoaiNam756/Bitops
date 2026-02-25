package SymbolicExecution.Expression.Literal;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.junit.Test;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralCharacterNode;

import static org.junit.Assert.*;

public class TestLiteralCharacterNode {

    private CharacterLiteral newCharacterLiteral(char value) {
        AST ast = AST.newAST(AST.JLS8);
        CharacterLiteral characterLiteral = ast.newCharacterLiteral();
        characterLiteral.setEscapedValue("'" + value + "'");
        return characterLiteral;
    }

    @Test
    public void from_createsLiteralCharacterNodeWithLowercaseValue() {
        CharacterLiteral characterLiteral = newCharacterLiteral('a');

        LiteralCharacterNode node = LiteralCharacterNode.from(characterLiteral);

        assertEquals('a', node.getValue());
        assertEquals("a", node.toString());
    }

    @Test
    public void from_createsLiteralCharacterNodeWithUppercaseValue() {
        CharacterLiteral characterLiteral = newCharacterLiteral('Z');

        LiteralCharacterNode node = LiteralCharacterNode.from(characterLiteral);

        assertEquals('Z', node.getValue());
        assertEquals("Z", node.toString());
    }

    @Test
    public void from_createsLiteralCharacterNodeWithDigitValue() {
        CharacterLiteral characterLiteral = newCharacterLiteral('0');

        LiteralCharacterNode node = LiteralCharacterNode.from(characterLiteral);

        assertEquals('0', node.getValue());
        assertEquals("0", node.toString());
    }
}


