package core.testdriver;

public class TestDriverEmitter {

    public static String emitDriver(String packageName,
                                    String className,
                                    String methodName,
                                    java.util.List<TestDriver.ParamInfo> params,
                                    String returnType) {
        StringBuilder sb = new StringBuilder();

        // ── Package & imports ────────────────────────────────────────────────
        if (packageName != null && !packageName.isBlank()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("import com.fasterxml.jackson.databind.JsonNode;\n");
        sb.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        sb.append("import com.fasterxml.jackson.databind.node.ObjectNode;\n");
        sb.append("import com.fasterxml.jackson.databind.node.ArrayNode;\n");
        sb.append("import core.testdriver.JsonArgParser;\n");
        sb.append("import core.testdriver.JsonArgParser.ArgConversionException;\n");
        sb.append("import core.testpath.TraceRecorder;\n");
        sb.append("import core.utils.FilePath;\n\n");
        sb.append("import java.io.File;\n");
        sb.append("import java.lang.reflect.Method;\n");
        sb.append("import java.nio.file.Path;\n");
        sb.append("import java.util.Arrays;\n\n");
        sb.append("import java.nio.file.Files;\n");
        sb.append("import java.nio.charset.StandardCharsets;\n");
        // ── Class header ─────────────────────────────────────────────────────
        sb.append("/**\n");
        sb.append(" * Auto-generated driver for {@code ")
                .append(className).append('.').append(methodName).append("}.\n");
        sb.append(" * Run: {@code java DriverMain --input=<path/to/args.json>}\n");
        sb.append(" */\n");
        sb.append("public final class DriverMain {\n\n");

        // ── Parameter metadata ───────────────────────────────────────────────
        sb.append("    /** Expected parameter names in declaration order. */\n");
        sb.append("    private static final String[] PARAM_NAMES = {\n");
        for (int i = 0; i < params.size(); i++) {
            sb.append("        \"").append(params.get(i).name()).append("\"");
            if (i < params.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    };\n\n");

        sb.append("    /** Corresponding JDT-style type strings. */\n");
        sb.append("    private static final String[] PARAM_TYPES = {\n");
        for (int i = 0; i < params.size(); i++) {
            sb.append("        \"").append(params.get(i).typeName()).append("\"");
            if (i < params.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    };\n\n");

        // ── main ─────────────────────────────────────────────────────────────
        sb.append("    public static void main(String[] args) {\n\n");

        sb.append("        // ── 1. Parse --input flag ────────────────────────\n");
        sb.append("        String inputPath = null;\n");
        sb.append("        String outputPath = null;\n");
        sb.append("        for (String arg : args) {\n");
        sb.append("            if (arg.startsWith(\"--input=\")) {\n");
        sb.append("                inputPath = arg.substring(\"--input=\".length());\n");
        sb.append("            } else if (arg.startsWith(\"--output=\")) {\n");
        sb.append("                outputPath = arg.substring(\"--output=\".length());\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        if (inputPath == null || inputPath.isBlank() || outputPath == null || outputPath.isBlank()) {\n");
        sb.append("            System.err.println(\"Usage: java DriverMain --input=<path-to-json> --output=<path-to-result-json>\");\n");
        sb.append("            System.exit(2);\n");
        sb.append("        }\n\n");

        sb.append("        // ── 2. Parse JSON ────────────────────────────────\n");
        sb.append("        ObjectMapper mapper = new ObjectMapper();\n");
        sb.append("        JsonNode root;\n");
        sb.append("        try {\n");
        sb.append("            root = mapper.readTree(new File(inputPath));\n");
        sb.append("        } catch (Exception e) {\n");
        sb.append("            System.err.println(\"[DriverMain] JSON parse failure in '\" + inputPath + \"': \" + e.getMessage());\n");
        sb.append("            System.exit(3);\n");
        sb.append("            return; // unreachable – satisfies compiler\n");
        sb.append("        }\n\n");

        sb.append("        // ── 3. Validate & convert arguments ─────────────\n");
        sb.append("        Object[] methodArgs = new Object[PARAM_NAMES.length];\n");
        sb.append("        for (int i = 0; i < PARAM_NAMES.length; i++) {\n");
        sb.append("            String name = PARAM_NAMES[i];\n");
        sb.append("            if (!root.has(name)) {\n");
        sb.append("                System.err.println(\"[DriverMain] Missing parameter '\" + name + \"' in JSON.\");\n");
        sb.append("                System.err.println(\"  Expected parameters: \" + Arrays.toString(PARAM_NAMES));\n");
        sb.append("                System.err.println(\"  Expected types     : \" + Arrays.toString(PARAM_TYPES));\n");
        sb.append("                System.exit(4);\n");
        sb.append("            }\n");
        sb.append("            try {\n");
        sb.append("                methodArgs[i] = JsonArgParser.convert(name, PARAM_TYPES[i], root.get(name));\n");
        sb.append("            } catch (ArgConversionException e) {\n");
        sb.append("                System.err.println(\"[DriverMain] Type mismatch for parameter '\" + name + \"': \" + e.getMessage());\n");
        sb.append("                System.err.println(\"  Expected parameters: \" + Arrays.toString(PARAM_NAMES));\n");
        sb.append("                System.err.println(\"  Expected types     : \" + Arrays.toString(PARAM_TYPES));\n");
        sb.append("                System.exit(4);\n");
        sb.append("            }\n");
        sb.append("        }\n\n");

        // ── 4. Reflective invocation ─────────────────────────────────────────
        sb.append("        // ── 4. Invoke instrumented method ────────────────\n");
        sb.append("        try {\n");
        sb.append("            Class<?> clazz = Class.forName(\"").append(packageName)
                .append(packageName.isBlank() ? "" : ".").append(className).append("\");\n");
        sb.append("            // Resolve parameter Class objects at runtime\n");
        sb.append("            Class<?>[] paramClasses = resolveParamClasses();\n");
        sb.append("            Method method = clazz.getMethod(\"").append(methodName)
                .append("\", paramClasses);\n");
        sb.append("            Object instance = null;  // null for static methods; instantiate for instance methods\n");
        sb.append("            try {\n");
        sb.append("                instance = clazz.getDeclaredConstructor().newInstance();\n");
        sb.append("            } catch (Exception ignored) { /* static method — no instance needed */ }\n");
        sb.append("            TraceRecorder.startSession(\"").append(packageName)
                .append(packageName.isBlank() ? "" : ".").append(className)
                .append("#").append(methodName)
                .append("\", Path.of(FilePath.PATH_TO_CLONED_PROJECT));\n");
        sb.append("            try {\n");
        sb.append("            Object result = method.invoke(instance, methodArgs);\n");

        if ("void".equals(returnType)) {
            sb.append("            ObjectNode resultJson = mapper.createObjectNode();\n");
            sb.append("            resultJson.set(\"input\", root);\n");
            sb.append("            resultJson.putNull(\"output\");\n");
            sb.append("            resultJson.set(\"coveredNodeIds\", mapper.createArrayNode());\n");
            sb.append("            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), resultJson);\n");
        } else {
            sb.append("            ObjectNode resultJson = mapper.createObjectNode();\n");
            sb.append("            resultJson.set(\"input\", root);\n");
            sb.append("            resultJson.put(\"output\", resultToString(result));\n");
            // ── end session FIRST so trace file is fully flushed ──
            sb.append("            TraceRecorder.flush();\n");
            // ── then read nodeIds from the trace file ──
            sb.append("            Path traceFile = TraceRecorder.getTraceFile();\n");
            sb.append("            ArrayNode arrayNode = mapper.createArrayNode();\n");
            sb.append("            if (traceFile != null && java.nio.file.Files.exists(traceFile)) {\n");
            sb.append("                java.nio.file.Files.lines(traceFile, java.nio.charset.StandardCharsets.UTF_8)\n");
            sb.append("                    .forEach(line -> {\n");
            sb.append("                        try {\n");
            sb.append("                            int nodeId = mapper.readTree(line).get(\"nodeId\").asInt();\n");
            sb.append("                            arrayNode.add(nodeId);\n");
            sb.append("                        } catch (Exception ignored) {}\n");
            sb.append("                    });\n");
            sb.append("            }\n");
            sb.append("            resultJson.set(\"coveredNodeIds\", arrayNode);\n");
            sb.append("            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), resultJson);\n");
        }

        // ── finally block no longer needs to call endSession for non-void
        //    but still needs it as safety net for the void case and exceptions
        sb.append("            } finally {\n");
        sb.append("                TraceRecorder.flush();\n");
        sb.append("                TraceRecorder.endSession();\n");  // safe: endSession() is idempotent
        sb.append("            }\n");


        sb.append("        } catch (Exception e) {\n");
        sb.append("            System.err.println(\"[DriverMain] Invocation failure: \" + e);\n");
        sb.append("            e.printStackTrace(System.err);\n");
        sb.append("            System.exit(5);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        // ── resolveParamClasses ──────────────────────────────────────────────
        sb.append("    private static Class<?>[] resolveParamClasses() throws ClassNotFoundException {\n");
        sb.append("        Class<?>[] classes = new Class<?>[PARAM_TYPES.length];\n");
        sb.append("        for (int i = 0; i < PARAM_TYPES.length; i++) {\n");
        sb.append("            classes[i] = typeNameToClass(PARAM_TYPES[i]);\n");
        sb.append("        }\n");
        sb.append("        return classes;\n");
        sb.append("    }\n\n");

        sb.append("    private static Class<?> typeNameToClass(String typeName) throws ClassNotFoundException {\n");
        sb.append("        return switch (typeName) {\n");
        sb.append("            case \"int\"        -> int.class;\n");
        sb.append("            case \"long\"       -> long.class;\n");
        sb.append("            case \"short\"      -> short.class;\n");
        sb.append("            case \"byte\"       -> byte.class;\n");
        sb.append("            case \"char\"       -> char.class;\n");
        sb.append("            case \"boolean\"    -> boolean.class;\n");
        sb.append("            case \"float\"      -> float.class;\n");
        sb.append("            case \"double\"     -> double.class;\n");
        sb.append("            case \"String\"     -> String.class;\n");
        sb.append("            case \"int[]\"      -> int[].class;\n");
        sb.append("            case \"long[]\"     -> long[].class;\n");
        sb.append("            case \"short[]\"    -> short[].class;\n");
        sb.append("            case \"byte[]\"     -> byte[].class;\n");
        sb.append("            case \"char[]\"     -> char[].class;\n");
        sb.append("            case \"boolean[]\"  -> boolean[].class;\n");
        sb.append("            case \"float[]\"    -> float[].class;\n");
        sb.append("            case \"double[]\"   -> double[].class;\n");
        sb.append("            case \"String[]\"   -> String[].class;\n");
        sb.append("            case \"int[][]\"    -> int[][].class;\n");
        sb.append("            case \"long[][]\"   -> long[][].class;\n");
        sb.append("            case \"short[][]\"  -> short[][].class;\n");
        sb.append("            case \"byte[][]\"   -> byte[][].class;\n");
        sb.append("            case \"char[][]\"   -> char[][].class;\n");
        sb.append("            case \"boolean[][]\"-> boolean[][].class;\n");
        sb.append("            case \"float[][]\"  -> float[][].class;\n");
        sb.append("            case \"double[][]\" -> double[][].class;\n");
        sb.append("            default -> Class.forName(typeName);\n");
        sb.append("        };\n");
        sb.append("    }\n\n");

        // ── resultToString ────────────────────────────────────────────────────
        sb.append("    /** Deep-toString for primitive arrays. */\n");
        sb.append("    private static String resultToString(Object r) {\n");
        sb.append("        if (r == null) return \"null\";\n");
        sb.append("        if (r instanceof int[]    v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof long[]   v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof short[]  v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof byte[]   v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof char[]   v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof boolean[]v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof float[]  v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof double[] v) return java.util.Arrays.toString(v);\n");
        sb.append("        if (r instanceof int[][]  v) return java.util.Arrays.deepToString(v);\n");
        sb.append("        if (r instanceof long[][] v) return java.util.Arrays.deepToString(v);\n");
        sb.append("        if (r instanceof Object[] v) return java.util.Arrays.deepToString(v);\n");
        sb.append("        return r.toString();\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        return sb.toString();
    }
}
