package core.testdriver;

import core.instrument.InstrumentationFactory;
import core.utils.Compiler;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a standalone Java {@code DriverMain} class that reads method
 * arguments from a JSON file and invokes the instrumented method.
 *
 * <h3>Usage in the pipeline</h3>
 * <pre>{@code
 * InstrumentationFactory.InstrumentationProduct product =
 *         new InstrumentationFactory().produce(cu, cfg, coverage);
 *
 * TestDriverGeneration.generate(product, methodDeclaration);
 * // → writes  <instrumentedDir>/DriverMain.java  next to instrumented source
 * }</pre>
 *
 * <h3>Generated driver CLI</h3>
 * <pre>
 * java DriverMain --input=args.json
 * </pre>
 * Exit codes: 2 = missing {@code --input}, 3 = JSON parse failure,
 * 4 = missing/wrong-typed parameter, 5 = invocation failure.
 */
public final class TestDriver {

    private TestDriver() {}

    private static volatile String currentDriverFqn;

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Generate a driver for {@code method} alongside the instrumented source
     * contained in {@code product}.
     *
     * @param product result of {@link InstrumentationFactory#produce}; supplies
     *                the output directory and the class name to call
     * @param method  the method under test (must have been compiled into the
     *                instrumented class in {@code product})
     * @throws IOException if the driver file cannot be written
     * @throws Exception if the generated driver cannot be compiled
     */
    public static void generate(InstrumentationFactory.InstrumentationProduct product,
                                MethodDeclaration method) throws Exception {

        List<ParamInfo> params = extractParams(method);
        String className = resolveClassName(method);

        // instrumentProductPath is the full path to the instrumented .java file,
        // e.g. /out/cloned/com/example/Foo.java.
        // The driver is written into the same directory.
        Path instrumentedFile = product.instrumentProductPath();
        Path outputDir = instrumentedFile.getParent();

        // Derive the package name from the instrumented file's first line so we
        // don't need a separate accessor on the record.
        String packageName = readPackageName(instrumentedFile);

        String driverSource = TestDriverEmitter.emitDriver(
                packageName,
                className,
                method.getName().getIdentifier(),
                params,
                returnTypeName(method)
        );

        Path driverFile = outputDir.resolve("DriverMain.java");
        Files.createDirectories(outputDir);
        Files.writeString(driverFile, driverSource, StandardCharsets.UTF_8);
        Compiler.getInstance().compileJavaFile(
                driverFile.toString(),
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES
        );
        currentDriverFqn = driverFqn(packageName);
    }

    // -----------------------------------------------------------------------
    // Run driver subprocesses and collect TestData
    // -----------------------------------------------------------------------

    /**
     * Execute the already-compiled {@code DriverMain} once for every input
     * map in {@code inputs}, collecting a {@link TestData} per run.
     *
     * <p>For each input the sequence is:
     * <ol>
     *   <li>Serialise the input map to a temp {@code input-N.json} file.</li>
     *   <li>Launch the driver subprocess with
     *       {@code --input=<input-N.json> --output=<result-N.json>}.</li>
     *   <li>The driver internally does:
     *       {@code TraceRecorder.reset() → method.invoke() → TraceRecorder.hitNodes()}
     *       and writes the result JSON atomically before exiting.</li>
     *   <li>Read the result JSON back via {@link TestData#fromJson}, which
     *       reconstructs typed inputs and the per-run coverage snapshot.</li>
     * </ol>
     *
     * <p>Each {@link TestData} in the returned list therefore contains the
     * coverage of <em>only its own</em> run — the reset before each invocation
     * prevents the additive accumulation that would occur if a single
     * {@code CoverageTracker} were shared across runs.
     *
     * @param classesDir   directory containing the compiled {@code DriverMain}
     *                     and instrumented class files
     * @param params       parameter metadata (same list used to generate the driver)
     * @param inputs       one input map per test case, keyed by parameter name
     * @param workDir      scratch directory for temp JSON files
     * @return one completed {@link TestData} per entry in {@code inputs},
     *         in the same order
     * @throws IOException          if a temp file cannot be written or read
     * @throws InterruptedException if a subprocess is interrupted
     * @throws DriverException      if any driver subprocess exits with a non-zero code
     */
    public static List<TestData> runAll(Path classesDir,
                                        List<ParamInfo> params,
                                        List<Map<String, Object>> inputs,
                                        Path workDir)
            throws IOException, InterruptedException {

        Files.createDirectories(workDir);
        ObjectMapper mapper = new ObjectMapper();

        List<TestData> results = new ArrayList<>(inputs.size());

        for (int i = 0; i < inputs.size(); i++) {
            Map<String, Object> inputMap = inputs.get(i);

            // ── 1. Write typed input map to JSON ─────────────────────────────
            // We need a plain JSON object keyed by param name.  Jackson can
            // serialise boxed primitives and arrays natively.
            Path inputFile  = workDir.resolve("input-" + i + ".json");
            Path outputFile = workDir.resolve("result-" + i + ".json");

            ObjectNode inputJson = mapper.createObjectNode();
            for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                inputJson.set(entry.getKey(),
                        mapper.valueToTree(entry.getValue()));
            }
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(inputFile.toFile(), inputJson);

            // ── 2. Launch driver subprocess ───────────────────────────────────
            String javaExe = ProcessHandle.current().info().command().orElse("java");
            String cp = classesDir + java.io.File.pathSeparator
                      + System.getProperty("java.class.path");

            // Derive the fully-qualified DriverMain name from the classesDir
            // convention: classes are laid out as <classesDir>/<pkg/path>/DriverMain.class
            // We detect the package from any compiled .class that lives one level up.
            String driverFqn = resolveDriverFqn(classesDir);

            Process proc = new ProcessBuilder(
                    javaExe,
                    "-cp", cp,
                    driverFqn,
                    "--input="  + inputFile.toAbsolutePath(),
                    "--output=" + outputFile.toAbsolutePath())
                    .redirectErrorStream(false)
                    .start();

            String stderr   = new String(proc.getErrorStream().readAllBytes(),
                                         StandardCharsets.UTF_8);
            int    exitCode = proc.waitFor();

            if (exitCode != 0) {
                throw new DriverException(i, exitCode, stderr);
            }

            // ── 3. Read result JSON → TestData ────────────────────────────────
            TestData td = TestData.fromJson(outputFile, params);

            results.add(td);
        }

        return results;
    }

