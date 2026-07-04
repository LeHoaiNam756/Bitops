package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Z3ExpressionMetricsTest {

    @Test
    public void countCountsDistinctVariablesAndSharedExpressionNodes() {
        try (Context context = new Context()) {
            IntExpr x = context.mkIntConst("x");
            IntExpr y = context.mkIntConst("y");
            BoolExpr expression = context.mkAnd(
                    context.mkGt(x, context.mkInt(1)),
                    context.mkEq(y, context.mkAdd(x, context.mkInt(2))));

            Z3ExpressionMetrics.Counts counts =
                    Z3ExpressionMetrics.count(new BoolExpr[]{expression});

            assertEquals(2, counts.variableCount());
            assertEquals(8, counts.expressionCount());
        }
    }

    @Test
    public void countDoesNotTreatLiteralsAsVariables() {
        try (Context context = new Context()) {
            BoolExpr expression = context.mkEq(context.mkInt(1), context.mkInt(1));

            Z3ExpressionMetrics.Counts counts =
                    Z3ExpressionMetrics.count(new BoolExpr[]{expression});

            assertEquals(0, counts.variableCount());
            assertEquals(2, counts.expressionCount());
        }
    }
}
