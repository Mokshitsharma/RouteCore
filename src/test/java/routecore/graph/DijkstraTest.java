package routecore.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {

    private static Graph diamond() {
        // A --1--> B --2--> D        A->C->D costs 4+1 = 5
        // A --4--> C --1--> D        A->B->D costs 1+2 = 3  (shortest)
        Graph g = new Graph();
        for (String id : List.of("A", "B", "C", "D")) g.addNode(id, 0, 0);
        g.addEdge("A", "B", 1);
        g.addEdge("B", "D", 2);
        g.addEdge("A", "C", 4);
        g.addEdge("C", "D", 1);
        return g;
    }

    @Test
    void findsShortestPathAndCost() {
        Path p = Dijkstra.shortestPath(diamond(), "A", "D");
        assertTrue(p.isReachable());
        assertEquals(List.of("A", "B", "D"), p.nodes());
        assertEquals(3.0, p.cost(), 1e-9);
        assertEquals(2, p.hops());
    }

    @Test
    void sourceEqualsDest() {
        Path p = Dijkstra.shortestPath(diamond(), "A", "A");
        assertEquals(List.of("A"), p.nodes());
        assertEquals(0.0, p.cost(), 1e-9);
    }

    @Test
    void unreachableReported() {
        Graph g = new Graph();
        g.addNode("X", 0, 0);
        g.addNode("Y", 1, 1);
        Path p = Dijkstra.shortestPath(g, "X", "Y");
        assertFalse(p.isReachable());
        assertEquals(Double.POSITIVE_INFINITY, p.cost());
    }

    @Test
    void unknownNodeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.shortestPath(diamond(), "A", "Z"));
    }

    @Test
    void prefersFewerHopsOnlyIfCheaper() {
        Graph g = new Graph();
        for (String id : List.of("A", "B", "C")) g.addNode(id, 0, 0);
        g.addEdge("A", "C", 10);          // 1 hop, cost 10
        g.addEdge("A", "B", 1);
        g.addEdge("B", "C", 1);           // 2 hops, cost 2  -> should win
        Path p = Dijkstra.shortestPath(g, "A", "C");
        assertEquals(List.of("A", "B", "C"), p.nodes());
        assertEquals(2.0, p.cost(), 1e-9);
    }

    @Test
    void matchesReferenceDijkstraOnRandomGraphs() {
        Random rnd = new Random(31);
        for (int trial = 0; trial < 40; trial++) {
            int n = 5 + rnd.nextInt(60);
            Graph g = new Graph();
            for (int i = 0; i < n; i++) g.addNode("n" + i, rnd.nextDouble(), rnd.nextDouble());
            Map<String, List<double[]>> ref = new HashMap<>();
            for (int i = 0; i < n; i++) ref.put("n" + i, new ArrayList<>());
            int edges = n * (2 + rnd.nextInt(4));
            for (int e = 0; e < edges; e++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a == b) continue;
                double w = 1 + rnd.nextInt(100);
                g.addEdge("n" + a, "n" + b, w);
                ref.get("n" + a).add(new double[]{b, w});
            }
            String src = "n" + rnd.nextInt(n);
            String dst = "n" + rnd.nextInt(n);

            double expected = refDijkstra(ref, n, Integer.parseInt(src.substring(1)),
                    Integer.parseInt(dst.substring(1)));
            Path got = Dijkstra.shortestPath(g, src, dst);

            if (Double.isInfinite(expected)) {
                assertFalse(got.isReachable());
            } else {
                assertEquals(expected, got.cost(), 1e-6);
                assertEquals(src, got.nodes().get(0));
                assertEquals(dst, got.nodes().get(got.nodes().size() - 1));
                assertEquals(expected, edgeSum(g, got), 1e-6, "reported cost must equal the path's real cost");
            }
        }
    }

    /** Reference shortest path with java.util.PriorityQueue (allowed in tests). */
    private static double refDijkstra(Map<String, List<double[]>> adj, int n, int s, int t) {
        double[] dist = new double[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[s] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> dist[a[0]]));
        pq.add(new int[]{s});
        boolean[] done = new boolean[n];
        while (!pq.isEmpty()) {
            int u = pq.poll()[0];
            if (done[u]) continue;
            done[u] = true;
            for (double[] edge : adj.get("n" + u)) {
                int v = (int) edge[0];
                if (dist[u] + edge[1] < dist[v]) {
                    dist[v] = dist[u] + edge[1];
                    pq.add(new int[]{v});
                }
            }
        }
        return dist[t];
    }

    private static double edgeSum(Graph g, Path p) {
        double sum = 0;
        for (int i = 0; i + 1 < p.nodes().size(); i++) {
            String a = p.nodes().get(i), b = p.nodes().get(i + 1);
            double best = Double.POSITIVE_INFINITY;
            for (Edge e : g.getNeighbors(a)) if (e.to.equals(b)) best = Math.min(best, e.weight);
            sum += best;
        }
        return sum;
    }
}
