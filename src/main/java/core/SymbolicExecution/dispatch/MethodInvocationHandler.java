package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.List;

public class MethodInvocationHandler implements AstHandler {
    @Override
    public boolean supports(ASTNode node) {
        return  node instanceof MethodInvocation;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        MethodInvocation m = (MethodInvocation) node;
        String name = m.getName().getIdentifier();
        @SuppressWarnings("unchecked")
        List<SymbolicValue> args = m.arguments().stream()
                .map(a -> dispatcher.eval((Expression) a, state))
                .toList();
        return switch (name) {
            case "abs"  -> new SymITE(
                    new SymBinaryOp(args.get(0), SymBinaryOp.Op.SGE, SymLiteral.of(0)),
                    args.get(0),
                    new SymUnaryOp(SymUnaryOp.Op.NEG, (SymVariable) args.get(0)));
            case "max"  -> new SymITE(
                    new SymBinaryOp(args.get(0), SymBinaryOp.Op.SGE, args.get(1)),
                    args.get(0), args.get(1));
            case "min"  -> new SymITE(
                    new SymBinaryOp(args.get(0), SymBinaryOp.Op.SLE, args.get(1)),
                    args.get(0), args.get(1));
            default -> new SymVariable("__call_" + name + "_" + System.identityHashCode(m));
        };
    }
}
