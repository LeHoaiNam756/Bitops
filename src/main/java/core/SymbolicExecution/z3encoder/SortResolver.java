package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.*;
import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.*;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Infers the Z3 {@link Sort} for any {@link SymbolicValue} node before encoding.
 *
 * ── Sort rules ───────────────────────────────────────────────────────────────
 *
 *  SymLiteral
 *    int / long / short / byte / char  →  IntSort
 *    float                             →  FPSort (32-bit IEEE 754)
 *    double                            →  FPSort (64-bit IEEE 754)
 *    boolean                           →  BoolSort
 *
 *  SymVariable
 *    looked up in the caller-supplied varSorts map (built from TypeContext).
 *    Unknown / missing variable → IntSort (safe default, flagged in log).
 *
 *  SymBinaryOp
 *    comparison ops  (EQ NEQ SGT SLT SGE SLE UGT UGE ULT ULE)  →  BoolSort
 *    logical ops     (AND OR)                                    →  BoolSort
 *    bitwise ops     (BAND BOR BXOR BLS BRS BURS)               →  BitVecSort(32|64)
 *    arithmetic ops  →  widened sort of operands (int+real→real, etc.)
 *
 *  SymUnaryOp
 *    NOT              →  BoolSort
 *    NEG / PLUS / INC / DEC / COMPLIMENT  →  sort of operand
 *
 *  SymITE            →  sort of thenBranch (must match elseBranch)
 *
 *  SymArraySelect    →  value sort of the array (the range of ArraySort)
 *  SymArrayStore     →  same ArraySort as the base array
 *  SymFieldAccess    →  treated as fresh variable → IntSort default
 *
 * ── Bitwise switch ────────────────────────────────────────────────────────────
 * When a bitwise op is detected, the whole expression switches to BitVecSort.
 * Width is 64-bit if either operand is long/double, 32-bit otherwise.
 * IntSort operands are sign-extended when being wrapped in a BitVec context
 * (handled in Z3Encoder, not here).
 *
 * ── Caching ──────────────────────────────────────────────────────────────────
 * Uses an IdentityHashMap memo per resolve() call.  Interned nodes make this
 * an O(1) pointer lookup.
 */
public final class SortResolver {

    private final Context             ctx;
    private final Map<String, Sort>   varSorts;   // variable name → Z3 Sort

    // Pre-built sorts (allocated once per Context)
    final IntSort    intSort;
    final BoolSort   boolSort;
    final FPSort     fp32Sort;   // float  – IEEE 754 single
    final FPSort     fp64Sort;   // double – IEEE 754 double
    final BitVecSort bv32Sort;
    final BitVecSort bv64Sort;

    // =========================================================================
    // Construction
    // =========================================================================

