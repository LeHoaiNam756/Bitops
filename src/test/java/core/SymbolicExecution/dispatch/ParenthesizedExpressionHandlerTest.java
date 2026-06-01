package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParenthesizedExpressionHandlerTest {

    private final ParenthesizedExpressionHandler handler = new ParenthesizedExpressionHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnsTrueForParenthesizedExpression() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (42);");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnsFalseForNumberLiteral() {
        ASTNode node = utils.Parser.parseStatementToAST("int x = 42;");
        assertTrue("Expected VariableDeclarationStatement but got " + node.getClass().getSimpleName(),
                node instanceof org.eclipse.jdt.core.dom.VariableDeclarationStatement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varStmt =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment frag =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varStmt.fragments().get(0);
        assertFalse(handler.supports(frag.getInitializer()));
    }

    // ─── eval() — Delegation to inner expression ─────────────────────────────

    @Test
    public void eval_delegatesInnerExpressionToDispatcher() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (42);");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();

        handler.eval(node, state, dispatcher);

        verify(dispatcher).eval(eq(node.getExpression()), eq(state));
    }

    @Test
    public void eval_returnsInnerExpressionsResult() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (42);");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue mockResult = SymLiteral.of(99);
        when(dispatcher.eval(any(), any())).thenReturn(mockResult);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(mockResult, result);
    }

    @Test
    public void eval_propagatesDispatcherException() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (42);");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        RuntimeException expected = new RuntimeException("dispatcher error");
        when(dispatcher.eval(any(), any())).thenThrow(expected);

        try {
            handler.eval(node, state, dispatcher);
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException e) {
            assertSame(expected, e);
        }
    }

    // ─── eval() — Inner node types ────────────────────────────────────────────

    @Test
    public void eval_dispatchesToInnerNumberLiteral() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (42);");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        when(dispatcher.eval(any(), any())).thenReturn(SymLiteral.of(42));

        handler.eval(node, state, dispatcher);

        ArgumentCaptor<ASTNode> captor = ArgumentCaptor.forClass(ASTNode.class);
        verify(dispatcher).eval(captor.capture(), eq(state));
        assertTrue("Expected NumberLiteral but got " + captor.getValue().getClass().getSimpleName(),
                captor.getValue() instanceof NumberLiteral);
    }

    @Test
    public void eval_dispatchesToInnerInfixExpression() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = (1 + 2);");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        when(dispatcher.eval(any(), any())).thenReturn(SymLiteral.of(42));

        handler.eval(node, state, dispatcher);

        ArgumentCaptor<ASTNode> captor = ArgumentCaptor.forClass(ASTNode.class);
        verify(dispatcher).eval(captor.capture(), eq(state));
        assertTrue("Expected InfixExpression but got " + captor.getValue().getClass().getSimpleName(),
                captor.getValue() instanceof InfixExpression);
    }

    // ─── eval() — Edge cases ──────────────────────────────────────────────────

    @Test
    public void eval_throwsNPEForNullNode() {
        try {
            handler.eval(null, null, mock(AstDispatcher.class));
            fail("Expected NullPointerException was not thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void eval_handlesNestedParentheses() {
        ParenthesizedExpression node = parseParenthesizedExpression("int x = ((42));");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();

        handler.eval(node, state, dispatcher);

        verify(dispatcher).eval(eq(node.getExpression()), eq(state));
    }

    @Test
    public void eval_dispatchesParenthesizedExpressionWithinLargerExpression() {
        ASTNode root = utils.Parser.parseStatementToAST("int x = 1 + (2 * 3);");
        assertTrue("Expected VariableDeclarationStatement but got " + root.getClass().getSimpleName(),
                root instanceof org.eclipse.jdt.core.dom.VariableDeclarationStatement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varStmt =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) root;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment frag =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varStmt.fragments().get(0);
        InfixExpression infix = (InfixExpression) frag.getInitializer();
        ParenthesizedExpression node = (ParenthesizedExpression) infix.getRightOperand();

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();

        handler.eval(node, state, dispatcher);

        verify(dispatcher).eval(eq(node.getExpression()), eq(state));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ParenthesizedExpression parseParenthesizedExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        assertTrue("Expected VariableDeclarationStatement but got " + node.getClass().getSimpleName(),
                node instanceof org.eclipse.jdt.core.dom.VariableDeclarationStatement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varStmt =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment frag =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varStmt.fragments().get(0);
        assertTrue("Expected ParenthesizedExpression initializer but got " + frag.getInitializer().getClass().getSimpleName(),
                frag.getInitializer() instanceof ParenthesizedExpression);
        return (ParenthesizedExpression) frag.getInitializer();
    }
}
