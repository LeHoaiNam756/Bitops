package core.parser;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.nio.file.Path;

/**
 * Immutable data record pairing a source file path with its parsed JDT {@link CompilationUnit}.
 *
 * @param relativePath    path relative to the project root  (e.g. {@code src/main/java/Foo.java})
 * @param absolutePath    absolute path on disk
 * @param compilationUnit the parsed JDT AST root node
 */
public record ParseEntry(
        Path relativePath,
        Path absolutePath,
        CompilationUnit compilationUnit
) {
    public static final String SOURCE_FILE_NAME_PROPERTY = "ct4j.sourceFileName";

    /**
     * Convenience: return the simple file name (e.g. {@code Foo.java}).
     */
    public String fileName() {
        return absolutePath.getFileName().toString();
    }

    /**
     * Returns the number of JDT parse errors recorded in the {@link CompilationUnit}.
     */
    public int errorCount() {
        return compilationUnit.getProblems().length;
    }

    @Override
    public String toString() {
        return "ParseEntry{relative=" + relativePath + ", errors=" + errorCount() + "}";
    }
}
