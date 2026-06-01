package core.SymbolicExecution.model.types;

import java.util.List;
import java.util.Objects;

public record UnionSymType(
        List<SymType> types
) implements SymType {

    public UnionSymType {
        Objects.requireNonNull(types);
    }

    @Override
    public String toString() {
        return types.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + " | " + b)
                .orElse("");
    }
}
