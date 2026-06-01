package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Expression;

import java.util.List;

public class ArrayCreationHandler implements AstHandler {
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof ArrayCreation;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ArrayCreation c = (ArrayCreation) node;
        String freshName = "__arr_" + System.identityHashCode(c);
        SymbolicValue arr = new SymVariable(freshName);
        if (c.getInitializer() != null) {
            // build SymArrayStore chain from initializer
            @SuppressWarnings("unchecked")
            List<Expression> exprs = c.getInitializer().expressions();
            for (int i = 0; i < exprs.size(); i++) {
                SymbolicValue val = dispatcher.eval(exprs.get(i), state);
                arr = new SymArrayStore(arr, SymLiteral.of(i), val);
            }
        }
        state.getMemoryModel().write(freshName, arr);
        return arr;
    }
}
