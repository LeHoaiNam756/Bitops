package CFG.Utils;

import core.CFG.Utils.TernarySplitHelper;
import core.utils.AstIdGenerator;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class TernarySplitHelperTest {

    @Test
    public void split_returnTernary_buildsIdMap() {
        ASTNode stmt = parseFirstStatement("return cond ? a : b;");
        Optional<TernarySplitHelper.TernarySplitResult> resultOpt = TernarySplitHelper.split(stmt);
        assertTrue(resultOpt.isPresent());

        TernarySplitHelper.TernarySplitResult result = resultOpt.get();
        ConditionalExpression original = findConditionalExpression(stmt);

        String condKey = AstIdGenerator.buildSignature(original.getExpression());
        String thenKey = AstIdGenerator.buildSignature(original.getThenExpression());
        String elseKey = AstIdGenerator.buildSignature(original.getElseExpression());

        IfStatement ifStmt = (IfStatement) result.getNormalizedStatement();
        Statement thenStmt = firstStatement(ifStmt.getThenStatement());
        Statement elseStmt = firstStatement(ifStmt.getElseStatement());

        Map<String, String> map = result.getOriginalPartIdToConvertedPartId();
        assertEquals(AstIdGenerator.buildSignature(ifStmt.getExpression()), map.get(condKey));
        assertEquals(AstIdGenerator.buildSignature(thenStmt), map.get(thenKey));
        assertEquals(AstIdGenerator.buildSignature(elseStmt), map.get(elseKey));
    }

    @Test
    public void split_variableDeclarationTernary_buildsIdMapForAssignments() {
        ASTNode stmt = parseFirstStatement("int x = cond ? 1 : 2;");
        Optional<TernarySplitHelper.TernarySplitResult> resultOpt = TernarySplitHelper.split(stmt);
        assertTrue(resultOpt.isPresent());

        TernarySplitHelper.TernarySplitResult result = resultOpt.get();
        assertTrue(result.getNormalizedStatement() instanceof Block);

        ConditionalExpression original = findConditionalExpression(stmt);
        String thenKey = AstIdGenerator.buildSignature(original.getThenExpression());
        String elseKey = AstIdGenerator.buildSignature(original.getElseExpression());

        IfStatement ifStmt = findIfInBlock((Block) result.getNormalizedStatement());
        Statement thenStmt = firstStatement(ifStmt.getThenStatement());
        Statement elseStmt = firstStatement(ifStmt.getElseStatement());

        Map<String, String> map = result.getOriginalPartIdToConvertedPartId();
        assertEquals(AstIdGenerator.buildSignature(thenStmt), map.get(thenKey));
        assertEquals(AstIdGenerator.buildSignature(elseStmt), map.get(elseKey));
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

    private ConditionalExpression findConditionalExpression(ASTNode stmt) {
        if (stmt instanceof ReturnStatement) {
            return getConditionalExpression(((ReturnStatement) stmt).getExpression());
        }
        if (stmt instanceof ExpressionStatement) {
            Expression expr = ((ExpressionStatement) stmt).getExpression();
            if (expr instanceof Assignment) {
                return getConditionalExpression(((Assignment) expr).getRightHandSide());
            }
        }
        if (stmt instanceof VariableDeclarationStatement) {
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments = ((VariableDeclarationStatement) stmt).fragments();
            for (VariableDeclarationFragment fragment : fragments) {
                ConditionalExpression ce = getConditionalExpression(fragment.getInitializer());
                if (ce != null) {
                    return ce;
                }
            }
        }
        return null;
    }

    private ConditionalExpression getConditionalExpression(Expression expression) {
        Expression current = expression;
        while (current instanceof ParenthesizedExpression) {
            current = ((ParenthesizedExpression) current).getExpression();
        }
        return current instanceof ConditionalExpression ? (ConditionalExpression) current : null;
    }

    private Statement firstStatement(Statement stmt) {
        if (stmt instanceof Block) {
            @SuppressWarnings("unchecked")
            List<Statement> statements = ((Block) stmt).statements();
            return statements.isEmpty() ? null : statements.get(0);
        }
        return stmt;
    }

    private IfStatement findIfInBlock(Block block) {
        @SuppressWarnings("unchecked")
        List<Statement> statements = block.statements();
        for (Statement statement : statements) {
            if (statement instanceof IfStatement) {
                return (IfStatement) statement;
            }
        }
        return null;
    }
}
