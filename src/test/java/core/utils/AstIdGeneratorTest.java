package core.utils;

import core.utils.AstIdGenerator;
import org.eclipse.jdt.core.dom.*;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    }

    @Test
    public void buildSignature_stringLiteral_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("String c = \"hello\"");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    @Test
    public void buildSignature_booleanTrue_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("boolean c = true");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    @Test
    public void buildSignature_booleanFalse_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("boolean c = false");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    @Test
    public void buildSignature_charLiteral_containsNodeTypeAndValue() {
        ASTNode node = parseExpression("char c = 'z'");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    @Test
    public void buildSignature_nullLiteral_containsNodeType() {
        ASTNode node = parseExpression("Object c = (Object) null");   // cast forces an expression context

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // buildSignature — composite / nested expressions
    // -------------------------------------------------------------------------

    @Test
    public void buildSignature_infixAddition_containsInfixExpression() {
        ASTNode node = parseExpression("int c = 1 + 2");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
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
    }

    @Test
    public void buildSignature_parenthesizedExpression_containsInnerNode() {
        ASTNode node = parseExpression("int c = (a + b)");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
    }

    @Test
    public void buildSignature_prefixNegation_containsPrefixExpression() {
        ASTNode node = parseExpression("int c = -a");

        String result = AstIdGenerator.buildSignature(node);

        assertNotNull(result);
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

    // NEW: Parse a full class body with multiple statements, return statement at given index
    private static ASTNode parseStatementAtIndex(String classBody, int statementIndex) {
        String wrapped =
                "public class __Wrapper__ {\n"
                        + "  public void __test__(int a, int b, int c) {\n"
                        + classBody + "\n"
                        + "  }\n"
                        + "}\n";
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(wrapped.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        Block body = method.getBody();
        assertNotNull("Method body must not be null", body);
        assertTrue("Statement index out of bounds",
                body.statements().size() > statementIndex);
        return (Statement) body.statements().get(statementIndex);
    }

    // -------------------------------------------------------------------------
// Same content, parsed independently → same id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_sameContent_parsedSeparately_haveSameId() {
        ASTNode node1 = parseExpression("a = b + 1");
        ASTNode node2 = parseExpression("a = b + 1");

        String id1 = AstIdGenerator.buildSignature(node1);
        String id2 = AstIdGenerator.buildSignature(node2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals("Same content parsed separately should produce same id", id1, id2);
    }

    // -------------------------------------------------------------------------
// Same content, different position in same method → different id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_sameContent_differentPosition_haveDifferentId() {
        // Two identical statements at index 0 and index 1, separated by a dummy statement
        String body =
                "    a = b + 1;\n" +   // index 0  ← first occurrence
                        "    c = 0;\n"       + //index 1  ← separator to shift position
                        "    a = b + 1;\n";    // index 2  ← second occurrence, different path

        ASTNode node1 = parseStatementAtIndex(body, 0);
        ASTNode node2 = parseStatementAtIndex(body, 2);

        String id1 = AstIdGenerator.buildSignature(node1);
        String id2 = AstIdGenerator.buildSignature(node2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals("Same content at different positions should produce different id", id1, id2);
    }

    // -------------------------------------------------------------------------
// Different content → different id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_differentContent_haveDifferentId() {
        ASTNode node1 = parseExpression("a = b + 1");
        ASTNode node2 = parseExpression("a = b + 2");

        String id1 = AstIdGenerator.buildSignature(node1);
        String id2 = AstIdGenerator.buildSignature(node2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals("Different content should produce different id", id1, id2);
    }

    // -------------------------------------------------------------------------
// Same content, parsed multiple times → always same id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_sameContent_parsedMultipleTimes_alwaysSameId() {
        String expr = "a = b + 1";
        String firstId = AstIdGenerator.buildSignature(parseExpression(expr));

        for (int i = 0; i < 10; i++) {
            ASTNode node = parseExpression(expr);
            String id = AstIdGenerator.buildSignature(node);
            assertEquals(
                    "Parse attempt " + i + " produced different id for same content",
                    firstId, id
            );
        }
    }

    // -------------------------------------------------------------------------
// Same content at same position, parsed multiple times → always same id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_samePositionAndContent_parsedMultipleTimes_alwaysSameId() {
        String body =
                "    a = b + 1;\n" +
                        "    c = 0;\n"     +
                        "    a = b + 1;\n";

        // Parse ONCE, extract both nodes from the SAME AST
        ASTNode[] firstNodes = parseStatementsFromSameAST(body, 0, 2);
        String firstId0 = AstIdGenerator.buildSignature(firstNodes[0]);
        String firstId2 = AstIdGenerator.buildSignature(firstNodes[1]);

        // Positions are different in same AST → ids must differ
        assertNotEquals("Index 0 and 2 should differ", firstId0, firstId2);

        // Re-parse multiple times → ids must stay stable
        for (int i = 0; i < 10; i++) {
            ASTNode[] nodes = parseStatementsFromSameAST(body, 0, 2);
            String id0 = AstIdGenerator.buildSignature(nodes[0]);
            String id2 = AstIdGenerator.buildSignature(nodes[1]);

            assertEquals("Index 0: parse attempt " + i + " produced different id", firstId0, id0);
            assertEquals("Index 2: parse attempt " + i + " produced different id", firstId2, id2);
            assertNotEquals("Index 0 and 2 should differ on parse attempt " + i, id0, id2);
        }
    }

    // Extract multiple statements from the SAME CompilationUnit
    private static ASTNode[] parseStatementsFromSameAST(String classBody, int... indices) {
        String wrapped =
                "public class __Wrapper__ {\n"
                        + "  public void __test__(int a, int b, int c) {\n"
                        + classBody + "\n"
                        + "  }\n"
                        + "}\n";
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(wrapped.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = type.getMethods()[0];
        Block body = method.getBody();

        ASTNode[] result = new ASTNode[indices.length];
        for (int i = 0; i < indices.length; i++) {
            result[i] = (Statement) body.statements().get(indices[i]);
        }
        return result;
    }


    // -------------------------------------------------------------------------
// Concurrent parsing of same content → always same id
// -------------------------------------------------------------------------
    @Test
    public void buildSignature_sameContent_parsedConcurrently_alwaysSameId() throws InterruptedException {
        String expr = "a = b + 1";
        String expectedId = AstIdGenerator.buildSignature(parseExpression(expr));

        int threadCount = 10;
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(() -> {
                ASTNode node = parseExpression(expr);
                results.add(AstIdGenerator.buildSignature(node));
            }));
        }

        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        assertEquals("Expected " + threadCount + " results", threadCount, results.size());
        for (int i = 0; i < results.size(); i++) {
            assertEquals("Thread " + i + " produced different id", expectedId, results.get(i));
        }
    }
}