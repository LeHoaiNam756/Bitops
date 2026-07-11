package core.SymbolicExecution;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AblationOptionsTest {

    @Test
    public void disableTurnsOffOnlyRequestedPart() {
        AblationOptions options = AblationOptions.disable(AblationPart.BIT_OPERATIONS);

        assertTrue(options.javaTypeConversionEnabled());
        assertFalse(options.bitOperationsEnabled());
        assertTrue(options.bitVectorArithmeticEnabled());
        assertTrue(options.simplifierEnabled());
    }

    @Test
    public void fromSystemPropertiesReadsBooleanAliases() {
        String oldJavaTypeConversion = System.getProperty("ablation.javaTypeConversion");
        String oldBitOperations = System.getProperty("ablation.bitOperations");
        String oldBitVectorArithmetic = System.getProperty("ablation.bitVectorArithmetic");
        String oldSimplifier = System.getProperty("ablation.simplifier");
        try {
            System.setProperty("ablation.javaTypeConversion", "off");
            System.setProperty("ablation.bitOperations", "0");
            System.setProperty("ablation.bitVectorArithmetic", "yes");
            System.setProperty("ablation.simplifier", "enabled");

            AblationOptions options = AblationOptions.fromSystemProperties();

            assertFalse(options.javaTypeConversionEnabled());
            assertFalse(options.bitOperationsEnabled());
            assertTrue(options.bitVectorArithmeticEnabled());
            assertTrue(options.simplifierEnabled());
        } finally {
            restore("ablation.javaTypeConversion", oldJavaTypeConversion);
            restore("ablation.bitOperations", oldBitOperations);
            restore("ablation.bitVectorArithmetic", oldBitVectorArithmetic);
            restore("ablation.simplifier", oldSimplifier);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
