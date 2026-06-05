package core.SymbolicExecution.model;

public sealed interface SymbolicValue
        permits SymArraySelect, SymArrayStore, SymBinaryOp, SymFieldAccess, SymITE, SymLiteral, SymStringOp, SymUnaryOp, SymVariable {

}
