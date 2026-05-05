package core.TestGeneration.result;

import lombok.Getter;
import lombok.Setter;
import java.util.Arrays;
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

    public static String formatValue(Object value) {
        if (value == null) return "null";
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) return Arrays.deepToString((Object[]) value);
            if (value instanceof int[])     return Arrays.toString((int[]) value);
            if (value instanceof long[])    return Arrays.toString((long[]) value);
            if (value instanceof double[])  return Arrays.toString((double[]) value);
            if (value instanceof float[])   return Arrays.toString((float[]) value);
            if (value instanceof boolean[]) return Arrays.toString((boolean[]) value);
            if (value instanceof byte[])    return Arrays.toString((byte[]) value);
            if (value instanceof short[])   return Arrays.toString((short[]) value);
            if (value instanceof char[])    return Arrays.toString((char[]) value);
        }
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return formatValue(value);
    }
}