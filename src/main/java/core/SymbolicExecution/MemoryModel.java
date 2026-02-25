package core.SymbolicExecution;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.Variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class MemoryModel {
    private final HashMap<Variable, AstNode> S = new HashMap<>();

    public void clear() {
        S.clear();
    }

    public int size() {
        return S.size();
    }

    public AstNode accessVariable(String name) {
        for (Map.Entry<Variable, AstNode> entry : S.entrySet()) {
            if (entry.getKey().getName().equals(name)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public void declareVariable(Variable variable, AstNode node) {
        S.put(variable, node);
    }

    public void assignVariable(String variable, AstNode node) {
        for (Map.Entry<Variable, AstNode> entry : S.entrySet()) {
            if (entry.getKey().getName().equals(variable)) {
                entry.setValue(node);
                return;
            }
        }
        throw new RuntimeException("Variable " + variable + " not declared.");
    }

    public boolean containsVariable(String name) {
        for (Map.Entry<Variable, AstNode> entry : S.entrySet()) {
            if (entry.getKey().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Variable getVariable(String name) {
        for (Map.Entry<Variable, AstNode> entry : S.entrySet()) {
            if (entry.getKey().getName().equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
