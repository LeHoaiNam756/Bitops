package core.parser;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link java.nio.file.FileVisitor} that walks a directory tree and collects
 * {@code .java} source files, guided by a {@link ParserConfig}.
 *
 * <p>Directory exclusion rules (evaluated in order in {@link #preVisitDirectory}):
 * <ol>
 *   <li>Hidden directories (name starts with {@code .}) are <em>always</em> skipped.</li>
 *   <li>Directories whose name appears in {@link ParserConfig#excludedDirectories()} are skipped.</li>
 * </ol>
 *
 * <p>File inclusion rules (evaluated in {@link #visitFile}):
 * <ol>
 *   <li>Must be a regular file ending in {@code .java} (glob {@code **.java}).</li>
 *   <li>Must pass {@link ParserConfig#fileFilter()} (defaults to {@code p -> true}).</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * JavaFileCollector collector = new JavaFileCollector(config);
 * Files.walkFileTree(rootDir, collector);
 * List<Path> files = collector.getJavaFiles();
 * }</pre>
 */
public class JavaFileCollector extends SimpleFileVisitor<Path> {

    private static final PathMatcher JAVA_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:**.java");

    private final ParserConfig config;
    private final List<Path>   javaFiles = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Create a collector driven by the given configuration.
     *
     * @param config parser configuration; must not be {@code null}
     */
    public JavaFileCollector(ParserConfig config) {
        this.config = config;
    }

    /**
     * Convenience no-arg constructor using {@link ParserConfig#defaults()}.
     * Behaviour is identical to the previous hard-coded implementation.
     */
    public JavaFileCollector() {
        this(ParserConfig.defaults());
    }

    // -----------------------------------------------------------------------
    // FileVisitor callbacks
    // -----------------------------------------------------------------------

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        String name = dir.getFileName() == null ? "" : dir.getFileName().toString();

        // Rule 1: always skip hidden directories (.git, .idea, …)
        if (name.startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        // Rule 2: skip directories explicitly listed in config
        if (config.excludedDirectories().contains(name)) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()
                && JAVA_MATCHER.matches(file)
                && config.fileFilter().test(file)) {
            javaFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.printf("[WARN] Cannot visit file %s: %s%n", file, exc.getMessage());
        return FileVisitResult.CONTINUE;
    }

    // -----------------------------------------------------------------------
    // Result accessor
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of all collected {@code .java} file paths
     * in file-system walk order.
     */
    public List<Path> getJavaFiles() {
        return Collections.unmodifiableList(javaFiles);
    }
}
