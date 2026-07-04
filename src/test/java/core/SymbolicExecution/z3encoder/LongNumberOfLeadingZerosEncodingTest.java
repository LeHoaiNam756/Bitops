package core.SymbolicExecution.z3encoder;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymUnaryOp;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class LongNumberOfLeadingZerosEncodingTest {

    @Test
    public void encodesExactLongSemanticsAtRepresentativeBitBoundaries() {
        for (long value : new long[]{
                0L, 1L, 2L, 1L << 31, 1L << 32,
                Long.MAX_VALUE, Long.MIN_VALUE, -1L}) {
            SymVariable x = new SymVariable("x");
            SymbolicValue leadingZeros = new SymUnaryOp(
                    SymUnaryOp.Op.LONG_NUMBER_OF_LEADING_ZEROS, x);
            List<SymbolicValue> constraints = List.of(
                    new SymBinaryOp(x, SymBinaryOp.Op.EQ, SymLiteral.of(value)),
                    new SymBinaryOp(
                            leadingZeros,
                            SymBinaryOp.Op.EQ,
                            SymLiteral.of(Long.numberOfLeadingZeros(value))));

            try (ConstraintSolver solver = ConstraintSolver.create(
                    Map.of("x", PrimitiveSymType.LONG))) {
                assertTrue("value=" + value, solver.check(constraints).isSat());
            }
        }
    }
}
