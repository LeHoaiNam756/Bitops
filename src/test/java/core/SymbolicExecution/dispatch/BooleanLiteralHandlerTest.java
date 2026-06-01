package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
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

public class BooleanLiteralHandlerTest {

    private final BooleanLiteralHandler handler = new BooleanLiteralHandler();

    private BooleanLiteral getBooleanLiteral(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        assertTrue("Expected VariableDeclarationStatement but got " + node.getClass().getSimpleName(),
                node instanceof VariableDeclarationStatement);
        VariableDeclarationStatement varDecl = (VariableDeclarationStatement) node;
        assertFalse("Expected at least one fragment", varDecl.fragments().isEmpty());
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDecl.fragments().get(0);
        assertTrue("Expected BooleanLiteral initializer but got " + fragment.getInitializer().getClass().getSimpleName(),
                fragment.getInitializer() instanceof BooleanLiteral);
        return (BooleanLiteral) fragment.getInitializer();
    }

    @Test
    public void supportsReturnsTrueForBooleanLiteral() {
        BooleanLiteral literal = getBooleanLiteral("boolean x = true;");
        assertTrue(handler.supports(literal));
    }

    @Test
    public void supportsReturnsFalseForNumberLiteral() {
        ASTNode node = Parser.parseStatementToAST("int x = 42;");
        assertTrue("Expected VariableDeclarationStatement", node instanceof VariableDeclarationStatement);
        VariableDeclarationStatement varDecl = (VariableDeclarationStatement) node;
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDecl.fragments().get(0);
        assertTrue("Expected NumberLiteral", fragment.getInitializer() instanceof NumberLiteral);
        assertFalse(handler.supports(fragment.getInitializer()));
    }

    @Test
    public void supportsReturnsFalseForCharacterLiteral() {
        ASTNode node = Parser.parseStatementToAST("char x = 'a';");
        assertTrue("Expected VariableDeclarationStatement", node instanceof VariableDeclarationStatement);
        VariableDeclarationStatement varDecl = (VariableDeclarationStatement) node;
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDecl.fragments().get(0);
        assertTrue("Expected CharacterLiteral", fragment.getInitializer() instanceof CharacterLiteral);
        assertFalse(handler.supports(fragment.getInitializer()));
    }

    @Test
    public void evalTrueLiteralReturnsSymLiteralTrue() {
        BooleanLiteral literal = getBooleanLiteral("boolean x = true;");
        SymbolicValue result = handler.eval(literal, null, null);
        assertTrue("Expected SymLiteral", result instanceof SymLiteral);
        SymLiteral symLiteral = (SymLiteral) result;
        assertEquals(Boolean.TRUE, symLiteral.value());
    }

    @Test
    public void evalFalseLiteralReturnsSymLiteralFalse() {
        BooleanLiteral literal = getBooleanLiteral("boolean x = false;");
        SymbolicValue result = handler.eval(literal, null, null);
        assertTrue("Expected SymLiteral", result instanceof SymLiteral);
        SymLiteral symLiteral = (SymLiteral) result;
        assertEquals(Boolean.FALSE, symLiteral.value());
    }

    @Test
    public void evalIgnoresStateAndDispatcher() {
        BooleanLiteral literal = getBooleanLiteral("boolean x = true;");
        SymbolicState state = SymbolicState.builder().build();
        AstDispatcher dispatcher = new AstDispatcher();

        SymbolicValue result1 = handler.eval(literal, null, null);
        SymbolicValue result2 = handler.eval(literal, state, dispatcher);

        assertTrue("Expected SymLiteral for result1", result1 instanceof SymLiteral);
        assertTrue("Expected SymLiteral for result2", result2 instanceof SymLiteral);
        assertEquals(((SymLiteral) result1).value(), ((SymLiteral) result2).value());
    }
}
