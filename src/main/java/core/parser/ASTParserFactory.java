package core.parser;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Factory that creates a properly configured {@link ASTParser} driven by a {@link ParserConfig}.
 *
 * <p>All previously hard-coded values (Java version, encoding, classpath, source roots,
 * binding flags) are now resolved from the supplied config.
 */
public final class ASTParserFactory {

    private ASTParserFactory() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Build an {@link ASTParser} with settings taken entirely from {@code config}.
     *
     * @param projectRoot     root directory of the Java project (always included as a source root)
     * @param sourceFilePaths absolute paths of all {@code .java} files to be parsed
     * @param config          dynamic configuration; use {@link ParserConfig#defaults()} for
     *                        behaviour identical to the previous hard-coded factory
     */
    public static ASTParser create(Path projectRoot,
                                   String[] sourceFilePaths,
                                   ParserConfig config) {

        ASTParser parser = ASTParser.newParser(jlsLevel(config.javaVersion()));

        // --- Compiler options ---
        Map<String, String> options = new Hashtable<>(JavaCore.getDefaultOptions());
        String ver = normalizeVersion(config.javaVersion());
        options.put(JavaCore.COMPILER_SOURCE,                    ver);
        options.put(JavaCore.COMPILER_COMPLIANCE,                ver);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,   ver);
        parser.setCompilerOptions(options);

        // --- Classpath entries ---
        String[] classpath = config.classpathEntries().stream()
                .map(p -> p.toAbsolutePath().toString())
                .toArray(String[]::new);

        // --- Source roots: project root + any additional roots from config ---
        String[] sourceRoots = Stream.concat(
                        Stream.of(projectRoot.toAbsolutePath().toString()),
                        config.additionalSourceRoots().stream()
                              .map(p -> p.toAbsolutePath().toString()))
                .distinct()
                .toArray(String[]::new);

        String[] sourceRootEncodings = new String[sourceRoots.length];
        Arrays.fill(sourceRootEncodings, config.sourceEncoding());

        parser.setEnvironment(
                classpath,
                sourceRoots,
                sourceRootEncodings,
                config.includeVMBootclasspath()
        );

        // --- Parser settings (all driven by config) ---
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(config.resolveBindings());
        parser.setBindingsRecovery(config.bindingsRecovery());
        parser.setStatementsRecovery(config.statementsRecovery());

        return parser;
    }

    /**
     * Convenience overload using {@link ParserConfig#defaults()} — keeps call sites
     * that haven't adopted config yet compiling without changes.
     *
     * @deprecated Prefer {@link #create(Path, String[], ParserConfig)}.
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public static ASTParser create(Path projectRoot, String[] sourceFilePaths) {
        return create(projectRoot, sourceFilePaths, ParserConfig.defaults());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Map a user-visible version string to the correct {@code AST.JLS_*} constant.
     * Falls back to {@link AST#JLS_Latest} for anything >= 22 or unrecognised.
     */
    private static int jlsLevel(String version) {
        return switch (normalizeVersion(version)) {
            case "1.2" -> AST.JLS2;
            case "1.3" -> AST.JLS3;
            case "1.4" -> AST.JLS4;
            case "1.5", "1.6", "1.7", "1.8" -> AST.JLS8;
            case "9"   -> AST.JLS9;
            case "10"  -> AST.JLS10;
            case "11"  -> AST.JLS11;
            case "12"  -> AST.JLS12;
            case "13"  -> AST.JLS13;
            case "14"  -> AST.JLS14;
            case "15"  -> AST.JLS15;
            case "16"  -> AST.JLS16;
            case "17"  -> AST.JLS17;
            case "18"  -> AST.JLS18;
            case "19"  -> AST.JLS19;
            default    -> AST.JLS_Latest;
        };
    }

    /**
     * Normalize shorthand versions to the canonical form JDT expects
     * as a compiler-option value.
     * Examples: {@code "8"} → {@code "1.8"}, {@code "5"} → {@code "1.5"},
     *           {@code "17"} → {@code "17"} (unchanged for 9+).
     */
    static String normalizeVersion(String version) {
        return switch (version.trim()) {
            case "5"  -> "1.5";
            case "6"  -> "1.6";
            case "7"  -> "1.7";
            case "8"  -> "1.8";
            default   -> version.trim();
        };
    }

    /**
     * Build a parser for a SINGLE source file with correct unit-name binding.
     * Use this instead of {@link #create} when calling {@code createAST(monitor)}
     * rather than the batch {@code createASTs(...)}.
     *
     * @param projectRoot root directory (used as the source root)
     * @param sourceFile  absolute path of the single {@code .java} file
     * @param source      file contents as a char array
     * @param config      parser configuration
     */
    public static ASTParser createSingle(Path projectRoot,
                                         Path sourceFile,
                                         char[] source,
                                         ParserConfig config) {

        ASTParser parser = create(projectRoot, new String[]{sourceFile.toAbsolutePath().toString()}, config);

        parser.setSource(source);

        // Unit name must be the package-relative path with forward slashes,
        // e.g. "com/example/util/Foo.java"
        // JDT uses this to construct binding keys — without it, ITypeBinding
        // and IMethodBinding resolve to null even with bindingsRecovery=true.
        String unitName = toUnitName(projectRoot, sourceFile);
        parser.setUnitName(unitName);

        return parser;
    }

    /**
     * Derive the JDT unit name from a source file path.
     * Result is always forward-slash-separated and ends in ".java",
     * e.g. {@code "com/example/Foo.java"}.
     */
    static String toUnitName(Path sourceRoot, Path sourceFile) {
        Path relative = sourceRoot.toAbsolutePath()
                .relativize(sourceFile.toAbsolutePath());
        // Normalise Windows back-slashes → forward slashes
        return relative.toString().replace('\\', '/');
    }
}