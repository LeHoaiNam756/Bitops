package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;

public class ClassInstanceCreationHandler implements AstHandler {
    @Override
    public boolean supports(ASTNode node) {
        return node instanceof ClassInstanceCreation;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        ClassInstanceCreation creation = (ClassInstanceCreation) node;

        if (creation.getExpression() != null) {
            dispatcher.eval(creation.getExpression(), state);
        }
        for (Object argument : creation.arguments()) {
            dispatcher.eval((Expression) argument, state);
        }

        String typeName = creation.getType() == null ? "Object" : creation.getType().toString();
        return new SymVariable("__obj_" + sanitize(typeName) + "_" + System.identityHashCode(creation));
    }

    private String sanitize(String typeName) {
        return typeName.replaceAll("[^A-Za-z0-9_$]", "_");
    }
}
