package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class ThrowStatementHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
        return node instanceof ThrowStatement;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        state.getMemoryModel().write("__throw_" + node.toString(), null);
        return null;
    }
}
