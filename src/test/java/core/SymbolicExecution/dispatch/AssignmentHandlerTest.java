package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymCastOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.junit.Test;
import org.mockito.InOrder;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssignmentHandlerTest {

    private final AssignmentHandler handler = new AssignmentHandler();

    @Test
    public void eval_arrayElementAssignment_writesArrayStoreToArrayVariable() {
        Assignment assignment = parseAssignment("nums[j] = nums[i];");
        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        SymbolicValue result = handler.eval(assignment, state, createDispatcher());

        SymbolicValue stored = state.getMemoryModel().read("nums").orElseThrow();
        assertTrue(stored instanceof SymArrayStore);
        SymArrayStore store = (SymArrayStore) stored;
        assertSame(result, stored);
        assertEquals(new SymVariable("nums"), store.arr());
        assertEquals(new SymVariable("j"), store.index());
        assertTrue(store.value() instanceof SymArraySelect);
        SymArraySelect selected = (SymArraySelect) store.value();
        assertEquals(new SymVariable("nums"), selected.arr());
        assertEquals(new SymVariable("i"), selected.index());
    }

    @Test
    public void eval_shortPlusAssignDouble_castsBinaryResultBackToShort() {
        Assignment assignment = parseAssignment("x += 4.6;");
        MemoryModel memory = new MemoryModel();
        memory.write("x", SymLiteral.of((short) 3));
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(new TypeContext())
                .build();

        SymbolicValue result = handler.eval(assignment, state, createDispatcher());

        assertTrue(result instanceof SymCastOp);
        SymCastOp cast = (SymCastOp) result;
        assertEquals(TypeContext.SHORT, cast.type());
        assertTrue(cast.operand() instanceof SymBinaryOp);
        SymBinaryOp sum = (SymBinaryOp) cast.operand();
        assertEquals(SymLiteral.of((short) 3), sum.left());
        assertEquals(SymBinaryOp.Op.ADD, sum.op());
        assertEquals(SymLiteral.of(4.6), sum.right());
    }

    @Test
    public void eval_bytePlusAssignInt_castsBinaryResultBackToByte() {
        Assignment assignment = parseAssignment("b += 300;");
        MemoryModel memory = new MemoryModel();
        memory.write("b", SymLiteral.of((byte) 10));
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(new TypeContext())
                .build();

        SymbolicValue result = handler.eval(assignment, state, createDispatcher());

        assertTrue(result instanceof SymCastOp);
        SymCastOp cast = (SymCastOp) result;
        assertEquals(TypeContext.BYTE, cast.type());
        SymBinaryOp sum = (SymBinaryOp) cast.operand();
        assertEquals(SymLiteral.of((byte) 10), sum.left());
        assertEquals(SymLiteral.of(300), sum.right());
    }

    @Test
    public void eval_stringPlusAssignLiteral_buildsStringConcat() {
        Assignment assignment = parseAssignment("s += 1;");
        MemoryModel memory = new MemoryModel();
        memory.write("s", SymLiteral.of("v="));
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(new TypeContext())
                .build();

        SymbolicValue result = handler.eval(assignment, state, createDispatcher());

        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp concat = (SymBinaryOp) result;
        assertEquals(SymBinaryOp.Op.ADD, concat.op());
        assertEquals(SymLiteral.of("v="), concat.left());
        assertEquals(SymLiteral.of(1), concat.right());
    }

    @Test
    public void eval_arrayCompoundAssignment_evaluatesArrayAndIndexBeforeRhs() {
        Assignment assignment = parseAssignment("nums[j] += rhs;");
        org.eclipse.jdt.core.dom.ArrayAccess access =
                (org.eclipse.jdt.core.dom.ArrayAccess) assignment.getLeftHandSide();
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        when(dispatcher.eval(eq(access.getArray()), eq(state))).thenReturn(new SymVariable("nums"));
        when(dispatcher.eval(eq(access.getIndex()), eq(state))).thenReturn(new SymVariable("j"));
        when(dispatcher.eval(eq((Expression) assignment.getRightHandSide()), eq(state)))
                .thenReturn(SymLiteral.of(5));

        handler.eval(assignment, state, dispatcher);

        InOrder inOrder = inOrder(dispatcher);
        inOrder.verify(dispatcher).eval(eq(access.getArray()), eq(state));
        inOrder.verify(dispatcher).eval(eq(access.getIndex()), eq(state));
        inOrder.verify(dispatcher).eval(eq((Expression) assignment.getRightHandSide()), eq(state));
    }

    @Test
    public void eval_arrayCompoundAssignment_usesSelectedComponentAsLeftOperand() {
        Assignment assignment = parseAssignment("nums[j] += rhs;");
        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        SymbolicValue result = handler.eval(assignment, state, createDispatcher());

        SymArrayStore store = (SymArrayStore) result;
        assertTrue(store.value() instanceof SymBinaryOp);
        SymBinaryOp sum = (SymBinaryOp) store.value();
        assertTrue(sum.left() instanceof SymArraySelect);
        SymArraySelect selected = (SymArraySelect) sum.left();
        assertEquals(new SymVariable("nums"), selected.arr());
        assertEquals(new SymVariable("j"), selected.index());
        assertEquals(new SymVariable("rhs"), sum.right());
    }

    private Assignment parseAssignment(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        assertTrue(node instanceof ExpressionStatement);
        ExpressionStatement expressionStatement = (ExpressionStatement) node;
        assertTrue(expressionStatement.getExpression() instanceof Assignment);
        return (Assignment) expressionStatement.getExpression();
    }

    private AstDispatcher createDispatcher() {
        AstDispatcher dispatcher = new AstDispatcher();
        dispatcher.register(new AssignmentHandler());
        dispatcher.register(new ArrayAccessHandler());
        dispatcher.register(new SimpleNameHandler());
        dispatcher.register(new NumberLiteralHandler());
        return dispatcher;
    }
}
