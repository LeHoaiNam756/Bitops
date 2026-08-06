package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
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
        if (op == SymUnaryOp.Op.INC || op == SymUnaryOp.Op.DEC) {
            return evalMutatingPrefix(prefixExpression, operand, op, state);
        }
        return new SymUnaryOp(op, operand);

    }

    private SymbolicValue evalMutatingPrefix(
            PrefixExpression expression,
            SymbolicValue operand,
            SymUnaryOp.Op op,
            SymbolicState state) {
        if (!(expression.getOperand() instanceof Name name)) {
            return new SymUnaryOp(op, operand);
        }
        String baseName = name.getFullyQualifiedName();
        SymbolicValue old = state.getMemoryModel().read(baseName).orElse(operand);
        SymBinaryOp.Op binaryOp = op == SymUnaryOp.Op.INC
                ? SymBinaryOp.Op.ADD
                : SymBinaryOp.Op.SUB;
        SymbolicValue updated = new SymBinaryOp(old, binaryOp, SymLiteral.of(1));
        state.getMemoryModel().write(baseName, updated);
        return updated;
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
