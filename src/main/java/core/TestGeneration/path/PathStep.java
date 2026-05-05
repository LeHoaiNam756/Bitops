package core.TestGeneration.path;

public class PathStep {
    private final int nodeId;
    private final Boolean decision;

    public PathStep(int nodeId, Boolean decision) {
        this.nodeId = nodeId;
        this.decision = decision;
    }

    public int getNodeId() {
        return nodeId;
    }

    public Boolean getDecision() {
        return decision;
    }
}
