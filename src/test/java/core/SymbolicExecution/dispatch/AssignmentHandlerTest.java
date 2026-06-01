package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
        return dispatcher;
    }
}
