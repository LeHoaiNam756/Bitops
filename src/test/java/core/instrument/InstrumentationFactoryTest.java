package core.instrument;

import core.parser.ParseEntry;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstrumentationFactoryTest {

    @Test
    public void cloneFileNameForUsesPublicTopLevelTypeName() {
        CompilationUnit cu = parse("""
                public class Solution {
                    public int hammingWeight(int n) {
                        return n;
                    }
                }
                """);
        cu.setProperty(ParseEntry.SOURCE_FILE_NAME_PROPERTY, "0191-number-of-1-bits.java");

        assertEquals("Solution.java", InstrumentationFactory.cloneFileNameFor(cu));
    }

    @Test
    public void cloneFileNameForKeepsOriginalFileNameWhenTypeIsPackagePrivate() {
        CompilationUnit cu = parse("""
                class Solution {
                    public int hammingWeight(int n) {
                        return n;
                    }
                }
                """);
        cu.setProperty(ParseEntry.SOURCE_FILE_NAME_PROPERTY, "0191-number-of-1-bits.java");

        assertEquals("0191-number-of-1-bits.java", InstrumentationFactory.cloneFileNameFor(cu));
    }

    private static CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }
}
