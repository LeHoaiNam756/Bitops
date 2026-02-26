import core.CFG.Utils.ASTHelper;
import core.TestGeneration.ConcolicTesting;
import core.TestGeneration.result.TestData;
import core.TestGeneration.result.TestResult;
import core.utils.FilePath;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        ConcolicTesting concolicTesting = new ConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH +  "\\TestSrc\\AccountBalance.java";
        String className = "AccountBalance.java";
        String methodName = "processTransaction";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.BRANCH;
        TestResult result = concolicTesting.runConcolicTesting(
                id, filePath, className, methodName,  coverage
        );
        List<TestData> fullTestData = result.getFullTestData();
        for (TestData data : fullTestData) {
            System.out.println(data);
        }
        System.out.println("Coverage: " + result.getCoveragePercent() + "%");
        System.out.println("Memory: " + result.getMemoryUsed() + " mb");
        System.out.println("Generation Time: " + result.getTimeToGenerate() + " ms");
    }
}
