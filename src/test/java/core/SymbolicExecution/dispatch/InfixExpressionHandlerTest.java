package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

public class InfixExpressionHandlerTest {

    private final InfixExpressionHandler handler = new InfixExpressionHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnsTrueForInfixExpression() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnsFalseForNonInfixNode() {
        ASTNode node = parseNumberLiteralNode("int x = 42;");
        assertFalse(handler.supports(node));
    }

    @Test
    public void supports_returnsFalseForMethodInvocation() {
        ASTNode node = parseMethodInvocationNode("System.out.println(\"hi\");");
        assertFalse(handler.supports(node));
    }

    // ─── eval() — Operand delegation ─────────────────────────────────────────

    @Test
    public void eval_delegatesLeftOperandToDispatcher() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(1);
        SymbolicValue rightStub = SymLiteral.of(2);

        when(dispatcher.eval(eq(node.getLeftOperand()), eq(state))).thenReturn(leftStub);
        when(dispatcher.eval(eq(node.getRightOperand()), eq(state))).thenReturn(rightStub);

        handler.eval(node, state, dispatcher);

        verify(dispatcher, times(1)).eval(eq(node.getLeftOperand()), eq(state));
    }

    @Test
    public void eval_delegatesRightOperandToDispatcher() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(1);
        SymbolicValue rightStub = SymLiteral.of(2);

        when(dispatcher.eval(eq(node.getLeftOperand()), eq(state))).thenReturn(leftStub);
        when(dispatcher.eval(eq(node.getRightOperand()), eq(state))).thenReturn(rightStub);

        handler.eval(node, state, dispatcher);

        verify(dispatcher, times(1)).eval(eq(node.getRightOperand()), eq(state));
    }

    @Test
    public void eval_returnsSymBinaryOpWithCorrectLeftValue() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(42);
        SymbolicValue rightStub = SymLiteral.of(99);

        when(dispatcher.eval(any(), eq(state))).thenReturn(leftStub).thenReturn(rightStub);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymBinaryOp", result instanceof SymBinaryOp);
        assertSame(leftStub, ((SymBinaryOp) result).left());
    }

    @Test
    public void eval_returnsSymBinaryOpWithCorrectRightValue() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(42);
        SymbolicValue rightStub = SymLiteral.of(99);

        when(dispatcher.eval(any(), eq(state))).thenReturn(leftStub).thenReturn(rightStub);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymBinaryOp", result instanceof SymBinaryOp);
        assertSame(rightStub, ((SymBinaryOp) result).right());
    }

    @Test
    public void eval_evaluatesLeftBeforeRight() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(1);
        SymbolicValue rightStub = SymLiteral.of(2);

        when(dispatcher.eval(eq(node.getLeftOperand()), eq(state))).thenReturn(leftStub);
        when(dispatcher.eval(eq(node.getRightOperand()), eq(state))).thenReturn(rightStub);

        handler.eval(node, state, dispatcher);

        InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).eval(eq(node.getLeftOperand()), eq(state));
        inOrder.verify(dispatcher).eval(eq(node.getRightOperand()), eq(state));
    }

    // ─── eval() — Operator propagation ───────────────────────────────────────

    @Test
    public void eval_mapsPlus_toAdd() {
        assertEvalOperator("int x = 1 + 2;", SymBinaryOp.Op.ADD);
    }

    @Test
    public void eval_mapsMinus_toSub() {
        assertEvalOperator("int x = 1 - 2;", SymBinaryOp.Op.SUB);
    }

    @Test
    public void eval_mapsTimes_toMul() {
        assertEvalOperator("int x = 1 * 2;", SymBinaryOp.Op.MUL);
    }

    @Test
    public void eval_mapsDivide_toDiv() {
        assertEvalOperator("int x = 1 / 2;", SymBinaryOp.Op.DIV);
    }

    @Test
    public void eval_mapsRemainder_toMod() {
        assertEvalOperator("int x = 1 % 2;", SymBinaryOp.Op.MOD);
    }

    @Test
    public void eval_mapsAnd_toBand() {
        assertEvalOperator("int x = 1 & 2;", SymBinaryOp.Op.BAND);
    }

    @Test
    public void eval_mapsOr_toBor() {
        assertEvalOperator("int x = 1 | 2;", SymBinaryOp.Op.BOR);
    }

    @Test
    public void eval_mapsXor_toBxor() {
        assertEvalOperator("int x = 1 ^ 2;", SymBinaryOp.Op.BXOR);
    }

    @Test
    public void eval_mapsLeftShift_toBls() {
        assertEvalOperator("int x = 1 << 2;", SymBinaryOp.Op.BLS);
    }

    @Test
    public void eval_mapsRightShiftSigned_toBrs() {
        assertEvalOperator("int x = 1 >> 2;", SymBinaryOp.Op.BRS);
    }

    @Test
    public void eval_mapsRightShiftUnsigned_toBurs() {
        assertEvalOperator("int x = 1 >>> 2;", SymBinaryOp.Op.BURS);
    }

    @Test
    public void eval_mapsEquals_toEq() {
        assertEvalOperator("boolean x = 1 == 2;", SymBinaryOp.Op.EQ);
    }

    @Test
    public void eval_mapsNotEquals_toNeq() {
        assertEvalOperator("boolean x = 1 != 2;", SymBinaryOp.Op.NEQ);
    }

    @Test
    public void eval_mapsGreater_toSgt() {
        assertEvalOperator("boolean x = 1 > 2;", SymBinaryOp.Op.SGT);
    }

    @Test
    public void eval_mapsLess_toSlt() {
        assertEvalOperator("boolean x = 1 < 2;", SymBinaryOp.Op.SLT);
    }

    @Test
    public void eval_mapsGreaterEquals_toSge() {
        assertEvalOperator("boolean x = 1 >= 2;", SymBinaryOp.Op.SGE);
    }

    @Test
    public void eval_mapsLessEquals_toSle() {
        assertEvalOperator("boolean x = 1 <= 2;", SymBinaryOp.Op.SLE);
    }

    @Test
    public void eval_mapsConditionalAnd_toAnd() {
        assertEvalOperator("boolean x = true && false;", SymBinaryOp.Op.AND);
    }

    @Test
    public void eval_mapsConditionalOr_toOr() {
        assertEvalOperator("boolean x = true || false;", SymBinaryOp.Op.OR);
    }

    // ─── eval() — Edge cases ─────────────────────────────────────────────────

    @Test
    public void eval_propagatesDispatcherExceptionForLeftOperand() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();

        when(dispatcher.eval(eq(node.getLeftOperand()), eq(state)))
                .thenThrow(new RuntimeException("left error"));

        assertThrows(RuntimeException.class, () -> handler.eval(node, state, dispatcher));
    }

    @Test
    public void eval_propagatesDispatcherExceptionForRightOperand() {
        InfixExpression node = parseInfixExpression("int x = 1 + 2;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(1);

        when(dispatcher.eval(eq(node.getLeftOperand()), eq(state))).thenReturn(leftStub);
        when(dispatcher.eval(eq(node.getRightOperand()), eq(state)))
                .thenThrow(new RuntimeException("right error"));

        assertThrows(RuntimeException.class, () -> handler.eval(node, state, dispatcher));
    }

    // ─── mapOp() — Exhaustive coverage ───────────────────────────────────────

    @Test
    public void mapOp_plusToPlus() {
        assertEquals(SymBinaryOp.Op.ADD, InfixExpressionHandler.mapOp(InfixExpression.Operator.PLUS));
    }

    @Test
    public void mapOp_minusToMinus() {
        assertEquals(SymBinaryOp.Op.SUB, InfixExpressionHandler.mapOp(InfixExpression.Operator.MINUS));
    }

    @Test
    public void mapOp_timesToTimes() {
        assertEquals(SymBinaryOp.Op.MUL, InfixExpressionHandler.mapOp(InfixExpression.Operator.TIMES));
    }

    @Test
    public void mapOp_divideToDivide() {
        assertEquals(SymBinaryOp.Op.DIV, InfixExpressionHandler.mapOp(InfixExpression.Operator.DIVIDE));
    }

    @Test
    public void mapOp_remainderToMod() {
        assertEquals(SymBinaryOp.Op.MOD, InfixExpressionHandler.mapOp(InfixExpression.Operator.REMAINDER));
    }

    @Test
    public void mapOp_andToBand() {
        assertEquals(SymBinaryOp.Op.BAND, InfixExpressionHandler.mapOp(InfixExpression.Operator.AND));
    }

    @Test
    public void mapOp_orToBor() {
        assertEquals(SymBinaryOp.Op.BOR, InfixExpressionHandler.mapOp(InfixExpression.Operator.OR));
    }

    @Test
    public void mapOp_xorToBxor() {
        assertEquals(SymBinaryOp.Op.BXOR, InfixExpressionHandler.mapOp(InfixExpression.Operator.XOR));
    }

    @Test
    public void mapOp_leftShiftToBls() {
        assertEquals(SymBinaryOp.Op.BLS, InfixExpressionHandler.mapOp(InfixExpression.Operator.LEFT_SHIFT));
    }

    @Test
    public void mapOp_rightShiftSignedToBrs() {
        assertEquals(SymBinaryOp.Op.BRS, InfixExpressionHandler.mapOp(InfixExpression.Operator.RIGHT_SHIFT_SIGNED));
    }

    @Test
    public void mapOp_rightShiftUnsignedToBurs() {
        assertEquals(SymBinaryOp.Op.BURS, InfixExpressionHandler.mapOp(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED));
    }

    @Test
    public void mapOp_equalsToEq() {
        assertEquals(SymBinaryOp.Op.EQ, InfixExpressionHandler.mapOp(InfixExpression.Operator.EQUALS));
    }

    @Test
    public void mapOp_notEqualsToNeq() {
        assertEquals(SymBinaryOp.Op.NEQ, InfixExpressionHandler.mapOp(InfixExpression.Operator.NOT_EQUALS));
    }

    @Test
    public void mapOp_greaterToSgt() {
        assertEquals(SymBinaryOp.Op.SGT, InfixExpressionHandler.mapOp(InfixExpression.Operator.GREATER));
    }

    @Test
    public void mapOp_lessToSlt() {
        assertEquals(SymBinaryOp.Op.SLT, InfixExpressionHandler.mapOp(InfixExpression.Operator.LESS));
    }

    @Test
    public void mapOp_greaterEqualsToSge() {
        assertEquals(SymBinaryOp.Op.SGE, InfixExpressionHandler.mapOp(InfixExpression.Operator.GREATER_EQUALS));
    }

    @Test
    public void mapOp_lessEqualsToSle() {
        assertEquals(SymBinaryOp.Op.SLE, InfixExpressionHandler.mapOp(InfixExpression.Operator.LESS_EQUALS));
    }

    @Test
    public void mapOp_conditionalAndToAnd() {
        assertEquals(SymBinaryOp.Op.AND, InfixExpressionHandler.mapOp(InfixExpression.Operator.CONDITIONAL_AND));
    }

    @Test
    public void mapOp_conditionalOrToOr() {
        assertEquals(SymBinaryOp.Op.OR, InfixExpressionHandler.mapOp(InfixExpression.Operator.CONDITIONAL_OR));
    }

    @Test
    public void mapOp_throwsIllegalArgumentException_forUnsupportedOperator() {
        InfixExpression.Operator mockOp = mock(InfixExpression.Operator.class);
        when(mockOp.toString()).thenReturn("MOCKED_UNSUPPORTED");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> InfixExpressionHandler.mapOp(mockOp)
        );
        assertTrue("Expected message to contain 'Unsupported operator'",
                ex.getMessage().contains("Unsupported operator"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private InfixExpression parseInfixExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        Expression initializer = fragment.getInitializer();
        assertTrue("Expected InfixExpression but got " + initializer.getClass().getSimpleName(),
                initializer instanceof InfixExpression);
        return (InfixExpression) initializer;
    }

    private ASTNode parseNumberLiteralNode(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        assertTrue("Expected NumberLiteral", fragment.getInitializer() instanceof NumberLiteral);
        return (NumberLiteral) fragment.getInitializer();
    }

    private ASTNode parseMethodInvocationNode(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        assertTrue("Expected ExpressionStatement but got " + node.getClass().getSimpleName(),
                node instanceof ExpressionStatement);
        ExpressionStatement exprStmt = (ExpressionStatement) node;
        assertTrue("Expected MethodInvocation but got " + exprStmt.getExpression().getClass().getSimpleName(),
                exprStmt.getExpression() instanceof MethodInvocation);
        return (MethodInvocation) exprStmt.getExpression();
    }

    private void assertEvalOperator(String statement, SymBinaryOp.Op expectedOp) {
        InfixExpression node = parseInfixExpression(statement);
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder().build();
        SymbolicValue leftStub = SymLiteral.of(0);
        SymbolicValue rightStub = SymLiteral.of(0);

        when(dispatcher.eval(any(), eq(state))).thenReturn(leftStub).thenReturn(rightStub);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymBinaryOp for: " + statement, result instanceof SymBinaryOp);
        assertEquals("Wrong operator for: " + statement, expectedOp, ((SymBinaryOp) result).op());
    }
}
