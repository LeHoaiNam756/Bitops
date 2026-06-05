package core.generation;

import core.utils.Setup;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
            Object value = createRandomValueForType(param.getType(), param.getExtraDimensions());
            result.put(name, value);
        }
        return result;
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
        // Extra dimensions (e.g. "int a[]") wrap the base type in array layers.
        if (extraDimensions > 0) {
            return createRandomArrayValue(type, extraDimensions);
        }

        if (type.isArrayType()) {
            return createRandomArrayValue((ArrayType) type);
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
        // Peel off one dimension level; generate elements for the element type.
        Type elementType = arrayType.getElementType();
        int dimensions = arrayType.getDimensions();
        return createRandomArrayValue(elementType, dimensions);
    }

    /**
     * Recursively creates a multi-dimensional array whose element type is
     * described by {@code baseType} and whose depth is {@code dimensions}.
     */
    private static Object createRandomArrayValue(Type baseType, int dimensions) {
        Random random = new Random();
        int length = 1 + random.nextInt(5);

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
        Object sample = createRandomArrayValue(baseType, dimensions - 1);
        Object array = Array.newInstance(sample.getClass(), length);
        Array.set(array, 0, sample);
        for (int i = 1; i < length; i++) {
            Array.set(array, i, createRandomArrayValue(baseType, dimensions - 1));
        }
        return array;
    }

    // -------------------------------------------------------------------------
    // Primitive generation
    // -------------------------------------------------------------------------

    private static Object createRandomPrimitiveValue(PrimitiveType.Code code) {
        Random random = new Random();

        if (PrimitiveType.INT.equals(code)) {
            int min = Setup.intMin;
            int max = Setup.intMax;
            validateBounds(min, max, "int");
            return min + random.nextInt((max - min) + 1);

        } else if (PrimitiveType.BOOLEAN.equals(code)) {
            return random.nextBoolean();

        } else if (PrimitiveType.BYTE.equals(code)) {
            byte[] bytes = new byte[1];
            random.nextBytes(bytes);
            return bytes[0];

        } else if (PrimitiveType.SHORT.equals(code)) {
            return (short) random.nextInt();

        } else if (PrimitiveType.CHAR.equals(code)) {
            return (char) random.nextInt(Character.MAX_VALUE + 1);

        } else if (PrimitiveType.LONG.equals(code)) {
            return random.nextLong();

        } else if (PrimitiveType.FLOAT.equals(code)) {
            float min = Setup.floatMin;
            float max = Setup.floatMax;
            validateBounds(min, max, "float");
            return min + random.nextFloat() * (max - min);

        } else if (PrimitiveType.DOUBLE.equals(code)) {
            double min = Setup.doubleMin;
            double max = Setup.doubleMax;
            validateBounds(min, max, "double");
            return min + random.nextDouble() * (max - min);

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
