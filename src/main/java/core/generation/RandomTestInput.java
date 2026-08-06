package core.generation;

import core.utils.ConcolicLimits;
import core.utils.Setup;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomTestInput {

    private RandomTestInput() {}

    /**
     * Creates random test data for each parameter of the given method.
     *
     * @param methodDeclaration the Eclipse JDT {@link MethodDeclaration} whose
     *                          parameters drive data generation.
     * @return a {@link LinkedHashMap} preserving parameter order, mapping each
     *         parameter name to a randomly generated value compatible with its
     *         declared type.
     */
    public static Map<String, Object> createRandomTestData(MethodDeclaration methodDeclaration) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters =
                (List<SingleVariableDeclaration>) methodDeclaration.parameters();

        Map<String, Object> result = new LinkedHashMap<>();
        for (SingleVariableDeclaration param : parameters) {
            String name = param.getName().getIdentifier();
            int minimumArrayLength = minimumArrayLength(methodDeclaration, name);
            Object value = createRandomValueForType(
                    param.getType(), param.getExtraDimensions(), minimumArrayLength);
            result.put(name, value);
        }
        return result;
    }

    /**
     * Creates the normal seed plus a bounded set of array-traversal seeds.
     *
     * <p>For methods with one bounded array index and two {@code int}
     * parameters, the additional cases exercise values around the inferred
     * array length and values obtained by scaling that length with constants
     * used by the method. This supplies concrete witnesses for loop-heavy code
     * when symbolic execution cannot model the loop mutation precisely.</p>
     */
    public static List<Map<String, Object>> createConcolicSeedData(
            MethodDeclaration methodDeclaration) {
        Map<String, Object> randomSeed = createRandomTestData(methodDeclaration);
        List<Map<String, Object>> seeds = new ArrayList<>();
        seeds.add(randomSeed);

        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
        List<String> intParameters = new ArrayList<>();
        int traversalLength = 0;
        for (SingleVariableDeclaration parameter : parameters) {
            String name = parameter.getName().getIdentifier();
            if (isIntScalar(parameter)) {
                intParameters.add(name);
            }
            if (parameter.getExtraDimensions() > 0 || parameter.getType().isArrayType()) {
                traversalLength = Math.max(
                        traversalLength,
                        minimumArrayLength(methodDeclaration, name));
            }
        }
        addByteArrayBitPatternSeeds(methodDeclaration, randomSeed, seeds);
        if (traversalLength <= 0 || intParameters.size() != 2) {
            return List.copyOf(seeds);
        }
        final int inferredTraversalLength = traversalLength;

        LinkedHashSet<Integer> upperValues = new LinkedHashSet<>();
        upperValues.add(1);
        upperValues.add(2);
        upperValues.add(inferredTraversalLength);
        if (inferredTraversalLength < Integer.MAX_VALUE) {
            upperValues.add(inferredTraversalLength + 1);
        }
        includeScaledValue(upperValues, inferredTraversalLength, 2);
        methodDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(NumberLiteral node) {
                Integer constant = constantInt(node);
                if (constant != null && constant > 0) {
                    includeScaledValue(upperValues, inferredTraversalLength, constant);
                }
                return true;
            }
        });

        String lowerParameter = intParameters.get(0);
        String upperParameter = intParameters.get(1);
        for (int upper : upperValues) {
            if (seeds.size() >= 16) break;
            Map<String, Object> seed = new LinkedHashMap<>(randomSeed);
            seed.put(lowerParameter, 0);
            seed.put(upperParameter, upper);
            seeds.add(seed);
        }

        // A non-zero lower value is needed to enter do/while-style partial
        // traversals such as an array suffix ending at traversalLength.
        Map<String, Object> partialTraversal = new LinkedHashMap<>(randomSeed);
        partialTraversal.put(lowerParameter, 1);
        partialTraversal.put(upperParameter, inferredTraversalLength);
        seeds.add(partialTraversal);
        return List.copyOf(seeds);
    }

    private static void addByteArrayBitPatternSeeds(
            MethodDeclaration methodDeclaration,
            Map<String, Object> baseSeed,
            List<Map<String, Object>> seeds) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
        for (SingleVariableDeclaration parameter : parameters) {
            if (!isOneDimensionalPrimitiveArray(parameter, PrimitiveType.BYTE)) {
                continue;
            }
            String name = parameter.getName().getIdentifier();
            int length = Math.max(1, minimumArrayLength(methodDeclaration, name));
            addBytePatternSeed(seeds, baseSeed, name, length, (byte) 0x00);
            addBytePatternSeed(seeds, baseSeed, name, length, (byte) 0x01);
            addBytePatternSeed(seeds, baseSeed, name, length, (byte) 0x03);
            addBytePatternSeed(seeds, baseSeed, name, length, (byte) 0x7f);
            addBytePatternSeed(seeds, baseSeed, name, length, (byte) 0xff);
            addAlternatingBytePatternSeed(seeds, baseSeed, name, length);
        }
    }

    private static boolean isOneDimensionalPrimitiveArray(
            SingleVariableDeclaration parameter,
            PrimitiveType.Code expectedElementType) {
        int dimensions = parameter.getExtraDimensions();
        Type type = parameter.getType();
        if (type instanceof ArrayType arrayType) {
            dimensions += arrayType.getDimensions();
            type = arrayType.getElementType();
        }
        if (dimensions != 1 || !type.isPrimitiveType()) {
            return false;
        }
        PrimitiveType.Code code = ((PrimitiveType) type).getPrimitiveTypeCode();
        return expectedElementType.equals(code);
    }

    private static void addBytePatternSeed(
            List<Map<String, Object>> seeds,
            Map<String, Object> baseSeed,
            String parameterName,
            int length,
            byte value) {
        Map<String, Object> seed = new LinkedHashMap<>(baseSeed);
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, value);
        seed.put(parameterName, bytes);
        seeds.add(seed);
    }

    private static void addAlternatingBytePatternSeed(
            List<Map<String, Object>> seeds,
            Map<String, Object> baseSeed,
            String parameterName,
            int length) {
        Map<String, Object> seed = new LinkedHashMap<>(baseSeed);
        byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((i & 1) == 0 ? 0x55 : 0xaa);
        }
        seed.put(parameterName, bytes);
        seeds.add(seed);
    }

    private static boolean isIntScalar(SingleVariableDeclaration parameter) {
        if (parameter.getExtraDimensions() > 0 || !parameter.getType().isPrimitiveType()) {
            return false;
        }
        PrimitiveType.Code code =
                ((PrimitiveType) parameter.getType()).getPrimitiveTypeCode();
        return PrimitiveType.INT.equals(code);
    }

    private static void includeScaledValue(
            Set<Integer> values, int traversalLength, int multiplier) {
        long scaled = (long) traversalLength * multiplier;
        if (scaled > 0 && scaled <= Integer.MAX_VALUE) {
            values.add((int) scaled);
        }
    }

    /**
     * Creates a bounded boundary-value portfolio without a Cartesian-product
     * explosion. Parameters advance through their own boundary lists together,
     * producing at most five seed inputs for one method.
     */
    public static List<Map<String, Object>> createBoundaryTestData(
            MethodDeclaration methodDeclaration) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
        if (parameters.isEmpty()) {
            return List.of(Map.of());
        }

        List<List<Object>> valuesByParameter = new ArrayList<>(parameters.size());
        int scenarioCount = 1;
        for (SingleVariableDeclaration parameter : parameters) {
            List<Object> values = boundaryValuesForType(
                    parameter.getType(), parameter.getExtraDimensions());
            valuesByParameter.add(values);
            scenarioCount = Math.max(scenarioCount, values.size());
        }
        scenarioCount = Math.min(scenarioCount, 5);

        List<Map<String, Object>> seeds = new ArrayList<>(scenarioCount);
        for (int scenario = 0; scenario < scenarioCount; scenario++) {
            Map<String, Object> seed = new LinkedHashMap<>();
            for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
                List<Object> values = valuesByParameter.get(parameterIndex);
                int valueIndex = boundaryValueIndex(
                        scenario, parameterIndex, scenarioCount, values.size());
                Object value = values.get(valueIndex);
                seed.put(parameters.get(parameterIndex).getName().getIdentifier(), value);
            }
            seeds.add(seed);
        }
        return List.copyOf(seeds);
    }

    private static int boundaryValueIndex(
            int scenario, int parameterIndex, int scenarioCount, int valueCount) {
        // Numeric primitive portfolios contain five values: 0, 1, -1, MIN, MAX.
        // Rotate later parameters so extrema are exercised against different
        // values instead of only (MIN, MIN) and (MAX, MAX). This remains bounded
        // at five scenarios while adding witnesses such as (MIN, -1), which is
        // required for signed-multiplication overflow.
        if (scenarioCount == 5 && valueCount == 5 && parameterIndex > 0) {
            int rotation = parameterIndex % valueCount;
            return Math.floorMod(scenario - rotation, valueCount);
        }
        return Math.min(scenario, valueCount - 1);
    }

    private static List<Object> boundaryValuesForType(Type type, int extraDimensions) {
        if (extraDimensions > 0 || type.isArrayType()) {
            return boundaryArrayValues(type, extraDimensions);
        }
        if (isStringType(type)) {
            return List.of("", "a");
        }
        if (!type.isPrimitiveType()) {
            return List.of(createRandomValueForType(type, extraDimensions));
        }

        PrimitiveType.Code code = ((PrimitiveType) type).getPrimitiveTypeCode();
        if (PrimitiveType.BOOLEAN.equals(code)) {
            return List.of(false, true);
        } else if (PrimitiveType.BYTE.equals(code)) {
            return List.of((byte) 0, (byte) 1, (byte) -1, Byte.MIN_VALUE, Byte.MAX_VALUE);
        } else if (PrimitiveType.SHORT.equals(code)) {
            return List.of((short) 0, (short) 1, (short) -1, Short.MIN_VALUE, Short.MAX_VALUE);
        } else if (PrimitiveType.CHAR.equals(code)) {
            return List.of((char) 0, (char) 1, 'a', Character.MAX_VALUE);
        } else if (PrimitiveType.INT.equals(code)) {
            return List.of(0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else if (PrimitiveType.LONG.equals(code)) {
            return List.of(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE);
        } else if (PrimitiveType.FLOAT.equals(code)) {
            return List.of(0.0f, 1.0f, -1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
        } else if (PrimitiveType.DOUBLE.equals(code)) {
            return List.of(0.0, 1.0, -1.0, -Double.MAX_VALUE, Double.MAX_VALUE);
        }
        return List.of(createRandomPrimitiveValue(code));
    }

    private static List<Object> boundaryArrayValues(Type type, int extraDimensions) {
        Type baseType = type;
        int dimensions = extraDimensions;
        if (type instanceof ArrayType arrayType) {
            baseType = arrayType.getElementType();
            dimensions += arrayType.getDimensions();
        }

        Class<?> componentType;
        if (baseType.isPrimitiveType()) {
            componentType = primitiveCodeToClass(
                    ((PrimitiveType) baseType).getPrimitiveTypeCode());
        } else if (isStringType(baseType)) {
            componentType = String.class;
        } else {
            return List.of(createRandomValueForType(type, extraDimensions));
        }

        int[] emptyDimensions = new int[dimensions];
        int[] singletonDimensions = new int[dimensions];
        Arrays.fill(singletonDimensions, 1);
        return List.of(
                Array.newInstance(componentType, emptyDimensions),
                Array.newInstance(componentType, singletonDimensions));
    }

    // -------------------------------------------------------------------------
    // Type dispatch
    // -------------------------------------------------------------------------

    /**
     * Generates a random value for a JDT {@link Type} node.
     *
     * @param type            the declared type of the parameter/element.
     * @param extraDimensions extra array dimensions written after the name,
     *                        e.g. {@code int a[]} has extraDimensions = 1.
     */
    private static Object createRandomValueForType(Type type, int extraDimensions) {
        return createRandomValueForType(type, extraDimensions, 0);
    }

    private static Object createRandomValueForType(
            Type type, int extraDimensions, int minimumArrayLength) {
        // Extra dimensions (e.g. "int a[]") wrap the base type in array layers.
        if (extraDimensions > 0) {
            return createRandomArrayValue(type, extraDimensions, minimumArrayLength);
        }

        if (type.isArrayType()) {
            return createRandomArrayValue((ArrayType) type, minimumArrayLength);
        }

        if (type.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType) type;
            return createRandomPrimitiveValue(primitiveType.getPrimitiveTypeCode());
        }

        if (isStringType(type)) {
            return createRandomStringValue();
        }

        throw new RuntimeException("Unsupported parameter type: " + type);
    }

    // -------------------------------------------------------------------------
    // Array generation
    // -------------------------------------------------------------------------

    /** Handles a proper {@link ArrayType} node (e.g. {@code int[]}, {@code int[][]}). */
    private static Object createRandomArrayValue(ArrayType arrayType) {
        return createRandomArrayValue(arrayType, 0);
    }

    private static Object createRandomArrayValue(
            ArrayType arrayType, int minimumArrayLength) {
        // Peel off one dimension level; generate elements for the element type.
        Type elementType = arrayType.getElementType();
        int dimensions = arrayType.getDimensions();
        return createRandomArrayValue(elementType, dimensions, minimumArrayLength);
    }

    /**
     * Recursively creates a multi-dimensional array whose element type is
     * described by {@code baseType} and whose depth is {@code dimensions}.
     */
    private static Object createRandomArrayValue(Type baseType, int dimensions) {
        return createRandomArrayValue(baseType, dimensions, 0);
    }

    private static Object createRandomArrayValue(
            Type baseType, int dimensions, int minimumArrayLength) {
        Random random = new Random();
        int length = Math.max(minimumArrayLength, 1 + random.nextInt(5));

        if (dimensions == 1) {
            // Base case: 1-D array of primitives or String.
            if (isStringType(baseType)) {
                String[] array = new String[length];
                for (int i = 0; i < length; i++) {
                    array[i] = createRandomStringValue();
                }
                return array;
            }

            if (!baseType.isPrimitiveType()) {
                throw new RuntimeException(
                        "Unsupported array element type (only primitives and String supported): " + baseType);
            }
            PrimitiveType.Code code = ((PrimitiveType) baseType).getPrimitiveTypeCode();
            Class<?> componentClass = primitiveCodeToClass(code);
            Object array = Array.newInstance(componentClass, length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, createRandomPrimitiveValue(code));
            }
            return array;
        }

        // Recursive case: array of arrays.
        // We need a representative element to determine the component class.
        Object sample = createRandomArrayValue(baseType, dimensions - 1, 0);
        Object array = Array.newInstance(sample.getClass(), length);
        Array.set(array, 0, sample);
        for (int i = 1; i < length; i++) {
            Array.set(array, i, createRandomArrayValue(baseType, dimensions - 1, 0));
        }
        return array;
    }

    // -------------------------------------------------------------------------
    // Primitive generation
    // -------------------------------------------------------------------------

//    private static Object createRandomPrimitiveValue(PrimitiveType.Code code) {
//        Random random = new Random();
//
//        if (PrimitiveType.INT.equals(code)) {
//            int min = Setup.intMin;
//            int max = Setup.intMax;
//            validateBounds(min, max, "int");
//            return min + random.nextInt((max - min) + 1);
//        } else if (PrimitiveType.BOOLEAN.equals(code)) {
//            return random.nextBoolean();
//        } else if (PrimitiveType.BYTE.equals(code)) {
//            byte[] bytes = new byte[1];
//            random.nextBytes(bytes);
//            return bytes[0];
//
//        } else if (PrimitiveType.SHORT.equals(code)) {
//            return (short) random.nextInt();
//
//        } else if (PrimitiveType.CHAR.equals(code)) {
//            return (char) random.nextInt(Character.MAX_VALUE + 1);
//
//        } else if (PrimitiveType.LONG.equals(code)) {
//            return random.nextLong();
//
//        } else if (PrimitiveType.FLOAT.equals(code)) {
//            float min = Setup.floatMin;
//            float max = Setup.floatMax;
//            validateBounds(min, max, "float");
//            return min + random.nextFloat() * (max - min);
//
//        } else if (PrimitiveType.DOUBLE.equals(code)) {
//            double min = Setup.doubleMin;
//            double max = Setup.doubleMax;
//            validateBounds(min, max, "double");
//            return min + random.nextDouble() * (max - min);
//
//        } else if (PrimitiveType.VOID.equals(code)) {
//            return null;
//        }
//
//        throw new RuntimeException("Unsupported primitive type code: " + code);
//    }

    private static Object createRandomPrimitiveValue(PrimitiveType.Code code) {
        if (PrimitiveType.INT.equals(code)) {
            return 8;
        } else if (PrimitiveType.BOOLEAN.equals(code)) {
            return true;
        } else if (PrimitiveType.BYTE.equals(code)) {
            return (byte) 8;
        } else if (PrimitiveType.SHORT.equals(code)) {
            return (short) 8;
        } else if (PrimitiveType.CHAR.equals(code)) {
            return 'x';
        } else if (PrimitiveType.LONG.equals(code)) {
            return 8L;
        } else if (PrimitiveType.FLOAT.equals(code)) {
            return 8.0f;
        } else if (PrimitiveType.DOUBLE.equals(code)) {
            return 8.0;
        } else if (PrimitiveType.VOID.equals(code)) {
            return null;
        }

        throw new RuntimeException("Unsupported primitive type code: " + code);
    }

    private static String createRandomStringValue() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz"
                + "0123456789"
                + "!@#$%^&*()_+=-[]{}|;:',.<>?/";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(5,21);
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            stringBuilder.append(characters.charAt(index));
        }
        return stringBuilder.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Finds the largest constant index used on a parameter in this method.
     * A seed that is shorter than this cannot reach the first real branch.
     */
    static int minimumArrayLength(MethodDeclaration method, String parameterName) {
        int[] minimum = {0};
        Set<String> indexVariables = new HashSet<>();
        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(ArrayAccess node) {
                if (!parameterName.equals(arrayBaseName(node.getArray()))) {
                    return true;
                }
                Integer index = constantInt(node.getIndex());
                if (index != null && index >= 0 && index < Integer.MAX_VALUE) {
                    includeRequiredLength(minimum, (long) index + 1);
                }
                indexVariables.addAll(indexVariableNames(node.getIndex()));
                return true;
            }
        });

        // Also account for variable indices with a constant upper bound, for
        // example table[i] in a loop guarded by i < 64. Without this, Z3 is
        // free to choose a zero-length array even for a path that executes the
        // indexed statement, and the concrete run terminates before the target.
        method.accept(new ASTVisitor() {
            @Override
            public boolean visit(InfixExpression node) {
                InfixExpression.Operator operator = node.getOperator();
                String leftVariable = indexVariableName(node.getLeftOperand());
                String rightVariable = indexVariableName(node.getRightOperand());
                Integer leftConstant = constantInt(node.getLeftOperand());
                Integer rightConstant = constantInt(node.getRightOperand());

                if (indexVariables.contains(leftVariable) && rightConstant != null) {
                    if (operator == InfixExpression.Operator.LESS) {
                        includeRequiredLength(minimum, rightConstant);
                    } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
                        includeRequiredLength(minimum, (long) rightConstant + 1);
                    }
                } else if (indexVariables.contains(rightVariable) && leftConstant != null) {
                    if (operator == InfixExpression.Operator.GREATER) {
                        includeRequiredLength(minimum, leftConstant);
                    } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
                        includeRequiredLength(minimum, (long) leftConstant + 1);
                    }
                }
                return true;
            }
        });
        return minimum[0];
    }

    private static void includeRequiredLength(int[] minimum, long required) {
        if (required <= 0) return;
        int bounded = (int) Math.min(required, ConcolicLimits.maxGeneratedArrayLength());
        minimum[0] = Math.max(minimum[0], bounded);
    }

    private static String indexVariableName(Expression expression) {
        Expression current = expression;
        while (current instanceof ParenthesizedExpression parenthesized) {
            current = parenthesized.getExpression();
        }
        if (current instanceof PostfixExpression postfix) {
            current = postfix.getOperand();
        }
        return current instanceof SimpleName name ? name.getIdentifier() : null;
    }

    private static Set<String> indexVariableNames(Expression expression) {
        Set<String> names = new HashSet<>();
        expression.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                names.add(node.getIdentifier());
                return false;
            }
        });
        return names;
    }

    private static String arrayBaseName(Expression expression) {
        Expression current = expression;
        while (current instanceof ArrayAccess nested) {
            current = nested.getArray();
        }
        return current instanceof SimpleName name ? name.getIdentifier() : null;
    }

    private static Integer constantInt(Expression expression) {
        Object constant = expression.resolveConstantExpressionValue();
        if (constant instanceof Number number) {
            long value = number.longValue();
            return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE
                    ? (int) value
                    : null;
        }
        if (expression instanceof NumberLiteral literal) {
            try {
                return Integer.decode(literal.getToken().replace("_", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Maps a JDT {@link PrimitiveType.Code} to its Java {@link Class}. */
    private static Class<?> primitiveCodeToClass(PrimitiveType.Code code) {
        if (PrimitiveType.INT.equals(code))     return int.class;
        if (PrimitiveType.BOOLEAN.equals(code)) return boolean.class;
        if (PrimitiveType.BYTE.equals(code))    return byte.class;
        if (PrimitiveType.SHORT.equals(code))   return short.class;
        if (PrimitiveType.CHAR.equals(code))    return char.class;
        if (PrimitiveType.LONG.equals(code))    return long.class;
        if (PrimitiveType.FLOAT.equals(code))   return float.class;
        if (PrimitiveType.DOUBLE.equals(code))  return double.class;
        throw new RuntimeException("Unsupported primitive type code: " + code);
    }

    private static boolean isStringType(Type type) {
        if (type instanceof SimpleType simpleType) {
            String name = simpleType.getName().getFullyQualifiedName();
            return "String".equals(name) || "java.lang.String".equals(name);
        }
        return false;
    }

    private static void validateBounds(double min, double max, String typeName) {
        if (max < min) {
            throw new RuntimeException(
                    "Invalid " + typeName + " bounds in Setup: max (" + max + ") < min (" + min + ")");
        }
    }
}
