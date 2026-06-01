package core.SymbolicExecution.z3encoder;

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
    private static final ThreadLocal<List<Map<String, String>>> RUN_STATISTICS =
            ThreadLocal.withInitial(ArrayList::new);

    private Z3StatisticsRecorder() {}

    public static void beginRun() {
        RUN_STATISTICS.get().clear();
    }

    public static void record(Statistics statistics) {
        if (statistics == null) {
            return;
        }
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Statistics.Entry entry : statistics.getEntries()) {
            snapshot.put(entry.Key, entry.getValueString());
        }
        RUN_STATISTICS.get().add(Collections.unmodifiableMap(snapshot));
    }

    public static List<Map<String, String>> snapshot() {
        return List.copyOf(RUN_STATISTICS.get());
    }
}
