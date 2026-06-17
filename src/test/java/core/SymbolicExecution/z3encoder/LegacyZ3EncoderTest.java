package core.SymbolicExecution.z3encoder;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyZ3EncoderTest {

    @Test
    public void constraintSolver_legacyIntArithmetic_extractsIntegerModel() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("x", PrimitiveSymType.INT),
                Z3EncodingMode.LEGACY_INT_REAL)) {
            SymVariable x = new SymVariable("x");
            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(
                            new SymBinaryOp(x, SymBinaryOp.Op.ADD, SymLiteral.of(1)),
                            SymBinaryOp.Op.SGT,
                            SymLiteral.of(10))
            ));

            assertTrue(result.isSat());
            Object value = ((SolverResult.Sat) result).model().lookup("x").orElseThrow().value();
            assertTrue(value instanceof Integer);
            assertTrue((Integer) value > 9);
        }
    }

    @Test
    public void constraintSolver_legacyRealArithmetic_extractsDoubleModel() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("d", PrimitiveSymType.DOUBLE),
                Z3EncodingMode.LEGACY_INT_REAL)) {
            SymVariable d = new SymVariable("d");
            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(
                            new SymBinaryOp(d, SymBinaryOp.Op.DIV, SymLiteral.of(2.0)),
                            SymBinaryOp.Op.SLT,
                            SymLiteral.of(3.5))
            ));

            assertTrue(result.isSat());
            Object value = ((SolverResult.Sat) result).model().lookup("d").orElseThrow().value();
            assertTrue(value instanceof Double);
            assertTrue((Double) value < 7.0);
        }
    }

    @Test
    public void constraintSolver_legacyBooleanAndNumericCondition_extractsModel() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of(
                        "flag", PrimitiveSymType.BOOLEAN,
                        "x", PrimitiveSymType.INT),
                Z3EncodingMode.LEGACY_INT_REAL)) {
            SymVariable flag = new SymVariable("flag");
            SymVariable x = new SymVariable("x");
            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(
                            flag,
                            SymBinaryOp.Op.AND,
                            new SymBinaryOp(x, SymBinaryOp.Op.SGT, SymLiteral.of(0)))
            ));

            assertTrue(result.isSat());
            Z3ModelBindings model = ((SolverResult.Sat) result).model();
            assertEquals(true, model.lookup("flag").orElseThrow().value());
            assertTrue((Integer) model.lookup("x").orElseThrow().value() > 0);
        }
    }

    @Test
    public void constraintSolver_legacyBitwise_returnsUnknownInsteadOfUsingBitVector() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("x", PrimitiveSymType.INT),
                Z3EncodingMode.LEGACY_INT_REAL)) {
            SymVariable x = new SymVariable("x");
            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(
                            new SymBinaryOp(x, SymBinaryOp.Op.BAND, SymLiteral.of(1)),
                            SymBinaryOp.Op.EQ,
                            SymLiteral.of(1))
            ));

            assertTrue(result.isUnknown());
            String reason = ((SolverResult.Unknown) result).reason();
            assertTrue(reason.contains("bitwise or shift"));
        }
    }
}
