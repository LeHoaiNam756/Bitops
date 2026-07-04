package core.generation;

import core.cfg.Coverage;
import core.testpath.AllPathsFinder;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExpandedCapacityConcolicTest {

    @Test
    public void expandedCapacityReachesFullStatementCoverage() throws Exception {
        var result = generate(Coverage.STATEMENT);

        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUncovered().size());
        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUnknown().size());
        assertEquals(result.fullCoverage().getInfeasibleReasons().toString(), 0,
                result.fullCoverage().getInfeasible().size());
        assertTrue(result.testDataList().stream().anyMatch(test ->
                test.input().get("minCapacity") instanceof Integer minCapacity
                        && minCapacity > (1 << 30)
                        && Integer.toString(Integer.MAX_VALUE).equals(test.output())));
    }

    @Test
    public void expandedCapacityReachesFullBranchCoverage() throws Exception {
        var result = generate(Coverage.BRANCH);

        assertFullDecisionCoverage(result);
    }

    @Test
    public void expandedCapacityReachesFullMcdcCoverage() throws Exception {
        var result = generate(Coverage.MCDC);

        assertFullDecisionCoverage(result);
    }

    private static void assertFullDecisionCoverage(core.testdriver.TestResult result) {
        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUncovered().size());
        assertEquals(result.fullCoverage().getUnknownReasons().toString(), 0,
                result.fullCoverage().getUnknown().size());
        assertEquals(result.fullCoverage().getInfeasibleReasons().toString(), 0,
                result.fullCoverage().getInfeasible().size());
        assertEquals(8, result.fullCoverage().getCovered().size());
    }

    private static core.testdriver.TestResult generate(Coverage coverage) throws Exception {
        Path zip = createZipProject();
        Project project = new Project(zip);
        MethodDeclaration method = project.getMethods().stream()
                .filter(m -> "expandedCapacity".equals(m.getName().getIdentifier()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> seed = new LinkedHashMap<>();
        seed.put("oldCapacity", 0);
        seed.put("minCapacity", 0);

        var result = ConcolicTesting.getInstance().generate(
                method,
                project.getRootAST(method),
                coverage,
                seed,
                new AllPathsFinder());
        return result;
    }

    private static Path createZipProject() throws Exception {
        String source = """
                public class CapacityUnit {
                    int expandedCapacity(int oldCapacity, int minCapacity) {
                        if (minCapacity < 0) {
                            throw new IllegalArgumentException(
                                    "cannot store more than Integer.MAX_VALUE elements");
                        } else if (minCapacity <= oldCapacity) {
                            return oldCapacity;
                        }
                        int newCapacity = oldCapacity + (oldCapacity >> 1) + 1;
                        if (newCapacity < minCapacity) {
                            newCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
                        }
                        if (newCapacity < 0) {
                            newCapacity = Integer.MAX_VALUE;
                        }
                        return newCapacity;
                    }
                }
                """;
        Path zip = Files.createTempFile("ct4j-expanded-capacity-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("CapacityUnit.java"));
            zos.write(source.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        zip.toFile().deleteOnExit();
        return zip;
    }
}
