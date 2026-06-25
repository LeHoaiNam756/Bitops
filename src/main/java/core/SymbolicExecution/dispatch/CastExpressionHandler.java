    package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymCastOp;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CastExpression;

public class CastExpressionHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
       return node instanceof CastExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        CastExpression cast = (CastExpression) node;
        SymType targetType = SymTypeMap.convert(cast.getType());
        state.getTypeContext().push(targetType);
        try {
            SymbolicValue operand = dispatcher.eval(cast.getExpression(), state);
            return new SymCastOp(targetType, operand);
        } finally {
            state.getTypeContext().pop();
        }
    }


}
