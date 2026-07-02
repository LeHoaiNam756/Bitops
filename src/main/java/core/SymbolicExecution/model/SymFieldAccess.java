package core.SymbolicExecution.model;

import core.SymbolicExecution.model.types.SymType;

public record SymFieldAccess(
        SymbolicValue receiver,
        String fieldName,
        SymType fieldType
) implements SymbolicValue {

    public SymFieldAccess(SymbolicValue receiver, String fieldName) {
        this(receiver, fieldName, null);
    }
}
