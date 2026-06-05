package core.testpath;

import core.cfg.CfgEdgeKind;
import core.cfg.CfgNodeKind;
import core.cfg.ControlFlowGraph;

import java.util.*;

/**
 * Computes a small set of complete ENTRY-to-EXIT paths that maximizes static
 * basic-block coverage.
 *
 * <p>The finder first identifies cyclic regions with Tarjan SCCs, removes DFS
 * back-edges inside those regions to obtain a DAG, then solves the DAG minimum
 * path cover using the standard bipartite-matching reduction. Each CFG node is
 * split into a left and right copy and matching capacity is one on both sides,
 * so a node can be the predecessor and successor of at most one cover chain.
 * Cover chains are finally extended back to complete ENTRY-to-EXIT CFG paths
 * with shortest paths in the original graph.</p>
 */
public class LoopCondensationFlowPathFinder implements PathFinder {

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(ControlFlowGraph cfg, int target) {
        return findPath(cfg, target, null);
    }

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(
            ControlFlowGraph cfg,
            int target,
            CfgEdgeKind requiredExit) {

        List<List<ControlFlowGraph.Edge>> cover = findPathCover(cfg);
        List<List<ControlFlowGraph.Edge>> filtered = new ArrayList<>();
        for (List<ControlFlowGraph.Edge> path : cover) {
            if (pathContainsNode(path, target) && usesRequiredExit(path, target, requiredExit)) {
                filtered.add(path);
            }
        }

        if (filtered.isEmpty()) {
            List<ControlFlowGraph.Edge> fallback = shortestCompletePathThroughTarget(cfg, target, requiredExit);
            if (fallback != null) {
                filtered.add(Collections.unmodifiableList(fallback));
            }
        }

        filtered.sort(Comparator.comparingInt(List::size));
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<List<ControlFlowGraph.Edge>> findPath(
            ControlFlowGraph cfg,
            int target,
            CfgEdgeKind requiredExit,
            CoverageTracker tracker) {

        if (tracker == null) {
            return findPath(cfg, target, requiredExit);
        }

        List<List<ControlFlowGraph.Edge>> filtered = new ArrayList<>();
        Set<List<ControlFlowGraph.Edge>> seen = new HashSet<>();
        for (List<ControlFlowGraph.Edge> path : findPathsForUncovered(cfg, tracker)) {
            if (!pathContainsNode(path, target) || !usesRequiredExit(path, target, requiredExit)) {
                continue;
            }
            if (seen.add(path)) {
                filtered.add(path);
            }
        }

        if (filtered.isEmpty()) {
            addFallbackCandidate(filtered, cfg, target, requiredExit);
        }

        filtered.sort(Comparator
                .comparingInt((List<ControlFlowGraph.Edge> path) -> uncoveredScore(path, tracker)).reversed()
                .thenComparing(Comparator
                        .comparingInt((List<ControlFlowGraph.Edge> path) -> coveredScore(path, tracker))
                        .reversed())
                .thenComparingInt(List::size));
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<List<ControlFlowGraph.Edge>> findPathsForUncovered(
            ControlFlowGraph cfg,
            CoverageTracker tracker) {

        if (tracker == null || tracker.isComplete()) {
            return Collections.emptyList();
        }

        List<List<ControlFlowGraph.Edge>> candidates = new ArrayList<>(findPathCover(cfg));
        for (int uncovered : sorted(tracker.getUncovered())) {
            addFallbackCandidate(
                    candidates,
                    cfg,
                    tracker.pathTargetFor(uncovered),
                    tracker.requiredExitFor(uncovered));
        }
        return Collections.unmodifiableList(rankForUncoveredCoverage(candidates, tracker));
    }

    /**
     * Returns the complete ENTRY-to-EXIT paths for the minimum path cover of
     * all nodes that are both reachable from ENTRY and able to reach EXIT.
     */
    public List<List<ControlFlowGraph.Edge>> findPathCover(ControlFlowGraph cfg) {
        int entryId = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exitId = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entryId == -1 || exitId == -1) {
            return Collections.emptyList();
        }

        Set<Integer> relevant = relevantNodes(cfg, entryId, exitId);
        if (relevant.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Integer> sccByNode = tarjanScc(cfg, relevant);
        Set<Integer> cyclicSccs = cyclicSccs(cfg, relevant, sccByNode);
        Set<ControlFlowGraph.Edge> backEdges = findBackEdges(cfg, relevant, sccByNode, cyclicSccs);
        Map<Integer, List<ControlFlowGraph.Edge>> dagEdges = dagEdges(cfg, relevant, backEdges);
        Matching matching = maximumMatching(relevant, dagEdges);

        List<List<Integer>> chains = buildCoverChains(relevant, matching);
        List<List<ControlFlowGraph.Edge>> result = new ArrayList<>();
        for (List<Integer> chain : chains) {
            List<ControlFlowGraph.Edge> path = completePathForChain(cfg, chain, entryId, exitId, matching);
            if (path != null) {
                result.add(Collections.unmodifiableList(path));
            }
        }

        return Collections.unmodifiableList(pruneRedundantPaths(result));
    }

    private Set<Integer> relevantNodes(ControlFlowGraph cfg, int entryId, int exitId) {
        Set<Integer> fromEntry = reachableForward(cfg, entryId);
        Set<Integer> toExit = reachableBackward(cfg, exitId);
        Set<Integer> relevant = new LinkedHashSet<>(fromEntry);
        relevant.retainAll(toExit);
        return relevant;
    }

    private Set<Integer> reachableForward(ControlFlowGraph cfg, int start) {
        Set<Integer> visited = new LinkedHashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (ControlFlowGraph.Edge edge : cfg.outgoing(current)) {
                if (visited.add(edge.getTo())) {
                    queue.addLast(edge.getTo());
                }
            }
        }
        return visited;
    }

    private Set<Integer> reachableBackward(ControlFlowGraph cfg, int start) {
        Set<Integer> visited = new LinkedHashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (ControlFlowGraph.Edge edge : cfg.incoming(current)) {
                if (visited.add(edge.getFrom())) {
                    queue.addLast(edge.getFrom());
                }
            }
        }
        return visited;
    }

