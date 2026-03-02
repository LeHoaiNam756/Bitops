import OldConcolic.OldConcolicTesting;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.result.TestData;
import core.TestGeneration.result.TestResult;
import core.utils.FilePath;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MainOld {
    public static void main(String[] args) {
        OldConcolicTesting oldConcolicTesting = new OldConcolicTesting();
        int id = 1;
        String filePath = FilePath.JCIA_PROJECT_ROOT_PATH + "\\TestSrc\\getBase64Char.java";
        String className = "getBase64Char.java";
        String methodName = "getBase64Char";
        ASTHelper.Coverage coverage = ASTHelper.Coverage.MCDC;

        TestResult result = oldConcolicTesting.runConcolicTesting(
                id, filePath, className, methodName, coverage
        );

        String outputFileName = "C:\\CIA\\JCIA\\CT4J\\src\\main\\java\\core\\output\\outputOld.txt";

        try (PrintWriter out = new PrintWriter(new FileWriter(outputFileName, true))) {
            out.println("Test Result for " + className + "." + methodName  + " (Coverage: " + coverage + "):");

            List<TestData> fullTestData = result.getFullTestData();

            for (TestData data : fullTestData) {
                System.out.println(data);
                out.println(data);
            }

            String coverageStr = "Coverage: " + result.getCoveragePercent() + "%";
            String memoryStr = "Memory: " + result.getMemoryUsed() + " mb";
            String timeStr = "Generation Time: " + result.getTimeToGenerate() + " ms";

            System.out.println(coverageStr);
            out.println(coverageStr);

            System.out.println(memoryStr);
            out.println(memoryStr);

            System.out.println(timeStr);
            out.println(timeStr);

            out.println("------------------------------------------");

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
