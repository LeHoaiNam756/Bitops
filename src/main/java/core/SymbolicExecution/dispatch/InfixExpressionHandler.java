package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.Map;

public class InfixExpressionHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
        return node instanceof InfixExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        InfixExpression infixExpression = (InfixExpression) node;
        SymbolicValue left = dispatcher.eval(infixExpression.getLeftOperand(), state);
        SymbolicValue right = dispatcher.eval(infixExpression.getRightOperand(), state);
        SymBinaryOp.Op op = InfixExpressionHandler.mapOp(infixExpression.getOperator());
        return new SymBinaryOp(left, op, right);
    }

    private static final Map<InfixExpression.Operator, SymBinaryOp.Op> OP_MAP =
        Map.ofEntries(
                Map.entry(InfixExpression.Operator.PLUS, SymBinaryOp.Op.ADD),
                Map.entry(InfixExpression.Operator.MINUS, SymBinaryOp.Op.SUB),
                Map.entry(InfixExpression.Operator.TIMES, SymBinaryOp.Op.MUL),
                Map.entry(InfixExpression.Operator.DIVIDE, SymBinaryOp.Op.DIV),
                Map.entry(InfixExpression.Operator.REMAINDER, SymBinaryOp.Op.MOD),

                Map.entry(InfixExpression.Operator.AND, SymBinaryOp.Op.BAND),
                Map.entry(InfixExpression.Operator.OR, SymBinaryOp.Op.BOR),
                Map.entry(InfixExpression.Operator.XOR, SymBinaryOp.Op.BXOR),

                Map.entry(InfixExpression.Operator.LEFT_SHIFT, SymBinaryOp.Op.BLS),
                Map.entry(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, SymBinaryOp.Op.BRS),
                Map.entry(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, SymBinaryOp.Op.BURS),

                Map.entry(InfixExpression.Operator.EQUALS, SymBinaryOp.Op.EQ),
                Map.entry(InfixExpression.Operator.NOT_EQUALS, SymBinaryOp.Op.NEQ),

                Map.entry(InfixExpression.Operator.GREATER, SymBinaryOp.Op.SGT),
                Map.entry(InfixExpression.Operator.LESS, SymBinaryOp.Op.SLT),
                Map.entry(InfixExpression.Operator.GREATER_EQUALS, SymBinaryOp.Op.SGE),
                Map.entry(InfixExpression.Operator.LESS_EQUALS, SymBinaryOp.Op.SLE),

                Map.entry(InfixExpression.Operator.CONDITIONAL_AND, SymBinaryOp.Op.AND),
                Map.entry(InfixExpression.Operator.CONDITIONAL_OR, SymBinaryOp.Op.OR)
        );

    public static SymBinaryOp.Op mapOp(InfixExpression.Operator op) {
        SymBinaryOp.Op result = OP_MAP.get(op);

        if (result == null) {
            throw new IllegalArgumentException("Unsupported operator: " + op);
        }

        return result;
    }
}
