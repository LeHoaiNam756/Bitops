package TestGeneration.path;

import org.junit.Test;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.ConcolicTesting;
import core.TestGeneration.path.FindPath;
import test.ParserForTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TestFindPath {
    @Test
    public void test_getUncoveredNode_1() {
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
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        ConcolicTesting concolicTesting = new ConcolicTesting();
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        concolicTesting.traverseAndSetTotalCfgNodes(beginNode, totalCfgNodes);
        assertEquals(11, totalCfgNodes.size());
        
        Set<CfgNode> coveredNodes = new HashSet<>();
        CfgNode nodeIequalsZero = beginNode.getAfterNode();
        CfgNode nodeIls10 = nodeIequalsZero.getAfterNode();
        coveredNodes.add(nodeIequalsZero);
        coveredNodes.add(nodeIls10);
        
        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, coveredNodes);
        assertTrue(totalCfgNodes.contains(uncoveredNode));
        assertNotNull("Should find an uncovered node", uncoveredNode);
        assertFalse("Uncovered node should not be in covered set", coveredNodes.contains(uncoveredNode));
        assertFalse("Uncovered node should not be begin node", uncoveredNode.isBeginCfgNode());
        assertFalse("Uncovered node should not be end node", uncoveredNode.isEndCfgNode());
    }

    @Test
    public void test_getUncoveredNode_AllNodesCovered() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int x = 5;\n" +
                "        int y = 10;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        
        ConcolicTesting concolicTesting = new ConcolicTesting();
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        concolicTesting.traverseAndSetTotalCfgNodes(beginNode, totalCfgNodes);
        
        Set<CfgNode> coveredNodes = new HashSet<>();
        for (CfgNode node : totalCfgNodes) {
            if (!node.isBeginCfgNode() && !node.isEndCfgNode()) {
                coveredNodes.add(node);
            }
        }
        
        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, coveredNodes);
        assertNull("Should return null when all nodes are covered", uncoveredNode);
    }

    @Test
    public void test_getUncoveredNode_NullInputs() {
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        Set<CfgNode> coveredNodes = new HashSet<>();
        
        CfgNode result1 = FindPath.getUncoveredNode(null, coveredNodes);
        assertNull("Should return null when totalCfgNodes is null", result1);
        
        CfgNode result2 = FindPath.getUncoveredNode(totalCfgNodes, null);
        assertNull("Should return null when coveredNodes is null", result2);
        
        CfgNode result3 = FindPath.getUncoveredNode(null, null);
        assertNull("Should return null when both inputs are null", result3);
    }

    @Test
    public void test_getUncoveredNode_EmptySets() {
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        Set<CfgNode> coveredNodes = new HashSet<>();
        
        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, coveredNodes);
        assertNull("Should return null when totalCfgNodes is empty", uncoveredNode);
    }

    @Test
    public void test_getUncoveredNode_FiltersBeginEndNodes() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int x = 5;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        
        ConcolicTesting concolicTesting = new ConcolicTesting();
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        concolicTesting.traverseAndSetTotalCfgNodes(beginNode, totalCfgNodes);
        
        assertTrue("Begin node should be in total set", totalCfgNodes.contains(beginNode));
        assertTrue("End node should be in total set", totalCfgNodes.contains(endNode));
        
        Set<CfgNode> coveredNodes = new HashSet<>();
        
        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, coveredNodes);
        assertNotNull("Should find an uncovered node", uncoveredNode);
        assertFalse("Uncovered node should not be begin node", uncoveredNode.isBeginCfgNode());
        assertFalse("Uncovered node should not be end node", uncoveredNode.isEndCfgNode());
    }

    @Test
    public void test_getUncoveredNode_SimpleIfStatement() {
        String sourceCode = "    public void testMethod() {\n" +
                "        int x = 5;\n" +
                "        if (x > 0) {\n" +
                "            x++;\n" +
                "        } else {\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        
        ConcolicTesting concolicTesting = new ConcolicTesting();
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        concolicTesting.traverseAndSetTotalCfgNodes(beginNode, totalCfgNodes);
        
        Set<CfgNode> coveredNodes = new HashSet<>();
        CfgNode firstNode = beginNode.getAfterNode();
        if (firstNode != null && !firstNode.isBeginCfgNode() && !firstNode.isEndCfgNode()) {
            coveredNodes.add(firstNode);
        }
        
        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, coveredNodes);
        assertNotNull("Should find an uncovered node", uncoveredNode);
        assertFalse("Uncovered node should not be in covered set", coveredNodes.contains(uncoveredNode));
        assertFalse("Uncovered node should not be begin node", uncoveredNode.isBeginCfgNode());
        assertFalse("Uncovered node should not be end node", uncoveredNode.isEndCfgNode());
    }

    @Test
    public void test_findPath_1 () {
        String sourceCode = "    public void testMethod() {\n" +
                "        int x = 5;\n" +
                "        if (x > 0) {\n" +
                "            x++;\n" +
                "        } else {\n" +
                "            x--;\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode intXequals5 = beginNode.getAfterNode();
        CfgNode ifXgreaterThanZero = intXequals5.getAfterNode();
        CfgBoolExprNode xGreaterThanZeroNode = (CfgBoolExprNode) ifXgreaterThanZero;
        CfgNode xIncrementNode = xGreaterThanZeroNode.getTrueNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, xIncrementNode, endNode);
