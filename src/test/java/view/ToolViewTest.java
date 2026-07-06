package view;

import core.testdriver.TestData;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ToolViewTest {
    @Test
    public void formattedTestDataUsesZeroCoverageWhenNoNodesAreTrackable() {
        TestData data = new TestData(Map.of("num", 3, "bit", 1));

        ToolView.FormattedTestData formatted = new ToolView.FormattedTestData(0, data);

        assertEquals(0, formatted.coverage());
    }

    @Test
    public void seedModesSelectOffRandomAndCoverageGuidedInputs() {
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(
                "void fill(int[] table, int start, int limit) {"
                        + " for (int i = start; i < 64; i++) table[i] = limit;"
                        + "}"
        ).get(0);

        assertTrue(ToolView.seedInputsFor(ToolView.SeedMode.OFF, method).isEmpty());
        assertEquals(1,
                ToolView.seedInputsFor(ToolView.SeedMode.RANDOM_ONLY, method).size());
        List<Map<String, Object>> guided =
                ToolView.seedInputsFor(ToolView.SeedMode.COVERAGE_GUIDED, method);
        assertTrue(guided.size() > 1);
    }
}
