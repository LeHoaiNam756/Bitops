package SymbolicExecution.Expression.Operation;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import test.ParserForTest;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TestInfixExpression {
    static Context context;

    @BeforeClass
    public static void setUp() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        context = new Context(cfg);
    }

    @AfterClass
    public static void tearDown() {
        // Do not call context.close() here.
        // Z3 Expr objects may still be awaiting GC finalization;
        // closing the context causes "context was closed" errors
        // when the GC finalizes them during other test classes.
        context = null;
    }

    @Test
    public void testExecuteInfixExpression_addition() {
        String source = "int result = 5 + 3;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(1, eclipseAstNodes.size());
        
        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);
        
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = varDeclStmt.fragments();
        assertEquals(1, fragments.size());
        
        VariableDeclarationFragment fragment = fragments.get(0);
        Expression initializer = fragment.getInitializer();
        assertNotNull(initializer);
        assertTrue(initializer instanceof InfixExpression);
        
        InfixExpression infixExpression = (InfixExpression) initializer;
        assertEquals(InfixExpression.Operator.PLUS, infixExpression.getOperator());
        
        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);
        
        assertNotNull(result);
        assertTrue(result instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) result).isInteger());
        assertEquals(8, ((LiteralNumberNode) result).getIntValue());
    }

    @Test
    public void testExecuteInfixExpression_subtraction() {
        String source = "int result = 10 - 4;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);
        
        @SuppressWarnings("unchecked")
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        
        assertEquals(InfixExpression.Operator.MINUS, infixExpression.getOperator());
        
        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);
        
        assertTrue(result instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) result).isInteger());
        assertEquals(6, ((LiteralNumberNode) result).getIntValue());
    }

    @Test
    public void testExecuteInfixExpression_multiplication() {
        String source = "int result = 4 * 5;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);
        
        @SuppressWarnings("unchecked")
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        
        assertEquals(InfixExpression.Operator.TIMES, infixExpression.getOperator());
        
        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);
        
        assertTrue(result instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) result).isInteger());
        assertEquals(20, ((LiteralNumberNode) result).getIntValue());
    }

    @Test
    public void testExecuteInfixExpression_division() {
        String source = "int result = 15 / 3;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);
        
        @SuppressWarnings("unchecked")
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        
        assertEquals(InfixExpression.Operator.DIVIDE, infixExpression.getOperator());
        
        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);
        
        assertTrue(result instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) result).isInteger());
        assertEquals(5, ((LiteralNumberNode) result).getIntValue());
    }

    @Test
    public void testExecuteInfixExpression_equals() {
        String source = "boolean result = 5 == 5;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);
        
        @SuppressWarnings("unchecked")
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        
        assertEquals(InfixExpression.Operator.EQUALS, infixExpression.getOperator());
        
        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);
        
        assertNotNull(result);
        assertTrue(result instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) result).isValue());
    }

    @Test
    public void testExecuteInfixExpression_extendedOperands() {
        String source = "int result = 1 + 2 + 3 + 4;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(0);

        @SuppressWarnings("unchecked")
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();

        assertEquals(InfixExpression.Operator.PLUS, infixExpression.getOperator());
        assertEquals(2, infixExpression.extendedOperands().size());

        MemoryModel memoryModel = new MemoryModel();
        AstNode result = AstNode.executeASTNode(infixExpression, memoryModel);

        assertNotNull(result);
        assertTrue(result instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) result).isInteger());
        assertEquals(10, ((LiteralNumberNode) result).getIntValue());
    }

    @Test
    public void testCreateZ3Expression_addition() {
        String source = "int n;int result = n + 3;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        assertNotNull(infixExpressionNode);
        assertTrue(infixExpressionNode instanceof InfixExpressionNode);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(
                infixNode,
                context,
                memoryModel
        );
        assertNotNull(z3Expr);
        assertEquals("(bvadd n #x00000003)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_subtraction() {
        String source = "int x;int result = x - 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode xNode = SimpleNameNode.of("x");
        memoryModel.assignVariable("x", xNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsub x #x00000005)", z3Expr.toString());

    }

    @Test
    public void testCreateZ3Expression_multiplication() {
        String source = "int a;int result = a * 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode aNode = SimpleNameNode.of("a");
        memoryModel.assignVariable("a", aNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvmul a #x00000002)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_division() {
        String source = "int b;int result = b / 4;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode bNode = SimpleNameNode.of("b");
        memoryModel.assignVariable("b", bNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsdiv b #x00000004)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_remainder() {
        String source = "int c;int result = c % 7;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsrem c #x00000007)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_lessThan() {
        String source = "int d;boolean result = d < 10;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode dNode = SimpleNameNode.of("d");
        memoryModel.assignVariable("d", dNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvslt d #x0000000a)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_greaterThan() {
        String source = "int e;boolean result = e > 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode eNode = SimpleNameNode.of("e");
        memoryModel.assignVariable("e", eNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsgt e #x00000005)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_equals() {
        String source = "int f;boolean result = f == 0;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode fNode = SimpleNameNode.of("f");
        memoryModel.assignVariable("f", fNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(= f #x00000000)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_notEquals() {
        String source = "int g;boolean result = g != 1;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode gNode = SimpleNameNode.of("g");
        memoryModel.assignVariable("g", gNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(distinct g #x00000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_bitwiseAnd() {
        String source = "int h;int result = h & 0xFF;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode hNode = SimpleNameNode.of("h");
        memoryModel.assignVariable("h", hNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvand h #x000000ff)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_bitwiseOr() {
        String source = "int i;int result = i | 0x01;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode iNode = SimpleNameNode.of("i");
        memoryModel.assignVariable("i", iNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvor i #x00000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_bitwiseXor() {
        String source = "int j;int result = j ^ 0x0F;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode jNode = SimpleNameNode.of("j");
        memoryModel.assignVariable("j", jNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvxor j #x0000000f)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_leftShift() {
        String source = "int k;int result = k << 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode kNode = SimpleNameNode.of("k");
        memoryModel.assignVariable("k", kNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvshl k (bvand #x00000002 #x0000001f))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_rightShiftSigned() {
        String source = "int l;int result = l >> 1;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode lNode = SimpleNameNode.of("l");
        memoryModel.assignVariable("l", lNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvashr l (bvand #x00000001 #x0000001f))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_rightShiftUnsigned() {
        String source = "int m;int result = m >>> 1;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode mNode = SimpleNameNode.of("m");
        memoryModel.assignVariable("m", mNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvlshr m (bvand #x00000001 #x0000001f))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_booleanAnd() {
        String source = "boolean p;boolean result = p && true;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode pNode = SimpleNameNode.of("p");
        memoryModel.assignVariable("p", pNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(and p true)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_booleanOr() {
        String source = "boolean q;boolean result = q || false;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode qNode = SimpleNameNode.of("q");
        memoryModel.assignVariable("q", qNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(or q false)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_longLiteral() {
        String source = "long n;long result = n + 3L;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvadd n #x0000000000000003)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charUnsigned() {
        String source = "char c;char result = c + 1;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvadd ((_ zero_extend 16) c) #x00000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charDivision() {
        String source = "char c;char result = c / 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvudiv ((_ zero_extend 16) c) #x00000002)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charComparison() {
        String source = "char c;boolean result = c < 100;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvult ((_ zero_extend 16) c) #x00000064)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_extendedOperands() {
        String source = "int x;int result = x + 1 + 2 + 3;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode xNode = SimpleNameNode.of("x");
        memoryModel.assignVariable("x", xNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvadd x #x00000001 #x00000002 #x00000003)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_mixedIntLong() {
        String source = "int a;long b;long result = a + b;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        SimpleNameNode aNode = SimpleNameNode.of("a");
        SimpleNameNode bNode = SimpleNameNode.of("b");
        memoryModel.assignVariable("a", aNode);
        memoryModel.assignVariable("b", bNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(2);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvadd ((_ sign_extend 32) a) b)", z3Expr.toString());

    }

    @Test
    public void testCreateZ3Expression_lessThanOrEqual() {
        String source = "int x;boolean result = x <= 10;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode xNode = SimpleNameNode.of("x");
        memoryModel.assignVariable("x", xNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsle x #x0000000a)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_greaterThanOrEqual() {
        String source = "int x;boolean result = x >= 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode xNode = SimpleNameNode.of("x");
        memoryModel.assignVariable("x", xNode);
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) eclipseAstNodes.get(1);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) varDeclStmt.fragments().get(0);
        InfixExpression infixExpression = (InfixExpression) fragment.getInitializer();
        AstNode infixExpressionNode = InfixExpressionNode.executeInfixExpression(infixExpression, memoryModel);
        InfixExpressionNode infixNode = (InfixExpressionNode) infixExpressionNode;
        Expr<?> z3Expr = InfixExpressionNode.convertInfixExpressionToZ3Expr(infixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsge x #x00000005)", z3Expr.toString());
    }

    
}
