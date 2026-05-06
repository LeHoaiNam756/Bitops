package core.SymbolicExecution.execution;

import com.microsoft.z3.BoolExpr;
import core.SymbolicExecution.dispatch.AstDispatcher;
import core.SymbolicExecution.model.SymbolicStore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class PathExecutorTest {
    @Test
    public void returnsEmptyConstraintsForEmptyPath() {
        AstDispatcher dispatcher = new AstDispatcher();
        SymbolicStore store = new SymbolicStore();
        try (SymbolicContext ctx = new SymbolicContext()) {
            PathExecutor executor = new PathExecutor(dispatcher, store, ctx);
            List<BoolExpr> constraints = executor.execute(Collections.emptyList());
            assertTrue(constraints.isEmpty());
        }
    }
}
