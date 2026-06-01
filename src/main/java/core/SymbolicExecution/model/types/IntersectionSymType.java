package core.SymbolicExecution.model.types;

import java.util.List;
import java.util.Objects;

public record IntersectionSymType(
        List<SymType> bounds
) implements SymType {

    public IntersectionSymType {
        Objects.requireNonNull(bounds);
    }

    @Override
    public String toString() {
        return bounds.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + " & " + b)
                .orElse("");
    }
}
