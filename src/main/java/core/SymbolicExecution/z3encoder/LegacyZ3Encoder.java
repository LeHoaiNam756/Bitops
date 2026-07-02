package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
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

import java.util.IdentityHashMap;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
final class LegacyZ3Encoder {

    private final Context ctx;
    private final LegacySortResolver sorts;

    LegacyZ3Encoder(LegacySortResolver sorts) {
        this.sorts = sorts;
        this.ctx = sorts.ctx();
    }

    List<BoolExpr> encodeAll(List<SymbolicValue> nodes) {
        IdentityHashMap<SymbolicValue, Expr<?>> exprCache = new IdentityHashMap<>();
        IdentityHashMap<SymbolicValue, Sort> sortCache = new IdentityHashMap<>();
        return nodes.stream()
                .map(node -> (BoolExpr) visit(node, exprCache, sortCache))
                .toList();
    }

    private Expr<?> visit(SymbolicValue node,
                          IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                          IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Expr<?> cached = exprCache.get(node);
        if (cached != null) return cached;
        
        Expr<?> result = encodeNode(node, exprCache, sortCache);
        exprCache.put(node, result);
        return result;
    }

    private Expr<?> encodeNode(SymbolicValue node,
                               IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                               IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (node instanceof SymLiteral lit) return encodeLiteral(lit);
        if (node instanceof SymVariable var) return ctx.mkConst(var.name(), sorts.resolve(var, sortCache));
        if (node instanceof SymUnaryOp unary) return encodeUnary(unary, exprCache, sortCache);
        if (node instanceof SymBinaryOp binary) return encodeBinary(binary, exprCache, sortCache);
        if (node instanceof SymITE ite) return encodeITE(ite, exprCache, sortCache);
        if (node instanceof SymArraySelect select) return encodeArraySelect(select, exprCache, sortCache);
        if (node instanceof SymArrayStore store) return encodeArrayStore(store, exprCache, sortCache);
        if (node instanceof SymFieldAccess field) return encodeFieldAccess(field, exprCache, sortCache);
        if (node instanceof SymStringOp) {
            throw new EncodingException("Legacy Original Concolic does not support String operations", node);
        }
        throw new EncodingException("Unsupported legacy symbolic node", node);
    }

    private Expr<?> encodeLiteral(SymLiteral lit) {
        Object value = lit.value();
        if (value instanceof Integer i) return ctx.mkInt(i);
        if (value instanceof Long l) return ctx.mkInt(Long.toString(l));
        if (value instanceof Short s) return ctx.mkInt(s);
        if (value instanceof Byte b) return ctx.mkInt(b);
        if (value instanceof Character c) return ctx.mkInt(c);
        if (value instanceof Float f) return ctx.mkReal(Float.toString(f));
        if (value instanceof Double d) return ctx.mkReal(Double.toString(d));
        if (value instanceof Boolean b) return ctx.mkBool(b);
        if (value instanceof String s) return ctx.mkString(s);
        throw new EncodingException("Unsupported legacy literal type: " + value.getClass(), lit);
    }

    private Expr<?> encodeUnary(SymUnaryOp unary,
                                IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Expr<?> operand = visit(unary.operand(), exprCache, sortCache);
        Sort operandSort = sorts.resolve(unary.operand(), sortCache);
        return switch (unary.op()) {
            case NOT -> ctx.mkNot((BoolExpr) operand);
            case PLUS -> operand;
            case NEG -> ctx.mkUnaryMinus(asArith(operand, unary));
            case INC -> ctx.mkAdd(asArith(operand, unary), oneFor(operandSort));
            case DEC -> ctx.mkSub(asArith(operand, unary), oneFor(operandSort));
            case COMPLIMENT -> ctx.mkUnaryMinus(ctx.mkAdd(asArith(operand, unary), oneFor(operandSort)));
        };
    }

