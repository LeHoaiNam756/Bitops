package core.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JavaFileCollector} using JUnit4 and Mockito.
 *
 * The tests focus on the visitor callback behaviour without performing an
 * actual file‑system walk.  The global {@code PATH_MATCHER} is used as‑is
 * (no mocking) to keep the test close to reality.
 */
@RunWith(MockitoJUnitRunner.class)
public class JavaFileCollectorTest {

    @Mock
    private ParserConfig config;

    @Mock
    private BasicFileAttributes attrs;

    // Path mocks for directories and files
    @Mock
    private Path directoryPath;
    @Mock
    private Path directoryFileNamePath;   // returned by directoryPath.getFileName()

    @Mock
    private Path javaFilePath;
    @Mock
    private Path nonJavaFilePath;
    @Mock
    private Path hiddenFilePath;          // unused typically, just for completeness

    private JavaFileCollector collector;

    private static final HashSet<String> EXCLUDED_DIRS = new HashSet<>();

    @Before
    public void setUp() {
        // Reset the mocked config for each test
        reset(config);

        // Default stubs for config – each test may override
        when(config.excludedDirectories()).thenReturn(EXCLUDED_DIRS);
        when(config.fileFilter()).thenReturn(p -> true);

        // Common stubs for directory mock
        when(directoryPath.getFileName()).thenReturn(directoryFileNamePath);
        // directoryFileNamePath's toString() will be overridden per test

        // Behaviour of the static PATH_MATCHER – the default Sun/nio/fs/Globs matcher
        // uses path.toString(). We'll rely on the real implementation, so we just
        // need to set appropriate toString() responses.

        collector = new JavaFileCollector(config);
    }

    // ------------------------------------------------------------------------
    // Constructor tests
    // ------------------------------------------------------------------------
    @Test
    public void noArgConstructorShouldUseDefaultConfig() {
        // We cannot easily mock ParserConfig.defaults(), so we simply verify that
        // the no-arg constructor does not throw.  Behaviour is covered by other tests.
        JavaFileCollector defaultCollector = new JavaFileCollector();
        // The list should be empty
        assertTrue(defaultCollector.getJavaFiles().isEmpty());
    }

    // ------------------------------------------------------------------------
    // preVisitDirectory
    // ------------------------------------------------------------------------
    @Test
    public void shouldSkipHiddenDirectories() {
        when(directoryFileNamePath.toString()).thenReturn(".git");

        FileVisitResult result = collector.preVisitDirectory(directoryPath, attrs);
        assertThat(result, is(FileVisitResult.SKIP_SUBTREE));
    }

    @Test
    public void shouldSkipExcludedDirectories() {
        when(directoryFileNamePath.toString()).thenReturn("build");
        EXCLUDED_DIRS.clear();
        EXCLUDED_DIRS.add("build");

        FileVisitResult result = collector.preVisitDirectory(directoryPath, attrs);
        assertThat(result, is(FileVisitResult.SKIP_SUBTREE));
    }

    @Test
    public void shouldContinueForNormalDirectories() {
        when(directoryFileNamePath.toString()).thenReturn("src");
        EXCLUDED_DIRS.clear();

        FileVisitResult result = collector.preVisitDirectory(directoryPath, attrs);
        assertThat(result, is(FileVisitResult.CONTINUE));
    }

    @Test
    public void shouldHandleNullFileNameGracefully() {
        when(directoryPath.getFileName()).thenReturn(null);

        // No hidden prefix, no excluded dir (unless "" is excluded, not typical)
        EXCLUDED_DIRS.clear();
        FileVisitResult result = collector.preVisitDirectory(directoryPath, attrs);
        assertThat(result, is(FileVisitResult.CONTINUE));
    }

