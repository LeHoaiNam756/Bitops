package core.generation;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RandomTestInputTest {

    @Test
    public void createRandomTestData_supportsStringScalarAndArray() {
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(
                "void target(String s, String[] values, java.lang.String qualified) {}"
        ).get(0);

        Map<String, Object> input = RandomTestInput.createRandomTestData(method);

        assertTrue(input.get("s") instanceof String);
        assertTrue(input.get("values") instanceof String[]);
        assertTrue(input.get("qualified") instanceof String);
    }

    @Test
    public void createBoundaryTestData_isBoundedAndIncludesPrimitiveExtrema() {
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(
                "void target(int i, long l, boolean enabled) {}"
        ).get(0);

        List<Map<String, Object>> seeds = RandomTestInput.createBoundaryTestData(method);

        assertEquals(5, seeds.size());
        assertEquals(0, seeds.get(0).get("i"));
        assertEquals(1, seeds.get(1).get("i"));
        assertEquals(-1, seeds.get(2).get("i"));
        assertEquals(Integer.MIN_VALUE, seeds.get(3).get("i"));
        assertEquals(Integer.MAX_VALUE, seeds.get(4).get("i"));
        assertEquals(false, seeds.get(0).get("enabled"));
        assertEquals(true, seeds.get(1).get("enabled"));
        assertEquals(true, seeds.get(4).get("enabled"));
    }

    @Test
    public void createBoundaryTestData_includesEmptyAndSingletonArrays() {
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(
                "void target(int[] values, String text) {}"
        ).get(0);

        List<Map<String, Object>> seeds = RandomTestInput.createBoundaryTestData(method);

        assertEquals(2, seeds.size());
        assertEquals(0, ((int[]) seeds.get(0).get("values")).length);
        assertEquals(1, ((int[]) seeds.get(1).get("values")).length);
        assertEquals("", seeds.get(0).get("text"));
        assertEquals("a", seeds.get(1).get("text"));
    }

    @Test
    public void createBoundaryTestData_crossesNumericExtremaAcrossParameters() {
        MethodDeclaration method = (MethodDeclaration) Parser.parseSourceToAstFuncList(
                "void target(long a, long b) {}"
        ).get(0);

        List<Map<String, Object>> seeds = RandomTestInput.createBoundaryTestData(method);

        assertTrue(seeds.stream().anyMatch(seed ->
                Long.valueOf(Long.MIN_VALUE).equals(seed.get("a"))
                        && Long.valueOf(-1L).equals(seed.get("b"))));
    }
}
