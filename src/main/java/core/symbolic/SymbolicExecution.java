package core.symbolic;

import core.SymbolicExecution.dispatch.AstDispatcher;
import core.SymbolicExecution.dispatch.*;
import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.*;
import core.SymbolicExecution.simplifier.SymbolicSimplifier;
import core.SymbolicExecution.z3encoder.ConstraintSolver;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
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
        return executePath(cfg, path, parameters, parameterTypes, Z3EncodingMode.BITVECTOR);
    }

    public SolverResult executePath(
            ControlFlowGraph cfg,
            List<ControlFlowGraph.Edge> path,
            List<ASTNode> parameters,
            Map<String, SymType> parameterTypes,
            Z3EncodingMode encodingMode) {

        SymbolicState state = SymbolicState.builder()
                .memoryModel(new MemoryModel())
                .typeContext(new TypeContext())
                .build();

        // --- 1. Materialize field initializers and declare parameters --------
        initializeFields(enclosingMethod(parameters, cfg), state);

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
        constraints.addAll(state.getAssumptions());

        // --- 3. Simplify expressions (constant-fold, identities, normalise) -
        List<SymbolicValue> simplifiedConstraints = expressionSimplifier.simplifyAll(constraints);

        // --- 4. Solve -------------------------------------------------------
        try (ConstraintSolver solver = ConstraintSolver.create(parameterTypes, encodingMode)) {
            return solver.check(simplifiedConstraints);
        }
    }

    private MethodDeclaration enclosingMethod(List<ASTNode> parameters, ControlFlowGraph cfg) {
        if (!parameters.isEmpty()) {
            ASTNode current = parameters.get(0);
            while (current != null && !(current instanceof MethodDeclaration)) {
                current = current.getParent();
            }
            if (current instanceof MethodDeclaration method) return method;
        }

        for (int nodeId : cfg.getNodes()) {
            ASTNode current = cfg.getNode(nodeId).getAst();
            while (current != null && !(current instanceof MethodDeclaration)) {
                current = current.getParent();
            }
            if (current instanceof MethodDeclaration method) return method;
        }
        return null;
    }

    private void initializeFields(MethodDeclaration method, SymbolicState state) {
        if (method == null) return;

        ASTNode owner = method.getParent();
        while (owner != null && !(owner instanceof AbstractTypeDeclaration)) {
            owner = owner.getParent();
        }
        if (!(owner instanceof AbstractTypeDeclaration type)) return;

        for (Object declaration : type.bodyDeclarations()) {
            if (!(declaration instanceof FieldDeclaration field)) continue;

            SymType declarationType = SymTypeMap.convert(field.getType());
            for (Object fragmentObject : field.fragments()) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentObject;
                int extraDimensions = fragment.getExtraDimensions();
                SymType fieldType = extraDimensions == 0
                        ? declarationType
                        : SymTypeMap.addArrayDimensions(declarationType, extraDimensions);
                Expression initializer = fragment.getInitializer();
                if (initializer == null) continue;

                String fieldName = fragment.getName().getIdentifier();
                SymbolicValue value;
                if (initializer instanceof ArrayInitializer arrayInitializer) {
                    value = materializeArrayInitializer(fieldName, fieldType, arrayInitializer, state);
                } else {
                    state.getTypeContext().pushAssignment(fieldType);
                    try {
                        value = dispatcher.eval(initializer, state);
                    } finally {
                        state.getTypeContext().pop();
                    }
                }
                if (value != null) state.getMemoryModel().write(fieldName, value);
            }
        }
    }

    private SymbolicValue materializeArrayInitializer(
            String fieldName,
            SymType fieldType,
            ArrayInitializer initializer,
            SymbolicState state) {
        SymbolicValue array = new SymFieldAccess(
                new SymVariable("this"), fieldName, fieldType);

        @SuppressWarnings("unchecked")
        List<Expression> expressions = initializer.expressions();
        for (int i = 0; i < expressions.size(); i++) {
            SymbolicValue element = dispatcher.eval(expressions.get(i), state);
            array = new SymArrayStore(array, SymLiteral.of(i), element);
        }
        state.rememberArrayLength(array, expressions.size());
        return array;
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
                .register(new StringLiteralHandler())
                .register(new ArrayAccessHandler())
                .register(new ArrayCreationHandler())
                .register(new ClassInstanceCreationHandler())
                .register(new MethodInvocationHandler())
                .register(new VariableDeclarationExpressionHandler())
                .register(new EnhancedForStatementHandler())
                .register(new BreakStatementHandler())
                .register(new ContinueStatementHandler())
                .register(new ThrowStatementHandler());
    }
}
