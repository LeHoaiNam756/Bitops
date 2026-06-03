package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class EnhancedForStatementHandler implements AstHandler {
    private static final int SYMBOLIC_FALLBACK_ITERATIONS = 1;

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof EnhancedForStatement;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        EnhancedForStatement statement = (EnhancedForStatement) node;
        SingleVariableDeclaration parameter = statement.getParameter();
        String loopVariableName = parameter.getName().getIdentifier();
        SymType loopVariableType = SymTypeMap.convert(parameter.getType());
        SymbolicValue array = dispatcher.eval(statement.getExpression(), state);

        int iterations = knownInitializerLength(array);
        if (iterations < 0) {
            iterations = SYMBOLIC_FALLBACK_ITERATIONS;
        }

        SymbolicValue last = null;
        for (int i = 0; i < iterations; i++) {
            state.getMemoryModel().write(loopVariableName, new SymArraySelect(array, SymLiteral.of(i)));
            state.getTypeContext().push(loopVariableType);
            try {
                last = dispatcher.eval(statement.getBody(), state);
            } finally {
                state.getTypeContext().pop();
            }
        }
        return last;
    }

    private int knownInitializerLength(SymbolicValue value) {
        int count = 0;
        SymbolicValue current = value;
        while (current instanceof SymArrayStore store) {
            count++;
            current = store.arr();
        }
        return count > 0 ? count : -1;
    }
}
