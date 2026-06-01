package core.testpath;

import core.instrument.TraceKind;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads JSON Lines trace files written by {@link TraceRecorder} and applies
 * them to a {@link CoverageTracker}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * TraceReader reader = new TraceReader(traceDir);
 * TraceReader.Summary summary = reader.applyTo(tracker);
 * System.out.println("Events processed: " + summary.eventsProcessed());
 * System.out.println("Files skipped:    " + summary.filesSkipped());
 * }</pre>
 *
 * <h3>Error handling</h3>
 * <ul>
 *   <li>Corrupted lines are skipped; valid events in the same file are still
 *       processed.</li>
 *   <li>Unreadable files are counted in {@link Summary#filesSkipped()} and do
 *       not abort processing of other files.</li>
 * </ul>
 */
public final class TraceReader {

    private final Path traceDir;

    /**
     * @param traceDir directory that contains {@code *.trace} files
     *                 (typically {@code <clonedRoot>/.ct4j-trace/})
     */
    public TraceReader(Path traceDir) {
        this.traceDir = traceDir;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parse every {@code *.trace} file under {@link #traceDir} and update
     * {@code tracker} accordingly.
     *
     * <p>For each {@link TraceEvent}:
     * <ul>
     *   <li>{@link TraceKind#NODE}   → {@link CoverageTracker#markCovered(int)}</li>
     *   <li>{@link TraceKind#COND_T} → {@link CoverageTracker#markCovered(int)} with true-branch ID</li>
     *   <li>{@link TraceKind#COND_F} → {@link CoverageTracker#markCovered(int)} with false-branch ID</li>
     * </ul>
     *
     * @return a {@link Summary} describing what was processed
     */
    public Summary applyTo(CoverageTracker tracker) throws IOException {
        int eventsProcessed = 0;
        int eventsSkipped   = 0;
        int filesSkipped    = 0;

        try (Stream<Path> files = Files.list(traceDir)) {
            for (Path file : (Iterable<Path>) files
                    .filter(p -> p.getFileName().toString().endsWith(".trace"))
                    ::iterator) {

                try {
                    int[] counts = processFile(file, tracker);
                    eventsProcessed += counts[0];
                    eventsSkipped   += counts[1];
                } catch (IOException e) {
                    System.err.println("[TraceReader] Cannot read " + file + ": " + e.getMessage());
                    filesSkipped++;
                }
            }
        }

        return new Summary(eventsProcessed, eventsSkipped, filesSkipped);
    }

    /**
     * Convenience overload: read a single trace file rather than the whole
     * directory.  Useful in unit tests.
     */
    public Summary applyFile(Path traceFile, CoverageTracker tracker) throws IOException {
        int[] counts = processFile(traceFile, tracker);
        return new Summary(counts[0], counts[1], 0);
    }

    // -----------------------------------------------------------------------
    // File processing
    // -----------------------------------------------------------------------

    /**
     * @return int[]{eventsProcessed, eventsSkipped}
     */
    private int[] processFile(Path file, CoverageTracker tracker) throws IOException {
        int processed = 0, skipped = 0;

        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    TraceEvent event = parseLine(line);
                    applyEvent(event, tracker);
                    processed++;
                } catch (Exception e) {
                    System.err.println("[TraceReader] Skipping bad line in "
                            + file.getFileName() + ": " + e.getMessage());
                    skipped++;
                }
            }
        }

        return new int[]{processed, skipped};
    }

    private void applyEvent(TraceEvent event, CoverageTracker tracker) {
        // All three kinds map to markCovered — the ID already encodes the kind
        // (true/false branch IDs were assigned by InstrumentationPlanner as
        // nodeId*2 and nodeId*2+1 respectively).
        tracker.markCovered(event.nodeId());
    }

    // -----------------------------------------------------------------------
    // JSON Lines parsing (no external deps)
    // -----------------------------------------------------------------------

    private static TraceEvent parseLine(String line) {
        int nodeId    = parseInt(line, "nodeId");
        String kindStr = parseString(line, "kind");
        long ts       = parseLong(line, "ts");
        return new TraceEvent(nodeId, TraceKind.valueOf(kindStr), ts);
    }

    private static int parseInt(String json, String key) {
        return Integer.parseInt(extractToken(json, key).trim());
    }

    private static long parseLong(String json, String key) {
        return Long.parseLong(extractToken(json, key).trim());
    }

    private static String parseString(String json, String key) {
        int ki    = json.indexOf("\"" + key + "\"");
        if (ki < 0) throw new IllegalArgumentException("Missing key: " + key);
        int colon = json.indexOf(':', ki);
        int open  = json.indexOf('"', colon + 1);
        int close = json.indexOf('"', open + 1);
        return json.substring(open + 1, close);
    }

    private static String extractToken(String json, String key) {
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) throw new IllegalArgumentException("Missing key: " + key);
        int colon = json.indexOf(':', ki);
        int start = colon + 1;
        int end   = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end);
    }

    // -----------------------------------------------------------------------
    // Value types
    // -----------------------------------------------------------------------

    private record TraceEvent(int nodeId, TraceKind kind, long timestamp) {}

    /**
     * Processing summary returned by {@link #applyTo}.
     *
     * @param eventsProcessed events successfully applied to the tracker
     * @param eventsSkipped   malformed lines that were ignored
     * @param filesSkipped    unreadable files that were ignored
     */
    public record Summary(int eventsProcessed, int eventsSkipped, int filesSkipped) {

        /** {@code true} if every file was readable and every line was valid. */
        public boolean isClean() {
            return eventsSkipped == 0 && filesSkipped == 0;
        }

        @Override
        public String toString() {
            return "TraceReader.Summary{processed=" + eventsProcessed
                    + ", skipped=" + eventsSkipped
                    + ", filesSkipped=" + filesSkipped + "}";
        }
    }
}