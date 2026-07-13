package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;
import org.eclipse.jdt.core.dom.BooleanLiteral;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BranchCoverageTracker extends CoverageTracker {
    public BranchCoverageTracker(ControlFlowGraph cfg) {
        super(branchOutcomeIds(cfg));
    }

    private static Set<Integer> branchOutcomeIds(ControlFlowGraph cfg) {
        return cfg.getNodes().stream()
            .filter(nodeId -> (cfg.getNode(nodeId).getKind() == CfgNodeKind.BRANCH
                    || cfg.getNode(nodeId).getKind() == CfgNodeKind.LOOP))
            // Synthetic control-flow nodes (for example the implicit "true"
            // condition in for (;;)) have no source AST and cannot be probed.
            .filter(nodeId -> cfg.getNode(nodeId).getAst() != null)
            // Literal true/false conditions must remain compile-time constants
            // (for example while (true)); instrumenting them can change javac's
            // reachability analysis and introduce missing-return errors.
            .filter(nodeId -> !(cfg.getNode(nodeId).getAst() instanceof BooleanLiteral))
            .flatMap(nodeId -> Stream.of(nodeId * 2, nodeId * 2 + 1))
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public int pathTargetFor(int branchOutcomeId) {
        return branchOutcomeId / 2;
    }

    @Override
    public CfgEdgeKind requiredExitFor(int branchOutcomeId) {
        return branchOutcomeId % 2 == 0 ? CfgEdgeKind.TRUE : CfgEdgeKind.FALSE;
    }
}
