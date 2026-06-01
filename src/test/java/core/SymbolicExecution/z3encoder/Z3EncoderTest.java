package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Z3EncoderTest {

    @Test
    public void encodeAll_eqBetweenBitVecArraySelectAndIntLiteral_encodesBoolConstraint() {
        try (Context ctx = new Context(Map.of())) {
            Sort dpSort = ctx.mkArraySort(ctx.getIntSort(), ctx.mkBitVecSort(32));
            SortResolver resolver = new SortResolver(ctx, Map.of(
                    "dp", dpSort,
                    "mask", ctx.getIntSort()
            ));
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymArraySelect selected = new SymArraySelect(new SymVariable("dp"), new SymVariable("mask"));
            SymBinaryOp constraint = new SymBinaryOp(selected, SymBinaryOp.Op.EQ, SymLiteral.of(-1));

            List<BoolExpr> encoded = encoder.encodeAll(List.of(constraint));

            assertEquals(1, encoded.size());
            assertEquals(ctx.getBoolSort(), encoded.get(0).getSort());
        }
    }

    @Test
    public void encodeAll_bitwiseConstraint_switchesNestedArithmeticToBitVec() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of("x", ctx.getIntSort()));
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymBinaryOp add = new SymBinaryOp(new SymVariable("x"), SymBinaryOp.Op.ADD, SymLiteral.of(1));
            SymBinaryOp masked = new SymBinaryOp(add, SymBinaryOp.Op.BAND, SymLiteral.of(3));
            SymBinaryOp constraint = new SymBinaryOp(masked, SymBinaryOp.Op.EQ, SymLiteral.of(0));

            List<BoolExpr> encoded = encoder.encodeAll(List.of(constraint));

            assertEquals(1, encoded.size());
            assertEquals(ctx.getBoolSort(), encoded.get(0).getSort());
        }
    }

    @Test
    public void encodeAll_bitwiseConstraint_switchesComparisonOperandsToBitVec() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of(
                    "x", ctx.getIntSort(),
                    "y", ctx.getIntSort()
            ));
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymBinaryOp masked = new SymBinaryOp(new SymVariable("x"), SymBinaryOp.Op.BAND, SymLiteral.of(255));
            SymBinaryOp constraint = new SymBinaryOp(masked, SymBinaryOp.Op.SLT, new SymVariable("y"));

            List<BoolExpr> encoded = encoder.encodeAll(List.of(constraint));

            assertEquals(1, encoded.size());
            assertEquals(ctx.getBoolSort(), encoded.get(0).getSort());
        }
    }
}
