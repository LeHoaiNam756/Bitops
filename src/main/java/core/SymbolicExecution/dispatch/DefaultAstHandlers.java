package core.SymbolicExecution.dispatch;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.TypedExpr;
import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValueTemp;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
@Deprecated
public final class DefaultAstHandlers {
    private DefaultAstHandlers() {
    }
    public static AstDispatcher defaultDispatcher() {
        return null;
    }
}
