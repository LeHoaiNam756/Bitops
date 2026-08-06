package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
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
        ASTNode statement = utils.Parser.parseStatementToAST("int y = x + 1;");
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) statement;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        Expression initializer = fragment.getInitializer();
        assertTrue(initializer instanceof InfixExpression);
        assertFalse(handler.supports(initializer));
    }

    // ─── eval() — Operand delegation ─────────────────────────────────────────

    @Test
    public void eval_delegatesOperandDispatcher() {
        PrefixExpression node = parsePrefixExpression("int y = -x;");
        RecordingDispatcher dispatcher = new RecordingDispatcher(new SymVariable("x"));
        SymbolicState state = SymbolicState.builder().build();

        handler.eval(node, state, dispatcher);

        assertEquals(node.getOperand(), dispatcher.lastNode);
        assertEquals(state, dispatcher.lastState);
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
        assertMutatingPrefix("int y = ++x;", SymBinaryOp.Op.ADD);
    }

    @Test
    public void eval_preDecrement_returnsDecOp() {
        assertMutatingPrefix("int y = --x;", SymBinaryOp.Op.SUB);
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
        AstDispatcher dispatcher = new RecordingDispatcher(new SymVariable("x"));
        SymbolicState state = SymbolicState.builder().build();

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymUnaryOp for: " + statement, result instanceof SymUnaryOp);
        assertEquals("Wrong operator for: " + statement, expectedOp, ((SymUnaryOp) result).op());
    }

    private void assertMutatingPrefix(String statement, SymBinaryOp.Op expectedOp) {
        PrefixExpression node = parsePrefixExpression(statement);
        AstDispatcher dispatcher = new RecordingDispatcher(new SymVariable("x"));
        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .build();
        SymVariable old = new SymVariable("x");
        state.getMemoryModel().write("x", old);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertTrue("Expected SymBinaryOp for: " + statement, result instanceof SymBinaryOp);
        SymBinaryOp updated = (SymBinaryOp) result;
        assertEquals(expectedOp, updated.op());
        assertEquals(old, updated.left());
        assertEquals(SymLiteral.of(1), updated.right());
        assertEquals(result, state.getMemoryModel().read("x").orElseThrow());
    }

    private static final class RecordingDispatcher extends AstDispatcher {
        private final SymbolicValue value;
        private ASTNode lastNode;
        private SymbolicState lastState;

        private RecordingDispatcher(SymbolicValue value) {
            this.value = value;
        }

        @Override
        public SymbolicValue eval(ASTNode node, SymbolicState state) {
            this.lastNode = node;
            this.lastState = state;
            return value;
        }
    }
}