    private Expr<?> encodeBinary(SymBinaryOp binary,
                                 IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                 IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (isBitwiseOrShift(binary.op())) {
            throw new EncodingException(
                    "Legacy Original Concolic does not support bitwise or shift operator " + binary.op(),
                    binary);
        }

        Sort resultSort = sorts.resolve(binary, sortCache);
        Sort leftSort = sorts.resolve(binary.left(), sortCache);
        Sort rightSort = sorts.resolve(binary.right(), sortCache);
        Expr<?> left = visit(binary.left(), exprCache, sortCache);
        Expr<?> right = visit(binary.right(), exprCache, sortCache);

        if (resultSort.equals(sorts.boolSort())) {
            return encodeBoolBinary(binary.op(), left, right, leftSort, rightSort, binary);
        }

        Sort target = numericTarget(leftSort, rightSort, binary);
        ArithExpr arithLeft = coerceToNumeric(left, leftSort, target, binary);
        ArithExpr arithRight = coerceToNumeric(right, rightSort, target, binary);
        return switch (binary.op()) {
            case ADD -> ctx.mkAdd(arithLeft, arithRight);
            case SUB -> ctx.mkSub(arithLeft, arithRight);
            case MUL -> ctx.mkMul(arithLeft, arithRight);
            case DIV -> ctx.mkDiv(arithLeft, arithRight);
            case MOD -> {
                if (!target.equals(sorts.intSort())) {
                    throw new EncodingException("Legacy modulo requires integer operands", binary);
                }
                yield ctx.mkMod((IntExpr) arithLeft, (IntExpr) arithRight);
            }
            default -> throw new EncodingException("Unsupported legacy arithmetic operator " + binary.op(), binary);
        };
    }

    private BoolExpr encodeBoolBinary(SymBinaryOp.Op op,
                                      Expr<?> left,
                                      Expr<?> right,
                                      Sort leftSort,
                                      Sort rightSort,
                                      SymBinaryOp source) {
        if (op == SymBinaryOp.Op.AND) return ctx.mkAnd((BoolExpr) left, (BoolExpr) right);
        if (op == SymBinaryOp.Op.OR) return ctx.mkOr((BoolExpr) left, (BoolExpr) right);
        if (op == SymBinaryOp.Op.EQ || op == SymBinaryOp.Op.NEQ) {
            BoolExpr eq = equality(left, right, leftSort, rightSort, source);
            return op == SymBinaryOp.Op.NEQ ? ctx.mkNot(eq) : eq;
        }

        Sort target = numericTarget(leftSort, rightSort, source);
        ArithExpr arithLeft = coerceToNumeric(left, leftSort, target, source);
        ArithExpr arithRight = coerceToNumeric(right, rightSort, target, source);
        return switch (op) {
            case SGT, UGT -> ctx.mkGt(arithLeft, arithRight);
            case SLT, ULT -> ctx.mkLt(arithLeft, arithRight);
            case SGE, UGE -> ctx.mkGe(arithLeft, arithRight);
            case SLE, ULE -> ctx.mkLe(arithLeft, arithRight);
            default -> throw new EncodingException("Unsupported legacy boolean operator " + op, source);
        };
    }

    private BoolExpr equality(Expr<?> left,
                              Expr<?> right,
                              Sort leftSort,
                              Sort rightSort,
                              SymBinaryOp source) {
        if (leftSort.equals(rightSort)) {
            return ctx.mkEq(left, right);
        }
        if (isNumeric(leftSort) && isNumeric(rightSort)) {
            Sort target = numericTarget(leftSort, rightSort, source);
            return ctx.mkEq(
                    coerceToNumeric(left, leftSort, target, source),
                    coerceToNumeric(right, rightSort, target, source));
        }
        throw new EncodingException(
                "Legacy equality cannot compare sorts " + leftSort + " and " + rightSort,
                source);
    }

    private Expr<?> encodeITE(SymITE ite,
                              IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                              IdentityHashMap<SymbolicValue, Sort> sortCache) {
        BoolExpr cond = (BoolExpr) visit(ite.cond(), exprCache, sortCache);
        Expr<?> thenExpr = visit(ite.thenBranch(), exprCache, sortCache);
        Expr<?> elseExpr = visit(ite.elseBranch(), exprCache, sortCache);
        Sort thenSort = sorts.resolve(ite.thenBranch(), sortCache);
        Sort elseSort = sorts.resolve(ite.elseBranch(), sortCache);

        if (!thenSort.equals(elseSort) && isNumeric(thenSort) && isNumeric(elseSort)) {
            Sort target = numericTarget(thenSort, elseSort, ite);
            thenExpr = coerceToNumeric(thenExpr, thenSort, target, ite);
            elseExpr = coerceToNumeric(elseExpr, elseSort, target, ite);
        }
        return ctx.mkITE(cond, thenExpr, elseExpr);
    }

