package CFG.Utils;

import core.CFG.Utils.ASTHelper;
import core.CFG.Utils.TernaryOperatorsConverter;
import core.utils.CloneProject;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TestTernaryOperatorsConverter  {

    @Test
    public void test_returnTernary_convertToIfThenElse() throws Exception {
        ASTNode node = parseFirstStatement("\nreturn cond ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "if(cond)");
        assertContainsNormalized(generated, "returna");
        assertContainsNormalized(generated, "returnb");
    }

    @Test
    public void test_assignmentTernary_convertToIfThenElse() throws Exception {
        ASTNode node = parseFirstStatement("\nx = cond ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "if(cond)");
        assertContainsNormalized(generated, "x=a");
        assertContainsNormalized(generated, "x=b");
    }

    @Test
    public void test_variableDeclarationTernary_convertToIfThenElse() throws Exception {
        ASTNode node = parseFirstStatement("\nint x = cond ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "if(cond)");
        assertContainsNormalized(generated, "x=a");
        assertContainsNormalized(generated, "x=b");
    }

    @Test
    public void test_parenthesizedTernary_convertToIfThenElse() throws Exception {
        ASTNode node = parseFirstStatement("\nreturn (cond) ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "if((cond))");
        assertContainsNormalized(generated, "returna");
        assertContainsNormalized(generated, "returnb");
    }

    @Test
    public void test_finalVariableDeclaration_noConversion() throws Exception {
        ASTNode node = parseFirstStatement("\nfinal int x = cond ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "intx=cond?a:b");
    }

    @Test
    public void test_varTypeDeclaration_noConversion() throws Exception {
        ASTNode node = parseFirstStatement("\nvar x = cond ? a : b;\n");
        ASTNode result = invokeConvertTernaryToIfThenElse(node);
        String generated = result.toString();
        assertContainsNormalized(generated, "varx=cond?a:b");
    }

    private String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    private void assertContainsNormalized(String generated, String expectedSnippet) {
        assertTrue(normalize(generated).contains(normalize(expectedSnippet)));
    }

    private ASTNode parseFirstStatement(String source) {
        String cuSource = "public class __Wrapper__ {\n"
                + "  public int test(int a, int b, boolean cond) {\n"
                + source + "\n"
                + "    return 0;\n"
                + "  }\n"
                + "}\n";

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cuSource.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        final ASTNode[] found = new ASTNode[1];

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                Block body = node.getBody();
                if (body == null) {
                    return false;
                }
                @SuppressWarnings("unchecked")
                List<Statement> statements = body.statements();
                if (!statements.isEmpty()) {
                    found[0] = statements.get(0);
                }
                return false;
            }
        });

        if (found[0] == null) {
            throw new IllegalStateException("No statement parsed from source: " + source);
        }

        return found[0];
    }

    private ASTNode invokeConvertTernaryToIfThenElse(ASTNode node) throws Exception {
       Method method =  TernaryOperatorsConverter.class.getDeclaredMethod(
               "convertTernaryToIfThenElse",
               ASTNode.class
       );
       method.setAccessible(true);
       return (ASTNode) method.invoke(null, node);
    }


}
