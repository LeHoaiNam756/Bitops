package core.TestGeneration.result;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Setter
@Getter
public class TestResult {
    private int id = 0;
    private Set<TestData> fullTestData = new HashSet<>();
    private double coveragePercent = 0;
    private double timeToGenerate = 0;
    private double memoryUsed = 0;

    public void addToFullTestData(TestData testData) {
        fullTestData.add(testData);
    }

    public void setCoveragePercent(double coveragePercent) {
        this.coveragePercent = (double) Math.round(coveragePercent * 100) / 100;
    }

    public List<List<Object>> getFullTestDataSet() {
        List<List<Object>> result = new ArrayList<>();
        for (TestData testData : fullTestData) {
            result.add(testData.getTestDataSet());
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(fullTestData);
        result.append("Full coverage: ").append(coveragePercent).append("\n");
        return result.toString();
    }
}

