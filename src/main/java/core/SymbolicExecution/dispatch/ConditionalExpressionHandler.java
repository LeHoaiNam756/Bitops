package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymITE;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;

public class ConditionalExpressionHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof ConditionalExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ConditionalExpression e = (ConditionalExpression) node;
        SymbolicValue cond = dispatcher.eval(e.getExpression(), state);
        SymbolicValue thenV = dispatcher.eval(e.getThenExpression(), state);
        SymbolicValue elseV = dispatcher.eval(e.getElseExpression(), state);
        return new SymITE(cond, thenV, elseV);
    }
}
