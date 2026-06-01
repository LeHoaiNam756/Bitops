package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

public class BlockHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof Block;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        Block block = (Block) node;
        SymbolicValue last = null;
        for (Object o : block.statements()) {
            last = dispatcher.eval((Statement) o, state);
        }
        return last;
    }
}
