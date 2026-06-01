package core.SymbolicExecution.model.types;

import java.util.List;
import java.util.Objects;

public record FunctionSymType(
        List<SymType> parameterTypes,
        SymType returnType
) implements SymType {

    public FunctionSymType {
        Objects.requireNonNull(parameterTypes);
        Objects.requireNonNull(returnType);
    }

    @Override
    public String toString() {
        return "(" +
                parameterTypes.stream()
                        .map(Object::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") +
                ") -> " + returnType;
    }
}
