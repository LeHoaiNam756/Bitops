package core.instrument;

import core.cfg.ControlFlowGraph;
import core.cfg.CfgNodeKind;
import core.cfg.Coverage;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless service that walks a {@link ControlFlowGraph} and its originating
 * {@link CompilationUnit} to produce an {@link InstrumentationPlan}.
 *
 * <h3>Mapping strategy</h3>
 * <p>The planner matches CFG nodes to AST nodes by <em>source position</em>
 * (start offset + length), which is stable across the two structures as long as
 * they were built from the same {@link CompilationUnit} instance (guaranteed by
 * the pipeline: {@code JavaProjectParser → CfgBuilder → InstrumentationPlanner}).
 *
 * <ul>
 *   <li>CFG nodes whose {@code ASTNode} is {@code null} or whose kind is
 *       {@link CfgNodeKind#ENTRY}/{@link CfgNodeKind#EXIT} are skipped
 *       (marked untrackable).</li>
 *   <li>Branch / loop-condition CFG nodes emit two {@link TracePoint}s:
 *       one for {@link TraceKind#COND_T} and one for {@link TraceKind#COND_F},
 *       using consecutive even/odd IDs derived from the CFG node ID.</li>
 * </ul>
 *
 * <h3>Single responsibility</h3>
 * <p>This class performs planning only — no source text generation, no file I/O.
 */
public final class InstrumentationPlanner {

    /**
     * Build an {@link InstrumentationPlan} for {@code cu} using the node IDs
     * already assigned in {@code cfg}.
     *
     * @param cu       compilation unit (must be the same instance used to build
     *                 {@code cfg})
     * @param cfg      control-flow graph for the methods in {@code cu}
     * @param coverage coverage type (affects whether branch pairs are emitted)
     * @return immutable plan ready to hand to {@link SourceEmitter} and
     *         {@code CoverageTracker}
     */
    public InstrumentationPlan plan(CompilationUnit cu,
                                    ControlFlowGraph cfg,
                                    Coverage coverage) {

        // Index every AST node by (startPosition, length) so we can look them
        // up from CFG node metadata in O(1).
        Map<Long, ASTNode> astIndex = buildAstIndex(cu);

        List<TracePoint> points = new ArrayList<>();

        for (int nodeId : cfg.getNodes()) {
            ControlFlowGraph.Node cfgNode = cfg.getNode(nodeId);
            if (cfgNode == null) continue;

            CfgNodeKind kind = cfgNode.getKind();

            // Skip structural sentinels — they have no executable source.
            if (kind == CfgNodeKind.ENTRY || kind == CfgNodeKind.EXIT) continue;

            ASTNode ast = cfgNode.getAst();
            if (ast == null) continue;

            if (isBranchNode(kind) && !isBooleanLiteral(ast)) {
                // A condition node produces two probe points that share the same
                // AST node but have distinct TraceKind values.  The IDs used in
                // TraceRecorder.mark() calls are:
                //   true-branch  → nodeId * 2
                //   false-branch → nodeId * 2 + 1
                // This scheme keeps IDs unique and lets CoverageTracker track
                // each branch outcome independently.
                int trueId  = nodeId * 2;
                int falseId = nodeId * 2 + 1;
                points.add(new TracePoint(trueId,  ast, TraceKind.COND_T));
                points.add(new TracePoint(falseId, ast, TraceKind.COND_F));
            }

            if (!isBranchNode(kind) && coverage == Coverage.STATEMENT) {
                points.add(new TracePoint(nodeId, ast, TraceKind.NODE));
            }
        }

        return new InstrumentationPlan(points);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Walk the entire {@link CompilationUnit} and index every {@link ASTNode}
     * by a long key encoding {@code (startPosition << 20 | length)}.
     * The key is collision-free for all practical Java source files.
     */
    private Map<Long, ASTNode> buildAstIndex(CompilationUnit cu) {
        Map<Long, ASTNode> index = new HashMap<>();
        cu.accept(new ASTVisitor() {
            @Override
            public void preVisit(ASTNode node) {
                int start = node.getStartPosition();
                int len   = node.getLength();
                if (start >= 0 && len > 0) {
                    index.put(posKey(start, len), node);
                }
            }
        });
        return index;
    }

    private static long posKey(int start, int length) {
        return ((long) start << 20) | (length & 0xFFFFF);
    }

    /**
     * Returns {@code true} for CFG node kinds that represent a branch condition
     * (if-test, loop-test) rather than a plain executable statement.
     */
    private static boolean isBranchNode(CfgNodeKind kind) {
        return switch (kind) {
            case BRANCH, LOOP -> true;
            default -> false;
        };
    }

    private static boolean isBooleanLiteral(ASTNode node) {
        return node instanceof BooleanLiteral;
    }
}
