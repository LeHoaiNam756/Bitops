package core.generation;

/** Per-thread diagnostics for failures that concolic generation intentionally recovers from. */
final class ConcolicRunDiagnostics {
    private static final ThreadLocal<Integer> DRIVER_FAILURES = ThreadLocal.withInitial(() -> 0);

    private ConcolicRunDiagnostics() {}

    static void begin() {
        DRIVER_FAILURES.set(0);
    }

    static void recordDriverFailure() {
        DRIVER_FAILURES.set(DRIVER_FAILURES.get() + 1);
    }

    static int driverFailures() {
        return DRIVER_FAILURES.get();
    }
}
