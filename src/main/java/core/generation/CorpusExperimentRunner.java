package core.generation;

import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import core.SymbolicExecution.z3encoder.Z3StatisticsRecorder;
import core.cfg.Coverage;
import core.testdriver.TestResult;
import core.testpath.AllPathsFinder;
import core.utils.ConcolicLimits;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Runs the normalized bit-operation corpus and writes one aggregate CSV row per coverage/method. */
public final class CorpusExperimentRunner {
    private record RunMetrics(double coverage, long timeMs, double memoryMb,
                              long variableCount, long expressionCount, long z3SampleCount,
                              int driverFailures) {}

    private record RunOutcome(RunMetrics metrics, String error) {}

    private CorpusExperimentRunner() {}

    public static void main(String[] args) throws Exception {
        configureLeanExperimentDefaults();
        Path zip = Path.of(requiredProperty("zipPath"));
        Path output = Path.of(System.getProperty("outputCsv", "bitops-concolic-results.csv"));
        Path runOutput = Path.of(System.getProperty("runOutputCsv", "bitops-concolic-runs.csv"));
        int runs = Integer.parseInt(System.getProperty("runs", "10"));
        int timeoutSeconds = Integer.parseInt(System.getProperty("timeoutSeconds", "60"));
        int maxMethods = Integer.parseInt(System.getProperty("maxMethods", "-1"));
        String coverageProperty = System.getProperty("coverage", "ALL").toUpperCase(Locale.ROOT);
        List<Coverage> coverages = "ALL".equals(coverageProperty)
                ? List.of(Coverage.values())
                : List.of(Coverage.valueOf(coverageProperty));

        Project project = new Project(zip);
        List<MethodDeclaration> methods = new ArrayList<>(project.getMethods());
        methods.sort(Comparator.comparing(CorpusExperimentRunner::displayName));
        System.out.println("Loaded methods: " + methods.size());
        if (maxMethods >= 0 && methods.size() > maxMethods) {
            methods = new ArrayList<>(methods.subList(0, maxMethods));
        }

        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.createDirectories(runOutput.toAbsolutePath().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             BufferedWriter runWriter = Files.newBufferedWriter(runOutput, StandardCharsets.UTF_8)) {
            writer.write("coverage_type,method_name,completed_runs,average_coverage_percent,"
                    + "average_time_ms,average_memory_mb,average_variable_count,"
                    + "average_z3_expression_count,status\n");
            runWriter.write("coverage_type,method_name,run_number,status,coverage_percent,"
                    + "time_ms,memory_mb,variable_count,z3_expression_count,z3_sample_count,"
                    + "driver_failures,error\n");

            int total = coverages.size() * methods.size();
            int index = 0;
            for (Coverage coverage : coverages) {
                for (MethodDeclaration method : methods) {
                    index++;
                    String name = displayName(method);
                    System.out.printf("[%d/%d] %s %s%n", index, total, coverage, name);
                    List<RunMetrics> completed = new ArrayList<>();
                    String lastError = null;
                    for (int run = 1; run <= runs; run++) {
                        RunOutcome outcome = execute(project, method, coverage, timeoutSeconds);
                        writeRunRow(runWriter, coverage, name, run, outcome);
                        runWriter.flush();
                        if (outcome.metrics() != null) {
                            completed.add(outcome.metrics());
                        } else {
                            lastError = outcome.error();
                        }
                    }
                    writeRow(writer, coverage, name, runs, completed, lastError);
                    writer.flush();
                }
            }
        }
        System.out.println("Wrote " + output.toAbsolutePath());
        System.out.println("Wrote " + runOutput.toAbsolutePath());
        System.exit(0);
    }

    private static RunOutcome execute(Project project, MethodDeclaration method,
                                      Coverage coverage, int timeoutSeconds) {
        CompilationUnit cu = project.getRootAST(method);
        if (cu == null) return new RunOutcome(null, "no_root_ast");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<RunMetrics> future = executor.submit(() -> {
            ConcolicRunDiagnostics.begin();
            TestResult result = ConcolicTesting.getInstance().generate(
                    method, cu, coverage,
                    RandomTestInput.createConcolicSeedData(method),
                    new AllPathsFinder(), Z3EncodingMode.BITVECTOR);
            Z3StatisticsRecorder.Summary z3Summary = Z3StatisticsRecorder.summary();
            double percent = result.fullCoverage().rawCoveragePercent();
            return new RunMetrics(percent, result.executionTimeMillis(),
                    result.memoryUsageBytes() / (1024.0 * 1024.0),
                    z3Summary.variableCount(),
                    z3Summary.expressionCount(),
                    z3Summary.sampleCount(),
                    ConcolicRunDiagnostics.driverFailures());
        });
        executor.shutdown();
        try {
            return new RunOutcome(future.get(timeoutSeconds, TimeUnit.SECONDS), null);
        } catch (TimeoutException e) {
            future.cancel(true);
            executor.shutdownNow();
            return new RunOutcome(null, "timeout");
        } catch (Exception e) {
            future.cancel(true);
            executor.shutdownNow();
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return new RunOutcome(null, cause.getClass().getSimpleName() + ":" + cause.getMessage());
        }
    }

