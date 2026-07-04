package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Statistics;

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

    private Z3StatisticsRecorder() {}

    public static void beginRun() {
        RUN_STATISTICS.get().clear();
    }

    public static void record(Statistics statistics) {
        record(statistics, new Expr<?>[0]);
    }

    public static void record(Statistics statistics, Expr<?>[] finalAssertions) {
        if (statistics == null) {
            return;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (Statistics.Entry entry : statistics.getEntries()) {
            snapshot.put(entry.Key, entry.getValueString());
        }
        Z3ExpressionMetrics.Counts counts = Z3ExpressionMetrics.count(finalAssertions);
        snapshot.put("variableCount", counts.variableCount());
        snapshot.put("expressionCount", counts.expressionCount());
        RUN_STATISTICS.get().add(Collections.unmodifiableMap(snapshot));
    }

    public static List<Map<String, Object>> snapshot() {
        return List.copyOf(RUN_STATISTICS.get());
    }
}