    // ------------------------------------------------------------------------
    // visitFile
    // ------------------------------------------------------------------------
    @Test
    public void shouldAddRegularJavaFileMatchingFilter() {
        // Arrange: a regular file ending with .java that passes the filter
        when(attrs.isRegularFile()).thenReturn(true);
        when(javaFilePath.toString()).thenReturn("MyClass.java");
        when(config.fileFilter()).thenReturn(p -> true);

        FileVisitResult result = collector.visitFile(javaFilePath, attrs);

        assertThat(result, is(FileVisitResult.CONTINUE));
        assertThat(collector.getJavaFiles(), hasItem(javaFilePath));
    }

    @Test
    public void shouldNotAddDirectory() {
        when(attrs.isRegularFile()).thenReturn(false);

        collector.visitFile(javaFilePath, attrs);
        assertTrue(collector.getJavaFiles().isEmpty());
    }

    @Test
    public void shouldNotAddNonJavaExtension() {
        when(attrs.isRegularFile()).thenReturn(true);
        when(nonJavaFilePath.toString()).thenReturn("readme.txt");

        collector.visitFile(nonJavaFilePath, attrs);
        assertTrue(collector.getJavaFiles().isEmpty());
    }

    @Test
    public void shouldNotAddWhenFilterRejects() {
        when(attrs.isRegularFile()).thenReturn(true);
        when(javaFilePath.toString()).thenReturn("Ignored.java");
        when(config.fileFilter()).thenReturn(p -> false);  // reject all

        collector.visitFile(javaFilePath, attrs);
        assertTrue(collector.getJavaFiles().isEmpty());
    }

    @Test
    public void shouldRespectCustomFilter() {
        when(attrs.isRegularFile()).thenReturn(true);
        when(javaFilePath.toString()).thenReturn("Include.java");
        when(nonJavaFilePath.toString()).thenReturn("Exclude.java");

        // The filter only accepts paths whose string contains "Include"
        when(config.fileFilter()).thenReturn(p -> p.toString().contains("Include"));

        collector.visitFile(javaFilePath, attrs);
        collector.visitFile(nonJavaFilePath, attrs);

        List<Path> files = collector.getJavaFiles();
        assertThat(files, hasItem(javaFilePath));
        assertThat(files, not(hasItem(nonJavaFilePath)));
    }

    // ------------------------------------------------------------------------
    // visitFileFailed
    // ------------------------------------------------------------------------
    @Test
    public void visitFileFailedShouldContinueAndNotThrow() {
        IOException error = new IOException("disk full");
        // It is allowed to pass a phantom Path mock; we just verify the callback survives
        FileVisitResult result = collector.visitFileFailed(javaFilePath, error);
        assertThat(result, is(FileVisitResult.CONTINUE));
        // The internal list must remain unchanged
        assertTrue(collector.getJavaFiles().isEmpty());
    }

    // ------------------------------------------------------------------------
    // getJavaFiles – immutability
    // ------------------------------------------------------------------------
    @Test(expected = UnsupportedOperationException.class)
    public void getJavaFilesShouldReturnUnmodifiableList() {
        when(attrs.isRegularFile()).thenReturn(true);
        when(javaFilePath.toString()).thenReturn("A.java");
        collector.visitFile(javaFilePath, attrs);

        collector.getJavaFiles().add(mock(Path.class));
    }

    // ------------------------------------------------------------------------
    // Edge cases for the PATH_MATCHER (real implementation)
    // ------------------------------------------------------------------------
    @Test
    public void shouldMatchFilesWithAbsolutePaths() {
        when(attrs.isRegularFile()).thenReturn(true);
        // Absolute path ending with .java – the glob **.java matches the entire string
        when(javaFilePath.toString()).thenReturn("/home/user/project/Main.java");

        collector.visitFile(javaFilePath, attrs);
        assertThat(collector.getJavaFiles(), hasItem(javaFilePath));
    }

    @Test
    public void shouldNotMatchDirectoriesWithJavaExtension() {
        // Even if the path string ends with .java, isRegularFile=false prevents collection
        when(attrs.isRegularFile()).thenReturn(false);

        collector.visitFile(javaFilePath, attrs);
        assertTrue(collector.getJavaFiles().isEmpty());
    }
}
