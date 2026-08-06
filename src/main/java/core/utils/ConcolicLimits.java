package core.utils;

/** Resource limits applied to solver-generated concrete test inputs. */
public final class ConcolicLimits {

    public static final String MAX_ARRAY_LENGTH_PROPERTY =
            "ct4j.max.generated.array.length";
    public static final int DEFAULT_MAX_ARRAY_LENGTH = 1_024;
    public static final String MAX_CONCOLIC_ITERATIONS_PROPERTY =
            "ct4j.max.concolic.iterations";
    public static final int DEFAULT_MAX_CONCOLIC_ITERATIONS = 1_000;
    public static final String MAX_NO_PROGRESS_ITERATIONS_PROPERTY =
            "ct4j.max.concolic.no_progress_iterations";
    public static final int DEFAULT_MAX_NO_PROGRESS_ITERATIONS = 3;
    public static final String RETAIN_TEST_DATA_PROPERTY =
            "ct4j.retain.test.data";
    public static final String WRITE_CONCOLIC_JSON_PROPERTY =
            "ct4j.write.concolic.json";
    public static final String RETAIN_Z3_STATISTICS_PROPERTY =
            "ct4j.retain.z3.statistics";
    public static final String CACHE_SOLVER_RESULTS_PROPERTY =
            "ct4j.cache.solver.results";

    private ConcolicLimits() {}

    /**
     * Maximum array length that concolic model extraction may materialize.
     * Invalid property values fall back to the safe default.
     */
    public static int maxGeneratedArrayLength() {
        int configured = Integer.getInteger(
                MAX_ARRAY_LENGTH_PROPERTY, DEFAULT_MAX_ARRAY_LENGTH);
        return configured >= 0 ? configured : DEFAULT_MAX_ARRAY_LENGTH;
    }

    /**
     * Maximum number of concolic paths explored before remaining obligations
     * are marked unknown. Invalid property values fall back to the default.
     */
    public static int maxConcolicIterations() {
        int configured = Integer.getInteger(
                MAX_CONCOLIC_ITERATIONS_PROPERTY, DEFAULT_MAX_CONCOLIC_ITERATIONS);
        return configured > 0 ? configured : DEFAULT_MAX_CONCOLIC_ITERATIONS;
    }

    /**
     * Maximum consecutive symbolic attempts that may fail to increase coverage
     * before generation stops and marks remaining obligations unknown.
     */
    public static int maxNoProgressIterations() {
        int configured = Integer.getInteger(
                MAX_NO_PROGRESS_ITERATIONS_PROPERTY, DEFAULT_MAX_NO_PROGRESS_ITERATIONS);
        return configured >= 0 ? configured : DEFAULT_MAX_NO_PROGRESS_ITERATIONS;
    }

    public static boolean retainTestData() {
        return Boolean.parseBoolean(
                System.getProperty(RETAIN_TEST_DATA_PROPERTY, "true"));
    }

    public static boolean writeConcolicJson() {
        return Boolean.parseBoolean(
                System.getProperty(WRITE_CONCOLIC_JSON_PROPERTY, "true"));
    }

    public static boolean retainZ3Statistics() {
        return Boolean.parseBoolean(
                System.getProperty(RETAIN_Z3_STATISTICS_PROPERTY, "true"));
    }

    public static boolean cacheSolverResults() {
        return Boolean.parseBoolean(
                System.getProperty(CACHE_SOLVER_RESULTS_PROPERTY, "true"));
    }
}
