package core.generation;

import core.cfg.Coverage;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import core.testpath.AllPathsFinder;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * BatchRunner — entry point for batch concolic testing via Maven exec plugin.
 *
 * Usage (via Maven):
 *   mvn exec:java -Dexec.mainClass=core.generation.BatchRunner
 *                 -Dmode=BITVECTOR
 *                 -Dcoverage=STATEMENT
 *                 -DzipPath="d:\CT4J\emLoc.zip"
 *                 [-DmaxMethods=N]
 *                 [-DtimeoutSeconds=90]
 *
 * Results are written to concolic-results/ (one JSON file per method, 3 runs each).
 */
public class BatchRunner {

    public static void main(String[] args) throws Exception {
        // --- Read parameters from System Properties ---
        String zipPathStr    = System.getProperty("zipPath",        "d:\\CT4J\\emLoc.zip");
        String modeStr       = System.getProperty("mode",           "BITVECTOR").toUpperCase();
        String coverageStr   = System.getProperty("coverage",       "STATEMENT").toUpperCase();
        int    maxMethods    = Integer.parseInt(System.getProperty("maxMethods", "-1"));
        int    timeoutSec    = Integer.parseInt(System.getProperty("timeoutSeconds", "90"));

        System.out.println("[BatchRunner] zipPath    = " + zipPathStr);
        System.out.println("[BatchRunner] mode       = " + modeStr);
        System.out.println("[BatchRunner] coverage   = " + coverageStr);
        System.out.println("[BatchRunner] maxMethods = " + (maxMethods < 0 ? "ALL" : maxMethods));
        System.out.println("[BatchRunner] timeout    = " + timeoutSec + "s");

        // --- Validate ---
        Path zipPath = Paths.get(zipPathStr);
        if (!zipPath.toFile().exists()) {
            System.err.println("[BatchRunner] ERROR: zip not found: " + zipPath);
            System.exit(1);
        }

        Z3EncodingMode encodingMode;
        try {
            encodingMode = Z3EncodingMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            System.err.println("[BatchRunner] ERROR: unknown mode: " + modeStr + " (use BITVECTOR or LEGACY_INT_REAL)");
            System.exit(1);
            return;
        }

        Coverage coverage;
        try {
            coverage = Coverage.valueOf(coverageStr);
        } catch (IllegalArgumentException e) {
            System.err.println("[BatchRunner] ERROR: unknown coverage: " + coverageStr + " (use STATEMENT, BRANCH, MCDC)");
            System.exit(1);
            return;
        }

        // --- Load project ---
        System.out.println("[BatchRunner] Loading project...");
        Project project = new Project(zipPath);
        List<MethodDeclaration> methods = new java.util.ArrayList<>(project.getMethods());
        int total = (maxMethods > 0) ? Math.min(maxMethods, methods.size()) : methods.size();
        System.out.println("[BatchRunner] Found " + methods.size() + " methods. Will run: " + total);

        // --- Results dir ---
        Path resultsDir = Paths.get("src", "main", "java", "core", "output", "concolic-results");
        Files.createDirectories(resultsDir);

        // --- Run each method 3 times ---
        int ok = 0, errors = 0, timeouts = 0;

        for (int i = 0; i < total; i++) {
            MethodDeclaration method = methods.get(i);
            String methodName = method.getName().getIdentifier();

            // Delete old JSON for this method so we get a clean 3-run array
            File oldJson = resultsDir.resolve(safeFileName(methodName) + ".json").toFile();
            if (oldJson.exists()) {
                oldJson.delete();
            }

            System.out.printf("[%d/%d] %s ...%n", i + 1, total, methodName);

            CompilationUnit cu = project.getRootAST(method);
            if (cu == null) {
                System.err.println("  -> SKIP: no root AST");
                errors++;
                continue;
            }

            boolean methodSuccess = true;
            for (int run = 1; run <= 3; run++) {
                System.out.printf("  -> Run %d/3 ...%n", run);

                final MethodDeclaration finalMethod = method;
                final CompilationUnit finalCu = cu;
                final Coverage finalCoverage = coverage;
                final Z3EncodingMode finalMode = encodingMode;

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<?> future = executor.submit(() -> {
                    try {
                        Map<String, Object> randomInput = RandomTestInput.createRandomTestData(finalMethod);
                        ConcolicTesting.getInstance().generate(
                                finalMethod,
                                finalCu,
                                finalCoverage,
                                randomInput,
                                new AllPathsFinder(),
                                finalMode
                        );
                    } catch (Exception e) {
                        System.err.println("  -> Run error: " + e.getMessage());
                    }
                });

                executor.shutdown();
                try {
                    future.get(timeoutSec, TimeUnit.SECONDS);
                    System.out.printf("  -> Run %d/3 OK%n", run);
                } catch (TimeoutException te) {
                    future.cancel(true);
                    executor.shutdownNow();
                    System.err.printf("  -> Run %d/3 TIMEOUT (>%ds)%n", run, timeoutSec);
                    methodSuccess = false;
                    timeouts++;
                    break; // skip remaining runs for this method
                } catch (Exception e) {
                    System.err.printf("  -> Run %d/3 ERROR: %s%n", run, e.getMessage());
                    methodSuccess = false;
                    errors++;
                    break;
                }
            }

            if (methodSuccess) ok++;
        }

        System.out.println();
        System.out.printf("[BatchRunner] DONE  mode=%s  coverage=%s%n", modeStr, coverageStr);
        System.out.printf("[BatchRunner] OK=%d  TIMEOUT=%d  ERROR=%d  /  TOTAL=%d%n",
                ok, timeouts, errors, total);
        
        // Bắt buộc JVM đóng hoàn toàn (để tránh các luồng non-daemon của Z3/TestDriver treo máy)
        System.exit(0);
    }

    private static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
