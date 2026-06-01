package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;

public class BreakStatementHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof BreakStatement;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        return null;
    }
}
