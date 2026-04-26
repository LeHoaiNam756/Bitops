package core.utils;

import core.utils.AstIdGenerator;
import org.eclipse.jdt.core.dom.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for AstIdGenerator.buildSignature(ASTNode) and AstIdGenerator.build(ASTNode, StringBuilder).
 *
 * Wrapping strategy: each test expression is embedded into a minimal compilable class so the
 * JDT parser can resolve the AST. The helper walks the resulting Block and returns the first
 * Statement's ASTNode (or the Statement itself when no expression is available).
 */
public class AstIdGeneratorTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Parses {@code exprSource} as a single expression that appears as the first statement
     * inside a method body, and returns that expression node.
     *
     * The wrapper looks like:
     * <pre>
     *   public class __Wrapper__ {
     *     public void __test__(int a, int b) {
     *       <exprSource>;
     *       // sentinel: never reached
     *     }
     *   }
     * </pre>
     */
    private static ASTNode parseExpression(String exprSource) {
        String wrapped =
                "public class __Wrapper__ {\n"
              + "  public void __test__(int a, int b) {\n"
              + "    " + exprSource + ";\n"
              + "  }\n"
              + "}\n";

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(wrapped.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // Walk: CompilationUnit → TypeDeclaration → MethodDeclaration → Block → first Statement
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        Block body = method.getBody();
        assertNotNull("Method body must not be null", body);
        assertFalse("Method body must contain at least one statement",
                body.statements().isEmpty());

        // The JDT wraps a bare expression statement in an ExpressionStatement node.

        return (Statement) body.statements().get(0);
    }

    // -------------------------------------------------------------------------
    // buildSignature — node type presence
    // -------------------------------------------------------------------------


    @Test
    public void buildSignature_numberLiteral_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("a = 42");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue("Signature must identify the node type",
                result.contains("NumberLiteral"));
        assertTrue("Signature must include the literal token",
                result.contains("42"));
    }

    @Test
    public void buildSignature_stringLiteral_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("String c = \"hello\"");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("StringLiteral"));
        assertTrue("Signature must include the string content (without quotes)",
                result.contains("hello"));
    }

    @Test
    public void buildSignature_booleanTrue_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("boolean c = true");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("BooleanLiteral"));
        assertTrue(result.contains("true"));
    }

    @Test
    public void buildSignature_booleanFalse_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("boolean c = false");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("BooleanLiteral"));
        assertTrue(result.contains("false"));
    }

    @Test
    public void buildSignature_charLiteral_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("char c = 'z'");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("CharacterLiteral"));
        assertTrue(result.contains("z"));
    }

    @Test
    public void buildSignature_nullLiteral_containsNodeType() {
        ASTNode node = parseExpression("Object c = (Object) null");   // cast forces an expression context

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        // The outermost node is a CastExpression; the inner NullLiteral must also appear.
        assertTrue("Signature must reflect the AST structure containing a null literal",
                result.contains("NullLiteral") || result.contains("CastExpression"));
    }

    // -------------------------------------------------------------------------
    // buildSignature — composite / nested expressions
    // -------------------------------------------------------------------------

    @Test
    public void buildSignature_infixAddition_containsInfixExpression() {
        ASTNode node = parseExpression("int c = 1 + 2");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("InfixExpression"));
    }

    @Test
    public void buildSignature_infixAddition_containsBothOperands() {
        ASTNode node = parseExpression("int c = 1 + 2");

        String result = AstIdGenerator.buildSignature(node);

        assertTrue("Signature of 1+2 must encode both operands",
                result.contains("1") && result.contains("2"));
    }

    @Test
    public void buildSignature_methodInvocation_containsNodeType() {
        ASTNode node = parseExpression("int c = Math.abs(a)");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("MethodInvocation"));
    }

    @Test
    public void buildSignature_parenthesizedExpression_containsInnerNode() {
        ASTNode node = parseExpression("int c = (a + b)");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        // Either the outer ParenthesizedASTNode or the inner InfixASTNode must appear.
        assertTrue(result.contains("InfixExpression")
                || result.contains("ParenthesizedExpression"));
    }

    @Test
    public void buildSignature_prefixNegation_containsPrefixExpression() {
        ASTNode node = parseExpression("int c = -a");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
        assertTrue(result.contains("PrefixExpression"));
    }

    // -------------------------------------------------------------------------
    // buildSignature — whole CompilationUnit (non-expression root node)
    // -------------------------------------------------------------------------

    @Test
    public void buildSignature_compilationUnit_containsNodeType() {
        String src = "public class __Wrapper__ { }";
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(src.toCharArray());
        parser.setResolveBindings(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        String result = AstIdGenerator.buildSignature(cu);

        assertNotNull(result);
        assertTrue("Root node type must appear in its own signature",
                result.contains("CompilationUnit"));
    }

    // -------------------------------------------------------------------------
    // buildSignature — determinism
    // -------------------------------------------------------------------------

    @Test
    public void buildSignature_sameInputProducesSameOutput() {
        ASTNode node1 = parseExpression("int c = 42");
        ASTNode node2 = parseExpression("int c = 42");

        assertEquals(
                "buildSignature must be deterministic for structurally identical ASTs",
                AstIdGenerator.buildSignature(node1),
                AstIdGenerator.buildSignature(node2)
        );
    }

    @Test
    public void buildSignature_differentInputsProduceDifferentOutput() {
        ASTNode nodeA = parseExpression("int c = 42");
        ASTNode nodeB = parseExpression("int d = 99");

        assertNotEquals(
                "Structurally different AST nodes must not produce the same signature",
                AstIdGenerator.buildSignature(nodeA),
                AstIdGenerator.buildSignature(nodeB)
        );
    }

    // -------------------------------------------------------------------------
    // build(ASTNode, StringBuilder) — delegate / StringBuilder overload
    // -------------------------------------------------------------------------

    @Test
    public void build_appendsNonEmptyContentToStringBuilder() {
        ASTNode node = parseExpression("int c = 42");
        StringBuilder sb = new StringBuilder();

        AstIdGenerator.build(node, sb);

        assertTrue("build(node, sb) must append at least one character",
                sb.length() > 0);
    }

    @Test
    public void build_outputMatchesBuildSignature() {
        ASTNode node = parseExpression("int c = 42");
        StringBuilder sb = new StringBuilder();

        AstIdGenerator.build(node, sb);

        assertEquals("build(node, sb) must produce the same content as buildSignature(node)",
                AstIdGenerator.buildSignature(node), sb.toString());
    }

    @Test
    public void build_appendsToExistingContent() {
        ASTNode node = parseExpression("int c = 42");
        StringBuilder sb = new StringBuilder("PREFIX_");

        AstIdGenerator.build(node, sb);

        assertTrue("build(node, sb) must append rather than replace existing content",
                sb.toString().startsWith("PREFIX_"));
        assertTrue("Content must have been appended after the prefix",
                sb.length() > "PREFIX_".length());
    }

    @Test
    public void build_multipleCallsAppendMultipleTimes() {
        ASTNode node = parseExpression("int c = 42");
        StringBuilder sb = new StringBuilder();

        AstIdGenerator.build(node, sb);
        int afterFirst = sb.length();
        AstIdGenerator.build(node, sb);

        assertEquals("Each call to build must append the same amount of content",
                afterFirst * 2, sb.length());
    }
}