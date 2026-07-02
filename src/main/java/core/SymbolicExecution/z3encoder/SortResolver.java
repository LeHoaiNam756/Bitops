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
 *    int                        →  BitVecSort(32)
 *    short / char               →  BitVecSort(16)
 *    byte                       →  BitVecSort(8)
 *    long                       →  BitVecSort(64)
 *    float                      →  FPSort(32)  – IEEE 754 single
 *    double                     →  FPSort(64)  – IEEE 754 double
 *    boolean                    →  BoolSort
 *    String                     →  StringSort
 *
 *  SymVariable
 *    looked up in the caller-supplied varSorts map (built from TypeContext).
 *    Unknown / missing integer variable → BitVecSort(32) (safe default, logged).
 *
 *  SymBinaryOp
 *    comparison ops  (EQ NEQ SGT SLT SGE SLE UGT UGE UGT UGT ULE UGE)  →  BoolSort
 *    logical ops     (AND OR)                                            →  BoolSort
 *    bitwise ops     (BAND BOR BXOR BLS BRS BURS)                       →  BitVecSort (widened)
 *    arithmetic ops  →  widened sort of operands (int32+int64→int64, int+FP→FP, etc.)
 *
 *  SymUnaryOp
 *    NOT              →  BoolSort
 *    NEG / PLUS / INC / DEC / COMPLIMENT  →  sort of operand
 *
 *  SymITE            →  sort of thenBranch (must match elseBranch)
 *
 *  SymArraySelect    →  value sort of the array (the range of ArraySort)
 *  SymArrayStore     →  same ArraySort as the base array
 *  SymFieldAccess    →  BitVecSort(32) default
 *
 * ── Design note ──────────────────────────────────────────────────────────────
 * IntSort has been removed from the integer domain entirely.  All Java integral
 * types are represented as BitVec so that bitwise operations, unsigned
 * comparisons, and overflow semantics are correct without any Int↔BV coercion.
 * The only remaining uses of IntSort are as the index sort for array theories
 * (Z3 array axioms require an unrestricted index domain) and for ObjectSymType /
 * reference types modelled as heap addresses.
 *
 * ── Caching ──────────────────────────────────────────────────────────────────
 * Uses an IdentityHashMap memo per resolve() call.  Interned nodes make this
 * an O(1) pointer lookup.
 */
public final class SortResolver {

    private final Context           ctx;
    private final Map<String, Sort> varSorts;   // variable name → Z3 Sort
    private final Map<String, SymType> varJavaTypes;

    // Pre-built sorts (allocated once per Context)
    final IntSort    intSort;   // kept for array-index and reference types ONLY
    final BoolSort   boolSort;
    final FPSort     fp32Sort;   // float  – IEEE 754 single
    final FPSort     fp64Sort;   // double – IEEE 754 double
    final BitVecSort bv8Sort;    // byte
    final BitVecSort bv16Sort;   // short / char
    final BitVecSort bv32Sort;   // int
    final BitVecSort bv64Sort;   // long
    final SeqSort<CharSort> stringSort; // java.lang.String

    // =========================================================================
    // Construction
    // =========================================================================

    /**
     * @param ctx          the Z3 Context (owns all Sort objects)
     * @param varSorts     caller-supplied map: variable name → Z3 Sort,
     *                     built from TypeContext before starting the encoding pass
     * @param varJavaTypes
     */
    public SortResolver(Context ctx, Map<String, Sort> varSorts, Map<String, SymType> varJavaTypes) {
        this.ctx      = ctx;
        this.varSorts = varSorts;
        this.varJavaTypes = varJavaTypes;

        this.intSort  = ctx.getIntSort();   // array indices / references only
        this.boolSort = ctx.getBoolSort();
        this.fp32Sort = ctx.mkFPSort32();
        this.fp64Sort = ctx.mkFPSort64();
        this.bv8Sort  = ctx.mkBitVecSort(8);
        this.bv16Sort = ctx.mkBitVecSort(16);
        this.bv32Sort = ctx.mkBitVecSort(32);
        this.bv64Sort = ctx.mkBitVecSort(64);
        this.stringSort = ctx.getStringSort();
    }

