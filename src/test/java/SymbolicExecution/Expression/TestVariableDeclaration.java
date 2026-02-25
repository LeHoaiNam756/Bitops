package SymbolicExecution.Expression;

import org.eclipse.jdt.core.dom.*;
import org.junit.Ignore;
import org.junit.Test;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralCharacterNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.PrimitiveVariable;

import java.util.List;

import static org.junit.Assert.*;

public class TestVariableDeclaration {

    // ========== VariableDeclarationExpression Tests ==========

    @Test
    public void parseVariableDeclarationExpression_singleVariableWithoutInitializer() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(astNode);
    }

    @Test
    public void parseVariableDeclarationExpression_singleVariableWithInitializer() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x = 5");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        AstNode symbolicValue = memoryModel.accessVariable("x");
        assertTrue(symbolicValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) symbolicValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) symbolicValue).getIntValue());
        assertNull(astNode);
    }

    @Test
    public void parseVariableDeclarationExpression_multipleVariablesWithoutInitializers() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x, y, z");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(memoryModel.accessVariable("y"));
        assertNull(memoryModel.accessVariable("z"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_multipleVariablesWithInitializers() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x = 5, y = 10, z = 15");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertTrue(memoryModel.accessVariable("x") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("y") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("z") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("x")).isInteger());
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("y")).isInteger());
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("z")).isInteger());
        assertEquals(5, ((LiteralNumberNode) memoryModel.accessVariable("x")).getIntValue());
        assertEquals(10, ((LiteralNumberNode) memoryModel.accessVariable("y")).getIntValue());
        assertEquals(15, ((LiteralNumberNode) memoryModel.accessVariable("z")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_mixedWithAndWithoutInitializers() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x = 5, y, z = 10");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertTrue(memoryModel.accessVariable("x") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("z") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("x")).isInteger());
        assertNull(memoryModel.accessVariable("y"));
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("z")).isInteger());
        assertEquals(5, ((LiteralNumberNode) memoryModel.accessVariable("x")).getIntValue());
        assertEquals(10, ((LiteralNumberNode) memoryModel.accessVariable("z")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_byteType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("byte b = 1");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        assertTrue(memoryModel.accessVariable("b") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("b")).isInteger());
        assertEquals(1, ((LiteralNumberNode) memoryModel.accessVariable("b")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("b") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BYTE, ((PrimitiveVariable) memoryModel.getVariable("b")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_shortType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("short s = 2");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("s"));
        assertTrue(memoryModel.accessVariable("s") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("s")).isInteger());
        assertEquals(2, ((LiteralNumberNode) memoryModel.accessVariable("s")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("s") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.SHORT, ((PrimitiveVariable) memoryModel.getVariable("s")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_longType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("long l = 100L");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("l"));
        assertTrue(memoryModel.accessVariable("l") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("l")).isInteger());
        assertEquals(100, ((LiteralNumberNode) memoryModel.accessVariable("l")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("l") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.LONG, ((PrimitiveVariable) memoryModel.getVariable("l")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_floatType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("float f = 3.14f");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("f"));
        assertTrue(memoryModel.accessVariable("f") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("f")).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) memoryModel.accessVariable("f")).getDoubleValue(),
                0.001);
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("f") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.FLOAT, ((PrimitiveVariable) memoryModel.getVariable("f")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_doubleType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("double d = 3.14");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("d"));
        assertTrue(memoryModel.accessVariable("d") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("d")).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) memoryModel.accessVariable("d")).getDoubleValue(), 0.001);
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("d") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.DOUBLE, ((PrimitiveVariable) memoryModel.getVariable("d")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_charType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("char c = 'a'");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("c"));
        assertTrue(memoryModel.accessVariable("c") instanceof LiteralCharacterNode);
        assertEquals('a', ((LiteralCharacterNode) memoryModel.accessVariable("c")).getValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("c") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.CHAR, ((PrimitiveVariable) memoryModel.getVariable("c")).getCode());
    }

    @Test
    public void parseVariableDeclarationExpression_booleanType() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("boolean flag = true");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(expr, memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        assertTrue(memoryModel.accessVariable("flag") instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) memoryModel.accessVariable("flag")).isValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("flag") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BOOLEAN, ((PrimitiveVariable) memoryModel.getVariable("flag")).getCode());
    }

    @Ignore
    @Test
    public void parseVariableDeclarationExpression_withFinalModifier() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("final int x = 5");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationExpression_withMultipleModifiers() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("final static int x = 5");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationExpression_withExpressionInitializer() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x = 5 + 3");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationExpression_withMethodCallInitializer() {
        VariableDeclarationExpression expr = VariableDeclarationTestHelper
                .parseVariableDeclarationExpression("int x = getValue()");

    }

    // ========== VariableDeclarationStatement Tests ==========

    @Test
    public void parseVariableDeclarationStatement_singleVariableWithoutInitializer() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(astNode);
    }

    @Test
    public void parseVariableDeclarationStatement_singleVariableWithInitializer() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x = 5;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        AstNode symbolicValue = memoryModel.accessVariable("x");
        assertTrue(symbolicValue instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) symbolicValue).isInteger());
        assertEquals(5, ((LiteralNumberNode) symbolicValue).getIntValue());
        assertNull(astNode);
    }

    @Test
    public void parseVariableDeclarationStatement_multipleVariablesWithoutInitializers() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x, y, z;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(memoryModel.accessVariable("y"));
        assertNull(memoryModel.accessVariable("z"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_multipleVariablesWithInitializers() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x = 5, y = 10, z = 15;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertTrue(memoryModel.accessVariable("x") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("y") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("z") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("x")).isInteger());
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("y")).isInteger());
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("z")).isInteger());
        assertEquals(5, ((LiteralNumberNode) memoryModel.accessVariable("x")).getIntValue());
        assertEquals(10, ((LiteralNumberNode) memoryModel.accessVariable("y")).getIntValue());
        assertEquals(15, ((LiteralNumberNode) memoryModel.accessVariable("z")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_mixedWithAndWithoutInitializers() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x = 5, y, z = 10;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertTrue(memoryModel.containsVariable("y"));
        assertTrue(memoryModel.containsVariable("z"));
        assertTrue(memoryModel.accessVariable("x") instanceof LiteralNumberNode);
        assertTrue(memoryModel.accessVariable("z") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("x")).isInteger());
        assertNull(memoryModel.accessVariable("y"));
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("z")).isInteger());
        assertEquals(5, ((LiteralNumberNode) memoryModel.accessVariable("x")).getIntValue());
        assertEquals(10, ((LiteralNumberNode) memoryModel.accessVariable("z")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("y") instanceof PrimitiveVariable);
        assertTrue(memoryModel.getVariable("z") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("y")).getCode());
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("z")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_byteType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("byte b = 1;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        assertTrue(memoryModel.accessVariable("b") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("b")).isInteger());
        assertEquals(1, ((LiteralNumberNode) memoryModel.accessVariable("b")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("b") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BYTE, ((PrimitiveVariable) memoryModel.getVariable("b")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_shortType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("short s = 2;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("s"));
        assertTrue(memoryModel.accessVariable("s") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("s")).isInteger());
        assertEquals(2, ((LiteralNumberNode) memoryModel.accessVariable("s")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("s") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.SHORT, ((PrimitiveVariable) memoryModel.getVariable("s")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_longType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("long l = 100L;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("l"));
        assertTrue(memoryModel.accessVariable("l") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("l")).isInteger());
        assertEquals(100, ((LiteralNumberNode) memoryModel.accessVariable("l")).getIntValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("l") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.LONG, ((PrimitiveVariable) memoryModel.getVariable("l")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_floatType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("float f = 3.14f;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("f"));
        assertTrue(memoryModel.accessVariable("f") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("f")).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) memoryModel.accessVariable("f")).getDoubleValue(),
                0.001);
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("f") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.FLOAT, ((PrimitiveVariable) memoryModel.getVariable("f")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_doubleType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("double d = 3.14;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("d"));
        assertTrue(memoryModel.accessVariable("d") instanceof LiteralNumberNode);
        assertTrue(((LiteralNumberNode) memoryModel.accessVariable("d")).isDouble());
        assertEquals(3.14, ((LiteralNumberNode) memoryModel.accessVariable("d")).getDoubleValue(),
                0.001);
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("d") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.DOUBLE, ((PrimitiveVariable) memoryModel.getVariable("d")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_charType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("char c = 'a';");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("c"));
        assertTrue(memoryModel.accessVariable("c") instanceof LiteralCharacterNode);
        assertEquals('a', ((LiteralCharacterNode) memoryModel.accessVariable("c")).getValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("c") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.CHAR, ((PrimitiveVariable) memoryModel.getVariable("c")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_booleanType() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("boolean flag = true;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        assertTrue(memoryModel.accessVariable("flag") instanceof LiteralBooleanNode);
        assertTrue(((LiteralBooleanNode) memoryModel.accessVariable("flag")).isValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("flag") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BOOLEAN, ((PrimitiveVariable) memoryModel.getVariable("flag")).getCode());
    }

    @Test
    public void parseVariableDeclarationStatement_booleanTypeFalse() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("boolean flag = false;");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(stmt, memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        assertTrue(memoryModel.accessVariable("flag") instanceof LiteralBooleanNode);
        assertFalse(((LiteralBooleanNode) memoryModel.accessVariable("flag")).isValue());
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("flag") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BOOLEAN, ((PrimitiveVariable) memoryModel.getVariable("flag")).getCode());
    }

    @Ignore
    @Test
    public void parseVariableDeclarationStatement_withFinalModifier() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("final int x = 5;");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationStatement_withMultipleModifiers() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("final static int x = 5;");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationStatement_withExpressionInitializer() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x = 5 + 3;");

    }

    @Ignore
    @Test
    public void parseVariableDeclarationStatement_withMethodCallInitializer() {
        VariableDeclarationStatement stmt = VariableDeclarationTestHelper
                .parseVariableDeclarationStatement("int x = getValue();");

    }

    // ========== SingleVariableDeclaration Tests ==========

    @Test
    public void parseSingleVariableDeclaration_intType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("int x");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_byteType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("byte b");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("b"));
        assertNull(memoryModel.accessVariable("b"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("b") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BYTE, ((PrimitiveVariable) memoryModel.getVariable("b")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_shortType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("short s");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("s"));
        assertNull(memoryModel.accessVariable("s"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("s") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.SHORT, ((PrimitiveVariable) memoryModel.getVariable("s")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_longType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("long l");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("l"));
        assertNull(memoryModel.accessVariable("l"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("l") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.LONG, ((PrimitiveVariable) memoryModel.getVariable("l")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_floatType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("float f");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("f"));
        assertNull(memoryModel.accessVariable("f"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("f") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.FLOAT, ((PrimitiveVariable) memoryModel.getVariable("f")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_doubleType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("double d");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("d"));
        assertNull(memoryModel.accessVariable("d"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("d") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.DOUBLE, ((PrimitiveVariable) memoryModel.getVariable("d")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_charType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("char c");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("c"));
        assertNull(memoryModel.accessVariable("c"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("c") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.CHAR, ((PrimitiveVariable) memoryModel.getVariable("c")).getCode());
    }

    @Test
    public void parseSingleVariableDeclaration_booleanType() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("boolean flag");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("flag"));
        assertNull(memoryModel.accessVariable("flag"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("flag") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.BOOLEAN, ((PrimitiveVariable) memoryModel.getVariable("flag")).getCode());
    }

    @Ignore
    @Test
    public void parseSingleVariableDeclaration_withFinalModifier() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("final int x");

        MemoryModel memoryModel = new MemoryModel();
        AstNode astNode = AstNode.executeASTNode(decl, memoryModel);
        assertTrue(memoryModel.containsVariable("x"));
        assertNull(memoryModel.accessVariable("x"));
        assertNull(astNode);
        assertTrue(memoryModel.getVariable("x") instanceof PrimitiveVariable);
        assertSame(PrimitiveType.INT, ((PrimitiveVariable) memoryModel.getVariable("x")).getCode());
        
        // Verify modifier
        @SuppressWarnings("unchecked")
        List<IExtendedModifier> modifiers = decl.modifiers();
        assertEquals(1, modifiers.size());
        assertTrue(modifiers.get(0) instanceof Modifier);
        assertEquals(Modifier.ModifierKeyword.FINAL_KEYWORD, ((Modifier) modifiers.get(0)).getKeyword());
    }

    @Ignore
    @Test
    public void parseSingleVariableDeclaration_fromCatchClause() {
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclarationFromCatch("Exception e");

        // Verify the declaration was parsed correctly
        assertNotNull(decl);
        assertEquals("e", decl.getName().getIdentifier());
        // Note: Exception is not a primitive type, so execution may throw an exception
        // This test primarily verifies the parsing works
    }

    @Ignore
    @Test
    public void parseSingleVariableDeclaration_withInitializer() {
        // SingleVariableDeclaration can have initializers in enhanced for loops
        // This would need special handling
        SingleVariableDeclaration decl = VariableDeclarationTestHelper
                .parseSingleVariableDeclaration("int x");

    }
}
