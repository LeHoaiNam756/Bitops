package core.parser;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates all {@link ParseEntry} objects produced by {@link JavaProjectParser}.
 *
 * <p>Provides convenient look-up methods so callers can retrieve a
 * {@link CompilationUnit} by either a relative or absolute {@link Path}.
 */
public final class ParseResult {

    private final Path projectRoot;
    private final List<ParseEntry> entries;

    /** Indexed by absolute path string for O(1) lookup. */
    private final Map<String, ParseEntry> byAbsolutePath;

    /** Indexed by relative path string for O(1) lookup. */
    private final Map<String, ParseEntry> byRelativePath;

    ParseResult(Path projectRoot, List<ParseEntry> entries) {
        this.projectRoot = projectRoot;
        this.entries     = List.copyOf(entries);

        this.byAbsolutePath = entries.stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> e.absolutePath().toString(),
                        e -> e));

        this.byRelativePath = entries.stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> e.relativePath().toString(),
                        e -> e));
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** All parsed entries (order matches file-system walk order). */
    public List<ParseEntry> entries() { return entries; }

    /** Root directory used during parsing. */
    public Path projectRoot()         { return projectRoot; }

    /** Total number of parsed files. */
    public int size()                 { return entries.size(); }

    /**
     * Look up a {@link ParseEntry} by its absolute {@link Path}.
     *
     * @return an {@link Optional} containing the entry, or empty if not found
     */
    public Optional<ParseEntry> byAbsolutePath(Path path) {
        return Optional.ofNullable(byAbsolutePath.get(path.toAbsolutePath().toString()));
    }

    /**
     * Look up a {@link ParseEntry} by its path relative to the project root.
     *
     * @return an {@link Optional} containing the entry, or empty if not found
     */
    public Optional<ParseEntry> byRelativePath(Path relativePath) {
        return Optional.ofNullable(byRelativePath.get(relativePath.toString()));
    }

    /**
     * Shortcut: get only the {@link CompilationUnit} for a relative path.
     */
    public Optional<CompilationUnit> compilationUnit(Path relativePath) {
        return byRelativePath(relativePath).map(ParseEntry::compilationUnit);
    }

    /**
     * All entries that had at least one JDT parse problem.
     */
    public List<ParseEntry> entriesWithErrors() {
        return entries.stream()
                      .filter(e -> e.errorCount() > 0)
                      .toList();
    }

    @Override
    public String toString() {
        return "ParseResult{files=" + entries.size() +
               ", projectRoot=" + projectRoot + "}";
    }
}