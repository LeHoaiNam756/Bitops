package core.TestGeneration.path;



import lombok.Getter;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;

import java.util.*;

public final class MarkedPath {

    @Getter
    private static final Set<MarkedStatement> markedStatements = new HashSet<>();
    private static final Set<MarkedStatement> fullTestSuiteCoveredStatements = new HashSet<>();
    private static final Set<MarkedStatement> fullTestSuiteCoveredBranchesAndMCDC = new HashSet<>();
    private static final Set<MarkedStatement> totalCoveredStatement = new HashSet<>();
    private static final Set<MarkedStatement> totalCoveredBranchAndMCDC = new HashSet<>();
    @Getter
    private static final Set<CfgNode> visitedNodes = new HashSet<>();

    private MarkedPath() {
    }



    public static boolean markOneStatement(String statement, boolean isTrueCondition, boolean isFalseCondition,
                                           int startPosition) {
        addNewStatementToPath(statement, isTrueCondition, isFalseCondition, startPosition);
        if (!isTrueCondition && !isFalseCondition) return true;
        return !isFalseCondition;
    }


    private static void addNewStatementToPath(String statement, boolean isTrueCondition,
                                              boolean isFalseCondition, int lineNumber) {
        MarkedStatement markedStatement = new MarkedStatement(statement, isTrueCondition, isFalseCondition, lineNumber);
        markedStatements.add(markedStatement);
    }

    public static void resetMarkStatements() {
        markedStatements.clear();
    }

    public static void reset() {
        markedStatements.clear();
        totalCoveredBranchAndMCDC.clear();
        totalCoveredStatement.clear();
        fullTestSuiteCoveredStatements.clear();
        fullTestSuiteCoveredBranchesAndMCDC.clear();
        visitedNodes.clear();
    }


    public static int getTotalCoveredStatement() {
        return totalCoveredStatement.size();
    }

    public static int getTotalCoveredBranchAndMCDC() {
        return totalCoveredBranchAndMCDC.size();
    }


    public static void resetFullTestSuiteCoveredStatements() {
        fullTestSuiteCoveredStatements.clear();
        fullTestSuiteCoveredBranchesAndMCDC.clear();
    }

    public static void resetVisitedNodes() {
        visitedNodes.clear();
    }

    public static int getFullTestSuiteTotalCoveredStatements() {
        return fullTestSuiteCoveredStatements.size();
    }

    public static int getFullTestSuiteTotalCoveredBranch() {
        return fullTestSuiteCoveredBranchesAndMCDC.size();
    }

    public static void markPathToCfg(CfgNode rootCfgNode) {
        totalCoveredBranchAndMCDC.clear();
        totalCoveredStatement.clear();
        if (markedStatements.isEmpty() || rootCfgNode == null) {
            return;
        }

        Map<String, List<CfgNode>> statementToNodes = new HashMap<>();
        Queue<CfgNode> queue = new LinkedList<>();
        Set<CfgNode> visited = new HashSet<>();
        queue.add(rootCfgNode);

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();
            if (node == null || visited.contains(node)) continue;
            visited.add(node);

            String content = node.getContent();
            if (content != null && !content.trim().isEmpty()) {
                String key = content.trim();
                statementToNodes.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
            }

            if (node instanceof CfgBoolExprNode) {
                CfgBoolExprNode b = (CfgBoolExprNode) node;
                queue.add(b.getTrueNode());
                queue.add(b.getFalseNode());
            } else {
                queue.add(node.getAfterNode());
            }
        }

        for (MarkedStatement marked : markedStatements) {
            System.out.println("Đang xử lý statement đã đánh dấu: [" + marked.getContent() + "] tại vị trí " + marked.getStartPosition());
            if (marked == null) continue;
            String stmt = marked.getContent();
            int startPosition = marked.getStartPosition();
            if (stmt == null || stmt.trim().isEmpty()) continue;
            String key = stmt.trim();

            List<CfgNode> candidates = statementToNodes.get(key);
            CfgNode matched = null;
            //TODO: Improve, need this code because a boolean statement can be stored twice
            if (candidates != null && !candidates.isEmpty()) {
                for (CfgNode n : candidates) {
                    System.out.println("Kiểm tra candidate CFG node: [" + n.getContent() + "] tại vị trí " + n.getStartPosition());
                    if (n.getStartPosition() == startPosition) {
                        matched = n;
                        break;
                    }

                }
            }

            if (matched == null) {
                System.err.println("Không tìm thấy CFG node cho statement: [" + stmt + "]");
                continue;
            }

            boolean wasMarkedBefore = matched.isVisited();
            for (MarkedStatement s : totalCoveredStatement) {
                if (s.getContent().trim().equals(matched.getContent().trim())
                        && s.getStartPosition() == matched.getStartPosition()) {
                    wasMarkedBefore = true;
                    break;
                }
            }
            if (!wasMarkedBefore) {
                totalCoveredStatement.add(marked);
            }

            if (!wasMarkedBefore) {
                fullTestSuiteCoveredStatements.add(marked);
                matched.setVisited(true);
                visitedNodes.add(matched);
            }

            marked.setCfgNode(matched);

            if (matched instanceof CfgBoolExprNode) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                CfgBoolExprNode boolNode = (CfgBoolExprNode) matched;
                if (marked.isTrueConditionalStatement()) {
                    MarkedStatement trueBranch = new MarkedStatement(
                        marked.getContent(), true, false,
                        marked.getStartPosition());
                    totalCoveredBranchAndMCDC.add(trueBranch);
                    fullTestSuiteCoveredBranchesAndMCDC.add(trueBranch);

                    boolNode.setTrueMarked(true);
                    visited.add(matched);
                }

                if (marked.isFalseConditionalStatement()) {
                    MarkedStatement falseBranch = new MarkedStatement(
                        marked.getContent(), false, true,
                        marked.getStartPosition());
                    totalCoveredBranchAndMCDC.add(falseBranch);
                    fullTestSuiteCoveredBranchesAndMCDC.add(falseBranch);

                    boolNode.setFalseMarked(true);
                    visited.add(matched);
                }
            }
        }
    }
}