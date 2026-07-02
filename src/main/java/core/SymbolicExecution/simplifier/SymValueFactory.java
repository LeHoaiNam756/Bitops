package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Hash-Consing (structural interning) factory for {@link SymbolicValue} nodes.
 *
 * ── What is hash-consing? ────────────────────────────────────────────────────
 * Two structurally equal expressions are mapped to the exact same Java object.
 * This turns O(n) structural equality checks into O(1) reference equality (==),
 * and collapses duplicate sub-trees into shared pointers automatically.
 *
 * ── Benefits in this pipeline ────────────────────────────────────────────────
 *  1. Simplifier memo-cache uses identity keys → HashMap lookup is O(1), no
 *     recursive equals() walk needed.
 *  2. Z3Encoder can key its Expr cache on object identity → zero re-encoding
 *     of shared sub-expressions.
 *  3. DeadConstraintStrategy's structural-equality checks (x == x, left==right)
 *     become a single pointer comparison.
 *  4. Memory: 10 000 occurrences of "x + 1" stored as one object.
 *
 * ── Thread safety ────────────────────────────────────────────────────────────
 * Uses ConcurrentHashMap. Safe to share across analysis threads (parallel
 * path exploration). Each intern() call is lock-free on the happy path.
 *
 * ── Scope ────────────────────────────────────────────────────────────────────
 * One factory per analysis session (or per method under analysis).
 * Call {@link #reset()} between top-level analyses to avoid cross-method
 * pollution and allow GC of stale nodes.
 */
public final class SymValueFactory {
    /**
     * The canonical store: structurally equal nodes map to the same reference.
     *
     * Key == Value: ConcurrentHashMap gives us putIfAbsent semantics cheaply.
     * The map owns weak-like semantics conceptually, but we use strong refs
     * intentionally — nodes live as long as the analysis session.
     */
    private final ConcurrentHashMap<SymbolicValue, SymbolicValue> table =
            new ConcurrentHashMap<>(1 << 14); // 16 K initial buckets

    private final LongAdder hits   = new LongAdder();
    private final LongAdder misses = new LongAdder();

    private static final SymValueFactory GLOBAL = new SymValueFactory();

    public static SymValueFactory global() { return GLOBAL; }


    /**
     * Return the canonical representative of {@code node}.
     *
     * If an equal node was already interned, return that existing object
     * (hit path – O(1) ConcurrentHashMap lookup).
     * Otherwise, store {@code node} as the canonical form and return it
     * (miss path – O(1) insert).
     *
     * IMPORTANT: callers must first intern all children before interning the
     * parent, so that the parent's equals()/hashCode() is based on already-
     * canonical children references.  {@link SymbolicSimplifier} does this
     * automatically as part of rebuildChildren().
     *
     * @param node any SymbolicValue (may already be interned)
     * @return the canonical representative, never null
     */
    public SymbolicValue intern(SymbolicValue node) {
        // putIfAbsent: atomic, returns null on insert (miss), existing on hit
        SymbolicValue existing = table.putIfAbsent(node, node);
        if (existing == null) {
            misses.increment();
            return node;     // node is now canonical
        }
        hits.increment();
        return existing;     // return pre-existing canonical object
    }

    // =========================================================================
    // Typed factory methods (intern at construction time)
    // =========================================================================
    // Callers use these instead of `new SymXxx(...)` to guarantee that every
    // freshly built node is immediately canonicalised.

    public SymLiteral literal(Object value) {
        return (SymLiteral) intern(new SymLiteral(value));
    }

    public SymLiteral litInt(int v)       { return literal(v); }
    public SymLiteral litLong(long v)     { return literal(v); }
    public SymLiteral litFloat(float v)   { return literal(v); }
    public SymLiteral litDouble(double v) { return literal(v); }
    public SymLiteral litBool(boolean v)  { return literal(v); }
    public SymLiteral litChar(char v)     { return literal(v); }

    public SymVariable variable(String name) {
        return (SymVariable) intern(new SymVariable(name));
    }

    public SymBinaryOp binaryOp(SymbolicValue left, SymBinaryOp.Op op, SymbolicValue right) {
        // Precondition: left and right are already interned
        return (SymBinaryOp) intern(new SymBinaryOp(left, op, right));
    }

    public SymUnaryOp unaryOp(SymUnaryOp.Op op, SymbolicValue operand) {
        return (SymUnaryOp) intern(new SymUnaryOp(op, operand));
    }

    public SymITE ite(SymbolicValue cond, SymbolicValue then, SymbolicValue else_) {
        return (SymITE) intern(new SymITE(cond, then, else_));
    }

    public SymFieldAccess fieldAccess(SymbolicValue receiver, String field) {
        return fieldAccess(receiver, field, null);
    }

    public SymFieldAccess fieldAccess(SymbolicValue receiver, String field, core.SymbolicExecution.model.types.SymType fieldType) {
        return (SymFieldAccess) intern(new SymFieldAccess(receiver, field, fieldType));
    }

    public SymArraySelect arraySelect(SymbolicValue arr, SymbolicValue index) {
        return (SymArraySelect) intern(new SymArraySelect(arr, index));
    }

    public SymArrayStore arrayStore(SymbolicValue arr, SymbolicValue index, SymbolicValue value) {
        return (SymArrayStore) intern(new SymArrayStore(arr, index, value));
    }


    public void reset() {
        table.clear();
        hits.reset();
        misses.reset();
    }


    public int size() { return table.size(); }

    // =========================================================================
    // Telemetry snapshot
    // =========================================================================

    public record Stats(long hits, long misses, int tableSize, double hitRate) {
        @Override public String toString() {
            return String.format(
                "SymValueFactory{hits=%d, misses=%d, size=%d, hitRate=%.1f%%}",
                hits, misses, tableSize, hitRate * 100);
        }
    }

    public Stats stats() {
        long h = hits.sum(), m = misses.sum();
        long total = h + m;
        return new Stats(h, m, table.size(), total == 0 ? 0.0 : (double) h / total);
    }
}