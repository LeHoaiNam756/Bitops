package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class AstDispatcher {
    private final List<AstHandler> handlers = new ArrayList<>();

    public AstDispatcher register(AstHandler handler) {
        handlers.add(handler);
        return this;
    }

    public SymbolicValue eval(ASTNode node, SymbolicStore store, SymbolicContext ctx) {
        for (AstHandler handler : handlers) {
            if (handler.supports(node)) {
                return handler.eval(node, store, ctx, this);
            }
        }
        throw new IllegalArgumentException("No handler for AST node: " + node.getClass());
    }
}
