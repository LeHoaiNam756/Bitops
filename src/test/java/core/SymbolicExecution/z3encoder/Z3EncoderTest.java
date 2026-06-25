package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymStringOp;
import core.SymbolicExecution.model.SymUnaryOp;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.types.ObjectSymType;
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

    @Test
    public void encodeAll_boolEquality_doesNotCoerceToBitVec() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of(
                    "a", ctx.getBoolSort(),
                    "b", ctx.getBoolSort()
            ));
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymBinaryOp constraint = new SymBinaryOp(
                    new SymVariable("a"),
                    SymBinaryOp.Op.EQ,
                    new SymVariable("b"));

            List<BoolExpr> encoded = encoder.encodeAll(List.of(constraint));

            assertEquals(1, encoded.size());
            assertEquals(ctx.getBoolSort(), encoded.get(0).getSort());
        }
    }

    @Test
    public void encodeAll_notOfBoolEquality_doesNotCoerceToBitVec() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of());
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymUnaryOp constraint = new SymUnaryOp(
                    SymUnaryOp.Op.NOT,
                    new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.EQ, SymLiteral.of(false)));

            List<BoolExpr> encoded = encoder.encodeAll(List.of(constraint));

            assertEquals(1, encoded.size());
            assertEquals(ctx.getBoolSort(), encoded.get(0).getSort());
        }
    }

    @Test
    public void encode_stringLiteral_usesZ3StringSort() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of());
            Z3Encoder encoder = new Z3Encoder(resolver);

            Expr<?> encoded = encoder.encode(SymLiteral.of("target"));

            assertEquals(ctx.getStringSort(), encoded.getSort());
        }
    }

    @Test
    public void encode_stringConcat_usesZ3StringSort() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of());
            Z3Encoder encoder = new Z3Encoder(resolver);
            SymBinaryOp concat = new SymBinaryOp(
                    SymLiteral.of("left"),
                    SymBinaryOp.Op.ADD,
                    SymLiteral.of("right"));

            Expr<?> encoded = encoder.encode(concat);

            assertEquals(ctx.getStringSort(), encoded.getSort());
        }
    }

    @Test
    public void constraintSolver_stringConcatWithPrimitiveLiteral_appliesStringConversion() {
        try (ConstraintSolver solver = ConstraintSolver.create(Map.of())) {
            SymBinaryOp concat = new SymBinaryOp(
                    SymLiteral.of("v="),
                    SymBinaryOp.Op.ADD,
                    SymLiteral.of(1));
            SymBinaryOp constraint = new SymBinaryOp(
                    concat,
                    SymBinaryOp.Op.EQ,
                    SymLiteral.of("v=1"));

            SolverResult result = solver.check(List.of(constraint));

            assertEquals(true, result.isSat());
        }
    }

    @Test
    public void constraintSolver_stringEquality_extractsStringModel() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("s", new ObjectSymType("java.lang.String")))) {
            SymBinaryOp constraint = new SymBinaryOp(
                    new SymVariable("s"),
                    SymBinaryOp.Op.EQ,
                    SymLiteral.of("target"));

            SolverResult result = solver.check(List.of(constraint));

            SolverResult.Sat sat = (SolverResult.Sat) result;
            assertEquals("target", sat.model().lookup("s").orElseThrow().value());
        }
    }

    @Test
    public void constraintSolver_stringContains_usesZ3StringTheory() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("s", new ObjectSymType("java.lang.String")))) {
            SymVariable s = new SymVariable("s");

            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(s, SymBinaryOp.Op.EQ, SymLiteral.of("target")),
                    new SymStringOp(s, SymStringOp.Op.CONTAINS, List.of(SymLiteral.of("arg")))
            ));

            assertEquals(true, result.isSat());
        }
    }

    @Test
    public void constraintSolver_stringLength_canCompareWithJavaIntLiteral() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("s", new ObjectSymType("java.lang.String")))) {
            SymVariable s = new SymVariable("s");
            SymStringOp length = new SymStringOp(s, SymStringOp.Op.LENGTH, List.of());

            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(s, SymBinaryOp.Op.EQ, SymLiteral.of("target")),
                    new SymBinaryOp(length, SymBinaryOp.Op.EQ, SymLiteral.of(6))
            ));

            assertEquals(true, result.isSat());
        }
    }

    @Test
    public void constraintSolver_stringSubstring_encodesExtract() {
        try (ConstraintSolver solver = ConstraintSolver.create(
                Map.of("s", new ObjectSymType("java.lang.String")))) {
            SymVariable s = new SymVariable("s");
            SymStringOp substring = new SymStringOp(
                    s,
                    SymStringOp.Op.SUBSTRING,
                    List.of(SymLiteral.of(0), SymLiteral.of(3)));

            SolverResult result = solver.check(List.of(
                    new SymBinaryOp(s, SymBinaryOp.Op.EQ, SymLiteral.of("target")),
                    new SymBinaryOp(substring, SymBinaryOp.Op.EQ, SymLiteral.of("tar"))
            ));

            assertEquals(true, result.isSat());
        }
    }
}
