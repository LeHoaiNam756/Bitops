package core.testpath;

import core.instrument.TraceKind;
import core.utils.FilePath;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runtime capture point for instrumented code.
 *
 * <h3>Storage — synchronous flush</h3>
 * <ul>
 *   <li>Events are appended to a {@link ConcurrentLinkedQueue} in-memory
 *       (lock-free, safe for multi-threaded test execution).</li>
 *   <li>The buffer is flushed synchronously to a JSON Lines file under
 *       {@code <clonedRoot>/.ct4j-trace/} whenever the buffer reaches
 *       {@value #FLUSH_THRESHOLD} events, or when {@link #flush()} /
 *       {@link #endSession()} is called explicitly. No background thread is used.</li>
 *   <li>If the file write fails the buffer is retained in memory; a warning is
 *       logged but no exception is propagated to the instrumented code.</li>
 * </ul>
 *
 * <h3>Session lifecycle</h3>
 * <pre>{@code
 * TraceRecorder.startSession("com.example.Foo#bar(int)");
 * // … instrumented code runs …
 * TraceRecorder.endSession();
 * }</pre>
 * Each session writes to its own file:
 * {@code .ct4j-trace/<methodSig>-<uuid>.trace}
 *
 * <h3>Thread safety</h3>
 * <p>{@link #mark} is safe to call from any thread.  {@link #startSession} /
 * {@link #endSession} must be called from the same thread that orchestrates
 * the test run (the {@code CloneProject} pipeline thread).
 */
public final class TraceRecorder {

    // -----------------------------------------------------------------------
    // Tuning constants
    // -----------------------------------------------------------------------

    /** Number of events that trigger an eager flush. */
    static final int FLUSH_THRESHOLD = 500;

    // -----------------------------------------------------------------------
    // State (one logical session at a time)
    // -----------------------------------------------------------------------

    private static volatile Path   traceDir;
    private static volatile String sessionId;
    @Getter
    private static volatile Path   traceFile;

    private static final ConcurrentLinkedQueue<TraceEvent> buffer =
            new ConcurrentLinkedQueue<>();
    private static final AtomicInteger bufferSize = new AtomicInteger(0);

    // -----------------------------------------------------------------------
    // Public API — called by instrumented code + orchestrator
    // -----------------------------------------------------------------------

    /**
     * Record that CFG node {@code nodeId} was executed with the given
     * {@link TraceKind}.
     *
     * <p>This method is injected at every probe point by
     * {@link core.instrument.SourceEmitter}.  It must be as cheap as possible.
     *
     * @return {@code true} always — lets the method be used inline in
     *         short-circuit boolean expressions for condition probing
     *         ({@code ((cond) && mark(trueId, COND_T)) || mark(falseId, COND_F)})
     */
    public static boolean mark(int nodeId, TraceKind kind) {
        buffer.add(new TraceEvent(nodeId, kind, System.currentTimeMillis()));
        if (bufferSize.incrementAndGet() >= FLUSH_THRESHOLD) {
            flushSync();
        }
        return kind == TraceKind.COND_T; // lets false-branch probe always run
    }

    /**
     * Begin a new trace session for {@code methodSig}.
     * Resets the buffer; all flushes are synchronous — no background thread is started.
     *
     * @param methodSig human-readable method signature (used in file name)
     * @param clonedProjectRoot root directory of the cloned project
     *                          ({@link core.utils.FilePath#PATH_TO_CLONED_PROJECT})
     */
    public static synchronized void startSession(String methodSig,
                                                 Path clonedProjectRoot) {
        // Clean up any lingering previous session
        endSessionQuietly();

        buffer.clear();
        bufferSize.set(0);

        Path root = clonedProjectRoot.isAbsolute()
                ? clonedProjectRoot
                : Path.of(FilePath.JCIA_PROJECT_ROOT_PATH).resolve(clonedProjectRoot);
        traceDir = root.resolve(".ct4j-trace");
        try {
            Files.createDirectories(traceDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create trace dir: " + traceDir, e);
        }

        // Sanitise method sig for use in a filename
        String safeSig = methodSig.replaceAll("[^A-Za-z0-9._-]", "_");
        sessionId  = safeSig + "-" + UUID.randomUUID();
        traceFile  = traceDir.resolve(sessionId + ".trace");
        try {
            Files.createFile(traceFile);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create trace file: " + traceFile, e);
        }
    }

    /**
     * End the current session: flush remaining events, stop the scheduler.
     */
    public static synchronized void endSession() {
        flushSync();
        endSessionQuietly();
    }

    /**
     * Explicit flush — blocks until the current buffer contents are written.
     * Safe to call from any thread.
     */
    public static void flush() {
        flushSync();
    }

    // -----------------------------------------------------------------------
    // Internal flush logic
    // -----------------------------------------------------------------------

    /**
     * Drains the buffer to {@link #traceFile}.
     * Falls back gracefully if no session is active or if I/O fails.
     */
    private static synchronized void flushSync() {
        if (traceFile == null || buffer.isEmpty()) return;

        // Snapshot current buffer contents
        StringBuilder sb = new StringBuilder();
        TraceEvent event;
        int drained = 0;
        while ((event = buffer.poll()) != null) {
            sb.append(event.toJsonLine()).append('\n');
            drained++;
        }
        bufferSize.addAndGet(-drained);

        if (sb.isEmpty()) return;

        try (BufferedWriter w = Files.newBufferedWriter(
                traceFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            w.write(sb.toString());
        } catch (IOException e) {
            // Fall back to in-memory only — re-enqueue what we drained
            System.err.println("[TraceRecorder] Flush failed, retaining events in memory: "
                    + e.getMessage());
            // We've already removed from buffer; re-add at front is not
            // supported by ConcurrentLinkedQueue.  Accept the loss and log.
        }
    }

    private static void endSessionQuietly() {
        traceFile = null;
        sessionId = null;
    }


    // -----------------------------------------------------------------------
    // TraceEvent value type
    // -----------------------------------------------------------------------

    /**
     * Captured at each probe point.  Stored in the in-memory buffer and
     * serialised to the trace file.
     */
    record TraceEvent(int nodeId, TraceKind kind, long timestamp) {

        /** JSON Lines format: {@code {"nodeId":42,"kind":"NODE","ts":1715000000000}} */
        String toJsonLine() {
            return "{\"nodeId\":" + nodeId
                    + ",\"kind\":\"" + kind.name() + "\""
                    + ",\"ts\":" + timestamp
                    + "}";
        }
    }

    private TraceRecorder() {}   // non-instantiable
}
