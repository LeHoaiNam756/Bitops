package core.CFG.graph;

import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class CfgGraphBuilder {
    public static CfgGraph fromLegacy(CfgNode root) {
        CfgGraph graph = new CfgGraph();
        Map<CfgNode, Integer> ids = new HashMap<>();
        Queue<CfgNode> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();
            if (node == null || ids.containsKey(node)) {
                continue;
            }
            int startPosition = node.getAst() != null ? node.getStartPosition() : -1;
            int id = graph.addNode(CfgNodeKind.STMT, node.getAst(), node.getContent(), startPosition);
            ids.put(node, id);

            if (node instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) node;
                if (boolNode.getTrueNode() != null) {
                    queue.add(boolNode.getTrueNode());
                }
                if (boolNode.getFalseNode() != null) {
                    queue.add(boolNode.getFalseNode());
                }
            } else if (node.getAfterNode() != null) {
                queue.add(node.getAfterNode());
            }
        }

        for (Map.Entry<CfgNode, Integer> entry : ids.entrySet()) {
            CfgNode node = entry.getKey();
            int from = entry.getValue();
            if (node instanceof CfgBoolExprNode) {
                CfgBoolExprNode boolNode = (CfgBoolExprNode) node;
                if (boolNode.getTrueNode() != null) {
                    graph.addEdge(from, ids.get(boolNode.getTrueNode()), CfgEdgeKind.TRUE, boolNode.getAst());
                }
                if (boolNode.getFalseNode() != null) {
                    graph.addEdge(from, ids.get(boolNode.getFalseNode()), CfgEdgeKind.FALSE, boolNode.getAst());
                }
            } else if (node.getAfterNode() != null) {
                graph.addEdge(from, ids.get(node.getAfterNode()), CfgEdgeKind.NORMAL, null);
            }
        }

        return graph;
    }
}
