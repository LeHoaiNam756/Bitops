package core.CFG.Utils;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.*;
import core.CFG.*;
import core.TestGeneration.path.MarkedStatement;

import java.util.*;

@Getter
@Setter
public class ASTHelper {
    public enum Coverage{
        STATEMENT,
        BRANCH,
        MCDC
    }

    private final static Set<MarkedStatement> statementToCfgNodeMap = new HashSet<>();
    public final static HashSet<String> primitiveTypes = new HashSet<>(Arrays.asList("boolean", "byte", "short", "char",
            "int", "long", "float", "double", "void"));

    private static Stack<CfgNode> endNodeStack = new Stack<>(); //for break statement
    private static Stack<CfgNode> conditionNodeStack = new Stack<>(); //for continue statement
    private static CfgNode endBlockNode = null; //for return statement
    private static Coverage coverage;
    private static CompilationUnit compilationUnit = null;

    public static CfgNode generateCfg(CfgNode block, CompilationUnit unit, Coverage coverageType) {
        endNodeStack.clear();
        conditionNodeStack.clear();
        endBlockNode = block.getAfterNode();
        compilationUnit = unit;
        coverage = coverageType;
        return generateCfgFromBlock(block);
    }

    private static CfgNode generateCfgFromBlock(CfgNode block) {
        CfgNode beforeStatementNode = block.getBeforeNode();
        CfgNode afterStatementNode = block.getAfterNode();
        afterStatementNode.setEndBlock(true);

        CfgNode rootNode = null;

        if (block.getAst() instanceof Block) {
            @SuppressWarnings("unchecked")
            List<? extends ASTNode> statements = ((Block) block.getAst()).statements();

            if (!statements.isEmpty()) {
                List<CfgNode> statementNodes = createAndLinkStatementNodes(statements, beforeStatementNode,
                        afterStatementNode);

                if (!statementNodes.isEmpty()) {
                    expandNestedStructures(statementNodes);

                    rootNode = findRootNode(statementNodes, beforeStatementNode);
                } else {
                    rootNode = block.getAfterNode();
                }
            } else {
                rootNode = block.getAfterNode();
            }

        } else {
            ASTNode statement = block.getAst();
            rootNode = createStatementNode(statement, beforeStatementNode, afterStatementNode);
            CfgNode expandedNode = expandStatementNode(rootNode, statement);
            if (expandedNode != null) {
                rootNode = expandedNode;
            }
        }
        return rootNode;
    }

    private static List<CfgNode> createAndLinkStatementNodes(List<? extends ASTNode> statements,
                                                           CfgNode beforeStatementNode,
                                                           CfgNode afterStatementNode) {
        List<CfgNode> statementNodes = new ArrayList<>();
        CfgNode currentBefore = beforeStatementNode;
        for (ASTNode statement : statements) {
            CfgNode currentNode = createStatementNode(statement, currentBefore, afterStatementNode);
            statementNodes.add(currentNode);
            currentBefore = currentNode;
        }

        return statementNodes;
    }

    private static void expandNestedStructures(List<CfgNode> statementNodes) {
        CfgNode afterStatementNode = null;
        if (!statementNodes.isEmpty()) {
            afterStatementNode = statementNodes.get(statementNodes.size() - 1).getAfterNode();
        }

        CfgNode nextStatementRoot = afterStatementNode;
        for (int i = statementNodes.size() - 1; i >= 0; i--) {
            CfgNode node = statementNodes.get(i);

            node.setAfterNode(nextStatementRoot);
            if (nextStatementRoot != null) {
                nextStatementRoot.setBeforeNode(node);
            }

            if (node.getAst() != null) {
                CfgNode expandedNode = expandStatementNode(node, node.getAst());
                if (expandedNode != null && expandedNode != node) {
                    statementNodes.set(i, expandedNode);
                    if (nextStatementRoot != null) {
                        nextStatementRoot.setBeforeNode(expandedNode);
                    }
                }
                nextStatementRoot = expandedNode != null ? expandedNode : node;
            } else {
                nextStatementRoot = node;
            }
        }
    }


