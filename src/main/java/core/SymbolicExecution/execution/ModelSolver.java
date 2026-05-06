package core.SymbolicExecution.execution;

import com.microsoft.z3.*;

public class ModelSolver {
    private final Context ctx;

    public ModelSolver(Context ctx) {
        this.ctx = ctx;
    }

    public Model solve(BoolExpr constraint) {
        Solver solver = ctx.mkSolver();
        solver.add(constraint);
        Status status = solver.check();
        if (status == Status.SATISFIABLE) {
            return solver.getModel();
        }
        if (status == Status.UNSATISFIABLE) {
            throw new IllegalStateException("Constraints are UNSAT");
        }
        throw new IllegalStateException("Solver returned UNKNOWN");
    }
}