    private Map<Integer, Integer> tarjanScc(ControlFlowGraph cfg, Set<Integer> nodes) {
        TarjanState state = new TarjanState();
        for (int node : sorted(nodes)) {
            if (!state.indexByNode.containsKey(node)) {
                strongConnect(cfg, node, nodes, state);
            }
        }
        return state.sccByNode;
    }

    private void strongConnect(
            ControlFlowGraph cfg,
            int node,
            Set<Integer> nodes,
            TarjanState state) {

        state.indexByNode.put(node, state.nextIndex);
        state.lowLinkByNode.put(node, state.nextIndex);
        state.nextIndex++;
        state.stack.push(node);
        state.onStack.add(node);

        for (ControlFlowGraph.Edge edge : sortedOutgoing(cfg, node, nodes)) {
            int next = edge.getTo();
            if (!state.indexByNode.containsKey(next)) {
                strongConnect(cfg, next, nodes, state);
                state.lowLinkByNode.put(node, Math.min(
                        state.lowLinkByNode.get(node),
                        state.lowLinkByNode.get(next)));
            } else if (state.onStack.contains(next)) {
                state.lowLinkByNode.put(node, Math.min(
                        state.lowLinkByNode.get(node),
                        state.indexByNode.get(next)));
            }
        }

        if (Objects.equals(state.lowLinkByNode.get(node), state.indexByNode.get(node))) {
            while (true) {
                int member = state.stack.pop();
                state.onStack.remove(member);
                state.sccByNode.put(member, state.nextSccId);
                if (member == node) {
                    break;
                }
            }
            state.nextSccId++;
        }
    }

