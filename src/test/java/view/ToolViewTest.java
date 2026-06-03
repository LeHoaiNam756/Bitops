package view;

import core.testdriver.TestData;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ToolViewTest {
    @Test
    public void formattedTestDataUsesZeroCoverageWhenNoNodesAreTrackable() {
        TestData data = new TestData(Map.of("num", 3, "bit", 1));

        ToolView.FormattedTestData formatted = new ToolView.FormattedTestData(0, data);

        assertEquals(0, formatted.coverage());
    }
}
