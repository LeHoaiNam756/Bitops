package core.utils;

import core.CFG.Utils.ASTHelper;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class CloneProjectTernaryConversionTest {

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

    private String invokeGenerateCodeForOneStatement(ASTNode statement, ASTHelper.Coverage coverage) throws Exception {
        Method method = CloneProject.class.getDeclaredMethod(
                "generateCodeForOneStatement",
                ASTNode.class,
                String.class,
                ASTHelper.Coverage.class
        );
        method.setAccessible(true);
        return (String) method.invoke(null, statement, ";", coverage);
    }

    @Test
    public void generateCodeForOneStatement_returnTernary_convertsToIfElse() throws Exception {
        ASTNode stmt = parseFirstStatement("return cond ? a : b;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "return a;");
        assertContainsNormalized(generated, "else");
        assertContainsNormalized(generated, "return b;");
        assertContainsNormalized(generated, "markOneStatement(\"cond\"");
    }

    @Test
    public void generateCodeForOneStatement_assignmentTernary_convertsToIfElseAssignments() throws Exception {
        ASTNode stmt = parseFirstStatement("a = cond ? 10 : 20;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "a = 10;");
        assertContainsNormalized(generated, "else");
        assertContainsNormalized(generated, "a = 20;");
        assertContainsNormalized(generated, "markOneStatement(\"cond\"");
    }

    @Test
    public void generateCodeForOneStatement_assignmentTernaryWithParentheses_convertsToIfElseAssignments() throws Exception {
        ASTNode stmt = parseFirstStatement("a = (cond ? 10 : 20);");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "a = 10;");
        assertContainsNormalized(generated, "a = 20;");
    }

    @Test
    public void generateCodeForOneStatement_assignmentTernaryWithComplexLeftHandSide_keepsOriginalStatement() throws Exception {
        ASTNode stmt = parseFirstStatement("arr[a++] = cond ? 10 : 20;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "arr[a++] = cond ? 10 : 20;");
    }

    @Test
    public void generateCodeForOneStatement_variableDeclarationMixedFragments_convertsOnlyTernaryFragments() throws Exception {
        ASTNode stmt = parseFirstStatement("int x = cond ? 1 : 2, y = 3;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "int x;");
        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "x = 1;");
        assertContainsNormalized(generated, "else");
        assertContainsNormalized(generated, "x = 2;");
        assertContainsNormalized(generated, "int y = 3;");
    }

    @Test
    public void generateCodeForOneStatement_variableDeclarationSingleTernary_convertsToDeclPlusIfElse() throws Exception {
        ASTNode stmt = parseFirstStatement("int z = cond ? 7 : 8;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "int z;");
        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "z = 7;");
        assertContainsNormalized(generated, "z = 8;");
    }

    @Test
    public void generateCodeForOneStatement_returnTernaryWithParentheses_convertsToIfElse() throws Exception {
        ASTNode stmt = parseFirstStatement("return (cond ? a : b);");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "if (");
        assertContainsNormalized(generated, "return a;");
        assertContainsNormalized(generated, "return b;");
    }

    @Test
    public void generateCodeForOneStatement_variableDeclarationTernaryWithParentheses_convertsToDeclPlusIfElse() throws Exception {
        ASTNode stmt = parseFirstStatement("int q = (cond ? 1 : 2);");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "int q;");
        assertContainsNormalized(generated, "q = 1;");
        assertContainsNormalized(generated, "q = 2;");
    }

    @Test
    public void generateCodeForOneStatement_finalVariableDeclarationTernary_keepsOriginalStatement() throws Exception {
        ASTNode stmt = parseFirstStatement("final int f = cond ? 1 : 2;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "final int f = cond ? 1 : 2;");
    }

    @Test
    public void generateCodeForOneStatement_varVariableDeclarationTernary_keepsOriginalStatement() throws Exception {
        ASTNode stmt = parseFirstStatement("var v = cond ? 1 : 2;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "var v = cond ? 1 : 2;");
    }

    @Test
    public void generateCodeForOneStatement_returnNestedTernary_convertsInnerTernary() throws Exception {
        ASTNode stmt = parseFirstStatement("return a ? (b ? c : d) : e;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        assertContainsNormalized(generated, "if (a)");
        assertContainsNormalized(generated, "if (b)");
        assertContainsNormalized(generated, "return c;");
        assertContainsNormalized(generated, "return d;");
        assertContainsNormalized(generated, "return e;");
    }

    @Test
    public void generateCodeForOneStatement_assignmentNestedTernary_convertsInnerTernary() throws Exception {
        ASTNode stmt = parseFirstStatement("a = x ? (y ? 1 : 2) : 3;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        System.out.println("ASSIGN NESTED: " + normalize(generated));
        
        assertContainsNormalized(generated, "if (x)");
        assertContainsNormalized(generated, "if (y)");
        assertContainsNormalized(generated, "a = 1;");
        assertContainsNormalized(generated, "a = 2;");
        assertContainsNormalized(generated, "a = 3;");
    }

    @Test
    public void generateCodeForOneStatement_variableDeclarationNestedTernary_convertsInnerTernary() throws Exception {
        ASTNode stmt = parseFirstStatement("int v = a ? b ? 1 : 2 : 3;");
        String generated = invokeGenerateCodeForOneStatement(stmt, ASTHelper.Coverage.BRANCH);

        System.out.println("VAR NESTED: " + normalize(generated));
        
        assertContainsNormalized(generated, "int v;");
        assertContainsNormalized(generated, "if (a)");
        assertContainsNormalized(generated, "if (b)");
        assertContainsNormalized(generated, "v = 1;");
        assertContainsNormalized(generated, "v = 2;");
        assertContainsNormalized(generated, "v = 3;");
    }
}
