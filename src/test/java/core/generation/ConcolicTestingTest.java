package core.generation;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;
import core.testdriver.TestDriver;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConcolicTestingTest {

    @Test
    public void extractInputsFromModel_usesExactPrimitiveArrayFallbacks() throws Exception {
        Z3ModelBindings bindings = new Z3ModelBindings(Map.of(
                "ints__length", SymLiteral.of(1),
                "longs__length", SymLiteral.of(2),
                "shorts__length", SymLiteral.of(3),
                "bytes__length", SymLiteral.of(4),
                "chars__length", SymLiteral.of(5),
                "bools__length", SymLiteral.of(6),
                "floats__length", SymLiteral.of(7),
                "doubles__length", SymLiteral.of(8),
                "val", SymLiteral.of(5)
        ));
        List<TestDriver.ParamInfo> params = List.of(
                new TestDriver.ParamInfo("ints", "int[]"),
                new TestDriver.ParamInfo("longs", "long[]"),
                new TestDriver.ParamInfo("shorts", "short[]"),
                new TestDriver.ParamInfo("bytes", "byte[]"),
                new TestDriver.ParamInfo("chars", "char[]"),
                new TestDriver.ParamInfo("bools", "boolean[]"),
                new TestDriver.ParamInfo("floats", "float[]"),
                new TestDriver.ParamInfo("doubles", "double[]"),
                new TestDriver.ParamInfo("val", "int")
        );

        Map<String, Object> inputs = extractInputsFromModel(bindings, params);

        assertArrayEquals(new int[] {0}, (int[]) inputs.get("ints"));
        assertArrayEquals(new long[] {0L, 0L}, (long[]) inputs.get("longs"));
        assertArrayEquals(new short[] {0, 0, 0}, (short[]) inputs.get("shorts"));
        assertArrayEquals(new byte[] {0, 0, 0, 0}, (byte[]) inputs.get("bytes"));
        assertArrayEquals(new char[] {'\0', '\0', '\0', '\0', '\0'}, (char[]) inputs.get("chars"));
        assertArrayEquals(new boolean[] {false, false, false, false, false, false}, (boolean[]) inputs.get("bools"));
        assertArrayEquals(new float[] {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f}, (float[]) inputs.get("floats"), 0.0f);
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, (double[]) inputs.get("doubles"), 0.0);
        assertEquals(5, inputs.get("val"));
    }

    @Test
    public void extractInputsFromModel_coercesNeutralNumericArraysToDeclaredTypes() throws Exception {
        Z3ModelBindings bindings = new Z3ModelBindings(Map.of(
                "ints", new SymLiteral(new long[] {1L, 2L}),
                "longs", new SymLiteral(new long[] {3L, 4L}),
                "shorts", new SymLiteral(new long[] {5L, 6L}),
                "bytes", new SymLiteral(new long[] {7L, 8L}),
                "chars", new SymLiteral(new long[] {65L, 66L}),
                "floats", new SymLiteral(new double[] {1.25, 2.5})
        ));
        List<TestDriver.ParamInfo> params = List.of(
                new TestDriver.ParamInfo("ints", "int[]"),
                new TestDriver.ParamInfo("longs", "long[]"),
                new TestDriver.ParamInfo("shorts", "short[]"),
                new TestDriver.ParamInfo("bytes", "byte[]"),
                new TestDriver.ParamInfo("chars", "char[]"),
                new TestDriver.ParamInfo("floats", "float[]")
        );

        Map<String, Object> inputs = extractInputsFromModel(bindings, params);

        assertArrayEquals(new int[] {1, 2}, (int[]) inputs.get("ints"));
        assertArrayEquals(new long[] {3L, 4L}, (long[]) inputs.get("longs"));
        assertArrayEquals(new short[] {5, 6}, (short[]) inputs.get("shorts"));
        assertArrayEquals(new byte[] {7, 8}, (byte[]) inputs.get("bytes"));
        assertArrayEquals(new char[] {'A', 'B'}, (char[]) inputs.get("chars"));
        assertArrayEquals(new float[] {1.25f, 2.5f}, (float[]) inputs.get("floats"), 0.0f);
    }

    @Test
    public void extractInputsFromModel_keepsScalarBindingsUnchanged() throws Exception {
        Z3ModelBindings bindings = new Z3ModelBindings(Map.of(
                "flag", SymLiteral.of(true),
                "count", SymLiteral.of(11),
                "unsupported", new SymLiteral(new String[] {"x"})
        ));
        List<TestDriver.ParamInfo> params = List.of(
                new TestDriver.ParamInfo("flag", "boolean"),
                new TestDriver.ParamInfo("count", "int"),
                new TestDriver.ParamInfo("unsupported", "String[]"),
                new TestDriver.ParamInfo("missingUnsupported", "String[]")
        );

        Map<String, Object> inputs = extractInputsFromModel(bindings, params);

        assertEquals(true, inputs.get("flag"));
        assertEquals(11, inputs.get("count"));
        assertArrayEquals(new String[] {"x"}, (String[]) inputs.get("unsupported"));
        assertTrue(inputs.get("missingUnsupported") instanceof int[]);
        assertArrayEquals(new int[0], (int[]) inputs.get("missingUnsupported"));
    }

    @Test
    public void extractInputsFromModel_defaultsInvalidArrayLengthToZero() throws Exception {
        Z3ModelBindings bindings = new Z3ModelBindings(Map.of(
                "ints__length", SymLiteral.of(-1),
                "longs__length", new SymLiteral("bad")
        ));
        List<TestDriver.ParamInfo> params = List.of(
                new TestDriver.ParamInfo("ints", "int[]"),
                new TestDriver.ParamInfo("longs", "long[]"),
                new TestDriver.ParamInfo("missing", "double[]")
        );

        Map<String, Object> inputs = extractInputsFromModel(bindings, params);

        assertArrayEquals(new int[0], (int[]) inputs.get("ints"));
        assertArrayEquals(new long[0], (long[]) inputs.get("longs"));
        assertArrayEquals(new double[0], (double[]) inputs.get("missing"), 0.0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractInputsFromModel(
            Z3ModelBindings bindings,
            List<TestDriver.ParamInfo> params) throws Exception {
        var method = ConcolicTesting.class.getDeclaredMethod(
                "extractInputsFromModel",
                Z3ModelBindings.class,
                List.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(null, bindings, params);
    }
}
