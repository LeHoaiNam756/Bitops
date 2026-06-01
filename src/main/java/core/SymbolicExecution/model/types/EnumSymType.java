package core.SymbolicExecution.model.types;

import java.util.Objects;

public record EnumSymType(
        String enumName
) implements SymType {

    public EnumSymType {
        Objects.requireNonNull(enumName);
    }

    @Override
    public String toString() {
        return enumName;
    }
}