    private static void writeRow(BufferedWriter writer, Coverage coverage, String methodName,
                                 int requestedRuns, List<RunMetrics> runs, String lastError)
            throws Exception {
        int count = runs.size();
        String status = count == requestedRuns ? "SUCCESS" : count == 0 ? "FAILED" : "PARTIAL";
        int driverFailures = runs.stream().mapToInt(RunMetrics::driverFailures).sum();
        if (count == requestedRuns && driverFailures > 0) {
            status = "SUCCESS_WITH_DRIVER_ERRORS:" + driverFailures;
        }
        if (lastError != null && count < requestedRuns) status += ":" + lastError;
        writer.write(String.join(",",
                coverage.name(), csv(methodName), Integer.toString(count),
                decimal(average(runs, RunMetrics::coverage)),
                decimal(average(runs, r -> r.timeMs())),
                decimal(average(runs, RunMetrics::memoryMb)),
                decimal(averageZ3(runs, r -> r.variableCount())),
                decimal(averageZ3(runs, r -> r.expressionCount())), csv(status)));
        writer.write('\n');
    }

    private static void writeRunRow(BufferedWriter writer, Coverage coverage, String methodName,
                                    int runNumber, RunOutcome outcome) throws Exception {
        RunMetrics metrics = outcome.metrics();
        String status = metrics != null ? "SUCCESS" : "FAILED";
        if (metrics != null && metrics.driverFailures() > 0) {
            status = "SUCCESS_WITH_DRIVER_ERRORS";
        } else if (metrics == null && "timeout".equals(outcome.error())) {
            status = "TIMEOUT";
        }
        writer.write(String.join(",",
                coverage.name(), csv(methodName), Integer.toString(runNumber), status,
                metrics == null ? "" : decimal(metrics.coverage()),
                metrics == null ? "" : Long.toString(metrics.timeMs()),
                metrics == null ? "" : decimal(metrics.memoryMb()),
                metrics == null ? "" : Long.toString(metrics.variableCount()),
                metrics == null ? "" : Long.toString(metrics.expressionCount()),
                metrics == null ? "" : Long.toString(metrics.z3SampleCount()),
                metrics == null ? "" : Integer.toString(metrics.driverFailures()),
                csv(outcome.error() == null ? "" : outcome.error())));
        writer.write('\n');
    }

    private interface Metric { double get(RunMetrics metrics); }

    private static double average(List<RunMetrics> runs, Metric metric) {
        return runs.isEmpty() ? 0.0 : runs.stream().mapToDouble(metric::get).average().orElse(0.0);
    }

    private static double averageZ3(List<RunMetrics> runs, Metric metric) {
        long samples = runs.stream().mapToLong(RunMetrics::z3SampleCount).sum();
        return samples == 0 ? 0.0 : runs.stream().mapToDouble(metric::get).sum() / samples;
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String displayName(MethodDeclaration method) {
        String className = method.getParent() instanceof TypeDeclaration type
                ? type.getName().getIdentifier() : "UnknownClass";
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = method.parameters();
        String signature = parameters.stream().map(p -> p.getType().toString()
                        + (p.isVarargs() ? "..." : ""))
                .reduce((a, b) -> a + ";" + b).orElse("");
        return className + "." + method.getName().getIdentifier() + "(" + signature + ")";
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing -D" + name);
        return value;
    }

    private static void configureLeanExperimentDefaults() {
        setDefault(ConcolicLimits.RETAIN_TEST_DATA_PROPERTY, "false");
        setDefault(ConcolicLimits.WRITE_CONCOLIC_JSON_PROPERTY, "false");
        setDefault(ConcolicLimits.RETAIN_Z3_STATISTICS_PROPERTY, "false");
        setDefault(ConcolicLimits.CACHE_SOLVER_RESULTS_PROPERTY, "false");
    }

    private static void setDefault(String property, String value) {
        if (System.getProperty(property) == null) {
            System.setProperty(property, value);
        }
    }
}
