package core.TestGeneration.result;

import lombok.Getter;
import core.TestGeneration.path.MarkedStatement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public final class RamStorage {
    @Getter
    private final static List<Object> outputs = new LinkedList<>();
    @Getter
    private final static Set<MarkedStatement> coveredStatements = new HashSet<>();

    private RamStorage() {
    }

    public static void reset() {
        outputs.clear();
        coveredStatements.clear();
    }
}
