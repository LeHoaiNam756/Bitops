package core.generation;

import core.parser.ParseEntry;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProjectTest {
    @Test
    public void parsesZipAndMapsMethodsToRootAst() throws IOException {
        Path zip = createZipProject("Sample.java", "class Sample { int add(int a, int b) { return a + b; } }");

        Project project = new Project(zip);

        List<MethodDeclaration> methods = project.getMethods();
        assertEquals(1, methods.size());
        assertEquals("add", methods.get(0).getName().getIdentifier());

        CompilationUnit root = project.getRootAST(methods.get(0));
        assertNotNull(root);
    }

    @Test
    public void parsedCompilationUnitKeepsSourceFileNameWhenJavaElementIsAbsent() throws IOException {
        Path zip = createZipProject("Sample.java", "class Sample { void run() {} }");

        Project project = new Project(zip);

        CompilationUnit root = project.getRootAST(project.getMethods().get(0));
        assertNull(root.getJavaElement());
        assertEquals("Sample.java", root.getProperty(ParseEntry.SOURCE_FILE_NAME_PROPERTY));
    }

    @Test
    public void getMethodsIncludesConstructorsAsUnits() throws IOException {
        Path zip = createZipProject(
                "Sample.java",
                "class Sample { Sample() {} Sample(int x) {} int add() { return 1; } }");

        Project project = new Project(zip);

        List<MethodDeclaration> methods = project.getMethods();
        assertEquals(3, methods.size());
        assertEquals(2, methods.stream().filter(MethodDeclaration::isConstructor).count());
        assertTrue(methods.stream().anyMatch(method -> "add".equals(method.getName().getIdentifier())));
    }

    private static Path createZipProject(String fileName, String source) throws IOException {
        Path zip = Files.createTempFile("ct4j-project-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(fileName));
            zos.write(source.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        zip.toFile().deleteOnExit();
        return zip;
    }
}
