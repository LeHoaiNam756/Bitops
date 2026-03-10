package SymbolicExecution.Expression;

import core.SymbolicExecution.AstNode.Expression.MethodInvocationNode;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import static org.junit.Assert.*;
import test.ParserForTest;

import java.util.ArrayList;
import java.util.List;

public class TestMethodInvocationNode {
    @Test
    public void testMethodInvocation_1() {
        String fullSrc = "class TestClass {\n" +
                "    public void methodA(String name) { }\n" +
                "    public void trigger() { methodA(\"Alice\"); }\n" +
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fullSrc.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (!node.getName().getIdentifier().equals("trigger")) decls.add(node);
                return true;
            }
            public boolean visit(MethodInvocation node) {
                invox.add(node);
                return true;
            }
        });

        MethodDeclaration matched = MethodInvocationNode.getInvokedMethodAST(invox.get(0), new ArrayList<>(decls));

        assertNotNull(matched);
        assertEquals("methodA", matched.getName().getIdentifier());
    }

    @Test
    public void testMethodInvocation_2() {
        String fullSrc = "class TestClass {\n" +
                "    public void methodA(String name) { }\n" +
                "    public void methodA(int number) { }\n" +
                "    public void trigger() { methodA(\"Alice\"); }\n" +
                "    public void trigger2() { methodA(42); }\n" +
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fullSrc.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (!node.getName().getIdentifier().equals("trigger")) decls.add(node);
                return true;
            }
            public boolean visit(MethodInvocation node) {
                invox.add(node);
                return true;
            }
        });

        MethodDeclaration matched1 = MethodInvocationNode.getInvokedMethodAST(invox.get(0), new ArrayList<>(decls));
        assertNotNull(matched1);
        assertEquals("methodA", matched1.getName().getIdentifier());
        assertTrue(matched1.parameters().get(0) instanceof SingleVariableDeclaration);
        assertEquals("String",
                ((SingleVariableDeclaration) matched1.parameters().get(0)).getType().toString());
        MethodDeclaration matched2 = MethodInvocationNode.getInvokedMethodAST(invox.get(1), new ArrayList<>(decls));
        assertNotNull(matched2);
        assertEquals("methodA", matched2.getName().getIdentifier());
        assertTrue(matched2.parameters().get(0) instanceof SingleVariableDeclaration);
        assertEquals("int",
                ((SingleVariableDeclaration) matched2.parameters().get(0)).getType().toString());
    }

    @Test
    public void testMethodInvocation_3() {
        String fullSrc = "class TestClass {\n" +
                "    public void methodA(String name) { }\n" +
                "    public void methodA(String lastName, String firstName) { }\n" +
                "    public void trigger() { methodA(\"Alice\"); }\n" +
                "    public void trigger2() { methodA(\"LinLin\", \"Alice\"); }\n" + // Fixed extra paren
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fullSrc.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                String name = node.getName().getIdentifier();
                if (name.equals("methodA")) {
                    decls.add(node);
                }
                return true;
            }
            @Override
            public boolean visit(MethodInvocation node) {
                invox.add(node);
                return true;
            }
        });

        MethodDeclaration matched1 = MethodInvocationNode.getInvokedMethodAST(invox.get(0), new ArrayList<>(decls));
        assertNotNull(matched1);
        assertEquals(1, matched1.parameters().size());

        MethodDeclaration matched2 = MethodInvocationNode.getInvokedMethodAST(invox.get(1), new ArrayList<>(decls));
        assertNotNull(matched2);
        assertEquals(2, matched2.parameters().size());
    }

    @Test
    public void testReplaceMethodInvocationWithStub_1() {
        String fullSrc = "class TestClass {\n" +
                "    public int methodA(int x) { return x + 1; }\n" +
                "    public void trigger() { int result = methodA(5);}\n" +
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fullSrc.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                if (!node.getName().getIdentifier().equals("trigger")) decls.add(node);
                return true;
            }
            public boolean visit(MethodInvocation node) {
                if (node.getName().getIdentifier().equals("methodA")) {
                    invox.add(node);
                }
                return true;
            }
        });

        MethodInvocation trigger = invox.get(0);
        ASTNode parentNode = trigger.getParent();
        assertTrue(parentNode instanceof VariableDeclarationFragment);
        VariableDeclarationFragment vdf = (VariableDeclarationFragment) parentNode;
        MethodInvocationNode.replaceMethodInvocationWithStub(trigger, "stub_val");
        assertEquals("result", vdf.getName().getIdentifier());
        assertTrue(vdf.getInitializer() instanceof SimpleName);
        assertEquals("stub_val", ((SimpleName) vdf.getInitializer()).getIdentifier());
    }

    @Test
    public void testAddStubVariableToParameterList_1() {
        String fullSrc = "class TestClass {\n" +
                "    public int methodA(int x) { return x;}\n" +
                "    public void trigger() { methodA(5);}\n" +
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fullSrc.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                decls.add(node);
                return true;
            }
            public boolean visit(MethodInvocation node) {
                if (node.getName().getIdentifier().equals("methodA")) {
                    invox.add(node);
                }
                return true;
            }
        });
        Type returnOfMethodA = ((MethodDeclaration) decls.get(0)).getReturnType2();
        MethodInvocationNode.addStubVariableToParameterList("stub_param", returnOfMethodA,
                decls.get(1)
                );
        List<?> parameters = decls.get(1).parameters();
        assertEquals(1, parameters.size());
        assertTrue(parameters.get(0) instanceof SingleVariableDeclaration);
        SingleVariableDeclaration param = (SingleVariableDeclaration) parameters.get(0);
        assertEquals("stub_param", param.getName().getIdentifier());
        assertEquals("int", param.getType().toString());
    }

    @Test
    public void testGetInvokedMethodReturnTypeName_1() {
        String src = "class TestClass {\n" +
                "    public void trigger() { int x = Math.min(2, 3);}\n" +
                "}";
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(src.toCharArray());
        parser.setResolveBindings(true);
        parser.setUnitName("TestClass.java");
        parser.setEnvironment(null, null, null, true);
        List<MethodDeclaration> decls = new ArrayList<>();
        List<MethodInvocation> invox = new ArrayList<>();
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                decls.add(node);
                return true;
            }
            public boolean visit(MethodInvocation node) {
                if (node.getName().getIdentifier().equals("min")) {
                    invox.add(node);
                }
                return true;
            }
        });
        String returnTypeName = MethodInvocationNode.getInvokedMethodReturnTypeName(invox.get(0));
        assertEquals("int", returnTypeName);
    }

}
