package core.SymbolicExecution.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;
import org.junit.Test;

import static org.junit.Assert.*;

public class SymbolicValueTest {
    @Test
    public void createsPrimitiveValue() {
        try (Context ctx = new Context()) {
            Expr<?> expr = ctx.mkBVConst("x", 32);
            SymbolicValue value = SymbolicValue.of("x", TypedExpr.JavaType.INT, expr, true);
            assertEquals("x", value.getName());
            assertEquals(TypedExpr.JavaType.INT, value.getType());
            assertSame(expr, value.getExpr());
            assertTrue(value.isParameter());
        }
    }

    @Test
    public void createsArrayValue() {
        try (Context ctx = new Context()) {
            Expr<?> expr = ctx.mkArrayConst("arr", ctx.getIntSort(), ctx.getIntSort());
            ArraySymbolicValue value = ArraySymbolicValue.of("arr", TypedExpr.JavaType.INT, expr, false, 1);
            assertEquals("arr", value.getName());
            assertEquals(1, value.getDimensions());
            assertEquals(TypedExpr.JavaType.INT, value.getElementType());
        }
    }
}