//        assertNotNull("Should find an uncovered node", path);
//        CfgNode pathBeginNode = path.get(0);
//        CfgNode pathIntXEquals5Node = path.get(1);
//        CfgBoolExprNode pathIfNode = (CfgBoolExprNode) path.get(2);
//        CfgNode pathXIncrementNode = path.get(3);
//        CfgNode pathEndNode = path.get(4);
//        assertEquals("int x=5;", pathIntXEquals5Node.getContent().trim());
//        assertEquals("x > 0", pathIfNode.getContent().trim());
//        assertEquals("x++;", pathXIncrementNode.getContent().trim());
    }

//    @Test
    public void test_findPath_2 () {
        String sourceCode = "    public void testMethod(int x, int y) {\n" +
                "        if (x > 0) {\n" +
                "            while (y > 0) {\n" +
                "                System.out.println(y);\n" +
                "                y--;\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode xGreaterThanZeroNode = beginNode.getAfterNode();
        CfgBoolExprNode ifXGreaterThanZeroNode = (CfgBoolExprNode) xGreaterThanZeroNode;
        CfgNode whileYGreaterThanZeroNode = ifXGreaterThanZeroNode.getTrueNode();
        CfgBoolExprNode whileYGreaterThanZeroBoolNode = (CfgBoolExprNode) whileYGreaterThanZeroNode;
        CfgNode printYNode = whileYGreaterThanZeroBoolNode.getTrueNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, printYNode, endNode);
//        assertNotNull("Should find an uncovered node", path);
    }

//    @Test
    public void test_findPath_Utf8Validator() {
        String sourceCode = "public static int feed(int n, int count) {\n" +
                "        n = n & 255;\n" +
                "\n" +
                "        if (count == 0) {\n" +
                "            if ((n >> 5) == 6) {\n" +
                "                return 1;\n" +
                "            } else if ((n >> 4) == 14) {\n" +
                "                return 2;\n" +
                "            } else if ((n >> 3) == 30) {\n" +
                "                return 3;\n" +
                "            } else if ((n >> 7) == 1) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "        } else {\n" +
                "            if ((n >> 6) != 2) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "            return count - 1;\n" +
                "        }\n" +
                "\n" +
                "        return count;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode nAndNNode = beginNode.getAfterNode();
        CfgNode countEqualsZeroNode = nAndNNode.getAfterNode();
        CfgBoolExprNode countEqualsZeroBoolNode = (CfgBoolExprNode) countEqualsZeroNode;
        CfgNode nRightShift5Equals6Node = countEqualsZeroBoolNode.getTrueNode();
        CfgBoolExprNode nRightShift5Equals6BoolNode = (CfgBoolExprNode) nRightShift5Equals6Node;
        CfgNode nRightShift4Equals14Node = nRightShift5Equals6BoolNode.getFalseNode();
        CfgNode return2Node = ((CfgBoolExprNode) nRightShift4Equals14Node).getTrueNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, return2Node, endNode);
