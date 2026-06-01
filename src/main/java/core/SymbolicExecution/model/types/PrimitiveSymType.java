package core.SymbolicExecution.model.types;

public enum PrimitiveSymType implements SymType {
    INT,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    CHAR,
    LONG,
    SHORT,
    BYTE;

    public boolean isIntegral() {
        return switch (this) {
            case INT, LONG, SHORT, BYTE, CHAR -> true;
            default -> false;
        };
    }

    public boolean isFloatingPoint() {
        return switch (this) {
            case FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    public boolean isNumeric() {
        return isIntegral() || isFloatingPoint();
    }
}
