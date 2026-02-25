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
    /**
     * Parses a Java file and returns a list of AST nodes representing functions/methods.
     * 
     * @param filePath the path to the Java source file
     * @return a list of AST nodes representing methods in the file, or an empty list if parsing fails
     */
    public static List<ASTNode> parseFile(String filePath) {
        try {
            String sourceCode = readFileToString(filePath);
            
            return parseSourceCodeToAstFuncList(sourceCode);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Reads a file and returns its content as a String.
     * 
     * @param filePath the path to the file to read
     * @return the file content as a String
     * @throws IOException if an I/O error occurs while reading the file
     */
    private static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }

    /**
     * Parses Java source code and extracts AST nodes for all methods (excluding constructors).
     * 
     * @param sourceCode the Java source code to parse
     * @return a list of AST nodes representing methods in the source code
     */
    private static List<ASTNode> parseSourceCodeToAstFuncList(String sourceCode) {
        List<ASTNode> astFuncList = new ArrayList<>();
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
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
     * Parses a Java file and returns its CompilationUnit AST node.
     * This method enables binding resolution and recovery for better AST analysis.
     * 
     * @param filePath the path to the Java source file
     * @return the CompilationUnit AST node, or null if parsing fails
     */
    public static CompilationUnit getCompilationUnit(String filePath) {
        try {
            String sourceCode = readFileToString(filePath);
            return parseSourceCodeToCompilationUnit(sourceCode);
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses Java source code and returns a CompilationUnit with binding resolution enabled.
     * 
     * @param sourceCode the Java source code to parse
     * @return the CompilationUnit AST node
     */
    private static CompilationUnit parseSourceCodeToCompilationUnit(String sourceCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(sourceCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        
        return (CompilationUnit) parser.createAST(null);
    }
}
