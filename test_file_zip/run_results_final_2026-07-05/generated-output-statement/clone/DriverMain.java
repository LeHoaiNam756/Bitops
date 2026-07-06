package core.output.clone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.testdriver.JsonArgParser;
import core.testdriver.JsonArgParser.ArgConversionException;
import core.testpath.TraceRecorder;
import core.utils.FilePath;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;

/** Auto-generated driver for Util.subtractWithOverflowDefault */
public class DriverMain {
    private static final String CLASS_NAME = "Util";
    private static final String TARGET_CLASS = "core.output.clone.Util";
    private static final String METHOD_NAME = "subtractWithOverflowDefault";
    private static final String RETURN_TYPE = "long";
    private static final String SESSION_NAME = "core.output.clone.Util#subtractWithOverflowDefault";
    private static final boolean CONSTRUCTOR_UNIT = false;
    private static final String[] PARAM_NAMES = new String[] {"x", "y", "overflowResult"};
    private static final String[] PARAM_TYPES = new String[] {"long", "long", "long"};

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: DriverMain --input=<input-json> --output=<output-json>");
            System.exit(2);
        }
        ObjectMapper mapper = new ObjectMapper();
        String inputPath = argumentValue(args, "--input=", 0);
        String outputPath = argumentValue(args, "--output=", 1);
        try {
            JsonNode root = mapper.readTree(new File(inputPath));
            Object[] methodArgs = new Object[PARAM_NAMES.length];
            for (int i = 0; i < PARAM_NAMES.length; i++) {
                JsonNode value = root.get(PARAM_NAMES[i]);
                if (value == null) {
                    throw new IllegalArgumentException("Missing input for parameter: " + PARAM_NAMES[i]);
                }
                try {
                    methodArgs[i] = JsonArgParser.convert(PARAM_NAMES[i], PARAM_TYPES[i], value);
                } catch (ArgConversionException e) {
                    System.err.println(e.getMessage());
                    System.exit(4);
                    return;
                }
            }
            Class<?> clazz = Class.forName(TARGET_CLASS);
            Class<?>[] paramClasses = resolveParamClasses();
            Object result = null;
            Throwable thrown = null;
            TraceRecorder.startSession(SESSION_NAME, Path.of(FilePath.PATH_TO_CLONED_PROJECT));
            try {
                if (CONSTRUCTOR_UNIT) {
                    Constructor<?> constructor = clazz.getDeclaredConstructor(paramClasses);
                    constructor.setAccessible(true);
                    try {
                        result = constructor.newInstance(methodArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        thrown = e.getCause() == null ? e : e.getCause();
                    }
                } else {
                    Method method = clazz.getDeclaredMethod(METHOD_NAME, paramClasses);
                    method.setAccessible(true);
                    Object instance = null;
                    if (!Modifier.isStatic(method.getModifiers())) {
                        Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
                        defaultConstructor.setAccessible(true);
                        instance = defaultConstructor.newInstance();
                    }
                    try {
                        result = method.invoke(instance, methodArgs);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        thrown = e.getCause() == null ? e : e.getCause();
                    }
                }
                ObjectNode resultJson = mapper.createObjectNode();
                resultJson.set("input", root);
                if (thrown != null) {
                    resultJson.put("output", exceptionToString(thrown));
                    resultJson.put("threwException", true);
                } else if (CONSTRUCTOR_UNIT) {
                    resultJson.put("output", "CONSTRUCTED: " + CLASS_NAME);
                } else if ("void".equals(RETURN_TYPE)) {
                    resultJson.putNull("output");
                } else {
                    resultJson.put("output", resultToString(result));
                }
                resultJson.set("coveredNodeIds", coveredNodeIdsJson(mapper));
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), resultJson);
            } finally {
                TraceRecorder.endSession();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(5);
        }
    }

    private static Object convert(JsonNode value, String typeName) {
        return switch (typeName) {
            case "int", "java.lang.Integer" -> value.asInt();
            case "long", "java.lang.Long" -> value.asLong();
            case "double", "java.lang.Double" -> value.asDouble();
            case "float", "java.lang.Float" -> (float) value.asDouble();
            case "boolean", "java.lang.Boolean" -> value.asBoolean();
            case "byte", "java.lang.Byte" -> (byte) value.asInt();
            case "short", "java.lang.Short" -> (short) value.asInt();
            case "char", "java.lang.Character" -> value.asText().isEmpty() ? '\0' : value.asText().charAt(0);
            case "java.lang.String", "String" -> value.asText();
            default -> throw new IllegalArgumentException("Unsupported parameter type: " + typeName);
        };
    }

    private static Class<?>[] resolveParamClasses() throws ClassNotFoundException {
        Class<?>[] classes = new Class<?>[PARAM_TYPES.length];
        for (int i = 0; i < PARAM_TYPES.length; i++) {
            classes[i] = resolveClass(PARAM_TYPES[i]);
        }
        return classes;
    }

    private static String argumentValue(String[] args, String prefix, int positionalIndex) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return args.length > positionalIndex ? args[positionalIndex] : "";
    }

    private static Class<?> resolveClass(String typeName) throws ClassNotFoundException {
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            case "String" -> String.class;
            case "int[]" -> int[].class;
            case "long[]" -> long[].class;
            case "double[]" -> double[].class;
            case "float[]" -> float[].class;
            case "boolean[]" -> boolean[].class;
            case "byte[]" -> byte[].class;
            case "short[]" -> short[].class;
            case "char[]" -> char[].class;
            case "String[]" -> String[].class;
            case "int[][]" -> int[][].class;
            case "long[][]" -> long[][].class;
            case "double[][]" -> double[][].class;
            case "float[][]" -> float[][].class;
            case "boolean[][]" -> boolean[][].class;
            case "byte[][]" -> byte[][].class;
            case "short[][]" -> short[][].class;
            case "char[][]" -> char[][].class;
            default -> Class.forName(typeName);
        };
    }

    private static String resultToString(Object result) {
        if (result == null) {
            return null;
        }
        Class<?> resultClass = result.getClass();
        if (resultClass.isArray()) {
            if (result instanceof int[] arr) return Arrays.toString(arr);
            if (result instanceof long[] arr) return Arrays.toString(arr);
            if (result instanceof double[] arr) return Arrays.toString(arr);
            if (result instanceof float[] arr) return Arrays.toString(arr);
            if (result instanceof boolean[] arr) return Arrays.toString(arr);
            if (result instanceof byte[] arr) return Arrays.toString(arr);
            if (result instanceof short[] arr) return Arrays.toString(arr);
            if (result instanceof char[] arr) return Arrays.toString(arr);
            return Arrays.deepToString((Object[]) result);
        }
        return String.valueOf(result);
    }

    private static String exceptionToString(Throwable thrown) {
        return "EXCEPTION: " + thrown.getClass().getName() + ": " + String.valueOf(thrown.getMessage());
    }

    private static ArrayNode coveredNodeIdsJson(ObjectMapper mapper) {
        ArrayNode array = mapper.createArrayNode();
        for (Integer nodeId : TraceRecorder.coveredNodeIdsSnapshot()) {
            array.add(nodeId);
        }
        return array;
    }
}
