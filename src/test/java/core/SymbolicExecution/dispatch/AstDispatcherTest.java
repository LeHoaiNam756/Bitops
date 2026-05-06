package core.SymbolicExecution.dispatch;

import com.microsoft.z3.Expr;
import core.SymbolicExecution.execution.SymbolicContext;
import core.SymbolicExecution.model.SymbolicStore;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class AstDispatcherTest {
    @Test
    public void evaluatesLiteralExpression() {
        List<ASTNode> nodes = ParserForTest.parseSourceToAstNodeList("int x = 1;");
        ASTNode node = nodes.get(0);

        AstDispatcher dispatcher = DefaultAstHandlers.defaultDispatcher();
        SymbolicStore store = new SymbolicStore();
        SymbolicContext ctx = new SymbolicContext();
        SymbolicValue value = dispatcher.eval(node, store, ctx);
        Expr<?> expr = value.getExpr();

        assertNotNull(expr);
    }
}