    public static TestData run(Path classesDir,
                               List<ParamInfo> params,
                               Map<String, Object> inputs,
                               Path workDir) throws IOException, InterruptedException {
        Files.createDirectories(workDir);
        ObjectMapper mapper = new ObjectMapper();
        Path inputFile = workDir.resolve("inputs" + ".json");
        Path outputFile = workDir.resolve("result" + ".json");
        ObjectNode inputJson = mapper.createObjectNode();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            inputJson.set(entry.getKey(),
                    mapper.valueToTree(entry.getValue()));
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(inputFile.toFile(), inputJson);

        // ── 2. Launch driver subprocess ───────────────────────────────────
        String javaExe = ProcessHandle.current().info().command().orElse("java");
        String cp = classesDir + java.io.File.pathSeparator
                + System.getProperty("java.class.path");

        // Derive the fully-qualified DriverMain name from the classesDir
        // convention: classes are laid out as <classesDir>/<pkg/path>/DriverMain.class
        // We detect the package from any compiled .class that lives one level up.
        String driverFqn = resolveDriverFqn(classesDir);

        Process proc = new ProcessBuilder(
                javaExe,
                "-cp", cp,
                driverFqn,
                "--input="  + inputFile.toAbsolutePath(),
                "--output=" + outputFile.toAbsolutePath())
                .redirectErrorStream(false)
                .start();

        String stderr   = new String(proc.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8);
        int    exitCode = proc.waitFor();

        if (exitCode != 0) {
            throw new DriverExceptionSimple(exitCode, stderr);
        }

        // ── 3. Read result JSON → TestData ────────────────────────────────
        return TestData.fromJson(outputFile, params);
    }
    // -----------------------------------------------------------------------
    // DriverException
    // -----------------------------------------------------------------------

    /**
     * Thrown by {@link #runAll} when the driver subprocess exits non-zero.
     */
    public static final class DriverException extends RuntimeException {

