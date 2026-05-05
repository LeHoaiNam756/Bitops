package core.CFG.graph;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgGraph {
    public static final class Node {
        private final int id;
        private final CfgNodeKind kind;
        private final ASTNode ast;
        private final String content;
        private final int startPosition;

        public Node(int id, CfgNodeKind kind, ASTNode ast, String content, int startPosition) {
            this.id = id;
            this.kind = kind;
            this.ast = ast;
            this.content = content;
            this.startPosition = startPosition;
        }

        public int getId() {
            return id;
        }

        public CfgNodeKind getKind() {
            return kind;
        }

        public ASTNode getAst() {
            return ast;
        }

        public String getContent() {
            return content;
        }

        public int getStartPosition() {
            return startPosition;
        }
    }

    public static final class Edge {
        private final int from;
        private final int to;
        private final CfgEdgeKind kind;
        private final ASTNode conditionAst;

        public Edge(int from, int to, CfgEdgeKind kind, ASTNode conditionAst) {
            this.from = from;
            this.to = to;
            this.kind = kind;
            this.conditionAst = conditionAst;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public CfgEdgeKind getKind() {
            return kind;
        }

        public ASTNode getConditionAst() {
            return conditionAst;
        }
    }

    private final Map<Integer, Node> nodes = new HashMap<>();
    private final Map<Integer, List<Edge>> outgoing = new HashMap<>();
    private int nextId = 1;

    public int addNode(CfgNodeKind kind, ASTNode ast, String content, int startPosition) {
        int id = nextId++;
        nodes.put(id, new Node(id, kind, ast, content, startPosition));
        outgoing.putIfAbsent(id, new ArrayList<>());
        return id;
    }

    public void addEdge(int from, int to, CfgEdgeKind kind, ASTNode conditionAst) {
        outgoing.computeIfAbsent(from, key -> new ArrayList<>())
                .add(new Edge(from, to, kind, conditionAst));
    }

    public Node getNode(int id) {
        return nodes.get(id);
    }

    public List<Edge> outgoing(int id) {
        return outgoing.getOrDefault(id, Collections.emptyList());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        int total = 0;
        for (List<Edge> edges : outgoing.values()) {
            total += edges.size();
        }
        return total;
    }
}
