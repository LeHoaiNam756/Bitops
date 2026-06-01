package core.SymbolicExecution.model.types;

import java.util.Objects;

public record ClassLiteralSymType(
        SymType targetType
) implements SymType {

    public ClassLiteralSymType {
        Objects.requireNonNull(targetType);
    }

    @Override
    public String toString() {
        return targetType + ".class";
    }
}
