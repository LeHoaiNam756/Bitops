package core.testdriver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.cfg.CfgEdgeKind;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/**
 * Holds the data for a single generated test case.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Created before execution with {@code input} only:
 *       {@link #TestData(Map)}.</li>
 *   <li>After the driver subprocess returns, the result JSON it wrote is
 *       loaded via {@link #fromJson(Path, List)} which populates
 *       {@code output} and {@code coveredNodeIds} in one step.</li>
 * </ol>
 *
 * <h3>Why {@code Set<Integer>} for coverage, not {@code CoverageTracker}</h3>
 * <p>{@code CoverageTracker} is <em>additive</em>: every {@code mark()} call
 * grows the same internal set for the lifetime of the tracker.  Storing the
 * live tracker in {@code TestData} would mean every instance in the list
 * sees the <em>union</em> of all runs, not the coverage of its own run.
 *
 * <p>Instead the generated driver does:
 * <pre>
 *   TraceRecorder.reset();          // clear any stale state
 *   result = method.invoke(...);    // run this input
 *   coveredNodeIds = TraceRecorder.hitNodes();  // snapshot immediately
 * </pre>
 * and writes the snapshot to a JSON file.  {@link #fromJson} reads it back
 * as an immutable {@code Set<Integer>} that can never drift after the fact.
 *
 * <h3>Input map</h3>
 * <p>{@code input} is keyed by parameter name, matching the JSON keys the
 * generated driver reads, so the same map can be serialised directly to the
 * {@code --input} file without any translation.
 */
public final class TestData {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /**
     * Parameter-name → argument value (boxed primitives / arrays as returned
     * by {@link core.testdriver.JsonArgParser}).  Never null; may be empty for void methods.
     */
    private final Map<String, Object> input;

    /**
     * String representation of the value returned by the instrumented method,
     * exactly as serialised by the driver.  {@code null} means either
     * "not yet executed" or a {@code void} return — use {@link #hasOutput()}.
     */
    private final String output;

    /** {@code true} once the driver result has been loaded. */
    private final boolean outputSet;

    /**
     * Immutable snapshot of CFG node IDs fired during this single invocation.
     * {@code null} until the driver result is loaded via {@link #fromJson}.
     */
    private final Set<Integer> coveredNodeIds;
    private final List<BranchStep> branchTrace;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Create a pre-execution {@code TestData} with inputs only.
     * Call {@link #fromJson} after the driver subprocess finishes to
     * obtain the completed instance.
     *
     * @param input parameter-name → argument value (defensive copy is taken)
     */
    public TestData(Map<String, Object> input) {
        this(Map.copyOf(Objects.requireNonNull(input, "input")), null, false, null, List.of());
    }

    private TestData(Map<String, Object> input,
                     String output,
                     boolean outputSet,
                     Set<Integer> coveredNodeIds,
                     List<BranchStep> branchTrace) {
        this.input          = input;
        this.output         = output;
        this.outputSet      = outputSet;
        this.coveredNodeIds = coveredNodeIds;
        this.branchTrace    = branchTrace == null ? List.of() : List.copyOf(branchTrace);
    }

    // -----------------------------------------------------------------------
    // Deserialisation — load the JSON file written by the driver subprocess
    // -----------------------------------------------------------------------

    /**
     * Read the JSON result file written by the generated driver and return a
     * completed {@code TestData}.
     *
     * <p>Expected file format (written by {@code DriverMain}):
     * <pre>{@code
     * {
     *   "input":          { "a": 3, "b": 4 },
     *   "output":         "7",
     *   "coveredNodeIds": [2, 4, 6]
     * }
     * }</pre>
     *
     * <p>The {@code input} node is re-parsed using {@code paramTypes} so that
     * the returned map contains the same typed objects ({@code int[]}, etc.)
     * as the original pre-execution {@code TestData}.
     *
     * @param resultJson path to the JSON file produced by {@code --output=<path>}
     * @param paramTypes ordered list of (name, typeName) pairs — same list used
     *                   to generate the driver — needed to re-type the input values
     * @return fully populated {@code TestData}
     * @throws IOException if the file cannot be read or the JSON is malformed
     */
    public static TestData fromJson(
            Path resultJson,
            List<TestDriver.ParamInfo> paramTypes) throws IOException {

        JsonNode root = MAPPER.readTree(resultJson.toFile());

        // ── input ────────────────────────────────────────────────────────────
        JsonNode inputNode = root.path("input");
        Map<String, Object> input = new LinkedHashMap<>();
        for (TestDriver.ParamInfo p : paramTypes) {
            JsonNode val = inputNode.get(p.name());
            if (val != null) {
                input.put(p.name(), JsonArgParser.convert(p.name(), p.typeName(), val));
            }
        }

        // ── output ───────────────────────────────────────────────────────────
        JsonNode outputNode = root.path("output");
        String output = outputNode.isNull() ? null : outputNode.asText();

        // ── coveredNodeIds ───────────────────────────────────────────────────
        JsonNode idsNode = root.path("coveredNodeIds");
        Set<Integer> ids = new HashSet<>();
        if (idsNode.isArray()) {
            for (JsonNode id : idsNode) {
                ids.add(id.intValue());
            }
        }

        // ── branchTrace ─────────────────────────────────────────────────────
        JsonNode branchTraceNode = root.path("branchTrace");
        List<BranchStep> branchTrace = new java.util.ArrayList<>();
        if (branchTraceNode.isArray()) {
            for (JsonNode item : branchTraceNode) {
                JsonNode nodeId = item.get("nodeId");
                JsonNode edgeKind = item.get("edgeKind");
                if (nodeId != null && edgeKind != null) {
                    branchTrace.add(new BranchStep(
                            nodeId.intValue(),
                            CfgEdgeKind.valueOf(edgeKind.asText())));
                }
            }
        }

        return new TestData(Map.copyOf(input), output, true, Set.copyOf(ids), branchTrace);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Parameter-name → typed argument value (unmodifiable). */
    public Map<String, Object> input() {
        return input;
    }

    /**
     * String representation of the method's return value, exactly as written
     * by the driver (e.g. {@code "7"}, {@code "[1, 2, 3]"}, {@code "null"}).
     * Returns {@code null} for {@code void} returns or before execution.
     *
     * @see #hasOutput()
     */
    public String output() {
        return output;
    }

    /**
     * {@code true} if the driver result has been loaded (even when the method
     * returned {@code void} and {@link #output()} is {@code null}).
     */
    public boolean hasOutput() {
        return outputSet;
    }

    /**
     * Immutable snapshot of CFG node IDs hit during this single invocation.
     * Returns {@code null} before {@link #fromJson} has been called.
     *
     * @see #hasCoverage()
     */
    public Set<Integer> coveredNodeIds() {
        return coveredNodeIds;
    }

    /** {@code true} if the coverage snapshot has been loaded. */
    public boolean hasCoverage() {
        return coveredNodeIds != null;
    }

    public List<BranchStep> branchTrace() {
        return branchTrace;
    }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return "TestData{"
                + "input=" + input
                + ", output=" + (outputSet ? output : "<not set>")
                + ", coveredNodeIds=" + (coveredNodeIds != null ? coveredNodeIds : "<not set>")
                + ", branchTrace=" + branchTrace
                + '}';
    }

    public record BranchStep(int nodeId, CfgEdgeKind edgeKind) {}
}
