package core.generation;

import core.cfg.Coverage;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;
import core.testdriver.TestDriver;
import core.testpath.AllPathsFinder;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConcolicTestingTest {

    @Test
    public void generate_rangeBitwiseAnd() throws Exception {
        Path zip = createZipProject("BitOps.java", """
                package sample;

                public class BitOps {
                    public int rangeBitwiseAnd(int left, int right) {
                        int rightShiftCnt = 0;
                        while(left != right){
                            left = left >> 1;
                            right = right >> 1;
                            rightShiftCnt++;
                    }
                    int commonPref = (right << rightShiftCnt);
                    return commonPref;
                    }
                }

                """);

        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "rangeBitwiseAnd".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("left", 0);
        seedInput.put("right", 0);
        var result = ConcolicTesting.getInstance().generate(
                method, project.getRootAST(method), Coverage.STATEMENT,
                seedInput,
                new AllPathsFinder()
        );
    }

    @Test
    public void generate_hammingDistanceUsesZeroZeroRandomInput() throws Exception {
        Path zip = createZipProject("BitOps.java", """
                package sample;

                public class BitOps {
                    public static int hammingDistance(int x, int y) {
                        int count = 0;
                        for (int i = 0; i < 31; i++) {
                            if ((x & 1) != (y & 1)) {
                                count++;
                            }
                            x = x >> 1;
                            y = y >> 1;
                        }
                        return count;
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "hammingDistance".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("x", 0);
        seedInput.put("y", 0);

        AllPathsFinder allPathsFinder = new AllPathsFinder();
        allPathsFinder.setMAX_LOOP_ITERATIONS(32);
        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT,
                seedInput,
                allPathsFinder);

        assertFalse(result.testDataList().isEmpty());
        assertEquals(seedInput, result.testDataList().get(0).input());
        assertEquals("0", result.testDataList().get(0).output());
        assertEquals(0, result.fullCoverage().getUncovered().size());
        assertEquals(0, result.fullCoverage().getSkipped().size());
        assertEquals(7, result.fullCoverage().getCovered().size());
    }

    @Test
    public void generate_flipBitUsesCurrentScalarDriverParameters() throws Exception {
        Path zip = createZipProject("BitOps.java", """
                package sample;

                public class BitOps {
                    public static int flipBit(final int num, final int bit) {
                        return num ^ (1 << bit);
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "flipBit".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT);

        assertFalse(result.testDataList().isEmpty());
        Map<String, Object> input = result.testDataList().get(0).input();
        assertTrue(input.containsKey("num"));
        assertTrue(input.containsKey("bit"));
        assertFalse(input.containsKey("nums"));
    }

    @Test
    public void generate_stringParameterCoversStringEqualityBranch() throws Exception {
        Path zip = createZipProject("StringUnit.java", """
                package sample;

                public class StringUnit {
                    public static int roleScore(String role) {
                        if (role.equals("admin")) {
                            return 10;
                        }
                        return 1;
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "roleScore".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("role", "guest");

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT,
                seedInput,
                new AllPathsFinder());

        assertFalse(result.testDataList().isEmpty());
        assertEquals(seedInput, result.testDataList().get(0).input());
        assertEquals("1", result.testDataList().get(0).output());
        assertTrue(result.testDataList().stream()
                .anyMatch(testData -> "admin".equals(testData.input().get("role"))
                        && "10".equals(testData.output())));
        assertEquals(0, result.fullCoverage().getUncovered().size());
    }

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
        assertTrue(inputs.get("missingUnsupported") instanceof String[]);
        assertArrayEquals(new String[0], (String[]) inputs.get("missingUnsupported"));
    }

    @Test
    public void extractInputsFromModel_defaultsMissingStringToNonNullValue() throws Exception {
        Z3ModelBindings bindings = Z3ModelBindings.empty();
        List<TestDriver.ParamInfo> params = List.of(
                new TestDriver.ParamInfo("ip", "String"),
                new TestDriver.ParamInfo("qualified", "java.lang.String")
        );

        Map<String, Object> inputs = extractInputsFromModel(bindings, params);

        assertEquals("1.2.3.4", inputs.get("ip"));
        assertEquals("1.2.3.4", inputs.get("qualified"));
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

    private static Path createZipProject(String fileName, String source) throws Exception {
        Path zip = Files.createTempFile("ct4j-flipbit-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(fileName));
            zos.write(source.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        zip.toFile().deleteOnExit();
        return zip;
    }
}
