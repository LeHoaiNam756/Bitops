package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnhancedForStatementHandlerTest {

    @Test
    public void eval_symbolicArray_executesBodyWithArrayElementBinding() {
        EnhancedForStatement statement = parse("for (int value : nums) { res ^= value; }");
        SymbolicState state = createState();
        state.getMemoryModel().write("res", SymLiteral.of(0));
        state.getMemoryModel().write("nums", new SymVariable("nums"));

        new EnhancedForStatementHandler().eval(statement, state, createDispatcher());

        SymbolicValue res = state.getMemoryModel().read("res").orElseThrow();
        assertTrue(res instanceof SymBinaryOp);
        SymBinaryOp xor = (SymBinaryOp) res;
        assertEquals(SymBinaryOp.Op.BXOR, xor.op());
        assertEquals(SymLiteral.of(0), xor.left());
        assertTrue(xor.right() instanceof SymArraySelect);
        SymArraySelect selected = (SymArraySelect) xor.right();
        assertEquals(new SymVariable("nums"), selected.arr());
        assertEquals(SymLiteral.of(0), selected.index());
    }

    @Test
    public void eval_initializerBackedArray_executesOncePerKnownElement() {
        ASTNode declaration = Parser.parseStatementToAST("int[] nums = new int[] { 4, 7 };");
        EnhancedForStatement statement = parse("for (int value : nums) { res += value; }");
        SymbolicState state = createState();
        state.getMemoryModel().write("res", SymLiteral.of(0));
        AstDispatcher dispatcher = createDispatcher();

        dispatcher.eval(declaration, state);
        new EnhancedForStatementHandler().eval(statement, state, dispatcher);

        SymbolicValue res = state.getMemoryModel().read("res").orElseThrow();
        assertTrue(res instanceof SymBinaryOp);
        SymBinaryOp secondAdd = (SymBinaryOp) res;
        assertEquals(SymBinaryOp.Op.ADD, secondAdd.op());
        assertTrue(secondAdd.right() instanceof SymArraySelect);
        SymArraySelect selected = (SymArraySelect) secondAdd.right();
        assertEquals(SymLiteral.of(1), selected.index());
    }

    private EnhancedForStatement parse(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        assertTrue(node instanceof EnhancedForStatement);
        return (EnhancedForStatement) node;
    }

    private SymbolicState createState() {
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();
    }

    private AstDispatcher createDispatcher() {
        return new AstDispatcher()
                .register(new BlockHandler())
                .register(new EnhancedForStatementHandler())
                .register(new ExpressionStatementHandler())
                .register(new VariableDeclarationStatementHandler())
                .register(new AssignmentHandler())
                .register(new ArrayAccessHandler())
                .register(new ArrayCreationHandler())
                .register(new SimpleNameHandler())
                .register(new NumberLiteralHandler());
    }
}
