package core.generation;

import core.cfg.CfgBuilder;
import core.cfg.ControlFlowGraph;
import core.cfg.Coverage;
import core.instrument.*;
import core.symbolic.SymbolicExecution;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;
import core.SymbolicExecution.z3encoder.Z3StatisticsRecorder;
import core.testdriver.TestData;
import core.testdriver.TestDriver;
import core.testdriver.TestResult;
import core.testpath.AllPathsFinder;
import core.testpath.CoverageTracker;
import core.testpath.PathFinder;
import core.testpath.TraceReader;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConcolicTesting {
    private static final ConcolicTesting INSTANCE = new ConcolicTesting();
    private static final int MAX_CONCOLIC_ITERATIONS = 100;

    private record MethodCoveragePair(
            MethodDeclaration methodDeclaration,
            Coverage coverage
    ) {}

    private final Map<MethodCoveragePair, ControlFlowGraph> methodToCfgCache = new java.util.HashMap<>();

    private ConcolicTesting() {}

    public static ConcolicTesting getInstance() {
        return INSTANCE;
    }

    /**
     * Generates test inputs for {@code methodDeclaration} using concolic testing.
     *
     * <ol>
     *   <li>Build the CFG for the method.</li>
     *   <li>Instrument the source and compile it.</li>
     *   <li>Generate and run a test driver with random seed inputs.</li>
     *   <li>Read execution traces to seed the {@link CoverageTracker}.</li>
     *   <li>While uncovered nodes remain, find paths through an uncovered node,
     *       symbolically execute them, solve constraints with Z3, and run the
     *       resulting concrete inputs.</li>
     * </ol>
     *
     * @param methodDeclaration the method under test
     * @param cu               the compilation unit containing the method
     * @param coverage         the coverage criterion (statement, branch, MCDC)
     * @return aggregated {@link TestResult} with all generated test cases
     * @throws Exception if instrumentation, compilation, or driver execution fails
     */
    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage) throws Exception {
        return generate(
                methodDeclaration,
                cu,
                coverage,
                RandomTestInput.createRandomTestData(methodDeclaration),
                new AllPathsFinder());
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               Map<String, Object> randomInput,
                               PathFinder pathFinder) throws Exception {
        return generate(methodDeclaration, cu, coverage, randomInput, pathFinder, Z3EncodingMode.BITVECTOR);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               Map<String, Object> randomInput,
                               PathFinder pathFinder,
                               Z3EncodingMode encodingMode) throws Exception {
        long startNanos = System.nanoTime();
        Z3StatisticsRecorder.beginRun();
        MemoryUsageMonitor.Snapshot initialMemoryUsage = MemoryUsageMonitor.capture();

        // --- 1. CFG ---------------------------------------------------------
        ControlFlowGraph cfg = getCfg(methodDeclaration, coverage);

        // --- 2. Instrument --------------------------------------------------
        InstrumentationFactory.InstrumentationProduct product =
                new InstrumentationFactory().produce(cu, cfg, coverage);
        CoverageTracker tracker = product.tracker();

        // --- 3. Generate test driver ----------------------------------------
        TestDriver.generate(product, methodDeclaration);

        List<TestDriver.ParamInfo> paramInfos =
                TestDriver.extractParams(methodDeclaration);

        // --- 4. Random seed run ---------------------------------------------
        List<TestData> allTestData = new ArrayList<>();
        TraceReader traceReader = new TraceReader(product.trackPath());
        try {
            TestData seedResult = TestDriver.run(
                    Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                    paramInfos,
                    randomInput,
                    Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
            allTestData.add(seedResult);
            // --- 5. Ingest traces from the seed run -----------------------------
            traceReader.applyTo(tracker);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        // --- 6. Prepare symbolic-execution context --------------------------
        Map<String, SymType> parameterTypes = buildParameterTypes(methodDeclaration);
        @SuppressWarnings("unchecked")
        List<ASTNode> parameters = new ArrayList<>(methodDeclaration.parameters());

        SymbolicExecution symbolicExecution = new SymbolicExecution();
        Map<List<ControlFlowGraph.Edge>, SolverResult> solverCache = new HashMap<>();

        // --- 7. Concolic loop -----------------------------------------------
        List<List<ControlFlowGraph.Edge>> uncoveredBatch =
                pathFinder.findPathsForUncovered(cfg, tracker);
        if (!uncoveredBatch.isEmpty()) {
            int iteration = 0;
            boolean exhaustedBatch = true;
            for (List<ControlFlowGraph.Edge> path : uncoveredBatch) {
                if (tracker.isComplete()) {
                    break;
                }
                if (iteration >= MAX_CONCOLIC_ITERATIONS) {
                    exhaustedBatch = false;
                    break;
                }
                SolverResult result = executePath(
                        symbolicExecution, solverCache, cfg, path,
                        parameters, parameterTypes, encodingMode);

                if (result instanceof SolverResult.Sat sat) {
                    Map<String, Object> newInputs =
                            extractInputsFromModel(sat.model(), paramInfos);

                    try {
                        TestData runResult = TestDriver.run(
                                Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                                paramInfos,
                                newInputs,
                                Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
                        allTestData.add(runResult);
                        // Ingest traces from this run
                        traceReader.applyTo(tracker);
                    } catch (TestDriver.DriverTimeoutException e) {
                        System.err.println(e.getMessage());
                        exhaustedBatch = false;
                        break;
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
                iteration++;
            }

            if (exhaustedBatch && !tracker.isComplete()) {
                for (int nodeId : new HashSet<>(tracker.getUncovered())) {
                    tracker.markSkipped(nodeId);
                }
            }
        } else {
            int iteration = 0;
            while (!tracker.isComplete() && iteration < MAX_CONCOLIC_ITERATIONS) {
                int uncoveredNodeId = tracker.getUncovered().iterator().next();
                int pathTargetNodeId = tracker.pathTargetFor(uncoveredNodeId);
                List<List<ControlFlowGraph.Edge>> paths =
                        pathFinder.findPath(
                                cfg,
                                pathTargetNodeId,
                                tracker.requiredExitFor(uncoveredNodeId),
                                tracker);

                boolean covered = false;
                boolean driverTimedOut = false;
                boolean solverTimedOut = false;
                for (List<ControlFlowGraph.Edge> path : paths) {
                    SolverResult result = executePath(
                            symbolicExecution, solverCache, cfg, path,
                            parameters, parameterTypes, encodingMode);

                    if (isTimeout(result)) {
                        solverTimedOut = true;
                        break;
                    }

                    if (result instanceof SolverResult.Sat sat) {
                        Map<String, Object> newInputs =
                                extractInputsFromModel(sat.model(), paramInfos);

                        try {
                            TestData runResult = TestDriver.run(
                                    Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                                    paramInfos,
                                    newInputs,
                                    Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
                            allTestData.add(runResult);
                            // Ingest traces from this run
                            traceReader.applyTo(tracker);
                        } catch (TestDriver.DriverTimeoutException e) {
                            System.err.println(e.getMessage());
                            driverTimedOut = true;
                            break;
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }

                        if (!tracker.isUncovered(uncoveredNodeId)) {
                            covered = true;
                            break; // target node now covered — move to next
                        }
                    }
                }

                if (!covered && !driverTimedOut && !solverTimedOut
                        && tracker.isUncovered(uncoveredNodeId)) {
                    List<List<ControlFlowGraph.Edge>> alternatives =
                            pathFinder.findAlternativePaths(
                                    cfg,
                                    pathTargetNodeId,
                                    tracker.requiredExitFor(uncoveredNodeId));
                    for (List<ControlFlowGraph.Edge> path : alternatives) {
                        SolverResult result = executePath(
                                symbolicExecution, solverCache, cfg, path,
                                parameters, parameterTypes, encodingMode);
                        if (!(result instanceof SolverResult.Sat sat)) continue;

                        Map<String, Object> newInputs =
                                extractInputsFromModel(sat.model(), paramInfos);
                        try {
                            TestData runResult = TestDriver.run(
                                    Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                                    paramInfos,
                                    newInputs,
                                    Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
                            allTestData.add(runResult);
                            traceReader.applyTo(tracker);
                        } catch (TestDriver.DriverTimeoutException e) {
                            System.err.println(e.getMessage());
                            driverTimedOut = true;
                            break;
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                        if (!tracker.isUncovered(uncoveredNodeId)) {
                            covered = true;
                            break;
                        }
                    }
                }

                if (!covered && tracker.isUncovered(uncoveredNodeId)) {
                    // Exhausted all paths without covering the node
                    tracker.markSkipped(uncoveredNodeId);
                }

                iteration++;
            }
        }

        // --- 8. Assemble result ---------------------------------------------
        long elapsedMillis = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        TestResult result = new TestResult(
                allTestData,
                tracker,
                MemoryUsageMonitor.allocatedBytesSince(initialMemoryUsage),
                elapsedMillis);
        ConcolicResultWriter.write(methodDeclaration, result, coverage, Z3StatisticsRecorder.snapshot());
        return result;
    }

    /**
     * Generates or retrieves the CFG.
     * Now returns the CFG instead of storing it in a field to ensure thread safety.
     *
     * @param method which is chosen to generate unit test.
     * @param coverage which {@link Coverage}, decides type of cfg: split condition or not.
     * @return an {@link ControlFlowGraph} for method with coverage
     */
    public ControlFlowGraph getCfg(MethodDeclaration method, Coverage coverage) {
        MethodCoveragePair pair = new MethodCoveragePair(method, coverage);

        return methodToCfgCache.computeIfAbsent(pair, p ->
                  new CfgBuilder(p.coverage() == Coverage.MCDC).build(p.methodDeclaration())
        );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static SolverResult executePath(
            SymbolicExecution symbolicExecution,
            Map<List<ControlFlowGraph.Edge>, SolverResult> solverCache,
            ControlFlowGraph cfg,
            List<ControlFlowGraph.Edge> path,
            List<ASTNode> parameters,
            Map<String, SymType> parameterTypes,
            Z3EncodingMode encodingMode) {
        List<ControlFlowGraph.Edge> cacheKey = List.copyOf(path);
        return solverCache.computeIfAbsent(cacheKey, ignored ->
                symbolicExecution.executePath(
                        cfg, cacheKey, parameters, parameterTypes, encodingMode));
    }

    private static boolean isTimeout(SolverResult result) {
        return result instanceof SolverResult.Unknown unknown
                && unknown.reason() != null
                && unknown.reason().toLowerCase(java.util.Locale.ROOT).contains("timeout");
    }

    /**
     * Builds a map of parameter name → {@link SymType} from the JDT method
     * declaration, using {@link SymTypeMap#convert(Type)}.
     */
    private static Map<String, SymType> buildParameterTypes(MethodDeclaration method) {
        Map<String, SymType> result = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = method.parameters();
        for (SingleVariableDeclaration param : params) {
            String name = param.getName().getIdentifier();
            Type type = param.getType();
            if (type != null) {
                result.put(name, SymTypeMap.convert(type));
            }
        }
        return result;
    }

    /**
     * Converts a Z3 model into a parameter-name → concrete-value map that
     * {@link TestDriver#run} can consume.
     *
     * <p>Each {@link core.SymbolicExecution.model.SymLiteral} in the bindings
     * already holds a boxed Java primitive (e.g. {@code Integer}, {@code Boolean}),
     * so the conversion is a straight pass-through.</p>
     */
    private static Map<String, Object> extractInputsFromModel(
            Z3ModelBindings bindings,
            List<TestDriver.ParamInfo> paramInfos) {

        Map<String, Object> inputs = new LinkedHashMap<>();
        for (TestDriver.ParamInfo param : paramInfos) {
            bindings.lookup(param.name())
                    .ifPresentOrElse(
                            lit -> inputs.put(param.name(), valueForParam(param, lit.value())),
                            () -> {
                                // Parameter unconstrained by the model —
                                // fall back to a default literal so the driver
                                // still receives every expected key.
                                Object fallback = defaultValueForType(param, bindings);
                                inputs.put(param.name(), fallback);
                            }
                    );
        }
        return inputs;
    }

    /** Produces a sensible default for a parameter when Z3 leaves it unconstrained. */
    private static Object defaultValueForType(TestDriver.ParamInfo param, Z3ModelBindings bindings) {
        String t = param.typeName().replace(" ", "");
        if (t.endsWith("[]")) {
            int length = arrayLengthFromModel(param.name(), bindings);
            Object primitiveArray = emptyPrimitiveArray(t, length);
            if (primitiveArray != null) return primitiveArray;
        }
        return defaultValueForType(param.typeName());
    }

    private static Object valueForParam(TestDriver.ParamInfo param, Object value) {
        return coercePrimitiveArray(param.typeName().replace(" ", ""), value);
    }

    private static Object emptyPrimitiveArray(String typeName, int length) {
        return switch (typeName) {
            case "boolean[]" -> new boolean[length];
            case "byte[]" -> new byte[length];
            case "short[]" -> new short[length];
            case "char[]" -> new char[length];
            case "int[]" -> new int[length];
            case "long[]" -> new long[length];
            case "float[]" -> new float[length];
            case "double[]" -> new double[length];
            case "String[]", "java.lang.String[]" -> {
                String[] values = new String[length];
                java.util.Arrays.fill(values, defaultStringValue());
                yield values;
            }
            default -> null;
        };
    }

    private static Object coercePrimitiveArray(String typeName, Object value) {
        if (value instanceof long[] values) {
            return switch (typeName) {
                case "byte[]" -> toByteArray(values);
                case "short[]" -> toShortArray(values);
                case "char[]" -> toCharArray(values);
                case "int[]" -> toIntArray(values);
                case "long[]" -> values;
                default -> value;
            };
        }
        if (value instanceof double[] values && "float[]".equals(typeName)) {
            return toFloatArray(values);
        }
        return value;
    }

    private static byte[] toByteArray(long[] values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }

    private static short[] toShortArray(long[] values) {
        short[] result = new short[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (short) values[i];
        return result;
    }

    private static char[] toCharArray(long[] values) {
        char[] result = new char[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (char) values[i];
        return result;
    }

    private static int[] toIntArray(long[] values) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (int) values[i];
        return result;
    }

    private static float[] toFloatArray(double[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (float) values[i];
        return result;
    }

    private static int arrayLengthFromModel(String name, Z3ModelBindings bindings) {
        return bindings.lookup(name + "__length")
                .map(SymLiteral::value)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(length -> length >= 0)
                .orElse(0);
    }

    private static Object defaultValueForType(String typeName) {
        String t = typeName.replace(" ", "");
        return switch (t) {
            case "boolean" -> Boolean.FALSE;
            case "byte"    -> (byte) 0;
            case "short"   -> (short) 0;
            case "char"    -> '\0';
            case "int"     -> 0;
            case "long"    -> 0L;
            case "float"   -> 0.0f;
            case "double"  -> 0.0;
            case "String", "java.lang.String" -> defaultStringValue();
            default -> {
                // Arrays or object types — can't generate a default here;
                // the caller will likely hit a driver failure, which is
                // acceptable for unsupported types.
                if (t.endsWith("[]")) {
                    yield java.lang.reflect.Array.newInstance(
                            int.class, 0); // generic empty array placeholder
                }
                yield null;
            }
        };
    }

    private static String defaultStringValue() {
        return "1.2.3.4";
    }
}
