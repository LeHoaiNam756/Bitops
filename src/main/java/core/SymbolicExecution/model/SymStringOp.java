package core.SymbolicExecution.model;

import java.util.List;

public record SymStringOp(
        SymbolicValue receiver,
        SymStringOp.Op op,
        List<SymbolicValue> args
) implements SymbolicValue {
    public SymStringOp {
        args = List.copyOf(args);
    }

    public enum Op {
        EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        LENGTH,
        IS_EMPTY,
        SUBSTRING,
        TO_LOWER_CASE,
        TO_UPPER_CASE,
        TRIM,
        REPLACE,
        INDEX_OF
    }
}
