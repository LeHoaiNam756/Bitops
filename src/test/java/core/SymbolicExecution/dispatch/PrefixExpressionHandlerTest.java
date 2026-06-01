package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.SymUnaryOp;
import core.SymbolicExecution.model.SymVariable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

public class PrefixExpressionHandlerTest {

    private final PrefixExpressionHandler handler = new PrefixExpressionHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnTrue_forPrefixNode() {
        PrefixExpression node = parsePrefixExpression("int y = -x;");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnFalse_forNotPrefix() {
        ASTNode mockNode = mock(InfixExpression.class);
        assertFalse(handler.supports(mockNode));
    }

    // ─── eval() — Operand delegation ─────────────────────────────────────────

    @Test
    public void eval_delegatesOperandDispatcher() {
        PrefixExpression node = parsePrefixExpression("int y = -x;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue mockOperand = new SymVariable("x");

        when(dispatcher.eval(eq(node.getOperand()), eq(state))).thenReturn(mockOperand);

        handler.eval(node, state, dispatcher);

        verify(dispatcher).eval(eq(node.getOperand()), eq(state));
    }

    // ─── eval() — Operator propagation ───────────────────────────────────────

    @Test
    public void eval_minus_returnsNegUnaryOp() {
        assertEvalOperator("int y = -x;", SymUnaryOp.Op.NEG);
    }

    @Test
    public void eval_plus_returnsPlusUnaryOp() {
        assertEvalOperator("int y = +x;", SymUnaryOp.Op.PLUS);
    }

    @Test
    public void eval_not_returnsNotUnaryOp() {
        assertEvalOperator("boolean y = !x;", SymUnaryOp.Op.NOT);
    }

    @Test
    public void eval_complement_returnsComplement() {
        assertEvalOperator("int y = ~x;", SymUnaryOp.Op.COMPLIMENT);
    }

    @Test
    public void eval_preIncrement_returnsIncOp() {
        assertEvalOperator("int y = ++x;", SymUnaryOp.Op.INC);
    }

    @Test
    public void eval_preDecrement_returnsDecOp() {
        assertEvalOperator("int y = --x;", SymUnaryOp.Op.DEC);
    }

    // ─── mapOp() — Exhaustive coverage ───────────────────────────────────────

    @Test
    public void mapOp_plusToPlus() {
        assertEquals(SymUnaryOp.Op.PLUS, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.PLUS));
    }

    @Test
    public void mapOp_minusToNeg() {
        assertEquals(SymUnaryOp.Op.NEG, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.MINUS));
    }

    @Test
    public void mapOp_incrementToInc() {
        assertEquals(SymUnaryOp.Op.INC, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.INCREMENT));
    }

    @Test
    public void mapOp_decrementToDec() {
        assertEquals(SymUnaryOp.Op.DEC, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.DECREMENT));
    }

    @Test
    public void mapOp_notToNot() {
        assertEquals(SymUnaryOp.Op.NOT, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.NOT));
    }

    @Test
    public void mapOp_complementToCompliment() {
        assertEquals(SymUnaryOp.Op.COMPLIMENT, PrefixExpressionHandler.mapOp(PrefixExpression.Operator.COMPLEMENT));
    }

    @Test
    public void mapOp_throwsIllegalArgumentException_forUnknownOperator() {
        PrefixExpression.Operator mockOp = mock(PrefixExpression.Operator.class);
        when(mockOp.toString()).thenReturn("MOCKED_UNSUPPORTED");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PrefixExpressionHandler.mapOp(mockOp)
        );
        assertTrue("Expected message to contain 'Unknown operator'",
                ex.getMessage().contains("Unknown operator"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PrefixExpression parsePrefixExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        Expression initializer = fragment.getInitializer();
        assertTrue("Expected PrefixExpression but got " + initializer.getClass().getSimpleName(),
                initializer instanceof PrefixExpression);
        return (PrefixExpression) initializer;
    }

    private void assertEvalOperator(String statement, SymUnaryOp.Op expectedOp) {
        PrefixExpression node = parsePrefixExpression(statement);
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue mockOperand = new SymVariable("x");

        when(dispatcher.eval(any(), eq(state))).thenReturn(mockOperand);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymUnaryOp for: " + statement, result instanceof SymUnaryOp);
        assertEquals("Wrong operator for: " + statement, expectedOp, ((SymUnaryOp) result).op());
    }
}
