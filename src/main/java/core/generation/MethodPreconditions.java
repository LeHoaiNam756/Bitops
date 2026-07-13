package core.generation;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.testdriver.TestDriver;
import core.utils.ConcolicLimits;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Caller-supplied method preconditions used to keep generated tests inside the
 * method's intended input domain.
 */
public final class MethodPreconditions {
    private static final MethodPreconditions NONE = builder().build();

    private final Map<String, Range> scalarRanges;
    private final Map<String, LengthRange> arrayLengthRanges;
    private final Set<String> nonNullParameters;
    private final Set<String> nullableParameters;
    private final List<Relation> relations;

    private MethodPreconditions(Builder builder) {
        this.scalarRanges = Map.copyOf(builder.scalarRanges);
        this.arrayLengthRanges = Map.copyOf(builder.arrayLengthRanges);
        this.nonNullParameters = Set.copyOf(builder.nonNullParameters);
        this.nullableParameters = Set.copyOf(builder.nullableParameters);
        this.relations = List.copyOf(builder.relations);
    }

    public static MethodPreconditions none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Integer> minimumArrayLengths() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, LengthRange> entry : arrayLengthRanges.entrySet()) {
            result.put(entry.getKey(), entry.getValue().min());
        }
        return result;
    }

    public List<SymbolicValue> symbolicConstraints() {
        List<SymbolicValue> constraints = new ArrayList<>();
        for (Map.Entry<String, Range> entry : scalarRanges.entrySet()) {
            SymVariable variable = new SymVariable(entry.getKey());
            Range range = entry.getValue();
            constraints.add(new SymBinaryOp(variable, SymBinaryOp.Op.SGE, literal(range.min())));
            constraints.add(new SymBinaryOp(variable, SymBinaryOp.Op.SLE, literal(range.max())));
        }
        for (Map.Entry<String, LengthRange> entry : arrayLengthRanges.entrySet()) {
            SymFieldAccess length = new SymFieldAccess(
                    new SymVariable(entry.getKey()), "length", PrimitiveSymType.INT);
            LengthRange range = entry.getValue();
            constraints.add(new SymBinaryOp(length, SymBinaryOp.Op.SGE, SymLiteral.of(range.min())));
            constraints.add(new SymBinaryOp(length, SymBinaryOp.Op.SLE, SymLiteral.of(range.max())));
        }
        for (Relation relation : relations) {
            constraints.add(new SymBinaryOp(
                    new SymVariable(relation.left()),
                    relation.operator().symbolicOp(),
                    new SymVariable(relation.right())));
        }
        return List.copyOf(constraints);
    }

    public Map<String, Object> apply(
            Map<String, Object> input,
            List<TestDriver.ParamInfo> paramInfos) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, TestDriver.ParamInfo> byName = new LinkedHashMap<>();
        for (TestDriver.ParamInfo param : paramInfos) {
            byName.put(param.name(), param);
            Object value = input.get(param.name());
            if (value == null && mustBeNonNull(param.name())) {
                value = defaultValue(param.typeName(), arrayLengthRanges.get(param.name()));
            }
            value = applyScalarRange(param, value);
            value = applyArrayLengthRange(param, value);
            result.put(param.name(), value);
        }

        for (Relation relation : relations) {
            applyRelation(result, byName, relation);
        }
        return result;
    }

    private boolean mustBeNonNull(String parameter) {
        return nonNullParameters.contains(parameter) || !nullableParameters.contains(parameter);
    }

    private Object applyScalarRange(TestDriver.ParamInfo param, Object value) {
        Range range = scalarRanges.get(param.name());
        if (range == null || !(value instanceof Number || value instanceof Character)) {
            return value;
        }
        long numeric = value instanceof Character c ? c : ((Number) value).longValue();
        long clamped = Math.max(range.min(), Math.min(range.max(), numeric));
        return coerceScalar(param.typeName(), clamped);
    }

    private Object applyArrayLengthRange(TestDriver.ParamInfo param, Object value) {
        LengthRange range = arrayLengthRanges.get(param.name());
        if (range == null) {
            return value;
        }
        if (value == null || !value.getClass().isArray()) {
            return defaultValue(param.typeName(), range);
        }

        int length = Array.getLength(value);
        int targetLength = Math.max(range.min(), Math.min(range.max(), length));
        if (targetLength == length) {
            return value;
        }
        Object resized = Array.newInstance(value.getClass().getComponentType(), targetLength);
        System.arraycopy(value, 0, resized, 0, Math.min(length, targetLength));
        return resized;
    }

    private void applyRelation(
            Map<String, Object> result,
            Map<String, TestDriver.ParamInfo> paramInfos,
            Relation relation) {
        Object leftValue = result.get(relation.left());
        Object rightValue = result.get(relation.right());
        if (!(leftValue instanceof Number || leftValue instanceof Character)
                || !(rightValue instanceof Number || rightValue instanceof Character)) {
            return;
        }
        long left = numericValue(leftValue);
        long right = numericValue(rightValue);
        if (relation.operator().test(left, right)) {
            return;
        }

        TestDriver.ParamInfo rightInfo = paramInfos.get(relation.right());
        TestDriver.ParamInfo leftInfo = paramInfos.get(relation.left());
        switch (relation.operator()) {
            case LT -> {
                if (canStore(relation.right(), rightInfo, left + 1)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left + 1));
                } else if (canStore(relation.left(), leftInfo, right - 1)) {
                    result.put(relation.left(), coerceScalar(leftInfo.typeName(), right - 1));
                }
            }
            case LE -> {
                if (canStore(relation.right(), rightInfo, left)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left));
                } else if (canStore(relation.left(), leftInfo, right)) {
                    result.put(relation.left(), coerceScalar(leftInfo.typeName(), right));
                }
            }
            case GT -> {
                if (canStore(relation.left(), leftInfo, right + 1)) {
                    result.put(relation.left(), coerceScalar(leftInfo.typeName(), right + 1));
                } else if (canStore(relation.right(), rightInfo, left - 1)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left - 1));
                }
            }
            case GE -> {
                if (canStore(relation.left(), leftInfo, right)) {
                    result.put(relation.left(), coerceScalar(leftInfo.typeName(), right));
                } else if (canStore(relation.right(), rightInfo, left)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left));
                }
            }
            case EQ -> {
                if (canStore(relation.right(), rightInfo, left)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left));
                }
            }
            case NE -> {
                if (canStore(relation.right(), rightInfo, left + 1)) {
                    result.put(relation.right(), coerceScalar(rightInfo.typeName(), left + 1));
                } else if (canStore(relation.left(), leftInfo, right - 1)) {
                    result.put(relation.left(), coerceScalar(leftInfo.typeName(), right - 1));
                }
            }
        }
    }

    private boolean canStore(String parameter, TestDriver.ParamInfo paramInfo, long value) {
        if (paramInfo == null) {
            return false;
        }
        Range range = scalarRanges.get(parameter);
        return (range == null || (value >= range.min() && value <= range.max()))
                && value >= typeMin(paramInfo.typeName())
                && value <= typeMax(paramInfo.typeName());
    }

    private static long numericValue(Object value) {
        return value instanceof Character c ? c : ((Number) value).longValue();
    }

    private static SymLiteral literal(long value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return SymLiteral.of((int) value);
        }
        return SymLiteral.of(value);
    }

    private static Object defaultValue(String typeName, LengthRange lengthRange) {
        String type = typeName.replace(" ", "");
        int length = lengthRange == null ? 0 : lengthRange.min();
        return switch (type) {
            case "boolean" -> false;
            case "byte" -> (byte) 0;
            case "short" -> (short) 0;
            case "char" -> '\0';
            case "int" -> 0;
            case "long" -> 0L;
            case "float" -> 0.0f;
            case "double" -> 0.0;
            case "String", "java.lang.String" -> "";
            case "boolean[]" -> new boolean[length];
            case "byte[]" -> new byte[length];
            case "short[]" -> new short[length];
            case "char[]" -> new char[length];
            case "int[]" -> new int[length];
            case "long[]" -> new long[length];
            case "float[]" -> new float[length];
            case "double[]" -> new double[length];
            case "String[]", "java.lang.String[]" -> new String[length];
            default -> null;
        };
    }

    private static Object coerceScalar(String typeName, long value) {
        return switch (typeName.replace(" ", "")) {
            case "byte" -> (byte) value;
            case "short" -> (short) value;
            case "char" -> (char) value;
            case "int" -> (int) value;
            case "long" -> value;
            case "float" -> (float) value;
            case "double" -> (double) value;
            default -> value;
        };
    }

    private static long typeMin(String typeName) {
        return switch (typeName.replace(" ", "")) {
            case "byte" -> Byte.MIN_VALUE;
            case "short" -> Short.MIN_VALUE;
            case "char" -> Character.MIN_VALUE;
            case "int" -> Integer.MIN_VALUE;
            case "long" -> Long.MIN_VALUE;
            default -> Long.MIN_VALUE;
        };
    }

    private static long typeMax(String typeName) {
        return switch (typeName.replace(" ", "")) {
            case "byte" -> Byte.MAX_VALUE;
            case "short" -> Short.MAX_VALUE;
            case "char" -> Character.MAX_VALUE;
            case "int" -> Integer.MAX_VALUE;
            case "long" -> Long.MAX_VALUE;
            default -> Long.MAX_VALUE;
        };
    }

    private record Range(long min, long max) {}

    private record LengthRange(int min, int max) {}

    private record Relation(String left, Operator operator, String right) {}

    public enum Operator {
        LT(SymBinaryOp.Op.SLT) {
            @Override boolean test(long left, long right) { return left < right; }
        },
        LE(SymBinaryOp.Op.SLE) {
            @Override boolean test(long left, long right) { return left <= right; }
        },
        GT(SymBinaryOp.Op.SGT) {
            @Override boolean test(long left, long right) { return left > right; }
        },
        GE(SymBinaryOp.Op.SGE) {
            @Override boolean test(long left, long right) { return left >= right; }
        },
        EQ(SymBinaryOp.Op.EQ) {
            @Override boolean test(long left, long right) { return left == right; }
        },
        NE(SymBinaryOp.Op.NEQ) {
            @Override boolean test(long left, long right) { return left != right; }
        };

        private final SymBinaryOp.Op symbolicOp;

        Operator(SymBinaryOp.Op symbolicOp) {
            this.symbolicOp = symbolicOp;
        }

        SymBinaryOp.Op symbolicOp() {
            return symbolicOp;
        }

        abstract boolean test(long left, long right);
    }

    public static final class Builder {
        private final Map<String, Range> scalarRanges = new LinkedHashMap<>();
        private final Map<String, LengthRange> arrayLengthRanges = new LinkedHashMap<>();
        private final Set<String> nonNullParameters = new LinkedHashSet<>();
        private final Set<String> nullableParameters = new LinkedHashSet<>();
        private final List<Relation> relations = new ArrayList<>();

        private Builder() {}

        public Builder range(String parameter, long min, long max) {
            Objects.requireNonNull(parameter, "parameter");
            if (max < min) {
                throw new IllegalArgumentException("max must be >= min");
            }
            scalarRanges.put(parameter, new Range(min, max));
            return this;
        }

        public Builder arrayLengthAtLeast(String parameter, int min) {
            return arrayLengthRange(parameter, min, ConcolicLimits.maxGeneratedArrayLength());
        }

        public Builder arrayLengthRange(String parameter, int min, int max) {
            Objects.requireNonNull(parameter, "parameter");
            if (min < 0) {
                throw new IllegalArgumentException("min length must be >= 0");
            }
            if (max < min) {
                throw new IllegalArgumentException("max length must be >= min length");
            }
            arrayLengthRanges.put(parameter, new LengthRange(min, max));
            return this;
        }

        public Builder nonNull(String parameter) {
            nonNullParameters.add(Objects.requireNonNull(parameter, "parameter"));
            nullableParameters.remove(parameter);
            return this;
        }

        public Builder nullable(String parameter) {
            nullableParameters.add(Objects.requireNonNull(parameter, "parameter"));
            nonNullParameters.remove(parameter);
            return this;
        }

        public Builder invariant(String left, Operator operator, String right) {
            relations.add(new Relation(
                    Objects.requireNonNull(left, "left"),
                    Objects.requireNonNull(operator, "operator"),
                    Objects.requireNonNull(right, "right")));
            return this;
        }

        public Builder lessThan(String left, String right) {
            return invariant(left, Operator.LT, right);
        }

        public Builder lessOrEqual(String left, String right) {
            return invariant(left, Operator.LE, right);
        }

        public Builder greaterThan(String left, String right) {
            return invariant(left, Operator.GT, right);
        }

        public Builder greaterOrEqual(String left, String right) {
            return invariant(left, Operator.GE, right);
        }

        public Builder equal(String left, String right) {
            return invariant(left, Operator.EQ, right);
        }

        public Builder notEqual(String left, String right) {
            return invariant(left, Operator.NE, right);
        }

        public MethodPreconditions build() {
            return new MethodPreconditions(this);
        }
    }
}
