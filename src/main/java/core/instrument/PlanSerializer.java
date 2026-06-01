package core.instrument;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional persistence layer for {@link InstrumentationPlan}.
 *
 * <h3>Why serialize?</h3>
 * <p>For large batch runs (many files, many methods) keeping every
 * {@link org.eclipse.jdt.core.dom.ASTNode} reference alive in memory is
 * expensive.  Serializing the plan to JSON Lines lets the orchestrator drop
 * the plan from RAM and reload it cheaply when needed.
 *
 * <h3>Serialized format</h3>
 * <p>JSON Lines (one JSON object per line, no wrapping array).  Each line:
 * <pre>{@code
 * {"nodeId":42,"kind":"NODE","start":1234,"len":56}
 * }</pre>
 * Only the four fields above are written — no {@code ASTNode} objects are
 * serialised.  On {@link #read}, the {@link CompilationUnit} is re-used to
 * look up {@link ASTNode}s by source position.
 *
 * <h3>No external JSON library required</h3>
 * <p>The format is simple enough that we write and parse it by hand, keeping
 * the class free of runtime dependencies beyond the JDK.
 */
public final class PlanSerializer {

    // -----------------------------------------------------------------------
    // Write
    // -----------------------------------------------------------------------

    /**
     * Persist {@code plan} to {@code dest}.  The file is created (or
     * overwritten) atomically via a temp-file rename on the same filesystem.
     */
    public static void write(InstrumentationPlan plan, Path dest) throws IOException {
        Path parent = dest.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path tmp = parent != null
                ? Files.createTempFile(parent, ".plan-", ".tmp")
                : Files.createTempFile(".plan-", ".tmp");

        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            for (TracePoint tp : plan.points()) {
                w.write(toJsonLine(tp));
                w.newLine();
            }
        }
        Files.move(tmp, dest,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Reload a plan from {@code src}.
     *
     * <p>Requires the same {@link CompilationUnit} that was used when the plan
     * was originally created, so that {@link ASTNode} references can be
     * restored by source position.  If a position cannot be matched (source
     * changed between write and read), that entry is silently skipped and
     * logged to {@code System.err}.
     *
     * @param src plan file written by {@link #write}
     * @param cu  original compilation unit for ASTNode lookup
     * @return rehydrated {@link InstrumentationPlan}
     */
    public static InstrumentationPlan read(Path src, CompilationUnit cu) throws IOException {
        Map<Long, ASTNode> index = buildIndex(cu);
        List<TracePoint>   points = new ArrayList<>();

        try (BufferedReader r = Files.newBufferedReader(src, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    points.add(parseLine(line, index));
                } catch (Exception e) {
                    System.err.println("[PlanSerializer] Skipping malformed line "
                            + lineNo + ": " + e.getMessage());
                }
            }
        }

        return new InstrumentationPlan(points);
    }

    // -----------------------------------------------------------------------
    // Serialization helpers
    // -----------------------------------------------------------------------

    private static String toJsonLine(TracePoint tp) {
        return "{\"nodeId\":" + tp.cfgNodeId()
                + ",\"kind\":\"" + tp.kind().name() + "\""
                + ",\"start\":" + tp.startPosition()
                + ",\"len\":" + tp.length()
                + "}";
    }

    private static TracePoint parseLine(String line, Map<Long, ASTNode> index) {
        int nodeId = parseInt(line, "nodeId");
        String kindStr = parseString(line, "kind");
        int start  = parseInt(line, "start");
        int len    = parseInt(line, "len");

        TraceKind kind = TraceKind.valueOf(kindStr);
        long key = posKey(start, len);

        ASTNode ast = index.get(key);
        if (ast == null) {
            throw new IllegalStateException(
                    "No AST node found at start=" + start + " len=" + len);
        }
        return new TracePoint(nodeId, ast, kind);
    }

    // -----------------------------------------------------------------------
    // AST index (same scheme as InstrumentationPlanner)
    // -----------------------------------------------------------------------

    private static Map<Long, ASTNode> buildIndex(CompilationUnit cu) {
        Map<Long, ASTNode> idx = new HashMap<>();
        cu.accept(new ASTVisitor() {
            @Override
            public void preVisit(ASTNode node) {
                int s = node.getStartPosition(), l = node.getLength();
                if (s >= 0 && l > 0) idx.put(posKey(s, l), node);
            }
        });
        return idx;
    }

    private static long posKey(int start, int length) {
        return ((long) start << 20) | (length & 0xFFFFF);
    }

    // -----------------------------------------------------------------------
    // Minimal hand-rolled JSON field extractors (no external libs)
    // -----------------------------------------------------------------------

    private static int parseInt(String json, String key) {
        String token = extractToken(json, key);
        return Integer.parseInt(token.trim());
    }

    private static String parseString(String json, String key) {
        // value is quoted: "key":"VALUE"
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) throw new IllegalArgumentException("Missing key: " + key);
        int colon = json.indexOf(':', ki);
        int open  = json.indexOf('"', colon + 1);
        int close = json.indexOf('"', open + 1);
        return json.substring(open + 1, close);
    }

    private static String extractToken(String json, String key) {
        // numeric value: "key":VALUE,  or  "key":VALUE}
        int ki = json.indexOf("\"" + key + "\"");
        if (ki < 0) throw new IllegalArgumentException("Missing key: " + key);
        int colon = json.indexOf(':', ki);
        int start = colon + 1;
        int end   = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end);
    }

    private PlanSerializer() {}   // non-instantiable utility class
}