    private static CfgNode createStatementNode(ASTNode statement, CfgNode beforeStatementNode,
                                               CfgNode afterStatementNode) {
        CfgNode currentNode = null;

        if (statement instanceof SwitchStatement) {
            currentNode = new CfgSwitchStatementNode();
            currentNode.setAst(statement);
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else if (statement instanceof BreakStatement) {
            currentNode = new CfgBreakStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            CfgNode endWithBreakNode = endNodeStack.isEmpty() ? afterStatementNode : endNodeStack.peek();
            LinkCurrentNodes(currentNode, beforeStatementNode, endWithBreakNode);
        } else if (statement instanceof ContinueStatement) {
            currentNode = new CfgContinueStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            CfgNode endWithContinueNode = conditionNodeStack.isEmpty() ? afterStatementNode : conditionNodeStack.peek();
            LinkCurrentNodes(currentNode, beforeStatementNode, endWithContinueNode);
        } else if (statement instanceof ReturnStatement) {
            currentNode = new CfgReturnStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, endBlockNode);
        } else if (statement instanceof Block) {
            currentNode = new CfgBlockNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else if (statement instanceof IfStatement) {
            currentNode = new CfgIfStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else if (statement instanceof WhileStatement) {
            currentNode = new CfgWhileStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else if (statement instanceof ForStatement) {
            currentNode = new CfgForStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else if (statement instanceof DoStatement) {
            currentNode = new CfgDoWhileStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        } else {
            currentNode = new CfgNormalStatementNode();
            currentNode.setAst(statement);
            currentNode.setContent(statement.toString());
            LinkCurrentNodes(currentNode, beforeStatementNode, afterStatementNode);
        }
        return currentNode;
    }

    private static CfgNode expandStatementNode(CfgNode currentNode, ASTNode statement) {
        if (statement instanceof Block) {
            CfgNode beginBlockNode = generateCfgFromBlock(currentNode);
            if (beginBlockNode != null) {
                CfgNode beforeNode = currentNode.getBeforeNode();
                CfgNode afterNode = currentNode.getAfterNode();
                if (beforeNode != null) {
                    beforeNode.setAfterNode(beginBlockNode);
                    beginBlockNode.setBeforeNode(beforeNode);
                }
                return beginBlockNode;
            }
        } else if (statement instanceof IfStatement && currentNode instanceof CfgIfStatementNode) {
            CfgBoolExprNode beginIfNode = generateCfgForIf((CfgIfStatementNode) currentNode, coverage);
            if (beginIfNode != null) {
                CfgNode beforeNode = currentNode.getBeforeNode();
                if (beforeNode != null) {
                    beforeNode.setAfterNode(beginIfNode);
                    beginIfNode.setBeforeNode(beforeNode);
                }
                return beginIfNode;
            }
        } else if (statement instanceof WhileStatement && currentNode instanceof CfgWhileStatementNode) {
            CfgNode beginWhileNode = generateCfgForWhile(currentNode, coverage);
            if (beginWhileNode != null) {
                CfgNode beforeNode = currentNode.getBeforeNode();
                if (beforeNode != null) {
                    beforeNode.setAfterNode(beginWhileNode);
                    beginWhileNode.setBeforeNode(beforeNode);
                }
                return beginWhileNode;
            }
        } else if (statement instanceof ForStatement && currentNode instanceof CfgForStatementNode) {
          CfgNode beginForNode = generateCfgForFor((CfgForStatementNode) currentNode, coverage);
          if (beginForNode != null) {
              CfgNode beforeNode = currentNode.getBeforeNode();
              if (beforeNode != null) {
                  beforeNode.setAfterNode(beginForNode);
                  beginForNode.setBeforeNode(beforeNode);
              }
              return beginForNode;
          }
        } else if (statement instanceof DoStatement && currentNode instanceof CfgDoWhileStatementNode) {
            CfgNode beginDoNode = generateCfgForDoWhile(currentNode, coverage);
            if (beginDoNode != null) {
                CfgNode beforeNode = currentNode.getBeforeNode();
                if (beforeNode != null) {
                    beforeNode.setAfterNode(beginDoNode);
                    beginDoNode.setBeforeNode(beforeNode);
                }
                return beginDoNode;
            }

        } else if (statement instanceof SwitchStatement && currentNode instanceof CfgSwitchStatementNode) {
            CfgBeginSwitchNode beginSwitchNode = generateCfgForSwitch(currentNode);
            if (beginSwitchNode != null) {
                CfgNode beforeNode = currentNode.getBeforeNode();
                if (beforeNode != null) {
                    beforeNode.setAfterNode(beginSwitchNode);
                    beginSwitchNode.setBeforeNode(beforeNode);
                }
                return beginSwitchNode;
            }
        }
        return currentNode;
    }

    private static CfgBoolExprNode generateCfgForIf(CfgIfStatementNode currentNode, Coverage coverage) {
        if (coverage == Coverage.MCDC) {
            return generateCfgForIfBlockWithMcdcCoverage(currentNode);
        } else {
            return generateCfgForIfBlockWithBranchCoverage(currentNode);
        }
    }

    private static CfgNode generateCfgForWhile(CfgNode currentNode, Coverage coverage) {
        if (coverage == Coverage.MCDC) {
            return generateCfgForWhileBlockWithMcdcCoverage(currentNode);
        } else {
            return generateCfgForWhileBlockWithBranchCoverage(currentNode, coverage);
        }
    }

    private static CfgNode generateCfgForFor(CfgForStatementNode currentNode, Coverage coverage) {
        if (coverage == Coverage.MCDC) {
            return generateCfgForForBlockWithMcdcCoverage(currentNode);
        } else {
            return generateCfgForForBlockWithBranchCoverage(currentNode, coverage);
        }
    }

    private static CfgNode generateCfgForDoWhile(CfgNode currentNode, Coverage coverage) {
        if (coverage == Coverage.MCDC) {
            return generateCfgForDoWhileBlockWithMcdcCoverage(currentNode);
        } else {
            return generateCfgForDoWhileBlockWithBranchCoverage(currentNode, coverage);
        }
    }

    private static CfgNode generateCfgForDoWhileBlockWithMcdcCoverage(CfgNode currentNode) {
        CfgNode beginDoNode = generateCfgForDoWhileBlockWithBranchCoverage(currentNode, Coverage.MCDC);
        CfgBoolExprNode doWhileConditionNode = (CfgBoolExprNode) conditionNodeStack.pop();
        CfgBoolExprNode mcdcDoWhileConditionNode = generateCfgForMcdcCondition(doWhileConditionNode);
        doWhileConditionNode.copyFrom(mcdcDoWhileConditionNode);
        return beginDoNode;
    }

    private static CfgNode generateCfgForDoWhileBlockWithBranchCoverage(CfgNode currentNode, Coverage coverage) {
        CfgNode beforeNode = currentNode.getBeforeNode();
        CfgNode afterNode = currentNode.getAfterNode();
        DoStatement doStatement = (DoStatement) currentNode.getAst();
        Statement bodyStatement = doStatement.getBody();
        CfgBlockNode bodyNode = new CfgBlockNode();
        bodyNode.setAst(bodyStatement);
        bodyNode.setContent(bodyStatement.toString());
        bodyNode.setBeforeNode(beforeNode);
        beforeNode.setAfterNode(bodyNode);

        Expression doWhileConditionAST = doStatement.getExpression();
        CfgBoolExprNode doWhileConditionNode = new CfgBoolExprNode();
        doWhileConditionNode.setAst(doWhileConditionAST);
        doWhileConditionNode.setContent(doWhileConditionAST.toString());
        bodyNode.setAfterNode(doWhileConditionNode);
        doWhileConditionNode.setBeforeNode(bodyNode);

        endNodeStack.push(afterNode);
        conditionNodeStack.push(doWhileConditionNode);

        CfgNode beginBodyNode = generateCfgFromBlock(bodyNode);

        endNodeStack.pop();
        conditionNodeStack.pop();

        doWhileConditionNode.setTrueNode(Objects.requireNonNullElse(beginBodyNode, doWhileConditionNode));
        doWhileConditionNode.setFalseNode(afterNode);
        doWhileConditionNode.setCfgEndBlockNode(afterNode);

        if (coverage == Coverage.MCDC) {
            conditionNodeStack.push(doWhileConditionNode);
        }

        return beginBodyNode;
    }

    private static CfgNode generateCfgForForBlockWithMcdcCoverage(CfgForStatementNode currentNode) {
        CfgNode beginForNode = generateCfgForForBlockWithBranchCoverage(currentNode, Coverage.MCDC);
        CfgBoolExprNode forConditionNode = (CfgBoolExprNode) conditionNodeStack.pop();
        CfgBoolExprNode mcdcForConditionNode = generateCfgForMcdcCondition(forConditionNode);
        forConditionNode.copyFrom(mcdcForConditionNode);
        return beginForNode;
    }

    private static CfgNode generateCfgForForBlockWithBranchCoverage(CfgForStatementNode currentNode, Coverage
                                                                            coverage) {
        CfgNode beforeNode = currentNode.getBeforeNode();
        CfgNode afterNode = currentNode.getAfterNode();

        ForStatement forStatement = (ForStatement) currentNode.getAst();
        List initializers = forStatement.initializers();
        Iterator iteratorOfInitilizers = initializers.iterator();
        CfgNode prevInitializerNode = beforeNode;
        CfgNode firstInitializerNode = null;
        while (iteratorOfInitilizers.hasNext()) {
            ASTNode initializer = (ASTNode) iteratorOfInitilizers.next();
            CfgNode initializerNode = createStatementNode(initializer, prevInitializerNode, afterNode);
            prevInitializerNode = initializerNode;
            if (firstInitializerNode == null) {
                firstInitializerNode = initializerNode;
            }
        }

        if (firstInitializerNode == null) {
            firstInitializerNode = new CfgNode();
            firstInitializerNode.setBeforeNode(beforeNode);
            firstInitializerNode.setContent("Empty Initializer");
            firstInitializerNode.setAfterNode(afterNode);
            prevInitializerNode = firstInitializerNode;
        }

        Expression forCondition = forStatement.getExpression();
        CfgBoolExprNode forConditionNode = new CfgBoolExprNode();
        forConditionNode.setAst(forCondition);
        forConditionNode.setContent(forCondition != null ? forCondition.toString() : "Empty Condition");
        prevInitializerNode.setAfterNode(forConditionNode);
        forConditionNode.setBeforeNode(prevInitializerNode);

        Statement bodyStatement = forStatement.getBody();
        CfgBlockNode bodyNode = new CfgBlockNode();
        bodyNode.setAst(bodyStatement);
        bodyNode.setContent(bodyStatement.toString());
        bodyNode.setBeforeNode(forConditionNode);


        List updaters = forStatement.updaters();
        Iterator iteratorOfUpdaters = updaters.iterator();
        CfgNode prevUpdaterNode = bodyNode;
        CfgNode firstUpdaterNode = null;
        while (iteratorOfUpdaters.hasNext()) {
            ASTNode updater = (ASTNode) iteratorOfUpdaters.next();
            CfgNode updaterNode = createStatementNode(updater, prevUpdaterNode, forConditionNode);
            prevUpdaterNode = updaterNode;
            if (firstUpdaterNode == null) {
                firstUpdaterNode = updaterNode;
            }
        }

        if (updaters.isEmpty()) {
            CfgNode updaterNode = new CfgNode();
            bodyNode.setAfterNode(updaterNode);
            updaterNode.setBeforeNode(bodyNode);
            updaterNode.setAfterNode(forConditionNode);
            updaterNode.setContent("Empty Updater");
            firstUpdaterNode = updaterNode;
        }

        endNodeStack.push(afterNode);
        conditionNodeStack.push(firstUpdaterNode);


        CfgNode bodyBeginNode = generateCfgFromBlock(bodyNode);

        endNodeStack.pop();
        conditionNodeStack.pop();

        forConditionNode.setTrueNode(Objects.requireNonNullElse(bodyBeginNode, afterNode));
        forConditionNode.setFalseNode(forCondition != null ? afterNode : bodyBeginNode);
        forConditionNode.setCfgEndBlockNode(afterNode);

        if (coverage == Coverage.MCDC) {
            conditionNodeStack.push(forConditionNode);
        }
        return firstInitializerNode;
    }



    private static CfgNode generateCfgForWhileBlockWithMcdcCoverage(CfgNode currentNode) {
        CfgNode beginWhileNode = generateCfgForWhileBlockWithBranchCoverage(currentNode, Coverage.MCDC);
        CfgBoolExprNode conditionNode = (CfgBoolExprNode) conditionNodeStack.pop();
        CfgBoolExprNode mcdcWhileConditionNode = generateCfgForMcdcCondition(conditionNode);
        conditionNode.copyFrom(mcdcWhileConditionNode);
        return beginWhileNode;
    }

    private static CfgNode generateCfgForWhileBlockWithBranchCoverage(CfgNode currentNode,
                                                                                Coverage coverage) {
        CfgNode beforeNode = currentNode.getBeforeNode();
        CfgNode afterNode = currentNode.getAfterNode();

        Expression whileConditionAST = ((WhileStatement) currentNode.getAst()).getExpression();
        CfgBoolExprNode whileConditionNode = new CfgBeginWhileNode();
        whileConditionNode.setAst(whileConditionAST);
        whileConditionNode.setContent(whileConditionAST.toString());
        whileConditionNode.setBeforeNode(beforeNode);
        beforeNode.setAfterNode(whileConditionNode);

        Statement bodyStatement = ((WhileStatement) currentNode.getAst()).getBody();
        CfgNode bodyBlockNode = new CfgBlockNode();
        bodyBlockNode.setAst(bodyStatement);
        bodyBlockNode.setContent(bodyStatement.toString());
        bodyBlockNode.setBeforeNode(whileConditionNode);
        bodyBlockNode.setAfterNode(whileConditionNode);

        endNodeStack.push(afterNode);
        conditionNodeStack.push(whileConditionNode);

        CfgNode bodyBeginNode = generateCfgFromBlock(bodyBlockNode);

        endNodeStack.pop();
        conditionNodeStack.pop();

        whileConditionNode.setTrueNode(Objects.requireNonNullElse(bodyBeginNode, currentNode));
        whileConditionNode.setFalseNode(afterNode);
        whileConditionNode.setCfgEndBlockNode(afterNode);

        if (coverage == Coverage.MCDC) {
            conditionNodeStack.push(whileConditionNode);
        }

        return whileConditionNode;
    }

    private static void LinkCurrentNodes(CfgNode currentNode, CfgNode beforeNode, CfgNode afterNode) {
        if (beforeNode == null) {
            beforeNode = new CfgNode();
        }

        if (afterNode == null) {
            afterNode = new CfgNode();
        }
        beforeNode.setAfterNode(currentNode);
        currentNode.setBeforeNode(beforeNode);
        currentNode.setAfterNode(afterNode);
        afterNode.setBeforeNode(currentNode);
    }

    private static CfgBeginSwitchNode generateCfgForSwitch(CfgNode switchNode) {
        return null;
    }

    private static CfgBoolExprNode generateCfgForIfBlockWithMcdcCoverage(CfgIfStatementNode ifNode) {
        CfgBoolExprNode beginIfNode = generateCfgForIfBlockWithBranchCoverage(ifNode);
        beginIfNode = generateCfgForMcdcCondition(beginIfNode);
        return beginIfNode;
    }

    private static CfgBoolExprNode generateCfgForMcdcCondition(CfgBoolExprNode conditionsNode) {
        Expression expression = removeParentheses((Expression) conditionsNode.getAst());
        CfgNode trueNode = conditionsNode.getTrueNode();
        CfgNode falseNode = conditionsNode.getFalseNode();


        if (expression instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) expression;
            InfixExpression.Operator operator = infixExpr.getOperator();

            boolean isAnd = isAndOperator(operator);
            boolean isOr = isOrOperator(operator);

            if (isAnd || isOr) {
                List<CfgBoolExprNode> conditionNodes = extractAndProcessOperands(infixExpr);
                linkNodes(conditionNodes, trueNode, falseNode, isAnd);

                List<CfgBoolExprNode> processedNodes = processNodesRecursively(conditionNodes);

                conditionsNode = processedNodes.get(0);

                updateNodeReferences(conditionNodes, processedNodes, isAnd);
            }
        } else if (expression instanceof PrefixExpression) {
            PrefixExpression prefixExpr = (PrefixExpression) expression;
            if (prefixExpr.getOperator().equals(PrefixExpression.Operator.NOT)) {
                CfgBoolExprNode innerConditionNode = new CfgBoolExprNode();
                innerConditionNode.setAst(prefixExpr.getOperand());
                innerConditionNode.setContent(prefixExpr.getOperand().toString());
                innerConditionNode.setBeforeNode(conditionsNode.getBeforeNode());
                innerConditionNode.setTrueNode(falseNode);
                innerConditionNode.setFalseNode(trueNode);

                CfgBoolExprNode processedInnerNode = generateCfgForMcdcCondition(innerConditionNode);
                conditionsNode = processedInnerNode;
            }
        }

        return conditionsNode;
    }

    private static List<CfgBoolExprNode> processNodesRecursively(List<CfgBoolExprNode> conditionNodes) {
        List<CfgBoolExprNode> processedNodes = new LinkedList<>();
        for (CfgBoolExprNode node : conditionNodes) {
            CfgBoolExprNode processedNode = generateCfgForMcdcCondition(node);
            processedNodes.add(processedNode);
        }
        return processedNodes;
    }

    private static void updateNodeReferences(List<CfgBoolExprNode> originalNodes,
                                             List<CfgBoolExprNode> processedNodes,
                                             boolean isAnd) {
        for (int i = 1; i < originalNodes.size(); i++) {
            CfgBoolExprNode originalNode = originalNodes.get(i);
            CfgBoolExprNode processedNode = processedNodes.get(i);

            if (originalNode != processedNode) {
                CfgBoolExprNode prevProcessedNode = processedNodes.get(i - 1);
                if (isAnd) {
                    if (prevProcessedNode.getTrueNode() == originalNode) {
                        prevProcessedNode.setTrueNode(processedNode);
                    }
                } else {
                    if (prevProcessedNode.getFalseNode() == originalNode) {
                        prevProcessedNode.setFalseNode(processedNode);
                    }
                }
            }
        }
    }

    private static Expression removeParentheses(Expression expr) {
        while (expr instanceof ParenthesizedExpression) {
            expr = ((ParenthesizedExpression) expr).getExpression();
        }
        return expr;
    }

    private static List<CfgBoolExprNode> extractAndProcessOperands(InfixExpression infixExpr) {
        List<Expression> rawExpressions = new LinkedList<>();

        rawExpressions.add(infixExpr.getLeftOperand());
        rawExpressions.add(infixExpr.getRightOperand());

        List extended = infixExpr.extendedOperands();
        if (extended != null) {
            for (Object obj : extended) {
                rawExpressions.add((Expression) obj);
            }
        }

        List<CfgBoolExprNode> processedNodes = new LinkedList<>();
        for (Expression expr : rawExpressions) {
            CfgBoolExprNode node = new CfgBoolExprNode();
            node.setAst(expr);
            node.setContent(expr.toString());
            processedNodes.add(node);
        }

        return processedNodes;
    }


    private static void linkNodes(List<CfgBoolExprNode> nodes, CfgNode finalTrue, CfgNode finalFalse,
                                  boolean isAndOperator) {
        Iterator<CfgBoolExprNode> iterator = nodes.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            CfgBoolExprNode current = iterator.next();
            boolean isLast = (i == nodes.size() - 1);
            CfgBoolExprNode nextNode = isLast ? null : nodes.get(i + 1);

            if (isAndOperator) {
                current.setFalseNode(finalFalse);
                current.setTrueNode(isLast ? finalTrue : nextNode);
            } else {
                current.setTrueNode(finalTrue);
                current.setFalseNode(isLast ? finalFalse : nextNode);
            }
            i++;
        }
    }

    private static CfgBoolExprNode generateCfgForIfBlockWithBranchCoverage(CfgIfStatementNode ifNode) {
        CfgNode beforeNode = ifNode.getBeforeNode();
        CfgNode afterNode = ifNode.getAfterNode();

        IfStatement ifStatement = (IfStatement) ifNode.getAst();
        Expression ifConditionAST = ifStatement.getExpression();
        CfgBoolExprNode ifConditionNode = new CfgBoolExprNode();
        ifConditionNode.setAst(ifConditionAST);
        ifConditionNode.setContent(ifConditionAST.toString());
        ifConditionNode.setBeforeNode(beforeNode);
        beforeNode.setAfterNode(ifConditionNode);

        Statement thenStatement = ifStatement.getThenStatement();
        CfgNode thenBlockNode = new CfgBlockNode();
        thenBlockNode.setAst(thenStatement);
        thenBlockNode.setContent(thenStatement.toString());
        thenBlockNode.setBeforeNode(ifConditionNode);
        thenBlockNode.setAfterNode(afterNode);

        CfgNode thenBeginNode = generateCfgFromBlock(thenBlockNode);

        ifConditionNode.setTrueNode(Objects.requireNonNullElse(thenBeginNode, afterNode));

        Statement elseStatement = ifStatement.getElseStatement();
        if (elseStatement == null) {
            ifConditionNode.setFalseNode(afterNode);
        } else {
            CfgNode elseBlockNode = new CfgBlockNode();
            elseBlockNode.setAst(elseStatement);
            elseBlockNode.setContent(elseStatement.toString());
            elseBlockNode.setBeforeNode(ifConditionNode);
            elseBlockNode.setAfterNode(afterNode);

            CfgNode elseBeginNode = generateCfgFromBlock(elseBlockNode);
            ifConditionNode.setFalseNode(Objects.requireNonNullElse(elseBeginNode, afterNode));
        }

        ifConditionNode.setCfgEndBlockNode(afterNode);
        return ifConditionNode;
    }


    private static CfgNode findRootNode(List<CfgNode> statementNodes, CfgNode beforeStatementNode) {
        if (statementNodes.isEmpty()) {
            return null;
        }

        CfgNode rootNode = statementNodes.get(0);
        while (rootNode.getBeforeNode() != null &&
               rootNode.getBeforeNode() != beforeStatementNode) {
            CfgNode prev = rootNode.getBeforeNode();
            if (prev == null || prev == beforeStatementNode) {
                break;
            }
            rootNode = prev;
        }

        return rootNode;
    }

    private static boolean isOrOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.CONDITIONAL_OR);
    }

    private static boolean isAndOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.CONDITIONAL_AND);
    }

}
