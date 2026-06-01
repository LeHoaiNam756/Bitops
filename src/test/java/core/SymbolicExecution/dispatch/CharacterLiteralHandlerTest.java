package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.*;

public class CharacterLiteralHandlerTest {

    private CharacterLiteral parseCharacterLiteral(String literalSource) {
        String statement = "char x = " + literalSource + ";";
        VariableDeclarationStatement stmt = (VariableDeclarationStatement) Parser.parseStatementToAST(statement);
        VariableDeclarationFragment frag = (VariableDeclarationFragment) stmt.fragments().get(0);
        return (CharacterLiteral) frag.getInitializer();
    }

    private ASTNode parseInitializer(String statement) {
        VariableDeclarationStatement stmt = (VariableDeclarationStatement) Parser.parseStatementToAST(statement);
        VariableDeclarationFragment frag = (VariableDeclarationFragment) stmt.fragments().get(0);
        return frag.getInitializer();
    }

    @Test
    public void supportsReturnsTrueForCharacterLiteral() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'a'");
        assertTrue(handler.supports(literal));
    }

    @Test
    public void evalRegularCharacter() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'a'");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('a'), result);
    }

    @Test
    public void evalNewlineEscape() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'\\n'");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('\n'), result);
    }

    @Test
    public void evalTabEscape() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'\\t'");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('\t'), result);
    }

    @Test
    public void evalBackslashEscape() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'\\\\'");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('\\'), result);
    }

    @Test
    public void evalSingleQuoteEscape() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'\\''");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('\''), result);
    }

    @Test
    public void evalUnicodeCharacter() {
        CharacterLiteralHandler handler = new CharacterLiteralHandler();
        CharacterLiteral literal = parseCharacterLiteral("'\\u0041'");
        SymbolicValue result = handler.eval(literal, null, null);
        assertEquals(SymLiteral.of('A'), result);
    }
}
