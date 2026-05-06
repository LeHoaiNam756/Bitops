package core.SymbolicExecution.dispatch;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;
import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public final class DefaultAstHandlers {
    private DefaultAstHandlers() {
    }

    public static AstDispatcher defaultDispatcher() {
        return new AstDispatcher()
                .register(new LiteralHandler())
                .register(new VariableDeclarationHandler());
    }

    private static final class LiteralHandler implements AstHandler {
        @Override
        public boolean supports(ASTNode node) {
            return node instanceof NumberLiteral
                    || node instanceof BooleanLiteral
                    || node instanceof CharacterLiteral;
        }

        @Override
        public SymbolicValue eval(ASTNode node, SymbolicStore store, SymbolicContext ctx, AstDispatcher dispatcher) {
            Expr<?> expr;
            if (node instanceof NumberLiteral) {
                int value = Integer.parseInt(((NumberLiteral) node).getToken());
                expr = ctx.z3().mkBV(value, 32);
                return SymbolicValue.of(null, TypedExpr.JavaType.INT, expr, false);
            }
            if (node instanceof BooleanLiteral) {
                boolean value = ((BooleanLiteral) node).booleanValue();
                expr = ctx.z3().mkBool(value);
                return SymbolicValue.of(null, TypedExpr.JavaType.BOOLEAN, expr, false);
            }
            char value = ((CharacterLiteral) node).charValue();
            expr = ctx.z3().mkBV(value, 16);
            return SymbolicValue.of(null, TypedExpr.JavaType.CHAR, expr, false);
        }
    }

    private static final class VariableDeclarationHandler implements AstHandler {
        @Override
        public boolean supports(ASTNode node) {
            return node instanceof VariableDeclarationStatement;
        }

        @Override
        public SymbolicValue eval(ASTNode node, SymbolicStore store, SymbolicContext ctx, AstDispatcher dispatcher) {
            VariableDeclarationStatement stmt = (VariableDeclarationStatement) node;
            @SuppressWarnings("unchecked")
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) stmt.fragments().get(0);
            String name = fragment.getName().getIdentifier();
            Expression initializer = fragment.getInitializer();
            SymbolicValue value = initializer == null
                    ? SymbolicValue.of(name, TypedExpr.JavaType.OTHER, ctx.z3().mkBVConst(name, 32), false)
                    : dispatcher.eval(initializer, store, ctx);
            store.declare(name, value);
            return value;
        }
    }
}
