package core.utils;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProjectParser {

    private final ASTParser parser;

    private String lastFilePath;
    private String lastSourceCode;
    private CompilationUnit lastCompilationUnit;
    private List<ASTNode> lastMethodDecls;

    public ProjectParser() {
        this.parser = ASTParser.newParser(AST.JLS8);
        configureParser();
    }

    /**
     * Load and parse a Java file, caching its source, CompilationUnit and method list.
     *
     * @param filePath path to the Java source file
     */
    public void loadFile(String filePath) {
        try {
            this.lastSourceCode = readFileToString(filePath);
            this.lastFilePath = filePath;

            this.lastCompilationUnit = parseSourceCodeToCompilationUnit(this.lastSourceCode);
            this.lastMethodDecls = extractMethodsFromCompilationUnit(this.lastCompilationUnit);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            this.lastSourceCode = null;
            this.lastFilePath = null;
            this.lastCompilationUnit = null;
            this.lastMethodDecls = new ArrayList<>();
        }
    }

    /**
     * @return cached CompilationUnit, or throws if {@link #loadFile(String)} was not called.
     */
    public CompilationUnit getCompilationUnit() {
        if (lastCompilationUnit == null) {
            throw new IllegalStateException("CompilationUnit not initialized. Call loadFile() first.");
        }
        return lastCompilationUnit;
    }

    /**
     * @return cached list of method declarations (non-constructors) from last CompilationUnit.
     */
    public List<ASTNode> getMethods() {
        if (lastMethodDecls == null) {
            throw new IllegalStateException("Methods not initialized. Call loadFile() first.");
        }
        return lastMethodDecls;
    }

    /**
     * Clears cached state and reinitializes parser configuration.
     */
    public void reset() {
        lastFilePath = null;
        lastSourceCode = null;
        lastCompilationUnit = null;
        lastMethodDecls = null;
        configureParser();
    }

    /**
     * Reads a file and returns its content as a String.
     *
     * @param filePath the path to the file to read
     * @return the file content as a String
     */
    private static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }

    /**
     * Parses Java source code and returns a CompilationUnit with binding resolution enabled
     * using this instance's {@link #parser}.
     *
     * @param sourceCode the Java source code to parse
     * @return the CompilationUnit AST node
     */
    private CompilationUnit parseSourceCodeToCompilationUnit(String sourceCode) {
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        return (CompilationUnit) parser.createAST(null);
    }

    /**
     * Extracts method declarations (excluding constructors) from a CompilationUnit.
     */
    private static List<ASTNode> extractMethodsFromCompilationUnit(CompilationUnit cu) {
        List<ASTNode> astFuncList = new ArrayList<>();

        ASTVisitor visitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                extractFunctionChildren(node, astFuncList);
                return true;
            }
        };

        cu.accept(visitor);
        return astFuncList;
    }

    /**
     * Extracts method declarations (excluding constructors) from a TypeDeclaration node.
     *
     * @param node the TypeDeclaration node to extract methods from
     * @param astFuncList the list to add the extracted method AST nodes to
     */
    private static void extractFunctionChildren(ASTNode node, List<ASTNode> astFuncList) {
        if (node instanceof TypeDeclaration) {
            List<MethodDeclaration> methods = Arrays.asList(((TypeDeclaration) node).getMethods());
            for (MethodDeclaration method : methods) {
                if (!method.isConstructor()) {
                    astFuncList.add(method);
                }
            }
        }
    }

    /**
     * Configure the underlying ASTParser with binding/compliance options.
     */
    private void configureParser() {
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);


        parser.setEnvironment(null, null, null, true);
        parser.setUnitName("dummy.java");

        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
    }
}
