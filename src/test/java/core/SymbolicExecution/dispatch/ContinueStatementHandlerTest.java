package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ContinueStatementHandlerTest {

    private final ContinueStatementHandler handler = new ContinueStatementHandler();

    @Test
    public void supportsReturnsTrueForContinueStatement() {
        ContinueStatement statement = parseContinueStatement();

        assertTrue(handler.supports(statement));
    }

    @Test
    public void supportsReturnsFalseForOtherNodeTypes() {
        ASTNode node = Parser.parseStatementToAST("int x = 1;");

        assertFalse(handler.supports(node));
    }

    @Test
    public void evalReturnsNull() {
        ContinueStatement statement = parseContinueStatement();
        SymbolicState state = SymbolicState.builder().build();
        AstDispatcher dispatcher = new AstDispatcher();

        SymbolicValue result = handler.eval(statement, state, dispatcher);

        assertNull(result);
    }

    private ContinueStatement parseContinueStatement() {
        ASTNode node = Parser.parseStatementToAST("continue;");
        assertTrue(node instanceof ContinueStatement);
        return (ContinueStatement) node;
    }
}
