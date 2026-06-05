package core.SymbolicExecution.model.types;

import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SymTypeMapTest {

    @Test
    public void convert_stringSimpleType_returnsObjectString() {
        VariableDeclarationStatement stmt =
                (VariableDeclarationStatement) Parser.parseStatementToAST("String s = \"x\";");

        SymType type = SymTypeMap.convert(stmt.getType());
        assertTrue(type instanceof ObjectSymType);
        assertTrue(type instanceof ClassSymType);
        assertEquals("java.lang.String", ((ObjectSymType) type).className());
    }

    @Test
    public void convert_stringQualifiedType_returnsObjectString() {
        VariableDeclarationStatement stmt =
                (VariableDeclarationStatement) Parser.parseStatementToAST("java.lang.String s = \"x\";");

        SymType type = SymTypeMap.convert(stmt.getType());
        assertTrue(type instanceof ObjectSymType);
        assertTrue(type instanceof ClassSymType);
        assertEquals("java.lang.String", ((ObjectSymType) type).className());
    }
}
