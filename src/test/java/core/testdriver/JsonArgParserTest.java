package core.testdriver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class JsonArgParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void convertsJacksonStringRepresentationOfCharArray() {
        char[] value = {'A', '\0', '\ud83d', '\ude00'};

        Object converted = JsonArgParser.convert(
                "table", "char[]", MAPPER.valueToTree(value));

        assertArrayEquals(value, (char[]) converted);
    }

    @Test
    public void stillConvertsExplicitJsonArrayToCharArray() throws Exception {
        Object converted = JsonArgParser.convert(
                "table", "char[]", MAPPER.readTree("[65, \"B\", 0]"));

        assertArrayEquals(new char[] {'A', 'B', '\0'}, (char[]) converted);
    }

    @Test
    public void convertsJacksonStringRowsOfTwoDimensionalCharArray() {
        char[][] value = {{'A', 'B'}, {}, {'C'}};

        Object converted = JsonArgParser.convert(
                "tables", "char[][]", MAPPER.valueToTree(value));

        char[][] actual = (char[][]) converted;
        assertArrayEquals(value[0], actual[0]);
        assertArrayEquals(value[1], actual[1]);
        assertArrayEquals(value[2], actual[2]);
    }
}
