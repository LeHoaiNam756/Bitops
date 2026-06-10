package core.testdriver;

import java.util.List;

public class TestDriverEmitter {
    public static String emitDriver(String packageName, String className, String methodName,
                                    List<TestDriver.ParamInfo> params, String returnType) {
        return emitDriver(packageName, className, methodName, params, returnType, false);
    }

    public static String emitDriver(String packageName, String className, String methodName,
                                    List<TestDriver.ParamInfo> params, String returnType,
                                    boolean constructorUnit) {
        StringBuilder sb = new StringBuilder();
        if (packageName != null && !packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        sb.append("import com.fasterxml.jackson.databind.JsonNode;\n")
                .append("import com.fasterxml.jackson.databind.ObjectMapper;\n")
                .append("import com.fasterxml.jackson.databind.node.ArrayNode;\n")
                .append("import com.fasterxml.jackson.databind.node.ObjectNode;\n")
                .append("import core.testdriver.JsonArgParser;\n")
                .append("import core.testdriver.JsonArgParser.ArgConversionException;\n")
                .append("import core.testpath.TraceRecorder;\n")
                .append("import core.utils.FilePath;\n")
                .append("import java.io.File;\n")
                .append("import java.lang.reflect.Constructor;\n")
                .append("import java.lang.reflect.Method;\n")
                .append("import java.lang.reflect.Modifier;\n")
                .append("import java.nio.file.Path;\n")
                .append("import java.util.Arrays;\n\n");

        sb.append("/** Auto-generated driver for ").append(className).append(".").append(methodName).append(" */\n");
        sb.append("public class DriverMain {\n");
        emitConstants(sb, packageName, className, methodName, returnType, params, constructorUnit);
        emitMain(sb);
        emitHelpers(sb);
        sb.append("}\n");
        return sb.toString();
    }

    private static void emitConstants(StringBuilder sb, String packageName, String className,
                                      String methodName, String returnType,
                                      List<TestDriver.ParamInfo> params,
                                      boolean constructorUnit) {
        String fqn = packageName == null || packageName.isEmpty()
                ? className
                : packageName + "." + className;
        String sessionName = fqn + "#" + methodName;

        sb.append("    private static final String CLASS_NAME = \"").append(javaString(className)).append("\";\n")
                .append("    private static final String TARGET_CLASS = \"").append(javaString(fqn)).append("\";\n")
                .append("    private static final String METHOD_NAME = \"").append(javaString(methodName)).append("\";\n")
                .append("    private static final String RETURN_TYPE = \"").append(javaString(returnType)).append("\";\n")
                .append("    private static final String SESSION_NAME = \"").append(javaString(sessionName)).append("\";\n")
                .append("    private static final boolean CONSTRUCTOR_UNIT = ").append(constructorUnit).append(";\n");

        sb.append("    private static final String[] PARAM_NAMES = new String[] {");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(javaString(params.get(i).name())).append("\"");
        }
        sb.append("};\n");

        sb.append("    private static final String[] PARAM_TYPES = new String[] {");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(javaString(params.get(i).typeName())).append("\"");
        }
        sb.append("};\n\n");
    }

    private static void emitMain(StringBuilder sb) {
        sb.append("    public static void main(String[] args) {\n")
                .append("        if (args.length < 2) {\n")
                .append("            System.err.println(\"Usage: DriverMain --input=<input-json> --output=<output-json>\");\n")
                .append("            System.exit(2);\n")
                .append("        }\n")
                .append("        ObjectMapper mapper = new ObjectMapper();\n")
                .append("        String inputPath = argumentValue(args, \"--input=\", 0);\n")
                .append("        String outputPath = argumentValue(args, \"--output=\", 1);\n")
                .append("        try {\n")
                .append("            JsonNode root = mapper.readTree(new File(inputPath));\n")
                .append("            Object[] methodArgs = new Object[PARAM_NAMES.length];\n")
                .append("            for (int i = 0; i < PARAM_NAMES.length; i++) {\n")
                .append("                JsonNode value = root.get(PARAM_NAMES[i]);\n")
                .append("                if (value == null) {\n")
                .append("                    throw new IllegalArgumentException(\"Missing input for parameter: \" + PARAM_NAMES[i]);\n")
                .append("                }\n")
                .append("                try {\n")
                .append("                    methodArgs[i] = JsonArgParser.convert(PARAM_NAMES[i], PARAM_TYPES[i], value);\n")
                .append("                } catch (ArgConversionException e) {\n")
                .append("                    System.err.println(e.getMessage());\n")
                .append("                    System.exit(4);\n")
                .append("                    return;\n")
                .append("                }\n")
                .append("            }\n")
                .append("            Class<?> clazz = Class.forName(TARGET_CLASS);\n")
                .append("            Class<?>[] paramClasses = resolveParamClasses();\n")
                .append("            Object result = null;\n")
                .append("            Throwable thrown = null;\n")
                .append("            TraceRecorder.startSession(SESSION_NAME, Path.of(FilePath.PATH_TO_CLONED_PROJECT));\n")
                .append("            try {\n")
                .append("                if (CONSTRUCTOR_UNIT) {\n")
                .append("                    Constructor<?> constructor = clazz.getDeclaredConstructor(paramClasses);\n")
                .append("                    constructor.setAccessible(true);\n")
                .append("                    try {\n")
                .append("                        result = constructor.newInstance(methodArgs);\n")
                .append("                    } catch (java.lang.reflect.InvocationTargetException e) {\n")
                .append("                        thrown = e.getCause() == null ? e : e.getCause();\n")
                .append("                    }\n")
                .append("                } else {\n")
                .append("                    Method method = clazz.getDeclaredMethod(METHOD_NAME, paramClasses);\n")
                .append("                    method.setAccessible(true);\n")
                .append("                    Object instance = null;\n")
                .append("                    if (!Modifier.isStatic(method.getModifiers())) {\n")
                .append("                        Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();\n")
                .append("                        defaultConstructor.setAccessible(true);\n")
                .append("                        instance = defaultConstructor.newInstance();\n")
                .append("                    }\n")
                .append("                    try {\n")
                .append("                        result = method.invoke(instance, methodArgs);\n")
                .append("                    } catch (java.lang.reflect.InvocationTargetException e) {\n")
                .append("                        thrown = e.getCause() == null ? e : e.getCause();\n")
                .append("                    }\n")
                .append("                }\n")
                .append("                ObjectNode resultJson = mapper.createObjectNode();\n")
                .append("                resultJson.set(\"input\", root);\n")
                .append("                if (thrown != null) {\n")
                .append("                    resultJson.put(\"output\", exceptionToString(thrown));\n")
                .append("                    resultJson.put(\"threwException\", true);\n")
                .append("                } else if (CONSTRUCTOR_UNIT) {\n")
                .append("                    resultJson.put(\"output\", \"CONSTRUCTED: \" + CLASS_NAME);\n")
                .append("                } else if (\"void\".equals(RETURN_TYPE)) {\n")
                .append("                    resultJson.putNull(\"output\");\n")
                .append("                } else {\n")
                .append("                    resultJson.put(\"output\", resultToString(result));\n")
                .append("                }\n")
                .append("                resultJson.set(\"coveredNodeIds\", coveredNodeIdsJson(mapper));\n")
                .append("                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), resultJson);\n")
                .append("            } finally {\n")
                .append("                TraceRecorder.endSession();\n")
                .append("            }\n")
                .append("        } catch (Exception e) {\n")
                .append("            e.printStackTrace();\n")
                .append("            System.exit(5);\n")
                .append("        }\n")
                .append("    }\n\n");
    }

    private static void emitHelpers(StringBuilder sb) {
        sb.append("    private static Object convert(JsonNode value, String typeName) {\n")
                .append("        return switch (typeName) {\n")
                .append("            case \"int\", \"java.lang.Integer\" -> value.asInt();\n")
                .append("            case \"long\", \"java.lang.Long\" -> value.asLong();\n")
                .append("            case \"double\", \"java.lang.Double\" -> value.asDouble();\n")
                .append("            case \"float\", \"java.lang.Float\" -> (float) value.asDouble();\n")
                .append("            case \"boolean\", \"java.lang.Boolean\" -> value.asBoolean();\n")
                .append("            case \"byte\", \"java.lang.Byte\" -> (byte) value.asInt();\n")
                .append("            case \"short\", \"java.lang.Short\" -> (short) value.asInt();\n")
                .append("            case \"char\", \"java.lang.Character\" -> value.asText().isEmpty() ? '\\0' : value.asText().charAt(0);\n")
                .append("            case \"java.lang.String\", \"String\" -> value.asText();\n")
                .append("            default -> throw new IllegalArgumentException(\"Unsupported parameter type: \" + typeName);\n")
                .append("        };\n")
                .append("    }\n\n")
                .append("    private static Class<?>[] resolveParamClasses() throws ClassNotFoundException {\n")
                .append("        Class<?>[] classes = new Class<?>[PARAM_TYPES.length];\n")
                .append("        for (int i = 0; i < PARAM_TYPES.length; i++) {\n")
                .append("            classes[i] = resolveClass(PARAM_TYPES[i]);\n")
                .append("        }\n")
                .append("        return classes;\n")
                .append("    }\n\n")
                .append("    private static String argumentValue(String[] args, String prefix, int positionalIndex) {\n")
                .append("        for (String arg : args) {\n")
                .append("            if (arg.startsWith(prefix)) {\n")
                .append("                return arg.substring(prefix.length());\n")
                .append("            }\n")
                .append("        }\n")
                .append("        return args.length > positionalIndex ? args[positionalIndex] : \"\";\n")
                .append("    }\n\n")
                .append("    private static Class<?> resolveClass(String typeName) throws ClassNotFoundException {\n")
                .append("        return switch (typeName) {\n")
                .append("            case \"int\" -> int.class;\n")
                .append("            case \"long\" -> long.class;\n")
                .append("            case \"double\" -> double.class;\n")
                .append("            case \"float\" -> float.class;\n")
                .append("            case \"boolean\" -> boolean.class;\n")
                .append("            case \"byte\" -> byte.class;\n")
                .append("            case \"short\" -> short.class;\n")
                .append("            case \"char\" -> char.class;\n")
                .append("            case \"String\" -> String.class;\n")
                .append("            case \"int[]\" -> int[].class;\n")
                .append("            case \"long[]\" -> long[].class;\n")
                .append("            case \"double[]\" -> double[].class;\n")
                .append("            case \"float[]\" -> float[].class;\n")
                .append("            case \"boolean[]\" -> boolean[].class;\n")
                .append("            case \"byte[]\" -> byte[].class;\n")
                .append("            case \"short[]\" -> short[].class;\n")
                .append("            case \"char[]\" -> char[].class;\n")
                .append("            case \"String[]\" -> String[].class;\n")
                .append("            case \"int[][]\" -> int[][].class;\n")
                .append("            case \"long[][]\" -> long[][].class;\n")
                .append("            case \"double[][]\" -> double[][].class;\n")
                .append("            case \"float[][]\" -> float[][].class;\n")
                .append("            case \"boolean[][]\" -> boolean[][].class;\n")
                .append("            case \"byte[][]\" -> byte[][].class;\n")
                .append("            case \"short[][]\" -> short[][].class;\n")
                .append("            case \"char[][]\" -> char[][].class;\n")
                .append("            default -> Class.forName(typeName);\n")
                .append("        };\n")
                .append("    }\n\n")
                .append("    private static String resultToString(Object result) {\n")
                .append("        if (result == null) {\n")
                .append("            return null;\n")
                .append("        }\n")
                .append("        Class<?> resultClass = result.getClass();\n")
                .append("        if (resultClass.isArray()) {\n")
                .append("            if (result instanceof int[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof long[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof double[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof float[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof boolean[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof byte[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof short[] arr) return Arrays.toString(arr);\n")
                .append("            if (result instanceof char[] arr) return Arrays.toString(arr);\n")
                .append("            return Arrays.deepToString((Object[]) result);\n")
                .append("        }\n")
                .append("        return String.valueOf(result);\n")
                .append("    }\n\n")
                .append("    private static String exceptionToString(Throwable thrown) {\n")
                .append("        return \"EXCEPTION: \" + thrown.getClass().getName() + \": \" + String.valueOf(thrown.getMessage());\n")
                .append("    }\n\n")
                .append("    private static ArrayNode coveredNodeIdsJson(ObjectMapper mapper) {\n")
                .append("        ArrayNode array = mapper.createArrayNode();\n")
                .append("        for (Integer nodeId : TraceRecorder.coveredNodeIdsSnapshot()) {\n")
                .append("            array.add(nodeId);\n")
                .append("        }\n")
                .append("        return array;\n")
                .append("    }\n");
    }

    private static String javaString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
