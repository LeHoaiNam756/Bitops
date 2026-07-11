package core.SymbolicExecution;

import java.util.Locale;

/**
 * Feature switches used for ablation studies. All features are enabled by
 * default, preserving the normal BitOps concolic behavior.
 */
public record AblationOptions(
        boolean javaTypeConversionEnabled,
        boolean bitOperationsEnabled,
        boolean bitVectorArithmeticEnabled,
        boolean simplifierEnabled) {

    public static final AblationOptions ALL_ENABLED =
            new AblationOptions(true, true, true, true);

    public static AblationOptions disable(AblationPart part) {
        return switch (part) {
            case JAVA_TYPE_CONVERSION ->
                    new AblationOptions(false, true, true, true);
            case BIT_OPERATIONS ->
                    new AblationOptions(true, false, true, true);
            case BITVECTOR_ARITHMETIC ->
                    new AblationOptions(true, true, false, true);
            case SIMPLIFIER ->
                    new AblationOptions(true, true, true, false);
        };
    }

    public static AblationOptions fromSystemProperties() {
        return new AblationOptions(
                boolProperty("ablation.javaTypeConversion", true),
                boolProperty("ablation.bitOperations", true),
                boolProperty("ablation.bitVectorArithmetic", true),
                boolProperty("ablation.simplifier", true));
    }

    public boolean isAllEnabled() {
        return equals(ALL_ENABLED);
    }

    public String label() {
        if (isAllEnabled()) {
            return "all-enabled";
        }
        StringBuilder label = new StringBuilder();
        appendDisabled(label, "java-type-conversion", javaTypeConversionEnabled);
        appendDisabled(label, "bit-operations", bitOperationsEnabled);
        appendDisabled(label, "bitvector-arithmetic", bitVectorArithmeticEnabled);
        appendDisabled(label, "simplifier", simplifierEnabled);
        return label.toString();
    }

    private static void appendDisabled(StringBuilder builder, String name, boolean enabled) {
        if (enabled) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('+');
        }
        builder.append("without-").append(name);
    }

    private static boolean boolProperty(String name, boolean defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "on", "enabled" -> true;
            case "false", "0", "no", "off", "disabled" -> false;
            default -> defaultValue;
        };
    }
}
