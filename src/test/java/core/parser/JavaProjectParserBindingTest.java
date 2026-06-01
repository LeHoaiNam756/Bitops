package core.parser;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JavaProjectParserBindingTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesArrayLengthBindingForMethodParameterArray() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project").toPath();
        Path sourceFile = projectRoot.resolve("Subject.java");
        Files.writeString(sourceFile, String.join("\n",
                "public class Subject {",
                "    public int size(int[] nums) {",
                "        return nums.length;",
                "    }",
                "}"), StandardCharsets.UTF_8);

        ParseResult result = new JavaProjectParser().parseDirectory(projectRoot);
        CompilationUnit compilationUnit = result.compilationUnit(Path.of("Subject.java")).orElseThrow();

        List<QualifiedName> lengthAccesses = collectLengthAccesses(compilationUnit);
        assertFalse("Expected nums.length to appear as a QualifiedName", lengthAccesses.isEmpty());

        IBinding binding = lengthAccesses.get(0).resolveBinding();
        assertNotNull("Expected JavaProjectParser to resolve a binding for nums.length", binding);
        assertEquals("Expected nums.length binding to be a variable", IBinding.VARIABLE, binding.getKind());
        assertTrue("Expected nums.length binding to be an IVariableBinding", binding instanceof IVariableBinding);

        IVariableBinding variableBinding = (IVariableBinding) binding;
        assertTrue("Expected array length to resolve as a field", variableBinding.isField());
        assertNotNull(variableBinding.getType());
        assertEquals("int", variableBinding.getType().getName());
    }

    @Test
    public void resolvesArrayLengthBindingWhenProjectContainsManyFiles() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("many-files-project").toPath();
        for (int i = 0; i < 1500; i++) {
            Path sourceFile = projectRoot.resolve("Helper" + i + ".java");
            Files.writeString(sourceFile, String.join("\n",
                    "public class Helper" + i + " {",
                    "    public int function(int[] n) {",
                    "        if (n.length == 0) {",
                    "            return 1;",
                    "        }",
                    "        return n[0];",
                    "    }",
                    "}"), StandardCharsets.UTF_8);
        }

        ParseResult result = new JavaProjectParser().parseDirectory(projectRoot);
        assertEquals("Expected every generated file to be parsed", 1500, result.entries().size());

        for (int i = 0; i < 1500; i++) {
            Path relativePath = Path.of("Helper" + i + ".java");
            CompilationUnit compilationUnit = result.compilationUnit(relativePath).orElseThrow();
            assertArrayLengthBindingResolved(compilationUnit, relativePath.toString());
        }
    }

    @Test
    public void resolvesBindingsForDuplicateDefaultPackageClassNamesInDifferentFolders() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("duplicate-default-package-classes").toPath();
        writeDuplicateSolution(projectRoot.resolve(Path.of("first", "src", "Solution.java")), 1);
        writeDuplicateSolution(projectRoot.resolve(Path.of("second", "src", "Solution.java")), 2);

        ParseResult result = new JavaProjectParser().parseDirectory(projectRoot);

        assertArrayLengthBindingResolved(
                result.compilationUnit(Path.of("first", "src", "Solution.java")).orElseThrow(),
                "first/src/Solution.java");
        assertArrayLengthBindingResolved(
                result.compilationUnit(Path.of("second", "src", "Solution.java")).orElseThrow(),
                "second/src/Solution.java");
    }

    @Test
    public void resolvesBindingsForLeetCodeStyleDuplicateDefaultPackageSources() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("leetcode-style-project").toPath();
        writeDuplicateSolution(projectRoot.resolve(Path.of("28-string", "1324-print-words-vertically", "src", "Solution.java")), 1);
        writeDuplicateSolution(projectRoot.resolve(Path.of("28-string", "1309-decrypt-string-from-alphabet-to-integer-mapping", "src", "Solution.java")), 2);
        writeDuplicateSolution(projectRoot.resolve(Path.of("03-Basic-Sorting", "Insertion-Sort", "src", "Solution.java")), 3);
        writeDuplicateSolution(projectRoot.resolve(Path.of("03-Basic-Sorting", "Insertion-Sort", "src", "Solution2.java")), 4);
        writeDuplicateSolution(projectRoot.resolve(Path.of("03-Basic-Sorting", "Insertion-Sort", "src", "SolutionCopy.java")), 5);

        ParseResult result = new JavaProjectParser().parseDirectory(projectRoot);
        assertEquals("Expected every mock LeetCode source file to be parsed", 5, result.entries().size());

        for (ParseEntry entry : result.entries()) {
            assertArrayLengthBindingResolved(entry.compilationUnit(), entry.relativePath().toString());
        }
    }

    private static void writeDuplicateSolution(Path sourceFile, int returnValue) throws IOException {
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, String.join("\n",
                "public class Solution {",
                "    int function(int[] n) {",
                "        if (n.length == 0) {",
                "            return " + returnValue + ";",
                "        }",
                "        return n[0];",
                "    }",
                "}"), StandardCharsets.UTF_8);
    }

    private static void assertArrayLengthBindingResolved(CompilationUnit compilationUnit, String sourceName) {
        List<QualifiedName> lengthAccesses = collectLengthAccesses(compilationUnit);
        assertFalse("Expected n.length to appear as a QualifiedName in " + sourceName, lengthAccesses.isEmpty());

        IBinding binding = lengthAccesses.get(0).resolveBinding();
        assertNotNull("Expected JavaProjectParser to resolve a binding for n.length in " + sourceName, binding);
        assertEquals("Expected n.length binding to be a variable in " + sourceName, IBinding.VARIABLE, binding.getKind());
        assertTrue("Expected n.length binding to be an IVariableBinding in " + sourceName, binding instanceof IVariableBinding);

        IVariableBinding variableBinding = (IVariableBinding) binding;
        assertTrue("Expected array length to resolve as a field in " + sourceName, variableBinding.isField());
        assertNotNull(variableBinding.getType());
        assertEquals("int", variableBinding.getType().getName());
    }

    private static List<QualifiedName> collectLengthAccesses(CompilationUnit compilationUnit) {
        List<QualifiedName> lengthAccesses = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(QualifiedName node) {
                if ("length".equals(node.getName().getIdentifier())) {
                    lengthAccesses.add(node);
                }
                return super.visit(node);
            }
        });
        return lengthAccesses;
    }
}