    /**
     * @param ctx      the Z3 Context (owns all Sort objects)
     * @param varSorts caller-supplied map: variable name → Z3 Sort,
     *                 built from TypeContext before starting the encoding pass
     */
    public SortResolver(Context ctx, Map<String, Sort> varSorts) {
        this.ctx      = ctx;
        this.varSorts = varSorts;

        this.intSort  = ctx.getIntSort();
        this.boolSort = ctx.getBoolSort();
        this.fp32Sort = ctx.mkFPSort32();
        this.fp64Sort = ctx.mkFPSort64();
        this.bv32Sort = ctx.mkBitVecSort(32);
        this.bv64Sort = ctx.mkBitVecSort(64);
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Resolve the Z3 Sort for {@code node}.
     * Uses a per-call IdentityHashMap to avoid redundant traversal.
     */
    public Sort resolve(SymbolicValue node) {
        return resolve(node, new IdentityHashMap<>());
    }

    // =========================================================================
    // Recursive resolution
    // =========================================================================

    Sort resolve(SymbolicValue node, IdentityHashMap<SymbolicValue, Sort> memo) {
        Sort cached = memo.get(node);
        if (cached != null) return cached;

        Sort result = compute(node, memo);
        memo.put(node, result);
        return result;
    }

    private Sort compute(SymbolicValue node,
                     IdentityHashMap<SymbolicValue, Sort> memo) {

    // ── Literals ──────────────────────────────────────────────────────
    if (node instanceof SymLiteral lit) {
        return sortOfLiteral(lit.value());
    }

    // ── Variables ─────────────────────────────────────────────────────
    if (node instanceof SymVariable var) {

        Sort s = varSorts.get(var.name());
        if (s == null) {
            // Unknown variable: default to IntSort (conservative)
            System.err.println("[SortResolver] WARNING: no sort for variable '"
                    + var.name() + "' – defaulting to IntSort");
            return intSort;
        }

        return s;
    }

    // ── Unary ─────────────────────────────────────────────────────────
    if (node instanceof SymUnaryOp u) {
        return switch (u.op()) {
            case NOT -> boolSort;
            case NEG, PLUS, INC, DEC, COMPLIMENT -> resolve(u.operand(), memo);
        };
    }

    // ── Binary ────────────────────────────────────────────────────────
    if (node instanceof SymBinaryOp) {
        SymBinaryOp b = (SymBinaryOp) node;
        return resolveBinary(b, memo);
    }

    // ── ITE ───────────────────────────────────────────────────────────
    // Sort comes from the branches (condition is always BoolSort)
    if (node instanceof SymITE ite) {
        return resolve(ite.thenBranch(), memo);
    }

    // ── Arrays ────────────────────────────────────────────────────────
    if (node instanceof SymArraySelect) {
        SymArraySelect s = (SymArraySelect) node;

        Sort arrSort = resolve(s.arr(), memo);

        if (arrSort instanceof ArraySort<?, ?> as) {
            return as.getRange();
        }

        throw new EncodingException(
                "SymArraySelect: base expression is not an array sort", s);
    }

    if (node instanceof SymArrayStore st) {

        // Store preserves array sort
        return resolve(st.arr(), memo);
    }

    // ── Field access: fresh variable, default IntSort ─────────────────
    if (node instanceof SymFieldAccess) {
        return intSort;
    }

    throw new IllegalArgumentException(
            "Unsupported SymbolicValue: " + node);
}

    // =========================================================================
    // Binary sort resolution
    // =========================================================================

    private Sort resolveBinary(SymBinaryOp b,
                                IdentityHashMap<SymbolicValue, Sort> memo) {
        return switch (b.op()) {

            // Always bool
            case EQ, NEQ,
                 SGT, SLT, SGE, SLE,
                 UGT, UGE, ULT, ULE,
                 AND, OR              -> boolSort;

            // Bitwise → BitVecSort; width determined by operands
            case BAND, BOR, BXOR,
                 BLS, BRS, BURS      -> {
                Sort ls = resolve(b.left(),  memo);
                Sort rs = resolve(b.right(), memo);
                yield widenToBitVec(ls, rs);
            }

            // Arithmetic → widen operand sorts
            default -> {
                Sort ls = resolve(b.left(),  memo);
                Sort rs = resolve(b.right(), memo);
                yield widenArithmetic(ls, rs);
            }
        };
    }

    // =========================================================================
    // Sort widening helpers
    // =========================================================================

    /**
     * Arithmetic widening (mirrors Java's numeric promotion):
     *   double  > float  > long  > int
     * FP sorts widen to the wider FP sort; int + FP → FP.
     */
    Sort widenArithmetic(Sort a, Sort b) {
        if (a.equals(fp64Sort) || b.equals(fp64Sort)) return fp64Sort;
        if (a.equals(fp32Sort) || b.equals(fp32Sort)) return fp32Sort;
        if (a.equals(bv64Sort) || b.equals(bv64Sort)) return bv64Sort;
        if (a.equals(bv32Sort) || b.equals(bv32Sort)) return bv32Sort;
        return intSort;  // both IntSort (int, long treated uniformly here)
    }

    /**
     * Bitwise context: promote both operands to the wider BitVecSort.
     * IntSort is treated as 32-bit by default (conservative).
     */
    private Sort widenToBitVec(Sort a, Sort b) {
        boolean a64 = a.equals(bv64Sort) || a.equals(fp64Sort) || isLongInt(a);
        boolean b64 = b.equals(bv64Sort) || b.equals(fp64Sort) || isLongInt(b);
        return (a64 || b64) ? bv64Sort : bv32Sort;
    }

    private boolean isLongInt(Sort s) {
        // IntSort is used for both int and long; we can't distinguish without
        // the original SymType.  Default: 32-bit unless the varSorts map says long.
        return false;
    }

    // =========================================================================
    // SymType → Z3 Sort  (called by the external builder of varSorts)
    // =========================================================================

    public Sort symTypeToSort(SymType type) {
        if (type instanceof PrimitiveSymType) {
            PrimitiveSymType p = (PrimitiveSymType) type;
            return switch (p) {
                case INT, SHORT, BYTE, CHAR, LONG -> intSort; // unbounded int in Z3
                case FLOAT -> fp32Sort;
                case DOUBLE -> fp64Sort;
                case BOOLEAN -> boolSort;
            };
        }
        // Object/String/reference → model as uninterpreted IntSort (heap address)
        if (type instanceof ArraySymType arrayType) {
            Sort range = symTypeToSort(arrayType.elementType());
            for (int i = 0; i < arrayType.dimensions(); i++) {
                range = ctx.mkArraySort(intSort, range);
            }
            return range;
        }
        if (type instanceof ObjectSymType) { return intSort; }
        if (type instanceof NullSymType) { return intSort; }
        if (type instanceof VoidSymType) { throw new IllegalArgumentException("void has no Z3 Sort"); }
        if (type instanceof UnknownSymType) { return intSort; } //fall back
        if (type instanceof BottomSymType) { return intSort; }
        throw new IllegalArgumentException("Unsupported SymType: " + type);
    }


    // =========================================================================
    // Literal sort helper
    // =========================================================================

    private Sort sortOfLiteral(Object value) {
        if (value instanceof Integer
                || value instanceof Long
                || value instanceof Short
                || value instanceof Byte
                || value instanceof Character) {
            return intSort;
        }
        if (value instanceof Float) { return fp32Sort; }
        if (value instanceof Double) { return fp64Sort; }
        if (value instanceof Boolean) { return boolSort; }
        throw new IllegalArgumentException("Unknown literal type: " + value.getClass());
    }


    // =========================================================================
    // Accessors (used by Z3Encoder to read pre-built sorts)
    // =========================================================================

    public Context  ctx()     { return ctx;     }
    public IntSort  intSort() { return intSort;  }
    public BoolSort boolSort(){ return boolSort; }
    public FPSort   fp32Sort(){ return fp32Sort; }
    public FPSort   fp64Sort(){ return fp64Sort; }
    public BitVecSort bv32Sort(){ return bv32Sort; }
    public BitVecSort bv64Sort(){ return bv64Sort; }
}
