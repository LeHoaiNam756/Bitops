package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Statistics;
import core.utils.ConcolicLimits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-thread collector for Z3 statistics produced during one concolic run.
 */
public final class Z3StatisticsRecorder {
    private static final ThreadLocal<List<Map<String, Object>>> RUN_STATISTICS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<SummaryAccumulator> RUN_SUMMARY =
            ThreadLocal.withInitial(SummaryAccumulator::new);

    public record Summary(long variableCount, long expressionCount, long sampleCount) {}

    private Z3StatisticsRecorder() {}

    public static void beginRun() {
        RUN_STATISTICS.get().clear();
        RUN_SUMMARY.get().clear();
    }

    public static void record(Statistics statistics) {
        record(statistics, new Expr<?>[0]);
    }

    public static void record(Statistics statistics, Expr<?>[] finalAssertions) {
        if (statistics == null) {
            return;
        }
        Z3ExpressionMetrics.Counts counts = Z3ExpressionMetrics.count(finalAssertions);
        RUN_SUMMARY.get().add(counts.variableCount(), counts.expressionCount());
        if (!ConcolicLimits.retainZ3Statistics()) {
            return;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (Statistics.Entry entry : statistics.getEntries()) {
            snapshot.put(entry.Key, entry.getValueString());
        }
        snapshot.put("variableCount", counts.variableCount());
        snapshot.put("expressionCount", counts.expressionCount());
        RUN_STATISTICS.get().add(Collections.unmodifiableMap(snapshot));
    }

    public static List<Map<String, Object>> snapshot() {
        return List.copyOf(RUN_STATISTICS.get());
    }

    public static Summary summary() {
        SummaryAccumulator accumulator = RUN_SUMMARY.get();
        return new Summary(
                accumulator.variableCount,
                accumulator.expressionCount,
                accumulator.sampleCount);
    }

    private static final class SummaryAccumulator {
        private long variableCount;
        private long expressionCount;
        private long sampleCount;

        private void add(long variables, long expressions) {
            variableCount += variables;
            expressionCount += expressions;
            sampleCount++;
        }

        private void clear() {
            variableCount = 0L;
            expressionCount = 0L;
            sampleCount = 0L;
        }
    }
}
