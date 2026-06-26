package core.utils;

import java.nio.file.Path;

public final class FilePath {
    public static final String PATH_TO_TOOL_OUTPUT = Path.of("src", "main", "java", "core", "output").toString();
    public static final String CLONED_PROJECT_PACKAGE = "clone";
    public static final String TEST_DRIVER_EXEC_OUTPUT_PACKAGE = "TestDriverExecOutput";
    public static final String TEST_DRIVER_EXEC_OUTPUT_FILE = "ExecOutput.txt";
    public static final String PATH_TO_CLONED_PROJECT = Path.of(PATH_TO_TOOL_OUTPUT, CLONED_PROJECT_PACKAGE).toString();
    public static final String PATH_TO_TEST_DRIVER_EXEC_OUTPUT_FILE =
            Path.of(PATH_TO_TOOL_OUTPUT, TEST_DRIVER_EXEC_OUTPUT_PACKAGE, TEST_DRIVER_EXEC_OUTPUT_FILE).toString();
    public static final String PATH_TO_TEST_DRIVER = Path.of(PATH_TO_TOOL_OUTPUT, "testDriver", "TestDriver.java").toString();
    public static final String RAM_STORAGE_CLASS_IMPORT = "core.TestGeneration.result.RamStorage";
    public static final String MARKED_STATEMENT_METHOD_IMPORT =
            "core.TestGeneration.path.MarkedPath.markOneStatement";
    public static final String TEST_DRIVER_FILE_PACKAGE_LOCATION = "core.output.testDriver";
    public static final String CLONED_PROJECT_ROOT_PACKAGE = "core.output.clone";
    public static final String JCIA_PROJECT_ROOT_PATH = "/home/qu4ll12/Documents/CT4J";
    public static final String MAVEN_TARGET_CLASSES_FOLDER = Path.of("target", "classes").toString();
    public static final String PATH_TO_MAVEN_TARGET_CLASSES =
            Path.of(JCIA_PROJECT_ROOT_PATH, MAVEN_TARGET_CLASSES_FOLDER).toString();
}
