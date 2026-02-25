package test;

import core.CFG.CfgBoolExprNode;
import core.CFG.CfgEndBlockNode;
import core.CFG.CfgNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates Mermaid graph syntax from a CFG node
 * 
 * Usage:
 * String mermaidGraph = test.CfgMermaidGenerator.generateMermaidGraph(rootCfgNode);
 * System.out.println(mermaidGraph);
 */
public class CfgMermaidGenerator {
    
    private static int nodeCounter = 0;
    private static Map<CfgNode, String> nodeIds = new HashMap<>();
    private static Set<CfgNode> visitedNodes = new HashSet<>();
    private static StringBuilder mermaidBuilder = new StringBuilder();
    
    /**
     * Generate Mermaid flowchart syntax from a root CFG node
     * 
     * @param rootNode The root CFG node to start traversal from
     * @return Mermaid flowchart syntax as a string
     */
    public static String generateMermaidGraph(CfgNode rootNode) {
        if (rootNode == null) {
            return "flowchart TD\n    Empty[Empty CFG]";
        }
        
        // Reset state
        nodeCounter = 0;
        nodeIds.clear();
        visitedNodes.clear();
        mermaidBuilder = new StringBuilder();
        
        // Start Mermaid flowchart
        mermaidBuilder.append("flowchart TD\n");
        
        // Traverse and generate graph
        traverseAndGenerate(rootNode);
        
        return mermaidBuilder.toString();
    }
    
    /**
     * Traverse the CFG and generate Mermaid syntax
     */
    private static void traverseAndGenerate(CfgNode node) {
        if (node == null || visitedNodes.contains(node)) {
            return;
        }
        
        visitedNodes.add(node);
        String nodeId = getOrCreateNodeId(node);
        String nodeLabel = getNodeLabel(node);
        
        // Generate node definition
        mermaidBuilder.append("    ").append(nodeId)
                     .append("[").append(escapeMermaidText(nodeLabel)).append("]");
        
        // Add node type styling
        // Check if node is visited first - visited nodes get priority styling
        if (node.isVisited()) {
            mermaidBuilder.append(":::visitedNode");
        } else if (node instanceof CfgBoolExprNode) {
            mermaidBuilder.append(":::boolNode");
        } else if (node instanceof CfgEndBlockNode || node.isEndBlock()) {
            mermaidBuilder.append(":::endNode");
        } else {
            mermaidBuilder.append(":::normalNode");
        }
        
        mermaidBuilder.append("\n");
        
        // Handle different node types
        if (node instanceof CfgBoolExprNode) {
            handleBoolExprNode((CfgBoolExprNode) node, nodeId);
        } else {
            handleRegularNode(node, nodeId);
        }
    }
    
    /**
     * Handle CfgBoolExprNode - use getTrueNode() and getFalseNode()
     */
    private static void handleBoolExprNode(CfgBoolExprNode boolNode, String nodeId) {
        // Handle true node
        if (boolNode.getTrueNode() != null) {
            String trueNodeId = getOrCreateNodeId(boolNode.getTrueNode());
            mermaidBuilder.append("    ").append(nodeId)
                         .append(" -->|T| ").append(trueNodeId).append("\n");
            traverseAndGenerate(boolNode.getTrueNode());
        }
        
        // Handle false node
        if (boolNode.getFalseNode() != null) {
            String falseNodeId = getOrCreateNodeId(boolNode.getFalseNode());
            mermaidBuilder.append("    ").append(nodeId)
                         .append(" -->|F| ").append(falseNodeId).append("\n");
            traverseAndGenerate(boolNode.getFalseNode());
        }
    }
    
    /**
     * Handle regular CfgNode - use getAfterNode() to get next node in forward traversal
     * Note: getBeforeNode() points to the previous node, so we don't traverse it forward
     */
    private static void handleRegularNode(CfgNode node, String nodeId) {
        // Use afterNode to get next node in the forward flow
        if (node.getAfterNode() != null) {
            String afterNodeId = getOrCreateNodeId(node.getAfterNode());
            mermaidBuilder.append("    ").append(nodeId)
                         .append(" --> ").append(afterNodeId).append("\n");
            traverseAndGenerate(node.getAfterNode());
        }
    }
    
    /**
     * Get or create a unique node ID for Mermaid
     */
    private static String getOrCreateNodeId(CfgNode node) {
        if (nodeIds.containsKey(node)) {
            return nodeIds.get(node);
        }
        
        String nodeId = "N" + nodeCounter++;
        nodeIds.put(node, nodeId);
        return nodeId;
    }
    
    /**
     * Get a label for the node
     */
    private static String getNodeLabel(CfgNode node) {
        String content = node.getContent();
        
        if (content == null || content.trim().isEmpty()) {
            String className = node.getClass().getSimpleName();
            if (node.isEndBlock()) {
                return "END";
            }
            return className;
        }
        
        // Truncate long content
        if (content.length() > 50) {
            content = content.substring(0, 47) + "...";
        }
        
        // Replace newlines and special characters
        content = content.replace("\n", " ").replace("\r", " ");
        content = content.replace("-->", "→").replace("||", "OR");
        
        return content.trim();
    }
    
    /**
     * Escape special characters for Mermaid syntax
     */
    private static String escapeMermaidText(String text) {
        if (text == null) {
            return "";
        }
        
        // Escape special Mermaid characters
        return text.replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("|", "&#124;")
                   .replace("[", "&#91;")
                   .replace("]", "&#93;")
                   .replace("{", "&#123;")
                   .replace("}", "&#125;")
                   .replace("(", "&#40;")
                   .replace(")", "&#41;");
    }
    
    /**
     * Generate Mermaid graph with styling classes
     * 
     * @param rootNode The root CFG node
     * @return Complete Mermaid graph with styling
     */
    public static String generateMermaidGraphWithStyles(CfgNode rootNode) {
        String graph = generateMermaidGraph(rootNode);
        
        // Add styling
        StringBuilder styledGraph = new StringBuilder(graph);
        styledGraph.append("\n");
        styledGraph.append("    classDef visitedNode fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px\n");
        styledGraph.append("    classDef boolNode fill:#e1f5ff,stroke:#01579b,stroke-width:2px\n");
        styledGraph.append("    classDef normalNode fill:#fff9c4,stroke:#f57f17,stroke-width:2px\n");
        styledGraph.append("    classDef endNode fill:#f3e5f5,stroke:#4a148c,stroke-width:2px\n");
        
        return styledGraph.toString();
    }
}

