package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymITE;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConditionalExpressionHandlerTest {

    private final ConditionalExpressionHandler handler = new ConditionalExpressionHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnsTrue_forConditionalExpression() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnsFalse_forOtherNodeTypes() {
        ASTNode node = utils.Parser.parseStatementToAST("int x = 42;");
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varStmt =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment frag =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varStmt.fragments().get(0);
        assertFalse(handler.supports(frag.getInitializer()));
    }

    // ─── eval() — Delegation to sub-expressions ──────────────────────────────

    @Test
    public void eval_returnsSymITE() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue stub = SymLiteral.of(0);

        when(dispatcher.eval(any(), eq(state))).thenReturn(stub);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertNotNull(result);
        assertTrue("Expected SymITE", result instanceof SymITE);
    }

    @Test
    public void eval_evaluatesConditionThenThenThenElse() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condStub = SymLiteral.of(1);
        SymbolicValue thenStub = SymLiteral.of(2);
        SymbolicValue elseStub = SymLiteral.of(3);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condStub);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state))).thenReturn(thenStub);
        when(dispatcher.eval(eq(node.getElseExpression()), eq(state))).thenReturn(elseStub);

        handler.eval(node, state, dispatcher);

        InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).eval(eq(node.getExpression()), eq(state));
        inOrder.verify(dispatcher).eval(eq(node.getThenExpression()), eq(state));
        inOrder.verify(dispatcher).eval(eq(node.getElseExpression()), eq(state));
    }

    @Test
    public void eval_passesStateCorrectlyToDispatcher() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue stub = SymLiteral.of(0);

        when(dispatcher.eval(any(), eq(state))).thenReturn(stub);

        handler.eval(node, state, dispatcher);

        verify(dispatcher, times(3)).eval(any(), eq(state));
    }

    // ─── eval() — SymITE field assignment ────────────────────────────────────

    @Test
    public void eval_conditionResultStoredAsFirstSymITEArgument() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condValue = SymLiteral.of(1);
        SymbolicValue thenValue = SymLiteral.of(2);
        SymbolicValue elseValue = SymLiteral.of(3);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condValue);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state))).thenReturn(thenValue);
        when(dispatcher.eval(eq(node.getElseExpression()), eq(state))).thenReturn(elseValue);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(condValue, ((SymITE) result).cond());
    }

    @Test
    public void eval_thenResultStoredAsSecondSymITEArgument() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condValue = SymLiteral.of(1);
        SymbolicValue thenValue = SymLiteral.of(2);
        SymbolicValue elseValue = SymLiteral.of(3);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condValue);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state))).thenReturn(thenValue);
        when(dispatcher.eval(eq(node.getElseExpression()), eq(state))).thenReturn(elseValue);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(thenValue, ((SymITE) result).thenBranch());
    }

    @Test
    public void eval_elseResultStoredAsThirdSymITEArgument() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condValue = SymLiteral.of(1);
        SymbolicValue thenValue = SymLiteral.of(2);
        SymbolicValue elseValue = SymLiteral.of(3);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condValue);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state))).thenReturn(thenValue);
        when(dispatcher.eval(eq(node.getElseExpression()), eq(state))).thenReturn(elseValue);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(elseValue, ((SymITE) result).elseBranch());
    }

    // ─── eval() — Edge cases ─────────────────────────────────────────────────

    @Test
    public void eval_propagatesExceptionFromDispatcher_forCondition() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();

        when(dispatcher.eval(eq(node.getExpression()), eq(state)))
                .thenThrow(new RuntimeException("condition error"));

        assertThrows(RuntimeException.class, () -> handler.eval(node, state, dispatcher));
    }

    @Test
    public void eval_propagatesExceptionFromDispatcher_forThenBranch() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condValue = SymLiteral.of(1);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condValue);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state)))
                .thenThrow(new RuntimeException("then error"));

        assertThrows(RuntimeException.class, () -> handler.eval(node, state, dispatcher));
    }

    @Test
    public void eval_propagatesExceptionFromDispatcher_forElseBranch() {
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue condValue = SymLiteral.of(1);
        SymbolicValue thenValue = SymLiteral.of(2);

        when(dispatcher.eval(eq(node.getExpression()), eq(state))).thenReturn(condValue);
        when(dispatcher.eval(eq(node.getThenExpression()), eq(state))).thenReturn(thenValue);
        when(dispatcher.eval(eq(node.getElseExpression()), eq(state)))
                .thenThrow(new RuntimeException("else error"));

        assertThrows(RuntimeException.class, () -> handler.eval(node, state, dispatcher));
    }

    @Test
    public void eval_doesNotModifySymbolicState() {
        SymbolicState state = mock(SymbolicState.class);
        ConditionalExpression node = parseConditionalExpression("int x = true ? 1 : 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicValue stub = SymLiteral.of(0);

        when(dispatcher.eval(any(), eq(state))).thenReturn(stub);

        handler.eval(node, state, dispatcher);

        verify(state, never()).getMemoryModel();
        verify(state, never()).getTypeContext();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ConditionalExpression parseConditionalExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        Expression initializer = fragment.getInitializer();
        assertTrue("Expected ConditionalExpression but got " + initializer.getClass().getSimpleName(),
                initializer instanceof ConditionalExpression);
        return (ConditionalExpression) initializer;
    }
}
