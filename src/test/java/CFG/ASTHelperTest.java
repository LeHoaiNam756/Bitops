package CFG;

import org.junit.Assert;
import org.junit.Test;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgBreakStatementNode;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import test.ParserForTest;


public class ASTHelperTest {

    @Test
    public void test_01_generateCfgForIfBlockWithBranchCoverage() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                      "        if (x > 0) {\n" +
                      "            System.out.println(\"Positive\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Non-positive\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"Positive\");\n", thenNode.getContent());
        Assert.assertEquals("System.out.println(\"Non-positive\");\n", elseNode.getContent());
        CfgNode endThenNode = thenNode.getAfterNode();
        CfgNode endElseNode = elseNode.getAfterNode();
        Assert.assertTrue(endThenNode.isEndBlock());
        Assert.assertEquals(endThenNode, endElseNode);
    }

    @Test
    public void test_02_generateCfgForNestedIf() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                      "        if (x > 0) {\n" +
                      "            if (y > 0) {\n" +
                      "                System.out.println(\"Both positive\");\n" +
                      "            }\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertTrue(thenNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", thenNode.getContent());
        CfgNode nestedThenNode = ((CfgBoolExprNode) thenNode).getTrueNode();
        Assert.assertEquals("System.out.println(\"Both positive\");\n", nestedThenNode.getContent());
        Assert.assertTrue(elseNode.isEndBlock());
        CfgNode nestedElseNode = ((CfgBoolExprNode) thenNode).getFalseNode();
        Assert.assertTrue(nestedElseNode.isEndBlock());
        CfgNode endNestedThenNode = nestedThenNode.getAfterNode();
        Assert.assertEquals(elseNode, endNestedThenNode);
        Assert.assertEquals(elseNode, nestedElseNode);
    }

    @Test
    public void test_03_generateCfgForIfWithoutElse() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                      "        if (x > 0) {\n" +
                      "            System.out.println(\"Positive\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"Positive\");\n", thenNode.getContent());
        Assert.assertTrue(elseNode.isEndBlock());
        CfgNode endThenNode = thenNode.getAfterNode();
        Assert.assertTrue(endThenNode.isEndBlock());
        Assert.assertEquals(endThenNode, elseNode);
    }

    @Test
    public void test_04_generateCfgForIfElseIfElse() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                      "        if (x > 0) {\n" +
                      "            System.out.println(\"Positive\");\n" +
                      "        } else if (x < 0) {\n" +
                      "            System.out.println(\"Negative\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Zero\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"Positive\");\n", thenNode.getContent());
        Assert.assertTrue(elseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("x < 0", elseNode.getContent());
        CfgNode elseIfThenNode = ((CfgBoolExprNode) elseNode).getTrueNode();
        CfgNode elseIfElseNode = ((CfgBoolExprNode) elseNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"Negative\");\n", elseIfThenNode.getContent());
        Assert.assertEquals("System.out.println(\"Zero\");\n", elseIfElseNode.getContent());
        CfgNode endBlockNode = elseIfElseNode.getAfterNode();
        Assert.assertTrue(endBlockNode.isEndBlock());
    }

    @Test
    public void test_05_generateCfgForIfElseIf() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                      "        if (x > 0) {\n" +
                      "            System.out.println(\"Positive\");\n" +
                      "        } else if (x < 0) {\n" +
                      "            System.out.println(\"Negative\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"Positive\");\n", thenNode.getContent());
        Assert.assertTrue(elseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("x < 0", elseNode.getContent());
        CfgNode elseIfThenNode = ((CfgBoolExprNode) elseNode).getTrueNode();
        CfgNode elseIfElseNode = ((CfgBoolExprNode) elseNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"Negative\");\n", elseIfThenNode.getContent());
        Assert.assertTrue(elseIfElseNode.isEndBlock());
    }

    @Test
    public void test_06_generateCfgForIfWithNoContent() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                      "        if (x > 0) {\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertTrue(thenNode.isEndBlock());
        Assert.assertTrue(elseNode.isEndBlock());
        Assert.assertEquals(thenNode, elseNode);
    }

    @Test
    public void test_07_generateCfgForIfWithComplexCondition() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                      "        if (x > 0 && y < 0) {\n" +
                      "            System.out.println(\"x positive and y negative\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Other cases\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y < 0", trueNode.getContent());
        CfgNode trueTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode trueFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"x positive and y negative\");\n", trueTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Other cases\");\n", falseNode.getContent());
        Assert.assertEquals(trueFalseNode, falseNode);
        CfgNode endNode = falseNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_08_generateCfgForIfWithTwoConditionsWithParentheses_Mcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                      "        if ((x > 0) && (y < 0)) {\n" +
                      "            System.out.println(\"x positive and y negative\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Other cases\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("(x > 0)", cfg.getContent());

        CfgNode firstTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode firstFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(firstTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("(y < 0)", firstTrueNode.getContent());

        CfgNode secondTrueNode = ((CfgBoolExprNode) firstTrueNode).getTrueNode();
        CfgNode secondFalseNode = ((CfgBoolExprNode) firstTrueNode).getFalseNode();

        Assert.assertEquals("System.out.println(\"x positive and y negative\");\n",
                secondTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Other cases\");\n", firstFalseNode.getContent());
        Assert.assertEquals(firstFalseNode, secondFalseNode);

        CfgNode endNode = firstFalseNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_09_generateCfgForIfWithThreeConditionsWithParentheses_Mcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                      "        if ((x > 0) && (y < 0) && (z == 0)) {\n" +
                      "            System.out.println(\"all conditions true\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Other cases\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("(x > 0)", cfg.getContent());

        CfgNode secondCond = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode firstFalse = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(secondCond instanceof CfgBoolExprNode);
        Assert.assertEquals("(y < 0)", secondCond.getContent());

        CfgNode thirdCond = ((CfgBoolExprNode) secondCond).getTrueNode();
        CfgNode secondFalse = ((CfgBoolExprNode) secondCond).getFalseNode();

        Assert.assertTrue(thirdCond instanceof CfgBoolExprNode);
        Assert.assertEquals("(z == 0)", thirdCond.getContent());
        Assert.assertEquals(firstFalse, secondFalse);

        CfgNode thenNode = ((CfgBoolExprNode) thirdCond).getTrueNode();
        CfgNode thirdFalse = ((CfgBoolExprNode) thirdCond).getFalseNode();

        Assert.assertEquals("System.out.println(\"all conditions true\");\n", thenNode.getContent());
        Assert.assertEquals("System.out.println(\"Other cases\");\n", firstFalse.getContent());
        Assert.assertEquals(firstFalse, thirdFalse);

        CfgNode endNode = firstFalse.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_10_generateCfgForIfWithFourConditionsWithoutParentheses_Mcdc() {
        String sourceCode = "    public void testMethod(int a, int b, int c, int d) {\n" +
                      "        if (a > 0 && b < 0 && c == 0 && d != 0) {\n" +
                      "            System.out.println(\"all four conditions true\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Other cases\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("a > 0", cfg.getContent());

        CfgNode secondCond = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode firstFalse = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(secondCond instanceof CfgBoolExprNode);
        Assert.assertEquals("b < 0", secondCond.getContent());

        CfgNode thirdCond = ((CfgBoolExprNode) secondCond).getTrueNode();
        CfgNode secondFalse = ((CfgBoolExprNode) secondCond).getFalseNode();
        Assert.assertEquals(firstFalse, secondFalse);

        Assert.assertTrue(thirdCond instanceof CfgBoolExprNode);
        Assert.assertEquals("c == 0", thirdCond.getContent());

        CfgNode fourthCond = ((CfgBoolExprNode) thirdCond).getTrueNode();
        CfgNode thirdFalse = ((CfgBoolExprNode) thirdCond).getFalseNode();
        Assert.assertEquals(firstFalse, thirdFalse);

        Assert.assertTrue(fourthCond instanceof CfgBoolExprNode);
        Assert.assertEquals("d != 0", fourthCond.getContent());

        CfgNode thenNode = ((CfgBoolExprNode) fourthCond).getTrueNode();
        CfgNode fourthFalse = ((CfgBoolExprNode) fourthCond).getFalseNode();

        Assert.assertEquals("System.out.println(\"all four conditions true\");\n", thenNode.getContent());
        Assert.assertEquals("System.out.println(\"Other cases\");\n", firstFalse.getContent());
        Assert.assertEquals(firstFalse, fourthFalse);

        CfgNode endNode = firstFalse.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_11_generateCfgForIfWithComplexConditionAndOr_Mcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                      "        if ((x > 0 && y < 0) || z == 1) {\n" +
                      "            System.out.println(\"complex condition true\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"complex condition false\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode xFalse = ((CfgBoolExprNode) cfg).getFalseNode();
        CfgNode xTrue = ((CfgBoolExprNode) cfg).getTrueNode();

        Assert.assertTrue(xFalse instanceof CfgBoolExprNode);
        Assert.assertEquals("z == 1", xFalse.getContent());

        Assert.assertTrue(xTrue instanceof CfgBoolExprNode);
        Assert.assertEquals("y < 0", xTrue.getContent());
        CfgNode yTrue = ((CfgBoolExprNode) xTrue).getTrueNode();
        CfgNode yFalse = ((CfgBoolExprNode) xTrue).getFalseNode();

        Assert.assertEquals("System.out.println(\"complex condition true\");\n", yTrue.getContent());
        Assert.assertEquals(yFalse, xFalse);

        CfgNode zTrue = ((CfgBoolExprNode) xFalse).getTrueNode();
        CfgNode zFalse = ((CfgBoolExprNode) xFalse).getFalseNode();

        Assert.assertEquals("System.out.println(\"complex condition true\");\n", zTrue.getContent());
        Assert.assertEquals("System.out.println(\"complex condition false\");\n", zFalse.getContent());

        CfgNode endNodeFromZFalse = zFalse.getAfterNode();
        CfgNode endNodeFromZTrue = zTrue.getAfterNode();
        CfgNode endNodeFromYTrue = yTrue.getAfterNode();
        Assert.assertEquals(endNodeFromZFalse, endNodeFromZTrue);
        Assert.assertEquals(endNodeFromYTrue, endNodeFromZFalse);
        Assert.assertTrue(endNodeFromZFalse.isEndBlock());
    }

    @Test
    public void test_12_generateCfgForNestedIfWithMcdcCoverage() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                      "        if (x > 0) {\n" +
                      "            if (y > 0 && x < 10) {\n" +
                      "                System.out.println(\"Nested and complex\");\n" +
                      "            }\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode outerTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode outerFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(outerFalseNode.isEndBlock());

        Assert.assertTrue(outerTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", outerTrueNode.getContent());

        CfgNode innerSecondCond = ((CfgBoolExprNode) outerTrueNode).getTrueNode();
        CfgNode innerElseNode = ((CfgBoolExprNode) outerTrueNode).getFalseNode();

        Assert.assertTrue(innerSecondCond instanceof CfgBoolExprNode);
        Assert.assertEquals("x < 10", innerSecondCond.getContent());

        CfgNode innerThenNode = ((CfgBoolExprNode) innerSecondCond).getTrueNode();
        CfgNode innerSecondFalse = ((CfgBoolExprNode) innerSecondCond).getFalseNode();

        Assert.assertEquals(innerElseNode, innerSecondFalse);
        Assert.assertEquals("System.out.println(\"Nested and complex\");\n", innerThenNode.getContent());
    }

    @Test
    public void test_13_generateCfgForIfElseIfElseWithMcdcCoverage() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                      "        if (x > 0 && y > 0) {\n" +
                      "            System.out.println(\"Both positive\");\n" +
                      "        } else if (x > 0 && y <= 0) {\n" +
                      "            System.out.println(\"x positive, y non-positive\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"Other cases\");\n" +
                      "        }\n" +
                      "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode secondOuterCond = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseOrElseIfNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(secondOuterCond instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", secondOuterCond.getContent());

        CfgNode firstThenNode = ((CfgBoolExprNode) secondOuterCond).getTrueNode();
        CfgNode secondIfNode = ((CfgBoolExprNode) secondOuterCond).getFalseNode();

        Assert.assertEquals("System.out.println(\"Both positive\");\n", firstThenNode.getContent());
        Assert.assertTrue(secondIfNode instanceof CfgBoolExprNode);
        Assert.assertEquals(elseOrElseIfNode, secondIfNode);

        Assert.assertEquals("x > 0", ((CfgBoolExprNode) secondIfNode).getContent());

        CfgNode secondInnerCond = ((CfgBoolExprNode) secondIfNode).getTrueNode();
        CfgNode finalElseNode = ((CfgBoolExprNode) secondIfNode).getFalseNode();

        Assert.assertTrue(secondInnerCond instanceof CfgBoolExprNode);
        Assert.assertEquals("y <= 0", secondInnerCond.getContent());

        CfgNode secondThenNode = ((CfgBoolExprNode) secondInnerCond).getTrueNode();
        CfgNode secondInnerFalse = ((CfgBoolExprNode) secondInnerCond).getFalseNode();

        Assert.assertEquals("System.out.println(\"x positive, y non-positive\");\n", secondThenNode.getContent());
        Assert.assertEquals("System.out.println(\"Other cases\");\n", finalElseNode.getContent());
        Assert.assertEquals(finalElseNode, secondInnerFalse);

        CfgNode endNode = finalElseNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_14_generateCfgForIfWithNestedOrConditions_Mcdc() {
        String sourceCode = "    public void testMethod(int a, int b, int c) {\n" +
                "        if (a > 0 || (b < 0 || c == 0)) {\n" +
                "            System.out.println(\"At least one condition true\");\n" +
                "        } else {\n" +
                "            System.out.println(\"All conditions false\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("a > 0", cfg.getContent());

        CfgNode aFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        CfgNode aTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();

        Assert.assertTrue(aTrueNode.getContent().contains("System.out.println(\"At least one condition true\");\n"));

        Assert.assertTrue(aFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("b < 0", aFalseNode.getContent());

        CfgNode bTrueNode = ((CfgBoolExprNode) aFalseNode).getTrueNode();
        CfgNode bFalseNode = ((CfgBoolExprNode) aFalseNode).getFalseNode();

        Assert.assertTrue(bTrueNode.getContent().contains("System.out.println(\"At least one condition true\");\n"));

        Assert.assertTrue(bFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("c == 0", bFalseNode.getContent());

        CfgNode cTrueNode = ((CfgBoolExprNode) bFalseNode).getTrueNode();
        CfgNode cFalseNode = ((CfgBoolExprNode) bFalseNode).getFalseNode();

        Assert.assertTrue(cTrueNode.getContent().contains("System.out.println(\"At least one condition true\");\n"));
        Assert.assertTrue(cFalseNode.getContent().contains("System.out.println(\"All conditions false\");\n"));

        CfgNode endNodeFromCFalse = cFalseNode.getAfterNode();
        CfgNode endNodeFromCTrue = cTrueNode.getAfterNode();
        CfgNode endNodeFromBTrue = bTrueNode.getAfterNode();
        Assert.assertEquals(endNodeFromCFalse, endNodeFromCTrue);
        Assert.assertEquals(endNodeFromBTrue, endNodeFromCFalse);
        Assert.assertTrue(endNodeFromCFalse.isEndBlock());
    }

    @Test
    public void test_15_generateCfgForIfWithEmptyElseBlock() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        if (x > 0) {\n" +
                "            System.out.println(\"Positive\");\n" +
                "        } else {\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"Positive\");\n", thenNode.getContent());
        Assert.assertTrue(elseNode.isEndBlock());
        CfgNode endThenNode = thenNode.getAfterNode();
        Assert.assertTrue(endThenNode.isEndBlock());
        Assert.assertEquals(endThenNode, elseNode);
    }

    @Test
    public void test_16_generateCfgForIfWithNotOperator_Mcdc() {
        String sourceCode = "    public void testMethod(boolean x) {\n" +
                "        if (!x) {\n" +
                "            System.out.println(\"x is false\");\n" +
                "        } else {\n" +
                "            System.out.println(\"x is true\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        String content = cfg.getContent();
        Assert.assertEquals("x", content);

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(falseNode.getContent().contains("x is false"));
        Assert.assertTrue(trueNode.getContent().contains("x is true"));

        CfgNode endNode = trueNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_17_generateCfgForIfWithNotAndCondition_Mcdc() {
        String sourceCode = "    public void testMethod(int a, int b) {\n" +
                "        if (!(a > 0 && b < 0)) {\n" +
                "            System.out.println(\"Condition false\");\n" +
                "        } else {\n" +
                "            System.out.println(\"Condition true\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        String content = cfg.getContent();
        Assert.assertTrue(content.contains("a > 0"));

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals("b < 0", trueNode.getContent());
        CfgNode bTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode bFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertTrue(bTrueNode.getContent().contains("Condition true"));
        Assert.assertTrue(bFalseNode.getContent().contains("Condition false"));

        Assert.assertTrue(falseNode.getContent().contains("Condition false"));
        CfgNode endNode = falseNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_18_generateCfgForIfWithNotOrCondition_Mcdc() {
        String sourceCode = "    public void testMethod(int a, int b) {\n" +
                "        if (!(a > 0 || b < 0)) {\n" +
                "            System.out.println(\"Both conditions false\");\n" +
                "        } else {\n" +
                "            System.out.println(\"At least one true\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        String content = cfg.getContent();
        Assert.assertTrue(content.contains("a > 0"));
        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("System.out.println(\"At least one true\");\n", trueNode.getContent());
        Assert.assertTrue(falseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("b < 0", falseNode.getContent());
        CfgNode bTrueNode = ((CfgBoolExprNode) falseNode).getTrueNode();
        CfgNode bFalseNode = ((CfgBoolExprNode) falseNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"At least one true\");\n", bTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Both conditions false\");\n", bFalseNode.getContent());
        CfgNode endNode = bFalseNode.getAfterNode();
        Assert.assertTrue(endNode.isEndBlock());
    }

    @Test
    public void test_19_generateCfgForWhileStatement() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(trueNode.getContent().contains("System.out.println"));
        CfgNode decrementNode = trueNode.getAfterNode();
        Assert.assertTrue(decrementNode.getContent().contains("x--"));
        CfgNode backToWhileNode = decrementNode.getAfterNode();
        Assert.assertEquals(backToWhileNode, cfg);

        Assert.assertEquals(falseNode, afterBlock);

        CfgNode bodyAfter = decrementNode.getAfterNode();
        Assert.assertEquals(bodyAfter, cfg);
    }

    @Test
    public void test_20_generateCfgForNestedWhileStatement() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        while (x > 0) {\n" +
                "            while (y > 0) {\n" +
                "                System.out.println(y);\n" +
                "                y--;\n" +
                "            }\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode outerTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode outerFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(outerFalseNode, afterBlock);

        Assert.assertTrue(outerTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", outerTrueNode.getContent());

        CfgNode innerTrueNode = ((CfgBoolExprNode) outerTrueNode).getTrueNode();
        CfgNode innerFalseNode = ((CfgBoolExprNode) outerTrueNode).getFalseNode();

        Assert.assertTrue(innerFalseNode.getContent().contains("x--"));

        Assert.assertTrue(innerTrueNode.getContent().contains("System.out.println(y);"));
        CfgNode decrementYNode = innerTrueNode.getAfterNode();
        Assert.assertTrue(decrementYNode.getContent().contains("y--"));
        CfgNode backToInnerWhileNode = decrementYNode.getAfterNode();
        Assert.assertEquals(backToInnerWhileNode, outerTrueNode);

        CfgNode bodyAfterInnerWhile = decrementYNode.getAfterNode();
        Assert.assertEquals(bodyAfterInnerWhile, outerTrueNode);
    }

    @Test
    public void test_21_generateCfgForWhileInsideIf() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        if (x > 0) {\n" +
                "            while (y > 0) {\n" +
                "                System.out.println(y);\n" +
                "                y--;\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode ifTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(ifTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", ifTrueNode.getContent());
        Assert.assertEquals(ifFalseNode, afterBlock);
    }

    @Test
    public void test_22_generateCfgForIfInsideWhile() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        while (x > 0) {\n" +
                "            if (y > 0) {\n" +
                "                System.out.println(\"Both positive\");\n" +
                "            }\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", trueNode.getContent());
        Assert.assertEquals(falseNode, afterBlock);

        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();

        Assert.assertTrue(ifTrueNode.getContent().contains("System.out.println(\"Both positive\");"));
        Assert.assertTrue(ifFalseNode.getContent().contains("x--"));
        CfgNode backToWhileNode = ifFalseNode.getAfterNode();
        Assert.assertEquals(backToWhileNode, cfg);
    }

    @Test
    public void test_23_generateCfgForWhileWithEmptyBody() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(trueNode, cfg);
        Assert.assertEquals(falseNode, afterBlock);
    }

    @Test
    public void test_24_generateCfgForWhileWithBreak() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            if (x == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode xLoopTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode xLoopFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(xLoopFalseNode, afterBlock);

        Assert.assertTrue(xLoopTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("x == 5", xLoopTrueNode.getContent());

        CfgNode xIfTrueNode = ((CfgBoolExprNode) xLoopTrueNode).getTrueNode();
        CfgNode xIfFalseNode = ((CfgBoolExprNode) xLoopTrueNode).getFalseNode();

        Assert.assertTrue(xIfTrueNode instanceof CfgBreakStatementNode);
        CfgNode afterBreakNode = xIfTrueNode.getAfterNode();
        Assert.assertEquals(afterBreakNode, afterBlock);

        Assert.assertEquals("x--;\n", xIfFalseNode.getContent());
        CfgNode afterXIfFalseNode = xIfFalseNode.getAfterNode();
        Assert.assertEquals(afterXIfFalseNode, cfg);
    }

    @Test
    public void test_25_generateCfgForWhileWithContinue() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            if (x == 5) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
    }

    @Test
    public void test_26_generateCfgForWhileWithReturn() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            if (x == 5) {\n" +
                "                return;\n" +
                "            }\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

    }

    @Test
    public void test_27_generateCfgForWhileWithMultipleStatements() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            System.out.println(\"Start\");\n" +
                "            x--;\n" +
                "            System.out.println(\"End\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = test.CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertTrue(trueNode.getContent().contains("System.out.println(\"Start\");"));
        CfgNode decrementNode = trueNode.getAfterNode();
        Assert.assertTrue(decrementNode.getContent().contains("x--"));
        CfgNode afterDecrementNode = decrementNode.getAfterNode();
        Assert.assertTrue(afterDecrementNode.getContent().contains("System.out.println(\"End\");"));
        CfgNode backToWhileNode = afterDecrementNode.getAfterNode();
        Assert.assertEquals(backToWhileNode, cfg);
        Assert.assertEquals(falseNode, afterBlock);
    }

    @Test
    public void test_28_generateCfgForWhileWithComplexCondition_Mcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        while (x > 0 && y < 10) {\n" +
                "            System.out.println(x + y);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        String content = cfg.getContent();
        Assert.assertTrue(content.contains("x > 0"));

        CfgNode xTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode xFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = ((CfgBoolExprNode) xTrueNode).getTrueNode();
        CfgNode yFalseNode = ((CfgBoolExprNode) xTrueNode).getFalseNode();

        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals("System.out.println(x + y);\n", yTrueNode.getContent());
        CfgNode xDecrementNode = yTrueNode.getAfterNode();
        Assert.assertTrue(xDecrementNode.getContent().contains("x--"));

        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertTrue(yIncrementNode.getContent().contains("y++"));
        CfgNode backToXConditionNode = yIncrementNode.getAfterNode();
        Assert.assertEquals(backToXConditionNode, cfg);


    }

    @Test
    public void test_29_generateCfgForWhileWithOrCondition_Mcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        while (x > 0 || y < 10) {\n" +
                "            System.out.println(\"Looping\");\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        CfgNode xTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode xFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals("System.out.println(\"Looping\");\n", xTrueNode.getContent());
        CfgNode xDecrementNode = xTrueNode.getAfterNode();
        Assert.assertTrue(xDecrementNode.getContent().contains("x--"));
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertTrue(yIncrementNode.getContent().contains("y++"));
        CfgNode backToXConditionNode = yIncrementNode.getAfterNode();
        Assert.assertEquals(backToXConditionNode, cfg);
        Assert.assertTrue(xFalseNode instanceof CfgBoolExprNode);
        CfgNode yTrueNode = ((CfgBoolExprNode) xFalseNode).getTrueNode();
        CfgNode yFalseNode = ((CfgBoolExprNode) xFalseNode).getFalseNode();
        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals(xTrueNode, yTrueNode);
    }

    @Test
    public void test_30_generateCfgForWhileWithoutBody() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0)\n" +
                "            x--;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("x--;\n", trueNode.getContent());
    }

    @Test
    public void test_31_generateCfgIfElseInsideWhile() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        while (x > 0) {\n" +
                "            if (y > 0) {\n" +
                "                System.out.println(\"Both positive\");\n" +
                "            } else {\n" +
                "                System.out.println(\"x positive, y non-positive\");\n" +
                "            }\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());

        CfgNode outerTrueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode outerFalseNode = ((CfgBoolExprNode) cfg).getFalseNode();

        Assert.assertEquals(outerFalseNode, afterBlock);

        Assert.assertTrue(outerTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("y > 0", outerTrueNode.getContent());

        CfgNode innerTrueNode = ((CfgBoolExprNode) outerTrueNode).getTrueNode();
        CfgNode innerFalseNode = ((CfgBoolExprNode) outerTrueNode).getFalseNode();

        Assert.assertTrue(innerTrueNode.getContent().contains("System.out.println(\"Both positive\");"));
        Assert.assertTrue(innerFalseNode.getContent().contains("System.out.println(\"x positive, y non-positive\");"));

        CfgNode afterIfNode = innerFalseNode.getAfterNode();
        Assert.assertTrue(afterIfNode.getContent().contains("x--"));
        CfgNode backToWhileNode = afterIfNode.getAfterNode();
        Assert.assertEquals(backToWhileNode, cfg);
    }

}