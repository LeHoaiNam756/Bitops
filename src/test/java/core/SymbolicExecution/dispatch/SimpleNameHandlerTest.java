package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

public class SimpleNameHandlerTest {

    private final SimpleNameHandler handler = new SimpleNameHandler();

    // --- Helpers ---

    private SimpleName findSimpleNameInSource(String source, String identifier) {
        CompilationUnit cu = parseCompilationUnit(source);
        final SimpleName[] result = new SimpleName[1];
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                if (node.getIdentifier().equals(identifier)) {
                    result[0] = node;
                    return false;
                }
                return true;
            }
        });
        assertNotNull("SimpleName '" + identifier + "' not found in source", result[0]);
        return result[0];
    }

    private SimpleName findTypeSimpleName(String source) {
        CompilationUnit cu = parseCompilationUnit(source);
        final SimpleName[] result = new SimpleName[1];
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleType node) {
                result[0] = (SimpleName) node.getName();
                return false;
            }
        });
        assertNotNull("No SimpleType found in source", result[0]);
        return result[0];
    }

    private SimpleName findSimpleNameInMethodInvocation(String source, String identifier) {
        CompilationUnit cu = parseCompilationUnit(source);
        final SimpleName[] result = new SimpleName[1];
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                SimpleName name = node.getName();
                if (name.getIdentifier().equals(identifier)) {
                    result[0] = name;
                    return false;
                }
                return true;
            }
        });
        assertNotNull("MethodInvocation SimpleName '" + identifier + "' not found", result[0]);
        return result[0];
    }

    private SimpleName findSimpleNameInVariableUsage(String source, String identifier) {
    CompilationUnit cu = parseCompilationUnit(source);

    final SimpleName[] result = new SimpleName[1];

    cu.accept(new ASTVisitor() {

        private ASTNode declarationNode;

        @Override
        public boolean visit(SingleVariableDeclaration node) {
            if (node.getName().getIdentifier().equals(identifier)) {
                declarationNode = node.getName();
            }
            return true;
        }

        @Override
        public boolean visit(VariableDeclarationFragment node) {
            if (node.getName().getIdentifier().equals(identifier)) {
                declarationNode = node.getName();
            }
            return true;
        }

        @Override
        public boolean visit(SimpleName node) {
            if (node == declarationNode) {
                return true;
            }

            if (declarationNode != null &&
                    node.getIdentifier().equals(identifier)) {

                result[0] = node;
                return false;
            }

            return true;
        }
    });

    assertNotNull("Usage SimpleName '" + identifier + "' not found", result[0]);
    return result[0];
}

    private CompilationUnit parseCompilationUnit(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setUnitName("Test.java");
        String[] classpath =
                System.getProperty("java.class.path")
                        .split(File.pathSeparator);
        parser.setEnvironment(
                classpath,
                new String[]{""},
                null,
                true
        );
        return (CompilationUnit) parser.createAST(null);
    }


    private SymbolicState createState() {
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .build();
    }

    private SymbolicState createStateWithMemory(Map<String, SymbolicValue> entries) {
        MemoryModel mm = new MemoryModel();
        for (Map.Entry<String, SymbolicValue> e : entries.entrySet()) {
            mm.write(e.getKey(), e.getValue());
        }
        return SymbolicState.builder()
                .memoryModel(mm)
                .build();
    }

    private SimpleName createUnresolvedSimpleName(String identifier) {
        AST ast = AST.newAST(AST.JLS8);
        return ast.newSimpleName(identifier);
    }

    // --- supports() tests ---

    @Test
    public void supportsReturnsTrueForSimpleName() {
        AST ast = AST.newAST(AST.JLS8);
        SimpleName name = ast.newSimpleName("x");
        assertTrue(handler.supports(name));
    }

    @Test
    public void supportsReturnsFalseForOtherNodes() {
        AST ast = AST.newAST(AST.JLS8);
        assertFalse(handler.supports(ast.newStringLiteral()));
        assertFalse(handler.supports(ast.newNumberLiteral()));
        assertFalse(handler.supports(ast.newBooleanLiteral(false)));
    }

    // ============================================================
    // Branch 1 — Type binding → SymVariable("__type_*")
    // ============================================================

    @Test
    public void sn01_simpleTypeName() {
        String source = "class Test { void m() { String s = null; } }";
        SimpleName name = findTypeSimpleName(source);
        assertEquals("String", name.getIdentifier());

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymVariable);
        assertEquals("__type_String", ((SymVariable) result).name());
    }

    @Test
    public void sn02_primitiveTypeName() {
        String source = "class Test { void m() { Object o = null; } }";
        SimpleName name = findTypeSimpleName(source);
        assertEquals("Object", name.getIdentifier());

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymVariable);
        assertEquals("__type_Object", ((SymVariable) result).name());
    }

    // Branch 2 — Compile-time constant field → SymLiteral
    @Test
    public void sn03_intConstant() {
        String source = "class T { static final int C = 42; void m() { int x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(42, ((SymLiteral) result).value());
    }

    @Test
    public void sn04_longConstant() {
        String source = "class T { static final long C = 100L; void m() { long x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(100L, ((SymLiteral) result).value());
    }

    @Test
    public void sn05_doubleConstant() {
        String source = "class T { static final double C = 3.14; void m() { double x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14d, ((SymLiteral) result).value());
    }

    @Test
    public void sn06_booleanConstant() {
        String source = "class T { static final boolean C = true; void m() { boolean x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void sn07_charConstant() {
        String source = "class T { static final char C = 'A'; void m() { char x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals('A', ((SymLiteral) result).value());
    }

    @Test
    public void sn08_floatConstant() {
        String source = "class T { static final float C = 1.5f; void m() { float x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(1.5f, ((SymLiteral) result).value());
    }

    @Test
    public void sn09_shortConstant() {
        String source = "class T { static final short C = 5; void m() { short x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals((short) 5, ((SymLiteral) result).value());
    }

    @Test
    public void sn10_byteConstant() {
        String source = "class T { static final byte C = 1; void m() { byte x = C; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "C");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals((byte) 1, ((SymLiteral) result).value());
    }

    @Test
    public void sn11_staticNotFinal_fallsThrough() {
        String source = "class T { static int count = 5; void m() { int x = count; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "count");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        // Not a compile-time constant (not final), falls through to field access
        assertTrue("Expected SymFieldAccess for static non-final field, got: " + result.getClass().getSimpleName(),
                result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertEquals("this", ((SymVariable) fa.receiver()).name());
        assertEquals("count", fa.fieldName());
    }

    @Test
    public void sn12_finalNotStatic_fallsThrough() {
        String source = "class T { final int limit = 10; void m() { int x = limit; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "limit");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        // Not a compile-time constant (not static), falls through to field access
        assertTrue("Expected SymFieldAccess for non-static final field, got: " + result.getClass().getSimpleName(),
                result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertEquals("this", ((SymVariable) fa.receiver()).name());
        assertEquals("limit", fa.fieldName());
    }

    @Test
    public void sn13_staticFinalNullConstant_fallsThrough() {
        String source = "class T { static final String S = null; void m() { String x = S; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "S");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        // getConstantValue() returns null, so isCompileTimeConstant is false
        assertTrue("Expected SymFieldAccess for static final with null constant, got: " + result.getClass().getSimpleName(),
                result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertEquals("this", ((SymVariable) fa.receiver()).name());
        assertEquals("S", fa.fieldName());
    }

    // ============================================================
    // Branch 3 — Variable present in state → returns stored value
    // ============================================================

    @Test
    public void sn14_localVariableInScope() {
        String source = "class T { void m() { int x = 0; } }";
        SimpleName name = findSimpleNameInSource(source, "x");

        Map<String, SymbolicValue> memory = new HashMap<>();
        memory.put("x", SymLiteral.of(7));

        SymbolicValue result = handler.eval(name, createStateWithMemory(memory), new AstDispatcher());

        assertTrue(result instanceof SymLiteral);
        assertEquals(7, ((SymLiteral) result).value());
    }

    @Test
    public void sn15_parameterInMemory() {
        String source = "class T { void m(int param) { int x = param; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "param");

        Map<String, SymbolicValue> memory = new HashMap<>();
        memory.put("param", new SymVariable("param"));

        SymbolicValue result = handler.eval(name, createStateWithMemory(memory), new AstDispatcher());

        assertTrue(result instanceof SymVariable);
        assertEquals("param", ((SymVariable) result).name());
    }

    // ============================================================
    // Branch 4 — Unresolved field via implicit this → SymFieldAccess
    // ============================================================

    @Test
    public void sn16_bareInstanceFieldInsideMethod() {
        String source = "class T { int count; void m() { int x = count; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "count");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertTrue(fa.receiver() instanceof SymVariable);
        assertEquals("this", ((SymVariable) fa.receiver()).name());
        assertEquals("count", fa.fieldName());
    }

    @Test
    public void sn17_fieldNameNoStateEntry() {
        String source = "class T { String value; void m() { String s = value; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "value");

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymFieldAccess);
        SymFieldAccess fa = (SymFieldAccess) result;
        assertTrue(fa.receiver() instanceof SymVariable);
        assertEquals("this", ((SymVariable) fa.receiver()).name());
        assertEquals("value", fa.fieldName());
    }

    // ============================================================
    // Branch 5 — Unknown / unresolved binding → SymVariable
    // ============================================================

    @Test
    public void sn18_nullBinding() {
        SimpleName name = createUnresolvedSimpleName("unknown");
        assertNull(name.resolveBinding());

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        assertTrue(result instanceof SymVariable);
        assertEquals("unknown", ((SymVariable) result).name());
    }

    @Test
    public void sn19_nonFieldVariableNotInState() {
        String source = "class T { void m() { int val = 5; int y = val; } }";
        SimpleName name = findSimpleNameInVariableUsage(source, "val");

        IBinding binding = name.resolveBinding();
        assertNotNull(binding);
        assertTrue(binding instanceof IVariableBinding);
        assertFalse(((IVariableBinding) binding).isField());

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        // Not in state, not a field → fallback to SymVariable
        assertTrue(result instanceof SymVariable);
        assertEquals("val", ((SymVariable) result).name());
    }

    @Test
    public void sn20_methodBinding() {
        String source = "class T { void target() {} void m() { target(); } }";
        SimpleName name = findSimpleNameInMethodInvocation(source, "target");

        IBinding binding = name.resolveBinding();
        assertNotNull(binding);
        assertTrue("Expected IMethodBinding but got: " + binding.getClass().getSimpleName(),
                binding instanceof IMethodBinding);

        SymbolicValue result = handler.eval(name, createState(), new AstDispatcher());

        // IMethodBinding is neither ITypeBinding nor IVariableBinding → fallback
        assertTrue(result instanceof SymVariable);
        assertEquals("target", ((SymVariable) result).name());
    }
}
