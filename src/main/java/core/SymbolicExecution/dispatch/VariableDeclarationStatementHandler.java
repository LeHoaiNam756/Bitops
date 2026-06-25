package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclarationStatementHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof VariableDeclarationStatement;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        VariableDeclarationStatement vds = (VariableDeclarationStatement) node;
        SymType declaredType = SymTypeMap.convert(vds.getType());

        for (Object o : vds.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            String varName = vdf.getName().getIdentifier();

            if (vdf.getInitializer() != null) {
                state.getTypeContext().pushAssignment(declaredType);
                try {
                    SymbolicValue val = dispatcher.eval(vdf.getInitializer(), state);
                    state.getMemoryModel().write(varName, val);
                } finally {
                    state.getTypeContext().pop();
                }
            } else {
                state.getMemoryModel().write(varName, new SymVariable(varName));
            }
        }
        return null;
    }
}
