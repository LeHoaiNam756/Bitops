package core.SymbolicExecution;

public enum AblationPart {
    JAVA_TYPE_CONVERSION("java-type-conversion", "Resolve Java type conversion"),
    BIT_OPERATIONS("bit-operations", "BitOps operation"),
    BITVECTOR_ARITHMETIC("bitvector-arithmetic", "BitVector arithmetic operator encoding"),
    SIMPLIFIER("simplifier", "Simplifier");

    private final String csvColumn;
    private final String displayName;

    AblationPart(String csvColumn, String displayName) {
        this.csvColumn = csvColumn;
        this.displayName = displayName;
    }

    public String csvColumn() {
        return csvColumn;
    }

    public String displayName() {
        return displayName;
    }
}
