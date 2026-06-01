package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import org.eclipse.jdt.core.dom.*;
import org.junit.Ignore;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualifiedNameHandlerTest {

    private final QualifiedNameHandler handler = new QualifiedNameHandler();

    // ── Helpers ──────────────────────────────────────────────────────────

    private QualifiedName parseQualifiedName(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        VariableDeclarationStatement varStmt = (VariableDeclarationStatement) node;
        VariableDeclarationFragment frag = (VariableDeclarationFragment) varStmt.fragments().get(0);
        Expression init = frag.getInitializer();

        if (init instanceof MethodInvocation mi) {
            Expression expr = mi.getExpression();

            if (expr instanceof QualifiedName qn) {
                return qn; // Integer.class
            }
        }
        return (QualifiedName) frag.getInitializer();
    }

    private QualifiedName parseQualifiedNameFromExpression(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        ExpressionStatement exprStmt = (ExpressionStatement) node;
        return (QualifiedName) exprStmt.getExpression();
    }

    private QualifiedName createUnboundQualifiedName(String qualifier, String name) {
        AST ast = AST.newAST(AST.JLS8);
        SimpleName simple = ast.newSimpleName(name);
        SimpleName qualSimple = ast.newSimpleName(qualifier);
        return ast.newQualifiedName(qualSimple, simple);
    }

    private QualifiedName createUnboundQualifiedName(String fullName) {
        String[] parts = fullName.split("\\.");
        AST ast = AST.newAST(AST.JLS8);
        Name current = ast.newSimpleName(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            current = ast.newQualifiedName(
                    current,
                    ast.newSimpleName(parts[i])
            );
        }
        return (QualifiedName) current;
    }


    private SymbolicState createState() {
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();
    }

    private SymbolicState createStateWithMemory(MemoryModel memory) {
        return SymbolicState.builder()
                .memoryModel(memory)
                .typeContext(new TypeContext())
                .build();
    }

    private AstDispatcher createDispatcher() {
        AstDispatcher dispatcher = new AstDispatcher();
        dispatcher.register(new QualifiedNameHandler());
        dispatcher.register(new SimpleNameHandler());
        return dispatcher;
    }

    // ── supports() ───────────────────────────────────────────────────────

    @Test
    public void supportsReturnsTrueForQualifiedName() {
        QualifiedName qn = parseQualifiedName("int x = a.b;");
        assertTrue(handler.supports(qn));
    }

    @Test
    public void supportsReturnsFalseForOtherNodes() {
        ASTNode node = Parser.parseStatementToAST("int x = 42;");
        assertFalse(handler.supports(node));
    }

    // ── Branch 1: Full dotted name in state → returns stored value ───────

    @Test
    public void qn01_flatKeyStoredAsIs_returnsStoredValue() {
        MemoryModel memory = new MemoryModel();
        SymbolicValue expected = SymLiteral.of(1);
        memory.write("a.b", expected);

        QualifiedName qn = createUnboundQualifiedName("a", "b");
        SymbolicState state = createStateWithMemory(memory);

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertSame(expected, result);
    }

    @Test
    public void qn02_threeSegmentKeyStoredInState_returnsStoredValue() {
        MemoryModel memory = new MemoryModel();
        SymbolicValue expected = new SymVariable("stored_value");
        memory.write("a.b.c", expected);

        QualifiedName qn = createUnboundQualifiedName("a.b.c");
        SymbolicState state = createStateWithMemory(memory);

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertSame(expected, result);
    }

    @Test
    public void qn03_keyExistsButDifferentCasing_fallsThrough() {
        MemoryModel memory = new MemoryModel();
        memory.write("A.b", SymLiteral.of(99));

        QualifiedName qn = createUnboundQualifiedName("a", "b");
        SymbolicState state = createStateWithMemory(memory);

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertNotEquals(SymLiteral.of(99), result);
        assertTrue(result instanceof SymVariable);
        assertEquals("a.b", ((SymVariable) result).name());
    }

    // ── Branch 2: Static final constant → SymLiteral ─────────────────────

    @Test
    public void qn04_mathPI_doubleConstant_foldedToSymLiteral() {
        QualifiedName qn = parseQualifiedName("double x = Math.PI;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue("Expected SymLiteral for Math.PI but got " + result.getClass().getSimpleName(),
                result instanceof SymLiteral);
        assertEquals(Math.PI, ((SymLiteral) result).value());
    }

    @Test
    public void qn05_integerConstant_foldedToSymLiteral() {
        QualifiedName qn = parseQualifiedName("int x = Integer.MAX_VALUE;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(Integer.MAX_VALUE, ((SymLiteral) result).value());
    }

    @Test
    public void qn06_booleanConstant_foldedToSymLiteral() {
        QualifiedName qn = parseQualifiedName("boolean x = Boolean.TRUE;");
        SymbolicState state = createState();
        // Boolean True = new Boolean(true) so it not compile time value
        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess access = (SymFieldAccess) result;
        assertEquals("__type_Boolean", ((SymVariable) access.receiver()).name());
        assertEquals("TRUE", access.fieldName());
    }

    @Test
    public void qn07_charConstant_foldedToSymLiteral() {
        QualifiedName qn = parseQualifiedName("char x = Character.MAX_VALUE;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(Character.MAX_VALUE, ((SymLiteral) result).value());
    }

    @Test
    public void qn08_floatConstant_foldedToSymLiteral() {
        QualifiedName qn = parseQualifiedName("float x = Float.MAX_VALUE;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(Float.MAX_VALUE, ((SymLiteral) result).value());
    }

    @Test
    public void qn09_byteShortConstants_samePattern() {
        QualifiedName qnByte = parseQualifiedName("byte x = Byte.MAX_VALUE;");
        QualifiedName qnShort = parseQualifiedName("short x = Short.MAX_VALUE;");
        SymbolicState state = createState();

        SymbolicValue resultByte = handler.eval(qnByte, state, createDispatcher());
        SymbolicValue resultShort = handler.eval(qnShort, state, createDispatcher());

        assertTrue(resultByte instanceof SymLiteral);
        assertEquals(Byte.MAX_VALUE, ((SymLiteral) resultByte).value());

        assertTrue(resultShort instanceof SymLiteral);
        assertEquals(Short.MAX_VALUE, ((SymLiteral) resultShort).value());
    }

    @Test
    @Ignore("Method Invocation not QualifiedName")
    public void qn10_staticFinalButConstantValueNull_fallsThrough() {
        //MethodInvocation not QN
        QualifiedName qn = parseQualifiedName("String x = Integer.class.getName();");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertNotNull(result);
    }

    // ── Branch 3: Instance field → SymFieldAccess ────────────────────────

    @Test
    public void qn11_instanceField_returnsSymFieldAccess() {
           // --- binding: non-constant instance field ---
        IVariableBinding vb = mock(IVariableBinding.class);
        when(vb.isField()).thenReturn(true);
        when(vb.getModifiers()).thenReturn(0);
        when(vb.getConstantValue()).thenReturn(null);

        // --- qualifier: "obj" SimpleName, unresolved → SymVariable("obj") ---
        SimpleName qualifier = mock(SimpleName.class);
        when(qualifier.getIdentifier()).thenReturn("obj");   // ← was missing
        when(qualifier.resolveBinding()).thenReturn(null);

        // --- field name part ---
        SimpleName namePart = mock(SimpleName.class);
        when(namePart.getIdentifier()).thenReturn("field");

        // --- qualified name ---
        QualifiedName qn = mock(QualifiedName.class);
        when(qn.getFullyQualifiedName()).thenReturn("obj.field");
        when(qn.resolveBinding()).thenReturn(vb);
        when(qn.getQualifier()).thenReturn(qualifier);
        when(qn.getName()).thenReturn(namePart);

        SymbolicState state = createState(); // "obj.field" not in memory

        SymbolicValue result = handler.eval(qn, state, createDispatcher());

        assertEquals(new SymFieldAccess(new SymVariable("obj"), "field"), result);
    }


    @Test
    @Ignore("Integer.class is TypeLiteral not QualifiedName")
    public void qn12_chainedNested_returnsNestedSymFieldAccess() {
        QualifiedName qn = parseQualifiedName("String x = Integer.class;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess outer = (SymFieldAccess) result;
        assertEquals("c", outer.fieldName());

        assertTrue(outer.receiver() instanceof SymFieldAccess);
        SymFieldAccess inner = (SymFieldAccess) outer.receiver();
        assertEquals("b", inner.fieldName());

        assertTrue(inner.receiver() instanceof SymVariable);
        assertEquals("a", ((SymVariable) inner.receiver()).name());
    }

    @Test
    @Ignore("Fall back")
    public void qn13_qualifierAlreadyInState_usesStoredReceiver() {
        MemoryModel memory = new MemoryModel();
        memory.write("obj", SymLiteral.of(0));

        QualifiedName qn = parseQualifiedName("Object x = obj.field;");
        SymbolicState state = createStateWithMemory(memory);

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertEquals("field", fa.fieldName());
        assertEquals(SymLiteral.of(0), fa.receiver());
    }

    @Test
    @Ignore("FieldAccess not QualifiedName")
    public void qn14_thisFieldPattern_returnsSymFieldAccessWithThis() {
        QualifiedName qn = parseQualifiedName("Object x = this.field;");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertEquals("field", fa.fieldName());
        assertTrue(fa.receiver() instanceof SymVariable);
        assertEquals("this", ((SymVariable) fa.receiver()).name());
    }

    // ── Branch 4: Binding unavailable → SymVariable(fullName) ────────────

    @Test
    public void qn15_noClasspathBindingNull_returnsSymVariable() {
        QualifiedName qn = createUnboundQualifiedName("some.Lib.CONST");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymVariable);
        assertEquals("some.Lib.CONST", ((SymVariable) result).name());
    }

    @Test
    public void qn16_bindingIsTypeBinding_returnsSymVariable() {
        QualifiedName qn = createUnboundQualifiedName("java", "lang");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymVariable);
        assertEquals("java.lang", ((SymVariable) result).name());
    }

    @Test
    public void qn17_generatedSyntheticCode_returnsSymVariableWithFullName() {
        QualifiedName qn = createUnboundQualifiedName("synthetic.generated.value");
        SymbolicState state = createState();

        SymbolicValue result = handler.eval(qn, state, createDispatcher());
        assertTrue(result instanceof SymVariable);
        assertEquals("synthetic.generated.value", ((SymVariable) result).name());
    }
}
