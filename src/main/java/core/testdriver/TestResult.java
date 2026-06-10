package core.testdriver;

import core.testpath.CoverageTracker;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of one concolic-testing run for a single method.
 *
 * <p>A {@code TestResult} is constructed by the test-generation engine once all
 * test cases have been executed and their individual {@link TestData} records
 * are finalised (inputs, outputs, and per-case coverage all set).
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #testDataList()} — every generated test case in generation order.</li>
 *   <li>{@link #fullCoverage()} — a {@link CoverageTracker} initialised from the
 *       full {@link core.instrument.InstrumentationPlan} and updated by running
 *       <em>all</em> test cases together, representing the combined coverage of
 *       the entire test suite.</li>
 *   <li>{@link #memoryUsageBytes()} — bytes allocated by the current thread during
 *       the entire generation run, measured by the caller with
 *       {@code com.sun.management.ThreadMXBean}.</li>
 *   <li>{@link #executionTimeMillis()} — wall-clock execution time for the
 *       generation run, in milliseconds.</li>
 * </ul>
 *
 * <p>Instances are immutable.  Use {@link Builder} to assemble the result
 * incrementally as test cases complete.
 *
 * <pre>{@code
 * TestResult.Builder builder = new TestResult.Builder();
 *
 * for (TestData td : generatedCases) {
 *     builder.add(td);
 * }
 *
 * // fullCoverage is the CoverageTracker fed probes from every test case combined
 * TestResult result = builder.build(fullRunTracker, allocatedBytes, elapsedMillis);
 * }</pre>
 */
public final class TestResult {

    private final List<TestData>  testDataList;
    private final CoverageTracker fullCoverage;
    private final long            memoryUsageBytes;
    private final long            executionTimeMillis;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Direct constructor — prefer {@link Builder} for incremental assembly.
     *
     * @param testDataList     all test cases (defensive copy is taken)
     * @param fullCoverage     coverage tracker updated by running all test cases
     * @param memoryUsageBytes current-thread allocated bytes over the generation run
     */
    public TestResult(List<TestData> testDataList,
                      CoverageTracker fullCoverage,
                      long memoryUsageBytes) {
        this(testDataList, fullCoverage, memoryUsageBytes, 0L);
    }

    /**
     * Direct constructor — prefer {@link Builder} for incremental assembly.
     *
     * @param testDataList          all test cases (defensive copy is taken)
     * @param fullCoverage          coverage tracker updated by running all test cases
     * @param memoryUsageBytes      current-thread allocated bytes over the generation run
     * @param executionTimeMillis   wall-clock execution time over the generation run
     */
    public TestResult(List<TestData> testDataList,
                      CoverageTracker fullCoverage,
                      long memoryUsageBytes,
                      long executionTimeMillis) {
        this.testDataList     = List.copyOf(Objects.requireNonNull(testDataList, "testDataList"));
        this.fullCoverage     = Objects.requireNonNull(fullCoverage, "fullCoverage");
        this.memoryUsageBytes = memoryUsageBytes;
        this.executionTimeMillis = executionTimeMillis;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * All generated test cases in the order they were produced.
     * The list is unmodifiable.
     */
    public List<TestData> testDataList() {
        return testDataList;
    }

    /**
     * Coverage state after running <em>every</em> test case in
     * {@link #testDataList()} against the instrumented method.
     * This is distinct from the per-case , which
     * reflects only what a single input covered.
     */
    public CoverageTracker fullCoverage() {
        return fullCoverage;
    }

    /**
     * Memory allocated by the current thread during the generation run, in bytes.
     * Computed from {@code com.sun.management.ThreadMXBean} allocation counters,
     * so concurrent thread allocations and GC heap sweeps do not produce negative
     * deltas. Returns zero when thread allocation tracking is unavailable.
     */
    public long memoryUsageBytes() {
        return memoryUsageBytes;
    }

    /**
     * Wall-clock execution time for the generation run, in milliseconds.
     */
    public long executionTimeMillis() {
        return executionTimeMillis;
    }

    /** Convenience: number of test cases in this result. */
    public int size() {
        return testDataList.size();
    }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return "TestResult{"
                + "cases=" + testDataList.size()
                + ", fullCoverage=" + fullCoverage
                + ", memoryUsageBytes=" + memoryUsageBytes
                + ", executionTimeMillis=" + executionTimeMillis
                + '}';
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    /**
     * Incrementally assembles a {@link TestResult}.
     *
     * <pre>{@code
     * TestResult.Builder builder = new TestResult.Builder();
     * builder.add(testData1);
     * builder.add(testData2);
     * // fullRunTracker has been fed probes from all test cases combined
     * TestResult result = builder.build(fullRunTracker, heapDelta, elapsedMillis);
     * }</pre>
     */
    public static final class Builder {

        private final java.util.ArrayList<TestData> cases = new java.util.ArrayList<>();

        /** Add a completed {@link TestData} to the accumulator. */
        public Builder add(TestData data) {
            cases.add(Objects.requireNonNull(data, "data"));
            return this;
        }

        /**
         * Finalise and return the immutable {@link TestResult}.
         *
         * @param fullCoverage     coverage tracker updated by the full test suite run
         * @param memoryUsageBytes current-thread allocated bytes measured over the whole run
         */
        public TestResult build(CoverageTracker fullCoverage, long memoryUsageBytes) {
            return build(fullCoverage, memoryUsageBytes, 0L);
        }

        /**
         * Finalise and return the immutable {@link TestResult}.
         *
         * @param fullCoverage          coverage tracker updated by the full test suite run
         * @param memoryUsageBytes      current-thread allocated bytes measured over the whole run
         * @param executionTimeMillis   wall-clock execution time measured over the whole run
         */
        public TestResult build(CoverageTracker fullCoverage,
                                long memoryUsageBytes,
                                long executionTimeMillis) {
            return new TestResult(cases, fullCoverage, memoryUsageBytes, executionTimeMillis);
        }
    }
}
