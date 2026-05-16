package core.cfg;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class CfgBuilder {
    // Pending exits from a sub-graph fragment
    record Fragment(int entry, List<PendingExit> exits) {}
    record PendingExit(int fromNode, CfgEdgeKind kind) {}


    // Loop context for break/continue resolution
    record LoopContext(int condNode, List<PendingExit> breakExits) {}

    //Result of building a boolean condition chain
    record ConditionChain(int entry, List<PendingExit> trueExits, List<PendingExit> falseExits) {}

    private final ControlFlowGraph graph = new ControlFlowGraph();
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private int returnSink = -1;


    private final boolean splitBooleanExpression;

    public CfgBuilder() {
        this(false);
    }

    public CfgBuilder(boolean splitBooleanExpression) {
        this.splitBooleanExpression = splitBooleanExpression;
    }

    public ControlFlowGraph build(MethodDeclaration method) {
        int entry = graph.addNode(CfgNodeKind.ENTRY, null, "entry");
        returnSink = graph.addNode(CfgNodeKind.EXIT,  null, "exit");

        Fragment body = buildStatement(method.getBody());

        graph.addEdge(entry, body.entry(), CfgEdgeKind.NORMAL);

        // Dangling exits fall through to return sink
        wire(body.exits(), returnSink);
        return graph;
    }

     private Fragment buildStatement(Statement stmt) {
       if (stmt instanceof Block b)             return buildBlock(b);
       if (stmt instanceof IfStatement i)       return buildIf(i);
       if (stmt instanceof WhileStatement w)    return buildWhile(w);
       if (stmt instanceof ForStatement f)      return buildFor(f);
       if (stmt instanceof DoStatement dw)       return buildDoWhile(dw);
       if (stmt instanceof ReturnStatement r)   return buildReturn(r);
       if (stmt instanceof BreakStatement b)    return buildBreak(b);
       if (stmt instanceof ContinueStatement c) return buildContinue(c);
       return buildPlain(stmt);
     }

    private Fragment buildBlock(Block block) {
        @SuppressWarnings("unchecked")
        List<Statement> stmts = block.statements();
        if (stmts.isEmpty()) {
            int node = graph.addNode(CfgNodeKind.STMT, block, "{}");
            return new Fragment(node, List.of(new PendingExit(node, CfgEdgeKind.NORMAL)));
        }
        Fragment current = buildStatement(stmts.get(0));
        for (int i = 1; i < stmts.size(); i++) {
            Fragment next = buildStatement(stmts.get(i));
            wire(current.exits(), next.entry());
            current = new Fragment(current.entry(), next.exits());
        }
        return current;
    }

    private Fragment buildIf(IfStatement stmt) {
        ConditionChain cond = buildCondition(stmt.getExpression());

        Fragment thenFrag = buildStatement(stmt.getThenStatement());
        wire(cond.trueExits(), thenFrag.entry());

        List<PendingExit> exits = new ArrayList<>(thenFrag.exits());

        if (stmt.getElseStatement() != null) {
            Fragment elseFrag = buildStatement(stmt.getElseStatement());
            wire(cond.falseExits(), elseFrag.entry());
            exits.addAll(elseFrag.exits());
        } else {
            exits.addAll(asFalseExits(cond.falseExits()));
        }
        return new Fragment(cond.entry, exits);
    }

    private Fragment buildWhile(WhileStatement stmt) {
        ConditionChain cond = buildLoopCondition(stmt.getExpression());


        LoopContext ctx = new LoopContext(cond.entry(), new ArrayList<>());
        loopStack.push(ctx);
        Fragment body = buildStatement(stmt.getBody());
        loopStack.pop();

        wire(cond.trueExits(), body.entry());
        wire(body.exits(), cond.entry());   // back edges

        List<PendingExit> exits = new ArrayList<>(asFalseExits(cond.falseExits()));
        exits.addAll(ctx.breakExits());    // break jumps out
        return new Fragment(cond.entry(), exits);
    }

    private Fragment buildFor(ForStatement stmt) {
        // 1. INIT
        Fragment initFrag = null;
        if (!stmt.initializers().isEmpty()) {
            List<PendingExit> currentExits = null;
            int firstEntry = -1;
            for (Object initObj : stmt.initializers()) {
                Expression init = (Expression) initObj;
                int initNode = graph.addNode(CfgNodeKind.STMT, init, init.toString());
                if (firstEntry == -1) firstEntry = initNode;
                if (currentExits != null) wire(currentExits, initNode);
                currentExits = List.of(new PendingExit(initNode, CfgEdgeKind.NORMAL));
            }
            initFrag = new Fragment(firstEntry, currentExits);
        }

        // 2. CONDITION (may be absent → infinite loop)
        ConditionChain cond;
        if (stmt.getExpression() != null) {
            cond = buildLoopCondition(stmt.getExpression());
        } else {
            // No condition — synthetic "true" node, always takes true branch
            int synthNode = graph.addNode(CfgNodeKind.LOOP, null, "true");
            cond = new ConditionChain(
                    synthNode,
                    List.of(new PendingExit(synthNode, CfgEdgeKind.TRUE)),
                    List.of()   // never false
            );
        }

        // Wire init → cond entry
        if (initFrag != null) wire(initFrag.exits(), cond.entry());

        // 3. BODY
        LoopContext ctx = new LoopContext(cond.entry(), new ArrayList<>());
        loopStack.push(ctx);
        Fragment bodyFrag = buildStatement(stmt.getBody());
        loopStack.pop();

        wire(cond.trueExits(), bodyFrag.entry());

        // 4. UPDATE
        if (!stmt.updaters().isEmpty()) {
            int firstUpdateEntry = -1;
            List<PendingExit> currentExits = null;
            for (Object updateObj : stmt.updaters()) {
                Expression update = (Expression) updateObj;
                int updateNode = graph.addNode(CfgNodeKind.STMT, update, update.toString());
                if (firstUpdateEntry == -1) firstUpdateEntry = updateNode;
                if (currentExits != null) wire(currentExits, updateNode);
                currentExits = List.of(new PendingExit(updateNode, CfgEdgeKind.NORMAL));
            }
            wire(bodyFrag.exits(), firstUpdateEntry);
            wire(currentExits, cond.entry());
        } else {
            wire(bodyFrag.exits(), cond.entry());
        }

        // 5. Exits: false side of condition + break statements
        List<PendingExit> exits = new ArrayList<>(asFalseExits(cond.falseExits()));
        exits.addAll(ctx.breakExits());

        int fragmentEntry = (initFrag != null) ? initFrag.entry() : cond.entry();
        return new Fragment(fragmentEntry, exits);
    }


    private Fragment buildDoWhile(DoStatement stmt) {
        // 1. BODY — build first, since do-while executes body before checking condition
        //    Push loop context so break/continue inside resolve correctly.
        //    Continue in a do-while jumps to the CONDITION, not the top of the body,
        //    so we need the condNode — but we don't have it yet.
        //    Solution: create condNode first, then build body.

        // 2. CONDITION node (created before body so continue can reference it)
        ConditionChain cond = buildLoopCondition(stmt.getExpression());

        LoopContext ctx = new LoopContext(cond.entry(), new ArrayList<>());
        loopStack.push(ctx);
        Fragment bodyFrag = buildStatement(stmt.getBody());
        loopStack.pop();

        // 3. Wire: body exits → condition (normal fall-through)
        wire(bodyFrag.exits(), cond.entry());

        // 4. TRUE branch: loop back to body entry
        wire(cond.trueExits(), bodyFrag.entry());
        // 5. Collect exits: FALSE off condition + any break statements
        List<PendingExit> exits = new ArrayList<>(asFalseExits(cond.falseExits()));
        exits.addAll(ctx.breakExits());

        // Entry is the body (not the condition), since body runs first
        return new Fragment(bodyFrag.entry(), exits);
    }
    private Fragment buildReturn(ReturnStatement stmt) {
        int node = graph.addNode(CfgNodeKind.STMT, stmt.getExpression(), stmt.toString());
        graph.addEdge(node, returnSink, CfgEdgeKind.NORMAL);
        return new Fragment(node, List.of());   // no dangling exits
    }

    private Fragment buildBreak(BreakStatement stmt) {
        int node = graph.addNode(CfgNodeKind.STMT, stmt, "break");
        Objects.requireNonNull(loopStack.peek()).breakExits().add(new PendingExit(node, CfgEdgeKind.NORMAL));
        return new Fragment(node, List.of());
    }

    private Fragment buildContinue(ContinueStatement stmt) {
        int node = graph.addNode(CfgNodeKind.STMT, stmt, "continue");
        graph.addEdge(node, Objects.requireNonNull(loopStack.peek()).condNode(), CfgEdgeKind.NORMAL);
        return new Fragment(node, List.of());
    }

    private Fragment buildPlain(Statement stmt) {
        int node = graph.addNode(CfgNodeKind.STMT, stmt, stmt.toString());
        return new Fragment(node, List.of(new PendingExit(node, CfgEdgeKind.NORMAL)));
    }

    // Wire all pending exits to a known target node
    private void wire(List<PendingExit> exits, int target) {
        for (PendingExit e : exits) {
            graph.addEdge(e.fromNode(), target, e.kind());
        }
    }

    private ConditionChain buildCondition(Expression expr) {
        if (splitBooleanExpression && expr instanceof InfixExpression infix) {
            InfixExpression.Operator op = infix.getOperator();

            if (op == InfixExpression.Operator.CONDITIONAL_AND) {
                return buildAndChain(infix);
            }
            if (op == InfixExpression.Operator.CONDITIONAL_OR) {
                return buildOrChain(infix);
            }
        }

        // Default: single BRANCH node for the whole expression
        int node = graph.addNode(CfgNodeKind.BRANCH, expr, expr.toString());
        return new ConditionChain(
                node,
                List.of(new PendingExit(node, CfgEdgeKind.TRUE)),
                List.of(new PendingExit(node, CfgEdgeKind.FALSE))
        );
    }

    private ConditionChain buildLoopCondition(Expression expr) {
        ConditionChain cond = buildCondition(expr);
        ControlFlowGraph.Node branchNode = graph.getNode(cond.entry());
        ControlFlowGraph.Node loopNode = new ControlFlowGraph.Node(branchNode.getId(), CfgNodeKind.LOOP,
                branchNode.getAst(), branchNode.getContent());
        graph.replaceNode(cond.entry(), loopNode);
        return new ConditionChain(loopNode.getId(), cond.trueExits(), cond.falseExits());
    }


    private ConditionChain buildAndChain(InfixExpression infix) {
        // Collect all operands in left-to-right order (handles extended operands)
        List<Expression> operands = collectOperands(infix, InfixExpression.Operator.CONDITIONAL_AND);

        // Build the chain right-to-left so we can thread TRUE edges forward
        ConditionChain chain = buildCondition(operands.get(operands.size() - 1));

        for (int i = operands.size() - 2; i >= 0; i--) {
            Expression left = operands.get(i);
            int leftNode = graph.addNode(CfgNodeKind.BRANCH, left, left.toString());

            // left TRUE → enter the rest of the chain
            graph.addEdge(leftNode, chain.entry(), CfgEdgeKind.TRUE);

            // left FALSE → combine with whatever false exits the chain already has
            List<PendingExit> falseExits = new ArrayList<>();
            falseExits.add(new PendingExit(leftNode, CfgEdgeKind.FALSE));
            falseExits.addAll(chain.falseExits());

            chain = new ConditionChain(leftNode, chain.trueExits(), falseExits);
        }
        return chain;
    }


    private ConditionChain buildOrChain(InfixExpression infix) {
        List<Expression> operands = collectOperands(infix, InfixExpression.Operator.CONDITIONAL_OR);

        ConditionChain chain = buildCondition(operands.get(operands.size() - 1));

        for (int i = operands.size() - 2; i >= 0; i--) {
            Expression left = operands.get(i);
            int leftNode = graph.addNode(CfgNodeKind.BRANCH, left, left.toString());

            // left FALSE → enter the rest of the chain
            graph.addEdge(leftNode, chain.entry(), CfgEdgeKind.FALSE);

            // left TRUE → combine with the chain's true exits
            List<PendingExit> trueExits = new ArrayList<>();
            trueExits.add(new PendingExit(leftNode, CfgEdgeKind.TRUE));
            trueExits.addAll(chain.trueExits());

            chain = new ConditionChain(leftNode, trueExits, chain.falseExits());
        }
        return chain;
    }


    @SuppressWarnings("unchecked")
    private List<Expression> collectOperands(InfixExpression infix, InfixExpression.Operator op) {
        List<Expression> result = new ArrayList<>();
        result.add(infix.getLeftOperand());
        result.add(infix.getRightOperand());
        result.addAll(infix.extendedOperands());
        return result;
    }

    private List<PendingExit> asFalseExits(List<PendingExit> falseExits) {
        List<PendingExit> result = new ArrayList<>(falseExits.size());
        for (PendingExit e : falseExits) {
            result.add(new PendingExit(e.fromNode(), CfgEdgeKind.FALSE));
        }
        return result;
    }
}
