package core.CFG.graph;

import org.junit.Test;

import static org.junit.Assert.*;

public class CfgGraphTest {
    @Test
    public void addsNodesAndEdges() {
        CfgGraph graph = new CfgGraph();
        int entry = graph.addNode(CfgNodeKind.ENTRY, null, "ENTRY", -1);
        int stmt = graph.addNode(CfgNodeKind.STMT, null, "x = 1", 10);
        graph.addEdge(entry, stmt, CfgEdgeKind.NORMAL, null);

        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(1, graph.outgoing(entry).size());
        assertEquals(stmt, graph.outgoing(entry).get(0).getTo());
    }
}
