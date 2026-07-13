package core.generation;

import core.testdriver.TestDriver;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MethodPreconditionsTest {

    @Test
    public void apply_clampsRangesRepairsArrayLengthAndDefaultsNonNulls() {
        MethodDeclaration method = method(
                "void target(int index, int limit, int[] values, String label) {}");
        List<TestDriver.ParamInfo> params = TestDriver.extractParams(method);
        MethodPreconditions preconditions = MethodPreconditions.builder()
                .range("index", 0, 10)
                .range("limit", 1, 10)
                .arrayLengthRange("values", 3, 4)
                .nonNull("label")
                .lessThan("index", "limit")
                .build();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("index", -8);
        input.put("limit", -1);
        input.put("values", new int[] {1});
        input.put("label", null);

        Map<String, Object> applied = preconditions.apply(input, params);

        assertEquals(0, applied.get("index"));
        assertEquals(1, applied.get("limit"));
        assertEquals(3, ((int[]) applied.get("values")).length);
        assertEquals("", applied.get("label"));
    }

    @Test
    public void apply_repairsRelationalInvariantAfterScalarRanges() {
        MethodDeclaration method = method("void target(int start, int end) {}");
        List<TestDriver.ParamInfo> params = TestDriver.extractParams(method);
        MethodPreconditions preconditions = MethodPreconditions.builder()
                .range("start", 0, 10)
                .range("end", 0, 10)
                .lessThan("start", "end")
                .build();

        Map<String, Object> applied = preconditions.apply(
                Map.of("start", 5, "end", 1), params);

        assertEquals(5, applied.get("start"));
        assertEquals(6, applied.get("end"));
    }

    @Test
    public void apply_respectsExplicitNullableReferenceSeed() {
        MethodDeclaration method = method("void target(String label) {}");
        List<TestDriver.ParamInfo> params = TestDriver.extractParams(method);
        MethodPreconditions preconditions = MethodPreconditions.builder()
                .nullable("label")
                .build();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("label", null);

        Map<String, Object> applied = preconditions.apply(input, params);

        assertNull(applied.get("label"));
    }

    @Test
    public void symbolicConstraints_includeRangesLengthsAndRelations() {
        MethodPreconditions preconditions = MethodPreconditions.builder()
                .range("index", 0, 10)
                .arrayLengthAtLeast("values", 3)
                .lessThan("index", "limit")
                .build();

        assertEquals(5, preconditions.symbolicConstraints().size());
        assertEquals(Integer.valueOf(3),
                preconditions.minimumArrayLengths().get("values"));
    }

    private static MethodDeclaration method(String source) {
        return (MethodDeclaration) Parser.parseSourceToAstFuncList(source).get(0);
    }
}
