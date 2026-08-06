package core.generation;

import core.SymbolicExecution.AblationOptions;
import core.SymbolicExecution.AblationPart;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the one-disabled-feature ablation study and writes a wide CSV. Each
 * feature group contains metrics for the run where that feature is disabled
 * and all other features are enabled.
 */
public final class AblationBatchRunner {
    private record RunMetrics(String status, double coveragePercent, long timeMs, double memoryMb,
                              long variableCount, long z3ExpressionCount, long z3SampleCount,
                              int driverFailures, String error) {}

    private AblationBatchRunner() {}

    public static void main(String[] args) throws Exception {
        configureLeanExperimentDefaults();
        String zipPathStr = System.getProperty("zipPath", "d:\\CT4J\\emLoc.zip");
        String zipPathsStr = System.getProperty("zipPaths", zipPathStr);
        String coverageStr = System.getProperty("coverage", "STATEMENT").toUpperCase(Locale.ROOT);
        String outputCsvStr = System.getProperty("outputCsv", "ablation-results.csv");
        String metric = System.getProperty("coverageMetric", "raw").toLowerCase(Locale.ROOT);
        int maxMethods = Integer.parseInt(System.getProperty("maxMethods", "-1"));
        int startMethod = Integer.parseInt(System.getProperty("startMethod", "1"));
        int timeoutSec = Integer.parseInt(System.getProperty("timeoutSeconds", "90"));

        List<Path> zipPaths = parseZipPaths(zipPathsStr);
        List<Coverage> coverages = parseCoverages(coverageStr);

        System.out.println("[AblationBatchRunner] zipPaths   = " + zipPaths);
        System.out.println("[AblationBatchRunner] coverage   = " + coverages);
        System.out.println("[AblationBatchRunner] metric     = " + metric);
        System.out.println("[AblationBatchRunner] maxMethods = " + (maxMethods < 0 ? "ALL" : maxMethods));
        System.out.println("[AblationBatchRunner] start     = " + startMethod);
        System.out.println("[AblationBatchRunner] timeout    = " + timeoutSec + "s");
        System.out.println("[AblationBatchRunner] outputCsv  = " + outputCsvStr);

        List<Map<String, String>> rows = new ArrayList<>();
        for (Path zipPath : zipPaths) {
            if (!Files.exists(zipPath)) {
                System.err.println("[AblationBatchRunner] ERROR: zip not found: " + zipPath);
                System.exit(1);
            }

            Project project = new Project(zipPath);
            List<MethodDeclaration> methods = new ArrayList<>(project.getMethods());
            methods.sort(Comparator.comparing(AblationBatchRunner::displayName));
            int startIndex = Math.max(0, startMethod - 1);
            int available = Math.max(0, methods.size() - startIndex);
            int total = maxMethods > 0 ? Math.min(maxMethods, available) : available;

            System.out.println("[AblationBatchRunner] corpus     = " + zipPath.getFileName());
            System.out.println("[AblationBatchRunner] methods    = " + methods.size()
                    + ", running " + total);

            int rowNumber = 0;
            int totalRows = coverages.size() * total;
            for (Coverage coverage : coverages) {
                for (int i = startIndex; i < startIndex + total; i++) {
                    rowNumber++;
                    MethodDeclaration method = methods.get(i);
                    CompilationUnit rootAst = project.getRootAST(method);
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("corpus", zipPath.getFileName().toString());
                    row.put("coverage_type", coverage.name());
                    row.put("method_name", displayName(method));

                    System.out.printf("[%d/%d] %s %s%n", rowNumber, totalRows, coverage, row.get("method_name"));
                    if (rootAst == null) {
                        for (AblationPart part : AblationPart.values()) {
                            putMetrics(row, part, failed("FAILED", "no_root_ast"));
                        }
                        rows.add(row);
                        writeCsv(Paths.get(outputCsvStr), rows);
                        continue;
                    }

                    for (AblationPart part : AblationPart.values()) {
                        System.out.printf("  -> disable %s ...%n", part.csvColumn());
                        putMetrics(row, part, runOne(
                                method,
                                rootAst,
                                coverage,
                                AblationOptions.disable(part),
                                metric,
                                timeoutSec));
                    }
                    rows.add(row);
                    writeCsv(Paths.get(outputCsvStr), rows);
                }
            }
        }

        writeCsv(Paths.get(outputCsvStr), rows);
        System.out.println("[AblationBatchRunner] DONE: " + outputCsvStr);
        System.exit(0);
    }

