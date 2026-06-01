package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VariableDeclarationExpressionHandlerTest {
    private final VariableDeclarationExpressionHandler handler = new VariableDeclarationExpressionHandler();

    @Test
    public void supports_returnsTrue_forVariableDeclarationExpression() {
        ASTNode node = parseVariableDeclarationExpression("for (int i = 0; i < 1; i++) {}");

        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnsFalse_forOtherNodeTypes() {
        ASTNode node = utils.Parser.parseStatementToAST("for (i = 0; i < 1; i++) {}");

        assertFalse(handler.supports(node));
    }

    @Test
    public void eval_singleFragment_withInitializer_writesValueAndReturnsNull() {
        VariableDeclarationExpression vde = parseVariableDeclarationExpression("for (int i = 0; i < 1; i++) {}");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vde.fragments().get(0);
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();
        SymbolicValue value = SymLiteral.of(0);
        when(dispatcher.eval(eq(fragment.getInitializer()), eq(state))).thenReturn(value);

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(PrimitiveSymType.INT);

            SymbolicValue result = handler.eval(vde, state, dispatcher);

            assertNull(result);
            mockedSymTypeMap.verify(() -> SymTypeMap.convert(eq(vde.getType())));
            verify(dispatcher).eval(eq(fragment.getInitializer()), eq(state));
            verify(memory).write(eq("i"), eq(value));
        }
    }

    @Test
    public void eval_singleFragment_withoutInitializer_writesSymVariable() {
        VariableDeclarationExpression vde = parseVariableDeclarationExpression("for (int i; ; ) {}");
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(PrimitiveSymType.INT);

            handler.eval(vde, state, dispatcher);

            verify(dispatcher, never()).eval(any(), any());
            ArgumentCaptor<SymbolicValue> valueCaptor = ArgumentCaptor.forClass(SymbolicValue.class);
            verify(memory).write(eq("i"), valueCaptor.capture());
            assertEquals(new SymVariable("i"), valueCaptor.getValue());
        }
    }

    @Test
    public void eval_pushesTypeContextBeforeInitializerEvalAndPopsAfter() {
        VariableDeclarationExpression vde = parseVariableDeclarationExpression("for (int i = 0; i < 1; i++) {}");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vde.fragments().get(0);
        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = org.mockito.Mockito.spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();
        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(0));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(PrimitiveSymType.INT);

            handler.eval(vde, state, dispatcher);

            InOrder inOrder = inOrder(typeContext, dispatcher);
            inOrder.verify(typeContext).push(eq(PrimitiveSymType.INT));
            inOrder.verify(dispatcher).eval(eq(fragment.getInitializer()), eq(state));
            inOrder.verify(typeContext).pop();
        }
    }

    private VariableDeclarationExpression parseVariableDeclarationExpression(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        assertTrue("Expected ForStatement but got " + node.getClass().getSimpleName(), node instanceof ForStatement);
        Object initializer = ((ForStatement) node).initializers().get(0);
        assertTrue("Expected VariableDeclarationExpression but got " + initializer.getClass().getSimpleName(),
                initializer instanceof VariableDeclarationExpression);
        return (VariableDeclarationExpression) initializer;
    }
}
