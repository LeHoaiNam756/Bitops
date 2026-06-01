package core.testdriver;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts a Jackson {@link JsonNode} value to a concrete Java object whose
 * type is described by a JDT type-name string (e.g. {@code "int"},
 * {@code "String"}, {@code "int[]"}, {@code "boolean[][]"}).
 *
 * <p>Supported target types:
 * <ul>
 *   <li>Java primitives: {@code int}, {@code long}, {@code short}, {@code byte},
 *       {@code char}, {@code boolean}, {@code float}, {@code double}</li>
 *   <li>{@code String}</li>
 *   <li>1-D arrays of each primitive and {@code String[]}</li>
 *   <li>2-D arrays of each primitive ({@code int[][]}, etc.)</li>
 * </ul>
 *
 * <p>On any type mismatch or unsupported type an {@link ArgConversionException}
 * is thrown with a human-readable message that includes the parameter name,
 * expected type, and the actual JSON fragment.
 */
public final class JsonArgParser {

    private JsonArgParser() {}

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Convert {@code node} to a value compatible with {@code typeName}.
     *
     * @param paramName human-readable name used in error messages
     * @param typeName  JDT-style type string, e.g. {@code "int"},
     *                  {@code "String"}, {@code "long[]"}, {@code "double[][]"}
     * @param node      JSON value for this parameter
     * @return boxed value (primitives are auto-boxed; arrays returned as
     *         {@code Object} — caller casts via reflection)
     * @throws ArgConversionException if the node cannot be converted to the
     *                                requested type
     */
    public static Object convert(String paramName, String typeName, JsonNode node) {
        if (node == null || node.isNull()) {
            throw new ArgConversionException(paramName, typeName, "null",
                    "JSON key is missing or null");
        }

        // Strip trailing whitespace just in case
        typeName = typeName.trim();

        // 2-D arrays  (e.g. "int[][]")
        if (typeName.endsWith("[][]")) {
            return convert2DArray(paramName, typeName, node);
        }

        // 1-D arrays  (e.g. "int[]")
        if (typeName.endsWith("[]")) {
            return convert1DArray(paramName, typeName, node);
        }

        // Scalar
        return convertScalar(paramName, typeName, node);
    }

    // -----------------------------------------------------------------------
    // Scalar conversion
    // -----------------------------------------------------------------------

    static Object convertScalar(String paramName, String typeName, JsonNode node) {
        return switch (typeName) {
            case "int"     -> requireIntegral(paramName, typeName, node).intValue();
            case "long"    -> requireIntegral(paramName, typeName, node).longValue();
            case "short"   -> (short) requireIntegral(paramName, typeName, node).intValue();
            case "byte"    -> (byte)  requireIntegral(paramName, typeName, node).intValue();
            case "char"    -> convertChar(paramName, node);
            case "boolean" -> convertBoolean(paramName, node);
            case "float"   -> (float) requireNumeric(paramName, typeName, node).doubleValue();
            case "double"  -> requireNumeric(paramName, typeName, node).doubleValue();
            case "String", "java.lang.String" -> convertString(paramName, node);
            default -> throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Unsupported type: " + typeName);
        };
    }

    // -----------------------------------------------------------------------
    // 1-D array conversion
    // -----------------------------------------------------------------------

