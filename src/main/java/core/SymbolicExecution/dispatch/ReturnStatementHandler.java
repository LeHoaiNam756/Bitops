package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ReturnStatement;

public class ReturnStatementHandler implements AstHandler {
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof ReturnStatement;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ReturnStatement r = (ReturnStatement) node;
        if (r.getExpression() != null) {
            SymbolicValue v = dispatcher.eval(r.getExpression(), state);
            state.getMemoryModel().write("__return", v);
            return v;
        }
        return null;
    }
}
