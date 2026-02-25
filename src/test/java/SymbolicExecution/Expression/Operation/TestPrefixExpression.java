package SymbolicExecution.Expression.Operation;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PrefixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.Variable;
import test.ParserForTest;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TestPrefixExpression {
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
    public void testPrefixExpression_1() {
        String src = "int a = 4;\na = ++a;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(5, ((LiteralNumberNode) aValue).getIntegerValue());
    }

    @Test
    public void testPrefixExpression_2() {
        String src = "int a = 4;\na = --a;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(3, ((LiteralNumberNode) aValue).getIntegerValue());
    }

    @Test
    public void testPrefixExpression_3() {
        String src = "int a = 4;\nint b = -a;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        AstNode bValue = memoryModel.accessVariable("b");
        assertTrue(bValue instanceof LiteralNumberNode);
        assertEquals(-4, ((LiteralNumberNode) bValue).getIntegerValue());
    }

    @Test
    public void testPrefixExpression_4() {
        String src = "boolean flag = false;\nboolean result = !flag;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("result"));
        AstNode resultValue = memoryModel.accessVariable("result");
        assertTrue(resultValue instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) resultValue).isValue());
    }


    @Test
    public void testPrefixExpression_5() {
       String src = "int a = 4;\nint b = ~a;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        AstNode bValue = memoryModel.accessVariable("b");
        assertTrue(bValue instanceof LiteralNumberNode);
        assertEquals(~4, ((LiteralNumberNode) bValue).getIntegerValue());
    }

    @Test
    public void testPrefixExpression_6() {
        String src = "int n;\nint a = n+1;\nint b = ++a;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(2), memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        AstNode bValue = memoryModel.accessVariable("b");
        assertTrue(bValue instanceof PrefixExpressionNode);
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) bValue;
        assertEquals("++(n+1)", prefixNode.toString());
    }

    @Test
    public void testCreateZ3Expression_increment() {
        String src = "int n;\nint a = ++n;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof PrefixExpressionNode);
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Variable nVar = memoryModel.getVariable("n");
        assertNotNull(nVar);
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(
            prefixNode,
            context,
            memoryModel
        );
        assertNotNull(z3Expr);
        assertEquals("(bvadd n #x00000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_decrement() {
        String src = "int x;\nint a = --x;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode xNode = SimpleNameNode.of("x");
        memoryModel.assignVariable("x", xNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvsub x #x00000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_unaryMinus() {
        String src = "int y;\nint a = -y;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode yNode = SimpleNameNode.of("y");
        memoryModel.assignVariable("y", yNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvneg y)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_unaryPlus() {
        String src = "int z;\nint a = +z;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode zNode = SimpleNameNode.of("z");
        memoryModel.assignVariable("z", zNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("z", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_bitwiseComplement() {
        String src = "int w;\nint a = ~w;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode wNode = SimpleNameNode.of("w");
        memoryModel.assignVariable("w", wNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvnot w)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_booleanNot() {
        String src = "boolean flag;\nboolean result = !flag;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode flagNode = SimpleNameNode.of("flag");
        memoryModel.assignVariable("flag", flagNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode resultValue = memoryModel.accessVariable("result");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) resultValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(not flag)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_longIncrement() {
        String src = "long l;\nlong a = ++l;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode lNode = SimpleNameNode.of("l");
        memoryModel.assignVariable("l", lNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvadd l #x0000000000000001)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charIncrement() {
        String src = "char c;\nchar a = ++c;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("((_ extract 15 0) (bvadd ((_ zero_extend 16) c) #x00000001))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charUnaryMinus() {
        String src = "char c;\nint a = -c;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvneg ((_ zero_extend 16) c))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charUnaryPlus() {
        String src = "char c;\nint a = +c;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("((_ zero_extend 16) c)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_charComplement() {
        String src = "char c;\nint a = ~c;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode cNode = SimpleNameNode.of("c");
        memoryModel.assignVariable("c", cNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvnot ((_ zero_extend 16) c))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_shortIncrement() {
        String src = "short s;\nshort a = ++s;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode sNode = SimpleNameNode.of("s");
        memoryModel.assignVariable("s", sNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("((_ extract 15 0) (bvadd ((_ sign_extend 16) s) #x00000001))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_shortUnaryMinus() {
        String src = "short s;\nint a = -s;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode sNode = SimpleNameNode.of("s");
        memoryModel.assignVariable("s", sNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvneg ((_ sign_extend 16) s))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_byteIncrement() {
        String src = "byte b;\nbyte a = ++b;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode bNode = SimpleNameNode.of("b");
        memoryModel.assignVariable("b", bNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("((_ extract 7 0) (bvadd ((_ sign_extend 24) b) #x00000001))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_byteUnaryMinus() {
        String src = "byte b;\nint a = -b;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode bNode = SimpleNameNode.of("b");
        memoryModel.assignVariable("b", bNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(bvneg ((_ sign_extend 24) b))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_floatUnaryMinus() {
        String src = "float f;\nfloat a = -f;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode fNode = SimpleNameNode.of("f");
        memoryModel.assignVariable("f", fNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(fp.neg f)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_doubleUnaryMinus() {
        String src = "double d;\ndouble a = -d;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode dNode = SimpleNameNode.of("d");
        memoryModel.assignVariable("d", dNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(fp.neg d)", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_floatIncrement() {
        String src = "float f;\nfloat a = ++f;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode fNode = SimpleNameNode.of("f");
        memoryModel.assignVariable("f", fNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals("(fp.add roundNearestTiesToEven f (fp #b0 #x7f #b00000000000000000000000))", z3Expr.toString());
    }

    @Test
    public void testCreateZ3Expression_doubleDecrement() {
        String src = "double d;\ndouble a = --d;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode dNode = SimpleNameNode.of("d");
        memoryModel.assignVariable("d", dNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode aValue = memoryModel.accessVariable("a");
        PrefixExpressionNode prefixNode = (PrefixExpressionNode) aValue;
        Expr<?> z3Expr = PrefixExpressionNode.convertPrefixExpressionToZ3Expr(prefixNode, context, memoryModel);
        
        assertNotNull(z3Expr);
        assertEquals(
                "(fp.sub roundNearestTiesToEven d (fp #b0 #b01111111111 #x0000000000000))",
                z3Expr.toString());
    }
}