    // for test only
    @Deprecated
    public SortResolver(Context ctx, Map<String, Sort> varSorts) {
        this(ctx, varSorts, null);
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
                System.err.println("[SortResolver] WARNING: no sort for variable '"
                        + var.name() + "' – defaulting to bv32Sort");
                return bv32Sort;
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
        if (node instanceof SymBinaryOp b) {
            return resolveBinary(b, memo);
        }

        if (node instanceof SymStringOp stringOp) {
            return switch (stringOp.op()) {
                case EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, IS_EMPTY -> boolSort;
                case LENGTH, INDEX_OF                                    -> bv32Sort;
                case SUBSTRING, TO_LOWER_CASE, TO_UPPER_CASE,
                     TRIM, REPLACE                                       -> stringSort;
            };
        }

        // ── ITE ───────────────────────────────────────────────────────────
        if (node instanceof SymITE ite) {
            return resolve(ite.thenBranch(), memo);
        }

        // ── Arrays ────────────────────────────────────────────────────────
        if (node instanceof SymArraySelect s) {
            Sort arrSort = resolve(s.arr(), memo);
            if (arrSort instanceof ArraySort<?, ?> as) {
                return as.getRange();
            }
            throw new EncodingException(
                    "SymArraySelect: base expression is not an array sort", s);
        }

        if (node instanceof SymArrayStore st) {
            return resolve(st.arr(), memo);
        }

        // ── Cast: result sort is the declared target type ─────────────────────
        if (node instanceof SymCastOp cast) {
            // The target SymType is already on the record; convert it directly.
            // This avoids recursing into the operand, which may have a different sort,
            // and correctly handles both narrowing and widening casts (§5.1.2/§5.1.3).
            return symTypeToSort(cast.type());
        }

        // ── Field access ─────────────────────────────────────────────────────
        if (node instanceof SymFieldAccess f) {
            if (f.fieldType() != null && !(f.fieldType() instanceof UnknownSymType)) {
                return symTypeToSort(f.fieldType());
            }
            String key = fieldSortKey(f);
            if (key != null) {
                Sort s = varSorts.get(key);
                if (s != null) return s;
            }
            System.err.println("[SortResolver] WARNING: no sort for field access '"
                    + key + "' – defaulting to bv32Sort");
            return bv32Sort;
        }

        throw new IllegalArgumentException("Unsupported SymbolicValue: " + node);
    }

    private String fieldSortKey(SymFieldAccess f) {
        String recvKey = receiverSortKey(f.receiver());
        return recvKey == null ? null : recvKey + "__" + f.fieldName();
    }

