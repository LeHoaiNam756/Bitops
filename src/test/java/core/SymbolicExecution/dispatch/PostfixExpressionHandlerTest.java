package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostfixExpressionHandlerTest {

    private final PostfixExpressionHandler handler = new PostfixExpressionHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnTrue_forPostfixNode() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnFalse_forNotPostfix() {
        ASTNode node = parseInfixExpressionNode("int x = 1 + 2;");
        assertFalse(handler.supports(node));
    }

    // ─── eval() — Post-increment returns old value ────────────────────────────

    @Test
    public void eval_postIncrement_returnsOldValue() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        MemoryModel memory = new MemoryModel();
        SymbolicValue storedValue = SymLiteral.of(42);
        memory.write("x", storedValue);
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(storedValue, result);
    }

    @Test
    public void eval_postDecrement_returnsOldValue() {
        PostfixExpression node = parsePostfixExpression("int y = x--;");
        MemoryModel memory = new MemoryModel();
        SymbolicValue storedValue = SymLiteral.of(99);
        memory.write("x", storedValue);
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(storedValue, result);
    }

    // ─── eval() — Memory updates ─────────────────────────────────────────────

    @Test
    public void eval_postIncrement_updatesMemory() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        MemoryModel memory = new MemoryModel();
        SymbolicValue initialValue = SymLiteral.of(42);
        memory.write("x", initialValue);
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        handler.eval(node, state, dispatcher);

        SymbolicValue updated = memory.read("x").orElse(null);
        assertNotNull(updated);
        assertTrue(updated instanceof SymBinaryOp);
        SymBinaryOp binaryOp = (SymBinaryOp) updated;
        assertEquals(SymBinaryOp.Op.ADD, binaryOp.op());
        assertSame(initialValue, binaryOp.left());
        assertEquals(SymLiteral.of(1), binaryOp.right());
    }

    @Test
    public void eval_postDecrement_updatesMemory() {
        PostfixExpression node = parsePostfixExpression("int y = x--;");
        MemoryModel memory = new MemoryModel();
        SymbolicValue initialValue = SymLiteral.of(99);
        memory.write("x", initialValue);
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        handler.eval(node, state, dispatcher);

        SymbolicValue updated = memory.read("x").orElse(null);
        assertNotNull(updated);
        assertTrue(updated instanceof SymBinaryOp);
        SymBinaryOp binaryOp = (SymBinaryOp) updated;
        assertEquals(SymBinaryOp.Op.SUB, binaryOp.op());
        assertSame(initialValue, binaryOp.left());
        assertEquals(SymLiteral.of(1), binaryOp.right());
    }

    // ─── eval() — Operand delegation ─────────────────────────────────────────

    @Test
    public void eval_delegatesOperandDispatcher() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        MemoryModel memory = new MemoryModel();
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        handler.eval(node, state, dispatcher);

        verify(dispatcher).eval(eq(node.getOperand()), eq(state));
    }

    // ─── eval() — Edge cases ─────────────────────────────────────────────────

    @Test
    public void eval_memoryStoreVariable_returnsOldValue() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = new MemoryModel();
        SymVariable symVar = new SymVariable("x");
        memory.write("x", symVar);
        memory.write("x", SymLiteral.of(42));
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(42));

        SymbolicValue result = handler.eval(node, state, dispatcher);
        assertTrue(result instanceof SymLiteral);
        assertEquals(SymLiteral.of(42).value(), ((SymLiteral) result).value());
    }

    @Test
    public void eval_memoryStoreVariable_updatesMemory() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        MemoryModel memory = new MemoryModel();
        SymVariable symVar = new SymVariable("x");
        SymLiteral symValue42 = new SymLiteral(42);
        memory.write("x", symVar);
        memory.write("x", symValue42);
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        when(dispatcher.eval(any(), eq(state))).thenReturn(symValue42);
        handler.eval(node, state, dispatcher);
        SymbolicValue updated = memory.read("x").orElse(null);
        assertNotNull(updated);
        assertTrue(updated instanceof SymBinaryOp);
        SymBinaryOp binaryOp = (SymBinaryOp) updated;
        assertEquals(SymBinaryOp.Op.ADD, binaryOp.op());
        assertSame(symValue42, binaryOp.left());
        assertEquals(SymLiteral.of(1), binaryOp.right());
    }

    @Test
    public void eval_missingMemoryVariable_returnsVariableItself() {
        PostfixExpression node = parsePostfixExpression("int y = x++;");
        MemoryModel memory = new MemoryModel();
        SymbolicState state = SymbolicState.builder().memoryModel(memory).build();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymVariable symVar = new SymVariable("x");
        when(dispatcher.eval(any(), eq(state))).thenReturn(symVar);

        SymbolicValue result = handler.eval(node, state, dispatcher);

        assertSame(symVar, result);
    }



    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PostfixExpression parsePostfixExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        Expression initializer = fragment.getInitializer();
        assertTrue("Expected PostfixExpression but got " + initializer.getClass().getSimpleName(),
                initializer instanceof PostfixExpression);
        return (PostfixExpression) initializer;
    }

    private ASTNode parseInfixExpressionNode(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        org.eclipse.jdt.core.dom.VariableDeclarationStatement varDecl =
                (org.eclipse.jdt.core.dom.VariableDeclarationStatement) node;
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment =
                (org.eclipse.jdt.core.dom.VariableDeclarationFragment) varDecl.fragments().get(0);
        assertTrue("Expected InfixExpression", fragment.getInitializer() instanceof InfixExpression);
        return (InfixExpression) fragment.getInitializer();
    }
}