        private final int runIndex;
        private final int exitCode;

        public DriverException(int runIndex, int exitCode, String stderr) {
            super("Driver run #" + runIndex + " failed with exit code " + exitCode
                    + ":\n" + stderr);
            this.runIndex = runIndex;
            this.exitCode = exitCode;
        }

        public int runIndex() { return runIndex; }
        public int exitCode() { return exitCode; }
    }

    public static final class DriverExceptionSimple extends RuntimeException {
        private final int exitCode;
        public DriverExceptionSimple(int exitCode, String message) {
            super("Driver run failed with exit code " + exitCode + ": " + message);
            this.exitCode = exitCode;
        }
        public int exitCode() { return exitCode; }
    }

    // -----------------------------------------------------------------------
    // Metadata extraction
    // -----------------------------------------------------------------------

    /**
     * Collects {@link ParamInfo} records from the JDT method declaration.
     */
    public static List<ParamInfo> extractParams(MethodDeclaration method) {
        List<ParamInfo> result = new ArrayList<>();
        for (Object obj : method.parameters()) {
            if (obj instanceof SingleVariableDeclaration svd) {
                String name = svd.getName().getIdentifier();
                String type = svd.getType().toString();
                // Append array dimensions declared on the variable name (varName[][])
                int extraDims = svd.getExtraDimensions();
                type = type + "[]".repeat(extraDims);
                result.add(new ParamInfo(name, type));
            }
        }
        return result;
    }

    /**
     * Simple record holding a parameter's name and JDT type string.
     *
     * @param name     parameter name as written in source
     * @param typeName JDT type string, e.g. {@code "int"}, {@code "String[]"},
     *                 {@code "double[][]"}
     */
    public record ParamInfo(String name, String typeName) {}

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String resolveClassName(MethodDeclaration method) {
        if (method.getParent() instanceof TypeDeclaration td) {
            return td.getName().getIdentifier();
        }
        throw new IllegalArgumentException(
                "MethodDeclaration parent is not a TypeDeclaration: " + method.getName());
    }

    private static String returnTypeName(MethodDeclaration method) {
        return method.getReturnType2() == null ? "void"
                : method.getReturnType2().toString();
    }

    /**
     * Reads the {@code package} declaration from the first non-blank line of
     * {@code javaFile} and returns the package name string, or {@code ""}
     * for the default package.
     */
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");

    static String readPackageName(Path javaFile) throws IOException {
        for (String line : Files.readAllLines(javaFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            Matcher m = PACKAGE_PATTERN.matcher(line);
            if (m.find()) return m.group(1);
            break;
        }
        return "";
    }

    /**
     * Walk {@code classesDir} to find the first {@code DriverMain.class} and
     * derive its fully-qualified class name from its path relative to
     * {@code classesDir}.
     *
     * <p>Example: if {@code classesDir} is {@code /out/classes} and
     * {@code DriverMain.class} lives at
     * {@code /out/classes/com/example/DriverMain.class}, this returns
     * {@code "com.example.DriverMain"}.
     */
    static String resolveDriverFqn(Path classesDir) throws IOException {
        String generatedDriverFqn = currentDriverFqn;
        if (generatedDriverFqn != null && driverClassExists(classesDir, generatedDriverFqn)) {
            return generatedDriverFqn;
        }

        try (var stream = Files.walk(classesDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals("DriverMain.class"))
                    .findFirst()
                    .map(p -> {
                        String rel = classesDir.relativize(p).toString();
                        // Convert OS path separator to '.' and strip ".class"
                        return rel.replace(java.io.File.separatorChar, '.')
                                  .replace('/', '.')
                                  .replaceAll("\\.class$", "");
                    })
                    .orElse("DriverMain"); // default package fallback
        }
    }

    private static String driverFqn(String packageName) {
        return packageName == null || packageName.isBlank()
                ? "DriverMain"
                : packageName + ".DriverMain";
    }

    private static boolean driverClassExists(Path classesDir, String driverFqn) {
        Path driverClass = classesDir.resolve(driverFqn.replace('.', java.io.File.separatorChar) + ".class");
        return Files.exists(driverClass);
    }
}
