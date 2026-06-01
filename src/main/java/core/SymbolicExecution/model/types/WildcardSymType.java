package core.SymbolicExecution.model.types;

public record WildcardSymType(
        SymType upperBound,
        SymType lowerBound
) implements SymType {

    @Override
    public String toString() {

        if (upperBound == null && lowerBound == null) {
            return "?";
        }

        if (upperBound != null) {
            return "? extends " + upperBound;
        }

        return "? super " + lowerBound;
    }
}
