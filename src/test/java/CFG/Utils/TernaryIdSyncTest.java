package CFG.Utils;

import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.CFG.Utils.TernarySplitHelper;
import core.utils.AstIdGenerator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.junit.Test;
import test.ParserForTest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class TernaryIdSyncTest {

    @Test
    public void cfgIds_matchConvertedTernaryIds() {
        ASTNode stmt = ParserForTest.parseSourceToAstNodeList("return x > 0 ? 1 : -1;").get(0);
        TernarySplitHelper.TernarySplitResult split = TernarySplitHelper.split(stmt).get();

        ConditionalExpression original = split.getOriginalConditionalExpression();
        Map<String, String> map = split.getOriginalPartIdToConvertedPartId();

        String condId = map.get(AstIdGenerator.buildSignature(original.getExpression()));
        String thenId = map.get(AstIdGenerator.buildSignature(original.getThenExpression()));
        String elseId = map.get(AstIdGenerator.buildSignature(original.getElseExpression()));

        String source = "public int test(int x) { return x > 0 ? 1 : -1; }";
        CfgNode block = ParserForTest.generateBlockFromSource(source);
        CfgNode before = new CfgNode();
        CfgNode after = new CfgNode();
        block.setBeforeNode(before);
        block.setAfterNode(after);

        CfgNode cfg = ASTHelper.generateCfg(block, null, ASTHelper.Coverage.STATEMENT);
        Set<String> cfgIds = collectIds(cfg);

        assertTrue(cfgIds.contains(condId));
        assertTrue(cfgIds.contains(thenId));
        assertTrue(cfgIds.contains(elseId));
    }

    private Set<String> collectIds(CfgNode root) {
        Set<String> ids = new HashSet<>();
        Set<CfgNode> visited = new HashSet<>();
        Queue<CfgNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();
            if (node == null || visited.contains(node)) {
                continue;
            }
            visited.add(node);
            ids.add(node.getId());

            if (node.getAfterNode() != null) {
                queue.add(node.getAfterNode());
            }
            if (node.getBeforeNode() != null) {
                queue.add(node.getBeforeNode());
            }
            if (node instanceof core.CFG.CfgBoolExprNode) {
                core.CFG.CfgBoolExprNode boolNode = (core.CFG.CfgBoolExprNode) node;
                queue.add(boolNode.getTrueNode());
                queue.add(boolNode.getFalseNode());
            }
        }
        return ids;
    }
}
