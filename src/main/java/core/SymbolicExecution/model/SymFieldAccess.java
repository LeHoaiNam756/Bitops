package core.SymbolicExecution.model;

public record SymFieldAccess(
        SymbolicValue receiver,
        String fieldName
) implements SymbolicValue{
}