    private String receiverSortKey(SymbolicValue receiver) {
        if (receiver instanceof SymVariable v)      return v.name();
        if (receiver instanceof SymFieldAccess fa)  return fieldSortKey(fa);
        return null;   // unsupported receiver shape (e.g. array element, ITE) — can't build a stable key
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

            case ADD -> {
                Sort ls = resolve(b.left(),  memo);
                Sort rs = resolve(b.right(), memo);
                if (isStringSort(ls) || isStringSort(rs)) {
                    yield stringSort;
                }
                yield widenArithmetic(ls, rs);
            }

            // Bitwise (non-shift) → symmetric widened BV sort
            case BAND, BOR, BXOR -> {
                Sort ls = resolve(b.left(),  memo);
                Sort rs = resolve(b.right(), memo);
                yield widenBitVec(ls, rs);
            }

            // Shift ops (JLS §15.19): result type = promoted type of LEFT operand only.
            // The right operand (shift distance) never widens the result.
            //   long << int  → bv64  (left is long)
            //   int  << long → bv32  (left is int;  long shift distance is masked to 6 bits but
            //                          the result sort is still int)
            case BLS, BRS, BURS -> resolve(b.left(), memo);

            // Arithmetic → widen operand sorts
            default -> {
                Sort ls = resolve(b.left(),  memo);
                Sort rs = resolve(b.right(), memo);
                if (isStringSort(ls) || isStringSort(rs)) {
                    throw new EncodingException(
                            "Op '" + b.op() + "' not supported for String operands", b);
                }
                yield widenArithmetic(ls, rs);
            }
        };
    }

    // =========================================================================
    // Sort widening helpers
    // =========================================================================

    /**
     * Arithmetic widening (mirrors Java's numeric promotion):
     *   double > float > long(bv64) > int(bv32)
     */
    Sort widenArithmetic(Sort a, Sort b) {
        if (a.equals(fp64Sort) || b.equals(fp64Sort)) return fp64Sort;
        if (a.equals(fp32Sort) || b.equals(fp32Sort)) return fp32Sort;
        if (a.equals(bv64Sort) || b.equals(bv64Sort)) return bv64Sort;
        return bv32Sort;
    }

    /**
     * Bitwise widening: promote both operands to the wider BV sort.
     * FP sorts treated as same width (fp32→bv32, fp64→bv64) for safety.
     */
    Sort widenBitVec(Sort a, Sort b) {
        boolean a64 = a.equals(bv64Sort) || a.equals(fp64Sort);
        boolean b64 = b.equals(bv64Sort) || b.equals(fp64Sort);
        return (a64 || b64) ? bv64Sort : bv32Sort;
    }

    // =========================================================================
    // SymType → Z3 Sort  (called by ConstraintSolver.buildSortMap)
    // =========================================================================

    public Sort symTypeToSort(SymType type) {
        if (type instanceof PrimitiveSymType p) {
            return switch (p) {
                case BYTE                   -> bv8Sort;
                case SHORT, CHAR            -> bv16Sort;
                case INT                    -> bv32Sort;
                case LONG                   -> bv64Sort;
                case FLOAT                  -> fp32Sort;
                case DOUBLE                 -> fp64Sort;
                case BOOLEAN                -> boolSort;
            };
        }
        if (type instanceof ArraySymType arrayType) {
            // Array indices stay IntSort (Z3 array theory).
            Sort range = symTypeToSort(arrayType.elementType());
            for (int i = 0; i < arrayType.dimensions(); i++) {
                range = ctx.mkArraySort(intSort, range);
            }
            return range;
        }
        if (type instanceof ObjectSymType objectType) {
            return isStringClass(objectType) ? stringSort : bv32Sort;
        }
        if (type instanceof NullSymType)   { return bv32Sort; }
        if (type instanceof VoidSymType)   { throw new IllegalArgumentException("void has no Z3 Sort"); }
        if (type instanceof UnknownSymType){ return bv32Sort; }
        if (type instanceof BottomSymType) { return bv32Sort; }
        throw new IllegalArgumentException("Unsupported SymType: " + type);
    }

    // =========================================================================
    // Literal sort helper
    // =========================================================================

    private Sort sortOfLiteral(Object value) {
        if (value instanceof Byte)      { return bv8Sort;  }
        if (value instanceof Short
                || value instanceof Character) { return bv16Sort; }
        if (value instanceof Integer)   { return bv32Sort; }
        if (value instanceof Long)    { return bv64Sort; }
        if (value instanceof Float)   { return fp32Sort; }
        if (value instanceof Double)  { return fp64Sort; }
        if (value instanceof Boolean) { return boolSort; }
        if (value instanceof String)  { return stringSort; }
        throw new IllegalArgumentException("Unknown literal type: " + value.getClass());
    }

    // =========================================================================
    // Accessors (used by Z3Encoder and ModelExtractor)
    // =========================================================================

    public Context    ctx()      { return ctx;      }
    public IntSort    intSort()  { return intSort;   }  // array indices / refs only
    public BoolSort   boolSort() { return boolSort;  }
    public FPSort     fp32Sort() { return fp32Sort;  }
    public FPSort     fp64Sort() { return fp64Sort;  }
    public BitVecSort bv8Sort()  { return bv8Sort;   }
    public BitVecSort bv16Sort() { return bv16Sort;  }
    public BitVecSort bv32Sort() { return bv32Sort;  }
    public BitVecSort bv64Sort() { return bv64Sort;  }
    public SeqSort<CharSort> stringSort() { return stringSort; }

    /**
     * Return the Java source {@link SymType} of a symbolic node if it can be
     * determined without traversal — used by Z3Encoder to choose sign- vs
     * zero-extension when coercing operands (JLS §5.1.2).
     *
     * <ul>
     *   <li>{@link SymVariable} → looked up in {@code varJavaTypes} map.
     *   <li>{@link SymLiteral}  → inferred from the literal's Java class
     *       ({@code Character} → {@code CHAR}, etc.).
     *   <li>All other nodes    → {@code null} (compound expression; its result
     *       is already int-promoted or wider, so the caller falls back to
     *       sign-extension, which is correct for non-char promoted types).
     * </ul>
     */
    public SymType javaTypeOf(SymbolicValue node) {
        if (node instanceof SymVariable var && varJavaTypes != null) {
            return varJavaTypes.get(var.name());   // may be null for unknown vars
        }
        if (node instanceof SymLiteral lit) {
            Object v = lit.value();
            if (v instanceof Character) return PrimitiveSymType.CHAR;
            if (v instanceof Byte)      return PrimitiveSymType.BYTE;
            if (v instanceof Short)     return PrimitiveSymType.SHORT;
            if (v instanceof Integer)   return PrimitiveSymType.INT;
            if (v instanceof Long)      return PrimitiveSymType.LONG;
            if (v instanceof Float)     return PrimitiveSymType.FLOAT;
            if (v instanceof Double)    return PrimitiveSymType.DOUBLE;
            if (v instanceof Boolean)   return PrimitiveSymType.BOOLEAN;
        }
        // Compound expression: result is already int-promoted; return null so
        // the caller uses sign-extension (the correct default for int and wider).
        return null;
    }

    boolean isStringSort(Sort sort) {
        return sort.equals(stringSort);
    }

    private boolean isStringClass(ObjectSymType type) {
        return "java.lang.String".equals(type.className()) || "String".equals(type.className());
    }
}