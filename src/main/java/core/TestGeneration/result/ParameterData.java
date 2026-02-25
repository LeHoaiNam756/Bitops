package core.TestGeneration.result;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ParameterData {
    private String name;
    private String type;
    private Object value;


    public ParameterData(String name, String type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

