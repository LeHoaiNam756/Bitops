package core;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests whether JDT ASTParser can resolve bindings for "nums.length" using JUnit 4
 */
public class ArrayLengthBindingTest {

    private static final String SOURCE = String.join("\n",
            "public class Subject {",
            "    public void function(int[] nums) {",
            "        int n = nums.length;",
            "    }",
            "}"
    );

    private CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(source.toCharArray());
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(new String[0], new String[0], new String[0], true);
        parser.setUnitName("Subject.java");

        @SuppressWarnings("unchecked")
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
        parser.setCompilerOptions(options);

        return (CompilationUnit) parser.createAST(null);
    }

    private List<ASTNode> collectLengthAccesses(CompilationUnit cu) {
        final List<ASTNode> found = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(QualifiedName node) {
                if ("length".equals(node.getName().getIdentifier())) {
                    found.add(node);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(FieldAccess node) {
                if ("length".equals(node.getName().getIdentifier())) {
                    found.add(node);
                }
                return super.visit(node);
            }
        });
        return found;
    }

    @Test
    public void numsLengthNodeExists() {
        CompilationUnit cu = parse(SOURCE);
        List<ASTNode> nodes = collectLengthAccesses(cu);

        Assert.assertFalse("Expected at least one '.length' access node in the AST", nodes.isEmpty());
    }

    @Test
    public void numsLengthHasBinding() {
        CompilationUnit cu = parse(SOURCE);
        List<ASTNode> nodes = collectLengthAccesses(cu);
        Assert.assertFalse(nodes.isEmpty());

        ASTNode node = nodes.get(0);
        IBinding binding = getBinding(node);

        Assert.assertNotNull("resolveBinding() returned null for nums.length", binding);
    }

    @Test
    public void numsLengthBindingIsVariable() {
        CompilationUnit cu = parse(SOURCE);
        List<ASTNode> nodes = collectLengthAccesses(cu);
        IBinding binding = getBinding(nodes.get(0));

        Assert.assertNotNull(binding);
        Assert.assertEquals("Expected IBinding.VARIABLE kind", IBinding.VARIABLE, binding.getKind());
    }

    @Test
    public void numsLengthVariableBindingDetails() {
        CompilationUnit cu = parse(SOURCE);
        List<ASTNode> nodes = collectLengthAccesses(cu);
        IBinding binding = getBinding(nodes.get(0));

        Assert.assertTrue("Binding should be IVariableBinding", binding instanceof IVariableBinding);
        IVariableBinding varBinding = (IVariableBinding) binding;

        Assert.assertTrue("Expected isField() == true for array length", varBinding.isField());
        Assert.assertNotNull(varBinding.getType());
        Assert.assertEquals("int", varBinding.getType().getName());
    }

    @Test
    public void numsQualifierResolvesToParameterBinding() {
        CompilationUnit cu = parse(SOURCE);
        final List<SimpleName> numsNames = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                if ("nums".equals(node.getIdentifier())) {
                    numsNames.add(node);
                }
                return super.visit(node);
            }
        });

        Assert.assertFalse("No SimpleName 'nums' found", numsNames.isEmpty());

        boolean foundParameterBinding = false;
        for (SimpleName name : numsNames) {
            IBinding b = name.resolveBinding();
            if (b instanceof IVariableBinding) {
                if (((IVariableBinding) b).isParameter()) {
                    foundParameterBinding = true;
                    break;
                }
            }
        }

        Assert.assertTrue("Expected at least one 'nums' to resolve to a parameter", foundParameterBinding);
    }

    private static IBinding getBinding(ASTNode node) {
        if (node instanceof QualifiedName) {
            return ((QualifiedName) node).resolveBinding();
        }
        if (node instanceof FieldAccess) {
            return ((FieldAccess) node).resolveFieldBinding();
        }
        throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
    }
}
