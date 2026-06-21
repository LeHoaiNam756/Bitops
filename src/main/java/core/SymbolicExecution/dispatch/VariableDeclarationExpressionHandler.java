package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class VariableDeclarationExpressionHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof VariableDeclarationExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        VariableDeclarationExpression vde = (VariableDeclarationExpression) node;
        SymType declaredType = SymTypeMap.convert(vde.getType());

        for (Object o : vde.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            String varName = vdf.getName().getIdentifier();

            if (vdf.getInitializer() != null) {
                state.getTypeContext().push(declaredType);
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