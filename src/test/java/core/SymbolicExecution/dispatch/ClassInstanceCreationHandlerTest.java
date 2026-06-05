package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

public class ClassInstanceCreationHandlerTest {
    private final ClassInstanceCreationHandler handler = new ClassInstanceCreationHandler();

    @Test
    public void supports_classInstanceCreation_returnsTrue() {
        assertTrue(handler.supports(parseInitializer("Object x = new Object();")));
    }

    @Test
    public void eval_returnsFreshObjectSymbol() {
        ClassInstanceCreation creation = parseInitializer("String x = new String(\"abc\");");
        SymbolicState state = state();

        SymbolicValue result = handler.eval(creation, state, dispatcher());

        assertTrue(result instanceof SymVariable);
        assertTrue(((SymVariable) result).name().startsWith("__obj_String_"));
    }

    @Test
    public void eval_evaluatesConstructorArguments() {
        ClassInstanceCreation creation = parseInitializer("String x = new String(\"abc\");");
        SymbolicState state = state();
        RecordingDispatcher dispatcher = new RecordingDispatcher();

        handler.eval(creation, state, dispatcher);

        assertSame(creation.arguments().get(0), dispatcher.lastNode);
        assertSame(state, dispatcher.lastState);
    }

    private ClassInstanceCreation parseInitializer(String statement) {
        VariableDeclarationStatement varDecl = (VariableDeclarationStatement) Parser.parseStatementToAST(statement);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDecl.fragments().get(0);
        return (ClassInstanceCreation) fragment.getInitializer();
    }

    private AstDispatcher dispatcher() {
        return new AstDispatcher()
                .register(new ClassInstanceCreationHandler())
                .register(new StringLiteralHandler());
    }

    private SymbolicState state() {
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();
    }

    private static class RecordingDispatcher extends AstDispatcher {
        private ASTNode lastNode;
        private SymbolicState lastState;

        @Override
        public SymbolicValue eval(ASTNode node, SymbolicState state) {
            this.lastNode = node;
            this.lastState = state;
            return null;
        }
    }
}
