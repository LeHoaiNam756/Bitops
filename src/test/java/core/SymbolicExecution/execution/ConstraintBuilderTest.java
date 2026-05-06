package core.SymbolicExecution.execution;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.TypedExpr;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ConstraintBuilderTest {
    @Test
    public void combinesConstraints() {
        try (Context ctx = new Context()) {
            BoolExpr a = ctx.mkBoolConst("a");
            BoolExpr b = ctx.mkBoolConst("b");
            ConstraintBuilder builder = new ConstraintBuilder(ctx);
            BoolExpr combined = builder.combine(Arrays.asList(a, b));
            assertTrue(combined.toString().contains("a"));
            assertTrue(combined.toString().contains("b"));
        }
    }
}
