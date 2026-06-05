package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;
import utils.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringLiteralHandlerTest {

    private StringLiteral parseStringLiteral(String literalSource) {
        VariableDeclarationStatement stmt =
                (VariableDeclarationStatement) Parser.parseStatementToAST("String s = " + literalSource + ";");
        VariableDeclarationFragment frag = (VariableDeclarationFragment) stmt.fragments().get(0);
        return (StringLiteral) frag.getInitializer();
    }

    @Test
    public void supportsReturnsTrueForStringLiteral() {
        StringLiteralHandler handler = new StringLiteralHandler();

        assertTrue(handler.supports(parseStringLiteral("\"hello\"")));
    }

    @Test
    public void evalStringLiteral() {
        StringLiteralHandler handler = new StringLiteralHandler();
        StringLiteral literal = parseStringLiteral("\"hello\\nworld\"");

        SymbolicValue result = handler.eval(literal, null, null);

        assertEquals(SymLiteral.of("hello\nworld"), result);
    }
}
