package utils;

import org.eclipse.jdt.core.dom.*;

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
}

