package core.SymbolicExecution.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SymbolicState {
    private final MemoryModel memoryModel;
    private final TypeContext typeContext;

    public SymbolicState fork() {
        return SymbolicState
                .builder()
                .memoryModel(this.memoryModel.fork())
                .typeContext(new TypeContext())
                .build();
    }
}

