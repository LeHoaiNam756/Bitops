package SymbolicExecution;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSymbolicExecution {
    @Test
    public void testCreateRandomValueForPrimitiveParameters() {
        Class<?>[] classes = {
                int.class,
                byte.class,
                short.class,
                long.class,
                float.class,
                double.class,
                char.class,
                boolean.class
        };
        Object[] randomValues = core.SymbolicExecution.SymbolicExecution.createRandomTestData(classes);
        assertEquals(randomValues.length, classes.length);
        assertTrue(randomValues[0] instanceof Integer);
        assertTrue(randomValues[1] instanceof Byte);
        assertTrue(randomValues[2] instanceof Short);
        assertTrue(randomValues[3] instanceof Long);
        assertTrue(randomValues[4] instanceof Float);
        assertTrue(randomValues[5] instanceof Double);
        assertTrue(randomValues[6] instanceof Character);
        assertTrue(randomValues[7] instanceof Boolean);
    }
}
