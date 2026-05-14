package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;

public class PostfixExpressionHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
        return node instanceof PostfixExpression;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        PostfixExpression e = (PostfixExpression) node;
        String baseName = ((Name) e.getOperand()).getFullyQualifiedName();
        SymbolicValue v = dispatcher.eval(e.getOperand(), state);
        SymbolicValue old = state.getMemoryModel().read(baseName).orElse(v);
        SymBinaryOp.Op op = (e.getOperator() == PostfixExpression.Operator.INCREMENT) ? SymBinaryOp.Op.ADD
                : SymBinaryOp.Op.SUB;
        SymbolicValue updated = new SymBinaryOp(old, op, SymLiteral.of(1));
        state.getMemoryModel().write(baseName, updated);
        return old; // post: return OLD value
    }
}
