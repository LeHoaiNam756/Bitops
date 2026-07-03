package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.SymLiteral;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelExtractorTest {

    @Test
    public void extract_readsSignedLongFromUnsignedBv64Numeral() {
        try (Context ctx = new Context(Map.of())) {
            BitVecExpr value = ctx.mkBVConst("value", 64);
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(value, ctx.mkBV(-1L, 64)));

            assertEquals(Status.SATISFIABLE, solver.check());
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            SymLiteral literal = extractor.extract(solver.getModel())
                    .lookup("value")
                    .orElseThrow();

            assertEquals(-1L, literal.value());
        }
    }

    @Test
    public void extract_readsSignedLongArrayFromUnsignedBv64Numerals() {
        try (Context ctx = new Context(Map.of())) {
            BitVecSort bv64 = ctx.mkBitVecSort(64);
            ArrayExpr<IntSort, BitVecSort> values =
                    ctx.mkArrayConst("values", ctx.getIntSort(), bv64);
            IntExpr length = ctx.mkIntConst("values__length");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(2)));
            solver.add(ctx.mkEq(ctx.mkSelect(values, ctx.mkInt(0)), ctx.mkBV(Long.MIN_VALUE, 64)));
            solver.add(ctx.mkEq(ctx.mkSelect(values, ctx.mkInt(1)), ctx.mkBV(-1L, 64)));

            assertEquals(Status.SATISFIABLE, solver.check());
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            SymLiteral literal = extractor.extract(solver.getModel())
                    .lookup("values")
                    .orElseThrow();

            assertTrue(literal.value() instanceof long[]);
            assertArrayEquals(new long[] {Long.MIN_VALUE, -1L}, (long[]) literal.value());
        }
    }

    @Test
    public void extract_readsIntArrayElementsUsingLengthBinding() {
        try (Context ctx = new Context(Map.of())) {
            ArrayExpr<IntSort, IntSort> nums = ctx.mkArrayConst("nums", ctx.getIntSort(), ctx.getIntSort());
            IntExpr length = ctx.mkIntConst("nums__length");
            IntExpr val = ctx.mkIntConst("val");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(1)));
            solver.add(ctx.mkEq(val, ctx.mkInt(5)));
            solver.add(ctx.mkEq(ctx.mkSelect(nums, ctx.mkInt(0)), ctx.mkInt(7)));

            assertEquals(Status.SATISFIABLE, solver.check());
            Model model = solver.getModel();
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            Z3ModelBindings bindings = extractor.extract(model);

            SymLiteral literal = bindings.lookup("nums").orElseThrow();
            assertTrue(literal.value() instanceof int[]);
            assertArrayEquals(new int[] {7}, (int[]) literal.value());
        }
    }

    @Test
    public void extract_readsBooleanArrayElementsUsingLengthBinding() {
        try (Context ctx = new Context(Map.of())) {
            ArrayExpr<IntSort, BoolSort> flags = ctx.mkArrayConst("flags", ctx.getIntSort(), ctx.getBoolSort());
            IntExpr length = ctx.mkIntConst("flags__length");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(2)));
            solver.add(ctx.mkEq(ctx.mkSelect(flags, ctx.mkInt(0)), ctx.mkTrue()));
            solver.add(ctx.mkEq(ctx.mkSelect(flags, ctx.mkInt(1)), ctx.mkFalse()));

            assertEquals(Status.SATISFIABLE, solver.check());
            Model model = solver.getModel();
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            Z3ModelBindings bindings = extractor.extract(model);

            SymLiteral literal = bindings.lookup("flags").orElseThrow();
            assertTrue(literal.value() instanceof boolean[]);
            assertArrayEquals(new boolean[] {true, false}, (boolean[]) literal.value());
        }
    }

    @Test
    public void extract_readsFloatArrayElementsUsingLengthBinding() {
        try (Context ctx = new Context(Map.of())) {
            FPSort fp32 = ctx.mkFPSort32();
            ArrayExpr<IntSort, FPSort> floats = ctx.mkArrayConst("floats", ctx.getIntSort(), fp32);
            IntExpr length = ctx.mkIntConst("floats__length");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(2)));
            solver.add(ctx.mkEq(ctx.mkSelect(floats, ctx.mkInt(0)), ctx.mkFP(1.5f, fp32)));
            solver.add(ctx.mkEq(ctx.mkSelect(floats, ctx.mkInt(1)), ctx.mkFP(-2.25f, fp32)));

            assertEquals(Status.SATISFIABLE, solver.check());
            Model model = solver.getModel();
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            Z3ModelBindings bindings = extractor.extract(model);

            SymLiteral literal = bindings.lookup("floats").orElseThrow();
            assertTrue(literal.value() instanceof float[]);
            assertArrayEquals(new float[] {1.5f, -2.25f}, (float[]) literal.value(), 0.0f);
        }
    }

    @Test
    public void extract_readsDoubleArrayElementsUsingLengthBinding() {
        try (Context ctx = new Context(Map.of())) {
            FPSort fp64 = ctx.mkFPSort64();
            ArrayExpr<IntSort, FPSort> doubles = ctx.mkArrayConst("doubles", ctx.getIntSort(), fp64);
            IntExpr length = ctx.mkIntConst("doubles__length");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(2)));
            solver.add(ctx.mkEq(ctx.mkSelect(doubles, ctx.mkInt(0)), ctx.mkFP(1.5, fp64)));
            solver.add(ctx.mkEq(ctx.mkSelect(doubles, ctx.mkInt(1)), ctx.mkFP(-2.25, fp64)));

            assertEquals(Status.SATISFIABLE, solver.check());
            Model model = solver.getModel();
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            Z3ModelBindings bindings = extractor.extract(model);

            SymLiteral literal = bindings.lookup("doubles").orElseThrow();
            assertTrue(literal.value() instanceof double[]);
            assertArrayEquals(new double[] {1.5, -2.25}, (double[]) literal.value(), 0.0);
        }
    }

    @Test
    public void extract_skipsIntArrayWhenElementExceedsLongRange() {
        try (Context ctx = new Context(Map.of())) {
            ArrayExpr<IntSort, IntSort> nums = ctx.mkArrayConst("nums", ctx.getIntSort(), ctx.getIntSort());
            IntExpr length = ctx.mkIntConst("nums__length");
            var solver = ctx.mkSolver();
            solver.add(ctx.mkEq(length, ctx.mkInt(1)));
            solver.add(ctx.mkEq(ctx.mkSelect(nums, ctx.mkInt(0)), ctx.mkInt("9223372036854775808")));

            assertEquals(Status.SATISFIABLE, solver.check());
            ModelExtractor extractor = new ModelExtractor(new SortResolver(ctx, Map.of()));

            Z3ModelBindings bindings = extractor.extract(solver.getModel());

            assertFalse(bindings.lookup("nums").isPresent());
        }
    }
}
