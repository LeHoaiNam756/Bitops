package core.generation;

import core.cfg.Coverage;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;
import core.testdriver.TestDriver;
import core.testdriver.TestResult;
import core.testpath.AllPathsFinder;
import core.testpath.LoopCondensationFlowPathFinder;
import core.testpath.PathFinder;
import core.utils.FilePath;
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
    public void generate_bmpSetFindCodePointReachesFullCoverage() throws Exception {
        Path zip = createZipProject("BMPSet.java", """
                public class BMPSet {
                    int[] list = {0, 4, 110000};

                    int findCodePoint(int c, int lo, int hi) {
                        if (c < list[lo]) {
                            return lo;
                        }
                        if (lo >= hi || c >= list[hi - 1]) {
                            return hi;
                        }
                        for (;;) {
                            int i = (lo + hi) >>> 1;
                            if (i == lo) {
                                break;
                            } else if (c < list[i]) {
                                hi = i;
                            } else {
                                lo = i;
                            }
                        }
                        return hi;
                    }
                }
                """);

        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "findCodePoint".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();

        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("c", 0);
        seedInput.put("lo", 0);
        seedInput.put("hi", 0);

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.BRANCH,
                seedInput,
                new AllPathsFinder());

        assertFalse(result.testDataList().isEmpty());
        assertFalse(result.testDataList().stream().anyMatch(testData ->
                testData.output() != null
                        && testData.output().startsWith("EXCEPTION:")));
        assertEquals(8, result.fullCoverage().getCovered().size());
        assertEquals(0, result.fullCoverage().getUncovered().size());
        assertEquals(0, result.fullCoverage().getSkipped().size());
    }

    @Test
    public void generate_publicSolutionFromLeetCodeStyleFileKeepsMethodNamedResultJson() throws Exception {
        Path zip = createZipProject("0191-number-of-1-bits.java", """
                public class Solution {
                    public int hammingWeight(int n) {
                        int count = 0;
                        for (int i = 0; i < 32; i++) {
                            if ((n & 1) == 1) {
                                count++;
                            }
                            n = n >> 1;
                        }
                        return count;
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "hammingWeight".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("n", 1);

        AllPathsFinder allPathsFinder = new AllPathsFinder();
        allPathsFinder.setMAX_LOOP_ITERATIONS(32);
        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT,
                seedInput,
                allPathsFinder);

        assertFalse(result.testDataList().isEmpty());
        assertEquals("1", result.testDataList().get(0).output());
        assertTrue(Files.exists(Path.of(FilePath.PATH_TO_CLONED_PROJECT, "Solution.java")));
        assertTrue(Files.exists(Path.of(
                FilePath.PATH_TO_TOOL_OUTPUT,
                "concolic-results",
                "hammingWeight.json")));
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
    public void generate_tryCatchExplicitThrowCoversCatchReturn() throws Exception {
        Path zip = createZipProject("TryCatchUnit.java", """
                package sample;

                public class TryCatchUnit {
                    public static int classify(int x) {
                        try {
                            if (x < 0) {
                                throw new IllegalArgumentException();
                            }
                            return 1;
                        } catch (IllegalArgumentException e) {
                            return -1;
                        }
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "classify".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("x", 0);

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT,
                seedInput,
                new AllPathsFinder());

        assertFalse(result.testDataList().isEmpty());
        assertEquals("1", result.testDataList().get(0).output());
        assertTrue(result.testDataList().stream()
                .anyMatch(testData -> testData.input().get("x") instanceof Integer x
                        && x < 0
                        && "-1".equals(testData.output())));
        assertEquals(0, result.fullCoverage().getUncovered().size());
    }

    @Test
    public void generate_toCompactValueReachesFullBranchAndMcdcCoverage() throws Exception {
        Path zip = createZipProject("CompactValue.java", """
                public class CompactValue {
                    private static final long LONG_MASK = 0xffffffffL;
                    private static final long INFLATED = Long.MIN_VALUE;

                    public static long toCompactValue(int intLen, int sign, int[] mag) {
                        if (intLen == 0 || sign == 0)
                            return 0L;
                        int len = mag.length;
                        int d = mag[0];
                        if (len > 2 || (d < 0 && len == 2))
                            return INFLATED;
                        long v = (len == 2)
                            ? ((mag[1] & LONG_MASK) | (d & LONG_MASK) << 32)
                            : d & LONG_MASK;
                        return sign == -1 ? -v : v;
                    }
                }
                """);

        for (Coverage coverage : List.of(Coverage.BRANCH, Coverage.MCDC)) {
            Project project = new Project(zip);
            MethodDeclaration method = project.getMethods().stream()
                    .filter(m -> "toCompactValue".equals(m.getName().getIdentifier()))
                    .findFirst()
                    .orElseThrow();
            Map<String, Object> seedInput = new LinkedHashMap<>();
            seedInput.put("intLen", 1);
            seedInput.put("sign", 1);
            seedInput.put("mag", new int[]{1, 2, 3});

            var result = ConcolicTesting.getInstance().generate(
                    method,
                    project.getRootAST(method),
                    coverage,
                    seedInput,
                    new AllPathsFinder());

            assertEquals(coverage + " uncovered", 0,
                    result.fullCoverage().getUncovered().size());
            assertEquals(coverage + " skipped", 0,
                    result.fullCoverage().getSkipped().size());
            assertEquals(coverage + " covered outcomes",
                    coverage == Coverage.BRANCH ? 4 : 8,
                    result.fullCoverage().getCovered().size());
            assertFalse(coverage + " generated an exceptional input",
                    result.testDataList().stream().anyMatch(testData ->
                            testData.output() != null
                                    && testData.output().startsWith("EXCEPTION:")));
        }
    }

    @Test
    public void generate_powReachesFullMcdcCoverage() throws Exception {
        Path zip = createZipProject("LongMathUnit.java", """
                public class LongMathUnit {
                    public long pow(long b, int k) {
                        if (k < 0) {
                            throw new IllegalArgumentException("exponent (" + k + ") must be >= 0");
                        }
                        if (-2 <= b && b <= 2) {
                            switch ((int) b) {
                                case 0:
                                    return (k == 0) ? 1 : 0;
                                case 1:
                                    return 1;
                                case -1:
                                    return ((k & 1) == 0) ? 1 : -1;
                                case 2:
                                    return (k < Long.SIZE) ? 1L << k : 0;
                                case -2:
                                    if (k < Long.SIZE) {
                                        return ((k & 1) == 0) ? 1L << k : -(1L << k);
                                    } else {
                                        return 0;
                                    }
                                default:
                                    throw new AssertionError();
                            }
                        }
                        for (long accum = 1; ; k >>= 1) {
                            switch (k) {
                                case 0:
                                    return accum;
                                case 1:
                                    return accum * b;
                                default:
                                    accum *= ((k & 1) == 0) ? 1 : b;
                                    b *= b;
                            }
                        }
                    }
                }
                """);

        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "pow".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("b", 0L);
        seedInput.put("k", 0);

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.MCDC,
                seedInput,
                new AllPathsFinder());

        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUncovered().size());
        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUnknown().size());
        assertEquals(result.fullCoverage().getInfeasibleReasons().toString(), 0,
                result.fullCoverage().getInfeasible().size());
        assertEquals(6, result.fullCoverage().getCovered().size());
        assertTrue(result.testDataList().stream().anyMatch(test ->
                test.input().get("b") instanceof Long b && b > 2));
        assertTrue(result.testDataList().stream().anyMatch(test ->
                test.input().get("b") instanceof Long b && b < -2));
    }

    @Test
    public void generate_saturatedMultiplyReachesFullBranchCoverage() throws Exception {
        Path zip = createZipProject("LongMathUnit.java", """
                public class LongMathUnit {
                    public long saturatedMultiply(long a, long b) {
                        int leadingZeros =
                            Long.numberOfLeadingZeros(a)
                                + Long.numberOfLeadingZeros(~a)
                                + Long.numberOfLeadingZeros(b)
                                + Long.numberOfLeadingZeros(~b);
                        if (leadingZeros > Long.SIZE + 1) {
                            return a * b;
                        }
                        long limit = Long.MAX_VALUE + ((a ^ b) >>> (Long.SIZE - 1));
                        if (leadingZeros < Long.SIZE | (a < 0 & b == Long.MIN_VALUE)) {
                            return limit;
                        }
                        long result = a * b;
                        if (a == 0 || result / a == b) {
                            return result;
                        }
                        return limit;
                    }
                }
                """);

        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "saturatedMultiply".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.BRANCH,
                RandomTestInput.createBoundaryTestData(method),
                new AllPathsFinder());

        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUncovered().size());
        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUnknown().size());
        assertEquals(result.fullCoverage().getInfeasibleReasons().toString(), 0,
                result.fullCoverage().getInfeasible().size());
    }

    @Test
    public void generate_divWordCompletes() throws Exception {
        Path zip = createZipProject("DivWord.java", """
                public class DivWord {
                    private static final long LONG_MASK = 0xffffffffL;

                    static long divWord(long n, int d) {
                        long dLong = d & LONG_MASK;
                        long r;
                        long q;
                        if (dLong == 1) {
                            q = (int) n;
                            r = 0;
                            return (r << 32) | (q & LONG_MASK);
                        }
                        q = (n >>> 1) / (dLong >>> 1);
                        r = n - q * dLong;
                        while (r < 0) {
                            r += dLong;
                            q--;
                        }
                        while (r >= dLong) {
                            r -= dLong;
                            q++;
                        }
                        return (r << 32) | (q & LONG_MASK);
                    }
                }
                """);
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "divWord".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        List<Map<String, Object>> seedInputs = List.of(
                Map.of("n", 0L, "d", 1),
                Map.of("n", -1L, "d", -2),
                Map.of("n", -1L, "d", -1));

        for (Coverage coverage : Coverage.values()) {
            var result = ConcolicTesting.getInstance().generate(
                    method,
                    project.getRootAST(method),
                    coverage,
                    seedInputs,
                    new AllPathsFinder());

            assertTrue(coverage + " seed executions",
                    result.testDataList().size() >= seedInputs.size());
            assertTrue(coverage + " covered no obligations",
                    !result.fullCoverage().getCovered().isEmpty());
            assertTrue(coverage + " left obligations unclassified",
                    result.fullCoverage().getUncovered().isEmpty());
            if (coverage == Coverage.STATEMENT) {
                assertTrue(result.fullCoverage().getCovered().size() >= 11);
                assertTrue(result.fullCoverage().getUnknown().size()
                        + result.fullCoverage().getInfeasible().size() <= 2);
            }
        }
    }

    @Test
    public void comparePathFinders_tn1BenchmarkSeq() throws Exception {
        Path zip = createZipProject("Scholarship.java", """
                package sample;

                public class Scholarship {
                    public static int TN1_benchmarkSeq(
                            int examScore,
                            int projectScore,
                            int attendanceDays,
                            int applicationTier) {
                        int scholarshipPoints = 0;

                        if (applicationTier >= 1) {
                            if (examScore > 10) {
                                scholarshipPoints += 1;
                            }
                            if (projectScore > 20) {
                                scholarshipPoints += 2;
                            }
                            if (attendanceDays < 30) {
                                scholarshipPoints += 4;
                            }
                        } else {
                            if (examScore <= 10) {
                                scholarshipPoints -= 1;
                            }
                        }

                        if (examScore > 0 && projectScore > 0) {
                            if (attendanceDays > 0) {
                                scholarshipPoints += 10;
                            } else {
                                scholarshipPoints += 20;
                            }
                        } else if (examScore > 0) {
                            scholarshipPoints += 30;
                        }

                        return scholarshipPoints;
                    }
                }
                """);
        Map<String, Object> seedInput = new LinkedHashMap<>();
        seedInput.put("examScore", 0);
        seedInput.put("projectScore", 0);
        seedInput.put("attendanceDays", 0);
        seedInput.put("applicationTier", 0);

        TestResult oldResult = runTn1Benchmark(zip, seedInput, new AllPathsFinder());
        TestResult newResult = runTn1Benchmark(zip, seedInput, new LoopCondensationFlowPathFinder());

        System.out.printf(
                "TN1 AllPathsFinder: tests=%d covered=%d uncovered=%d skipped=%d memory=%d%n",
                oldResult.size(),
                oldResult.fullCoverage().getCovered().size(),
                oldResult.fullCoverage().getUncovered().size(),
                oldResult.fullCoverage().getSkipped().size(),
                oldResult.memoryUsageBytes());
        System.out.printf(
                "TN1 LoopCondensationFlowPathFinder: tests=%d covered=%d uncovered=%d skipped=%d memory=%d%n",
                newResult.size(),
                newResult.fullCoverage().getCovered().size(),
                newResult.fullCoverage().getUncovered().size(),
                newResult.fullCoverage().getSkipped().size(),
                newResult.memoryUsageBytes());

        assertEquals(oldResult.fullCoverage().getUncovered().size(),
                newResult.fullCoverage().getUncovered().size());
        assertTrue(newResult.size() <= oldResult.size());
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

    private TestResult runTn1Benchmark(
            Path zip,
            Map<String, Object> seedInput,
            PathFinder pathFinder) throws Exception {

        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "TN1_benchmarkSeq".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        return ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                Coverage.STATEMENT,
                seedInput,
                pathFinder);
    }
}
