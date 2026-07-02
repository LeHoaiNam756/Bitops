package core.SymbolicExecution.model;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class SymbolicState {
    private final MemoryModel memoryModel;
    private final TypeContext typeContext;

    @Builder.Default
    private final Map<SymbolicValue, Integer> knownArrayLengths = new HashMap<>();

    @Builder.Default
    private final List<SymbolicValue> assumptions = new ArrayList<>();

    public void rememberArrayLength(SymbolicValue array, int length) {
        knownArrayLengths.put(array, length);
    }

    public Integer knownArrayLength(SymbolicValue array) {
        return knownArrayLengths.get(array);
    }

    public void assume(SymbolicValue condition) {
        assumptions.add(condition);
    }

    public SymbolicState fork() {
        return SymbolicState
                .builder()
                .memoryModel(this.memoryModel.fork())
                .typeContext(new TypeContext())
                .knownArrayLengths(new HashMap<>(this.knownArrayLengths))
                .assumptions(new ArrayList<>(this.assumptions))
                .build();
    }
}
