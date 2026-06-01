package core.SymbolicExecution.model;

public record SymArraySelect(
        SymbolicValue arr,
        SymbolicValue index
) implements SymbolicValue {
}