    private static RunMetrics runOne(MethodDeclaration method,
                                     CompilationUnit rootAst,
                                     Coverage coverage,
                                     AblationOptions options,
                                     String metric,
                                     int timeoutSec) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<RunMetrics> future = executor.submit(() -> {
            ConcolicRunDiagnostics.begin();
            TestResult result = ConcolicTesting.getInstance().generate(
                    method,
                    rootAst,
                    coverage,
                    RandomTestInput.createConcolicSeedData(method),
                    new AllPathsFinder(),
                    Z3EncodingMode.BITVECTOR,
                    options);

            Z3StatisticsRecorder.Summary z3Summary = Z3StatisticsRecorder.summary();
            double percent = "feasible".equals(metric)
                    ? result.fullCoverage().feasibleCoveragePercent()
                    : result.fullCoverage().rawCoveragePercent();
            int driverFailures = ConcolicRunDiagnostics.driverFailures();
            String status = driverFailures > 0 ? "SUCCESS_WITH_DRIVER_ERRORS" : "SUCCESS";
            return new RunMetrics(status, percent, result.executionTimeMillis(),
                    result.memoryUsageBytes() / (1024.0 * 1024.0),
                    z3Summary.variableCount(),
                    z3Summary.expressionCount(),
                    z3Summary.sampleCount(),
                    driverFailures, "");
        });
        executor.shutdown();

        try {
            return future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            executor.shutdownNow();
            return failed("TIMEOUT", "timeout");
        } catch (Exception e) {
            future.cancel(true);
            executor.shutdownNow();
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return failed("FAILED", cause.getClass().getSimpleName() + ":" + sanitizeError(cause.getMessage()));
        }
    }

    private static List<Path> parseZipPaths(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Paths::get)
                .toList();
    }

    private static List<Coverage> parseCoverages(String value) {
        try {
            if ("ALL".equals(value)) {
                return List.of(Coverage.values());
            }
            return List.of(Coverage.valueOf(value));
        } catch (IllegalArgumentException e) {
            System.err.println("[AblationBatchRunner] ERROR: unknown coverage: " + value
                    + " (use STATEMENT, BRANCH, MCDC, ALL)");
            System.exit(1);
            return List.of();
        }
    }

    private static RunMetrics failed(String status, String error) {
        return new RunMetrics(status, 0.0, 0L, 0.0, 0L, 0L, 0L, 0, error);
    }

    private static void putMetrics(Map<String, String> row, AblationPart part, RunMetrics metrics) {
        String prefix = part.csvColumn();
        row.put(prefix + "_status", metrics.status());
        row.put(prefix + "_coverage_percent", decimal(metrics.coveragePercent()));
        row.put(prefix + "_time_ms", Long.toString(metrics.timeMs()));
        row.put(prefix + "_memory_mb", decimal(metrics.memoryMb()));
        row.put(prefix + "_variable_count", Long.toString(metrics.variableCount()));
        row.put(prefix + "_z3_expression_count", Long.toString(metrics.z3ExpressionCount()));
        row.put(prefix + "_z3_sample_count", Long.toString(metrics.z3SampleCount()));
        row.put(prefix + "_driver_failures", Integer.toString(metrics.driverFailures()));
        row.put(prefix + "_error", metrics.error());
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

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static void writeCsv(Path outputCsv, List<Map<String, String>> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        columns.add("corpus");
        columns.add("coverage_type");
        columns.add("method_name");
        for (AblationPart part : AblationPart.values()) {
            String prefix = part.csvColumn();
            columns.add(prefix + "_status");
            columns.add(prefix + "_coverage_percent");
            columns.add(prefix + "_time_ms");
            columns.add(prefix + "_memory_mb");
            columns.add(prefix + "_variable_count");
            columns.add(prefix + "_z3_expression_count");
            columns.add(prefix + "_z3_sample_count");
            columns.add(prefix + "_driver_failures");
            columns.add(prefix + "_error");
        }
        lines.add(String.join(",", columns));
        for (Map<String, String> row : rows) {
            List<String> cells = new ArrayList<>();
            for (String column : columns) {
                cells.add(escapeCsv(row.getOrDefault(column, "")));
            }
            lines.add(String.join(",", cells));
        }
        if (outputCsv.getParent() != null) {
            Files.createDirectories(outputCsv.getParent());
        }
        Files.write(outputCsv, lines, StandardCharsets.UTF_8);
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private static String sanitizeError(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return message.replace('\n', ' ').replace('\r', ' ');
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
