package SymbolicExecution.Expression.Operation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.MemoryModel;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestParenthesizedExpression {
    @Test
    public void testParenthesizedExpression_1() {
        String source = "int x = (3);";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        AstNode xValue = memoryModel.accessVariable("x");
        assertTrue(xValue instanceof LiteralNumberNode);
        assertEquals(3, ((LiteralNumberNode) xValue).getIntValue());
    }

    @Test
    public void testParenthesizedExpression_2() {
        String source = "int y = ((9));";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("y"));
        AstNode yValue = memoryModel.accessVariable("y");
        assertTrue(yValue instanceof LiteralNumberNode);
        assertEquals(9, ((LiteralNumberNode) yValue).getIntValue());
    }

    @Test
    public void testParenthesizedExpression_3() {
        String source = "int z = ((((((9))))));";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("z"));
        AstNode zValue = memoryModel.accessVariable("z");
        assertTrue(zValue instanceof LiteralNumberNode);
        assertEquals(9, ((LiteralNumberNode) zValue).getIntValue());
    }
}
