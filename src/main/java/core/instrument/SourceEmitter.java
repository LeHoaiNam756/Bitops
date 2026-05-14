package core.instrument;

import core.cfg.Coverage;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transforms a {@link CompilationUnit} into an annotation-only clone source
 * string, using the {@link InstrumentationPlan} to place
 * {@code TraceRecorder.mark(...)} calls before each tracked statement or
 * inside each tracked condition.
 *
 * <h3>What this class does</h3>
 * <ul>
 *   <li>Emits the relocated {@code package} declaration and all original
 *       imports.</li>
 *   <li>For each non-constructor method: reproduces the signature verbatim,
 *       then recursively walks the body, inserting direct trace calls before
 *       tracked nodes.</li>
 *   <li>Condition expressions (branch nodes) are wrapped in the same
 *       {@code ((cond) && TraceRecorder.mark(trueId, COND_T)) ||
 *       TraceRecorder.mark(falseId, COND_F)} idiom as before, but using
 *       integer CFG node IDs instead of string signatures.</li>
 * </ul>
 *
 * <h3>What this class does NOT do</h3>
 * <p>No file I/O, no compilation, no counter mutation.  Those responsibilities
 * belong to {@link InstrumentationFactory} (orchestrator) and {@link core.testpath.TraceRecorder}
 * (runtime).
 */
public final class SourceEmitter {

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Produce the full instrumented source for {@code cu}.
     *
     * @param cu                  compilation unit (same instance used to build
     *                            the plan)
     * @param plan                probe-point mapping produced by
     *                            {@link InstrumentationPlanner}
     * @param coverage            drives branch vs. statement instrumentation
     * @param clonedPackagePrefix e.g. {@link FilePath#CLONED_PROJECT_ROOT_PACKAGE}
     * @return instrumented Java source as a {@code String}
     */
    public String emit(CompilationUnit cu,
                       InstrumentationPlan plan,
                       Coverage coverage,
                       String clonedPackagePrefix) {

        // Index plan by AST start-position so the walk can look up probes
        // without carrying the full plan through every recursive call.
        Map<Integer, List<TracePoint>> nodeProbesByAnchorPos = plan.points().stream()
                .filter(tp -> tp.kind() == TraceKind.NODE)
                .collect(Collectors.groupingBy(tp -> markerAnchor(tp.astNode()).getStartPosition()));

        Map<Long, TracePoint> branchProbesByPosAndKind = plan.points().stream()
                .filter(tp -> tp.kind() != TraceKind.NODE)
                .collect(Collectors.toMap(
                        tp -> EmitContext.kindKey(tp.astNode().getStartPosition(), tp.kind()),
                        Function.identity(),
                        (a, b) -> a));

        EmitContext ctx = new EmitContext(nodeProbesByAnchorPos, branchProbesByPosAndKind, coverage);

        StringBuilder sb = new StringBuilder();

        // ── Package ──────────────────────────────────────────────────────────
        if (cu.getPackage() != null) {
            sb.append("package ").append(clonedPackagePrefix)
                    .append(".").append(cu.getPackage().getName()).append(";\n");
        } else {
            sb.append("package ").append(clonedPackagePrefix).append(";\n");
        }

        // ── Imports ──────────────────────────────────────────────────────────
        for (Object imp : cu.imports()) {
            sb.append(imp);
        }
        sb.append("import core.instrument.TraceKind;\n");
        sb.append("import core.testpath.TraceRecorder;\n");

        // ── Type body ────────────────────────────────────────────────────────
        for (Object rawType : cu.types()) {
            if (!(rawType instanceof TypeDeclaration td)) continue;
            emitType(td, ctx, sb);
        }

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Type
    // -----------------------------------------------------------------------

    private void emitType(TypeDeclaration td, EmitContext ctx, StringBuilder sb) {
        // Modifiers
        for (Object mod : td.modifiers()) sb.append(mod).append(" ");

        sb.append(td.isInterface() ? "interface " : "class ").append(td.getName());

        if (td.getSuperclassType() != null) {
            sb.append(" extends ").append(td.getSuperclassType());
        }
        List<?> ifaces = td.superInterfaceTypes();
        if (!ifaces.isEmpty()) {
            sb.append(" implements ");
            for (int i = 0; i < ifaces.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(ifaces.get(i));
            }
        }
        sb.append(" {\n");

        // Fields – reproduced verbatim
        for (FieldDeclaration fd : td.getFields()) {
            sb.append(fd).append("\n");
        }

        // Methods
        for (MethodDeclaration md : td.getMethods()) {
            if (md.isConstructor()) {
                sb.append(md).append("\n");
            } else {
                emitMethod(md, ctx, sb);
            }
        }

        sb.append("}\n");
    }

    // -----------------------------------------------------------------------
    // Method
    // -----------------------------------------------------------------------

    private void emitMethod(MethodDeclaration md, EmitContext ctx, StringBuilder sb) {
        // Modifiers – promote private → public (preserves original behaviour)
        for (Object mod : md.modifiers()) {
            String m = mod.toString();
            sb.append("private".equals(m) ? "public" : m).append(" ");
        }

        sb.append(md.getReturnType2() != null ? md.getReturnType2() : "").append(" ");
        sb.append(md.getName()).append("(");

        List<?> params = md.parameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i));
        }
        sb.append(") ");

