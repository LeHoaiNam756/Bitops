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
            if (paramType == String.class || paramType == char.class || paramType == Character.class) {
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
        
        if (paramType == String.class) {
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
        return result.toString();
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
        Path path = Path.of(FilePath.PATH_TO_TEST_DRIVER);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes());
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
    
    private static String serializeValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return escapeString((String) value);
        } else if (value instanceof Character) {
            return escapeChar((Character) value);
        } else {
            return value.toString();
        }
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
            throw new RuntimeException("Array type is not supported in test driver generation");
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
}
