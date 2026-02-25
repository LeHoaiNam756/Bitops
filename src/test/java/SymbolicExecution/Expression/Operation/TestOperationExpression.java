package SymbolicExecution.Expression.Operation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.MemoryModel;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.*;

public class TestOperationExpression {
    @Test
    public void testOperationExpression_1() {
        String src = "int a = (1 + 2);";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(3, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_2() {
        String src = "int a = (1 + 2) * 3;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(9, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_3() {
        String src = "int a = (1 + 2) * (2 - 4);";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(-6, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_4() {
        String src = "int a = (1 + 2) + 3 + 4 + 5;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(15, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_5() {
        String src = "int a = 10 - (2 + 3) * 2;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(0, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_6() {
        String src = "int a = (1 + (2 + (3 + 4)));";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(10, ((LiteralNumberNode) aValue).getIntValue());
    }

    @Test
    public void testOperationExpression_7() {
        String src = "int a = (10 - (2 * 3 - (10 / 5) ) + (1));";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("a"));
        AstNode aValue = memoryModel.accessVariable("a");
        assertTrue(aValue instanceof LiteralNumberNode);
        assertEquals(7, ((LiteralNumberNode) aValue).getIntValue());
    }
}
