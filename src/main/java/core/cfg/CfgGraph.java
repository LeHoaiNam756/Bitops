package core.cfg;

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

        public Node(int id, CfgNodeKind kind, ASTNode ast, String content) {
            this.id = id;
            this.kind = kind;
            this.ast = ast;
            this.content = content;
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
    }

    public static final class Edge {
        private final int from;
        private final int to;
        private final CfgEdgeKind kind;

        public Edge(int from, int to, CfgEdgeKind kind) {
            this.from = from;
            this.to = to;
            this.kind = kind;
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
    }

    private final Map<Integer, Node> nodes = new HashMap<>();
    private final Map<Integer, List<Edge>> outgoing = new HashMap<>();
    private final Map<Integer, List<Edge>> incoming = new HashMap<>();
    private int nextId = 1;

    public int addNode(CfgNodeKind kind, ASTNode ast, String content) {
        int id = nextId++;
        nodes.put(id, new Node(id, kind, ast, content));
        outgoing.putIfAbsent(id, new ArrayList<>());
        incoming.putIfAbsent(id, new ArrayList<>());
        return id;
    }

    public void addEdge(int from, int to, CfgEdgeKind kind) {
        Edge edge = new Edge(from, to, kind);
        outgoing.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);
        incoming.computeIfAbsent(to, k -> new ArrayList<>()).add(edge);
    }

    public Node getNode(int id) {
        return nodes.get(id);
    }

    public List<Edge> outgoing(int id) {
        return outgoing.getOrDefault(id, Collections.emptyList());
    }

    public List<Edge> incoming(int id) {
        return incoming.getOrDefault(id, Collections.emptyList());
    }

    public int nodeCount() {
        return nodes.size();
    }
}