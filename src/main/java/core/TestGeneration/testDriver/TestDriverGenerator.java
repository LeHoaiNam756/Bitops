package core.TestGeneration.testDriver;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import core.utils.FilePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestDriverGenerator {

    /**
     * Generates a test driver that reads parameters from command-line arguments.
     * This allows the test driver to be compiled once and reused with different inputs.
     */
    public static void generateTestDriver(MethodDeclaration testUnit, Class<?>[] parameterClasses,
                                          String fullyClonedClassName, String simpleClassName) {
        if (testUnit == null) {
            throw new IllegalArgumentException("testUnit cannot be null");
        }
        if (parameterClasses == null) {
            throw new IllegalArgumentException("parameterClasses cannot be null");
        }
        if (simpleClassName == null || simpleClassName.isEmpty()) {
            throw new IllegalArgumentException("simpleClassName cannot be null or empty");
        }

        StringBuilder result = new StringBuilder();

        result.append("package ").append(FilePath.TEST_DRIVER_FILE_PACKAGE_LOCATION).append(";\n\n");

        result.append("import ").append(FilePath.RAM_STORAGE_CLASS_IMPORT).append(";\n");
        result.append("import ").append(fullyClonedClassName).append(";\n");
        result.append("import java.util.List;\n\n");
        result.append("public class TestDriver {\n");
        result.append(generateTestRunner(testUnit, parameterClasses, simpleClassName));
        result.append("}\n");
        try {
            createTestDriverFile(result.toString());
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate test driver file with error: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method for backward compatibility. Generates test driver with hardcoded values.
     * @deprecated Use generateTestDriver(MethodDeclaration, Class<?>[], String, String) instead
     */
    @Deprecated
    public static void generateTestDriver(MethodDeclaration testUnit, Object[] testInputs, String fullyClonedClassName,
                                          String simpleClassName) {
        if (testUnit == null) {
            throw new IllegalArgumentException("testUnit cannot be null");
        }
        if (testInputs == null) {
            throw new IllegalArgumentException("testInputs cannot be null");
        }
        if (simpleClassName == null || simpleClassName.isEmpty()) {
            throw new IllegalArgumentException("simpleClassName cannot be empty");
        }

        // Extract parameter classes from testInputs
        Class<?>[] parameterClasses = new Class<?>[testInputs.length];
        for (int i = 0; i < testInputs.length; i++) {
            if (testInputs[i] == null) {
                @SuppressWarnings("unchecked")
                List<ASTNode> params = testUnit.parameters();
                if (i < params.size()) {
                    Class<?>[] allParamClasses = TestDriverUtils.getParameterClasses(params);
                    parameterClasses[i] = allParamClasses[i];
                } else {
                    throw new IllegalArgumentException("Cannot infer type for null parameter at index " + i);
                }
            } else {
                parameterClasses[i] = testInputs[i].getClass();
            }
        }

        generateTestDriver(testUnit, parameterClasses, fullyClonedClassName, simpleClassName);
    }

    public static String generateTestRunner(MethodDeclaration testUnit, Class<?>[] parameterClasses,
                                            String simpleClassName) {
        StringBuilder result = new StringBuilder();
        result.append("    public static void main(String[] args) {\n");
        result.append("        List<Object> outputs = RamStorage.getOutputs();\n");
        result.append("        if (args.length != ").append(parameterClasses.length).append(") {\n");
        result.append("            throw new IllegalArgumentException(\"Expected ").append(parameterClasses.length)
                .append(" arguments, got \" + args.length);\n");
        result.append("        }\n");

        // Parse arguments based on parameter types
        for (int i = 0; i < parameterClasses.length; i++) {
            result.append("        ").append(getTypeName(parameterClasses[i])).append(" arg").append(i)
                    .append(" = parseArg").append(i).append("(args[").append(i).append("]);\n");
        }

        boolean isStatic = isStaticMethod(testUnit);
        if (isStatic) {
            result.append("        Object output = ").append(simpleClassName).append(".");
        } else {
            result.append("        Object output = new ").append(simpleClassName).append("().");
        }
        result.append(testUnit.getName().toString()).append("(");
        for (int i = 0; i < parameterClasses.length; i++) {
            result.append("arg").append(i);
            if (i != parameterClasses.length - 1) {
                result.append(", ");
            }
        }
        result.append(");\n");
        result.append("        outputs.add(output);\n");
        result.append("    }\n");

        // Generate parse methods for each parameter
        for (int i = 0; i < parameterClasses.length; i++) {
            result.append(generateParseMethod(i, parameterClasses[i]));
        }

        // Add unescape utility methods if needed
        boolean needsUnescape = false;
        for (Class<?> paramType : parameterClasses) {
            if (requiresUnescape(paramType)) {
                needsUnescape = true;
                break;
            }
        }
        if (needsUnescape) {
            result.append(generateUnescapeMethods());
        }

        return result.toString();
    }

    private static String generateParseMethod(int index, Class<?> paramType) {
        StringBuilder result = new StringBuilder();
        result.append("    private static ").append(getTypeName(paramType)).append(" parseArg").append(index)
                .append("(String arg) {\n");

        if (paramType.isArray()) {
            // Delegate to a named helper method so the main method stays clean
            result.append("        return parse").append(getArrayHelperName(paramType)).append("Array(arg);\n");
        } else if (paramType == String.class) {
            result.append("        if (\"null\".equals(arg)) return null;\n");
            result.append("        return unescapeString(arg);\n");
        } else if (paramType == int.class || paramType == Integer.class) {
            result.append("        return Integer.parseInt(arg);\n");
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            result.append("        return Boolean.parseBoolean(arg);\n");
        } else if (paramType == byte.class || paramType == Byte.class) {
            result.append("        return Byte.parseByte(arg);\n");
        } else if (paramType == short.class || paramType == Short.class) {
            result.append("        return Short.parseShort(arg);\n");
        } else if (paramType == char.class || paramType == Character.class) {
            result.append("        if (arg.length() == 0) throw new IllegalArgumentException(\"Empty string for char\");\n");
            result.append("        return unescapeChar(arg);\n");
        } else if (paramType == long.class || paramType == Long.class) {
            result.append("        return Long.parseLong(arg);\n");
        } else if (paramType == float.class || paramType == Float.class) {
            result.append("        return Float.parseFloat(arg);\n");
        } else if (paramType == double.class || paramType == Double.class) {
            result.append("        return Double.parseDouble(arg);\n");
        } else {
            throw new RuntimeException("Unsupported parameter type: " + paramType);
        }

        result.append("    }\n");

        // Append the shared array-parsing helper when needed
        if (paramType.isArray()) {
            result.append(generateArrayParseHelper(paramType));
        }

        return result.toString();
    }

    /**
     * Produces a stable method-name segment for a given array type, e.g.
     * int[]   -> "Int"
     * long[]  -> "Long"
     * int[][] -> "IntArray"  (component is itself an array)
     */
    private static String getArrayHelperName(Class<?> arrayType) {
        Class<?> component = arrayType.getComponentType();
        if (component.isArray()) {
            return getArrayHelperName(component) + "Array";
        }
        // Capitalise the first letter of the component type name
        String name = getTypeName(component);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Generates a private static helper method that parses a bracket-encoded
     * string like "[1,2,3]" into the correct Java array type.
     * Serialisation format: "[elem0,elem1,...]"  — empty array: "[]"
     */
    private static String generateArrayParseHelper(Class<?> arrayType) {
        Class<?> component = arrayType.getComponentType();
        String helperName = "parse" + getArrayHelperName(arrayType) + "Array";
        String typeName   = getTypeName(arrayType);          // e.g. "int[]"
        String compName   = getTypeName(component);          // e.g. "int"

        StringBuilder sb = new StringBuilder();
        sb.append("    private static ").append(typeName).append(" ").append(helperName)
                .append("(String arg) {\n");
        sb.append("        if (\"null\".equals(arg)) return null;\n");
        sb.append("        arg = arg.trim();\n");
        sb.append("        if (!arg.startsWith(\"[\") || !arg.endsWith(\"]\"))\n");
        sb.append("            throw new IllegalArgumentException(\"Invalid array format: \" + arg);\n");
        sb.append("        String inner = arg.substring(1, arg.length() - 1).trim();\n");
        sb.append("        if (inner.isEmpty()) return new ").append(compName).append("[0];\n");
        sb.append("        String[] parts = inner.split(\",\", -1);\n");
        sb.append("        ").append(typeName).append(" result = new ").append(compName)
                .append("[parts.length];\n");
        sb.append("        for (int i = 0; i < parts.length; i++) {\n");
        sb.append("            String elem = parts[i].trim();\n");

        if (component.isArray()) {
            // Nested array: recurse into the component helper
            sb.append("            result[i] = ").append("parse").append(getArrayHelperName(component))
                    .append("Array(elem);\n");
        } else if (component == int.class || component == Integer.class) {
            sb.append("            result[i] = Integer.parseInt(elem);\n");
        } else if (component == boolean.class || component == Boolean.class) {
            sb.append("            result[i] = Boolean.parseBoolean(elem);\n");
        } else if (component == byte.class || component == Byte.class) {
            sb.append("            result[i] = Byte.parseByte(elem);\n");
        } else if (component == short.class || component == Short.class) {
            sb.append("            result[i] = Short.parseShort(elem);\n");
        } else if (component == char.class || component == Character.class) {
            sb.append("            result[i] = elem.isEmpty() ? (char)0 : unescapeChar(elem);\n");
        } else if (component == long.class || component == Long.class) {
            sb.append("            result[i] = Long.parseLong(elem);\n");
        } else if (component == float.class || component == Float.class) {
            sb.append("            result[i] = Float.parseFloat(elem);\n");
        } else if (component == double.class || component == Double.class) {
            sb.append("            result[i] = Double.parseDouble(elem);\n");
        } else if (component == String.class) {
            sb.append("            result[i] = \"null\".equals(elem) ? null : unescapeString(elem);\n");
        } else {
            throw new RuntimeException("Unsupported array component type: " + component);
        }

        sb.append("        }\n");
        sb.append("        return result;\n");
        sb.append("    }\n");
        return sb.toString();
    }

    private static String getTypeName(Class<?> type) {
        if (type == int.class) return "int";
        if (type == boolean.class) return "boolean";
        if (type == byte.class) return "byte";
        if (type == short.class) return "short";
        if (type == char.class) return "char";
        if (type == long.class) return "long";
        if (type == float.class) return "float";
        if (type == double.class) return "double";
        if (type == void.class) return "void";
        if (type.isArray()) {
            // Recursively build the correct Java array declaration, e.g. "int[]", "int[][]"
            return getTypeName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    private static String generateUnescapeMethods() {
        StringBuilder result = new StringBuilder();
        result.append("    private static String unescapeString(String s) {\n");
        result.append("        if (s == null || s.isEmpty()) return s;\n");
        result.append("        StringBuilder sb = new StringBuilder();\n");
        result.append("        for (int i = 0; i < s.length(); i++) {\n");
        result.append("            char c = s.charAt(i);\n");
        result.append("            if (c == '\\\\' && i + 1 < s.length()) {\n");
        result.append("                char next = s.charAt(i + 1);\n");
        result.append("                switch (next) {\n");
        result.append("                    case 'n': sb.append('\\n'); i++; break;\n");
        result.append("                    case 'r': sb.append('\\r'); i++; break;\n");
        result.append("                    case 't': sb.append('\\t'); i++; break;\n");
        result.append("                    case 'b': sb.append('\\b'); i++; break;\n");
        result.append("                    case 'f': sb.append('\\f'); i++; break;\n");
        result.append("                    case '\\\\': sb.append('\\\\'); i++; break;\n");
        result.append("                    case 'u': \n");
        result.append("                        if (i + 5 < s.length()) {\n");
        result.append("                            String hex = s.substring(i + 2, i + 6);\n");
        result.append("                            sb.append((char) Integer.parseInt(hex, 16));\n");
        result.append("                            i += 5;\n");
        result.append("                        } else {\n");
        result.append("                            sb.append(c);\n");
        result.append("                        }\n");
        result.append("                        break;\n");
        result.append("                    default: sb.append(c);\n");
        result.append("                }\n");
        result.append("            } else {\n");
        result.append("                sb.append(c);\n");
        result.append("            }\n");
        result.append("        }\n");
        result.append("        return sb.toString();\n");
        result.append("    }\n");

        result.append("    private static char unescapeChar(String s) {\n");
        result.append("        if (s.length() == 1) return s.charAt(0);\n");
        result.append("        if (s.startsWith(\"\\\\u\") && s.length() == 6) {\n");
        result.append("            return (char) Integer.parseInt(s.substring(2), 16);\n");
        result.append("        }\n");
        result.append("        if (s.length() == 2 && s.charAt(0) == '\\\\') {\n");
        result.append("            switch (s.charAt(1)) {\n");
        result.append("                case 'n': return '\\n';\n");
        result.append("                case 'r': return '\\r';\n");
        result.append("                case 't': return '\\t';\n");
        result.append("                case 'b': return '\\b';\n");
        result.append("                case 'f': return '\\f';\n");
        result.append("                case '\\\\': return '\\\\';\n");
        result.append("                case '\\'': return '\\'';\n");
        result.append("                default: return s.charAt(1);\n");
        result.append("            }\n");
        result.append("        }\n");
        result.append("        return s.charAt(0);\n");
        result.append("    }\n");

        return result.toString();
    }

    private static boolean isStaticMethod(MethodDeclaration method) {
        @SuppressWarnings("unchecked")
        List<Modifier> modifiers = method.modifiers();
        for (Modifier modifier : modifiers) {
            if (modifier.getKeyword() != null &&
                    modifier.getKeyword().toFlagValue() == Modifier.ModifierKeyword.STATIC_KEYWORD.toFlagValue()) {
                return true;
            }
        }
        return false;
    }

    private static void createTestDriverFile(String content) throws IOException {
        Path testDriverPath = Path.of(FilePath.PATH_TO_TEST_DRIVER);
        Files.createDirectories(testDriverPath.getParent());
        Files.write(testDriverPath, content.getBytes());
    }

    /**
     * Serializes test inputs to command-line arguments that can be parsed by the generated test driver.
     */
    public static String[] serializeTestInputs(Object[] testInputs) {
        if (testInputs == null) {
            return new String[0];
        }
        String[] args = new String[testInputs.length];
        for (int i = 0; i < testInputs.length; i++) {
            args[i] = serializeValue(testInputs[i]);
        }
        return args;
    }

    /**
     * Returns true when the type (or its array component) needs the unescape helpers in the
     * generated test driver.
     */
    private static boolean requiresUnescape(Class<?> type) {
        if (type == String.class || type == char.class || type == Character.class) return true;
        if (type.isArray()) return requiresUnescape(type.getComponentType());
        return false;
    }

    private static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value.getClass().isArray()) {
            return serializeArray(value);
        } else if (value instanceof String) {
            return escapeString((String) value);
        } else if (value instanceof Character) {
            return escapeChar((Character) value);
        } else {
            return value.toString();
        }
    }

    /**
     * Serialises any primitive or String array (including multi-dimensional) to the
     * bracket format "[e0,e1,e2]" consumed by the generated parseXxxArray helpers.
     */
    private static String serializeArray(Object array) {
        if (array == null) return "null";
        int length = java.lang.reflect.Array.getLength(array);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(",");
            Object elem = java.lang.reflect.Array.get(array, i);
            if (elem == null) {
                sb.append("null");
            } else if (elem.getClass().isArray()) {
                sb.append(serializeArray(elem));   // nested arrays
            } else if (elem instanceof Character) {
                sb.append(escapeChar((Character) elem));
            } else if (elem instanceof String) {
                sb.append(escapeString((String) elem));
            } else {
                sb.append(elem);
            }
        }
        sb.append("]");
        return sb.toString();
    }



    private static String escapeString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 32 || (c > 126 && c < 256)) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static String escapeChar(char c) {
        if (c == '\'') {
            return "\\'";
        } else if (c == '\\') {
            return "\\\\";
        } else if (c < 32 || c > 126) {
            return String.format("\\u%04X", (int) c);
        } else {
            return String.valueOf(c);
        }
    }

    private static void formatValue(StringBuilder result, Object value) {
        if (value == null) {
            result.append("null");
        } else if (value.getClass().isArray()) {
            // Produce a Java array initialiser literal, e.g. new int[]{1,2,3}
            result.append(buildArrayLiteral(value));
        } else if (value instanceof Float) {
            result.append(value).append("f");
        } else if (value instanceof Double) {
            result.append(value).append("d");
        } else if (value instanceof Long) {
            result.append(value).append("L");
        } else if (value instanceof Character) {
            formatCharacter(result, (Character) value);
        } else if (value instanceof Short) {
            result.append("(short)").append(value);
        } else if (value instanceof Byte) {
            result.append("(byte)").append(value);
        } else if (value instanceof Boolean) {
            result.append(value);
        } else if (value instanceof String) {
            formatString(result, (String) value);
        } else {
            result.append(value);
        }
    }

    private static void formatCharacter(StringBuilder result, char value) {
        if (value == '\'') {
            result.append("'\\''");
        } else if (value == '\\') {
            result.append("'\\\\'");
        } else if (value < 32 || value > 126) {
            String escaped = String.format("\\u%04X", (int) value);
            result.append("'").append(escaped).append("'");
        } else {
            result.append("'").append(value).append("'");
        }
    }

    private static void formatString(StringBuilder result, String value) {
        result.append("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                default:
                    if (c < 32 || (c > 126 && c < 256)) {
                        result.append(String.format("\\u%04X", (int) c));
                    } else {
                        result.append(c);
                    }
                    break;
            }
        }
        result.append("\"");
    }

    /** such as {@code new int[]{1,2,3}} for use
     * in the legacy (hardcoded) test driver path.
     */
    private static String buildArrayLiteral(Object array) {
        if (array == null) return "null";
        Class<?> componentType = array.getClass().getComponentType();
        int length = java.lang.reflect.Array.getLength(array);
        StringBuilder sb = new StringBuilder("new ").append(getTypeName(componentType)).append("[]{");
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(", ");
            Object elem = java.lang.reflect.Array.get(array, i);
            if (elem == null) {
                sb.append("null");
            } else if (elem.getClass().isArray()) {
                sb.append(buildArrayLiteral(elem));
            } else {
                StringBuilder tmp = new StringBuilder();
                formatValue(tmp, elem);
                sb.append(tmp);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}