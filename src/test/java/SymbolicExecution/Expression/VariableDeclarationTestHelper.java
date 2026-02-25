package SymbolicExecution.Expression;

import org.eclipse.jdt.core.dom.*;

/**
 * Helper class for creating AST nodes from strings for testing variable declarations.
 * Supports VariableDeclaration, VariableDeclarationExpression, and VariableDeclarationStatement.
 */
public class VariableDeclarationTestHelper {

    /**
     * Parses a string and returns a VariableDeclaration AST node.
     * The string should contain a variable declaration (e.g., method parameter: "int x").
     * 
     * @param source The source code string containing a variable declaration
     * @return VariableDeclaration AST node (typically SingleVariableDeclaration)
     */
    public static VariableDeclaration parseVariableDeclaration(String source) {
        return parseASTNode(source, VariableDeclaration.class);
    }

    /**
     * Parses a string and returns a VariableDeclarationExpression AST node.
     * The string should contain a variable declaration expression (e.g., "int x, y = 5;").
     * 
     * @param source The source code string containing a variable declaration expression
     * @return VariableDeclarationExpression AST node
     */
    public static VariableDeclarationExpression parseVariableDeclarationExpression(String source) {
        return parseASTNode(source, VariableDeclarationExpression.class);
    }

    /**
     * Parses a string and returns a VariableDeclarationStatement AST node.
     * The string should contain a variable declaration statement (e.g., "int x = 5;").
     * 
     * @param source The source code string containing a variable declaration statement
     * @return VariableDeclarationStatement AST node
     */
    public static VariableDeclarationStatement parseVariableDeclarationStatement(String source) {
        return parseASTNode(source, VariableDeclarationStatement.class);
    }

    /**
     * Parses a string and returns a SingleVariableDeclaration AST node.
     * The string should contain a single variable declaration (e.g., method parameter: "int x").
     * 
     * @param source The source code string containing a single variable declaration
     * @return SingleVariableDeclaration AST node
     */
    public static SingleVariableDeclaration parseSingleVariableDeclaration(String source) {
        return parseASTNode(source, SingleVariableDeclaration.class);
    }

    /**
     * Parses a string and returns a SingleVariableDeclaration AST node from a catch clause.
     * The string should contain a single variable declaration with optional initializer (e.g., "Exception e").
     * 
     * @param source The source code string containing a single variable declaration for catch clause
     * @return SingleVariableDeclaration AST node
     */
    public static SingleVariableDeclaration parseSingleVariableDeclarationFromCatch(String source) {
        return parseASTNodeFromCatch(source, SingleVariableDeclaration.class);
    }

    /**
     * Internal helper method to parse a string from a catch clause and extract the first AST node of the specified type.
     * 
     * @param source The source code string
     * @param nodeType The class type of the AST node to find
     * @param <T> The type of AST node to return
     * @return The first AST node of the specified type found
     */
    @SuppressWarnings("unchecked")
    private static <T extends ASTNode> T parseASTNodeFromCatch(String source, Class<T> nodeType) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source must not be null or empty");
        }

        String trimmed = source.trim();
        String cleanSource = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        
        // Wrap in a catch clause
        String cuSource = "public class __Wrapper__ {\n" +
                          "    public void test() {\n" +
                          "        try {} catch (" + cleanSource + ") {}\n" +
                          "    }\n" +
                          "}\n";

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cuSource.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        final ASTNode[] foundNode = new ASTNode[1];
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SingleVariableDeclaration node) {
                if (foundNode[0] == null && nodeType.isInstance(node)) {
                    foundNode[0] = node;
                    return false; // Stop after finding the first one
                }
                return foundNode[0] == null; // Continue if not found yet
            }
        });

        if (foundNode[0] == null) {
            throw new IllegalArgumentException("No " + nodeType.getSimpleName() + " found in source: " + source);
        }

        return (T) foundNode[0];
    }

    /**
     * Internal helper method to parse a string and extract the first AST node of the specified type.
     * 
     * @param source The source code string
     * @param nodeType The class type of the AST node to find
     * @param <T> The type of AST node to return
     * @return The first AST node of the specified type found
     */
    @SuppressWarnings("unchecked")
    private static <T extends ASTNode> T parseASTNode(String source, Class<T> nodeType) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source must not be null or empty");
        }

        String trimmed = source.trim();
        
        // Wrap the source in a method if it's not already a full compilation unit
        String cuSource;
        if (trimmed.startsWith("package ") 
                || trimmed.contains("class ") 
                || trimmed.contains("interface ") 
                || trimmed.contains("enum ")) {
            cuSource = source;
        } else {
            // VariableDeclarationExpression is used in for loops, so wrap it in a for loop
            if (nodeType == VariableDeclarationExpression.class) {
                // Remove semicolon if present (for loop initialization doesn't need it)
                String cleanSource = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                cuSource = "public class __Wrapper__ {\n" +
                          "    public void test() {\n" +
                          "        for (" + cleanSource + "; ; ) {}\n" +
                          "    }\n" +
                          "}\n";
            } else if (nodeType == SingleVariableDeclaration.class) {
                // SingleVariableDeclaration is used in method parameters, so wrap it in a method signature
                // Remove semicolon if present (method parameters don't have semicolons)
                String cleanSource = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                cuSource = "public class __Wrapper__ {\n" +
                          "    public void test(" + cleanSource + ") {}\n" +
                          "}\n";
            } else {
                // Wrap in a method body for statements
                cuSource = "public class __Wrapper__ {\n" +
                          "    public void test() {\n" +
                          "        " + source + "\n" +
                          "    }\n" +
                          "}\n";
            }
        }

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cuSource.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        final ASTNode[] foundNode = new ASTNode[1];
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationStatement node) {
                if (foundNode[0] == null && nodeType.isInstance(node)) {
                    foundNode[0] = node;
                    return false; // Stop after finding the first one
                }
                return foundNode[0] == null; // Continue if not found yet
            }

            @Override
            public boolean visit(VariableDeclarationExpression node) {
                if (foundNode[0] == null && nodeType.isInstance(node)) {
                    foundNode[0] = node;
                    return false; // Stop after finding the first one
                }
                return foundNode[0] == null; // Continue if not found yet
            }

            @Override
            public boolean visit(SingleVariableDeclaration node) {
                // SingleVariableDeclaration implements VariableDeclaration
                // Used for method parameters, catch clauses, etc.
                if (foundNode[0] == null && nodeType.isInstance(node)) {
                    foundNode[0] = node;
                    return false; // Stop after finding the first one
                }
                return foundNode[0] == null; // Continue if not found yet
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                // VariableDeclarationFragment implements VariableDeclaration
                // Used in VariableDeclarationStatement and VariableDeclarationExpression
                if (foundNode[0] == null && nodeType.isInstance(node)) {
                    foundNode[0] = node;
                    return false; // Stop after finding the first one
                }
                return foundNode[0] == null; // Continue if not found yet
            }
        });

        if (foundNode[0] == null) {
            throw new IllegalArgumentException("No " + nodeType.getSimpleName() + " found in source: " + source);
        }

        return (T) foundNode[0];
    }
}

