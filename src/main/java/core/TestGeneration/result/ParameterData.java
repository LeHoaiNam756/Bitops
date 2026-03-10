package core.TestGeneration.result;

import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ParameterData that = (ParameterData) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}