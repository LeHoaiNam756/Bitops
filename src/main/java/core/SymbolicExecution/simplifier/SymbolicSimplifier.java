package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;

import java.util.IdentityHashMap;
import java.util.List;

public class SymbolicSimplifier {
    private final List<SimplificationStrategy> pipeline;
    private final SymValueFactory              factory;
    private static final int MAX_ROUNDS = 8;


    public SymbolicSimplifier(List<SimplificationStrategy> pipeline, SymValueFactory factory) {
        this.pipeline = List.copyOf(pipeline);
        this.factory  = factory;
    }


    public static SymbolicSimplifier withDefaultPipeline() {
        return withDefaultPipeline(SymValueFactory.global());
    }


    public static SymbolicSimplifier withDefaultPipeline(SymValueFactory factory) {
        return new SymbolicSimplifier(List.of(
                ConstantFoldingStrategy.INSTANCE,
                IdentityEliminationStrategy.INSTANCE,
                DeadConstraintStrategy.INSTANCE,
                NormalisationStrategy.INSTANCE
        ), factory);
    }

    /**
     * Expression-simplification pipeline (no dead-constraint scan).
     *
     * Use this in {@link core.symbolic.SymbolicExecution} to keep symbolic
     * expressions compact while a path is being built.  Dead-constraint
     * detection is left to the solver phase.
     */
    public static SymbolicSimplifier withExpressionPipeline() {
        return withExpressionPipeline(SymValueFactory.global());
    }

    public static SymbolicSimplifier withExpressionPipeline(SymValueFactory factory) {
        return new SymbolicSimplifier(List.of(
                ConstantFoldingStrategy.INSTANCE,
                IdentityEliminationStrategy.INSTANCE,
                NormalisationStrategy.INSTANCE
        ), factory);
    }

    public SymbolicValue simplify(SymbolicValue value) {
        IdentityHashMap<SymbolicValue, SymbolicValue> memo = new IdentityHashMap<>();
        return visit(value, memo);
    }


    public List<SymbolicValue> simplifyAll(List<SymbolicValue> values) {
        IdentityHashMap<SymbolicValue, SymbolicValue> memo = new IdentityHashMap<>();
        return values.stream().map(v -> visit(v, memo)).toList();
    }


    public DeadConstraintStrategy.ScanResult scanPathCondition(
            List<SymbolicValue> pathCondition) {
        List<SymbolicValue> simplified = simplifyAll(pathCondition);
        return DeadConstraintStrategy.INSTANCE.scan(simplified);
    }


    private SymbolicValue visit(SymbolicValue node,
                                IdentityHashMap<SymbolicValue, SymbolicValue> memo) {

        SymbolicValue cached = memo.get(node);
        if (cached != null) return cached;

        SymbolicValue rebuilt = rebuildChildren(node, memo);

        SymbolicValue simplified = applyPipeline(rebuilt, memo);

        SymbolicValue canonical = factory.intern(simplified);

        memo.put(node, canonical);

        return canonical;
    }


    private SymbolicValue rebuildChildren(SymbolicValue node,
                                          IdentityHashMap<SymbolicValue, SymbolicValue> memo) {
        if (node instanceof SymLiteral) return node;
        if (node instanceof SymVariable) return node;
        if (node instanceof SymUnaryOp u) {
            SymbolicValue operand = visit(u.operand(), memo);
            return (operand == u.operand()) ? u : factory.unaryOp(u.op(), operand);
        }
        if (node instanceof SymBinaryOp b) {
            SymbolicValue left = visit(b.left(), memo);
            SymbolicValue right = visit(b.right(), memo);
            return (left == b.left() && right == b.right()) ? b : factory.binaryOp(left, b.op(), right);
        }
        if (node instanceof SymITE ite) {
            SymbolicValue cond = visit(ite.cond(), memo);
            SymbolicValue thenBranch = visit(ite.thenBranch(), memo);
            SymbolicValue elseBranch = visit(ite.elseBranch(), memo);
            return (cond == ite.cond() && thenBranch == ite.thenBranch()
                    && elseBranch == ite.elseBranch()) ? ite :
                    factory.ite(cond, thenBranch, elseBranch);
        }
        if (node instanceof SymFieldAccess f) {
            SymbolicValue receiver = visit(f.receiver(), memo);
            return (receiver == f.receiver()) ? f : factory.fieldAccess(receiver, f.fieldName());
        }
        if (node instanceof SymArraySelect as) {
            SymbolicValue arr = visit(as.arr(), memo);
            SymbolicValue index = visit(as.index(), memo);
            return (arr == as.arr() && index == as.index()) ? as : factory.arraySelect(arr, index);
        }
        if (node instanceof SymArrayStore as) {
            SymbolicValue arr = visit(as.arr(), memo);
            SymbolicValue index = visit(as.index(), memo);
            SymbolicValue val = visit(as.value(), memo);
            return (arr == as.arr() && index == as.index() && val == as.value()) ? as :
                    factory.arrayStore(arr, index, val);
        }
        return node;
    }


    private SymbolicValue applyPipeline(SymbolicValue node,
                                        IdentityHashMap<SymbolicValue, SymbolicValue> memo) {
        SymbolicValue current = node;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            SymbolicValue after = applyAllStrategies(current);
            if (after == current) break;
            after   = rebuildChildren(after, memo);
            current = factory.intern(after);
        }
        return current;
    }


    private SymbolicValue applyAllStrategies(SymbolicValue node) {
        for (SimplificationStrategy strategy : pipeline) {
            SymbolicValue result = strategy.apply(node);
            if (result != node) return result;
        }
        return node;
    }

//    public SymValueFactory.Stats factoryStats() { return factory.stats(); }
}
