package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;

public interface AstHandler {
    boolean supports(ASTNode node);

    SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher);
}
