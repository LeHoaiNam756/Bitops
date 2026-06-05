package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertNull;

public class AstDispatcherTest {
    @Test
    public void eval_unsupportedNode_returnsNull() {
        ASTNode node = Parser.parseStatementToAST("int x = 1;");
        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        assertNull(new AstDispatcher().eval(node, state));
    }

    @Test
    public void eval_nullNode_returnsNull() {
        assertNull(new AstDispatcher().eval(null, null));
    }
}
