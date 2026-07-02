package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import core.SymbolicExecution.model.types.UnknownSymType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

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
        return new SymFieldAccess(receiver, fieldName, resolveFieldType(fieldAccess));
    }

    private SymType resolveFieldType(FieldAccess fieldAccess) {
        IVariableBinding binding = fieldAccess.resolveFieldBinding();
        if (binding == null) {
            return UnknownSymType.INSTANCE;
        }
        ITypeBinding typeBinding = binding.getType();
        if (typeBinding == null) {
            return UnknownSymType.INSTANCE;
        }
        return SymTypeMap.convertBinding(typeBinding);
    }
}
