package SymbolicExecution.Expression.Operation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PostfixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPostfixExpression {
    @Test
    public void testPostfixExpression_1() {
        String src = "int i = 0; i++;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        for (ASTNode astNode : eclipseAstNodes) {
            AstNode.executeASTNode(astNode, memoryModel);
        }
        assertTrue(memoryModel.containsVariable("i"));
        AstNode iNode = memoryModel.accessVariable("i");
        assertTrue(iNode instanceof LiteralNumberNode);
        LiteralNumberNode iLiteral = (LiteralNumberNode) iNode;
        assertEquals(1L, iLiteral.getIntegerValue());
    }

    @Test
    public void testPostfixExpression_2() {
        String src = "int j = 5; j--;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        for (ASTNode astNode : eclipseAstNodes) {
            AstNode.executeASTNode(astNode, memoryModel);
        }
        assertTrue(memoryModel.containsVariable("j"));
        AstNode jNode = memoryModel.accessVariable("j");
        assertTrue(jNode instanceof LiteralNumberNode);
        LiteralNumberNode jLiteral = (LiteralNumberNode) jNode;
        assertEquals(4L, jLiteral.getIntegerValue());
    }

    @Test
    public void testPostfixExpression_3() {
        String src = "int k = 10; int m = k++;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        for (ASTNode astNode : eclipseAstNodes) {
            AstNode.executeASTNode(astNode, memoryModel);
        }
        assertTrue(memoryModel.containsVariable("k"));
        AstNode kNode = memoryModel.accessVariable("k");
        assertTrue(kNode instanceof LiteralNumberNode);
        LiteralNumberNode kLiteral = (LiteralNumberNode) kNode;
        assertEquals(11L, kLiteral.getIntegerValue());

        assertTrue(memoryModel.containsVariable("m"));
        AstNode mNode = memoryModel.accessVariable("m");
        assertTrue(mNode instanceof LiteralNumberNode);
        LiteralNumberNode mLiteral = (LiteralNumberNode) mNode;
        assertEquals(10L, mLiteral.getIntegerValue());
    }

    @Test
    public void testPostfixExpression_4() {
        String src = "int n;int i = n + 1; int p = i++;";
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(src);
        MemoryModel memoryModel = new MemoryModel();
        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        AstNode iNode = memoryModel.accessVariable("i");
        assertTrue(iNode instanceof InfixExpressionNode);
        assertEquals("n+1", iNode.toString());
        AstNode.executeASTNode(eclipseAstNodes.get(2), memoryModel);
        AstNode pNode = memoryModel.accessVariable("p");
        assertTrue(pNode instanceof InfixExpressionNode);
        assertEquals("n+1", pNode.toString());
        iNode = memoryModel.accessVariable("i");
        assertTrue(iNode instanceof PostfixExpressionNode);
        assertEquals("(n+1)++", iNode.toString());
    }
}
