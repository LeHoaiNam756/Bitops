package core.testpath;

import core.cfg.ControlFlowGraph;

import java.util.List;

public interface PathFinder {
    List<ControlFlowGraph.Edge> findPath(ControlFlowGraph cfg, int target);
}
