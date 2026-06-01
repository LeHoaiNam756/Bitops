package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
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
        return new SymArraySelect(arr, index);
    }
}
