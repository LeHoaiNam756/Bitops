package core.testpath;

import core.cfg.CfgEdgeKind;

import java.util.Set;

public interface CoverageTrackerInterface {
    void markCovered(int node);
    void markSkipped(int node);
    void markInfeasible(int node, String reason);
    void markUnknown(int node, String reason);
    Set<Integer> getUncovered();
    Set<Integer> getCovered();
    Set<Integer> getSkipped();
    Set<Integer> getInfeasible();
    Set<Integer> getUnknown();
    boolean isCovered(int nodeId);
    boolean isSkipped(int nodeId);
    boolean isInfeasible(int nodeId);
    boolean isUnknown(int nodeId);
    boolean isUncovered(int nodeId);
    int pathTargetFor(int nodeId);
    CfgEdgeKind requiredExitFor(int nodeId);
    boolean isComplete();

}
