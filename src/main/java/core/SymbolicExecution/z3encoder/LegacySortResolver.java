package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BoolSort;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.RealSort;
import com.microsoft.z3.SeqSort;
import com.microsoft.z3.CharSort;
import com.microsoft.z3.Sort;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymITE;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymStringOp;
import core.SymbolicExecution.model.SymUnaryOp;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.ArraySymType;
import core.SymbolicExecution.model.types.BottomSymType;
import core.SymbolicExecution.model.types.NullSymType;
import core.SymbolicExecution.model.types.ObjectSymType;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.UnknownSymType;
import core.SymbolicExecution.model.types.VoidSymType;

import java.util.IdentityHashMap;
import java.util.Map;

final class LegacySortResolver {

    private final Context ctx;
    private final Map<String, SymType> varTypes;
    private final IntSort intSort;
    private final RealSort realSort;
    private final BoolSort boolSort;
    private final SeqSort<CharSort> stringSort;

    LegacySortResolver(Context ctx, Map<String, SymType> varTypes) {
        this.ctx = ctx;
        this.varTypes = varTypes == null ? Map.of() : Map.copyOf(varTypes);
        this.intSort = ctx.getIntSort();
        this.realSort = ctx.getRealSort();
        this.boolSort = ctx.getBoolSort();
        this.stringSort = ctx.getStringSort();
    }

    Sort resolve(SymbolicValue node) {
        return resolve(node, new IdentityHashMap<>());
    }

    Sort resolve(SymbolicValue node, IdentityHashMap<SymbolicValue, Sort> memo) {
        Sort cached = memo.get(node);
        if (cached != null) return cached;
        Sort result = compute(node, memo);
        memo.put(node, result);
        return result;
    }

    private Sort compute(SymbolicValue node, IdentityHashMap<SymbolicValue, Sort> memo) {
        if (node instanceof SymLiteral lit) {
            return sortOfLiteral(lit.value(), node);
        }
        if (node instanceof SymVariable var) {
            return symTypeToSort(varTypes.getOrDefault(var.name(), UnknownSymType.INSTANCE));
        }
        if (node instanceof SymUnaryOp unary) {
            return switch (unary.op()) {
                case NOT -> boolSort;
                case NEG, PLUS, INC, DEC, COMPLIMENT -> resolve(unary.operand(), memo);
                case LONG_NUMBER_OF_LEADING_ZEROS -> intSort;
            };
        }
        if (node instanceof SymBinaryOp binary) {
            return resolveBinary(binary, memo);
        }
        if (node instanceof SymITE ite) {
            return resolve(ite.thenBranch(), memo);
        }
        if (node instanceof SymArraySelect select) {
            Sort arrSort = resolve(select.arr(), memo);
            if (arrSort instanceof ArraySort<?, ?> arraySort) {
                return arraySort.getRange();
            }
            throw new EncodingException("Legacy array select requires an array sort", select);
        }
        if (node instanceof SymArrayStore store) {
            return resolve(store.arr(), memo);
        }
        if (node instanceof SymFieldAccess field) {
            if (field.fieldType() != null && !(field.fieldType() instanceof UnknownSymType)) {
                return symTypeToSort(field.fieldType());
            }
            return intSort;
        }
        if (node instanceof SymStringOp) {
            throw new EncodingException("Legacy Original Concolic does not support String operations", node);
        }
        throw new EncodingException("Unsupported legacy symbolic node", node);
    }

    private Sort resolveBinary(SymBinaryOp binary, IdentityHashMap<SymbolicValue, Sort> memo) {
        return switch (binary.op()) {
            case EQ, NEQ, SGT, SLT, SGE, SLE, UGT, UGE, ULT, ULE, AND, OR -> boolSort;
            case BAND, BOR, BXOR, BLS, BRS, BURS ->
                    throw new EncodingException(
                            "Legacy Original Concolic does not support bitwise or shift operator " + binary.op(),
                            binary);
            default -> {
                Sort left = resolve(binary.left(), memo);
                Sort right = resolve(binary.right(), memo);
                if (left.equals(realSort) || right.equals(realSort)) {
                    yield realSort;
                }
                if (left.equals(intSort) && right.equals(intSort)) {
                    yield intSort;
                }
                throw new EncodingException(
                        "Legacy arithmetic requires Int or Real operands, got " + left + " and " + right,
                        binary);
            }
        };
    }

    Sort symTypeToSort(SymType type) {
        if (type instanceof PrimitiveSymType primitive) {
            return switch (primitive) {
                case INT, LONG, SHORT, BYTE, CHAR -> intSort;
                case FLOAT, DOUBLE -> realSort;
                case BOOLEAN -> boolSort;
            };
        }
        if (type instanceof ArraySymType arrayType) {
            Sort range = symTypeToSort(arrayType.elementType());
            for (int i = 0; i < arrayType.dimensions(); i++) {
                range = ctx.mkArraySort(intSort, range);
            }
            return range;
        }
        if (type instanceof ObjectSymType objectType) {
            if ("java.lang.String".equals(objectType.className()) || "String".equals(objectType.className())) {
                return stringSort;
            }
            return intSort;
        }
        if (type instanceof NullSymType) return intSort;
        if (type instanceof UnknownSymType) return intSort;
        if (type instanceof BottomSymType) return intSort;
        if (type instanceof VoidSymType) throw new IllegalArgumentException("void has no Z3 Sort");
        return intSort;
    }

    private Sort sortOfLiteral(Object value, SymbolicValue source) {
        if (value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte
                || value instanceof Character) {
            return intSort;
        }
        if (value instanceof Float || value instanceof Double) {
            return realSort;
        }
        if (value instanceof Boolean) {
            return boolSort;
        }
        if (value instanceof String) {
            return stringSort;
        }
        throw new EncodingException("Unsupported legacy literal type: " + value.getClass(), source);
    }

    Context ctx() { return ctx; }
    IntSort intSort() { return intSort; }
    RealSort realSort() { return realSort; }
    BoolSort boolSort() { return boolSort; }
    SeqSort<CharSort> stringSort() { return stringSort; }
    Map<String, SymType> varTypes() { return varTypes; }
}
