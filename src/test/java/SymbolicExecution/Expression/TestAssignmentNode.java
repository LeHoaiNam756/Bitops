package SymbolicExecution.Expression;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.AssignmentNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralCharacterNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.InfixExpressionNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.PrimitiveVariable;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.*;

public class TestAssignmentNode {

    @Test
    public void testVariableDeclarationAndAssignment() {
        String source = "int x;\n" +
                       "x = 5;";
        
        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());
        
        assertNotNull(eclipseAstNodes.get(0));
        assertNotNull(eclipseAstNodes.get(1));
        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);
        
        MemoryModel memoryModel = new MemoryModel();
        
        AstNode varDeclNode = AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertNull(memoryModel.accessVariable("x"));
        
        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        
        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);
        
        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testVariableDeclarationAndAssignment_2() {
        String source = "int x = 10;\n" +
                "x = 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertNotNull(eclipseAstNodes.get(0));
        assertNotNull(eclipseAstNodes.get(1));
        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode varDeclNode = AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        AstNode initialValue = memoryModel.accessVariable("x");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isInteger());
        assertEquals(10, ((LiteralNumberNode) initialValue).getIntValue());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) assignedValue).getIntValue());

        assertEquals(1, memoryModel.size());
    }

    @Test
    public void testAssignment_byteType() {
        String source = "byte b = 1;\n" +
                       "b = 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        AstNode initialValue = memoryModel.accessVariable("b");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isInteger());
        assertEquals(1, ((LiteralNumberNode) initialValue).getIntValue());
        assertTrue(memoryModel.getVariable("b") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BYTE, ((PrimitiveVariable) memoryModel.getVariable("b")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("b");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(2, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_shortType() {
        String source = "short s = 10;\n" +
                       "s = 20;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("s"));
        AstNode initialValue = memoryModel.accessVariable("s");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isInteger());
        assertEquals(10, ((LiteralNumberNode) initialValue).getIntValue());
        assertTrue(memoryModel.getVariable("s") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.SHORT, ((PrimitiveVariable) memoryModel.getVariable("s")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("s");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(20, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_longType() {
        String source = "long l = 100L;\n" +
                       "l = 200L;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("l"));
        AstNode initialValue = memoryModel.accessVariable("l");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isInteger());
        assertEquals(100, ((LiteralNumberNode) initialValue).getIntValue());
        assertTrue(memoryModel.getVariable("l") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.LONG, ((PrimitiveVariable) memoryModel.getVariable("l")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("l");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(200, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_floatType() {
        String source = "float f = 3.14f;\n" +
                       "f = 2.5f;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("f"));
        AstNode initialValue = memoryModel.accessVariable("f");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) initialValue).getDoubleValue(), 0.001);
        assertTrue(memoryModel.getVariable("f") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.FLOAT, ((PrimitiveVariable) memoryModel.getVariable("f")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("f");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(2.5, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    @Test
    public void testAssignment_doubleType() {
        String source = "double d = 3.14;\n" +
                       "d = 2.5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("d"));
        AstNode initialValue = memoryModel.accessVariable("d");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) initialValue).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) initialValue).getDoubleValue(), 0.001);
        assertTrue(memoryModel.getVariable("d") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.DOUBLE, ((PrimitiveVariable) memoryModel.getVariable("d")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("d");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(2.5, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    @Test
    public void testAssignment_charType() {
        String source = "char c = 'a';\n" +
                       "c = 'b';";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("c"));
        AstNode initialValue = memoryModel.accessVariable("c");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralCharacterNode);
        assertEquals('a', ((LiteralCharacterNode) initialValue).getValue());
        assertTrue(memoryModel.getVariable("c") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.CHAR, ((PrimitiveVariable) memoryModel.getVariable("c")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("c");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralCharacterNode);
        assertEquals('b', ((LiteralCharacterNode) assignedValue).getValue());
    }

    @Test
    public void testAssignment_booleanType() {
        String source = "boolean flag = true;\n" +
                       "flag = false;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        AstNode initialValue = memoryModel.accessVariable("flag");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) initialValue).isValue());
        assertTrue(memoryModel.getVariable("flag") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BOOLEAN, ((PrimitiveVariable) memoryModel.getVariable("flag")).getCode());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("flag");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) assignedValue).isValue());
    }

    @Test
    public void testAssignment_booleanTypeTrue() {
        String source = "boolean flag = false;\n" +
                       "flag = true;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        assertEquals(2, eclipseAstNodes.size());

        assertTrue(eclipseAstNodes.get(0) instanceof VariableDeclarationStatement);
        assertTrue(eclipseAstNodes.get(1) instanceof ExpressionStatement);

        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        AstNode initialValue = memoryModel.accessVariable("flag");
        assertNotNull(initialValue);
        assertTrue(initialValue instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) initialValue).isValue());

        ExpressionStatement exprStmt = (ExpressionStatement) eclipseAstNodes.get(1);
        assertTrue(exprStmt.getExpression() instanceof Assignment);
        AstNode assignmentNode = AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        assertNotNull(assignmentNode);
        assertTrue(assignmentNode instanceof AssignmentNode);

        AstNode assignedValue = memoryModel.accessVariable("flag");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) assignedValue).isValue());
    }

    // Tests for compound assignment operators

    @Test
    public void testAssignment_plusAssign() {
        String source = "int x = 10;\n" +
                       "x += 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(15, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_minusAssign() {
        String source = "int x = 20;\n" +
                       "x -= 8;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(12, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_timesAssign() {
        String source = "int x = 6;\n" +
                       "x *= 7;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(42, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_divideAssign() {
        String source = "int x = 20;\n" +
                       "x /= 4;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_remainderAssign() {
        String source = "int x = 17;\n" +
                       "x %= 5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(2, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_bitOrAssign() {
        String source = "int x = 5;\n" +  // 5 = 101 in binary
                       "x |= 3;";          // 3 = 011 in binary, result = 111 = 7

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(7, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_bitAndAssign() {
        String source = "int x = 7;\n" +  // 7 = 111 in binary
                       "x &= 5;";         // 5 = 101 in binary, result = 101 = 5

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_bitXorAssign() {
        String source = "int x = 5;\n" +  // 5 = 101 in binary
                       "x ^= 3;";         // 3 = 011 in binary, result = 110 = 6

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(6, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_leftShiftAssign() {
        String source = "int x = 4;\n" +  // 4 << 2 = 16
                       "x <<= 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(16, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_rightShiftSignedAssign() {
        String source = "int x = 16;\n" +  // 16 >> 2 = 4
                       "x >>= 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(4, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_rightShiftUnsignedAssign() {
        String source = "int x = 16;\n" +  // 16 >>> 2 = 4
                       "x >>>= 2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isInteger());
        assertEquals(4, ((LiteralNumberNode) assignedValue).getIntValue());
    }

    @Test
    public void testAssignment_plusAssign_double() {
        String source = "double d = 3.5;\n" +
                       "d += 2.5;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("d");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(6.0, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    @Test
    public void testAssignment_minusAssign_double() {
        String source = "double d = 10.5;\n" +
                       "d -= 3.2;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("d");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(7.3, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    @Test
    public void testAssignment_timesAssign_double() {
        String source = "double d = 2.5;\n" +
                       "d *= 4.0;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("d");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(10.0, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    @Test
    public void testAssignment_divideAssign_double() {
        String source = "double d = 15.0;\n" +
                       "d /= 3.0;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("d");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) assignedValue).isDouble());
        assertEquals(5.0, ((LiteralNumberNode) assignedValue).getDoubleValue(), 0.001);
    }

    // Test non-literal cases (where result should be InfixExpressionNode)
    @Test
    public void testAssignment_plusAssign_nonLiteral() {
        String source = "int n;\n" +
                       "int x = 10;\n" +
                       "x += n;";

        List<ASTNode> eclipseAstNodes = ParserForTest.parseSourceToAstNodeList(source);
        MemoryModel memoryModel = new MemoryModel();

        AstNode.executeASTNode(eclipseAstNodes.get(0), memoryModel);
        SimpleNameNode nNode = SimpleNameNode.of("n");
        memoryModel.assignVariable("n", nNode);
        AstNode.executeASTNode(eclipseAstNodes.get(1), memoryModel);
        

        
        AstNode.executeASTNode(eclipseAstNodes.get(2), memoryModel);

        AstNode assignedValue = memoryModel.accessVariable("x");
        assertNotNull(assignedValue);
        assertTrue(assignedValue instanceof InfixExpressionNode);
        InfixExpressionNode infixNode = (InfixExpressionNode) assignedValue;
        assertEquals("10+n", infixNode.toString());
    }

}
