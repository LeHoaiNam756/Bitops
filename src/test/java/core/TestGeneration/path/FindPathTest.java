package core.TestGeneration.path;

import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FindPathTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testFindPathThrough_simpleLinearPath() {
        CfgNode start = new CfgNode();
        start.setBeginCfgNode(true);

        CfgNode middle = new CfgNode();
        middle.setContent("middle");

        CfgNode end = new CfgNode();
        end.setEndCfgNode(true);

        start.setAfterNode(middle);
        middle.setAfterNode(end);

        LinkedList<FindPath.PathNode> path = FindPath.findPathThrough(start, middle, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());

        boolean foundMiddle = false;
        boolean foundEnd = false;
        for (FindPath.PathNode pn : path) {
            if (pn.node == middle) foundMiddle = true;
            if (pn.node == end) foundEnd = true;
        }
        assertTrue("Path should contain middle node", foundMiddle);
        assertTrue("Path should contain end node", foundEnd);
    }

    @Test
    public void testFindPathThrough_nullWhenNoPath() {
        CfgNode start = new CfgNode();
        start.setBeginCfgNode(true);

        CfgNode middle = new CfgNode();
        middle.setContent("middle");

        CfgNode end = new CfgNode();
        end.setEndCfgNode(true);

        start.setAfterNode(null);
        middle.setAfterNode(null);

        LinkedList<FindPath.PathNode> path = FindPath.findPathThrough(start, middle, end);

        assertNull(path);
    }

    @Test
    public void testFindPathThrough_withBranching() {
        CfgNode start = new CfgNode();
        start.setBeginCfgNode(true);

        CfgBoolExprNode decision = new CfgBoolExprNode();
        decision.setContent("x > 0");

        CfgNode trueBranch = new CfgNode();
        trueBranch.setContent("trueBranch");

        CfgNode falseBranch = new CfgNode();
        falseBranch.setContent("falseBranch");

        CfgNode merge = new CfgNode();
        merge.setContent("merge");

        CfgNode end = new CfgNode();
        end.setEndCfgNode(true);

        start.setAfterNode(decision);
        decision.setTrueNode(trueBranch);
        decision.setFalseNode(falseBranch);
        trueBranch.setAfterNode(merge);
        falseBranch.setAfterNode(merge);
        merge.setAfterNode(end);

        LinkedList<FindPath.PathNode> path = FindPath.findPathThrough(start, merge, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());

        boolean foundMerge = false;
        boolean foundEnd = false;
        for (FindPath.PathNode pn : path) {
            if (pn.node == merge) foundMerge = true;
            if (pn.node == end) foundEnd = true;
        }
        assertTrue("Path should contain merge node", foundMerge);
        assertTrue("Path should contain end node", foundEnd);
    }

    @Test
    public void testFindPathThrough_withLoop() {
        CfgNode start = new CfgNode();
        start.setBeginCfgNode(true);

        CfgBoolExprNode loopCondition = new CfgBoolExprNode();
        loopCondition.setContent("i < 10");
        loopCondition.setLoopCondition(true);

        CfgNode loopBody = new CfgNode();
        loopBody.setContent("loopBody");

        CfgNode afterLoop = new CfgNode();
        afterLoop.setContent("afterLoop");

        CfgNode end = new CfgNode();
        end.setEndCfgNode(true);

        start.setAfterNode(loopCondition);
        loopCondition.setTrueNode(loopBody);
        loopCondition.setFalseNode(afterLoop);
        loopBody.setAfterNode(loopCondition);
        afterLoop.setAfterNode(end);

        LinkedList<FindPath.PathNode> path = FindPath.findPathThrough(start, afterLoop, end);

        assertNotNull(path);
        assertFalse(path.isEmpty());

        boolean foundAfterLoop = false;
        boolean foundEnd = false;
        for (FindPath.PathNode pn : path) {
            if (pn.node == afterLoop) foundAfterLoop = true;
            if (pn.node == end) foundEnd = true;
        }
        assertTrue("Path should contain afterLoop node", foundAfterLoop);
        assertTrue("Path should contain end node", foundEnd);
    }

    @Test
    public void testFindPathThrough_startEqualsEnd() {
        CfgNode node = new CfgNode();
        node.setBeginCfgNode(true);
        node.setEndCfgNode(true);

        LinkedList<FindPath.PathNode> path = FindPath.findPathThrough(node, node, node);

        assertNotNull(path);
    }

    @Test
    public void testFindBackEdges_simpleLoop() {
        CfgNode start = new CfgNode();
        start.setBeginCfgNode(true);

        CfgBoolExprNode loopCondition = new CfgBoolExprNode();
        loopCondition.setContent("i < 10");
        loopCondition.setLoopCondition(true);

        CfgNode loopBody = new CfgNode();
        loopBody.setContent("loopBody");

        start.setAfterNode(loopCondition);
        loopCondition.setTrueNode(loopBody);
        loopCondition.setFalseNode(null);
        loopBody.setAfterNode(loopCondition);

        List<FindPath.BackEdge> backEdges = FindPath.findBackEdges(start);

        assertNotNull(backEdges);
        assertFalse(backEdges.isEmpty());

        boolean foundLoopBackEdge = false;
        for (FindPath.BackEdge be : backEdges) {
            if (be.head() == loopCondition && be.tail() == loopBody) {
                foundLoopBackEdge = true;
                break;
            }
        }
        assertTrue("Should find back-edge from loopBody to loopCondition", foundLoopBackEdge);
    }

    @Test
    public void testGetUncoveredNode() {
        CfgNode node1 = new CfgNode();
        CfgNode node2 = new CfgNode();
        CfgNode node3 = new CfgNode();

        Set<CfgNode> total = new HashSet<>();
        total.add(node1);
        total.add(node2);
        total.add(node3);

        Set<CfgNode> covered = new HashSet<>();
        covered.add(node1);

        CfgNode uncovered = FindPath.getUncoveredNode(total, covered);

        assertNotNull(uncovered);
        assertTrue("Should find uncovered node", uncovered == node2 || uncovered == node3);
    }

    @Test
    public void testGetUncoveredNode_allCovered() {
        CfgNode node1 = new CfgNode();
        CfgNode node2 = new CfgNode();

        Set<CfgNode> total = new HashSet<>();
        total.add(node1);
        total.add(node2);

        Set<CfgNode> covered = new HashSet<>();
        covered.add(node1);
        covered.add(node2);

        CfgNode uncovered = FindPath.getUncoveredNode(total, covered);

        assertNull(uncovered);
    }

    @Test
    public void testGetUncoveredNode_nullInputs() {
        assertNull(FindPath.getUncoveredNode(null, null));
        assertNull(FindPath.getUncoveredNode(null, new HashSet<>()));
    }

    @Test
    public void testRequiredCycleCount_withinLimit() {
        CfgNode start = new CfgNode();
        CfgBoolExprNode loopHeader = new CfgBoolExprNode();
        CfgNode loopBody = new CfgNode();

        start.setAfterNode(loopHeader);
        loopHeader.setTrueNode(loopBody);
        loopHeader.setFalseNode(null);
        loopBody.setAfterNode(loopHeader);

        FindPath.BackEdge backEdge = new FindPath.BackEdge(loopHeader, loopBody);

        int cycles = FindPath.requiredCycleCount(start, loopBody, backEdge, 3);

        assertTrue("Cycles should be >= 0", cycles >= 0);
    }

    @Test
    public void testPathNode_equals() {
        CfgNode node1 = new CfgNode();
        node1.setContent("test");

        FindPath.PathNode pn1 = new FindPath.PathNode(node1, true);
        FindPath.PathNode pn2 = new FindPath.PathNode(node1, true);
        FindPath.PathNode pn3 = new FindPath.PathNode(node1, false);

assertEquals(pn1, pn2);
        assertFalse(pn1.equals(pn3));
        assertEquals(pn1.hashCode(), pn2.hashCode());
    }
}