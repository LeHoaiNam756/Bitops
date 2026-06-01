package core.SymbolicExecution.model.types;

import java.util.Objects;

public record ArraySymType(
        SymType elementType,
        int dimensions
) implements SymType {

    public ArraySymType {
        Objects.requireNonNull(elementType);

        if (dimensions <= 0) {
            throw new IllegalArgumentException(
                    "dimensions must be > 0"
            );
        }
    }

    @Override
    public String toString() {
        return elementType +
                "[]".repeat(dimensions);
    }
}
