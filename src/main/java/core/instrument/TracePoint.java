package core.instrument;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Immutable record linking a CFG node ID to the AST node that produced it,
 * and the {@link TraceKind} that describes the probe's role.
 *
 * <p>Instances are created exclusively by {@link InstrumentationPlanner} and
 * consumed by {@link SourceEmitter} (for code generation) and by
 * {@link InstrumentationPlan} (for runtime look-up).
 *
 * @param cfgNodeId integer ID assigned by {@code ControlFlowGraph.addNode()}
 * @param astNode   the original JDT AST node
 * @param kind      role of this probe point
 */
public record TracePoint(
        int      cfgNodeId,
        ASTNode  astNode,
        TraceKind kind
) {
    /** Source character offset of the underlying AST node. */
    public int startPosition() {
        return astNode.getStartPosition();
    }

    /** Source character length of the underlying AST node. */
    public int length() {
        return astNode.getLength();
    }
}