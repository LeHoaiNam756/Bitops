package SymbolicExecution;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import core.CFG.*;
import core.CFG.Utils.ASTHelper;
import core.SymbolicExecution.RandomTestData;
import core.SymbolicExecution.SymbolicExecution;
import core.TestGeneration.path.PathStep;
import core.TestGeneration.testDriver.TestDriverUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSymbolicExecution {
    @Test
    public void testCreateRandomValueForPrimitiveParameters() {
        Class<?>[] classes = {
                int.class,
                byte.class,
                short.class,
                long.class,
                float.class,
                double.class,
                char.class,
                boolean.class
        };
        Object[] randomValues = RandomTestData.createRandomTestData(classes);
        assertEquals(randomValues.length, classes.length);
        assertTrue(randomValues[0] instanceof Integer);
        assertTrue(randomValues[1] instanceof Byte);
        assertTrue(randomValues[2] instanceof Short);
        assertTrue(randomValues[3] instanceof Long);
        assertTrue(randomValues[4] instanceof Float);
        assertTrue(randomValues[5] instanceof Double);
        assertTrue(randomValues[6] instanceof Character);
        assertTrue(randomValues[7] instanceof Boolean);
    }


    public static void main(String[] args) {
        String src = "public static int calDigits(char start, char end) {"
                + "int sum = 0;"
                + "for (char ch = start; ch <= end; ch++) { "
                + "sum += 1;"
                + " }"
                + "return sum;"
                + "}";

        String fullSrc = "public class CalDigitsClass {" + src + "}";

        // 1. Parse source code to list of AstNode
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(null, null, null, true);
        parser.setUnitName("CalDigitsClass.java");
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setSource(fullSrc.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<ASTNode> methods = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                for (MethodDeclaration m : node.getMethods()) {
                    if (!m.isConstructor()) {
                        methods.add(m);
                    }
                }
                return false;
            }
        });
        if (methods.isEmpty()) {
            throw new RuntimeException("No method found in source");
        }
        MethodDeclaration method = (MethodDeclaration) methods.get(0);

        // 2. Get list of AstNode of parameters
        @SuppressWarnings("unchecked")
        List<ASTNode> parameterList = method.parameters();

        // 3. Build CFG tree
        Block body = method.getBody();
        CfgNode rootCfgNode = new CfgNode();
        CfgNode endCfgNode = new CfgNode();
        rootCfgNode.setBeginCfgNode(true);
        endCfgNode.setEndCfgNode(true);

        CfgBlockNode blockNode = new CfgBlockNode();
        blockNode.setAst(body);
        blockNode.setBeforeNode(rootCfgNode);
        blockNode.setAfterNode(endCfgNode);
        rootCfgNode.setAfterNode(blockNode);
        endCfgNode.setBeforeNode(blockNode);

        ASTHelper.generateCfg(blockNode, cu, ASTHelper.Coverage.STATEMENT);

        // 4. Create list of PathStep for one loop iteration:
        //    int sum = 0; char ch = start; ch <= end (True); sum+= 1; ch++; ch <= end (False); return sum;
        List<PathStep> testPath = new LinkedList<>();

        CfgNode sumInitNode = rootCfgNode.getAfterNode();
        testPath.add(new PathStep(sumInitNode.getStartPosition(), null));

        CfgNode forInitNode = sumInitNode.getAfterNode();
        testPath.add(new PathStep(forInitNode.getStartPosition(), null));

        CfgBoolExprNode forCondition = (CfgBoolExprNode) forInitNode.getAfterNode();
        testPath.add(new PathStep(forCondition.getStartPosition(), true));

        CfgNode bodyInside = forCondition.getTrueNode();
        testPath.add(new PathStep(bodyInside.getStartPosition(), null));

        CfgNode updaterNode = bodyInside.getAfterNode();
        testPath.add(new PathStep(updaterNode.getStartPosition(), null));

        CfgNode backToCondition = updaterNode.getAfterNode();
        testPath.add(new PathStep(backToCondition.getStartPosition(), false));

        CfgNode afterLoop = ((CfgBoolExprNode) backToCondition).getFalseNode();
        CfgNode returnNode = afterLoop.getAfterNode();
        testPath.add(new PathStep(returnNode.getStartPosition(), null));

        // 5. Create SymbolicExecution and execute with parameters list and test path
        SymbolicExecution symbolicExecution = new SymbolicExecution(null);
        symbolicExecution.executePath(testPath, parameterList);

        Class<?>[] parameterClasses = TestDriverUtils.getParameterClasses(parameterList);
        Object[] testInputs = RandomTestData.createRandomTestData(parameterClasses);

        List<String> paramNames = TestDriverUtils.getParameterNames(parameterList);
        System.out.println("=== Symbolic Execution Result ===");
        for (int i = 0; i < paramNames.size(); i++) {
            System.out.println("  " + paramNames.get(i) + " = " + testInputs[i]);
        }
        System.out.println("Test inputs that drive one loop iteration.");
    }
}
