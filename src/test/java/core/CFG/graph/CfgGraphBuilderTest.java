package core.CFG.graph;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import test.ParserForTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class CfgGraphBuilderTest {
    @Test
    public void buildsGraphFromMethod() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/test/resources/refactor/SampleMethods.java")));
        List<ASTNode> methods = ParserForTest.parseSourceToAstFuncList(source);
        MethodDeclaration method = (MethodDeclaration) methods.get(0);

        CfgGraph graph = core.CFG.Utils.ASTHelper.generateCfg(method);
        assertNotNull(graph);
        assertTrue(graph.nodeCount() > 0);
        assertTrue(graph.edgeCount() > 0);
    }
}
