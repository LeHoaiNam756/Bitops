package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.ControlFlowGraph;

import java.util.List;

public interface PathFinder {
    List<List<ControlFlowGraph.Edge>> findPath(ControlFlowGraph cfg, int target);

    default List<List<ControlFlowGraph.Edge>> findPath(
            ControlFlowGraph cfg,
            int target,
            CfgEdgeKind requiredExit) {
        return findPath(cfg, target);
    }
}
