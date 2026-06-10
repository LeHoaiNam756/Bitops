package core.testdriver;

import core.instrument.InstrumentationFactory;
import core.utils.Compiler;
import core.utils.FilePath;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDriverEmitterTest {

    @After
    public void tearDown() throws Exception {
        Path driverClass = Path.of(
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES,
                "sample",
                "DriverMain.class"
        );
        Files.deleteIfExists(driverClass);
    }

    @Test
    public void emittedDriverWritesMethodResultToOutputJson() {
        String emitted = TestDriverEmitter.emitDriver(
                "sample",
                "Calculator",
                "add",
                List.of(
                        new TestDriver.ParamInfo("a", "int"),
                        new TestDriver.ParamInfo("b", "int")
                ),
                "int"
        );

        assertTrue("driver should parse --output", emitted.contains("--output="));
        assertTrue("driver should create a result JSON object", emitted.contains("ObjectNode resultJson"));
        assertTrue("driver should persist original input", emitted.contains("resultJson.set(\"input\", root)"));
        assertTrue("driver should persist method output", emitted.contains("resultJson.put(\"output\", resultToString(result))"));
        assertTrue("driver should persist coverage snapshot", emitted.contains("resultJson.set(\"coveredNodeIds\""));
        assertTrue("driver should snapshot in-memory coverage before TraceRecorder.endSession clears it",
                emitted.contains("TraceRecorder.coveredNodeIdsSnapshot()"));
        assertFalse("driver should not read coverage from the trace file before it is flushed",
                emitted.contains("Files.lines(traceFile"));
        assertTrue("driver should write the output file", emitted.contains("writeValue(new File(outputPath), resultJson)"));
    }

    @Test
    public void emittedDriverWrapsInvocationInTraceSession() {
        String emitted = TestDriverEmitter.emitDriver(
                "sample",
                "Calculator",
                "add",
                List.of(
                        new TestDriver.ParamInfo("a", "int"),
                        new TestDriver.ParamInfo("b", "int")
                ),
                "int"
        );

        assertTrue("driver should import TraceRecorder", emitted.contains("import core.testpath.TraceRecorder;"));
        assertTrue("driver should keep the trace session name", emitted.contains("SESSION_NAME = \"sample.Calculator#add\""));
        assertTrue("driver should start a trace session", emitted.contains("TraceRecorder.startSession(SESSION_NAME"));
        assertTrue("driver should end the trace session", emitted.contains("TraceRecorder.endSession();"));
    }

    @Test
    public void generateCompilesGeneratedDriverForCurrentUnit() throws Exception {
        String source = """
                package sample;

                public class Calculator {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        Path tempDir = Files.createTempDirectory("ct4j-driver-test");
        Path sourceDir = tempDir.resolve("sample");
        Files.createDirectories(sourceDir);
        Path instrumentedFile = sourceDir.resolve("Calculator.java");
        Files.writeString(instrumentedFile, source, StandardCharsets.UTF_8);
        Compiler.getInstance().compileJavaFile(
                instrumentedFile.toString(),
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES
        );

        Path driverClass = Path.of(
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES,
                "sample",
                "DriverMain.class"
        );
        Files.deleteIfExists(driverClass);

        CompilationUnit cu = parse(source);
        MethodDeclaration method = ((TypeDeclaration) cu.types().get(0)).getMethods()[0];
        TestDriver.generate(
                new InstrumentationFactory.InstrumentationProduct(null, null, instrumentedFile, null),
                method
        );

        assertTrue("generated driver should be compiled for subprocess execution", Files.exists(driverClass));
    }

    @Test
    public void resolveDriverFqnPrefersMostRecentlyGeneratedDriver() throws Exception {
        Path classesDir = Files.createTempDirectory("ct4j-driver-fqn-test");
        Path staleDriver = classesDir.resolve(Path.of("stale", "DriverMain.class"));
        Path currentDriver = classesDir.resolve(Path.of("wanted", "DriverMain.class"));
        Files.createDirectories(staleDriver.getParent());
        Files.createDirectories(currentDriver.getParent());
        Files.write(staleDriver, new byte[] {0});
        Files.write(currentDriver, new byte[] {0});

        var field = TestDriver.class.getDeclaredField("currentDriverFqn");
        field.setAccessible(true);
        field.set(null, "wanted.DriverMain");

        String driverFqn = TestDriver.resolveDriverFqn(classesDir);

        assertTrue("runner should use the driver generated for the current method",
                "wanted.DriverMain".equals(driverFqn));
    }

    @Test
    public void generatedDriverInvokesPublicConstructorUnit() throws Exception {
        String source = """
                package sample;

                public class CtorSubject {
                    public CtorSubject(int x) {
                        core.testpath.TraceRecorder.mark(101, core.instrument.TraceKind.NODE);
                    }
                }
                """;

        MethodDeclaration constructor = compileAndGenerateDriver(
                "CtorSubject",
                source,
                method -> method.isConstructor() && method.parameters().size() == 1
        );

        TestData data = TestDriver.run(
                Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                TestDriver.extractParams(constructor),
                Map.of("x", 7),
                Files.createTempDirectory("ct4j-driver-run")
        );

        assertEquals("CONSTRUCTED: CtorSubject", data.output());
        assertTrue("constructor coverage should be captured before endSession clears it",
                data.coveredNodeIds().contains(101));
    }

    @Test
    public void generatedDriverInvokesPrivateConstructorUnit() throws Exception {
        String source = """
                package sample;

                public class PrivateCtorSubject {
                    private PrivateCtorSubject(String value) {
                        core.testpath.TraceRecorder.mark(102, core.instrument.TraceKind.NODE);
                    }
                }
                """;

        MethodDeclaration constructor = compileAndGenerateDriver(
                "PrivateCtorSubject",
                source,
                method -> method.isConstructor()
        );

        TestData data = TestDriver.run(
                Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                TestDriver.extractParams(constructor),
                Map.of("value", "abc"),
                Files.createTempDirectory("ct4j-driver-run")
        );

        assertEquals("CONSTRUCTED: PrivateCtorSubject", data.output());
        assertTrue(data.coveredNodeIds().contains(102));
    }

    @Test
    public void generatedDriverCapturesConstructorExceptionCoverage() throws Exception {
        String source = """
                package sample;

                public class FailingCtorSubject {
                    public FailingCtorSubject(String value) {
                        core.testpath.TraceRecorder.mark(103, core.instrument.TraceKind.NODE);
                        throw new IllegalArgumentException("bad " + value);
                    }
                }
                """;

        MethodDeclaration constructor = compileAndGenerateDriver(
                "FailingCtorSubject",
                source,
                method -> method.isConstructor()
        );

        TestData data = TestDriver.run(
                Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                TestDriver.extractParams(constructor),
                Map.of("value", "input"),
                Files.createTempDirectory("ct4j-driver-run")
        );

        assertTrue(data.output().startsWith("EXCEPTION: java.lang.IllegalArgumentException: bad input"));
        assertTrue("exception path should still report covered nodes",
                data.coveredNodeIds().contains(103));
    }

    @Test
    public void generatedDriverInvokesPrivateMethodUnit() throws Exception {
        String source = """
                package sample;

                public class PrivateMethodSubject {
                    private int twice(int x) {
                        core.testpath.TraceRecorder.mark(104, core.instrument.TraceKind.NODE);
                        return x * 2;
                    }
                }
                """;

        MethodDeclaration method = compileAndGenerateDriver(
                "PrivateMethodSubject",
                source,
                candidate -> "twice".equals(candidate.getName().getIdentifier())
        );

        TestData data = TestDriver.run(
                Path.of(FilePath.PATH_TO_MAVEN_TARGET_CLASSES),
                TestDriver.extractParams(method),
                Map.of("x", 3),
                Files.createTempDirectory("ct4j-driver-run")
        );

        assertEquals("6", data.output());
        assertTrue(data.coveredNodeIds().contains(104));
    }

    private static CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        return (CompilationUnit) parser.createAST(null);
    }

    private static MethodDeclaration compileAndGenerateDriver(String className,
                                                              String source,
                                                              Predicate<MethodDeclaration> selector)
            throws Exception {
        Path tempDir = Files.createTempDirectory("ct4j-driver-test");
        Path sourceDir = tempDir.resolve("sample");
        Files.createDirectories(sourceDir);
        Path instrumentedFile = sourceDir.resolve(className + ".java");
        Files.writeString(instrumentedFile, source, StandardCharsets.UTF_8);
        Compiler.getInstance().compileJavaFile(
                instrumentedFile.toString(),
                FilePath.PATH_TO_MAVEN_TARGET_CLASSES
        );

        CompilationUnit cu = parse(source);
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration method = null;
        for (MethodDeclaration candidate : type.getMethods()) {
            if (selector.test(candidate)) {
                method = candidate;
                break;
            }
        }
        assertTrue("test source should contain selected unit", method != null);

        TestDriver.generate(
                new InstrumentationFactory.InstrumentationProduct(null, null, instrumentedFile, null),
                method
        );
        return method;
    }
}
