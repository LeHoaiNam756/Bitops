package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymUnaryOp;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import java.util.Map;

public class PrefixExpressionHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof PrefixExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        PrefixExpression prefixExpression = (PrefixExpression) node;
        SymbolicValue operand = dispatcher.eval(prefixExpression.getOperand(), state);
        SymUnaryOp.Op op = mapOp(prefixExpression.getOperator());
        return new SymUnaryOp(op, operand);

    }

    private static final Map<PrefixExpression.Operator, SymUnaryOp.Op> OP_MAP =
            Map.ofEntries(
                    Map.entry(PrefixExpression.Operator.PLUS, SymUnaryOp.Op.PLUS),
                    Map.entry(PrefixExpression.Operator.MINUS, SymUnaryOp.Op.NEG),
                    Map.entry(PrefixExpression.Operator.INCREMENT, SymUnaryOp.Op.INC),
                    Map.entry(PrefixExpression.Operator.DECREMENT, SymUnaryOp.Op.DEC),
                    Map.entry(PrefixExpression.Operator.NOT, SymUnaryOp.Op.NOT),
                    Map.entry(PrefixExpression.Operator.COMPLEMENT, SymUnaryOp.Op.COMPLIMENT)
            );

    public static SymUnaryOp.Op mapOp(PrefixExpression.Operator operator) {
        SymUnaryOp.Op op = OP_MAP.get(operator);
        if (op == null) {
            throw new IllegalArgumentException("Unknown operator: " + operator);
        }
        return op;
    }
}