//        assertNotNull("Should find an path", path);
//        CfgNode pathBeginNode = path.get(0);
//        CfgNode pathNAndNNode = path.get(1);
//        CfgNode pathCountEqualsZeroNode = path.get(2);
//        CfgNode pathNRightShift5Equals6Node = path.get(3);
//        CfgNode pathNRightShift4Equals14Node = path.get(4);
//        CfgNode pathReturn2Node = path.get(5);
//        CfgNode pathEndNode = path.get(6);
//        assertEquals("n=n & 255;", pathNAndNNode.getContent().trim());
//        assertEquals("count == 0", pathCountEqualsZeroNode.getContent().trim());
//        assertEquals("(n >> 5) == 6", pathNRightShift5Equals6Node.getContent().trim());
//        assertEquals("(n >> 4) == 14", pathNRightShift4Equals14Node.getContent().trim());
//        assertEquals("return 2;", pathReturn2Node.getContent().trim());
    }

    @Test
    public void test_findPath_Utf8Validator_2() {
        String sourceCode = "public static int feed(int n, int count) {\n" +
                "        n = n & 255;\n" +
                "\n" +
                "        if (count == 0) {\n" +
                "            if ((n >> 5) == 6) {\n" +
                "                return 1;\n" +
                "            } else if ((n >> 4) == 14) {\n" +
                "                return 2;\n" +
                "            } else if ((n >> 3) == 30) {\n" +
                "                return 3;\n" +
                "            } else if ((n >> 7) == 1) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "        } else {\n" +
                "            if ((n >> 6) != 2) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "            return count - 1;\n" +
                "        }\n" +
                "\n" +
                "        return count;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode nAndNNode = beginNode.getAfterNode();
        CfgNode countEqualsZeroNode = nAndNNode.getAfterNode();
        CfgBoolExprNode countEqualsZeroBoolNode = (CfgBoolExprNode) countEqualsZeroNode;
        CfgBoolExprNode nRightShift6NotEquals2Node = (CfgBoolExprNode) countEqualsZeroBoolNode.getFalseNode();
        CfgNode returnMinus1Node = nRightShift6NotEquals2Node.getTrueNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, returnMinus1Node, endNode);
//        assertNotNull("Should find an path", path);
//        CfgNode pathBeginNode = path.get(0);
//        CfgNode pathNAndNNode = path.get(1);
//        CfgNode pathCountEqualsZeroNode = path.get(2);
//        CfgNode pathNRightShift6NotEquals2 = path.get(3);
//        CfgNode pathReturnMinus1 = path.get(4);
//        CfgNode pathEndNode = path.get(5);
//        assertEquals("n=n & 255;", pathNAndNNode.getContent().trim());
//        assertEquals("count == 0", pathCountEqualsZeroNode.getContent().trim());
//        assertFalse(((CfgBoolExprNode) pathCountEqualsZeroNode).isPathConstraintTrue());
//        assertEquals("(n >> 6) != 2", pathNRightShift6NotEquals2.getContent().trim());
//        assertTrue(((CfgBoolExprNode) pathNRightShift6NotEquals2).isPathConstraintTrue());
//        assertEquals("return -1;", pathReturnMinus1.getContent().trim());
    }

    @Test
    public void test_findPath_Utf8Validator_3() {
        String sourceCode = "public static int feed(int n, int count) {\n" +
                "        n = n & 255;\n" +
                "\n" +
                "        if (count == 0) {\n" +
                "            if ((n >> 5) == 6) {\n" +
                "                return 1;\n" +
                "            } else if ((n >> 4) == 14) {\n" +
                "                return 2;\n" +
                "            } else if ((n >> 3) == 30) {\n" +
                "                return 3;\n" +
                "            } else if ((n >> 7) == 1) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "        } else {\n" +
                "            if ((n >> 6) != 2) {\n" +
                "                return -1;\n" +
                "            }\n" +
                "            return count - 1;\n" +
                "        }\n" +
                "\n" +
                "        return count;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode nAndNNode = beginNode.getAfterNode();
        CfgNode countEqualsZeroNode = nAndNNode.getAfterNode();
        CfgBoolExprNode countEqualsZeroBoolNode = (CfgBoolExprNode) countEqualsZeroNode;
        CfgBoolExprNode nRightShift5Equals6Node = (CfgBoolExprNode) countEqualsZeroBoolNode.getTrueNode();
        CfgBoolExprNode nRightShift4Equals14Node = (CfgBoolExprNode) nRightShift5Equals6Node.getFalseNode();
        CfgBoolExprNode nRightShift3Equals30Node = (CfgBoolExprNode) nRightShift4Equals14Node.getFalseNode();
        CfgBoolExprNode nRightShift7Equals1Node = (CfgBoolExprNode) nRightShift3Equals30Node.getFalseNode();
        CfgNode returnMinus1Node = nRightShift7Equals1Node.getTrueNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, returnMinus1Node, endNode);
