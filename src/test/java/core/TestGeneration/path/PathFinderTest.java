package core.TestGeneration.path;

import core.CFG.graph.CfgEdgeKind;
import core.CFG.graph.CfgGraph;
import core.CFG.graph.CfgNodeKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PathFinderTest {
    @Test
    public void findsPathThroughMid() {
        CfgGraph graph = new CfgGraph();
        int entry = graph.addNode(CfgNodeKind.ENTRY, null, "ENTRY", -1);
        int mid = graph.addNode(CfgNodeKind.STMT, null, "M", 1);
        int exit = graph.addNode(CfgNodeKind.EXIT, null, "EXIT", -1);
        graph.addEdge(entry, mid, CfgEdgeKind.NORMAL, null);
        graph.addEdge(mid, exit, CfgEdgeKind.NORMAL, null);

        PathFinder finder = new PathFinder();
        List<PathStep> steps = finder.findPathThrough(graph, entry, mid, exit);
        assertEquals(3, steps.size());
        assertEquals(entry, steps.get(0).getNodeId());
        assertEquals(mid, steps.get(1).getNodeId());
        assertEquals(exit, steps.get(2).getNodeId());
    }
}