        // Exceptions
        List<?> thrown = md.thrownExceptionTypes();
        if (!thrown.isEmpty()) {
            sb.append("throws ");
            for (int i = 0; i < thrown.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(thrown.get(i));
            }
            sb.append(" ");
        }

        emitBlock(md.getBody(), ctx, sb);
        sb.append("\n");
    }

    // -----------------------------------------------------------------------
    // Statement dispatch
    // -----------------------------------------------------------------------

    private void emitStatement(ASTNode stmt, EmitContext ctx, StringBuilder sb) {
        if (stmt == null) return;

        ctx.probesForAnchor(stmt).forEach(tp -> emitNodeMarker(tp, sb));

        if (stmt instanceof Block b)              { emitBlock(b, ctx, sb); }
        else if (stmt instanceof IfStatement is)  { emitIf(is, ctx, sb); }
        else if (stmt instanceof ForStatement fs) { emitFor(fs, ctx, sb); }
        else if (stmt instanceof WhileStatement ws){ emitWhile(ws, ctx, sb); }
        else if (stmt instanceof DoStatement ds)  { emitDo(ds, ctx, sb); }
        else                                       { emitNormal(stmt, ctx, sb); }
    }

    private void emitBlock(Block block, EmitContext ctx, StringBuilder sb) {
        sb.append("{\n");
        if (block != null) {
            ctx.probesForAnchor(block).forEach(tp -> emitNodeMarker(tp, sb));
            for (Object s : block.statements()) {
                emitStatement((ASTNode) s, ctx, sb);
            }
        }
        sb.append("}\n");
    }

    // ── If ───────────────────────────────────────────────────────────────────

    private void emitIf(IfStatement is, EmitContext ctx, StringBuilder sb) {
        sb.append("if (");
        emitCondition(is.getExpression(), ctx, sb);
        sb.append(") {\n");
        emitStatement(is.getThenStatement(), ctx, sb);
        sb.append("}\n");

        Statement elseStmt = is.getElseStatement();
        if (elseStmt != null) {
            sb.append("else {\n");
            emitStatement(elseStmt, ctx, sb);
            sb.append("}\n");
        }
    }

    // ── For ──────────────────────────────────────────────────────────────────

    private void emitFor(ForStatement fs, EmitContext ctx, StringBuilder sb) {
        sb.append("for (");
        List<?> inits = fs.initializers();
        for (int i = 0; i < inits.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(inits.get(i));
        }
        sb.append("; ");
        emitCondition(fs.getExpression(), ctx, sb);
        sb.append("; ");
        List<?> updaters = fs.updaters();
        for (int i = 0; i < updaters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(updaters.get(i));   // updaters stay as-is
        }
        sb.append(") {\n");
        emitStatement(fs.getBody(), ctx, sb);
        sb.append("}\n");
    }

    // ── While ────────────────────────────────────────────────────────────────

    private void emitWhile(WhileStatement ws, EmitContext ctx, StringBuilder sb) {
        sb.append("while (");
        emitCondition(ws.getExpression(), ctx, sb);
        sb.append(") {\n");
        emitStatement(ws.getBody(), ctx, sb);
        sb.append("}\n");
    }

    // ── Do-While ─────────────────────────────────────────────────────────────

    private void emitDo(DoStatement ds, EmitContext ctx, StringBuilder sb) {
        sb.append("do {\n");
        emitStatement(ds.getBody(), ctx, sb);
        sb.append("} while (");
        emitCondition(ds.getExpression(), ctx, sb);
        sb.append(");\n");
    }

    // ── Normal (non-control-flow) statement ──────────────────────────────────

    /**
     * Emits the original statement verbatim. Node trace calls are emitted by
     * {@link #emitStatement(ASTNode, EmitContext, StringBuilder)} before the
     * statement because CFG nodes may be anchored inside the statement AST.
     *
     * <pre>{@code
     * // emitted output:
     * TraceRecorder.mark(7, TraceKind.NODE);
     * int x = compute();
     * }</pre>
     */
    private void emitNormal(ASTNode stmt, EmitContext ctx, StringBuilder sb) {
        sb.append(stmt);
    }

    // ── Condition expression ──────────────────────────────────────────────────

    /**
     * For branch/MC-DC coverage:
     * <pre>{@code
     * ((cond) && TraceRecorder.mark(trueId, TraceKind.COND_T))
     *     || TraceRecorder.mark(falseId, TraceKind.COND_F)
     * }</pre>
     * For statement coverage the condition is emitted verbatim (no wrapping).
     */
    private void emitCondition(Expression cond, EmitContext ctx, StringBuilder sb) {
        if (ctx.coverage == Coverage.STATEMENT) {
            sb.append(cond);
            return;
        }

        // BRANCH / MCDC
        if (ctx.coverage == Coverage.MCDC) {
            emitMcdcCondition(cond, ctx, sb);
        } else {
            emitBranchCondition(cond, ctx, sb);
        }
    }

    private void emitBranchCondition(Expression cond, EmitContext ctx, StringBuilder sb) {
        Optional<TracePoint> trueProbe  = ctx.probeFor(cond, TraceKind.COND_T);
        Optional<TracePoint> falseProbe = ctx.probeFor(cond, TraceKind.COND_F);

        if (trueProbe.isEmpty() || falseProbe.isEmpty()) {
            // Untracked condition — emit verbatim
            sb.append(cond);
            return;
        }

        int trueId  = trueProbe.get().cfgNodeId();
        int falseId = falseProbe.get().cfgNodeId();

        sb.append("((").append(cond)
                .append(") && TraceRecorder.mark(").append(trueId).append(", TraceKind.COND_T))")
                .append(" || TraceRecorder.mark(").append(falseId).append(", TraceKind.COND_F)");
    }

    /**
     * MC/DC: split on {@code &&} / {@code ||} recursively; leaf conditions
     * get branch probes.
     */
    private void emitMcdcCondition(Expression cond, EmitContext ctx, StringBuilder sb) {
        if (cond instanceof InfixExpression ie && isSeparable(ie.getOperator())) {
            sb.append("(");
            emitMcdcCondition(ie.getLeftOperand(), ctx, sb);
            sb.append(") ").append(ie.getOperator()).append(" (");
            emitMcdcCondition(ie.getRightOperand(), ctx, sb);
            sb.append(")");
            for (Object ext : ie.extendedOperands()) {
                sb.append(" ").append(ie.getOperator()).append(" (");
                emitMcdcCondition((Expression) ext, ctx, sb);
                sb.append(")");
            }
        } else {
            emitBranchCondition(cond, ctx, sb);
        }
    }

    // -----------------------------------------------------------------------
    // Trace mark emission
    // -----------------------------------------------------------------------

    /**
     * Writes a direct statement trace call.
     */
    private void emitNodeMarker(TracePoint tp, StringBuilder sb) {
        sb.append("TraceRecorder.mark(").append(tp.cfgNodeId())
                .append(", TraceKind.").append(tp.kind()).append(");\n");
    }

    // -----------------------------------------------------------------------
    // Internal context
    // -----------------------------------------------------------------------

    private static final class EmitContext {
        /** Keyed by the emitted statement/control node that should own markers. */
        private final Map<Integer, List<TracePoint>> byAnchorPos;
        /**
         * Keyed by (startPosition, kind): enables separate lookup of
         * COND_T vs COND_F for the same AST expression.
         */
        private final Map<Long, TracePoint>    byPosAndKind;
        final Coverage coverage;

        EmitContext(Map<Integer, List<TracePoint>> byAnchorPos,
                    Map<Long, TracePoint> byPosAndKind,
                    Coverage coverage) {
            this.byAnchorPos = byAnchorPos;
            this.coverage = coverage;
            this.byPosAndKind = byPosAndKind;
        }

        List<TracePoint> probesForAnchor(ASTNode node) {
            return byAnchorPos.getOrDefault(node.getStartPosition(), List.of());
        }

        Optional<TracePoint> probeFor(ASTNode node, TraceKind kind) {
            return Optional.ofNullable(
                    byPosAndKind.get(kindKey(node.getStartPosition(), kind)));
        }

        private static long kindKey(int pos, TraceKind kind) {
            return ((long) pos << 4) | kind.ordinal();
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private static boolean isSeparable(InfixExpression.Operator op) {
        return op == InfixExpression.Operator.CONDITIONAL_AND
                || op == InfixExpression.Operator.AND
                || op == InfixExpression.Operator.CONDITIONAL_OR
                || op == InfixExpression.Operator.OR;
    }

    private static ASTNode markerAnchor(ASTNode node) {
        ASTNode current = node;
        while (current != null) {
            if (current instanceof Statement) {
                return current;
            }
            current = current.getParent();
        }
        return node;
    }
}
