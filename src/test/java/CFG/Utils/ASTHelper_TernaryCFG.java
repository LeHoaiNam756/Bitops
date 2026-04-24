package CFG.Utils;


import org.junit.Assert;
import org.junit.Test;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import test.CfgMermaidGenerator;
import test.ParserForTest;

import static org.junit.Assert.assertTrue;


public class ASTHelper_TernaryCFG {

    private String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    @Test
    public void test_ternaryInReturnStatement() {
        String sourceCode = "    public int testMethod(int x) {\n" +
                "        return x > 0 ? 1 : -1;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);

        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        assertTrue(cfg instanceof CfgBoolExprNode);
        CfgBoolExprNode boolNode = (CfgBoolExprNode) cfg;
        Assert.assertEquals("x > 0", boolNode.getContent());

        CfgNode thenNode = boolNode.getTrueNode();
        CfgNode elseNode = boolNode.getFalseNode();

        Assert.assertEquals("return1;", normalize(thenNode.getContent()));
        Assert.assertEquals("return-1;", normalize(elseNode.getContent()));

    }

    @Test
    public void test_ternaryInAssignment() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        int result;\n" +
                "        result = x > 0 ? 1 : -1;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);

        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode boolNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("x > 0", boolNode.getContent());

        CfgNode thenNode = boolNode.getTrueNode();
        CfgNode elseNode = boolNode.getFalseNode();

        Assert.assertEquals("result=1;", normalize(thenNode.getContent()));
        Assert.assertEquals("result=-1;",normalize(elseNode.getContent()));

    }

    @Test
    public void test_ternaryInVariableDeclaration() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        int result = x > 0 ? 1 : -1;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);

        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgBoolExprNode boolNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("x > 0", boolNode.getContent());

        CfgNode thenNode = boolNode.getTrueNode();
        CfgNode elseNode = boolNode.getFalseNode();

        Assert.assertEquals("result=1;", normalize(thenNode.getContent()));
        Assert.assertEquals("result=-1;",normalize(elseNode.getContent()));

    }

    @Test
    public void test_nestedTernaryInAssignment() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        int result;\n" +
                "        result = x > 0 ? (y > 0 ? 1 : 2) : -1;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);

        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode boolNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("x>0", normalize(boolNode.getContent()));

        CfgNode thenNode = boolNode.getTrueNode();
        CfgNode elseNode = boolNode.getFalseNode();

        Assert.assertTrue(thenNode instanceof CfgBoolExprNode);
        CfgBoolExprNode nestedBool = (CfgBoolExprNode) thenNode;
        Assert.assertEquals("y>0",normalize(nestedBool.getContent()));

        Assert.assertEquals("result=-1;",normalize(elseNode.getContent()));
        CfgNode innerThenNode = nestedBool.getTrueNode();
        CfgNode innerElseNode = nestedBool.getFalseNode();

        Assert.assertEquals("result=1;",normalize(innerThenNode.getContent()));
        Assert.assertEquals("result=2;",normalize(innerElseNode.getContent()));
    }
}