    private Expr<?> encodeArraySelect(SymArraySelect select,
                                      IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                      IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Expr<?> arr = visit(select.arr(), exprCache, sortCache);
        Expr<?> index = visit(select.index(), exprCache, sortCache);
        return ctx.mkSelect((ArrayExpr) arr, asInt(index, select));
    }

    private Expr<?> encodeArrayStore(SymArrayStore store,
                                     IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                     IdentityHashMap<SymbolicValue, Sort> sortCache) {
        Expr<?> arr = visit(store.arr(), exprCache, sortCache);
        Expr<?> index = visit(store.index(), exprCache, sortCache);
        Expr<?> value = visit(store.value(), exprCache, sortCache);
        return ctx.mkStore((ArrayExpr) arr, asInt(index, store), value);
    }

    private Expr<?> encodeFieldAccess(SymFieldAccess field,
                                      IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
                                      IdentityHashMap<SymbolicValue, Sort> sortCache) {
        String name = receiverKey(field.receiver(), exprCache, sortCache) + "__" + field.fieldName();
        return ctx.mkConst(name, sorts.resolve(field, sortCache));
    }

    private String receiverKey(
            SymbolicValue receiver,
            IdentityHashMap<SymbolicValue, Expr<?>> exprCache,
            IdentityHashMap<SymbolicValue, Sort> sortCache) {
        if (receiver instanceof SymVariable variable) return variable.name();
        if (receiver instanceof SymFieldAccess field) {
            return receiverKey(field.receiver(), exprCache, sortCache) + "__" + field.fieldName();
        }
        return visit(receiver, exprCache, sortCache)
                .toString()
                .replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private ArithExpr oneFor(Sort sort) {
        return sort.equals(sorts.realSort()) ? ctx.mkReal("1") : ctx.mkInt(1);
    }

    private Sort numericTarget(Sort left, Sort right, SymbolicValue source) {
        if (!isNumeric(left) || !isNumeric(right)) {
            throw new EncodingException(
                    "Legacy numeric operation requires Int or Real operands, got " + left + " and " + right,
                    source);
        }
        return left.equals(sorts.realSort()) || right.equals(sorts.realSort())
                ? sorts.realSort()
                : sorts.intSort();
    }

    private ArithExpr coerceToNumeric(Expr<?> expr, Sort from, Sort target, SymbolicValue source) {
        if (!isNumeric(from) || !isNumeric(target)) {
            throw new EncodingException("Legacy coercion requires numeric sorts", source);
        }
        if (from.equals(target)) return asArith(expr, source);
        if (from.equals(sorts.intSort()) && target.equals(sorts.realSort())) {
            return ctx.mkInt2Real((IntExpr) expr);
        }
        throw new EncodingException("Legacy cannot coerce " + from + " to " + target, source);
    }

    private ArithExpr asArith(Expr<?> expr, SymbolicValue source) {
        if (expr instanceof ArithExpr arith) return arith;
        throw new EncodingException("Expected arithmetic expression, got " + expr.getSort(), source);
    }

    private IntExpr asInt(Expr<?> expr, SymbolicValue source) {
        if (expr instanceof IntExpr intExpr) return intExpr;
        throw new EncodingException("Expected integer expression, got " + expr.getSort(), source);
    }

    private boolean isNumeric(Sort sort) {
        return sort.equals(sorts.intSort()) || sort.equals(sorts.realSort());
    }

    private static boolean isBitwiseOrShift(SymBinaryOp.Op op) {
        return switch (op) {
            case BAND, BOR, BXOR, BLS, BRS, BURS -> true;
            default -> false;
        };
    }
}
