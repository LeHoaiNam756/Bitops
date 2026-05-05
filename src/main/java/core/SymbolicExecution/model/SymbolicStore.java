package core.SymbolicExecution.model;

import java.util.HashMap;
import java.util.Map;

public class SymbolicStore {
    private final Map<String, SymbolicValue> values = new HashMap<>();

    public void declare(String name, SymbolicValue value) {
        if (values.containsKey(name)) {
            throw new IllegalStateException("Variable already declared: " + name);
        }
        values.put(name, value);
    }

    public void assign(String name, SymbolicValue value) {
        if (!values.containsKey(name)) {
            throw new IllegalStateException("Variable not declared: " + name);
        }
        values.put(name, value);
    }

    public SymbolicValue resolve(String name) {
        return values.get(name);
    }

    public boolean contains(String name) {
        return values.containsKey(name);
    }

    public int size() {
        return values.size();
    }
}
