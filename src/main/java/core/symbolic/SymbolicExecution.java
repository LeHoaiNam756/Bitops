package core.symbolic;

import core.SymbolicExecution.dispatch.AstDispatcher;
import core.SymbolicExecution.dispatch.*;
import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.*;
import core.SymbolicExecution.simplifier.SymbolicSimplifier;
import core.SymbolicExecution.z3encoder.ConstraintSolver;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.cfg.ControlFlowGraph;
import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * Refactored symbolic-execution facade over the new
 * {@code core.SymbolicExecution.*} packages.
 *
 * <ul>
 *   <li>Manually wires every concrete {@link AstHandler} —
 *       {@code DefaultAstHandlers} is intentionally avoided.</li>
 *   <li>Accepts a raw {@code List<ControlFlowGraph.Edge>}.</li>
 *   <li>Returns {@link SolverResult} (SAT / UNSAT / UNKNOWN)
 *       instead of a raw Z3 {@code Model}.</li>
 * </ul>
 */
public class SymbolicExecution {

    private final AstDispatcher dispatcher;
    private final SymbolicSimplifier expressionSimplifier;

    public SymbolicExecution() {
        this.expressionSimplifier = SymbolicSimplifier.withExpressionPipeline();
        this.dispatcher = buildDispatcher();
    }

    /**
     * Symbolically execute a concrete path through the CFG.
     *
     * @param cfg           the control-flow graph (used to resolve AST nodes)
     * @param path          ordered list of edges forming the path
     * @param parameters    JDT parameter nodes (used to seed the symbolic store)
     * @param parameterTypes map of parameter name → SymType for Z3 sort resolution
     * @return a {@link SolverResult} describing feasibility and model
     */
    public SolverResult executePath(
            ControlFlowGraph cfg,
            List<ControlFlowGraph.Edge> path,
            List<ASTNode> parameters,
            Map<String, SymType> parameterTypes) {

        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        // --- 1. Declare parameters as symbolic variables --------------------
        for (ASTNode p : parameters) {
            if (p instanceof SingleVariableDeclaration svd) {
                String name = svd.getName().getIdentifier();
                SymType symType = parameterTypes.getOrDefault(name, UnknownSymType.INSTANCE);
                state.getTypeContext().push(symType);
                state.getMemoryModel().write(name, new SymVariable(name));
                state.getTypeContext().pop();
            }
        }

        // --- 2. Walk the path, accumulating constraints ---------------------
        List<SymbolicValue> constraints = new ArrayList<>();
        for (ControlFlowGraph.Edge edge : path) {
            ControlFlowGraph.Node node = cfg.getNode(edge.getFrom());
            if (node == null) continue;

            ASTNode ast = node.getAst();
            if (ast == null) continue;

            SymbolicValue value = dispatcher.eval(ast, state);
            if (value == null) continue;
            // Branch edges contribute path-condition constraints
            CfgNodeKind kind = node.getKind();
            if (kind == CfgNodeKind.BRANCH || kind == CfgNodeKind.LOOP) {
                if (edge.getKind() == CfgEdgeKind.TRUE) {
                    constraints.add(value);
                } else if (edge.getKind() == CfgEdgeKind.FALSE) {
                    constraints.add(new SymUnaryOp(SymUnaryOp.Op.NOT, value));
                }
            }
        }

        // --- 3. Simplify expressions (constant-fold, identities, normalise) -
        List<SymbolicValue> simplifiedConstraints = expressionSimplifier.simplifyAll(constraints);

        // --- 4. Solve -------------------------------------------------------
        try (ConstraintSolver solver = ConstraintSolver.create(parameterTypes)) {
            return solver.check(simplifiedConstraints);
        }
    }

    // ------------------------------------------------------------------
    // Dispatcher wiring — every concrete handler registered explicitly
    // ------------------------------------------------------------------

    private static AstDispatcher buildDispatcher() {
        return new AstDispatcher()
                .register(new BlockHandler())
                .register(new ExpressionStatementHandler())
                .register(new VariableDeclarationStatementHandler())
                .register(new AssignmentHandler())
                .register(new ReturnStatementHandler())
                .register(new InfixExpressionHandler())
                .register(new PrefixExpressionHandler())
                .register(new PostfixExpressionHandler())
                .register(new ConditionalExpressionHandler())
                .register(new CastExpressionHandler())
                .register(new ParenthesizedExpressionHandler())
                .register(new SimpleNameHandler())
                .register(new QualifiedNameHandler())
                .register(new FieldAccessHandler())
                .register(new NumberLiteralHandler())
                .register(new BooleanLiteralHandler())
                .register(new CharacterLiteralHandler())
                .register(new ArrayAccessHandler())
                .register(new ArrayCreationHandler())
                .register(new MethodInvocationHandler())
                .register(new VariableDeclarationExpressionHandler())
                .register(new BreakStatementHandler());
    }
}