//        assertNotNull("Should find an path", path);
//        CfgNode pathBeginNode = path.get(0);
//        CfgNode pathNAndNNode = path.get(1);
//        CfgNode pathCountEqualsZeroNode = path.get(2);
//        CfgNode pathNRightShift5Equals6Node = path.get(3);
//        CfgNode pathNRightShift4Equals14Node = path.get(4);
//        CfgNode pathNRightShift3Equals30Node = path.get(5);
//        CfgNode pathNRightShift7Equals1Node = path.get(6);
//        CfgNode pathReturnMinus1Node = path.get(7);
//        CfgNode pathEndNode = path.get(5);
//        assertEquals("n=n & 255;", pathNAndNNode.getContent().trim());
//        assertEquals("count == 0", pathCountEqualsZeroNode.getContent().trim());
//        assertTrue(((CfgBoolExprNode) pathCountEqualsZeroNode).isPathConstraintTrue());
//        assertEquals("(n >> 5) == 6", pathNRightShift5Equals6Node.getContent().trim());
//        assertFalse(((CfgBoolExprNode) pathNRightShift5Equals6Node).isPathConstraintTrue());
//        assertEquals("(n >> 4) == 14", pathNRightShift4Equals14Node.getContent().trim());
//        assertFalse(((CfgBoolExprNode) pathNRightShift4Equals14Node).isPathConstraintTrue());
//        assertEquals("(n >> 3) == 30", pathNRightShift3Equals30Node.getContent().trim());
//        assertFalse(((CfgBoolExprNode) pathNRightShift3Equals30Node).isPathConstraintTrue());
//        assertEquals("(n >> 7) == 1", pathNRightShift7Equals1Node.getContent().trim());
//        assertTrue(((CfgBoolExprNode) pathNRightShift7Equals1Node).isPathConstraintTrue());
//        assertEquals("return -1;", pathReturnMinus1Node.getContent().trim());

    }

    @Test
    public void test_findPath_LogicFlow() {
        String sourceCode = "public int processValue(int input, int threshold, boolean scaleUp) {\n" +
                "        int result = 0;\n" +
                "\n" +
                "        if (input < 0) {\n" +
                "            result = -1;\n" +
                "        } else if (input == 0) {\n" +
                "            result = 0;\n" +
                "        } else if (input > threshold) {\n" +
                "            int temp = input;\n" +
                "            while (temp > threshold) {\n" +
                "                temp -= 2;\n" +
                "                result++;\n" +
                "            }\n" +
                "        } else {\n" +
                "            for (int i = 0; i < input; i++) {\n" +
                "                result += i;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        int counter = 0;\n" +
                "        do {\n" +
                "            if (scaleUp) {\n" +
                "                result += 10;\n" +
                "            } else {\n" +
                "                result -= 5;\n" +
                "            }\n" +
                "            counter++;\n" +
                "        } while (counter < 3);\n" +
                "\n" +
                "        return result;\n" +
                "    }";
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        CfgNode resultEqualsZeroNode = beginNode.getAfterNode();
        CfgBoolExprNode inputLessThanZeroNode = (CfgBoolExprNode) resultEqualsZeroNode.getAfterNode();
        CfgBoolExprNode inputEqualsZeroNode = (CfgBoolExprNode) inputLessThanZeroNode.getFalseNode();
//        List<CfgNode> path = FindPath.findPathThrough(beginNode, inputEqualsZeroNode, endNode);
//        assertNotNull("Should find an uncovered node", path);
    }
}
