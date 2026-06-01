package core.parser;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * Immutable configuration for {@link JavaProjectParser}.
 *
 * <p>Build via {@link #builder()} or grab sensible defaults with {@link #defaults()}.
 *
 * <pre>{@code
 * // Minimal – identical behaviour to the old hard-coded factory
 * ParserConfig cfg = ParserConfig.defaults();
 *
 * // Fully custom
 * ParserConfig cfg = ParserConfig.builder()
 *     .javaVersion("11")
 *     .sourceEncoding("ISO-8859-1")
 *     .addClasspathEntry(Path.of("lib/guava.jar"))
 *     .excludeDirectory("target")
 *     .excludeDirectory("build")
 *     .resolveBindings(false)
 *     .build();
 * }</pre>
 */
public final class ParserConfig {

    // -----------------------------------------------------------------------
    // Known JLS levels accepted by JDT (source-version string → AST level)
    // -----------------------------------------------------------------------
    private static final Set<String> SUPPORTED_VERSIONS = Set.of(
            "1.1", "1.2", "1.3", "1.4", "1.5", "5",
            "1.6", "6", "1.7", "7", "1.8", "8",
            "9", "10", "11", "12", "13", "14", "15", "16",
            "17", "18", "19", "20", "21", "22", "23"
    );

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final String javaVersion;
    private final String sourceEncoding;
    private final boolean resolveBindings;
    private final boolean bindingsRecovery;
    private final boolean statementsRecovery;
    private final boolean includeVMBootclasspath;
    private final List<Path> classpathEntries;
    private final List<Path> additionalSourceRoots;
    private final Set<String> excludedDirectories;
    private final Predicate<Path> fileFilter;
    private final Map<Path, String> perFileEncodings;

    private ParserConfig(Builder b) {
        this.javaVersion            = b.javaVersion;
        this.sourceEncoding         = b.sourceEncoding;
        this.resolveBindings        = b.resolveBindings;
        this.bindingsRecovery       = b.bindingsRecovery;
        this.statementsRecovery     = b.statementsRecovery;
        this.includeVMBootclasspath = b.includeVMBootclasspath;
        this.classpathEntries       = List.copyOf(b.classpathEntries);
        this.additionalSourceRoots  = List.copyOf(b.additionalSourceRoots);
        this.excludedDirectories    = Set.copyOf(b.excludedDirectories);
        this.fileFilter             = b.fileFilter;
        this.perFileEncodings       = Map.copyOf(b.perFileEncodings);
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /** Returns a config identical to the previous hard-coded behaviour. */
    public static ParserConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Java source / compliance version, e.g. {@code "17"}. */
    public String javaVersion()             { return javaVersion; }

    /** Default file encoding, e.g. {@code "UTF-8"}. */
    public String sourceEncoding()          { return sourceEncoding; }

    public boolean resolveBindings()        { return resolveBindings; }
    public boolean bindingsRecovery()       { return bindingsRecovery; }
    public boolean statementsRecovery()     { return statementsRecovery; }
    public boolean includeVMBootclasspath() { return includeVMBootclasspath; }

    /** Additional jars / class-output directories for binding resolution. */
    public List<Path> classpathEntries()    { return classpathEntries; }

    /** Source roots beyond the project root (for multi-module projects). */
    public List<Path> additionalSourceRoots() { return additionalSourceRoots; }

    /**
     * Directory names (not paths) that should be skipped during the file walk.
     * Hidden directories (names starting with {@code .}) are always skipped regardless.
     */
    public Set<String> excludedDirectories() { return excludedDirectories; }

    /**
     * Optional extra predicate applied to each candidate {@code .java} file.
     * Return {@code true} to include, {@code false} to skip.
     */
    public Predicate<Path> fileFilter()     { return fileFilter; }

    /**
     * Per-file encoding overrides; key is the <em>absolute</em> path of the file.
     * Falls back to {@link #sourceEncoding()} when a file is absent from this map.
     */
    public Map<Path, String> perFileEncodings() { return perFileEncodings; }

    /**
     * Resolve the effective encoding for a single file.
     *
     * @param absolutePath absolute path of the .java file
     * @return per-file override if present, otherwise {@link #sourceEncoding()}
     */
    public String encodingFor(Path absolutePath) {
        return perFileEncodings.getOrDefault(absolutePath.toAbsolutePath(), sourceEncoding);
    }

    @Override
    public String toString() {
        return "ParserConfig{" +
               "javaVersion=" + javaVersion +
               ", encoding=" + sourceEncoding +
               ", resolveBindings=" + resolveBindings +
               ", excludedDirs=" + excludedDirectories +
               ", classpathEntries=" + classpathEntries.size() +
               ", additionalSourceRoots=" + additionalSourceRoots.size() +
               '}';
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    public static final class Builder {

        private String javaVersion            = "17";
        private String sourceEncoding         = "UTF-8";
        private boolean resolveBindings       = true;
        private boolean bindingsRecovery      = true;
        private boolean statementsRecovery    = true;
        private boolean includeVMBootclasspath = true;
        private final List<Path>         classpathEntries      = new ArrayList<>();
        private final List<Path>         additionalSourceRoots = new ArrayList<>();
        private final Set<String>        excludedDirectories   = new LinkedHashSet<>();
        private Predicate<Path>          fileFilter            = p -> true;
        private final Map<Path, String>  perFileEncodings      = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Java source / compliance version (e.g. {@code "8"}, {@code "11"}, {@code "17"}).
         * Must be a version string recognised by JDT.
         */
        public Builder javaVersion(String version) {
            Objects.requireNonNull(version, "javaVersion must not be null");
            this.javaVersion = version.trim();
            return this;
        }

        /** Default charset for all source files (e.g. {@code "UTF-8"}, {@code "ISO-8859-1"}). */
        public Builder sourceEncoding(String encoding) {
            this.sourceEncoding = Objects.requireNonNull(encoding, "sourceEncoding");
            return this;
        }

        /**
         * Whether JDT should resolve type bindings across files.
         * Disable for a significant speed-up when you only need structural AST info.
         */
        public Builder resolveBindings(boolean resolve) {
            this.resolveBindings = resolve;
            // Binding recovery only makes sense when resolving
            if (!resolve) this.bindingsRecovery = false;
            return this;
        }

        /** Continue even when some bindings can't be resolved (default {@code true}). */
        public Builder bindingsRecovery(boolean recovery) {
            this.bindingsRecovery = recovery;
            return this;
        }

        /** Try to recover a partial AST despite statement-level syntax errors (default {@code true}). */
        public Builder statementsRecovery(boolean recovery) {
            this.statementsRecovery = recovery;
            return this;
        }

        /** Include the running JVM's boot classpath for binding resolution (default {@code true}). */
        public Builder includeVMBootclasspath(boolean include) {
            this.includeVMBootclasspath = include;
            return this;
        }

        /** Append a single jar or class-output directory to the classpath. */
        public Builder addClasspathEntry(Path entry) {
            classpathEntries.add(Objects.requireNonNull(entry, "classpathEntry"));
            return this;
        }

        /** Append multiple classpath entries at once. */
        public Builder addClasspathEntries(Collection<Path> entries) {
            entries.forEach(this::addClasspathEntry);
            return this;
        }

        /** Add an extra source root (for multi-module projects). */
        public Builder addSourceRoot(Path root) {
            additionalSourceRoots.add(Objects.requireNonNull(root, "sourceRoot"));
            return this;
        }

        /**
         * Add a directory <em>name</em> (not a full path) to skip during the file walk.
         * Example: {@code "target"}, {@code "build"}, {@code "generated-sources"}.
         */
        public Builder excludeDirectory(String dirName) {
            excludedDirectories.add(Objects.requireNonNull(dirName, "dirName"));
            return this;
        }

        /** Add multiple directory names to exclude. */
        public Builder excludeDirectories(Collection<String> dirNames) {
            dirNames.forEach(this::excludeDirectory);
            return this;
        }

        /**
         * Additional file-level predicate applied after the {@code .java} glob check.
         * Multiple calls <em>AND</em> the predicates together.
         */
        public Builder fileFilter(Predicate<Path> filter) {
            Objects.requireNonNull(filter, "fileFilter");
            this.fileFilter = this.fileFilter.and(filter);
            return this;
        }

        /** Override the encoding for a specific file (keyed by absolute path). */
        public Builder encodingFor(Path absoluteFilePath, String encoding) {
            perFileEncodings.put(
                    absoluteFilePath.toAbsolutePath(),
                    Objects.requireNonNull(encoding, "encoding"));
            return this;
        }

        /**
         * Build and validate the configuration.
         *
         * @throws IllegalArgumentException if any field has an invalid value
         */
        public ParserConfig build() {
            validate();
            return new ParserConfig(this);
        }

        private void validate() {
            if (!SUPPORTED_VERSIONS.contains(javaVersion)) {
                throw new IllegalArgumentException(
                        "Unsupported javaVersion '" + javaVersion +
                        "'. Supported: " + SUPPORTED_VERSIONS);
            }
            if (sourceEncoding == null || sourceEncoding.isBlank()) {
                throw new IllegalArgumentException("sourceEncoding must not be blank");
            }
            // Validate charset is actually supported by the JVM
            try {
                java.nio.charset.Charset.forName(sourceEncoding);
            } catch (java.nio.charset.UnsupportedCharsetException e) {
                throw new IllegalArgumentException(
                        "Unknown charset '" + sourceEncoding + "'", e);
            }
        }
    }
}