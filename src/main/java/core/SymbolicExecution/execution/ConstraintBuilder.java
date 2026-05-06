package core.SymbolicExecution.execution;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.List;

public class ConstraintBuilder {
    private final Context ctx;

    public ConstraintBuilder(Context ctx) {
        this.ctx = ctx;
    }

    public BoolExpr combine(List<BoolExpr> constraints) {
        if (constraints.isEmpty()) {
            return ctx.mkTrue();
        }
        BoolExpr result = constraints.get(0);
        for (int i = 1; i < constraints.size(); i++) {
            result = ctx.mkAnd(result, constraints.get(i));
        }
        return result;
    }
}
