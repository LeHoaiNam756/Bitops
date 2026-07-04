package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.enumerations.Z3_decl_kind;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Counts the distinct AST nodes and free symbolic constants in Z3 assertions. */
final class Z3ExpressionMetrics {
    private Z3ExpressionMetrics() {}

    static Counts count(Expr<?>[] assertions) {
        Set<Integer> expressions = new HashSet<>();
        Set<Integer> variables = new HashSet<>();
        ArrayDeque<Expr<?>> pending = new ArrayDeque<>();
        for (Expr<?> assertion : assertions) {
            if (assertion != null) {
                pending.push(assertion);
            }
        }

        while (!pending.isEmpty()) {
            Expr<?> expression = pending.pop();
            if (!expressions.add(expression.getId())) {
                continue;
            }

            if (isFreeSymbolicConstant(expression)) {
                variables.add(expression.getId());
            }

            if (expression instanceof Quantifier quantifier) {
                pending.push(quantifier.getBody());
            } else {
                for (Expr<?> argument : expression.getArgs()) {
                    pending.push(argument);
                }
            }
        }

        return new Counts(variables.size(), expressions.size());
    }

    private static boolean isFreeSymbolicConstant(Expr<?> expression) {
        return expression.isConst()
                && expression.getFuncDecl().getDeclKind() == Z3_decl_kind.Z3_OP_UNINTERPRETED;
    }

    record Counts(int variableCount, int expressionCount) {}
}
