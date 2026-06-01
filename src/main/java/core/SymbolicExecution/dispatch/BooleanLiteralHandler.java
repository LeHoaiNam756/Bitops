package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;

public class BooleanLiteralHandler implements AstHandler{

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof BooleanLiteral;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        return SymLiteral.of(((BooleanLiteral) node).booleanValue());
    }
}
