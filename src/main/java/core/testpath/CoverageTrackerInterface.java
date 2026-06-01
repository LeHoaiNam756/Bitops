package core.testpath;

import core.cfg.CfgEdgeKind;

import java.util.Set;

public interface CoverageTrackerInterface {
    void markCovered(int node);
    public void markSkipped(int node);
    public Set<Integer> getUncovered();
    public Set<Integer> getCovered();
    public Set<Integer> getSkipped();
    public boolean isCovered(int nodeId);
    public boolean isSkipped(int nodeId);
    public boolean isUncovered(int nodeId);
    public int pathTargetFor(int nodeId);
    public CfgEdgeKind requiredExitFor(int nodeId);
    public boolean isComplete();

}
