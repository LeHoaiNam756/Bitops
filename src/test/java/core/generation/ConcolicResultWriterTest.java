package core.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.cfg.Coverage;
import core.testdriver.TestData;
import core.testdriver.TestResult;
import core.testpath.CoverageTracker;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConcolicResultWriterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Path outputFile = Path.of(
            FilePath.PATH_TO_TOOL_OUTPUT,
            "concolic-results",
            "same.json"
    );

    @Before
    public void setUp() throws Exception {
        Files.deleteIfExists(outputFile);
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(outputFile);
    }

    @Test
    public void writeAppendsRunForSameMethodAndStoresCoverageType() throws Exception {
        MethodDeclaration method = parseMethod("""
                class Sample {
                    int same(int x) {
                        return x;
                    }
                }
                """);
        TestResult result = resultWithCoverage();

        ConcolicResultWriter.write(method, result, Coverage.STATEMENT, List.of());
        ConcolicResultWriter.write(method, result, Coverage.BRANCH, List.of());

        JsonNode root = MAPPER.readTree(outputFile.toFile());
        assertTrue(root.isArray());
        assertEquals(2, root.size());
        assertEquals("same", root.get(0).path("methodName").asText());
        assertEquals("STATEMENT", root.get(0).path("coverageType").asText());
        assertEquals("BRANCH", root.get(1).path("coverageType").asText());
    }

    private static TestResult resultWithCoverage() {
        CoverageTracker tracker = new CoverageTracker(Set.of(1, 2));
        tracker.markCovered(1);
        return new TestResult(
                List.of(new TestData(Map.of("x", 1))),
                tracker,
                1024
        );
    }

    private static MethodDeclaration parseMethod(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        return ((TypeDeclaration) cu.types().get(0)).getMethods()[0];
    }
}
