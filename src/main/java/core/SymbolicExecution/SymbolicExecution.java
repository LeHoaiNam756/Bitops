package core.SymbolicExecution;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Model;
import core.SymbolicExecution.dispatch.AstDispatcher;
import core.SymbolicExecution.execution.ConstraintBuilder;
import core.SymbolicExecution.execution.ModelSolver;
import core.SymbolicExecution.execution.PathExecutor;
import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.TestGeneration.path.PathStep;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;

public class SymbolicExecution {
    public static boolean isRelatedToParameter;

    private final AstDispatcher dispatcher;
    private final SymbolicStore store;
    private final SymbolicContext ctx;

    public SymbolicExecution(AstDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.store = new SymbolicStore();
        this.ctx = new SymbolicContext();
    }

    public Model executePath(List<PathStep> path, List<ASTNode> parameters) {
        PathExecutor executor = new PathExecutor(dispatcher, store, ctx);
        List<BoolExpr> constraints = executor.execute(path);
        ConstraintBuilder builder = new ConstraintBuilder(ctx.z3());
        BoolExpr combined = builder.combine(constraints);
        ModelSolver solver = new ModelSolver(ctx.z3());
        return solver.solve(combined);
    }
}
