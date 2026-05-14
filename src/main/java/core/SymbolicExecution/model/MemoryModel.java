package core.SymbolicExecution.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryModel {
    // The live state map: versioned name → node
    private final Map<String, SymbolicValue> store = new HashMap<>();

    // Version counters per base name
    private final Map<String, Integer> versions = new HashMap<>();

    /** Read: look up current version of a base name. */
    public Optional<SymbolicValue> read(String baseName) {
        int ver = versions.getOrDefault(baseName, -1);
        if (ver < 0) return Optional.empty();
        return Optional.ofNullable(store.get(versioned(baseName, ver)));
    }

    /** Write: bump version, store node, return the new versioned key. */
    public String write(String baseName, SymbolicValue node) {
        int next = versions.merge(baseName, 0, (old, z) -> old + 1);
        String key = versioned(baseName, next);
        store.put(key, node);
        return key;
    }

    private static String versioned(String base, int v) {
        return base + "_" + v;
    }

    // Snapshot for path fork (branch handling)
    public MemoryModel fork() {
        MemoryModel s = new MemoryModel();
        s.store.putAll(this.store);
        s.versions.putAll(this.versions);
        return s;
    }
}
