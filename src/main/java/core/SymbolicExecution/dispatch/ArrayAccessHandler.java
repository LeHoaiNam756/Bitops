package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
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

        Integer length = state.knownArrayLength(arr);
        if (length != null) {
            state.assume(new SymBinaryOp(index, SymBinaryOp.Op.SGE, SymLiteral.of(0)));
            state.assume(new SymBinaryOp(index, SymBinaryOp.Op.SLT, SymLiteral.of(length)));
        }
        return new SymArraySelect(arr, index);
    }
}
