package core.generation;

import core.cfg.CfgBuilder;
import core.cfg.ControlFlowGraph;
import core.cfg.Coverage;
import core.SymbolicExecution.AblationOptions;
import core.instrument.*;
import core.symbolic.SymbolicExecution;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;
import core.SymbolicExecution.z3encoder.Z3StatisticsRecorder;
import core.testdriver.TestData;
import core.testdriver.TestDriver;
import core.testdriver.TestResult;
import core.testpath.AllPathsFinder;
import core.testpath.BranchFlippingPathExplorer;
import core.testpath.CoverageTracker;
import core.testpath.PathFinder;
import core.testpath.TraceReader;
import core.utils.ConcolicLimits;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConcolicTesting {
    private static final ConcolicTesting INSTANCE = new ConcolicTesting();

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
        return generate(methodDeclaration, cu, coverage, MethodPreconditions.none());
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               MethodPreconditions preconditions) throws Exception {
        return generate(
                methodDeclaration,
                cu,
                coverage,
                RandomTestInput.createConcolicSeedData(methodDeclaration),
                new AllPathsFinder(),
                preconditions);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               Map<String, Object> randomInput,
                               PathFinder pathFinder) throws Exception {
        return generate(methodDeclaration, cu, coverage, List.of(randomInput), pathFinder,
                Z3EncodingMode.BITVECTOR);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               Map<String, Object> randomInput,
                               PathFinder pathFinder,
                               Z3EncodingMode encodingMode) throws Exception {
        return generate(methodDeclaration, cu, coverage, List.of(randomInput), pathFinder, encodingMode);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               List<Map<String, Object>> seedInputs,
                               PathFinder pathFinder) throws Exception {
        return generate(methodDeclaration, cu, coverage, seedInputs, pathFinder,
                Z3EncodingMode.BITVECTOR);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               List<Map<String, Object>> seedInputs,
                               PathFinder pathFinder,
                               MethodPreconditions preconditions) throws Exception {
        return generate(methodDeclaration, cu, coverage, seedInputs, pathFinder,
                Z3EncodingMode.BITVECTOR, AblationOptions.ALL_ENABLED, preconditions);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               List<Map<String, Object>> seedInputs,
                               PathFinder pathFinder,
                               Z3EncodingMode encodingMode) throws Exception {
        return generate(
                methodDeclaration,
                cu,
                coverage,
                seedInputs,
                pathFinder,
                encodingMode,
                AblationOptions.ALL_ENABLED);
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               List<Map<String, Object>> seedInputs,
                               PathFinder pathFinder,
                               Z3EncodingMode encodingMode,
                               AblationOptions ablationOptions) throws Exception {
        return generate(
                methodDeclaration,
                cu,
                coverage,
                seedInputs,
                pathFinder,
                encodingMode,
                ablationOptions,
                MethodPreconditions.none());
    }

    public TestResult generate(MethodDeclaration methodDeclaration,
                               CompilationUnit cu,
                               Coverage coverage,
                               List<Map<String, Object>> seedInputs,
                               PathFinder pathFinder,
                               Z3EncodingMode encodingMode,
                               AblationOptions ablationOptions,
                               MethodPreconditions preconditions) throws Exception {
        if (seedInputs == null) {
            throw new IllegalArgumentException("seedInputs must not be null");
        }
        MethodPreconditions methodPreconditions = preconditions == null
                ? MethodPreconditions.none()
                : preconditions;
        AblationOptions options = ablationOptions == null
                ? AblationOptions.ALL_ENABLED
                : ablationOptions;
        long startNanos = System.nanoTime();
        Z3StatisticsRecorder.beginRun();
        MemoryUsageMonitor.Snapshot initialMemoryUsage = MemoryUsageMonitor.capture();
        boolean retainTestData = ConcolicLimits.retainTestData();
        boolean writeConcolicJson = ConcolicLimits.writeConcolicJson();
        boolean cacheSolverResults = ConcolicLimits.cacheSolverResults();

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
        Map<String, Integer> minimumArrayLengths = new HashMap<>();
        for (TestDriver.ParamInfo param : paramInfos) {
            if (param.typeName().replace(" ", "").endsWith("[]")) {
                minimumArrayLengths.put(
                        param.name(),
                        RandomTestInput.minimumArrayLength(methodDeclaration, param.name()));
            }
        }
        methodPreconditions.minimumArrayLengths().forEach((name, length) ->
                minimumArrayLengths.merge(name, length, Math::max));
        List<Map<String, Object>> runnableSeeds =
                applyPreconditions(seedInputs, paramInfos, methodPreconditions);

        // --- 4. Seed runs ----------------------------------------------------
        List<TestData> allTestData = new ArrayList<>();
        TraceReader traceReader = new TraceReader(product.trackPath());
        BranchFlippingPathExplorer pathExplorer = new BranchFlippingPathExplorer();
        Deque<List<ControlFlowGraph.Edge>> pathWorklist = new ArrayDeque<>();
        Set<List<ControlFlowGraph.Edge>> queuedPaths = new HashSet<>();
        for (Map<String, Object> seedInput : runnableSeeds) {
            try {
                TestData seedResult = TestDriver.run(
                        Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                        paramInfos,
                        seedInput,
                        Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
                if (retainTestData) {
                    allTestData.add(seedResult);
                }
                // --- 5. Ingest traces from the seed run ---------------------
                traceReader.applyTo(tracker);
                enqueueFlippedPaths(pathExplorer, cfg, seedResult, pathWorklist, queuedPaths);
            } catch (Exception e) {
                ConcolicRunDiagnostics.recordDriverFailure();
                System.err.println(e.getMessage());
            }
        }

        // --- 6. Prepare symbolic-execution context --------------------------
        Map<String, SymType> parameterTypes = buildParameterTypes(methodDeclaration);
        @SuppressWarnings("unchecked")
        List<ASTNode> parameters = new ArrayList<>(methodDeclaration.parameters());

        SymbolicExecution symbolicExecution = new SymbolicExecution();
        Map<List<ControlFlowGraph.Edge>, SolverResult> solverCache =
                cacheSolverResults ? new HashMap<>() : null;
        List<SymbolicValue> preconditionConstraints =
                methodPreconditions.symbolicConstraints();

        // --- 7. Concolic loop -----------------------------------------------
        if (pathWorklist.isEmpty() && !tracker.isComplete()) {
            enqueueUncoveredTargetPaths(pathExplorer, cfg, tracker, pathWorklist, queuedPaths);
            if (pathWorklist.isEmpty()) {
                enqueuePathFinderTargetPaths(pathFinder, cfg, tracker, pathWorklist, queuedPaths);
            }
            if (pathWorklist.isEmpty()) {
                enqueuePath(pathExplorer.shortestEntryToExit(cfg), pathWorklist, queuedPaths);
            }
        }

        int maxConcolicIterations = ConcolicLimits.maxConcolicIterations();
        int maxNoProgressIterations = ConcolicLimits.maxNoProgressIterations();
        int iteration = 0;
        int noProgressIterations = 0;
        boolean stoppedByTimeout = false;
        boolean stoppedByNoProgress = false;
        while (!tracker.isComplete() && iteration < maxConcolicIterations) {
            int coveredBefore = tracker.getCovered().size();
            if (pathWorklist.isEmpty()) {
                enqueueUncoveredTargetPaths(pathExplorer, cfg, tracker, pathWorklist, queuedPaths);
                if (pathWorklist.isEmpty()) {
                    enqueuePathFinderTargetPaths(pathFinder, cfg, tracker, pathWorklist, queuedPaths);
                }
                if (pathWorklist.isEmpty()) {
                    break;
                }
            }
            List<ControlFlowGraph.Edge> path = pathWorklist.removeFirst();
            SolverResult result = executePath(
                    symbolicExecution, solverCache, cfg, path,
                    parameters, parameterTypes, encodingMode, options,
                    preconditionConstraints);

            if (isTimeout(result)) {
                stoppedByTimeout = true;
                break;
            }

            if (result instanceof SolverResult.Sat sat) {
                Map<String, Object> newInputs =
                        extractInputsFromModel(sat.model(), paramInfos, minimumArrayLengths);
                newInputs = methodPreconditions.apply(newInputs, paramInfos);

                try {
                    TestData runResult = TestDriver.run(
                            Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                            paramInfos,
                            newInputs,
                            Path.of(FilePath.PATH_TO_TOOL_OUTPUT));
                    if (retainTestData) {
                        allTestData.add(runResult);
                    }
                    traceReader.applyTo(tracker);
                    enqueueFlippedPaths(pathExplorer, cfg, runResult, pathWorklist, queuedPaths);
                } catch (TestDriver.DriverTimeoutException e) {
                    ConcolicRunDiagnostics.recordDriverFailure();
                    System.err.println(e.getMessage());
                    stoppedByTimeout = true;
                    break;
                } catch (Exception e) {
                    ConcolicRunDiagnostics.recordDriverFailure();
                    System.err.println(e.getMessage());
                }
            }

            if (tracker.getCovered().size() > coveredBefore) {
                noProgressIterations = 0;
            } else {
                noProgressIterations++;
                if (maxNoProgressIterations > 0
                        && noProgressIterations >= maxNoProgressIterations) {
                    stoppedByNoProgress = true;
                    break;
                }
            }

            iteration++;
        }

        String unfinishedReason;
        if (stoppedByTimeout) {
            unfinishedReason = "generated-run-timeout";
        } else if (stoppedByNoProgress) {
            unfinishedReason = "coverage-stalled";
        } else if (iteration >= maxConcolicIterations) {
            unfinishedReason = "generation-iteration-limit";
        } else {
            unfinishedReason = "branch-flip-worklist-exhausted";
        }
        for (int nodeId : new HashSet<>(tracker.getUncovered())) {
            tracker.markUnknown(nodeId, unfinishedReason);
        }

        // --- 8. Assemble result ---------------------------------------------
        long elapsedMillis = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        TestResult result = new TestResult(
                allTestData,
                tracker,
                MemoryUsageMonitor.allocatedBytesSince(initialMemoryUsage),
                elapsedMillis);
        if (writeConcolicJson) {
            ConcolicResultWriter.write(methodDeclaration, result, coverage, Z3StatisticsRecorder.snapshot());
        }
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
            Z3EncodingMode encodingMode,
            AblationOptions ablationOptions,
            List<SymbolicValue> preconditionConstraints) {
        List<ControlFlowGraph.Edge> cacheKey = List.copyOf(path);
        if (solverCache == null) {
            return symbolicExecution.executePath(
                    cfg, cacheKey, parameters, parameterTypes, encodingMode,
                    ablationOptions, preconditionConstraints);
        }
        return solverCache.computeIfAbsent(cacheKey, ignored ->
                symbolicExecution.executePath(
                        cfg, cacheKey, parameters, parameterTypes, encodingMode,
                        ablationOptions, preconditionConstraints));
    }

    private static void enqueueFlippedPaths(
            BranchFlippingPathExplorer pathExplorer,
            ControlFlowGraph cfg,
            TestData testData,
            Deque<List<ControlFlowGraph.Edge>> worklist,
            Set<List<ControlFlowGraph.Edge>> queuedPaths) {
        List<BranchFlippingPathExplorer.BranchDecision> decisions =
                testData.branchTrace().stream()
                        .map(step -> new BranchFlippingPathExplorer.BranchDecision(
                                step.nodeId(), step.edgeKind()))
                        .toList();
        for (List<ControlFlowGraph.Edge> path : pathExplorer.flippedPaths(cfg, decisions)) {
            enqueuePath(path, worklist, queuedPaths);
        }
    }

    private static void enqueueUncoveredTargetPaths(
            BranchFlippingPathExplorer pathExplorer,
            ControlFlowGraph cfg,
            CoverageTracker tracker,
            Deque<List<ControlFlowGraph.Edge>> worklist,
            Set<List<ControlFlowGraph.Edge>> queuedPaths) {
        for (int uncoveredNodeId : new HashSet<>(tracker.getUncovered())) {
            enqueuePath(
                    pathExplorer.shortestPrefixThrough(
                            cfg,
                            tracker.pathTargetFor(uncoveredNodeId),
                            tracker.requiredExitFor(uncoveredNodeId)),
                    worklist,
                    queuedPaths);
        }
    }

    private static void enqueuePathFinderTargetPaths(
            PathFinder pathFinder,
            ControlFlowGraph cfg,
            CoverageTracker tracker,
            Deque<List<ControlFlowGraph.Edge>> worklist,
            Set<List<ControlFlowGraph.Edge>> queuedPaths) {
        for (int uncoveredNodeId : new HashSet<>(tracker.getUncovered())) {
            int pathTargetNodeId = tracker.pathTargetFor(uncoveredNodeId);
            core.cfg.CfgEdgeKind requiredExit = tracker.requiredExitFor(uncoveredNodeId);
            boolean added = false;
            for (List<ControlFlowGraph.Edge> path :
                    pathFinder.findPath(cfg, pathTargetNodeId, requiredExit, tracker)) {
                added |= enqueuePath(path, worklist, queuedPaths);
            }
            if (!added) {
                for (List<ControlFlowGraph.Edge> path :
                        pathFinder.findAlternativePaths(cfg, pathTargetNodeId, requiredExit)) {
                    added |= enqueuePath(path, worklist, queuedPaths);
                }
            }
            if (added) {
                return;
            }
        }
    }

    private static boolean enqueuePath(
            List<ControlFlowGraph.Edge> path,
            Deque<List<ControlFlowGraph.Edge>> worklist,
            Set<List<ControlFlowGraph.Edge>> queuedPaths) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        List<ControlFlowGraph.Edge> immutablePath = List.copyOf(path);
        if (queuedPaths.add(immutablePath)) {
            worklist.addLast(immutablePath);
            return true;
        }
        return false;
    }

    private static boolean isTimeout(SolverResult result) {
        return result instanceof SolverResult.Unknown unknown
                && unknown.reason() != null
                && unknown.reason().toLowerCase(java.util.Locale.ROOT).contains("timeout");
    }

    private static List<Map<String, Object>> applyPreconditions(
            List<Map<String, Object>> seedInputs,
            List<TestDriver.ParamInfo> paramInfos,
            MethodPreconditions preconditions) {
        List<Map<String, Object>> result = new ArrayList<>(seedInputs.size());
        for (Map<String, Object> seedInput : seedInputs) {
            result.add(preconditions.apply(seedInput, paramInfos));
        }
        return List.copyOf(result);
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
        return extractInputsFromModel(bindings, paramInfos, Map.of());
    }

    private static Map<String, Object> extractInputsFromModel(
            Z3ModelBindings bindings,
            List<TestDriver.ParamInfo> paramInfos,
            Map<String, Integer> minimumArrayLengths) {

        Map<String, Object> inputs = new LinkedHashMap<>();
        for (TestDriver.ParamInfo param : paramInfos) {
            int minimumArrayLength = minimumArrayLengths.getOrDefault(param.name(), 0);
            bindings.lookup(param.name())
                    .ifPresentOrElse(
                            lit -> inputs.put(param.name(), valueForParam(
                                    param, lit.value(), minimumArrayLength)),
                            () -> {
                                // Parameter unconstrained by the model —
                                // fall back to a default literal so the driver
                                // still receives every expected key.
                                Object fallback = defaultValueForType(
                                        param, bindings, minimumArrayLength);
                                inputs.put(param.name(), fallback);
                            }
                    );
        }
        return inputs;
    }

    /** Produces a sensible default for a parameter when Z3 leaves it unconstrained. */
    private static Object defaultValueForType(
            TestDriver.ParamInfo param,
            Z3ModelBindings bindings,
            int minimumArrayLength) {
        String t = param.typeName().replace(" ", "");
        if (t.endsWith("[]")) {
            int length = Math.max(
                    minimumArrayLength,
                    arrayLengthFromModel(param.name(), bindings));
            Object primitiveArray = emptyPrimitiveArray(t, length);
            if (primitiveArray != null) return primitiveArray;
        }
        return defaultValueForType(param.typeName());
    }

    private static Object valueForParam(
            TestDriver.ParamInfo param, Object value, int minimumArrayLength) {
        Object coerced = coercePrimitiveArray(param.typeName().replace(" ", ""), value);
        if (coerced == null || !coerced.getClass().isArray()
                || java.lang.reflect.Array.getLength(coerced) >= minimumArrayLength) {
            return coerced;
        }
        Object expanded = java.lang.reflect.Array.newInstance(
                coerced.getClass().getComponentType(), minimumArrayLength);
        System.arraycopy(
                coerced, 0, expanded, 0, java.lang.reflect.Array.getLength(coerced));
        return expanded;
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
                .filter(length -> length >= 0
                        && length <= ConcolicLimits.maxGeneratedArrayLength())
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
