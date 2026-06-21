package core.SymbolicExecution.model;

import core.SymbolicExecution.model.types.SymType;

public record SymCastOp(
        SymType type, SymbolicValue operand
) implements SymbolicValue{}
