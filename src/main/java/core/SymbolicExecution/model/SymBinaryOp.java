package core.SymbolicExecution.model;

public record SymBinaryOp(
        SymbolicValue left, SymBinaryOp.Op op, SymbolicValue right
) implements SymbolicValue {
    public enum Op {
        ADD, SUB, MUL, DIV, MOD,
        BAND, BOR, BXOR, BLS, BRS, BURS,
        EQ, NEQ, SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE,
        AND, OR,
    }
}
