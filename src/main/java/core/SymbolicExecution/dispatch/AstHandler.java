package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;

public interface AstHandler {
    boolean supports(ASTNode node);

    SymbolicValue eval(ASTNode node, SymbolicStore store, SymbolicContext ctx, AstDispatcher dispatcher);
}
