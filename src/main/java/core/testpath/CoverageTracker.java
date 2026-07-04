package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.ControlFlowGraph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CoverageTracker implements CoverageTrackerInterface{
    private final Set<Integer> uncovered;
    private final Set<Integer> covered;
    private final Set<Integer> infeasible;
    private final Set<Integer> unknown;
    private final Map<Integer, String> infeasibleReasons;
    private final Map<Integer, String> unknownReasons;

    public CoverageTracker(Set<Integer> nodeIds) {
        this.uncovered = new HashSet<>(nodeIds);
        this.covered   = new HashSet<>();
        this.infeasible = new HashSet<>();
        this.unknown = new HashSet<>();
        this.infeasibleReasons = new HashMap<>();
        this.unknownReasons = new HashMap<>();
    }

    public CoverageTracker(ControlFlowGraph graph) {
        this(graph.getNodes());
    }

    public void markCovered(int node) {
        if (uncovered.remove(node) || infeasible.remove(node) || unknown.remove(node)) {
            infeasibleReasons.remove(node);
            unknownReasons.remove(node);
            covered.add(node);
        }
    }

    /** Legacy compatibility: an unclassified skip is conservatively UNKNOWN. */
    public void markSkipped(int node) {
        markUnknown(node, "legacy-skipped");
    }

    public void markInfeasible(int node, String reason) {
        if (uncovered.remove(node) || unknown.remove(node)) {
            unknownReasons.remove(node);
            infeasible.add(node);
            infeasibleReasons.put(node, normalizeReason(reason, "solver-proved-unsat"));
        }
    }

    public void markUnknown(int node, String reason) {
        if (uncovered.remove(node)) {
            unknown.add(node);
            unknownReasons.put(node, normalizeReason(reason, "unknown"));
        }
    }

    public Set<Integer> getUncovered() {
        return Collections.unmodifiableSet(uncovered);
    }

    public Set<Integer> getCovered() {
        return Collections.unmodifiableSet(covered);
    }

    public Set<Integer> getSkipped() {
        Set<Integer> skipped = new HashSet<>(infeasible);
        skipped.addAll(unknown);
        return Collections.unmodifiableSet(skipped);
    }

    public Set<Integer> getInfeasible() {
        return Collections.unmodifiableSet(infeasible);
    }

    public Set<Integer> getUnknown() {
        return Collections.unmodifiableSet(unknown);
    }

    public Map<Integer, String> getInfeasibleReasons() {
        return Collections.unmodifiableMap(infeasibleReasons);
    }

    public Map<Integer, String> getUnknownReasons() {
        return Collections.unmodifiableMap(unknownReasons);
    }

    public boolean isCovered(int nodeId) { return covered.contains(nodeId); }
    public boolean isSkipped(int nodeId)  { return isInfeasible(nodeId) || isUnknown(nodeId); }
    public boolean isInfeasible(int nodeId) { return infeasible.contains(nodeId); }
    public boolean isUnknown(int nodeId) { return unknown.contains(nodeId); }
    public boolean isUncovered(int nodeId){ return uncovered.contains(nodeId); }

    public int pathTargetFor(int nodeId) { return nodeId; }

    public CfgEdgeKind requiredExitFor(int nodeId) { return null; }

    public boolean isComplete() { return uncovered.isEmpty(); }

    public int totalObligations() {
        return covered.size() + uncovered.size() + infeasible.size() + unknown.size();
    }

    public double rawCoveragePercent() {
        int total = totalObligations();
        return total == 0 ? 100.0 : covered.size() * 100.0 / total;
    }

    public double feasibleCoveragePercent() {
        int feasibleOrUnknown = totalObligations() - infeasible.size();
        return feasibleOrUnknown == 0 ? 100.0 : covered.size() * 100.0 / feasibleOrUnknown;
    }

    private static String normalizeReason(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason;
    }
}
