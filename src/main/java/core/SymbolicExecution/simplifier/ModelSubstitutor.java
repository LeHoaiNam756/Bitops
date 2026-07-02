package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;
import core.SymbolicExecution.z3encoder.Z3ModelBindings;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * Substitutes concrete variable assignments from a Z3 SAT model into
 * {@link SymbolicValue} trees.
 *
 * ── What it does ─────────────────────────────────────────────────────────────
 * Given the bindings  { x → 42, flag → false }  from a Z3 model, this class
 * walks an expression tree and replaces every {@link SymVariable} whose name
 * appears in the bindings with the corresponding {@link SymLiteral}.
 *
 * The result is a partially (or fully) concrete expression that the downstream
 * {@link core.SymbolicExecution.simplifier.SymbolicSimplifier} can
 * then constant-fold, potentially collapsing entire sub-trees to literals.
 *
 * ── Caching ──────────────────────────────────────────────────────────────────
 * Uses an IdentityHashMap memo per substitute() call.  Because input nodes
 * are interned (via SymValueFactory), identical sub-trees share one Java
 * object, making IdentityHashMap lookup an O(1) pointer comparison.
 *
 * ── Immutability ─────────────────────────────────────────────────────────────
 * All SymbolicValue nodes are records (immutable).  Substitution always builds
 * new nodes; the originals are untouched.
 *
 * ── Usage ────────────────────────────────────────────────────────────────────
 * <pre>
 *   ModelSubstitutor sub = new ModelSubstitutor(factory);
 *   SymbolicValue result = sub.substitute(expr, bindings);
 *   // then simplify result with SymbolicSimplifier for constant-folding
 * </pre>
 */
public final class ModelSubstitutor {

    private final SymValueFactory factory;

    public ModelSubstitutor(SymValueFactory factory) {
        this.factory = factory;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Substitute all variables in {@code node} that have a binding in
     * {@code model}, returning a new (partially-concrete) expression tree.
     *
     * @param node    the expression to rewrite
     * @param model   concrete assignments from Z3's SAT model
     * @return a new tree with bound variables replaced by literals
     */
    public SymbolicValue substitute(SymbolicValue node, Z3ModelBindings model) {
        if (model.isEmpty()) return node;  // fast path: nothing to substitute
        IdentityHashMap<SymbolicValue, SymbolicValue> memo = new IdentityHashMap<>();
        return visit(node, model, memo);
    }

    /**
     * Substitute into every expression in {@code nodes}, sharing one memo
     * cache across all of them (shared sub-trees substituted only once).
     */
    public List<SymbolicValue> substituteAll(List<SymbolicValue> nodes, Z3ModelBindings model) {
        if (model.isEmpty()) return nodes;
        IdentityHashMap<SymbolicValue, SymbolicValue> memo = new IdentityHashMap<>();
        return nodes.stream().map(n -> visit(n, model, memo)).toList();
    }

    // =========================================================================
    // Recursive visitor
    // =========================================================================

    private SymbolicValue visit(SymbolicValue node,
                                Z3ModelBindings model,
                                IdentityHashMap<SymbolicValue, SymbolicValue> memo) {

        // ── Memo hit ─────────────────────────────────────────────────────────
        SymbolicValue cached = memo.get(node);
        if (cached != null) return cached;

        SymbolicValue result = rewrite(node, model, memo);

        // Intern the result so downstream IdentityHashMap caches work correctly
        SymbolicValue canonical = factory.intern(result);
        memo.put(node, canonical);
        return canonical;
    }

    private SymbolicValue rewrite(SymbolicValue node,
                                  Z3ModelBindings model,
                                  IdentityHashMap<SymbolicValue, SymbolicValue> memo) {

        if (node instanceof SymVariable var) {
            return model.lookup(var.name())
                    .map(lit -> (SymbolicValue) lit)
                    .orElse(var);
        }

        if (node instanceof SymLiteral) {
            return node;
        }

        if (node instanceof SymUnaryOp u) {
            SymbolicValue operand = rewrite(u.operand(), model, memo);
            return operand == u.operand()
                    ? u
                    : factory.unaryOp(u.op(), operand);
        }

        if (node instanceof SymBinaryOp b) {
            SymbolicValue left = rewrite(b.left(), model, memo);
            SymbolicValue right = rewrite(b.right(), model, memo);
            return (left == b.left() && right == b.right())
                    ? b
                    : factory.binaryOp(left, b.op(), right);
        }

        if (node instanceof SymITE ite) {
            SymbolicValue cond = rewrite(ite.cond(), model, memo);
            SymbolicValue thenBranch = rewrite(ite.thenBranch(), model, memo);
            SymbolicValue elseBranch = rewrite(ite.elseBranch(), model, memo);

            return (cond == ite.cond()
                    && thenBranch == ite.thenBranch()
                    && elseBranch == ite.elseBranch())
                    ? ite
                    : factory.ite(cond, thenBranch, elseBranch);
        }

        if (node instanceof SymFieldAccess f) {
            SymbolicValue recv = rewrite(f.receiver(), model, memo);
            return recv == f.receiver()
                    ? f
                    : factory.fieldAccess(recv, f.fieldName(), f.fieldType());
        }

        if (node instanceof SymArraySelect s) {
            SymbolicValue arr = rewrite(s.arr(), model, memo);
            SymbolicValue index = rewrite(s.index(), model, memo);
            return (arr == s.arr() && index == s.index())
                    ? s
                    : factory.arraySelect(arr, index);
        }

        if (node instanceof SymArrayStore st) {
            SymbolicValue arr = rewrite(st.arr(), model, memo);
            SymbolicValue index = rewrite(st.index(), model, memo);
            SymbolicValue val = rewrite(st.value(), model, memo);
            return (arr == st.arr() && index == st.index() && val == st.value())
                    ? st
                    : factory.arrayStore(arr, index, val);
        }

        throw new IllegalStateException("Unknown SymbolicValue: " + node);
    }

}