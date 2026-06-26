package core.cfg;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import utils.Parser;

import java.util.List;
import static org.junit.Assert.*;

public class ControlFlowGraphBuilderTest {
    @Test
    public void buildsGraphFromMethod() throws Exception {
       String src = "void foo() {}";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));
        assertEquals(3, cfg.nodeCount());
    }

    @Test
    public void testForLoopInfinite() throws Exception {
        String src = """
            class Test {
                void foo() { for (;;) { x++; } }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        assertEquals(4, cfg.nodeCount());

        ControlFlowGraph.Node entry = null, loop = null, stmt = null, exit = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            switch (n.getKind()) {
                case ENTRY: entry = n; break;
                case LOOP: loop = n; break;
                case STMT: stmt = n; break;
                case EXIT: exit = n; break;
            }
        }
        assertNotNull(entry);
        assertNotNull(loop);
        assertNotNull(stmt);
        assertNotNull(exit);

        assertEquals("true", loop.getContent());

        List<ControlFlowGraph.Edge> branchOut = cfg.outgoing(loop.getId());
        assertEquals(1, branchOut.size());
        assertEquals(CfgEdgeKind.TRUE, branchOut.get(0).getKind());
        assertEquals(stmt.getId(), branchOut.get(0).getTo());

        for (ControlFlowGraph.Edge e : branchOut) {
            assertNotEquals(CfgEdgeKind.FALSE, e.getKind());
        }

        List<ControlFlowGraph.Edge> stmtOut = cfg.outgoing(stmt.getId());
        assertEquals(1, stmtOut.size());
        assertEquals(CfgEdgeKind.NORMAL, stmtOut.get(0).getKind());
        assertEquals(loop.getId(), stmtOut.get(0).getTo());

        List<ControlFlowGraph.Edge> exitIn = cfg.incoming(exit.getId());
        assertEquals(0, exitIn.size());
    }

    @Test
    public void testNestedLoopsBreakTargetsCorrectLoop() throws Exception {
        String src = """
            class Test {
                void foo() {
                    for (int i = 0; i < 10; i++) {
                        while (x > 0) {
                            break;
                        }
                        y++;
                    }
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node breakNode = null, yPlusPlusNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String content = n.getContent();
            if (content != null && content.contains("break")) {
                breakNode = n;
            }
            if (content != null && content.contains("y++")) {
                yPlusPlusNode = n;
            }
        }
        assertNotNull("break node should exist", breakNode);
        assertNotNull("y++ node should exist", yPlusPlusNode);

        List<ControlFlowGraph.Edge> breakOut = cfg.outgoing(breakNode.getId());
        assertFalse("break should have outgoing edges", breakOut.isEmpty());
        boolean breakReachesY = false;
        for (ControlFlowGraph.Edge e : breakOut) {
            if (e.getTo() == yPlusPlusNode.getId()) {
                breakReachesY = true;
            }
        }
        assertTrue("break should exit to y++ (after while), not skip it", breakReachesY);

        ControlFlowGraph.Node exit = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            if (cfg.getNode(i).getKind() == CfgNodeKind.EXIT) {
                exit = cfg.getNode(i);
                break;
            }
        }
        assertNotNull(exit);
        assertTrue("EXIT should be reachable", cfg.incoming(exit.getId()).size() > 0);
    }

    @Test
    public void testReturnInMiddle() throws Exception {
        String src = """
            class Test {
                void foo() { x = 1; return; y = 2; }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        assertEquals(5, cfg.nodeCount());

        ControlFlowGraph.Node entry = null, returnNode = null, y2Node = null, exit = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            switch (n.getKind()) {
                case ENTRY: entry = n; break;
                case EXIT: exit = n; break;
                case STMT:
                    String c = n.getContent();
                    if (c != null && c.contains("return")) returnNode = n;
                    if (c != null && c.strip().contains("y=2")) y2Node = n;
                    break;
            }
        }
        assertNotNull(entry);
        assertNotNull(returnNode);
        assertNotNull(y2Node);
        assertNotNull(exit);

        List<ControlFlowGraph.Edge> returnOut = cfg.outgoing(returnNode.getId());
        assertEquals(1, returnOut.size());
        assertEquals(CfgEdgeKind.NORMAL, returnOut.get(0).getKind());
        assertEquals(exit.getId(), returnOut.get(0).getTo());

        List<ControlFlowGraph.Edge> y2In = cfg.incoming(y2Node.getId());
        assertEquals("y=2 should have no incoming edges (unreachable)", 0, y2In.size());

        int reachableEdges = 0;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getKind() == CfgNodeKind.EXIT) continue;
            for (ControlFlowGraph.Edge e : cfg.outgoing(i)) {
                ControlFlowGraph.Node target = cfg.getNode(e.getTo());
                if (target.getKind() != CfgNodeKind.ENTRY) {
                    String content = target.getContent();
                    if (content == null || !content.strip().contains("y=2")) {
                        reachableEdges++;
                    }
                }
            }
        }
        assertEquals(4, reachableEdges);
    }

    @Test
    public void testReturnInsideIf() throws Exception {
        String src = """
            class Test {
                void foo() { if (x > 0) { return; } y = 1; }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node branch = null, returnNode = null, y1Node = null, exit = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            switch (n.getKind()) {
                case BRANCH: branch = n; break;
                case EXIT: exit = n; break;
                case STMT:
                    String c = n.getContent();
                    if (c != null && c.contains("return")) returnNode = n;
                    if (c != null && c.strip().contains("y=1")) y1Node = n;
                    break;
            }
        }
        assertNotNull(branch);
        assertNotNull(returnNode);
        assertNotNull(y1Node);
        assertNotNull(exit);

        List<ControlFlowGraph.Edge> branchOut = cfg.outgoing(branch.getId());
        ControlFlowGraph.Edge trueEdge = null, falseEdge = null;
        for (ControlFlowGraph.Edge e : branchOut) {
            if (e.getKind() == CfgEdgeKind.TRUE) trueEdge = e;
            if (e.getKind() == CfgEdgeKind.FALSE) falseEdge = e;
        }
        assertNotNull(trueEdge);
        assertNotNull(falseEdge);
        assertEquals(returnNode.getId(), trueEdge.getTo());
        assertEquals(y1Node.getId(), falseEdge.getTo());

        List<ControlFlowGraph.Edge> returnOut = cfg.outgoing(returnNode.getId());
        assertEquals(1, returnOut.size());
        assertEquals(exit.getId(), returnOut.get(0).getTo());

        List<ControlFlowGraph.Edge> y1Out = cfg.outgoing(y1Node.getId());
        assertEquals(1, y1Out.size());
        assertEquals(exit.getId(), y1Out.get(0).getTo());

        List<ControlFlowGraph.Edge> exitIn = cfg.incoming(exit.getId());
        assertEquals(2, exitIn.size());
    }

    @Test
    public void testNestedIf() throws Exception {
        String src = """
            class Test {
                void foo() {
                    if (a) {
                        if (b) {
                            x = 1;
                        }
                    }
                    y = 2;
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node y2Node = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (c != null && c.strip().contains("y=2")) {
                y2Node = n;
                break;
            }
        }
        assertNotNull("y=2 node should exist", y2Node);

        List<ControlFlowGraph.Edge> y2In = cfg.incoming(y2Node.getId());
        assertEquals("y=2 should have 3 incoming edges (3 paths)", 3, y2In.size());

        int fromOuterBranch = 0, fromInnerBranch = 0, fromX1Stmt = 0;
        for (ControlFlowGraph.Edge e : y2In) {
            ControlFlowGraph.Node src2 = cfg.getNode(e.getFrom());
            if (src2.getKind() == CfgNodeKind.BRANCH) {
                if (e.getKind() == CfgEdgeKind.FALSE) {
                    String content = src2.getContent();
                    if (content != null && content.contains("a")) {
                        fromOuterBranch++;
                    } else if (content != null && content.contains("b")) {
                        fromInnerBranch++;
                    }
                }
            } else if (src2.getKind() == CfgNodeKind.STMT) {
                fromX1Stmt++;
            }
        }
        assertEquals(fromOuterBranch + fromInnerBranch + fromX1Stmt, 3);
        assertTrue("outer branch FALSE should reach y=2", fromOuterBranch >= 1);
        assertTrue("inner branch FALSE should reach y=2", fromInnerBranch >= 1);
        assertTrue("x=1 stmt should reach y=2", fromX1Stmt >= 1);
    }

    @Test
    public void testWhileLoop() throws Exception {
        String src = "class T { void foo() { while (x > 0) { x--; } } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        assertEquals(4, cfg.nodeCount());

        ControlFlowGraph.Node loopNode = null, stmtNode = null, exitNode = null, entryNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String content = n.getContent();
            if (n.getKind() == CfgNodeKind.ENTRY) entryNode = n;
            else if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (content.contains("x>0") || content.contains("x > 0")) loopNode = n;
            else if (content.contains("x--")) stmtNode = n;
        }
        assertNotNull(loopNode);
        assertNotNull(stmtNode);
        assertNotNull(exitNode);
        assertEquals(CfgNodeKind.LOOP, loopNode.getKind());
        assertEquals(CfgNodeKind.STMT, stmtNode.getKind());

        List<ControlFlowGraph.Edge> loopOut = cfg.outgoing(loopNode.getId());
        assertEquals(2, loopOut.size());
        boolean foundTrueToStmt = false, foundFalseToExit = false;
        for (ControlFlowGraph.Edge e : loopOut) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == stmtNode.getId()) foundTrueToStmt = true;
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) foundFalseToExit = true;
        }
        assertTrue("LOOP should have TRUE edge to STMT", foundTrueToStmt);
        assertTrue("LOOP should have FALSE edge to EXIT", foundFalseToExit);

        List<ControlFlowGraph.Edge> stmtOut = cfg.outgoing(stmtNode.getId());
        assertEquals(1, stmtOut.size());
        assertEquals(CfgEdgeKind.NORMAL, stmtOut.get(0).getKind());
        assertEquals(loopNode.getId(), stmtOut.get(0).getTo());

        List<ControlFlowGraph.Edge> exitIn = cfg.incoming(exitNode.getId());
        assertEquals(1, exitIn.size());
        assertEquals(loopNode.getId(), exitIn.get(0).getFrom());
        assertEquals(CfgEdgeKind.FALSE, exitIn.get(0).getKind());
    }

    @Test
    public void testWhileWithBreak() throws Exception {
        String src = "class T { void foo() { while (true) { if (x == 0) break; x--; } } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node breakNode = null, stmtDecr = null, exitNode = null, condNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (c.contains("break")) breakNode = n;
            else if (c.contains("x--")) stmtDecr = n;
            else if (n.getKind() == CfgNodeKind.LOOP) condNode = n;
        }
        assertNotNull(breakNode);
        assertNotNull(exitNode);

        boolean breakGoesToExit = false, breakGoesToCond = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(breakNode.getId())) {
            if (e.getTo() == exitNode.getId()) breakGoesToExit = true;
            if (condNode != null && e.getTo() == condNode.getId()) breakGoesToCond = true;
        }
        assertTrue("break should route to EXIT", breakGoesToExit);
        assertFalse("break should NOT route back to loop cond", breakGoesToCond);

        if (stmtDecr != null && condNode != null) {
            boolean decrGoesToCond = false;
            for (ControlFlowGraph.Edge e : cfg.outgoing(stmtDecr.getId())) {
                if (e.getTo() == condNode.getId()) decrGoesToCond = true;
            }
            assertTrue("x-- should route back to loop cond", decrGoesToCond);
        }

        ControlFlowGraph.Node entryNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            if (cfg.getNode(i).getKind() == CfgNodeKind.ENTRY) { entryNode = cfg.getNode(i); break; }
        }
        assertNotNull(entryNode);
        java.util.Queue<Integer> queue = new java.util.ArrayDeque<>();
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        queue.add(entryNode.getId());
        visited.add(entryNode.getId());
        boolean pathExists = false;
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == exitNode.getId()) { pathExists = true; break; }
            for (ControlFlowGraph.Edge e : cfg.outgoing(cur)) {
                if (visited.add(e.getTo())) queue.add(e.getTo());
            }
        }
        assertTrue("There should be a path from ENTRY to EXIT", pathExists);
    }

    @Test
    public void testWhileWithContinue() throws Exception {
        String src = "class T { void foo() { while (x > 0) { if (x == 5) continue; x--; } } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node continueNode = null, stmtDecr = null, exitNode = null, loopCond = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (c.contains("continue")) continueNode = n;
            else if (c.contains("x--") && !c.contains("x > 0")) stmtDecr = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopCond = n;
        }
        assertNotNull(continueNode);
        assertNotNull(loopCond);
        assertNotNull(exitNode);

        List<ControlFlowGraph.Edge> contOut = cfg.outgoing(continueNode.getId());
        assertEquals(1, contOut.size());
        assertEquals(CfgEdgeKind.NORMAL, contOut.get(0).getKind());
        assertEquals(loopCond.getId(), contOut.get(0).getTo());

        if (stmtDecr != null) {
            boolean decrToCond = false;
            for (ControlFlowGraph.Edge e : cfg.outgoing(stmtDecr.getId())) {
                if (e.getTo() == loopCond.getId()) decrToCond = true;
            }
            assertTrue("x-- should route back to loop cond", decrToCond);
        }

        int falseEdgesToExit = 0;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopCond.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) falseEdgesToExit++;
        }
        assertEquals(1, falseEdgesToExit);
    }

    @Test
    public void testForLoopStandard() throws Exception {
        String src = "class T { void foo() { for (int i = 0; i < 10; i++) { sum += i; } } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        assertEquals(6, cfg.nodeCount());

        ControlFlowGraph.Node initNode = null, condNode = null, bodyNode = null, updateNode = null, exitNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (c.contains("i=0") || c.contains("i = 0")) initNode = n;
            else if (c.contains("i<10") || c.contains("i < 10")) condNode = n;
            else if (c.contains("sum") && c.contains("+=")) bodyNode = n;
            else if (c.contains("i++")) updateNode = n;
        }
        assertNotNull(initNode);
        assertNotNull(condNode);
        assertNotNull(bodyNode);
        assertNotNull(updateNode);
        assertNotNull(exitNode);

        assertEquals(CfgNodeKind.STMT, initNode.getKind());
        assertEquals(CfgNodeKind.LOOP, condNode.getKind());
        assertEquals(CfgNodeKind.STMT, bodyNode.getKind());
        assertEquals(CfgNodeKind.STMT, updateNode.getKind());

        boolean updateToCond = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(updateNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == condNode.getId()) updateToCond = true;
        }
        assertTrue("update i++ should have NORMAL edge back to cond", updateToCond);

        boolean condFalseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(condNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) condFalseToExit = true;
        }
        assertTrue("cond should have FALSE edge to EXIT", condFalseToExit);

        boolean condTrueToBody = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(condNode.getId())) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == bodyNode.getId()) condTrueToBody = true;
        }
        assertTrue("cond should have TRUE edge to body", condTrueToBody);
    }

    @Test
    public void testForLoopNoInitNoUpdate() throws Exception {
        String src = "class T { void foo() { for (; x < 10;) { x++; } } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        assertEquals(4, cfg.nodeCount());

        ControlFlowGraph.Node condNode = null, bodyNode = null, exitNode = null;
        int stmtCount = 0, loopCount = 0;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.STMT) { stmtCount++; if (c.contains("x++")) bodyNode = n; }
            else if (n.getKind() == CfgNodeKind.LOOP) { loopCount++; if (c.contains("x<10") || c.contains("x < 10")) condNode = n; }
        }
        assertEquals(1, loopCount);
        assertEquals(1, stmtCount);
        assertNotNull(condNode);
        assertNotNull(bodyNode);
        assertNotNull(exitNode);

        boolean bodyToCond = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(bodyNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == condNode.getId()) bodyToCond = true;
        }
        assertTrue("body x++ should have NORMAL edge back to cond", bodyToCond);

        boolean condFalseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(condNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) condFalseToExit = true;
        }
        assertTrue("cond should have FALSE edge to EXIT", condFalseToExit);
    }

    // ─────────────────────────────────────────────
    // DO-WHILE TESTS
    // ─────────────────────────────────────────────

    @Test
    public void testDoWhile_basic_bodyExecutesBeforeCondition() throws Exception {
        String src = "class T { void foo() { int x = 0; do { x++; } while (x < 10); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node loopNode = null, stmtNode = null, exitNode = null, entryNode = null, initXNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getKind() == CfgNodeKind.ENTRY) entryNode = n;
            else if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getContent() != null && n.getContent().contains("x++")) stmtNode = n;
            else if (n.getContent() != null && n.getContent().strip().contains("x=0")) initXNode = n;
        }
        assertNotNull(loopNode);
        assertNotNull(stmtNode);
        assertNotNull(exitNode);

        // fragment entry should be STMT (body), not LOOP
        List<ControlFlowGraph.Edge> initXOut = cfg.outgoing(initXNode.getId());
        assertEquals(1, initXOut.size());
        assertEquals(stmtNode.getId(), initXOut.get(0).getTo());

        // LOOP TRUE edge back to body STMT
        boolean trueToBody = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == stmtNode.getId()) trueToBody = true;
        }
        assertTrue("LOOP TRUE should target body STMT (back edge)", trueToBody);

        // LOOP FALSE edge to EXIT
        boolean falseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) falseToExit = true;
        }
        assertTrue("LOOP FALSE should target EXIT", falseToExit);
    }

    @Test
    public void testDoWhile_conditionFalseFromStart_bodyStillExecutesOnce() throws Exception {
        String src = "class T { void foo() { do { log(); } while (false); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        // Exactly 4 nodes: ENTRY, STMT(log()), LOOP(false), EXIT
        assertEquals(4, cfg.nodeCount());

        ControlFlowGraph.Node stmtNode = null, loopNode = null, exitNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getKind() == CfgNodeKind.STMT && n.getContent() != null && n.getContent().contains("log()")) stmtNode = n;
        }
        assertNotNull(stmtNode);
        assertNotNull(loopNode);
        assertNotNull(exitNode);

        // FALSE exit of LOOP wires to EXIT
        boolean falseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) falseToExit = true;
        }
        assertTrue("LOOP FALSE should wire to EXIT", falseToExit);

        // Body STMT is reachable from ENTRY (executes once unconditionally)
        ControlFlowGraph.Node entryNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            if (cfg.getNode(i).getKind() == CfgNodeKind.ENTRY) { entryNode = cfg.getNode(i); break; }
        }
        java.util.Set<Integer> reachable = new java.util.HashSet<>();
        java.util.Queue<Integer> q = new java.util.ArrayDeque<>();
        q.add(entryNode.getId());
        reachable.add(entryNode.getId());
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (ControlFlowGraph.Edge e : cfg.outgoing(cur)) {
                if (reachable.add(e.getTo())) q.add(e.getTo());
            }
        }
        assertTrue("body STMT should be reachable from ENTRY", reachable.contains(stmtNode.getId()));
    }

    @Test
    public void testDoWhile_break_exitsLoopWithoutCheckingCondition() throws Exception {
        String src = "class T { void foo() { do { if (flag) break; process(); } while (true); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node breakNode = null, processNode = null, loopNode = null, branchNode = null, exitNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getKind() == CfgNodeKind.BRANCH) branchNode = n;
            else if (c != null && c.contains("break")) breakNode = n;
            else if (c != null && c.contains("process()")) processNode = n;
        }
        assertNotNull(breakNode);
        assertNotNull(loopNode);
        assertNotNull(exitNode);
        assertNotNull(branchNode);

        // break has NORMAL edge that bypasses LOOP node entirely (wired to EXIT by build())
        boolean breakToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(breakNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == exitNode.getId()) breakToExit = true;
        }
        assertTrue("break NORMAL edge should go to EXIT", breakToExit);

        // LOOP TRUE back-edge targets BRANCH(flag), not break or process directly
        boolean trueToBranch = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == branchNode.getId()) trueToBranch = true;
        }
        assertTrue("LOOP TRUE back-edge should target BRANCH (top of body)", trueToBranch);
    }

    @Test
    public void testDoWhile_continue_jumpsToConditionNotBodyTop() throws Exception {
        String src = "class T { void foo() { do { if (skip) continue; work(); } while (running); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node continueNode = null, workNode = null, loopNode = null, branchNode = null, exitNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getKind() == CfgNodeKind.BRANCH) branchNode = n;
            else if (c != null && c.contains("continue")) continueNode = n;
            else if (c != null && c.contains("work()")) workNode = n;
        }
        assertNotNull(continueNode);
        assertNotNull(loopNode);
        assertNotNull(branchNode);
        assertNotNull(exitNode);

        // continue has NORMAL edge to LOOP node (condition), NOT to BRANCH
        boolean contToLoop = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(continueNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == loopNode.getId()) contToLoop = true;
        }
        assertTrue("continue NORMAL edge should go to LOOP (condition)", contToLoop);

        // Both paths converge at LOOP node
        boolean workToLoop = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(workNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == loopNode.getId()) workToLoop = true;
        }
        assertTrue("work() NORMAL edge should go to LOOP", workToLoop);

        // LOOP TRUE back-edge targets BRANCH(skip) (top of body)
        boolean trueToBranch = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == branchNode.getId()) trueToBranch = true;
        }
        assertTrue("LOOP TRUE back-edge should target BRANCH(skip)", trueToBranch);

        // LOOP FALSE to EXIT
        boolean falseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) falseToExit = true;
        }
        assertTrue("LOOP FALSE should target EXIT", falseToExit);
    }

    @Test
    public void testDoWhile_nested_innerBreakDoesNotExitOuterLoop() throws Exception {
        String src = """
            class T {
                void foo() {
                    do {
                        do { if (x) break; } while (inner);
                        x=2;
                    } while (outer);
                }
            }""";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        // Count LOOP nodes
        int loopCount = 0;
        ControlFlowGraph.Node[] loopNodes = new ControlFlowGraph.Node[2];
        int li = 0;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getKind() == CfgNodeKind.LOOP) {
                loopCount++;
                if (li < 2) loopNodes[li++] = n;
            }
        }
        assertEquals("should have exactly 2 LOOP nodes", 2, loopCount);

        ControlFlowGraph.Node breakNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getContent() != null && n.getContent().contains("break")) { breakNode = n; break; }
        }
        assertNotNull(breakNode);
        // ── Distinguish inner vs outer LOOP ──────────────────────────────────────
        // Inner LOOP's FALSE edge → outer LOOP (it exits to the outer condition)
        // Outer LOOP's FALSE edge → EXIT
        ControlFlowGraph.Node innerLoop = null, outerLoop = null;
        for (ControlFlowGraph.Node ln : loopNodes) {
            for (ControlFlowGraph.Edge e : cfg.outgoing(ln.getId())) {
                if (e.getKind() == CfgEdgeKind.FALSE
                        && cfg.getNode(e.getTo()).getKind() == CfgNodeKind.LOOP) {
                    innerLoop = ln;   // false-exit lands on another LOOP → this is inner
                }
                if (e.getKind() == CfgEdgeKind.FALSE
                        && cfg.getNode(e.getTo()).getKind() == CfgNodeKind.EXIT) {
                    outerLoop = ln;   // false-exit lands on EXIT → this is outer
                }
            }
        }
        int outerLoopId = outerLoop.getId();

        // break should NOT target outer loop
        for (ControlFlowGraph.Edge e : cfg.outgoing(breakNode.getId())) {
            assertNotEquals("break should NOT target outer LOOP", outerLoopId, e.getTo());
        }

        // outer LOOP TRUE back-edge targets inner do-while entry (the inner do-while's body or inner LOOP)
        boolean outerTrueToInner = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(outerLoopId)) {
            if (e.getKind() == CfgEdgeKind.TRUE) {
                ControlFlowGraph.Node target = cfg.getNode(e.getTo());
                // Should target the inner do-while fragment entry (not the inner LOOP node itself)
                if (target.getKind() != CfgNodeKind.EXIT && target.getKind() != CfgNodeKind.ENTRY) {
                    outerTrueToInner = true;
                }
            }
        }
        assertTrue("outer LOOP TRUE should target inner do-while entry", outerTrueToInner);
    }

    @Test
    public void testDoWhile_returnInsideBody_wiresDirectlyToExitSink() throws Exception {
        String src = "class T { void foo() { do { if (done) return result; step(); } while (more); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node returnNode = null, stepNode = null, loopNode = null, branchNode = null, exitNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            String c = n.getContent();
            if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getKind() == CfgNodeKind.BRANCH) branchNode = n;
            else if (c != null && c.contains("return")) returnNode = n;
            else if (c != null && c.contains("step()")) stepNode = n;
        }
        assertNotNull(returnNode);
        assertNotNull(loopNode);
        assertNotNull(exitNode);

        // return has direct NORMAL edge to EXIT
        boolean returnToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(returnNode.getId())) {
            if (e.getKind() == CfgEdgeKind.NORMAL && e.getTo() == exitNode.getId()) returnToExit = true;
        }
        assertTrue("return should have direct NORMAL edge to EXIT", returnToExit);

        // EXIT has in-degree >= 2 (return path + FALSE from LOOP)
        assertTrue("EXIT in-degree should be >= 2", cfg.incoming(exitNode.getId()).size() >= 2);

        // LOOP node still present
        assertNotNull("LOOP node should still exist", loopNode);
    }

    @Test
    public void testDoWhile_emptyBody_syntheticNodeStillPresent() throws Exception {
        String src = "class T { void foo() { do { } while (x > 0); } }";
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        // 4 nodes: ENTRY, STMT({}), LOOP(x>0), EXIT
        assertEquals(4, cfg.nodeCount());

        ControlFlowGraph.Node stmtNode = null, loopNode = null, exitNode = null, entryNode = null;
        for (int i = 1; i <= cfg.nodeCount(); i++) {
            ControlFlowGraph.Node n = cfg.getNode(i);
            if (n.getKind() == CfgNodeKind.ENTRY) entryNode = n;
            else if (n.getKind() == CfgNodeKind.EXIT) exitNode = n;
            else if (n.getKind() == CfgNodeKind.LOOP) loopNode = n;
            else if (n.getKind() == CfgNodeKind.STMT) stmtNode = n;
        }
        assertNotNull(stmtNode);
        assertEquals("{}", stmtNode.getContent());
        assertNotNull(loopNode);
        assertNotNull(exitNode);

        // fragment entry is STMT({}), not LOOP
        List<ControlFlowGraph.Edge> entryOut = cfg.outgoing(entryNode.getId());
        assertEquals(1, entryOut.size());
        assertEquals(stmtNode.getId(), entryOut.get(0).getTo());

        // LOOP TRUE edge targets STMT({}) (body re-entry)
        boolean trueToBody = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.TRUE && e.getTo() == stmtNode.getId()) trueToBody = true;
        }
        assertTrue("LOOP TRUE should target STMT({}) (body re-entry)", trueToBody);

        // LOOP FALSE to EXIT
        boolean falseToExit = false;
        for (ControlFlowGraph.Edge e : cfg.outgoing(loopNode.getId())) {
            if (e.getKind() == CfgEdgeKind.FALSE && e.getTo() == exitNode.getId()) falseToExit = true;
        }
        assertTrue("LOOP FALSE should target EXIT", falseToExit);
    }

    @Test
    public void testTryCatch_explicitThrowWiresExceptionEdgeToMatchingCatch() throws Exception {
        String src = """
            class Test {
                int foo(int x) {
                    try {
                        if (x < 0) {
                            throw new IllegalArgumentException();
                        }
                        return 1;
                    } catch (IllegalArgumentException e) {
                        return -1;
                    }
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node throwNode = nodeContaining(cfg, "throw new IllegalArgumentException");
        ControlFlowGraph.Node catchReturn = nodeContaining(cfg, "return -1");
        assertNotNull(throwNode);
        assertNotNull(catchReturn);

        assertTrue("throw should flow to matching catch with EXCEPTION edge",
                hasEdge(cfg, throwNode.getId(), catchReturn.getId(), CfgEdgeKind.EXCEPTION));
    }

    @Test
    public void testTryCatch_unionCatchMatchesThrownType() throws Exception {
        String src = """
            class Test {
                int foo(int x) {
                    try {
                        throw new IllegalStateException();
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        return -1;
                    }
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node throwNode = nodeContaining(cfg, "throw new IllegalStateException");
        ControlFlowGraph.Node catchReturn = nodeContaining(cfg, "return -1");
        assertNotNull(throwNode);
        assertNotNull(catchReturn);

        assertTrue("union catch should receive matching explicit throw",
                hasEdge(cfg, throwNode.getId(), catchReturn.getId(), CfgEdgeKind.EXCEPTION));
    }

    @Test
    public void testTryCatch_unmatchedThrowBubblesToExit() throws Exception {
        String src = """
            class Test {
                int foo() {
                    throw new IllegalArgumentException();
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node throwNode = nodeContaining(cfg, "throw new IllegalArgumentException");
        ControlFlowGraph.Node exitNode = nodeWithKind(cfg, CfgNodeKind.EXIT);
        assertNotNull(throwNode);
        assertNotNull(exitNode);

        assertTrue("unmatched throw should leave method via EXCEPTION edge",
                hasEdge(cfg, throwNode.getId(), exitNode.getId(), CfgEdgeKind.EXCEPTION));
    }

    @Test
    public void testTryCatch_nestedTryUsesNearestMatchingCatch() throws Exception {
        String src = """
            class Test {
                int foo() {
                    try {
                        try {
                            throw new IllegalStateException();
                        } catch (IllegalStateException e) {
                            return 1;
                        }
                    } catch (Exception e) {
                        return 2;
                    }
                }
            }
            """;
        List<ASTNode> methods = Parser.parseSourceToAstFuncList(src);
        ControlFlowGraph cfg = new CfgBuilder().build((MethodDeclaration) methods.get(0));

        ControlFlowGraph.Node throwNode = nodeContaining(cfg, "throw new IllegalStateException");
        ControlFlowGraph.Node innerCatchReturn = nodeContaining(cfg, "return 1");
        ControlFlowGraph.Node outerCatchReturn = nodeContaining(cfg, "return 2");
        assertNotNull(throwNode);
        assertNotNull(innerCatchReturn);
        assertNotNull(outerCatchReturn);

        assertTrue("inner catch should receive nested explicit throw",
                hasEdge(cfg, throwNode.getId(), innerCatchReturn.getId(), CfgEdgeKind.EXCEPTION));
        assertFalse("outer catch should not receive a throw already handled by inner catch",
                hasEdge(cfg, throwNode.getId(), outerCatchReturn.getId(), CfgEdgeKind.EXCEPTION));
    }

    private static ControlFlowGraph.Node nodeContaining(ControlFlowGraph cfg, String content) {
        for (int id : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getContent() != null && node.getContent().contains(content)) {
                return node;
            }
        }
        return null;
    }

    private static ControlFlowGraph.Node nodeWithKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int id : cfg.getNodes()) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getKind() == kind) {
                return node;
            }
        }
        return null;
    }

    private static boolean hasEdge(ControlFlowGraph cfg, int from, int to, CfgEdgeKind kind) {
        for (ControlFlowGraph.Edge edge : cfg.outgoing(from)) {
            if (edge.getTo() == to && edge.getKind() == kind) {
                return true;
            }
        }
        return false;
    }
}
