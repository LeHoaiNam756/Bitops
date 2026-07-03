package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.MemoryModel;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import core.SymbolicExecution.model.types.SymType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.*;

public class NumberLiteralHandlerTest {

    private final NumberLiteralHandler handler = new NumberLiteralHandler();

    private NumberLiteral parseLiteral(String statement) {
        ASTNode node = Parser.parseStatementToAST(statement);
        VariableDeclarationStatement varStmt = (VariableDeclarationStatement) node;
        VariableDeclarationFragment frag = (VariableDeclarationFragment) varStmt.fragments().get(0);
        return (NumberLiteral) frag.getInitializer();
    }

    private NumberLiteral createUnboundLiteral(String token) {
        AST ast = AST.newAST(AST.JLS8);
        NumberLiteral literal = ast.newNumberLiteral();
        literal.setToken(token);
        return literal;
    }

    private SymbolicState createState(SymType type) {
        TypeContext tc = new TypeContext();
        if (type != null) {
            tc.pushAssignment(type);
        }
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(tc)
                .build();
    }

    private SymbolicState createCastState(SymType type) {
        TypeContext tc = new TypeContext();
        tc.pushCast(type);
        return SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(tc)
                .build();
    }

    @Test
    public void supportsReturnsTrueForNumberLiteral() {
        NumberLiteral literal = parseLiteral("int x = 42;");
        assertTrue(handler.supports(literal));
    }

    @Test
    public void supportsReturnsFalseForOtherNodes() {
        ASTNode node = Parser.parseStatementToAST("int x = 42;");
        assertFalse(handler.supports(node));
    }

    @Test
    public void evalIntegerLiteral() {
        NumberLiteral literal = parseLiteral("int x = 42;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(42, ((SymLiteral) result).value());
    }

    @Test
    public void evalLongLiteralWithUppercaseSuffix() {
        NumberLiteral literal = parseLiteral("long x = 42L;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(42L, ((SymLiteral) result).value());
    }

    @Test
    public void evalLongLiteralWithLowercaseSuffix() {
        NumberLiteral literal = parseLiteral("long x = 42l;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(42L, ((SymLiteral) result).value());
    }

    @Test
    public void evalFloatLiteralWithUppercaseSuffix() {
        NumberLiteral literal = parseLiteral("float x = 3.14F;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14f, ((SymLiteral) result).value());
    }

    @Test
    public void evalFloatLiteralWithLowercaseSuffix() {
        NumberLiteral literal = parseLiteral("float x = 3.14f;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14f, ((SymLiteral) result).value());
    }

    @Test
    public void evalDoubleLiteralWithUppercaseSuffix() {
        NumberLiteral literal = parseLiteral("double x = 3.14D;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14d, ((SymLiteral) result).value());
    }

    @Test
    public void evalDoubleLiteralWithLowercaseSuffix() {
        NumberLiteral literal = parseLiteral("double x = 3.14d;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14d, ((SymLiteral) result).value());
    }

    @Test
    public void evalDoubleLiteralWithoutSuffix() {
        NumberLiteral literal = parseLiteral("double x = 3.14;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.14d, ((SymLiteral) result).value());
    }

    @Test
    public void evalHexLiteral() {
        NumberLiteral literal = parseLiteral("int x = 0xFF;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(255, ((SymLiteral) result).value());
    }

    @Test
    public void evalHexIntWithSignBitSet() {
        NumberLiteral literal = parseLiteral("int x = 0x80000000;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertEquals(Integer.MIN_VALUE, ((SymLiteral) result).value());
    }

    @Test
    public void evalHexIntWithAllBitsSet() {
        NumberLiteral literal = parseLiteral("int x = 0xffffffff;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertEquals(-1, ((SymLiteral) result).value());
    }

    @Test
    public void evalBinaryIntWithSignBitSet() {
        NumberLiteral literal = parseLiteral(
                "int x = 0b10000000000000000000000000000000;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertEquals(Integer.MIN_VALUE, ((SymLiteral) result).value());
    }

    @Test
    public void evalUnboundHexIntWithSignBitSet() {
        SymbolicValue result = handler.eval(
                createUnboundLiteral("0x80000000"),
                createState(null),
                new AstDispatcher());
        assertEquals(Integer.MIN_VALUE, ((SymLiteral) result).value());
    }

    @Test(expected = NumberFormatException.class)
    public void evalIntContextRejectsMoreThan32Bits() {
        handler.eval(
                createUnboundLiteral("0x100000000"),
                createState(TypeContext.INT),
                new AstDispatcher());
    }

    @Test
    public void evalIntCastContextTruncatesMoreThan32Bits() {
        SymbolicValue result = handler.eval(
                createUnboundLiteral("0x100000001"),
                createCastState(TypeContext.INT),
                new AstDispatcher());
        assertEquals(1, ((SymLiteral) result).value());
    }

    @Test
    public void evalOctalLiteral() {
        NumberLiteral literal = parseLiteral("int x = 077;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(63, ((SymLiteral) result).value());
    }

    @Test
    public void evalBinaryLiteral() {
        NumberLiteral literal = parseLiteral("int x = 0b1010;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(10, ((SymLiteral) result).value());
    }

    @Test
    public void evalLiteralWithUnderscores() {
        NumberLiteral literal = parseLiteral("int x = 1_000_000;");
        SymbolicValue result = handler.eval(literal, createState(null), new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(1000000, ((SymLiteral) result).value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void evalEmptyTokenThrowsIllegalArgumentException() {
        NumberLiteral literal = createUnboundLiteral("");
        handler.eval(literal, createState(null), new AstDispatcher());
    }

    @Test
    public void evalUsesTypeContextWhenBindingIsNull() {
        NumberLiteral literal = createUnboundLiteral("42");
        SymbolicState state = createState(TypeContext.LONG);
        SymbolicValue result = handler.eval(literal, state, new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals(42L, ((SymLiteral) result).value());
    }

    @Test
    public void evalShortTypeContext() {
        NumberLiteral literal = createUnboundLiteral("42");
        SymbolicState state = createState(TypeContext.SHORT);
        SymbolicValue result = handler.eval(literal, state, new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals((short) 42, ((SymLiteral) result).value());
    }

    @Test
    public void evalByteTypeContext() {
        NumberLiteral literal = createUnboundLiteral("42");
        SymbolicState state = createState(TypeContext.BYTE);
        SymbolicValue result = handler.eval(literal, state, new AstDispatcher());
        assertTrue(result instanceof SymLiteral);
        assertEquals((byte) 42, ((SymLiteral) result).value());
    }

    @Test
    public void evalByteAssignmentContextAllowsRepresentableConstant() {
        NumberLiteral literal = createUnboundLiteral("127");
        SymbolicValue result = handler.eval(literal, createState(TypeContext.BYTE), new AstDispatcher());
        assertEquals((byte) 127, ((SymLiteral) result).value());
    }

    @Test(expected = NumberFormatException.class)
    public void evalByteAssignmentContextRejectsOutOfRangeConstant() {
        handler.eval(createUnboundLiteral("128"), createState(TypeContext.BYTE), new AstDispatcher());
    }

    @Test
    public void evalByteCastContextTruncatesOutOfRangeConstant() {
        SymbolicValue result = handler.eval(
                createUnboundLiteral("200"),
                createCastState(TypeContext.BYTE),
                new AstDispatcher());
        assertEquals((byte) -56, ((SymLiteral) result).value());
    }
}
