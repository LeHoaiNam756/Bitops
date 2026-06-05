package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StringLiteral;

public class StringLiteralHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof StringLiteral;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        return SymLiteral.of(((StringLiteral) node).getLiteralValue());
    }
}
