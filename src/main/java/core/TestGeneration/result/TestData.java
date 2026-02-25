package core.TestGeneration.result;

import lombok.Getter;
import lombok.Setter;
import core.TestGeneration.path.MarkedStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
public class TestData {
    private Set<MarkedStatement> markedStatements = new HashSet<>();
    private List<ParameterData> parameterDataList = new ArrayList<>();
    private Object output;
    private double unitCoverage;
    private String status;

    public TestData(List<String> names, Class<?>[] types, Object[] values, Set<MarkedStatement> markedStatements,
                    Object output, double unitCoverage) {
        if(names.size() != types.length || types.length != values.length) {
            throw new RuntimeException("Invalid");
        }

        for(int i = 0; i < names.size(); i++) {
            this.addToParameterDataList(new ParameterData(names.get(i), types[i].toString(), values[i]));
        }

        this.markedStatements = markedStatements;
        this.output = output;
        this.unitCoverage = round(unitCoverage);
        this.status = "PASS";
    }

    private double round(double number) {
        return (double) Math.round(number * 100) / 100;
    }

    public void addToCoveredStatements(MarkedStatement statement) {
        markedStatements.add(statement);
    }

    public void addToParameterDataList(ParameterData parameterData) {
        parameterDataList.add(parameterData);
    }

    public List<Object> getTestDataSet() {
        List<Object> result = new ArrayList<>();
        for (ParameterData parameterData : parameterDataList) {
            result.add(parameterData.getValue());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Parameters: ");
        for (ParameterData parameterData : parameterDataList) {
            result.append(parameterData.toString()).append("; ");
        }
        result.append(" | Output: ").append(output);
        result.append(" | Coverage: ").append(unitCoverage);
        return result.toString();
    }
}