    static Object convert1DArray(String paramName, String typeName, JsonNode node) {
        if (!node.isArray()) {
            throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Expected a JSON array");
        }

        String elem = typeName.substring(0, typeName.length() - 2); // strip "[]"
        int n = node.size();

        return switch (elem) {
            case "int" -> {
                int[] arr = new int[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (int) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "long" -> {
                long[] arr = new long[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (long) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "short" -> {
                short[] arr = new short[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (short) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "byte" -> {
                byte[] arr = new byte[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (byte) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "char" -> {
                char[] arr = new char[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (char) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "boolean" -> {
                boolean[] arr = new boolean[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (boolean) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "float" -> {
                float[] arr = new float[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (float) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "double" -> {
                double[] arr = new double[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (double) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            case "String", "java.lang.String" -> {
                String[] arr = new String[n];
                for (int i = 0; i < n; i++)
                    arr[i] = (String) convertScalar(paramName + "[" + i + "]", elem, node.get(i));
                yield arr;
            }
            default -> throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Unsupported array element type: " + elem);
        };
    }

    // -----------------------------------------------------------------------
    // 2-D array conversion
    // -----------------------------------------------------------------------

    static Object convert2DArray(String paramName, String typeName, JsonNode node) {
        if (!node.isArray()) {
            throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Expected a JSON array of arrays");
        }

        String elem1D = typeName.substring(0, typeName.length() - 2); // strip outer "[]"
        int rows = node.size();

        return switch (elem1D) {
            case "int[]" -> {
                int[][] arr = new int[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (int[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "long[]" -> {
                long[][] arr = new long[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (long[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "short[]" -> {
                short[][] arr = new short[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (short[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "byte[]" -> {
                byte[][] arr = new byte[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (byte[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "char[]" -> {
                char[][] arr = new char[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (char[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "boolean[]" -> {
                boolean[][] arr = new boolean[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (boolean[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "float[]" -> {
                float[][] arr = new float[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (float[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            case "double[]" -> {
                double[][] arr = new double[rows][];
                for (int i = 0; i < rows; i++)
                    arr[i] = (double[]) convert1DArray(paramName + "[" + i + "]", elem1D, node.get(i));
                yield arr;
            }
            default -> throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Unsupported 2-D array element type: " + elem1D);
        };
    }

    // -----------------------------------------------------------------------
    // Low-level helpers
    // -----------------------------------------------------------------------

    private static Number requireIntegral(String paramName, String typeName, JsonNode node) {
        if (!node.isIntegralNumber()) {
            throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Expected an integer JSON number");
        }
        return node.numberValue();
    }

    private static Number requireNumeric(String paramName, String typeName, JsonNode node) {
        if (!node.isNumber()) {
            throw new ArgConversionException(paramName, typeName, node.toString(),
                    "Expected a numeric JSON value");
        }
        return node.numberValue();
    }

    private static char convertChar(String paramName, JsonNode node) {
        if (node.isTextual()) {
            String s = node.textValue();
            if (s.length() == 1) return s.charAt(0);
            throw new ArgConversionException(paramName, "char", node.toString(),
                    "Expected a single-character string for char, got length " + s.length());
        }
        if (node.isIntegralNumber()) {
            return (char) node.intValue();
        }
        throw new ArgConversionException(paramName, "char", node.toString(),
                "Expected a single-character string or integer for char");
    }

    private static boolean convertBoolean(String paramName, JsonNode node) {
        if (!node.isBoolean()) {
            throw new ArgConversionException(paramName, "boolean", node.toString(),
                    "Expected a JSON boolean (true/false)");
        }
        return node.booleanValue();
    }

    private static String convertString(String paramName, JsonNode node) {
        if (!node.isTextual()) {
            throw new ArgConversionException(paramName, "String", node.toString(),
                    "Expected a JSON string");
        }
        return node.textValue();
    }

    // -----------------------------------------------------------------------
    // Exception type
    // -----------------------------------------------------------------------

    /**
     * Thrown when a {@link JsonNode} cannot be converted to the requested type.
     * Callers should catch this to produce user-friendly error output.
     */
    public static final class ArgConversionException extends RuntimeException {

        private final String paramName;
        private final String expectedType;
        private final String actualJson;

        ArgConversionException(String paramName, String expectedType,
                               String actualJson, String detail) {
            super("Parameter '" + paramName + "': expected type <" + expectedType
                    + "> but could not convert JSON value " + actualJson + ". " + detail);
            this.paramName    = paramName;
            this.expectedType = expectedType;
            this.actualJson   = actualJson;
        }

        public String paramName()    { return paramName; }
        public String expectedType() { return expectedType; }
        public String actualJson()   { return actualJson; }
    }
}
