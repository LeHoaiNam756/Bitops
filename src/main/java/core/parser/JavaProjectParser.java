package core.parser;

import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Parses all {@code .java} files in a project (from a zip or directory) into
 * JDT {@link CompilationUnit}s, driven by a {@link ParserConfig}.
 *
 * <h3>Quick-start</h3>
 * <pre>{@code
 * // Zero-config – identical to the original hard-coded behaviour
 * ParseResult result = new JavaProjectParser().parseZip(Path.of("project.zip"));
 *
 * // With custom config
 * ParserConfig config = ParserConfig.builder()
 *     .javaVersion("11")
 *     .excludeDirectory("target")
 *     .addClasspathEntry(Path.of("lib/guava.jar"))
 *     .build();
 *
 * ParseResult result = new JavaProjectParser(config).parseDirectory(Path.of("/src"));
 *
 * for (ParseEntry entry : result.entries()) {
 *     Path          srcFile = entry.relativePath();
 *     CompilationUnit  cu   = entry.compilationUnit();
 * }
 * }</pre>
 */
public class JavaProjectParser {

    private final ParserConfig config;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Create a parser driven by the given configuration.
     *
     * @param config parser configuration; must not be {@code null}
     */
    public JavaProjectParser(ParserConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /**
     * Convenience no-arg constructor using {@link ParserConfig#defaults()}.
     * Behaviour is identical to the previous hard-coded implementation.
     */
    public JavaProjectParser() {
        this(ParserConfig.defaults());
    }

    // -----------------------------------------------------------------------
    // Public parse methods
    // -----------------------------------------------------------------------

    /**
     * Parse all {@code .java} files from a zip archive.
     *
     * @param zipPath path to the {@code .zip} file
     * @return {@link ParseResult} containing all (sourcePath → CompilationUnit) mappings
     */
    public ParseResult parseZip(Path zipPath) throws IOException {
        Path tempDir = Files.createTempDirectory("java-parser-").toAbsolutePath().normalize();
        try {
            ZipExtractor.extract(zipPath, tempDir);
            return parseDirectory(tempDir);
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(tempDir)));
        }
    }

    /**
     * Parse all {@code .java} files under a root directory, respecting the
     * exclusion rules and file filters in {@link #config}.
     *
     * @param rootDir root of the Java source tree
     * @return {@link ParseResult} containing all (sourcePath → CompilationUnit) mappings
     */
    public ParseResult parseDirectory(Path rootDir) throws IOException {
        rootDir = rootDir.toAbsolutePath().normalize();
        // --- 1. Collect source files via FileVisitor ---
        JavaFileCollector collector = new JavaFileCollector(config);
        Files.walkFileTree(rootDir, collector);

        List<Path> javaFiles = collector.getJavaFiles();

        List<Path> packagedFiles = new ArrayList<>();
        List<Path> defaultPackageFiles = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            if (declaresPackage(javaFile)) {
                packagedFiles.add(javaFile);
            } else {
                defaultPackageFiles.add(javaFile);
            }
        }

        Map<Path, ParseEntry> entriesByAbsolutePath = new LinkedHashMap<>();
        parseFiles(rootDir, rootDir, packagedFiles, entriesByAbsolutePath);
        for (Path defaultPackageFile : defaultPackageFiles) {
            parseFiles(rootDir,
                    defaultPackageFile.getParent().toAbsolutePath().normalize(),
                    List.of(defaultPackageFile),
                    entriesByAbsolutePath);
        }

        List<ParseEntry> entries = javaFiles.stream()
                .map(path -> entriesByAbsolutePath.get(path.toAbsolutePath().normalize()))
                .filter(Objects::nonNull)
                .toList();

        return new ParseResult(rootDir, entries);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Returns the configuration this parser was constructed with. */
    public ParserConfig config() {
        return config;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void deleteQuietly(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    private void parseFiles(Path projectRoot,
                            Path parserSourceRoot,
                            List<Path> javaFiles,
                            Map<Path, ParseEntry> entriesByAbsolutePath) {
        if (javaFiles.isEmpty()) {
            return;
        }

        String[] sourcePathArray = javaFiles.stream()
                .map(p -> p.toAbsolutePath().normalize().toString())
                .toArray(String[]::new);
        String[] encodings = Arrays.stream(sourcePathArray)
                .map(p -> config.encodingFor(Path.of(p)))
                .toArray(String[]::new);
        ASTParser parser = ASTParserFactory.create(parserSourceRoot, sourcePathArray, config);

        parser.createASTs(
                sourcePathArray,
                encodings,
                new String[0],
                new FileASTRequestor() {
                    @Override
                    public void acceptAST(String sourceFilePath, CompilationUnit cu) {
                        Path absolute = Path.of(sourceFilePath).toAbsolutePath().normalize();
                        Path relative = projectRoot.relativize(absolute);
                        cu.setProperty(ParseEntry.SOURCE_FILE_NAME_PROPERTY, relative.getFileName().toString());
                        entriesByAbsolutePath.put(absolute, new ParseEntry(relative, absolute, cu));
                    }
                },
                null
        );
    }

    private static boolean declaresPackage(Path sourceFile) throws IOException {
        for (String line : Files.readAllLines(sourceFile)) {
            String trimmed = line.stripLeading();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue;
            }
            return trimmed.startsWith("package ");
        }
        return false;
    }
}
