package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;

public class ParenthesizedExpressionHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof ParenthesizedExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ParenthesizedExpression expression = (ParenthesizedExpression) node;
        ASTNode inner = expression.getExpression();
        return dispatcher.eval(inner, state);
    }
}
