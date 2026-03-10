package core.TestGeneration.testDriver;

import core.utils.FilePath;

import javax.script.Compilable;
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
import core.utils.Compiler;

public class TestDriverRunner {
    private static boolean isCompiled = false;
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
            System.out.println("Error running test driver: " + e.getMessage());
            throw new RuntimeException("Failed to run test driver in-process", e);
        }
    }


    /**
     * Resets the compilation state. Call this when a new test driver is generated.
     */
    public static void reset() {
        isCompiled = false;
    }

    private static void compileTestDriver(String testDriverPath) throws Exception {
        try {
            Compiler.getInstance().compileJavaFile(testDriverPath, FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile test driver", e);
        }
    }

    private static void invokeTestDriverMain(String[] args) throws Exception {
        String mainClassName = FilePath.TEST_DRIVER_FILE_PACKAGE_LOCATION + ".TestDriver";

        File classesDir = new File(FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        URL[] urls = {classesDir.toURI().toURL()};


        ClassLoader parent = TestDriverRunner.class.getClassLoader();
        try (URLClassLoader loader = new URLClassLoader(urls, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(FilePath.CLONED_PROJECT_ROOT_PACKAGE)
                        || name.startsWith(FilePath.TEST_DRIVER_FILE_PACKAGE_LOCATION)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> c = findLoadedClass(name);
                        if (c == null) {
                            try {
                                c = findClass(name);
                            } catch (ClassNotFoundException e) {
                                c = super.loadClass(name, resolve);
                            }
                        }
                        if (resolve) {
                            resolveClass(c);
                        }
                        return c;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> mainClass = loader.loadClass(mainClassName);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        }
    }
}
