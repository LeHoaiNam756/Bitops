package TestGeneration;

import core.utils.FilePath;
import org.junit.Ignore;
import org.junit.Test;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.ConcolicTesting;
import core.TestGeneration.result.TestData;
import core.TestGeneration.result.TestResult;
import test.ParserForTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestConcolicTesting {
    @Test
    public void test_setAllCfgNode_1() {
        String sourceCode = """
                    public void testMethod() {
                        for (int i = 0; i < 10; i++) {
                            if (i % 2 == 0) {
                                int j = 0;
                                while (j < i) {
                                    System.out.println(j);
                                    j++;
                                }
                            } else {
                                System.out.println("Odd");
                            }
                        }
                    }\
                """;
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
    }

    @Test
    public void test_setAllCfgNode_2() {
        String sourceCode = """
                    public void testMethod(int x, int y) {
                        do {
                            do {
                                System.out.println(x + y);
                                y++;
                            } while (y < 5 && x > 0);
                            x--;
                        } while (x > 0 && y < 10);
                    }\
                """;
        CfgNode block = ParserForTest.generateBlockFromSource(sourceCode);
        CfgNode beginNode = new CfgNode();
        beginNode.setBeginCfgNode(true);
        CfgNode endNode = new CfgNode();
        endNode.setEndCfgNode(true);
        beginNode.setAfterNode(block);
        block.setBeforeNode(beginNode);
        block.setAfterNode(endNode);
        endNode.setBeforeNode(block);
        ASTHelper.generateCfg(block, null, ASTHelper.Coverage.MCDC);
        ConcolicTesting concolicTesting = new ConcolicTesting();
        Set<CfgNode> totalCfgNodes = new HashSet<>();
        concolicTesting.traverseAndSetTotalCfgNodes(beginNode, totalCfgNodes);
        assertEquals(9, totalCfgNodes.size());
    }

    @Test
    public void test_runFullConcolic_Utf8Validator_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH + "\\TestSrc\\Utf8Validator.java";
        String className = "Utf8Validator.java";
        String methodName = "feed";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
        assertEquals(7, result.getFullTestDataSet().size());
    }

    @Test
    public void test_runFullConcolic_Utf8Validator_BRANCH() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Utf8Validator.java";
        String className = "Utf8Validator.java";
        String methodName = "feed";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
        assertEquals(7, result.getFullTestDataSet().size());
    }

    @Test
    @Ignore
    public void test_runFullConcolic_GameMechanics_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\GameMechanics.java";
        String className = "GameMechanics.java";
        String methodName = "simulateBattleLog";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        System.out.println("Coverage: " + result.getCoveragePercent() + "%");
    }

    @Test
    public void test_runFullConcolic_LogicFlow_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\LogicFlow.java";
        String className = "LogicFlow.java";
        String methodName = "processValue";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_LogicFlow_BRANCH() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\LogicFlow.java";
        String className = "LogicFlow.java";
        String methodName = "processValue";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_LogicFlow_MCDC() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\LogicFlow.java";
        String className = "LogicFlow.java";
        String methodName = "processValue";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.MCDC;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_Independence_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Independence.java";
        String className = "Independence.java";
        String methodName = "independentBranches";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_Independence_BRANCH() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Independence.java";
        String className = "Independence.java";
        String methodName = "independentBranches";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_Independence_MCDC() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Independence.java";
        String className = "Independence.java";
        String methodName = "independentBranches";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.MCDC;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolic_Independence_Loop_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Independence.java";
        String className = "Independence.java";
        String methodName = "loop";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
    public void test_runFullConcolicWithStub_1() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\StubOne.java";
        String className = "StubOne.java.java";
        String methodName = "getValue";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        Set<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    public void test_runFullConcolic_Independence_Loop_BRANCH() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\Independence.java";
        String className = "Independence.java";
        String methodName = "loop";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        List<TestData> fullTestData = (List<TestData>) result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }

    @Test
     public void test_runFullConcolic_AccountBalance_STATEMENT() {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\AccountBalance.java";
        String className = "AccountBalance.java";
        String methodName = "processTransaction";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.STATEMENT;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        List<TestData> fullTestData = (List<TestData>) result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
     }

    @Test
    public void test_runFullConcolic_AccountBalance_BRANCH() {
        ConcolicTesting concolicTesting = new
                ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\AccountBalance.java";
        String className = "AccountBalance.java";
        String methodName = "processTransaction";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        List<TestData> fullTestData = (List<TestData>) result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        assertEquals(100.0, result.getCoveragePercent(), 0.01);
    }
}
