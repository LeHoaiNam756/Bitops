package core.SymbolicExecution.z3encoder;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.types.ArraySymType;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.utils.ConcolicLimits;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ConstraintSolverArrayLengthLimitTest {

    @Test
    public void check_rejectsPathRequiringOversizedArray() {
        var arrayType = new ArraySymType(PrimitiveSymType.BYTE, 1);
        var bytes = new SymVariable("bytes");
        var length = new SymFieldAccess(bytes, "length", PrimitiveSymType.INT);
        var tooLarge = new SymBinaryOp(
                length,
                SymBinaryOp.Op.SGT,
                SymLiteral.of(ConcolicLimits.maxGeneratedArrayLength()));

        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("bytes", arrayType), Z3EncodingMode.BITVECTOR)) {
            assertTrue(solver.check(List.of(tooLarge)) instanceof SolverResult.Unsat);
        }
    }

    @Test
    public void check_materializesArrayWithinConfiguredLimit() {
        var arrayType = new ArraySymType(PrimitiveSymType.BYTE, 1);
        var bytes = new SymVariable("bytes");
        var length = new SymFieldAccess(bytes, "length", PrimitiveSymType.INT);
        var positive = new SymBinaryOp(length, SymBinaryOp.Op.SGT, SymLiteral.of(0));

        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("bytes", arrayType), Z3EncodingMode.BITVECTOR)) {
            SolverResult result = solver.check(List.of(positive));
            assertTrue(result instanceof SolverResult.Sat);
            SolverResult.Sat sat = (SolverResult.Sat) result;
            int modelLength = ((Number) sat.model().lookup("bytes__length")
                    .orElseThrow().value()).intValue();
            assertTrue(modelLength > 0);
            assertTrue(modelLength <= ConcolicLimits.maxGeneratedArrayLength());
        }
    }

    @Test
    public void reset_preservesArrayLengthLimit() {
        var arrayType = new ArraySymType(PrimitiveSymType.BYTE, 1);
        var length = new SymFieldAccess(
                new SymVariable("bytes"), "length", PrimitiveSymType.INT);
        var tooLarge = new SymBinaryOp(
                length,
                SymBinaryOp.Op.SGT,
                SymLiteral.of(ConcolicLimits.maxGeneratedArrayLength()));

        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("bytes", arrayType), Z3EncodingMode.BITVECTOR)) {
            solver.reset();
            assertTrue(solver.check(List.of(tooLarge)) instanceof SolverResult.Unsat);
        }
    }
}
