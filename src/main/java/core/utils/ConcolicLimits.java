package core.utils;

/** Resource limits applied to solver-generated concrete test inputs. */
public final class ConcolicLimits {

    public static final String MAX_ARRAY_LENGTH_PROPERTY =
            "ct4j.max.generated.array.length";
    public static final int DEFAULT_MAX_ARRAY_LENGTH = 1_024;

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
}
