package core.SymbolicExecution.model;

public record SymITE(
        SymbolicValue cond, SymbolicValue thenBranch, SymbolicValue elseBranch
) implements SymbolicValue{}
