package routecore.graph;

import routecore.datastructures.CustomHashMap;
import routecore.datastructures.MinHeap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Dijkstra's single-source shortest-path algorithm, built on our own
 * {@link MinHeap} and {@link CustomHashMap}.
 *
 * <p>Algorithm: keep a tentative distance for every node (+Inf initially, 0 for the
 * source). Repeatedly settle the closest unsettled node, then relax its outgoing
 * edges — if going "through" it reaches a neighbour more cheaply, lower that
 * neighbour's tentative distance (a {@code decreaseKey} on the frontier heap).
 * Because edge weights are non-negative, the first time a node is settled its
 * distance is final.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Binary-heap frontier</b>: gives O((V + E) log V). The textbook array
 *       version is O(V^2 + E) — fine for dense graphs, wasteful for sparse road
 *       networks.</li>
 *   <li><b>Real {@code decreaseKey}</b> (not "insert a duplicate and skip stale
 *       pops"): keeps the heap at O(V) entries. The lazy-deletion variant is simpler
 *       but can grow the heap to O(E) and needs a "already settled?" guard on pop.</li>
 *   <li><b>Early exit</b>: for a point-to-point query we stop the moment the
 *       destination is settled — no need to finish the whole SSSP tree.</li>
 * </ul>
 *
 * <p>Complexity: O((V + E) log V) time, O(V) space.
 */
public final class Dijkstra {

    private Dijkstra() {
    }

    /** @return the shortest path from {@code sourceId} to {@code destId}, or {@link Path#unreachable()}. */
    public static Path shortestPath(Graph graph, String sourceId, String destId) {
        return search(graph, sourceId, destId).path;
    }

    /** Same as {@link #shortestPath} but also reports how many nodes were expanded. */
    public static SearchResult search(Graph graph, String sourceId, String destId) {
        if (!graph.hasNode(sourceId)) throw new IllegalArgumentException("Unknown source: " + sourceId);
        if (!graph.hasNode(destId)) throw new IllegalArgumentException("Unknown dest: " + destId);

        CustomHashMap<String, Double> dist = new CustomHashMap<>();
        CustomHashMap<String, String> prev = new CustomHashMap<>();
        CustomHashMap<String, Boolean> settled = new CustomHashMap<>();
        MinHeap<String> frontier = new MinHeap<>();

        dist.put(sourceId, 0.0);
        frontier.insert(sourceId, 0.0);
        int expanded = 0;

        while (!frontier.isEmpty()) {
            String u = frontier.extractMin();
            settled.put(u, Boolean.TRUE);
            expanded++;

            if (u.equals(destId)) {
                return new SearchResult(reconstruct(prev, sourceId, destId, dist.get(destId)), expanded);
            }

            double du = dist.get(u);
            for (Edge e : graph.getNeighbors(u)) {
                if (Boolean.TRUE.equals(settled.get(e.to))) continue;
                double candidate = du + e.weight;
                double known = dist.getOrDefault(e.to, Double.POSITIVE_INFINITY);
                if (candidate < known) {
                    dist.put(e.to, candidate);
                    prev.put(e.to, u);
                    frontier.insertOrDecrease(e.to, candidate);
                }
            }
        }
        return new SearchResult(Path.unreachable(), expanded);
    }

    /**
     * Full single-source shortest-path tree: distance from {@code sourceId} to every
     * reachable node. Handy for building all-pairs distance matrices for the route
     * optimizer. O((V + E) log V).
     */
    public static CustomHashMap<String, Double> distancesFrom(Graph graph, String sourceId) {
        if (!graph.hasNode(sourceId)) throw new IllegalArgumentException("Unknown source: " + sourceId);
        CustomHashMap<String, Double> dist = new CustomHashMap<>();
        CustomHashMap<String, Boolean> settled = new CustomHashMap<>();
        MinHeap<String> frontier = new MinHeap<>();
        dist.put(sourceId, 0.0);
        frontier.insert(sourceId, 0.0);

        while (!frontier.isEmpty()) {
            String u = frontier.extractMin();
            if (Boolean.TRUE.equals(settled.get(u))) continue;
            settled.put(u, Boolean.TRUE);
            double du = dist.get(u);
            for (Edge e : graph.getNeighbors(u)) {
                double candidate = du + e.weight;
                if (candidate < dist.getOrDefault(e.to, Double.POSITIVE_INFINITY)) {
                    dist.put(e.to, candidate);
                    frontier.insertOrDecrease(e.to, candidate);
                }
            }
        }
        return dist;
    }

    static Path reconstruct(CustomHashMap<String, String> prev, String source, String dest, double cost) {
        Deque<String> stack = new ArrayDeque<>();
        String cur = dest;
        stack.push(cur);
        while (!cur.equals(source)) {
            cur = prev.get(cur);
            if (cur == null) return Path.unreachable();
            stack.push(cur);
        }
        List<String> nodes = new ArrayList<>(stack);
        return new Path(nodes, cost);
    }
}
