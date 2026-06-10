package core.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.cfg.Coverage;
import core.testdriver.TestData;
import core.testdriver.TestResult;
import core.testpath.CoverageTracker;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConcolicResultWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ConcolicResultWriter() {}

    public static Path write(MethodDeclaration method,
                             TestResult result,
                             Coverage coverage,
                             List<Map<String, String>> z3Statistics) throws IOException {
        Path outputDir = Path.of(FilePath.PATH_TO_TOOL_OUTPUT, "concolic-results");
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve(safeFileName(method.getName().getIdentifier()) + ".json");
        ArrayNode runs = readExistingRuns(outputFile);
        runs.add(MAPPER.valueToTree(toJson(method, result, coverage, z3Statistics)));
        MAPPER.writeValue(outputFile.toFile(), runs);
        return outputFile;
    }

    private static Map<String, Object> toJson(MethodDeclaration method,
                                              TestResult result,
                                              Coverage coverage,
                                              List<Map<String, String>> z3Statistics) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("methodName", method.getName().getIdentifier());
        json.put("coverageType", coverage.name());
        json.put("testResult", testResults(result));
        json.put("totalCoverage", totalCoverage(result.fullCoverage()));
        json.put("memoryUseBytes", result.memoryUsageBytes());
        json.put("memoryUseMb", result.memoryUsageBytes() / (1024.0 * 1024.0));
        json.put("executionTimeMs", result.executionTimeMillis());
        json.put("z3Statistics", z3Statistics);
        return json;
    }

    private static ArrayNode readExistingRuns(Path outputFile) throws IOException {
        ArrayNode runs = MAPPER.createArrayNode();
        if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
            return runs;
        }

        var existing = MAPPER.readTree(outputFile.toFile());
        if (existing == null || existing.isNull()) {
            return runs;
        }
        if (existing.isArray()) {
            runs.addAll((ArrayNode) existing);
        } else if (existing.isObject()) {
            runs.add((ObjectNode) existing);
        }
        return runs;
    }

    private static List<Map<String, Object>> testResults(TestResult result) {
        int totalNodes = totalCoverageNodes(result.fullCoverage());
        return result.testDataList().stream()
                .map(data -> testData(data, totalNodes))
                .toList();
    }

    private static Map<String, Object> testData(TestData data, int totalNodes) {
        Map<String, Object> json = new LinkedHashMap<>();
        Set<Integer> coveredNodeIds = coveredNodeIds(data);
        json.put("input", data.input());
        json.put("output", data.output());
        json.put("coveredNodeIds", coveredNodeIds);
        json.put("coverage", coverage(coveredNodeIds.size(), totalNodes));
        return json;
    }

    private static Map<String, Object> totalCoverage(CoverageTracker tracker) {
        int covered = tracker.getCovered().size();
        int uncovered = tracker.getUncovered().size();
        int skipped = tracker.getSkipped().size();
        int total = covered + uncovered + skipped;

        Map<String, Object> json = coverage(covered, total);
        json.put("uncoveredNodes", uncovered);
        json.put("skippedNodes", skipped);
        json.put("coveredNodeIds", tracker.getCovered());
        json.put("uncoveredNodeIds", tracker.getUncovered());
        json.put("skippedNodeIds", tracker.getSkipped());
        return json;
    }

    private static Map<String, Object> coverage(int covered, int total) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("coveredNodes", covered);
        json.put("totalNodes", total);
        json.put("percent", total == 0 ? 0.0 : covered * 100.0 / total);
        return json;
    }

    private static int totalCoverageNodes(CoverageTracker tracker) {
        return tracker.getCovered().size() + tracker.getUncovered().size() + tracker.getSkipped().size();
    }

    private static Set<Integer> coveredNodeIds(TestData data) {
        return data.coveredNodeIds() == null ? Collections.emptySet() : data.coveredNodeIds();
    }

    private static String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
