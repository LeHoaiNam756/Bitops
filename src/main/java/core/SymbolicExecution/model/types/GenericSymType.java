package core.SymbolicExecution.model.types;

import java.util.List;
import java.util.Objects;

public record GenericSymType(
        String rawType,
        List<SymType> typeArguments
) implements SymType {

    public GenericSymType {
        Objects.requireNonNull(rawType);
        Objects.requireNonNull(typeArguments);
    }

    @Override
    public String toString() {
        return rawType + "<" +
                typeArguments.stream()
                        .map(Object::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("") +
                ">";
    }
}
