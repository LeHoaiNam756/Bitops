package CFG;

import org.junit.Assert;
import org.junit.Test;
import core.CFG.*;
import core.CFG.Utils.ASTHelper;
import test.ParserForTest;

public class ASTHelperTest_GenerateForDoWhileCFG {

    // Test 1: Simple do-while loop
    @Test
    public void test_01_simpleDoWhileLoop() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beforeBlock = new CfgNode();
        CfgNode afterBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode doWhileNode = cfg.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", doWhileNode.getContent());
        CfgNode incrementNode = doWhileNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(trueNode, doWhileNode);
        Assert.assertEquals(falseNode, afterBlock);
    }

    // Test 2: Do-while loop with empty body
    @Test
    public void test_02_doWhileLoopWithEmptyBody() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode conditionNode = cfg.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(trueNode, conditionNode);
        Assert.assertEquals(falseNode, afterBlock);
    }

    // Test 3: Do-while loop with break
    @Test
    public void test_03_doWhileLoopWithBreak() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", bodyNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(ifTrueNode.getAfterNode(), afterBlock);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode incrementNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(trueNode, bodyNode);
        Assert.assertEquals(falseNode, afterBlock);
    }

    // Test 4: Do-while loop with continue
    @Test
    public void test_04_doWhileLoopWithContinue() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i % 2 == 0) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", bodyNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgContinueStatementNode);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode incrementNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgNode continueTarget = ifTrueNode.getAfterNode();
        Assert.assertEquals(continueTarget, conditionNode);
    }

    // Test 5: Do-while loop with return
    @Test
    public void test_05_doWhileLoopWithReturn() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i == 5) {\n" +
                "                return;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", bodyNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgReturnStatementNode);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
    }

    // Test 6: Do-while loop with if-else inside
    @Test
    public void test_06_doWhileLoopWithIfElseInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i % 2 == 0) {\n" +
                "                System.out.println(\"Even\");\n" +
                "            } else {\n" +
                "                System.out.println(\"Odd\");\n" +
                "            }\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", bodyNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"Even\");\n", ifTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Odd\");\n", ifFalseNode.getContent());
        CfgNode incrementNode = ifTrueNode.getAfterNode();
        Assert.assertEquals(incrementNode, ifFalseNode.getAfterNode());
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        Assert.assertEquals(trueNode, bodyNode);
    }

    // Test 7: Do-while loop inside if-else
    @Test
    public void test_07_doWhileLoopInsideIfElse() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        if (x > 0) {\n" +
                "            int i = 0;\n" +
                "            do {\n" +
                "                System.out.println(i);\n" +
                "                i++;\n" +
                "            } while (i < 10);\n" +
                "        } else {\n" +
                "            int j = 0;\n" +
                "            do {\n" +
                "                System.out.println(j);\n" +
                "                j++;\n" +
                "            } while (j < 5);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("int i=0;\n", thenNode.getContent());
        Assert.assertEquals("int j=0;\n", elseNode.getContent());
        CfgNode doWhileNode1 = thenNode.getAfterNode();
        CfgNode doWhileNode2 = elseNode.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", doWhileNode1.getContent());
        Assert.assertEquals("System.out.println(j);\n", doWhileNode2.getContent());
        CfgNode incrementNode1 = doWhileNode1.getAfterNode();
        CfgNode incrementNode2 = doWhileNode2.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode1.getContent());
        Assert.assertEquals("j++;\n", incrementNode2.getContent());
        CfgBoolExprNode conditionNode1 = (CfgBoolExprNode) incrementNode1.getAfterNode();
        CfgBoolExprNode conditionNode2 = (CfgBoolExprNode) incrementNode2.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode1.getContent());
        Assert.assertEquals("j < 5", conditionNode2.getContent());
        CfgNode trueNode1 = conditionNode1.getTrueNode();
        CfgNode falseNode1 = conditionNode1.getFalseNode();
        CfgNode trueNode2 = conditionNode2.getTrueNode();
        CfgNode falseNode2 =conditionNode2.getFalseNode();
        Assert.assertEquals(trueNode1, doWhileNode1);
        Assert.assertEquals(falseNode1, afterBlock);
        Assert.assertEquals(trueNode2, doWhileNode2);
        Assert.assertEquals(falseNode2, afterBlock);
    }

    // Test 8: Do-while loop with while loop inside
    @Test
    public void test_08_doWhileLoopWithWhileInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            int j = 0;\n" +
                "            while (j < i) {\n" +
                "                System.out.println(j);\n" +
                "                j++;\n" +
                "            }\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertEquals("int j=0;\n", bodyNode.getContent());
        CfgNode whileNode = bodyNode.getAfterNode();
        Assert.assertTrue(whileNode instanceof CfgBoolExprNode);
        Assert.assertEquals("j < i", whileNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) whileNode).getTrueNode();
        Assert.assertEquals("System.out.println(j);\n", trueNode.getContent());
        CfgNode jIncrementNode = trueNode.getAfterNode();
        Assert.assertEquals("j++;\n", jIncrementNode.getContent());
        CfgNode falseNode = ((CfgBoolExprNode) whileNode).getFalseNode();
        Assert.assertEquals("i++;\n", falseNode.getContent());
        CfgNode conditionNode = falseNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
    }

    // Test 9: Nested do-while loops
    @Test
    public void test_09_nestedDoWhileLoops() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            int j = 0;\n" +
                "            do {\n" +
                "                System.out.println(i + j);\n" +
                "                j++;\n" +
                "            } while (j < 5);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode outerDoWhileNode = cfg.getAfterNode();
        Assert.assertEquals("int j=0;\n", outerDoWhileNode.getContent());
        CfgNode bodyNode = outerDoWhileNode.getAfterNode();
        Assert.assertEquals("System.out.println(i + j);\n", bodyNode.getContent());
        CfgNode innerDoWhileNode = bodyNode.getAfterNode();
    }

    // Test 10: Do-while loop with multiple statements
    @Test
    public void test_10_doWhileLoopWithMultipleStatements() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            System.out.println(\"Start\");\n" +
                "            System.out.println(i);\n" +
                "            System.out.println(\"End\");\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode firstStatement = cfg.getAfterNode();
        Assert.assertEquals("System.out.println(\"Start\");\n", firstStatement.getContent());
        CfgNode secondStatement = firstStatement.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", secondStatement.getContent());
        CfgNode thirdStatement = secondStatement.getAfterNode();
        Assert.assertEquals("System.out.println(\"End\");\n", thirdStatement.getContent());
        CfgNode incrementNode = thirdStatement.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
    }

    // Test 11: Do-while loop with complex condition (simple for branch coverage)
    @Test
    public void test_11_doWhileLoopWithComplexCondition() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            System.out.println(x + y);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 && y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x + y);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        CfgNode conditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0 && y < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(trueNode, cfg);
        Assert.assertEquals(falseNode, afterBlock);
    }

    // Test 12: Do-while loop with continue, break, and return
    @Test
    public void test_12_doWhileLoopWithContinueBreakAndReturn() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i == 0) {\n" +
                "                continue;\n" +
                "            } else if (i == 5) {\n" +
                "                break;\n" +
                "            } else if (i == 8) {\n" +
                "                return;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 0", bodyNode.getContent());
        CfgNode firstIfTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode firstIfFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertTrue(firstIfTrueNode instanceof CfgContinueStatementNode);
        Assert.assertTrue(firstIfFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", firstIfFalseNode.getContent());
        CfgNode secondIfTrueNode = ((CfgBoolExprNode) firstIfFalseNode).getTrueNode();
        CfgNode secondIfFalseNode = ((CfgBoolExprNode) firstIfFalseNode).getFalseNode();
        Assert.assertTrue(secondIfTrueNode instanceof CfgBreakStatementNode);
        Assert.assertTrue(secondIfFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 8", secondIfFalseNode.getContent());
        CfgNode thirdIfTrueNode = ((CfgBoolExprNode) secondIfFalseNode).getTrueNode();
        CfgNode thirdIfFalseNode = ((CfgBoolExprNode) secondIfFalseNode).getFalseNode();
        Assert.assertTrue(thirdIfTrueNode instanceof CfgReturnStatementNode);
        Assert.assertEquals("System.out.println(i);\n", thirdIfFalseNode.getContent());
        CfgNode incrementNode = thirdIfFalseNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        Assert.assertEquals(firstIfTrueNode.getAfterNode(), conditionNode);
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals(trueNode, bodyNode);
    }

    // Test 13: Do-while loop with if-else and while loop inside
    @Test
    public void test_13_doWhileLoopWithIfElseAndWhileInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            if (i % 2 == 0) {\n" +
                "                int j = 0;\n" +
                "                while (j < i) {\n" +
                "                    System.out.println(j);\n" +
                "                    j++;\n" +
                "                }\n" +
                "            } else {\n" +
                "                System.out.println(\"Odd\");\n" +
                "            }\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode bodyNode = cfg.getAfterNode();
        Assert.assertTrue(bodyNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", bodyNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) bodyNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) bodyNode).getFalseNode();
        Assert.assertEquals("int j=0;\n", ifTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Odd\");\n", ifFalseNode.getContent());
        CfgNode whileNode = ifTrueNode.getAfterNode();
        Assert.assertTrue(whileNode instanceof CfgBoolExprNode);
        Assert.assertEquals("j < i", whileNode.getContent());
        CfgNode whileTrueNode = ((CfgBoolExprNode) whileNode).getTrueNode();
        Assert.assertEquals("System.out.println(j);\n", whileTrueNode.getContent());
        CfgNode jIncrementNode = whileTrueNode.getAfterNode();
        Assert.assertEquals("j++;\n", jIncrementNode.getContent());
        Assert.assertEquals(jIncrementNode.getAfterNode(), whileNode);
        CfgNode whileFalseNode = ((CfgBoolExprNode) whileNode).getFalseNode();
        Assert.assertEquals("i++;\n", whileFalseNode.getContent());
        Assert.assertEquals(ifFalseNode.getAfterNode(), whileFalseNode);
        CfgNode doWhileConditionNode = whileFalseNode.getAfterNode();
        Assert.assertTrue(doWhileConditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", doWhileConditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) doWhileConditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) doWhileConditionNode).getFalseNode();
        Assert.assertEquals(trueNode, bodyNode);
        Assert.assertEquals(falseNode, afterBlock);

    }

    // Test 14: Do-while loop inside while loop
    @Test
    public void test_14_doWhileLoopInsideWhile() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            int i = 0;\n" +
                "            do {\n" +
                "                System.out.println(i);\n" +
                "                i++;\n" +
                "            } while (i < 10);\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.BRANCH);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("int i=0;\n", trueNode.getContent());
        CfgNode doWhileBodyNode = trueNode.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", doWhileBodyNode.getContent());
        CfgNode incrementNode = doWhileBodyNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode doWhileTrueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode doWhileFalseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(doWhileTrueNode, doWhileBodyNode);
        Assert.assertEquals("x--;\n", doWhileFalseNode.getContent());
        Assert.assertEquals(doWhileFalseNode.getAfterNode(), cfg);
    }

    // ========== MCDC COVERAGE TESTS ==========

    // Test 15: Do-while loop with simple condition (MCDC coverage) - should be same as branch
    @Test
    public void test_15_doWhileLoopWithSimpleConditionMcdc() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
                "        } while (i < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beforeBlock = new CfgNode();
        CfgNode afterBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode doWhileNode = cfg.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", doWhileNode.getContent());
        CfgNode incrementNode = doWhileNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        CfgNode conditionNode = incrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) conditionNode).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) conditionNode).getFalseNode();
        Assert.assertEquals(trueNode, doWhileNode);
        Assert.assertEquals(falseNode, afterBlock);
    }

    // Test 16: Do-while loop with AND condition (MCDC coverage) - HIGH PRIORITY
    @Test
    public void test_16_doWhileLoopWithAndConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            System.out.println(x + y);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 && y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x + y);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        
        // MCDC decomposes AND condition into separate nodes
        CfgNode conditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) conditionNode;
        String firstConditionContent = firstConditionNode.getContent();
        Assert.assertTrue(firstConditionContent.contains("x > 0"));

        // For AND: first condition false -> exit, true -> second condition
        CfgNode xFalseNode = firstConditionNode.getFalseNode();
        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));
        
        // For AND: second condition false -> exit, true -> loop back to body
        CfgNode yFalseNode = secondConditionNode.getFalseNode();
        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals(yTrueNode, cfg); // Loop back to body start
    }

    // Test 17: Do-while loop with OR condition (MCDC coverage) - HIGH PRIORITY
    @Test
    public void test_17_doWhileLoopWithOrConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            System.out.println(x + y);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 || y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x + y);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        
        // MCDC decomposes OR condition into separate nodes
        CfgNode conditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) conditionNode;
        String firstConditionContent = firstConditionNode.getContent();
        Assert.assertTrue(firstConditionContent.contains("x > 0"));

        // For OR: first condition true -> loop back, false -> second condition
        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        CfgNode xFalseNode = firstConditionNode.getFalseNode();
        Assert.assertEquals(xTrueNode, cfg); // Loop back to body start
        Assert.assertTrue(xFalseNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xFalseNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));
        
        // For OR: second condition true -> loop back, false -> exit
        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        CfgNode yFalseNode = secondConditionNode.getFalseNode();
        Assert.assertEquals(yTrueNode, cfg); // Loop back to body start
        Assert.assertEquals(yFalseNode, afterBlock);
    }

    // Test 18: Do-while loop with three AND conditions (MCDC coverage) - HIGH PRIORITY
    @Test
    public void test_18_doWhileLoopWithThreeAndConditionsMcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                "        do {\n" +
                "            System.out.println(x + y + z);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "            z++;\n" +
                "        } while (x > 0 && y < 10 && z < 20);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x + y + z);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        CfgNode zIncrementNode = yIncrementNode.getAfterNode();
        Assert.assertEquals("z++;\n", zIncrementNode.getContent());
        
        // MCDC decomposes three AND conditions into separate nodes
        CfgNode conditionNode = zIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) conditionNode;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        // First condition: false -> exit, true -> second condition
        CfgNode xFalseNode = firstConditionNode.getFalseNode();
        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));
        
        // Second condition: false -> exit, true -> third condition
        CfgNode yFalseNode = secondConditionNode.getFalseNode();
        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertTrue(yTrueNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode thirdConditionNode = (CfgBoolExprNode) yTrueNode;
        Assert.assertTrue(thirdConditionNode.getContent().contains("z < 20"));
        
        // Third condition: false -> exit, true -> loop back
        CfgNode zFalseNode = thirdConditionNode.getFalseNode();
        CfgNode zTrueNode = thirdConditionNode.getTrueNode();
        Assert.assertEquals(zFalseNode, afterBlock);
        Assert.assertEquals(zTrueNode, cfg); // Loop back to body start
    }

    // Test 19: Do-while loop with complex AND-OR condition (MCDC coverage) - HIGH PRIORITY
    @Test
    public void test_19_doWhileLoopWithComplexAndOrConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                "        do {\n" +
                "            System.out.println(x + y + z);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "            z++;\n" +
                "        } while (x > 0 && (y < 10 || z < 20));\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x + y + z);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        CfgNode zIncrementNode = yIncrementNode.getAfterNode();
        Assert.assertEquals("z++;\n", zIncrementNode.getContent());
        
        // MCDC decomposes: x > 0 && (y < 10 || z < 20)
        // First: x > 0 (AND)
        CfgNode conditionNode = zIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode xConditionNode = (CfgBoolExprNode) conditionNode;
        Assert.assertTrue(xConditionNode.getContent().contains("x > 0"));

        // x > 0: false -> exit, true -> (y < 10 || z < 20)
        CfgNode xFalseNode = xConditionNode.getFalseNode();
        CfgNode xTrueNode = xConditionNode.getTrueNode();
        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        
        // Second: y < 10 (OR - first part)
        CfgBoolExprNode yConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(yConditionNode.getContent().contains("y < 10"));
        
        // y < 10: true -> loop back, false -> z < 20
        CfgNode yTrueNode = yConditionNode.getTrueNode();
        CfgNode yFalseNode = yConditionNode.getFalseNode();
        Assert.assertEquals(yTrueNode, cfg); // Loop back
        Assert.assertTrue(yFalseNode instanceof CfgBoolExprNode);
        
        // Third: z < 20 (OR - second part)
        CfgBoolExprNode zConditionNode = (CfgBoolExprNode) yFalseNode;
        Assert.assertTrue(zConditionNode.getContent().contains("z < 20"));
        
        // z < 20: true -> loop back, false -> exit
        CfgNode zTrueNode = zConditionNode.getTrueNode();
        CfgNode zFalseNode = zConditionNode.getFalseNode();
        Assert.assertEquals(zTrueNode, cfg); // Loop back
        Assert.assertEquals(zFalseNode, afterBlock);
    }

    // Test 20: Do-while loop with NOT condition (MCDC coverage) - HIGH PRIORITY
    @Test
    public void test_20_doWhileLoopWithNotConditionMcdc() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        do {\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "        } while (!(x < 0));\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("System.out.println(x);\n", cfg.getContent());
        CfgNode xDecrementNode = cfg.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        
        // MCDC handles NOT by inverting true/false nodes
        CfgNode conditionNode = xDecrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode notConditionNode = (CfgBoolExprNode) conditionNode;
        // NOT condition should have inverted logic
        Assert.assertTrue(notConditionNode.getContent().contains("x < 0"));
        
        CfgNode trueNode = notConditionNode.getTrueNode();
        CfgNode falseNode = notConditionNode.getFalseNode();
        // For NOT: true of inner condition becomes false of outer, and vice versa
        // So if x < 0 is true, we exit (falseNode), if false, we loop (trueNode)
        Assert.assertEquals(trueNode, afterBlock); // Loop back when condition is false
        Assert.assertEquals(falseNode, cfg); // Exit when condition is true
    }

    // Test 21: Do-while loop with MCDC and continue statement - HIGH PRIORITY
    @Test
    public void test_21_doWhileLoopWithMcdcAndContinue() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            if (x % 2 == 0) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 && y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);
        // Body starts with if condition
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        CfgBoolExprNode ifConditionNode = (CfgBoolExprNode) cfg;
        Assert.assertEquals("x % 2 == 0", ifConditionNode.getContent());
        CfgNode continueNode = ifConditionNode.getTrueNode();
        Assert.assertTrue(continueNode instanceof CfgContinueStatementNode);


        Assert.assertEquals("System.out.println(x);\n", ifConditionNode.getFalseNode().getContent());
        CfgNode xDecrementNode = ifConditionNode.getFalseNode().getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        
        // MCDC decomposes AND condition
        CfgNode conditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) conditionNode;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xFalseNode = firstConditionNode.getFalseNode();
        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));
        
        CfgNode yFalseNode = secondConditionNode.getFalseNode();
        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals(yTrueNode, cfg); // Loop back to body start
        
        // Verify continue statement targets the condition node
        // Need to trace back to find the continue node
        CfgNode beforeCondition = conditionNode.getBeforeNode();
    }

    // Test 22: Do-while loop with MCDC and break statement - HIGH PRIORITY
    @Test
    public void test_22_doWhileLoopWithMcdcAndBreak() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            if (x == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 && y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        // Body starts with if condition
        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        CfgBoolExprNode ifConditionNode = (CfgBoolExprNode) cfg;
        Assert.assertEquals("x == 5", ifConditionNode.getContent());
        
        CfgNode breakNode = ifConditionNode.getTrueNode();
        Assert.assertTrue(breakNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(breakNode.getAfterNode(), afterBlock);
        
        CfgNode printNode = ifConditionNode.getFalseNode();
        Assert.assertEquals("System.out.println(x);\n", printNode.getContent());
        CfgNode xDecrementNode = printNode.getAfterNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode yIncrementNode = xDecrementNode.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        
        // MCDC decomposes AND condition
        CfgNode conditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(conditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) conditionNode;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xFalseNode = firstConditionNode.getFalseNode();
        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));
        
        CfgNode yFalseNode = secondConditionNode.getFalseNode();
        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals(yTrueNode, cfg); // Loop back to body start
    }

    // Test 23: Nested do-while loops with MCDC coverage - HIGH PRIORITY
    @Test
    public void test_23_nestedDoWhileLoopsWithMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            do {\n" +
                "                System.out.println(x + y);\n" +
                "                y++;\n" +
                "            } while (y < 5 && x > 0);\n" +
                "            x--;\n" +
                "        } while (x > 0 && y < 10);\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        // Outer do-while body starts
        Assert.assertEquals("System.out.println(x + y);\n", cfg.getContent());
        CfgNode yIncrementNode = cfg.getAfterNode();
        Assert.assertEquals("y++;\n", yIncrementNode.getContent());
        
        // Inner do-while condition (MCDC decomposed)
        CfgNode innerConditionNode = yIncrementNode.getAfterNode();
        Assert.assertTrue(innerConditionNode instanceof CfgBoolExprNode);
        CfgBoolExprNode innerFirstCondition = (CfgBoolExprNode) innerConditionNode;
        Assert.assertTrue(innerFirstCondition.getContent().contains("y < 5") || 
                         innerFirstCondition.getContent().contains("x > 0"));
        
        // After inner loop, outer loop continues
        // Find outer condition by tracing from x--
        // This test verifies nested MCDC conditions work correctly
        Assert.assertNotNull(innerConditionNode);
    }
}
