package core.SymbolicExecution.execution;

import com.microsoft.z3.BoolExpr;
import core.SymbolicExecution.dispatch.AstDispatcher;
import core.SymbolicExecution.model.SymbolicStore;
import core.TestGeneration.path.PathStep;

import java.util.ArrayList;
import java.util.List;

public class PathExecutor {
    private final AstDispatcher dispatcher;
    private final SymbolicStore store;
    private final SymbolicContext ctx;

    public PathExecutor(AstDispatcher dispatcher, SymbolicStore store, SymbolicContext ctx) {
        this.dispatcher = dispatcher;
        this.store = store;
        this.ctx = ctx;
    }

    public List<BoolExpr> execute(List<PathStep> path) {
        List<BoolExpr> constraints = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return constraints;
        }
        for (PathStep step : path) {
            // TODO: resolve AST for nodeId (will be done via ConcolicTesting lookup)
        }
        return constraints;
    }
}
