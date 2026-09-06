package routecore.routing;

import routecore.datastructures.CustomHashMap;
import routecore.graph.Dijkstra;
import routecore.graph.Graph;
import routecore.graph.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-vehicle route construction and improvement — the classic TSP/VRP-lite
 * heuristic pair.
 *
 * <h2>1. Nearest-neighbour construction</h2>
 * Start at the depot; repeatedly hop to the closest not-yet-visited stop; return to
 * the depot. Greedy and fast — O(n^2) distance lookups for n stops — but it can be
 * ~25% worse than optimal because early greedy choices strand far-away stops that
 * must be mopped up with long legs at the end.
 *
 * <h2>2. 2-opt improvement (local search)</h2>
 * Repeatedly pick two edges of the tour, delete them, and reconnect the tour the
 * only other way possible — which reverses the segment between them. Keep the swap
 * whenever it shortens the tour; stop when a full pass finds no improving swap
 * (a 2-opt local optimum). Each pass is O(n^2); the number of passes is small in
 * practice. 2-opt removes route "crossings" and typically recovers most of the gap
 * to optimal.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Why heuristics at all?</b> Exact TSP is NP-hard; for n beyond ~15 the
 *       exact DP (Held-Karp, O(n^2 2^n)) is infeasible. Construction + local search
 *       gives a good tour in polynomial time.</li>
 *   <li><b>Distance abstraction</b>: the optimizer never touches the graph directly.
 *       Feed it a {@link Route.DistanceFn}. {@link #dijkstraDistance(Graph)} builds
 *       one from true road-network shortest paths (with memoised single-source
 *       trees); {@link #straightLineDistance(Graph)} uses Euclidean coords.</li>
 *   <li><b>First-improvement vs best-improvement</b>: this uses first-improvement
 *       (apply the first shortening swap found) — fewer distance evaluations per
 *       pass, usually similar final quality.</li>
 * </ul>
 */
public final class RouteOptimizer {

    private RouteOptimizer() {
    }

    /** Greedy nearest-neighbour tour: depot -> ... -> depot. O(n^2) distance calls. */
    public static Route nearestNeighborRoute(String depot, List<String> stops, Route.DistanceFn dist) {
        List<String> remaining = new ArrayList<>(stops);
        List<String> order = new ArrayList<>();
        order.add(depot);
        String current = depot;
        while (!remaining.isEmpty()) {
            String best = null;
            double bestD = Double.POSITIVE_INFINITY;
            for (String s : remaining) {
                double d = dist.distance(current, s);
                if (d < bestD) {
                    bestD = d;
                    best = s;
                }
            }
            order.add(best);
            remaining.remove(best);
            current = best;
        }
        order.add(depot);
        return new Route(order);
    }

    /**
     * 2-opt local search until no improving swap remains (or {@code maxPasses} is hit).
     * Returns a new {@link Route}; the input is not mutated.
     */
    public static Route twoOptImprove(Route route, Route.DistanceFn dist, int maxPasses) {
        List<String> best = new ArrayList<>(route.order());
        int n = best.size();
        boolean improved = true;
        int passes = 0;
        while (improved && passes < maxPasses) {
            improved = false;
            passes++;
            // Endpoints (0 and n-1) are the depot and stay fixed.
            for (int i = 1; i < n - 2; i++) {
                for (int k = i + 1; k < n - 1; k++) {
                    double before = dist.distance(best.get(i - 1), best.get(i))
                            + dist.distance(best.get(k), best.get(k + 1));
                    double after = dist.distance(best.get(i - 1), best.get(k))
                            + dist.distance(best.get(i), best.get(k + 1));
                    if (after + 1e-9 < before) {
                        reverse(best, i, k);
                        improved = true;
                    }
                }
            }
        }
        return new Route(best);
    }

    public static Route twoOptImprove(Route route, Route.DistanceFn dist) {
        return twoOptImprove(route, dist, 100);
    }

    /** Convenience: build a NN route then 2-opt it. */
    public static Route optimize(String depot, List<String> stops, Route.DistanceFn dist) {
        return twoOptImprove(nearestNeighborRoute(depot, stops, dist), dist);
    }

    private static void reverse(List<String> list, int i, int k) {
        while (i < k) {
            String tmp = list.get(i);
            list.set(i, list.get(k));
            list.set(k, tmp);
            i++;
            k--;
        }
    }

    // --- distance functions --------------------------------------------------

    /** Euclidean distance between node coordinates. O(1) per call. */
    public static Route.DistanceFn straightLineDistance(Graph graph) {
        return (a, b) -> {
            Node na = graph.getNode(a);
            Node nb = graph.getNode(b);
            double dx = na.x - nb.x;
            double dy = na.y - nb.y;
            return Math.sqrt(dx * dx + dy * dy);
        };
    }

    /**
     * True road-network distance via Dijkstra, memoised per source: the first query
     * from a node computes its whole shortest-path tree (O((V+E) log V)); later
     * queries from that node are O(1) lookups. Unreachable pairs return +Inf.
     */
    public static Route.DistanceFn dijkstraDistance(Graph graph) {
        CustomHashMap<String, CustomHashMap<String, Double>> cache = new CustomHashMap<>();
        return (a, b) -> {
            CustomHashMap<String, Double> tree = cache.get(a);
            if (tree == null) {
                tree = Dijkstra.distancesFrom(graph, a);
                cache.put(a, tree);
            }
            return tree.getOrDefault(b, Double.POSITIVE_INFINITY);
        };
    }
}
