package TestGeneration;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import core.TestGeneration.testDriver.TestDriverUtils;
import test.ParserForTest;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestForTestDriverUtils {

    @Test
    public void test_getClassOfPrimitiveParameters() {
        String src = "int func(int a, double b, boolean c) { return 0; }";
        List<ASTNode> astNode = ParserForTest.parseSourceToAstFuncList(src);
        ASTNode methodNode = astNode.get(0);
        @SuppressWarnings("unchecked")
        List<ASTNode> parameters = ((MethodDeclaration) methodNode).parameters();
        Class<?>[] parameterClasses = TestDriverUtils.getParameterClasses(parameters);
        assertEquals(3, parameterClasses.length);
        assertEquals(int.class, parameterClasses[0]);
        assertEquals(double.class, parameterClasses[1]);
        assertEquals(boolean.class, parameterClasses[2]);
    }

    @Test
    public void test_getClassOfPrimitiveParameters_2() {
        String src = "void func(char c, float f, long l) { }";
        List<ASTNode> astNode = ParserForTest.parseSourceToAstFuncList(src);
        ASTNode methodNode = astNode.get(0);
        @SuppressWarnings("unchecked")
        List<ASTNode> parameters = ((MethodDeclaration) methodNode).parameters();
        Class<?>[] parameterClasses = TestDriverUtils.getParameterClasses(parameters);
        assertEquals(3, parameterClasses.length);
        assertEquals(char.class, parameterClasses[0]);
        assertEquals(float.class, parameterClasses[1]);
        assertEquals(long.class, parameterClasses[2]);
    }

}
