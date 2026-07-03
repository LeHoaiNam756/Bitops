package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;

public class ArrayAccessHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof ArrayAccess;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ArrayAccess arrayAccess = (ArrayAccess) node;
        SymbolicValue arr = dispatcher.eval(arrayAccess.getArray(), state);
        SymbolicValue index = dispatcher.eval(arrayAccess.getIndex(), state);

        Integer knownLength = state.knownArrayLength(arr);
        SymbolicValue length = knownLength != null
                ? SymLiteral.of(knownLength)
                : new SymFieldAccess(arr, "length", PrimitiveSymType.INT);

        // Reaching the statement after an array read implies that Java's
        // implicit bounds checks succeeded.  Keep that fact in the path
        // condition even when the array parameter has a symbolic length.
        state.assume(new SymBinaryOp(index, SymBinaryOp.Op.SGE, SymLiteral.of(0)));
        state.assume(new SymBinaryOp(index, SymBinaryOp.Op.SLT, length));
        return new SymArraySelect(arr, index);
    }
}
