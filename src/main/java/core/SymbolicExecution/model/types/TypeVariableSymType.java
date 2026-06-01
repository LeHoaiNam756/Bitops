package core.SymbolicExecution.model.types;

import java.util.Objects;

public record TypeVariableSymType(
        String name
) implements SymType {

    public TypeVariableSymType {
        Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
