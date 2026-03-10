package test;

import org.eclipse.jdt.core.dom.*;
import core.CFG.CfgBlockNode;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;

import java.util.*;

public class ParserForTest {
    public static CfgNode generateBlockFromSource(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }

        String trimmed = source.trim();

        String cuSource;
        if (trimmed.startsWith("package ")
                || trimmed.contains("class ")
                || trimmed.contains("interface ")
                || trimmed.contains("enum ")) {
            cuSource = source;
        } else {
            cuSource = "public class __Wrapper__ {\n" + source + "\n}\n";
        }

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cuSource.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        final Block[] foundBody = new Block[1];
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (foundBody[0] == null && node.getBody() != null) {
                    foundBody[0] = node.getBody();
                }
                return foundBody[0] == null;
            }

            @Override
            public boolean visit(Initializer node) {
                if (foundBody[0] == null && node.getBody() != null) {
                    foundBody[0] = node.getBody();
                }
                return foundBody[0] == null;
            }
        });

        CfgNode block = new CfgBlockNode();

        if (foundBody[0] != null) {
            Block body = foundBody[0];
            block.setAst(body);
            block.setContent(body.toString());
            block.setStartPosition(body.getStartPosition());
            block.setEndPosition(body.getStartPosition() + body.getLength());
        } else {
            // Fallback: at least preserve the raw text for debugging.
            block.setAst(cu);
            block.setContent(source);
        }

        return block;
    }

    /**
     * Helper function that parses a source code string and returns a list of Eclipse JDT DOM ASTNode.
     * The source code is wrapped in a method body if it's not already a full compilation unit.
     * 
     * @param source The source code string to parse
     * @return List of Eclipse JDT DOM ASTNode objects parsed from the source code
     */
    public static List<ASTNode> parseSourceToAstNodeList(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source must not be null or empty");
        }

        String trimmed = source.trim();
        
        String cuSource;
        if (trimmed.startsWith("package ") 
                || trimmed.contains("class ") 
                || trimmed.contains("interface ") 
                || trimmed.contains("enum ")) {
            cuSource = source;
        } else {
            cuSource = "public class __Wrapper__ {\n" +
                      "    public void test() {\n" +
                      "        " + source + "\n" +
                      "    }\n" +
                      "}\n";
        }

        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(cuSource.toCharArray());
        parser.setResolveBindings(false);
        parser.setBindingsRecovery(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        final List<ASTNode> eclipseAstNodes = new ArrayList<>();
        
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                Block body = node.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Statement> statements = body.statements();
                    eclipseAstNodes.addAll(statements);
                }
                return false;
            }

            @Override
            public boolean visit(ExpressionStatement node) {
                if (!eclipseAstNodes.contains(node)) {
                    eclipseAstNodes.add(node);
                }
                return false;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                if (!eclipseAstNodes.contains(node)) {
                    eclipseAstNodes.add(node);
                }
                return false;
            }
        });

        return eclipseAstNodes;
    }

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

    /**
     * Debug helper to print all CFG node positions
     */
    public static void printCfgNodePositions(CfgNode rootNode) {
        Map<String, List<Integer>> contentToPositions = new HashMap<>();
        Set<CfgNode> visited = new HashSet<>();
        Queue<CfgNode> queue = new LinkedList<>();
        queue.add(rootNode);

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();
            if (node == null || visited.contains(node)) {
                continue;
            }
            visited.add(node);

            String content = node.getContent();
            if (content != null && !content.trim().isEmpty()) {
                String key = content.trim();
                contentToPositions.computeIfAbsent(key, k -> new ArrayList<>()).add(node.getStartPosition());
            }

            if (node instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) node;
                if (boolNode.getTrueNode() != null) {
                    queue.add(boolNode.getTrueNode());
                }
                if (boolNode.getFalseNode() != null) {
                    queue.add(boolNode.getFalseNode());
                }
            }
            else {
                if (node.getAfterNode() != null) {
                    queue.add(node.getAfterNode());
                }
            }
        }

        System.out.println("=== CFG Node Start Positions ===");
        for (Map.Entry<String, List<Integer>> entry : contentToPositions.entrySet()) {
            System.out.println("\"" + entry.getKey() + "\" -> " + entry.getValue());
        }
        System.out.println("=================================");
    }
}

