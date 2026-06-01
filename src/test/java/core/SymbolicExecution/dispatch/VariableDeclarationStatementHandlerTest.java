package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import core.SymbolicExecution.model.types.UnknownSymType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;

public class VariableDeclarationStatementHandlerTest {

    private final VariableDeclarationStatementHandler handler = new VariableDeclarationStatementHandler();

    // ─── supports() ───────────────────────────────────────────────────────────

    @Test
    public void supports_returnsTrue_forVariableDeclarationStatement() {
        ASTNode node = parseVariableDeclarationStatement("int x = 1;");
        assertTrue(handler.supports(node));
    }

    @Test
    public void supports_returnsFalse_forOtherNodeTypes() {
        ASTNode node = parseExpressionStatement("System.out.println(\"hi\");");
        assertFalse(handler.supports(node));
    }

    // ─── eval() — basic behavior ─────────────────────────────────────────────

    @Test
    public void eval_singleFragment_withInitializer_writesValue() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 42;");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vds.fragments().get(0);

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        SymbolicValue stubValue = SymLiteral.of(42);
        when(dispatcher.eval(eq(fragment.getInitializer()), eq(state))).thenReturn(stubValue);

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            SymbolicValue result = handler.eval(vds, state, dispatcher);

            assertNull(result);
            verify(memory).write(eq("x"), eq(stubValue));
            verify(dispatcher).eval(eq(fragment.getInitializer()), eq(state));
        }
    }

    @Test
    public void eval_singleFragment_withoutInitializer_writesSymVariable() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x;");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vds.fragments().get(0);

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            handler.eval(vds, state, dispatcher);

            verify(dispatcher, never()).eval(any(), any());

            ArgumentCaptor<SymbolicValue> valueCaptor = ArgumentCaptor.forClass(SymbolicValue.class);
            verify(memory).write(eq("x"), valueCaptor.capture());

            SymbolicValue writtenValue = valueCaptor.getValue();
            assertTrue(writtenValue instanceof core.SymbolicExecution.model.SymVariable);
            assertEquals("x", ((core.SymbolicExecution.model.SymVariable) writtenValue).name());
        }
    }

    @Test
    public void eval_multipleFragments_processesAllFragments() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int a = 1, b = 2, c = 3;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        SymbolicValue stubValue = SymLiteral.of(0);
        when(dispatcher.eval(any(), eq(state))).thenReturn(stubValue);

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            handler.eval(vds, state, dispatcher);

            verify(memory, times(3)).write(anyString(), any(SymbolicValue.class));
            verify(memory).write(eq("a"), any(SymbolicValue.class));
            verify(memory).write(eq("b"), any(SymbolicValue.class));
            verify(memory).write(eq("c"), any(SymbolicValue.class));
            verify(dispatcher, times(3)).eval(any(), eq(state));
        }
    }

    // ─── eval() — TypeContext ordering ────────────────────────────────────────

    @Test
    public void eval_pushesTypeContextBeforeInitializerEval() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vds.fragments().get(0);

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            SymType convertedType = core.SymbolicExecution.model.types.PrimitiveSymType.INT;
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(convertedType);

            handler.eval(vds, state, dispatcher);

            InOrder inOrder = inOrder(typeContext, dispatcher);
            inOrder.verify(typeContext).push(eq(convertedType));
            inOrder.verify(dispatcher).eval(eq(fragment.getInitializer()), eq(state));
        }
    }

    @Test
    public void eval_popsTypeContextAfterEachInitializer() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vds.fragments().get(0);

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            SymType convertedType = core.SymbolicExecution.model.types.PrimitiveSymType.INT;
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(convertedType);

            handler.eval(vds, state, dispatcher);

            InOrder inOrder = inOrder(typeContext, dispatcher);
            inOrder.verify(typeContext).push(eq(convertedType));
            inOrder.verify(dispatcher).eval(eq(fragment.getInitializer()), eq(state));
            inOrder.verify(typeContext).pop();
        }
    }

    @Test
    public void eval_typeContextRestoredEvenIfInitializerThrows() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenThrow(new RuntimeException("eval error"));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            SymType convertedType = core.SymbolicExecution.model.types.PrimitiveSymType.INT;
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(convertedType);

            assertThrows(RuntimeException.class, () -> handler.eval(vds, state, dispatcher));

            verify(typeContext).push(eq(convertedType));
            verify(typeContext, never()).pop();
        }
    }

    // ─── eval() — SymTypeMap integration ──────────────────────────────────────

    @Test
    public void eval_declaredTypeConvertedUsingSymTypeMap() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            handler.eval(vds, state, dispatcher);

            mockedSymTypeMap.verify(() -> SymTypeMap.convert(eq(vds.getType())), times(1));
        }
    }

    @Test
    public void eval_typeContextPushReceivesConvertedType() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            SymType customType = core.SymbolicExecution.model.types.PrimitiveSymType.LONG;
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(customType);

            handler.eval(vds, state, dispatcher);

            verify(typeContext).push(eq(customType));
        }
    }

    // ─── eval() — return value ────────────────────────────────────────────────

    @Test
    public void eval_returnsNull() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = new TypeContext();
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            SymbolicValue result = handler.eval(vds, state, dispatcher);

            assertNull(result);
        }
    }

    // ─── eval() — UnknownType handling ────────────────────────────────────────

    @Test
    public void eval_handlesUnknownType_gracefully() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(UnknownSymType.INSTANCE);

            handler.eval(vds, state, dispatcher);

            verify(typeContext).push(eq(UnknownSymType.INSTANCE));
            verify(memory).write(eq("x"), eq(SymLiteral.of(1)));
        }
    }

    // ─── eval() — edge cases ──────────────────────────────────────────────────

    @Test
    public void eval_forgetsToPopOnEmptyFragments() {
        VariableDeclarationStatement vds = parseVariableDeclarationStatement("int x = 1;");
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) vds.fragments().get(0);

        AstDispatcher dispatcher = mock(AstDispatcher.class);
        MemoryModel memory = mock(MemoryModel.class);
        TypeContext typeContext = spy(new TypeContext());
        SymbolicState state = SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(typeContext)
                .build();

        when(dispatcher.eval(any(), eq(state))).thenReturn(SymLiteral.of(1));

        try (MockedStatic<SymTypeMap> mockedSymTypeMap = mockStatic(SymTypeMap.class)) {
            mockedSymTypeMap.when(() -> SymTypeMap.convert(any())).thenReturn(core.SymbolicExecution.model.types.PrimitiveSymType.INT);

            handler.eval(vds, state, dispatcher);

            verify(typeContext).push(isA(SymType.class));
            verify(typeContext).pop();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private VariableDeclarationStatement parseVariableDeclarationStatement(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        assertTrue("Expected VariableDeclarationStatement but got " + node.getClass().getSimpleName(),
                node instanceof VariableDeclarationStatement);
        return (VariableDeclarationStatement) node;
    }

    private ASTNode parseExpressionStatement(String statement) {
        ASTNode node = utils.Parser.parseStatementToAST(statement);
        assertTrue("Expected ExpressionStatement but got " + node.getClass().getSimpleName(),
                node instanceof ExpressionStatement);
        return (ExpressionStatement) node;
    }
}
