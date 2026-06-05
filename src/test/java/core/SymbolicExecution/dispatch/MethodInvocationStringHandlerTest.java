package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymStringOp;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodInvocationStringHandlerTest {

    private MethodInvocation parseInitializer(String statement) {
        VariableDeclarationStatement stmt =
                (VariableDeclarationStatement) Parser.parseStatementToAST(statement);
        VariableDeclarationFragment frag = (VariableDeclarationFragment) stmt.fragments().get(0);
        return (MethodInvocation) frag.getInitializer();
    }

    private AstDispatcher dispatcher() {
        return new AstDispatcher()
                .register(new MethodInvocationHandler())
                .register(new SimpleNameHandler())
                .register(new StringLiteralHandler())
                .register(new NumberLiteralHandler());
    }

    private SymbolicState stateWithStringVariable() {
        MemoryModel memory = new MemoryModel();
        memory.write("s", new SymVariable("s"));
        return SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(new TypeContext())
                .build();
    }

    @Test
    public void eval_contains_returnsSymStringOp() {
        MethodInvocation call = parseInitializer("boolean b = s.contains(\"arg\");");

        SymbolicValue result = new MethodInvocationHandler().eval(call, stateWithStringVariable(), dispatcher());

        assertTrue(result instanceof SymStringOp);
        SymStringOp op = (SymStringOp) result;
        assertEquals(SymStringOp.Op.CONTAINS, op.op());
        assertEquals(new SymVariable("s"), op.receiver());
        assertEquals(SymLiteral.of("arg"), op.args().get(0));
    }

    @Test
    public void eval_substring_returnsSymStringOpWithIndexes() {
        MethodInvocation call = parseInitializer("String sub = s.substring(1, 3);");

        SymbolicValue result = new MethodInvocationHandler().eval(call, stateWithStringVariable(), dispatcher());

        assertTrue(result instanceof SymStringOp);
        SymStringOp op = (SymStringOp) result;
        assertEquals(SymStringOp.Op.SUBSTRING, op.op());
        assertEquals(SymLiteral.of(1), op.args().get(0));
        assertEquals(SymLiteral.of(3), op.args().get(1));
    }
}