    private Set<Integer> cyclicSccs(
            ControlFlowGraph cfg,
            Set<Integer> relevant,
            Map<Integer, Integer> sccByNode) {

        Map<Integer, Integer> sizeByScc = new HashMap<>();
        for (int node : relevant) {
            sizeByScc.merge(sccByNode.get(node), 1, Integer::sum);
        }

        Set<Integer> cyclic = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : sizeByScc.entrySet()) {
            if (entry.getValue() > 1) {
                cyclic.add(entry.getKey());
            }
        }
        for (int node : relevant) {
            for (ControlFlowGraph.Edge edge : cfg.outgoing(node)) {
                if (relevant.contains(edge.getTo()) && edge.getTo() == node) {
                    cyclic.add(sccByNode.get(node));
                }
            }
        }
        return cyclic;
    }

    private Set<ControlFlowGraph.Edge> findBackEdges(
            ControlFlowGraph cfg,
            Set<Integer> relevant,
            Map<Integer, Integer> sccByNode,
            Set<Integer> cyclicSccs) {

        Map<Integer, VisitColor> color = new HashMap<>();
        Set<ControlFlowGraph.Edge> backEdges = new HashSet<>();
        for (int node : sorted(relevant)) {
            if (color.getOrDefault(node, VisitColor.WHITE) == VisitColor.WHITE) {
                findBackEdgesDfs(cfg, node, relevant, sccByNode, cyclicSccs, color, backEdges);
            }
        }
        return backEdges;
    }

    private void findBackEdgesDfs(
            ControlFlowGraph cfg,
            int node,
            Set<Integer> relevant,
            Map<Integer, Integer> sccByNode,
            Set<Integer> cyclicSccs,
            Map<Integer, VisitColor> color,
            Set<ControlFlowGraph.Edge> backEdges) {

        color.put(node, VisitColor.GRAY);
        for (ControlFlowGraph.Edge edge : sortedOutgoing(cfg, node, relevant)) {
            int next = edge.getTo();
            boolean sameCyclicScc = Objects.equals(sccByNode.get(node), sccByNode.get(next))
                    && cyclicSccs.contains(sccByNode.get(node));
            VisitColor nextColor = color.getOrDefault(next, VisitColor.WHITE);
            if (sameCyclicScc && nextColor == VisitColor.GRAY) {
                backEdges.add(edge);
                continue;
            }
            if (nextColor == VisitColor.WHITE) {
                findBackEdgesDfs(cfg, next, relevant, sccByNode, cyclicSccs, color, backEdges);
            }
        }
        color.put(node, VisitColor.BLACK);
    }

    private Map<Integer, List<ControlFlowGraph.Edge>> dagEdges(
            ControlFlowGraph cfg,
            Set<Integer> relevant,
            Set<ControlFlowGraph.Edge> backEdges) {

        Map<Integer, List<ControlFlowGraph.Edge>> dag = new HashMap<>();
        for (int node : relevant) {
            List<ControlFlowGraph.Edge> edges = new ArrayList<>();
            for (ControlFlowGraph.Edge edge : cfg.outgoing(node)) {
                if (relevant.contains(edge.getTo()) && !backEdges.contains(edge)) {
                    edges.add(edge);
                }
            }
            edges.sort(edgeComparator());
            dag.put(node, Collections.unmodifiableList(edges));
        }
        return dag;
    }

    private Matching maximumMatching(
            Set<Integer> relevant,
            Map<Integer, List<ControlFlowGraph.Edge>> dagEdges) {

        Matching matching = new Matching();
        for (int left : sorted(relevant)) {
            Set<Integer> seenRight = new HashSet<>();
            augment(left, seenRight, dagEdges, matching);
        }
        return matching;
    }

    private boolean augment(
            int left,
            Set<Integer> seenRight,
            Map<Integer, List<ControlFlowGraph.Edge>> dagEdges,
            Matching matching) {

        for (ControlFlowGraph.Edge edge : dagEdges.getOrDefault(left, Collections.emptyList())) {
            int right = edge.getTo();
            if (!seenRight.add(right)) {
                continue;
            }
            Integer previousLeft = matching.leftByRight.get(right);
            if (previousLeft == null || augment(previousLeft, seenRight, dagEdges, matching)) {
                ControlFlowGraph.Edge previousEdge = matching.edgeByLeft.get(left);
                if (previousEdge != null) {
                    matching.leftByRight.remove(previousEdge.getTo());
                }
                matching.edgeByLeft.put(left, edge);
                matching.leftByRight.put(right, left);
                return true;
            }
        }
        return false;
    }

    private List<List<Integer>> buildCoverChains(Set<Integer> relevant, Matching matching) {
        List<List<Integer>> chains = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (int node : sorted(relevant)) {
            if (matching.leftByRight.containsKey(node)) {
                continue;
            }
            List<Integer> chain = new ArrayList<>();
            int current = node;
            while (visited.add(current)) {
                chain.add(current);
                ControlFlowGraph.Edge next = matching.edgeByLeft.get(current);
                if (next == null) {
                    break;
                }
                current = next.getTo();
            }
            chains.add(chain);
        }
        for (int node : sorted(relevant)) {
            if (!visited.contains(node)) {
                chains.add(List.of(node));
            }
        }
        return chains;
    }

    private List<ControlFlowGraph.Edge> completePathForChain(
            ControlFlowGraph cfg,
            List<Integer> chain,
            int entryId,
            int exitId,
            Matching matching) {

        if (chain.isEmpty()) {
            return null;
        }

        List<ControlFlowGraph.Edge> result = new ArrayList<>();
        int first = chain.get(0);
        List<ControlFlowGraph.Edge> prefix = shortestPath(cfg, entryId, first);
        if (prefix == null) {
            return null;
        }
        result.addAll(prefix);

        for (int i = 0; i < chain.size() - 1; i++) {
            ControlFlowGraph.Edge edge = matching.edgeByLeft.get(chain.get(i));
            if (edge == null || edge.getTo() != chain.get(i + 1)) {
                return null;
            }
            result.add(edge);
        }

        int last = chain.get(chain.size() - 1);
        List<ControlFlowGraph.Edge> suffix = shortestPath(cfg, last, exitId);
        if (suffix == null) {
            return null;
        }
        result.addAll(suffix);
        return result;
    }

    private List<ControlFlowGraph.Edge> shortestCompletePathThroughTarget(
            ControlFlowGraph cfg,
            int target,
            CfgEdgeKind requiredExit) {

        int entryId = findNodeByKind(cfg, CfgNodeKind.ENTRY);
        int exitId = findNodeByKind(cfg, CfgNodeKind.EXIT);
        if (entryId == -1 || exitId == -1) {
            return null;
        }

        List<ControlFlowGraph.Edge> prefix = shortestPath(cfg, entryId, target);
        if (prefix == null) {
            return null;
        }

        List<ControlFlowGraph.Edge> suffix = requiredExit == null
                ? shortestPath(cfg, target, exitId)
                : pathViaRequiredExit(cfg, target, exitId, requiredExit);
        if (suffix == null) {
            return null;
        }

        List<ControlFlowGraph.Edge> result = new ArrayList<>(prefix.size() + suffix.size());
        result.addAll(prefix);
        result.addAll(suffix);
        return result;
    }

    private void addFallbackCandidate(
            List<List<ControlFlowGraph.Edge>> candidates,
            ControlFlowGraph cfg,
            int target,
            CfgEdgeKind requiredExit) {

        List<ControlFlowGraph.Edge> fallback =
                shortestCompletePathThroughTarget(cfg, target, requiredExit);
        if (fallback != null) {
            candidates.add(Collections.unmodifiableList(fallback));
        }
    }

    private int uncoveredScore(List<ControlFlowGraph.Edge> path, CoverageTracker tracker) {
        return coverageScore(path, tracker, tracker.getUncovered());
    }

    private int coveredScore(List<ControlFlowGraph.Edge> path, CoverageTracker tracker) {
        return coverageScore(path, tracker, tracker.getCovered());
    }

    private int coverageScore(
            List<ControlFlowGraph.Edge> path,
            CoverageTracker tracker,
            Set<Integer> coverageItems) {

        int score = 0;
        for (int item : coverageItems) {
            if (coversCoverageItem(path, tracker, item)) {
                score++;
            }
        }
        return score;
    }

    private List<List<ControlFlowGraph.Edge>> rankForUncoveredCoverage(
            List<List<ControlFlowGraph.Edge>> candidates,
            CoverageTracker tracker) {

        List<List<ControlFlowGraph.Edge>> useful = dedupeUsefulCandidates(candidates, tracker);
        List<List<ControlFlowGraph.Edge>> ranked = new ArrayList<>();
        Set<Integer> remainingUncovered = new HashSet<>(tracker.getUncovered());

        while (!remainingUncovered.isEmpty() && !useful.isEmpty()) {
            List<ControlFlowGraph.Edge> best = null;
            int bestGain = 0;
            for (List<ControlFlowGraph.Edge> candidate : useful) {
                int gain = coverageScore(candidate, tracker, remainingUncovered);
                if (gain > bestGain
                        || (gain == bestGain && best != null && comparePathPriority(candidate, best, tracker) < 0)) {
                    best = candidate;
                    bestGain = gain;
                }
            }
            if (best == null || bestGain == 0) {
                break;
            }
            ranked.add(best);
            useful.remove(best);
            removeCoveredItems(remainingUncovered, best, tracker);
        }

        useful.sort((left, right) -> comparePathPriority(left, right, tracker));
        ranked.addAll(useful);
        return ranked;
    }

    private List<List<ControlFlowGraph.Edge>> dedupeUsefulCandidates(
            List<List<ControlFlowGraph.Edge>> candidates,
            CoverageTracker tracker) {

        List<List<ControlFlowGraph.Edge>> useful = new ArrayList<>();
        Set<List<ControlFlowGraph.Edge>> seen = new HashSet<>();
        for (List<ControlFlowGraph.Edge> path : candidates) {
            if (uncoveredScore(path, tracker) == 0) {
                continue;
            }
            if (seen.add(path)) {
                useful.add(path);
            }
        }
        return useful;
    }

    private int comparePathPriority(
            List<ControlFlowGraph.Edge> left,
            List<ControlFlowGraph.Edge> right,
            CoverageTracker tracker) {

        int byUncovered = Integer.compare(uncoveredScore(right, tracker), uncoveredScore(left, tracker));
        if (byUncovered != 0) {
            return byUncovered;
        }
        int byCovered = Integer.compare(coveredScore(right, tracker), coveredScore(left, tracker));
        if (byCovered != 0) {
            return byCovered;
        }
        return Integer.compare(left.size(), right.size());
    }

    private void removeCoveredItems(
            Set<Integer> remaining,
            List<ControlFlowGraph.Edge> path,
            CoverageTracker tracker) {

        remaining.removeIf(item -> coversCoverageItem(path, tracker, item));
    }

    private boolean coversCoverageItem(
            List<ControlFlowGraph.Edge> path,
            CoverageTracker tracker,
            int item) {

        int target = tracker.pathTargetFor(item);
        CfgEdgeKind requiredExit = tracker.requiredExitFor(item);
        return pathContainsNode(path, target) && usesRequiredExit(path, target, requiredExit);
    }

    private List<List<ControlFlowGraph.Edge>> pruneRedundantPaths(
            List<List<ControlFlowGraph.Edge>> paths) {

        List<List<ControlFlowGraph.Edge>> candidates = new ArrayList<>(paths);
        candidates.sort(Comparator
                .<List<ControlFlowGraph.Edge>>comparingInt(path -> uniqueNodeCount(path))
                .reversed()
                .thenComparingInt(List::size));

        Set<Integer> covered = new HashSet<>();
        List<List<ControlFlowGraph.Edge>> kept = new ArrayList<>();
        for (List<ControlFlowGraph.Edge> path : candidates) {
            Set<Integer> nodes = pathNodes(path);
            if (!covered.containsAll(nodes)) {
                kept.add(path);
                covered.addAll(nodes);
            }
        }
        kept.sort(Comparator.comparingInt(List::size));
        return kept;
    }

    private int uniqueNodeCount(List<ControlFlowGraph.Edge> path) {
        return pathNodes(path).size();
    }

    private Set<Integer> pathNodes(List<ControlFlowGraph.Edge> path) {
        Set<Integer> nodes = new HashSet<>();
        for (ControlFlowGraph.Edge edge : path) {
            nodes.add(edge.getFrom());
            nodes.add(edge.getTo());
        }
        return nodes;
    }

    private List<ControlFlowGraph.Edge> pathViaRequiredExit(
            ControlFlowGraph cfg,
            int target,
            int exitId,
            CfgEdgeKind requiredExit) {

        for (ControlFlowGraph.Edge edge : sortedOutgoing(cfg, target, null)) {
            if (edge.getKind() == requiredExit) {
                List<ControlFlowGraph.Edge> rest = shortestPath(cfg, edge.getTo(), exitId);
                if (rest != null) {
                    List<ControlFlowGraph.Edge> result = new ArrayList<>(rest.size() + 1);
                    result.add(edge);
                    result.addAll(rest);
                    return result;
                }
            }
        }
        return null;
    }

    private List<ControlFlowGraph.Edge> shortestPath(ControlFlowGraph cfg, int start, int end) {
        if (start == end) {
            return Collections.emptyList();
        }

        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, ControlFlowGraph.Edge> edgeByNode = new HashMap<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int current = queue.remove();
            for (ControlFlowGraph.Edge edge : sortedOutgoing(cfg, current, null)) {
                int next = edge.getTo();
                if (!visited.add(next)) {
                    continue;
                }
                edgeByNode.put(next, edge);
                if (next == end) {
                    return reconstructPath(edgeByNode, start, end);
                }
                queue.add(next);
            }
        }
        return null;
    }

    private List<ControlFlowGraph.Edge> reconstructPath(
            Map<Integer, ControlFlowGraph.Edge> edgeByNode,
            int start,
            int end) {

        LinkedList<ControlFlowGraph.Edge> path = new LinkedList<>();
        int current = end;
        while (current != start) {
            ControlFlowGraph.Edge edge = edgeByNode.get(current);
            if (edge == null) {
                return null;
            }
            path.addFirst(edge);
            current = edge.getFrom();
        }
        return new ArrayList<>(path);
    }

    private boolean pathContainsNode(List<ControlFlowGraph.Edge> path, int target) {
        for (ControlFlowGraph.Edge edge : path) {
            if (edge.getFrom() == target || edge.getTo() == target) {
                return true;
            }
        }
        return false;
    }

    private boolean usesRequiredExit(
            List<ControlFlowGraph.Edge> path,
            int target,
            CfgEdgeKind requiredExit) {

        if (requiredExit == null) {
            return true;
        }
        for (ControlFlowGraph.Edge edge : path) {
            if (edge.getFrom() == target && edge.getKind() == requiredExit) {
                return true;
            }
        }
        return false;
    }

    private List<ControlFlowGraph.Edge> sortedOutgoing(
            ControlFlowGraph cfg,
            int node,
            Set<Integer> allowedNodes) {

        List<ControlFlowGraph.Edge> edges = new ArrayList<>();
        for (ControlFlowGraph.Edge edge : cfg.outgoing(node)) {
            if (allowedNodes == null || allowedNodes.contains(edge.getTo())) {
                edges.add(edge);
            }
        }
        edges.sort(edgeComparator());
        return edges;
    }

    private Comparator<ControlFlowGraph.Edge> edgeComparator() {
        return Comparator
                .comparingInt(ControlFlowGraph.Edge::getTo)
                .thenComparing(edge -> edge.getKind().ordinal())
                .thenComparingInt(ControlFlowGraph.Edge::getFrom);
    }

    private List<Integer> sorted(Collection<Integer> nodes) {
        List<Integer> sorted = new ArrayList<>(nodes);
        Collections.sort(sorted);
        return sorted;
    }

    private int findNodeByKind(ControlFlowGraph cfg, CfgNodeKind kind) {
        for (int id : sorted(cfg.getNodes())) {
            ControlFlowGraph.Node node = cfg.getNode(id);
            if (node != null && node.getKind() == kind) {
                return id;
            }
        }
        return -1;
    }

    private enum VisitColor {
        WHITE,
        GRAY,
        BLACK
    }

    private static final class TarjanState {
        int nextIndex = 0;
        int nextSccId = 0;
        final Map<Integer, Integer> indexByNode = new HashMap<>();
        final Map<Integer, Integer> lowLinkByNode = new HashMap<>();
        final Deque<Integer> stack = new ArrayDeque<>();
        final Set<Integer> onStack = new HashSet<>();
        final Map<Integer, Integer> sccByNode = new HashMap<>();
    }

    private static final class Matching {
        final Map<Integer, ControlFlowGraph.Edge> edgeByLeft = new HashMap<>();
        final Map<Integer, Integer> leftByRight = new HashMap<>();
    }
}
