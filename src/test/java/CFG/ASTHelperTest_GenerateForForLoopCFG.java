package CFG;

import org.junit.Assert;
import org.junit.Test;
import core.CFG.*;
import core.CFG.Utils.ASTHelper;
import test.ParserForTest;

public class ASTHelperTest_GenerateForForLoopCFG {

    // Test 1: Simple for loop
    @Test
    public void test_01_simpleForLoop() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beforeBlock = new CfgNode();
        CfgNode afterBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraph(cfg);
//        System.out.println(mermaid);

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitilizer = cfg.getAfterNode();
        Assert.assertTrue(afterInitilizer instanceof CfgBoolExprNode);
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) afterInitilizer;
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();

        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("System.out.println(i);\n", trueNode.getContent());
        CfgNode updateNode = trueNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
    }

    // Test 2: For loop with 0 initializers
    @Test
    public void test_02_forLoopWithZeroInitializers() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int i = 0;\n" +
                "        for (; i < 10; i++) {\n" +
                "            System.out.println(i);\n" +
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
        Assert.assertEquals("int i=0;\n", cfg.getContent());
        CfgNode forNode = cfg.getAfterNode();
        CfgNode afterInitilizer = forNode.getAfterNode();
        Assert.assertTrue(afterInitilizer instanceof CfgBoolExprNode);
        CfgNode trueNode = ((CfgBoolExprNode) afterInitilizer).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) afterInitilizer).getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("System.out.println(i);\n", trueNode.getContent());
        CfgNode updateNode = trueNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(afterInitilizer, updateNode.getAfterNode());
    }


    // Test 4: For loop with 2 initializers
    @Test
    public void test_04_forLoopWithTwoInitializers() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0, j = 0; i < 10; i++) {\n" +
                "            System.out.println(i + j);\n" +
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

        Assert.assertEquals("int i=0, j=0", cfg.getContent());
    }

    // Test 5: For loop with 0 condition (infinite loop)
    @Test
    public void test_05_forLoopWithZeroCondition() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; ; i++) {\n" +
                "            if (i > 10) break;\n" +
                "            System.out.println(i);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("Empty Condition", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(trueNode, falseNode);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertEquals("break;\n", ifTrueNode.getContent());
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode updateNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
    }

    // Test 6: For loop with 1 condition

    // Test 7: For loop with complex condition (multiple conditions with &&)
    @Test
    public void test_07_forLoopWithComplexCondition() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10 && i > 0; i++) {\n" +
                "            System.out.println(i);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10 && i > 0", conditionNode.getContent());
    }

    // Test 8: For loop with 0 updaters
    @Test
    public void test_08_forLoopWithZeroUpdaters() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; ) {\n" +
                "            System.out.println(i);\n" +
                "            i++;\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        Assert.assertEquals("System.out.println(i);\n", trueNode.getContent());
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        CfgNode incrementNode = trueNode.getAfterNode();
        Assert.assertEquals("i++;\n", incrementNode.getContent());
        Assert.assertEquals("Empty Updater", incrementNode.getAfterNode().getContent());
        Assert.assertEquals(conditionNode, incrementNode.getAfterNode().getAfterNode());
    }

    // Test 9: For loop with 1 updater

    // Test 10: For loop with 2 updaters
    @Test
    public void test_10_forLoopWithTwoUpdaters() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0, j = 0; i < 10; i++, j++) {\n" +
                "            System.out.println(i + j);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("System.out.println(i + j);\n", trueNode.getContent());
        CfgNode updateNode = trueNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        CfgNode secondUpdateNode = updateNode.getAfterNode();
        Assert.assertEquals("j++", secondUpdateNode.getContent());
        Assert.assertEquals(conditionNode, secondUpdateNode.getAfterNode());
    }

    // Test 11: For loop with empty body
    @Test
    public void test_11_forLoopWithEmptyBody() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("i++", trueNode.getContent());
        Assert.assertEquals(conditionNode, trueNode.getAfterNode());
    }

    // Test 12: For loop with if-else inside
    @Test
    public void test_12_forLoopWithIfElseInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i % 2 == 0) {\n" +
                "                System.out.println(\"Even\");\n" +
                "            } else {\n" +
                "                System.out.println(\"Odd\");\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", trueNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertEquals("System.out.println(\"Even\");\n", ifTrueNode.getContent());
        Assert.assertEquals("System.out.println(\"Odd\");\n", ifFalseNode.getContent());
        CfgNode updateNode = ifTrueNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(ifTrueNode.getAfterNode(), ifFalseNode.getAfterNode());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
    }

    // Test 13: For loop with while loop inside
    @Test
    public void test_13_forLoopWithWhileInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            int j = 0;\n" +
                "            while (j < i) {\n" +
                "                System.out.println(j);\n" +
                "                j++;\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode forTrueNode = conditionNode.getTrueNode();
        CfgNode forFalseNode = conditionNode.getFalseNode();
        Assert.assertEquals(forFalseNode, afterBlock);
        Assert.assertEquals("int j=0;\n", forTrueNode.getContent());
        CfgNode whileNode = forTrueNode.getAfterNode();
        Assert.assertTrue(whileNode instanceof CfgBoolExprNode);
        Assert.assertEquals("j < i", whileNode.getContent());
        CfgNode whileTrueNode = ((CfgBoolExprNode) whileNode).getTrueNode();
        CfgNode whileFalseNode = ((CfgBoolExprNode) whileNode).getFalseNode();
        Assert.assertEquals("System.out.println(j);\n", whileTrueNode.getContent());
        CfgNode updateNode = whileTrueNode.getAfterNode();
        Assert.assertEquals("j++;\n", updateNode.getContent());
        Assert.assertEquals("i++", whileFalseNode.getContent());
        Assert.assertEquals(conditionNode, whileFalseNode.getAfterNode());
    }

    // Test 14: For loop with if-else and while loop inside
    @Test
    public void test_14_forLoopWithIfElseAndWhileInside() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i % 2 == 0) {\n" +
                "                int j = 0;\n" +
                "                while (j < i) {\n" +
                "                    System.out.println(j);\n" +
                "                    j++;\n" +
                "                }\n" +
                "            } else {\n" +
                "                System.out.println(\"Odd\");\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode forTrueNode = conditionNode.getTrueNode();
        CfgNode forFalseNode = conditionNode.getFalseNode();
        Assert.assertEquals(forFalseNode, afterBlock);
        Assert.assertTrue(forTrueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", forTrueNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) forTrueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) forTrueNode).getFalseNode();
        Assert.assertEquals("int j=0;\n", ifTrueNode.getContent());
        CfgNode whileNode = ifTrueNode.getAfterNode();
        Assert.assertTrue(whileNode instanceof CfgBoolExprNode);
        Assert.assertEquals("j < i", whileNode.getContent());
        CfgNode whileTrueNode = ((CfgBoolExprNode) whileNode).getTrueNode();
        CfgNode whileFalseNode = ((CfgBoolExprNode) whileNode).getFalseNode();
        Assert.assertEquals("System.out.println(j);\n", whileTrueNode.getContent());
        Assert.assertEquals("i++", whileFalseNode.getContent());
        CfgNode updateNode = whileTrueNode.getAfterNode();
        Assert.assertEquals("j++;\n", updateNode.getContent());
        Assert.assertEquals(conditionNode, whileFalseNode.getAfterNode());
        Assert.assertEquals("System.out.println(\"Odd\");\n", ifFalseNode.getContent());
        Assert.assertEquals(conditionNode, whileFalseNode.getAfterNode());
        Assert.assertEquals(ifFalseNode.getAfterNode(), whileFalseNode);
        Assert.assertEquals(forFalseNode, afterBlock);
    }

    // Test 15: For loop inside if-else
    @Test
    public void test_15_forLoopInsideIfElse() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        if (x > 0) {\n" +
                "            for (int i = 0; i < 10; i++) {\n" +
                "                System.out.println(i);\n" +
                "            }\n" +
                "        } else {\n" +
                "            for (int j = 0; j < 5; j++) {\n" +
                "                System.out.println(j);\n" +
                "            }\n" +
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
        CfgNode thenNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode elseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals("int i=0", thenNode.getContent());
        Assert.assertEquals("int j=0", elseNode.getContent());
        CfgBoolExprNode iConditionNode = (CfgBoolExprNode) thenNode.getAfterNode();
        Assert.assertEquals("i < 10", iConditionNode.getContent());
        CfgBoolExprNode jConditionNode = (CfgBoolExprNode) elseNode.getAfterNode();
        Assert.assertEquals("j < 5", jConditionNode.getContent());
        CfgNode iTrueNode = iConditionNode.getTrueNode();
        CfgNode jTrueNode = jConditionNode.getTrueNode();
        Assert.assertEquals("System.out.println(i);\n", iTrueNode.getContent());
        Assert.assertEquals("System.out.println(j);\n", jTrueNode.getContent());
        CfgNode iFalseNode = iConditionNode.getFalseNode();
        CfgNode jFalseNode = jConditionNode.getFalseNode();
        Assert.assertEquals(iFalseNode, afterBlock);
        Assert.assertEquals(jFalseNode, afterBlock);
        CfgNode iUpdateNode = iTrueNode.getAfterNode();
        CfgNode jUpdateNode = jTrueNode.getAfterNode();
        Assert.assertEquals("i++", iUpdateNode.getContent());
        Assert.assertEquals("j++", jUpdateNode.getContent());
        Assert.assertEquals(iConditionNode, iUpdateNode.getAfterNode());
        Assert.assertEquals(jConditionNode, jUpdateNode.getAfterNode());
    }


    // Test 16: For loop inside while loop
    @Test
    public void test_16_forLoopInsideWhile() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        while (x > 0) {\n" +
                "            for (int i = 0; i < 10; i++) {\n" +
                "                System.out.println(i);\n" +
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

        Assert.assertTrue(cfg instanceof CfgBoolExprNode);
        Assert.assertEquals("x > 0", cfg.getContent());
        CfgNode trueNode = ((CfgBoolExprNode) cfg).getTrueNode();
        CfgNode falseNode = ((CfgBoolExprNode) cfg).getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("int i=0", trueNode.getContent());
        CfgBoolExprNode iConditionNode = (CfgBoolExprNode) trueNode.getAfterNode();
        Assert.assertEquals("i < 10", iConditionNode.getContent());
        CfgNode iTrueNode = iConditionNode.getTrueNode();
        Assert.assertEquals("System.out.println(i);\n", iTrueNode.getContent());
        CfgNode xDecrementNode = iConditionNode.getFalseNode();
        Assert.assertEquals("x--;\n", xDecrementNode.getContent());
        CfgNode iUpdateNode = iTrueNode.getAfterNode();
        Assert.assertEquals("i++", iUpdateNode.getContent());
        Assert.assertEquals(iConditionNode, iUpdateNode.getAfterNode());
        Assert.assertEquals(cfg, xDecrementNode.getAfterNode());

    }

    // Test 17: For loop with continue
    @Test
    public void test_17_forLoopWithContinue() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i % 2 == 0) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i % 2 == 0", trueNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgContinueStatementNode);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode updateNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
        Assert.assertEquals(ifTrueNode.getAfterNode(), updateNode);
    }

    // Test 18: For loop with break
    @Test
    public void test_18_forLoopWithBreak() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", trueNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(ifTrueNode.getAfterNode(), afterBlock);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode updateNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());

    }

    // Test 19: For loop with return
    @Test
    public void test_19_forLoopWithReturn() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i == 5) {\n" +
                "                return;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", trueNode.getContent());
        CfgNode ifTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode ifFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertTrue(ifTrueNode instanceof CfgReturnStatementNode);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
        CfgNode updateNode = ifFalseNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());

    }

    // Test 20: For loop with continue, break, and return
    @Test
    public void test_20_forLoopWithContinueBreakAndReturn() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            if (i == 0) {\n" +
                "                continue;\n" +
                "            } else if (i == 5) {\n" +
                "                break;\n" +
                "            } else if (i == 8) {\n" +
                "                return;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 0", trueNode.getContent());
        CfgNode firstIfTrueNode = ((CfgBoolExprNode) trueNode).getTrueNode();
        CfgNode firstIfFalseNode = ((CfgBoolExprNode) trueNode).getFalseNode();
        Assert.assertTrue(firstIfTrueNode instanceof CfgContinueStatementNode);
        Assert.assertTrue(firstIfFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 5", firstIfFalseNode.getContent());
        CfgNode secondIfTrueNode = ((CfgBoolExprNode) firstIfFalseNode).getTrueNode();
        CfgNode secondIfFalseNode = ((CfgBoolExprNode) firstIfFalseNode).getFalseNode();
        Assert.assertTrue(secondIfTrueNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(secondIfTrueNode.getAfterNode(), afterBlock);
        Assert.assertTrue(secondIfFalseNode instanceof CfgBoolExprNode);
        Assert.assertEquals("i == 8", secondIfFalseNode.getContent());
        CfgNode thirdIfTrueNode = ((CfgBoolExprNode) secondIfFalseNode).getTrueNode();
        CfgNode thirdIfFalseNode = ((CfgBoolExprNode) secondIfFalseNode).getFalseNode();
        Assert.assertTrue(thirdIfTrueNode instanceof CfgReturnStatementNode);
        Assert.assertEquals("System.out.println(i);\n", thirdIfFalseNode.getContent());
        CfgNode updateNode = thirdIfFalseNode.getAfterNode();
        Assert.assertEquals(firstIfTrueNode.getAfterNode(), updateNode);
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
    }

    // Test 21: Nested for loops
    @Test
    public void test_21_nestedForLoops() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            for (int j = 0; j < 5; j++) {\n" +
                "                System.out.println(i + j);\n" +
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

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("int j=0", trueNode.getContent());
        CfgBoolExprNode jConditionNode = (CfgBoolExprNode) trueNode.getAfterNode();
        Assert.assertEquals("j < 5", jConditionNode.getContent());
        CfgNode jTrueNode = jConditionNode.getTrueNode();
        Assert.assertEquals("System.out.println(i + j);\n", jTrueNode.getContent());
        CfgNode jUpdateNode = jTrueNode.getAfterNode();
        Assert.assertEquals("j++", jUpdateNode.getContent());
        Assert.assertEquals(jConditionNode, jUpdateNode.getAfterNode());
        CfgNode iUpdateNode = jConditionNode.getFalseNode();
        Assert.assertEquals("i++", iUpdateNode.getContent());
        Assert.assertEquals(conditionNode, iUpdateNode.getAfterNode());

    }

    // Test 22: For loop with all parts empty (infinite loop)
    @Test
    public void test_22_forLoopWithAllPartsEmpty() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (;;) {\n" +
                "            System.out.println(\"Infinite\");\n" +
                "            break;\n" +
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

        Assert.assertEquals("Empty Initializer", cfg.getContent());
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("Empty Condition", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(trueNode, falseNode);
        Assert.assertEquals("System.out.println(\"Infinite\");\n", trueNode.getContent());
        CfgNode breakNode = trueNode.getAfterNode();
        Assert.assertTrue(breakNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(breakNode.getAfterNode(), afterBlock);
    }

    // Test 23: For loop with multiple statements in body
    @Test
    public void test_23_forLoopWithMultipleStatements() {
        String sourceCode = "    public void testMethod() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            System.out.println(\"Start\");\n" +
                "            System.out.println(i);\n" +
                "            System.out.println(\"End\");\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);

        CfgBoolExprNode conditionNode = (CfgBoolExprNode) cfg.getAfterNode();
        Assert.assertEquals("i < 10", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("System.out.println(\"Start\");\n", trueNode.getContent());
        CfgNode secondStatementNode = trueNode.getAfterNode();
        Assert.assertEquals("System.out.println(i);\n", secondStatementNode.getContent());
        CfgNode thirdStatementNode = secondStatementNode.getAfterNode();
        Assert.assertEquals("System.out.println(\"End\");\n", thirdStatementNode.getContent());
        CfgNode updateNode = thirdStatementNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(conditionNode, updateNode.getAfterNode());
    }

    // Test 24: For loop with simple condition (MCDC coverage)
    @Test
    public void test_24_forLoopWithSimpleConditionMcdc() {
        String sourceCode = "    public void testMethod(int x) {\n" +
                "        for (int i = 0; x > 0; i++) {\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) afterInitializer;
        Assert.assertEquals("x > 0", conditionNode.getContent());
        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();
        Assert.assertEquals(falseNode, afterBlock);
        Assert.assertEquals("System.out.println(i);\n", trueNode.getContent());
    }

    // Test 25: For loop with AND condition (MCDC coverage)
    @Test
    public void test_25_forLoopWithAndConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        for (int i = 0; x > 0 && y < 10; i++) {\n" +
                "            System.out.println(i);\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        String content = firstConditionNode.getContent();
        Assert.assertTrue(content.contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        CfgNode xFalseNode = firstConditionNode.getFalseNode();

        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        CfgNode yFalseNode = secondConditionNode.getFalseNode();

        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertEquals("System.out.println(i);\n", yTrueNode.getContent());
        CfgNode afterPrintNode = yTrueNode.getAfterNode();
        Assert.assertEquals("x--;\n", afterPrintNode.getContent());
        CfgNode afterXDecrementNode = afterPrintNode.getAfterNode();
        Assert.assertEquals("y++;\n", afterXDecrementNode.getContent());
        CfgNode updateNode = afterXDecrementNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(firstConditionNode, updateNode.getAfterNode());
    }

    // Test 26: For loop with OR condition (MCDC coverage)
    @Test
    public void test_26_forLoopWithOrConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        for (int i = 0; x > 0 || y < 10; i++) {\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        String content = firstConditionNode.getContent();
        Assert.assertTrue(content.contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        CfgNode xFalseNode = firstConditionNode.getFalseNode();

        Assert.assertEquals("System.out.println(\"Looping\");\n", xTrueNode.getContent());
        Assert.assertTrue(xFalseNode instanceof CfgBoolExprNode);
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xFalseNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        CfgNode yFalseNode = secondConditionNode.getFalseNode();

        Assert.assertEquals(xTrueNode, yTrueNode);
        Assert.assertEquals(yFalseNode, afterBlock);
        CfgNode afterPrintNode = yTrueNode.getAfterNode();
        Assert.assertEquals("x--;\n", afterPrintNode.getContent());
        CfgNode afterXDecrementNode = afterPrintNode.getAfterNode();
        Assert.assertEquals("y++;\n", afterXDecrementNode.getContent());
        CfgNode updateNode = afterXDecrementNode.getAfterNode();
        Assert.assertEquals("i++", updateNode.getContent());
        Assert.assertEquals(firstConditionNode, updateNode.getAfterNode());
    }

    // Test 27: For loop with three AND conditions (MCDC coverage)
    @Test
    public void test_27_forLoopWithThreeAndConditionsMcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                "        for (int i = 0; x > 0 && y < 10 && z == 5; i++) {\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        CfgNode xFalseNode = firstConditionNode.getFalseNode();

        Assert.assertEquals(xFalseNode, afterBlock);
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        CfgNode yFalseNode = secondConditionNode.getFalseNode();

        Assert.assertEquals(yFalseNode, afterBlock);
        Assert.assertTrue(yTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode thirdConditionNode = (CfgBoolExprNode) yTrueNode;
        Assert.assertTrue(thirdConditionNode.getContent().contains("z == 5"));

        CfgNode zTrueNode = thirdConditionNode.getTrueNode();
        CfgNode zFalseNode = thirdConditionNode.getFalseNode();

        Assert.assertEquals(zFalseNode, afterBlock);
        Assert.assertEquals("System.out.println(i);\n", zTrueNode.getContent());
        CfgNode afterPrintNode = zTrueNode.getAfterNode();
        Assert.assertEquals("i++", afterPrintNode.getContent());
        Assert.assertEquals(firstConditionNode, afterPrintNode.getAfterNode());

    }

    // Test 28: For loop with complex AND-OR condition (MCDC coverage)
    @Test
    public void test_28_forLoopWithComplexAndOrConditionMcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                "        for (int i = 0; (x > 0 && y < 10) || z == 1; i++) {\n" +
                "            System.out.println(\"Complex condition true\");\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        CfgNode xFalseNode = firstConditionNode.getFalseNode();

        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode yConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(yConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = yConditionNode.getTrueNode();
        CfgNode yFalseNode = yConditionNode.getFalseNode();

        Assert.assertEquals(yFalseNode, xFalseNode);
        Assert.assertEquals("System.out.println(\"Complex condition true\");\n", yTrueNode.getContent());
        Assert.assertTrue(xFalseNode instanceof CfgBoolExprNode);
        CfgBoolExprNode zConditionNode = (CfgBoolExprNode) xFalseNode;
        Assert.assertTrue(zConditionNode.getContent().contains("z == 1"));

        CfgNode zTrueNode = zConditionNode.getTrueNode();
        CfgNode zFalseNode = zConditionNode.getFalseNode();

        Assert.assertEquals(yTrueNode, zTrueNode);
        Assert.assertEquals(zFalseNode, afterBlock);
        CfgNode afterPrintNode = zTrueNode.getAfterNode();
        Assert.assertEquals("i++", afterPrintNode.getContent());
        Assert.assertEquals(firstConditionNode, afterPrintNode.getAfterNode());
    }

    // Test 29: For loop with NOT condition (MCDC coverage)
    @Test
    public void test_29_forLoopWithNotConditionMcdc() {
        String sourceCode = "    public void testMethod(boolean x) {\n" +
                "        for (int i = 0; !x; i++) {\n" +
                "            System.out.println(i);\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) afterInitializer;
        String content = conditionNode.getContent();
        Assert.assertTrue(content.contains("x"));

        CfgNode trueNode = conditionNode.getTrueNode();
        CfgNode falseNode = conditionNode.getFalseNode();

        // With NOT operator, true and false are swapped
        Assert.assertEquals("System.out.println(i);\n", falseNode.getContent());
        Assert.assertEquals(trueNode, afterBlock);
    }

    // Test 30: For loop with NOT AND condition (MCDC coverage)
    @Test
    public void test_30_forLoopWithNotAndConditionMcdc() {
        String sourceCode = "    public void testMethod(int a, int b) {\n" +
                "        for (int i = 0; !(a > 0 && b < 0); i++) {\n" +
                "            System.out.println(i);\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        String content = firstConditionNode.getContent();
        Assert.assertTrue(content.contains("a > 0"));

        CfgNode aTrueNode = firstConditionNode.getTrueNode();
        CfgNode aFalseNode = firstConditionNode.getFalseNode();

        Assert.assertTrue(aTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode bConditionNode = (CfgBoolExprNode) aTrueNode;
        Assert.assertTrue(bConditionNode.getContent().contains("b < 0"));

        CfgNode bTrueNode = bConditionNode.getTrueNode();
        CfgNode bFalseNode = bConditionNode.getFalseNode();

        // With NOT, the logic is inverted
        Assert.assertEquals("System.out.println(i);\n", bFalseNode.getContent());
        Assert.assertEquals("System.out.println(i);\n", aFalseNode.getContent());
    }

    // Test 31: For loop with MCDC and continue statement
    @Test
    public void test_31_forLoopWithMcdcAndContinue() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        for (int i = 0; x > 0 && y < 10; i++) {\n" +
                "            if (i % 2 == 0) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertTrue(yTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode ifConditionNode = (CfgBoolExprNode) yTrueNode;
        Assert.assertEquals("i % 2 == 0", ifConditionNode.getContent());

        CfgNode ifTrueNode = ifConditionNode.getTrueNode();
        CfgNode ifFalseNode = ifConditionNode.getFalseNode();

        Assert.assertTrue(ifTrueNode instanceof CfgContinueStatementNode);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
    }

    // Test 32: For loop with MCDC and break statement
    @Test
    public void test_32_forLoopWithMcdcAndBreak() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        for (int i = 0; x > 0 && y < 10; i++) {\n" +
                "            if (i == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            System.out.println(i);\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode afterBlock = new CfgNode();
        CfgNode beforeBlock = new CfgNode();
        block.setBeforeNode(beforeBlock);
        block.setAfterNode(afterBlock);
        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode firstConditionNode = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(firstConditionNode.getContent().contains("x > 0"));

        CfgNode xTrueNode = firstConditionNode.getTrueNode();
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode secondConditionNode = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(secondConditionNode.getContent().contains("y < 10"));

        CfgNode yTrueNode = secondConditionNode.getTrueNode();
        Assert.assertTrue(yTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode ifConditionNode = (CfgBoolExprNode) yTrueNode;
        Assert.assertEquals("i == 5", ifConditionNode.getContent());

        CfgNode ifTrueNode = ifConditionNode.getTrueNode();
        CfgNode ifFalseNode = ifConditionNode.getFalseNode();

        Assert.assertTrue(ifTrueNode instanceof CfgBreakStatementNode);
        Assert.assertEquals(ifTrueNode.getAfterNode(), afterBlock);
        Assert.assertEquals("System.out.println(i);\n", ifFalseNode.getContent());
    }

    // Test 33: Nested for loops with MCDC coverage
    @Test
    public void test_33_nestedForLoopsWithMcdc() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        for (int i = 0; x > 0 && i < 10; i++) {\n" +
                "            for (int j = 0; y > 0 && j < 5; j++) {\n" +
                "                System.out.println(i + j);\n" +
                "            }\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode outerFirstCondition = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(outerFirstCondition.getContent().contains("x > 0"));

        CfgNode xTrueNode = outerFirstCondition.getTrueNode();
        Assert.assertTrue(xTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode outerSecondCondition = (CfgBoolExprNode) xTrueNode;
        Assert.assertTrue(outerSecondCondition.getContent().contains("i < 10"));

        CfgNode iTrueNode = outerSecondCondition.getTrueNode();
        Assert.assertEquals("int j=0", iTrueNode.getContent());
        CfgNode iFalseNode = outerSecondCondition.getFalseNode();
        Assert.assertEquals(iFalseNode, afterBlock);
        CfgNode innerAfterInitializer = iTrueNode.getAfterNode();
        Assert.assertTrue(innerAfterInitializer instanceof CfgBoolExprNode);
        CfgBoolExprNode innerFirstCondition = (CfgBoolExprNode) innerAfterInitializer;
        Assert.assertTrue(innerFirstCondition.getContent().contains("y > 0"));
        CfgNode yTrueNode = innerFirstCondition.getTrueNode();
        Assert.assertTrue(yTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode innerSecondCondition = (CfgBoolExprNode) yTrueNode;
        Assert.assertTrue(innerSecondCondition.getContent().contains("j < 5"));
        CfgNode jTrueNode = innerSecondCondition.getTrueNode();
        Assert.assertEquals("System.out.println(i + j);\n", jTrueNode.getContent());
        CfgNode jFalseNode = innerSecondCondition.getFalseNode();
        Assert.assertEquals("i++", jFalseNode.getContent());
        Assert.assertEquals(outerFirstCondition, jFalseNode.getAfterNode());
        CfgNode yFalseNode = innerFirstCondition.getFalseNode();
        Assert.assertEquals("i++", yFalseNode.getContent());
        CfgNode afterPrintNode = jTrueNode.getAfterNode();
        Assert.assertEquals("j++", afterPrintNode.getContent());
        Assert.assertEquals(innerFirstCondition, afterPrintNode.getAfterNode());
    }

    // Test 34: For loop inside if with MCDC coverage
    @Test
    public void test_34_forLoopInsideIfWithMcdc() {
        String sourceCode = "    public void testMethod(int x, int y, int z) {\n" +
                "        if (x > 0 && y < 10) {\n" +
                "            for (int i = 0; z > 0 && i < 5; i++) {\n" +
                "                System.out.println(i);\n" +
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
        CfgBoolExprNode ifFirstCondition = (CfgBoolExprNode) cfg;
        Assert.assertTrue(ifFirstCondition.getContent().contains("x > 0"));

        CfgNode ifXTrueNode = ifFirstCondition.getTrueNode();
        Assert.assertTrue(ifXTrueNode instanceof CfgBoolExprNode);
        CfgBoolExprNode ifSecondCondition = (CfgBoolExprNode) ifXTrueNode;
        Assert.assertTrue(ifSecondCondition.getContent().contains("y < 10"));

        CfgNode ifYTrueNode = ifSecondCondition.getTrueNode();
        Assert.assertEquals("int i=0", ifYTrueNode.getContent());
    }

    // Test 35: For loop with four AND conditions (MCDC coverage)
    @Test
    public void test_35_forLoopWithFourAndConditionsMcdc() {
        String sourceCode = "    public void testMethod(int a, int b, int c, int d) {\n" +
                "        for (int i = 0; a > 0 && b < 0 && c == 0 && d != 0; i++) {\n" +
                "            System.out.println(i);\n" +
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

        Assert.assertEquals("int i=0", cfg.getContent());
        CfgNode afterInitializer = cfg.getAfterNode();
        Assert.assertTrue(afterInitializer instanceof CfgBoolExprNode);
        
        CfgBoolExprNode currentCondition = (CfgBoolExprNode) afterInitializer;
        Assert.assertTrue(currentCondition.getContent().contains("a > 0"));
        
        for (int i = 0; i < 3; i++) {
            CfgNode trueNode = currentCondition.getTrueNode();
            Assert.assertTrue(trueNode instanceof CfgBoolExprNode);
            currentCondition = (CfgBoolExprNode) trueNode;
        }
        
        CfgNode finalTrueNode = currentCondition.getTrueNode();
        Assert.assertEquals("System.out.println(i);\n", finalTrueNode.getContent());
    }
    
}
