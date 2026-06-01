package utils;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.*;

public class Parser {
    public static List<ASTNode> parseSourceToAstFuncList(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source must not be null or empty");
        }

        String wrappedSource = "class DummyWrapper {\n" + source + "\n}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(wrappedSource.toCharArray());
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<ASTNode> methods = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                // Collecting MethodDeclaration specifically
                methods.add(node);
                return super.visit(node);
            }
        });

        return methods;
    }

    public static ASTNode parseStatementToAST(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new IllegalArgumentException("Statement must not be null or empty");
        }
        // wrap in a class and method
        String src = "class DummyWrapper {\n" +
                "    void dummy() {\n" +
                "        " + statement + "\n" +
                "    }\n" +
                "}";
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(src.toCharArray());
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setUnitName("Test.java");
        String[] classpath =
                System.getProperty("java.class.path")
                        .split(File.pathSeparator);
        parser.setEnvironment(
                classpath,
                new String[]{""},
                null,
                true
        );
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        // get the first type (should be the class)
        TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
        // get the first method
        MethodDeclaration method = type.getMethods()[0];
        Block body = method.getBody();
        // return first statement (assuming it's present)
        List<?> statements = body.statements();
        if (!statements.isEmpty()) {
            return (ASTNode) statements.get(0);
        }
        return null;
    }
}

