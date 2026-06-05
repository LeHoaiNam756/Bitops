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
        SymbolicValue receiver = m.getExpression() == null
                ? null
                : dispatcher.eval(m.getExpression(), state);
        @SuppressWarnings("unchecked")
        List<SymbolicValue> args = m.arguments().stream()
                .map(a -> dispatcher.eval((Expression) a, state))
                .toList();

        if (receiver != null && shouldHandleAsStringCall(m, receiver, name)) {
            SymStringOp.Op stringOp = mapStringOp(name);
            if (stringOp != null) {
                return new SymStringOp(receiver, stringOp, args);
            }
        }

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

    private SymStringOp.Op mapStringOp(String name) {
        return switch (name) {
            case "equals" -> SymStringOp.Op.EQUALS;
            case "contains" -> SymStringOp.Op.CONTAINS;
            case "startsWith" -> SymStringOp.Op.STARTS_WITH;
            case "endsWith" -> SymStringOp.Op.ENDS_WITH;
            case "length" -> SymStringOp.Op.LENGTH;
            case "isEmpty" -> SymStringOp.Op.IS_EMPTY;
            case "substring" -> SymStringOp.Op.SUBSTRING;
            case "toLowerCase" -> SymStringOp.Op.TO_LOWER_CASE;
            case "toUpperCase" -> SymStringOp.Op.TO_UPPER_CASE;
            case "trim"        -> SymStringOp.Op.TRIM;
            case "replace"     -> SymStringOp.Op.REPLACE;
            case "indexOf"     -> SymStringOp.Op.INDEX_OF;
            default -> null;
        };
    }

    private boolean shouldHandleAsStringCall(MethodInvocation invocation,
                                             SymbolicValue receiver,
                                             String methodName) {
        return isStringSpecificMethod(methodName)
                || receiver instanceof SymLiteral literal && literal.value() instanceof String
                || invocation.getExpression() != null
                        && invocation.getExpression().resolveTypeBinding() != null
                        && isStringType(invocation.getExpression().resolveTypeBinding().getQualifiedName());
    }

    private boolean isStringSpecificMethod(String methodName) {
        return mapStringOp(methodName) != null;
    }


    private boolean isStringType(String typeName) {
        return "java.lang.String".equals(typeName) || "String".equals(typeName);
    }
}
