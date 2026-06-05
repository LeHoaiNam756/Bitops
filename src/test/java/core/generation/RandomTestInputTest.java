package core.generation;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.Map;

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
}
