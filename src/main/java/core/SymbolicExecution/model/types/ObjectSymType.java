package core.SymbolicExecution.model.types;

import java.util.Objects;

public record ObjectSymType(
        String className
) implements SymType {

    public ObjectSymType {
        Objects.requireNonNull(className);
    }

    @Override
    public String toString() {
        return className;
    }
}
