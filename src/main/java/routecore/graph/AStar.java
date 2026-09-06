package routecore.graph;

import routecore.datastructures.CustomHashMap;
import routecore.datastructures.MinHeap;

/**
 * A* search: Dijkstra with a sense of direction.
 *
 * <p>Dijkstra orders its frontier by {@code g(n)} — the cost paid so far. A* orders
 * it by {@code f(n) = g(n) + h(n)}, where {@code h(n)} estimates the cost still to
 * go (see {@link Heuristic}). With an admissible heuristic the path returned is
 * still optimal, but the search fans out toward the goal instead of expanding in
 * every direction, so it settles far fewer nodes on a road-shaped graph.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Same frontier machinery as Dijkstra</b> (our {@link MinHeap} with
 *       {@code decreaseKey}); only the priority key changes, from {@code g} to
 *       {@code g + h}.</li>
 *   <li><b>h must not overestimate</b>: {@link Heuristic#EUCLIDEAN} /
 *       {@link Heuristic#HAVERSINE} are admissible when edge weights are true
 *       distances. {@link Heuristic#ZERO} degenerates A* back into Dijkstra — a
 *       useful equivalence check in tests.</li>
 *   <li><b>Speedup is heuristic-dependent</b>: a perfect heuristic expands only the
 *       nodes on the optimal path; a weak one saves little. The benchmark quantifies
 *       this as an expansion-count ratio.</li>
 * </ul>
 *
 * <p>Complexity: O((V + E) log V) worst case (no better than Dijkstra in the limit),
 * but typically expands O(path length) nodes with a good heuristic. Space O(V).
 */
public final class AStar {

    private AStar() {
    }

    public static Path shortestPath(Graph graph, String sourceId, String destId, Heuristic heuristic) {
        return search(graph, sourceId, destId, heuristic).path;
    }

    public static SearchResult search(Graph graph, String sourceId, String destId, Heuristic heuristic) {
        if (!graph.hasNode(sourceId)) throw new IllegalArgumentException("Unknown source: " + sourceId);
        if (!graph.hasNode(destId)) throw new IllegalArgumentException("Unknown dest: " + destId);

        Node goal = graph.getNode(destId);
        CustomHashMap<String, Double> g = new CustomHashMap<>();
        CustomHashMap<String, String> prev = new CustomHashMap<>();
        CustomHashMap<String, Boolean> settled = new CustomHashMap<>();
        MinHeap<String> frontier = new MinHeap<>();

        g.put(sourceId, 0.0);
        frontier.insert(sourceId, heuristic.estimate(graph.getNode(sourceId), goal));
        int expanded = 0;

        while (!frontier.isEmpty()) {
            String u = frontier.extractMin();
            if (Boolean.TRUE.equals(settled.get(u))) continue;
            settled.put(u, Boolean.TRUE);
            expanded++;

            if (u.equals(destId)) {
                return new SearchResult(Dijkstra.reconstruct(prev, sourceId, destId, g.get(destId)), expanded);
            }

            double gu = g.get(u);
            for (Edge e : graph.getNeighbors(u)) {
                if (Boolean.TRUE.equals(settled.get(e.to))) continue;
                double tentativeG = gu + e.weight;
                if (tentativeG < g.getOrDefault(e.to, Double.POSITIVE_INFINITY)) {
                    g.put(e.to, tentativeG);
                    prev.put(e.to, u);
                    double f = tentativeG + heuristic.estimate(graph.getNode(e.to), goal);
                    frontier.insertOrDecrease(e.to, f);
                }
            }
        }
        return new SearchResult(Path.unreachable(), expanded);
    }
}
