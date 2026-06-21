package core.SymbolicExecution.model;

public sealed interface SymbolicValue
        permits SymArraySelect, SymArrayStore, SymBinaryOp, SymCastOp, SymFieldAccess, SymITE, SymLiteral, SymStringOp, SymUnaryOp, SymVariable {

}
