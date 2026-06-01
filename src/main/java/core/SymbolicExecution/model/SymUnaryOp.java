package core.SymbolicExecution.model;

public record SymUnaryOp(
        SymUnaryOp.Op op,
        SymbolicValue operand
) implements SymbolicValue {
    public enum Op {
        NEG, NOT, INC, DEC, COMPLIMENT, PLUS
    }
}
