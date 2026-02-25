package core.TestGeneration.testDriver;

import core.utils.FilePath;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;


public class TestDriverRunner {
    private static boolean isCompiled = false;
    private static Class<?> cachedMainClass = null;
    private static Method cachedMainMethod = null;
    private static ClassLoader cachedLoader = null;
    
    /**
     * Runs the test driver with the given test inputs as command-line arguments.
     * The test driver is compiled only once on the first call.
     */
    public static void runTestDriver(String testDriverPath, Object[] testInputs) {
        try {
            if (!isCompiled) {
                compileTestDriver(testDriverPath);
                isCompiled = true;
            }
            String[] args = TestDriverGenerator.serializeTestInputs(testInputs);
            invokeTestDriverMain(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run test driver in-process", e);
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * @deprecated Use runTestDriver(String, Object[]) instead
     */
    @Deprecated
    public static void runTestDriver(String testDriverPath) {
        runTestDriver(testDriverPath, new Object[0]);
    }
    
    /**
     * Resets the compilation state. Call this when a new test driver is generated.
     */
    public static void reset() {
        isCompiled = false;
        cachedMainClass = null;
        cachedMainMethod = null;
        cachedLoader = null;
    }

    private static void compileTestDriver(String testDriverPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "System Java compiler not available. Make sure you are running on a JDK, not a JRE.");
        }

        File sourceFile = new File(testDriverPath);
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("Test driver source file does not exist: " +
                    sourceFile.getAbsolutePath());
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                null)) {
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));

            String currentCp = System.getProperty("java.class.path");
            StringBuilder cpBuilder = new StringBuilder();
            cpBuilder.append(FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
            if (currentCp != null && !currentCp.isEmpty()) {
                cpBuilder.append(File.pathSeparator).append(currentCp);
            }
            String classpath = cpBuilder.toString();

            List<String> options = Arrays.asList(
                    "-classpath", classpath,
                    "-d", FilePath.PATH_TO_MAVEN_TARGET_CLASSES
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, null, options, null, units
            );

            Boolean success = task.call();
            if (success == null || !success) {
                throw new RuntimeException("Compilation of TestDriver.java failed");
            }
        }
    }

    private static void invokeTestDriverMain(String[] args) throws Exception {
        String mainClassName = FilePath.TEST_DRIVER_FILE_PACKAGE_LOCATION + ".TestDriver";

        // Reuse cached class loader and method if available
        if (cachedMainClass != null && cachedMainMethod != null && cachedLoader != null) {
            try {
                cachedMainMethod.invoke(null, (Object) args);
                return;
            } catch (Exception e) {
                // If invocation fails, reset and reload
                reset();
            }
        }

        // Create a class loader pointing at the compiled classes directory.
        // Since we compile once and reuse, we don't need the complex reloading logic.
        File classesDir = new File(FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        URL[] urls = {classesDir.toURI().toURL()};

        ClassLoader parent = TestDriverRunner.class.getClassLoader();
        ClassLoader loader = new URLClassLoader(urls, parent);

        Class<?> mainClass = loader.loadClass(mainClassName);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        
        // Cache for future use
        cachedMainClass = mainClass;
        cachedMainMethod = mainMethod;
        cachedLoader = loader;
        
        mainMethod.invoke(null, (Object) args);
    }
}
