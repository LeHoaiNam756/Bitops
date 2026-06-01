package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;

public class FieldAccessHandler implements AstHandler{

    @Override
    public boolean supports(ASTNode node) {
       return node instanceof FieldAccess;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        FieldAccess fieldAccess = (FieldAccess) node;
        SymbolicValue receiver = dispatcher.eval(fieldAccess.getExpression(), state);
        String fieldName = fieldAccess.getName().getIdentifier();
        return new SymFieldAccess(receiver, fieldName);
    }
}
