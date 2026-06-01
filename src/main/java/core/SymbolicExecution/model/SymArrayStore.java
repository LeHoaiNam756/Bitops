package core.SymbolicExecution.model;

public record SymArrayStore(
        SymbolicValue arr,
        SymbolicValue index,
        SymbolicValue value
) implements SymbolicValue{
}
