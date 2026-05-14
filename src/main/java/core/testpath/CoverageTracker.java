package core.testpath;

import core.cfg.ControlFlowGraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CoverageTracker {
    private final Set<Integer> uncovered;  // not yet covered or skipped
    private final Set<Integer> covered;
    private final Set<Integer> skipped;

    public CoverageTracker(Set<Integer> nodeIds) {
        this.uncovered = new HashSet<>(nodeIds);
        this.covered   = new HashSet<>();
        this.skipped   = new HashSet<>();
    }

    public CoverageTracker(ControlFlowGraph graph) {
        this(graph.getNodes());
    }

    public void markCovered(int node) {
        if (uncovered.remove(node) || skipped.remove(node)) {
            covered.add(node);
        }
    }

    public void markSkipped(int node) {
        if (uncovered.remove(node)) {
            skipped.add(node);
        }
    }

    public Set<Integer> getUncovered() {
        return Collections.unmodifiableSet(uncovered);
    }

    public Set<Integer> getCovered() {
        return Collections.unmodifiableSet(covered);
    }

    public Set<Integer> getSkipped() {
        return Collections.unmodifiableSet(skipped);
    }

    public boolean isCovered(int nodeId) { return covered.contains(nodeId); }
    public boolean isSkipped(int nodeId)  { return skipped.contains(nodeId); }
    public boolean isUncovered(int nodeId){ return uncovered.contains(nodeId); }

    public boolean isComplete() { return uncovered.isEmpty(); }
}
