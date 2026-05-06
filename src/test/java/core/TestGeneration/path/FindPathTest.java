package core.TestGeneration.path;

import core.CFG.CfgNode;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FindPathTest {

    @Before
    public void setUp() {
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
}
