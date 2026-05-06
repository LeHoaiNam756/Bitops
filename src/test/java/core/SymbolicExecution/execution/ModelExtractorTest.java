package core.SymbolicExecution.execution;

import com.microsoft.z3.*;
import core.SymbolicExecution.TypedExpr;
import core.SymbolicExecution.model.SymbolicValue;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModelExtractorTest {
    @Test
    public void extractsIntValue() {
        try (Context ctx = new Context()) {
            Solver solver = ctx.mkSolver();
            Expr<?> x = ctx.mkBVConst("x", 32);
            solver.add(ctx.mkEq(x, ctx.mkBV(5, 32)));
            assertEquals(Status.SATISFIABLE, solver.check());
            Model model = solver.getModel();

            ModelExtractor extractor = new ModelExtractor(ctx);
            Object result = extractor.extractPrimitive(model, x, int.class);
            assertEquals(5, result);
        }
    }
}
