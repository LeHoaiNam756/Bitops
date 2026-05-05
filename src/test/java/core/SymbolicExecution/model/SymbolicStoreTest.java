package core.SymbolicExecution.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;
import org.junit.Test;

import static org.junit.Assert.*;

public class SymbolicStoreTest {
    @Test
    public void declareAssignResolve() {
        try (Context ctx = new Context()) {
            SymbolicStore store = new SymbolicStore();
            Expr<?> expr = ctx.mkBVConst("x", 32);
            SymbolicValue value = SymbolicValue.of("x", TypedExpr.JavaType.INT, expr, true);
            store.declare("x", value);
            assertTrue(store.contains("x"));
            assertSame(value, store.resolve("x"));

            Expr<?> expr2 = ctx.mkBVConst("x2", 32);
            SymbolicValue value2 = SymbolicValue.of("x", TypedExpr.JavaType.INT, expr2, false);
            store.assign("x", value2);
            assertSame(value2, store.resolve("x"));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void assignWithoutDeclareThrows() {
        SymbolicStore store = new SymbolicStore();
        store.assign("x", null);
    }
}
