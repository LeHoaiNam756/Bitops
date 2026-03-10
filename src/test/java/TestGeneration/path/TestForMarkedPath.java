package TestGeneration.path;

import org.junit.Before;
import org.junit.Test;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.path.MarkedPath;
import test.ParserForTest;

public class TestForMarkedPath {
    @Before
    public void setUp() {
        MarkedPath.resetMarkStatements();
        MarkedPath.resetVisitedNodes();
        MarkedPath.resetFullTestSuiteCoveredStatements();
    }

    @Test
    public void test_markedStatementToCfgNode_1() {
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
        CfgNode rootCfgNode = new CfgNode();
        CfgNode finalEndCfgNode = new CfgNode();
        rootCfgNode.setBeginCfgNode(true);
        finalEndCfgNode.setEndCfgNode(true);
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        rootCfgNode.setAfterNode(block);
        block.setBeforeNode(rootCfgNode);
        block.setAfterNode(finalEndCfgNode);
        finalEndCfgNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
//        ParserForTest.printCfgNodePositions(rootCfgNode);
        MarkedPath.markOneStatement("int i=0", false, false, 71);
        MarkedPath.markOneStatement("i < 10", true, false, 82);
        MarkedPath.markOneStatement("i % 2 == 0", true, false, 113);
        MarkedPath.markOneStatement("int j=0;", false, false, 143);
        MarkedPath.markOneStatement("j < i", true, false, 177);
        MarkedPath.markOneStatement("System.out.println(j);", false,
                false, 206);
        MarkedPath.markOneStatement("j++;", false, false, 249);
        MarkedPath.markOneStatement("j < i", false, true, 177);
        MarkedPath.markOneStatement("i++", false, false, 90);
        MarkedPath.markOneStatement("i < 10", false, true, 82);
        MarkedPath.markPathToCfg(rootCfgNode);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraphWithStyles(rootCfgNode);
//        System.out.println(mermaid);
    }

    @Test
    public void test_markedStatementToCfgNode_2() {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        do {\n" +
                "            if (x == 5) {\n" +
                "                break;\n" +
                "            }\n" +
                "            System.out.println(x);\n" +
                "            x--;\n" +
                "            y++;\n" +
                "        } while (x > 0 || y < 10);\n" +
                "    }";
        CfgNode rootCfgNode = new CfgNode();
        CfgNode finalEndCfgNode = new CfgNode();
        rootCfgNode.setBeginCfgNode(true);
        finalEndCfgNode.setEndCfgNode(true);
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        rootCfgNode.setAfterNode(block);
        block.setBeforeNode(rootCfgNode);
        block.setAfterNode(finalEndCfgNode);
        finalEndCfgNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
//        ParserForTest.printCfgNodePositions(rootCfgNode);
        MarkedPath.markOneStatement("x == 5", false, true,  99);
        MarkedPath.markOneStatement("System.out.println(x);", false,
                false,  158);
        MarkedPath.markOneStatement("x--;", false, false,  193);
        MarkedPath.markOneStatement("y++;", false, false,  210);
        MarkedPath.markOneStatement("x > 0", false, true,  232);
        MarkedPath.markOneStatement("y < 10", false, true,  241);
        MarkedPath.markPathToCfg(rootCfgNode);
//        String mermaid = CfgMermaidGenerator.generateMermaidGraphWithStyles(rootCfgNode);
//        System.out.println(mermaid);

    }
}
