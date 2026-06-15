package core.utils;

public final class FilePath {
    public static final String PATH_TO_TOOL_OUTPUT = "src" + java.io.File.separator + "main" + java.io.File.separator + "java" + java.io.File.separator + "core" + java.io.File.separator + "output";
    public static final String CLONED_PROJECT_PACKAGE = "clone";
    public static final String TEST_DRIVER_EXEC_OUTPUT_PACKAGE = "TestDriverExecOutput";
    public static final String TEST_DRIVER_EXEC_OUTPUT_FILE = "ExecOutput.txt";
    public static final String PATH_TO_CLONED_PROJECT = PATH_TO_TOOL_OUTPUT + java.io.File.separator + CLONED_PROJECT_PACKAGE;
    public static final String PATH_TO_TEST_DRIVER_EXEC_OUTPUT_FILE = PATH_TO_TOOL_OUTPUT + java.io.File.separator +
            TEST_DRIVER_EXEC_OUTPUT_PACKAGE + java.io.File.separator + TEST_DRIVER_EXEC_OUTPUT_FILE;
    public static final String PATH_TO_TEST_DRIVER = PATH_TO_TOOL_OUTPUT + java.io.File.separator + "testDriver" + java.io.File.separator + "TestDriver.java";
    public static final String RAM_STORAGE_CLASS_IMPORT = "core.TestGeneration.result.RamStorage";
    public static final String MARKED_STATEMENT_METHOD_IMPORT =
            "core.TestGeneration.path.MarkedPath.markOneStatement";
    public static final String TEST_DRIVER_FILE_PACKAGE_LOCATION = "core.output.testDriver";
    public static final String CLONED_PROJECT_ROOT_PACKAGE = "core.output.clone";
    public static final String JCIA_PROJECT_ROOT_PATH = System.getProperty("user.dir");
    public static final String MAVEN_TARGET_CLASSES_FOLDER = "target" + java.io.File.separator + "classes";
    public static final String PATH_TO_MAVEN_TARGET_CLASSES =
            JCIA_PROJECT_ROOT_PATH + java.io.File.separator + MAVEN_TARGET_CLASSES_FOLDER;
}
