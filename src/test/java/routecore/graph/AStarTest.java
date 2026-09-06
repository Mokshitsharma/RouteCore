package routecore.graph;

import org.junit.jupiter.api.Test;
import routecore.bench.BenchmarkRunner;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AStarTest {

    @Test
    void zeroHeuristicEqualsDijkstra() {
        Graph g = BenchmarkRunner.grid(15);
        Random rnd = new Random(3);
        for (int i = 0; i < 30; i++) {
            String s = rnd.nextInt(15) + "_" + rnd.nextInt(15);
            String t = rnd.nextInt(15) + "_" + rnd.nextInt(15);
            Path a = AStar.shortestPath(g, s, t, Heuristic.ZERO);
            Path d = Dijkstra.shortestPath(g, s, t);
            assertEquals(d.cost(), a.cost(), 1e-9);
        }
    }

    @Test
    void euclideanHeuristicStillOptimal() {
        Graph g = BenchmarkRunner.grid(25);
        Random rnd = new Random(8);
        for (int i = 0; i < 40; i++) {
            String s = rnd.nextInt(25) + "_" + rnd.nextInt(25);
            String t = rnd.nextInt(25) + "_" + rnd.nextInt(25);
            Path a = AStar.shortestPath(g, s, t, Heuristic.EUCLIDEAN);
            Path d = Dijkstra.shortestPath(g, s, t);
            assertEquals(d.cost(), a.cost(), 1e-9, "A* must return an optimal-cost path");
        }
    }

    @Test
    void aStarExpandsNoMoreThanDijkstra() {
        Graph g = BenchmarkRunner.grid(60);
        SearchResult d = Dijkstra.search(g, "0_0", "59_59");
        SearchResult a = AStar.search(g, "0_0", "59_59", Heuristic.EUCLIDEAN);
        assertEquals(d.path.cost(), a.path.cost(), 1e-9);
        assertTrue(a.nodesExpanded <= d.nodesExpanded,
                "A* expanded " + a.nodesExpanded + " vs Dijkstra " + d.nodesExpanded);
        // On a big open grid the heuristic should give a real (not marginal) saving.
        assertTrue(a.nodesExpanded < d.nodesExpanded * 0.9);
    }

    @Test
    void findsKnownPathOnSampleShape() {
        // small weighted graph where the geometric shortcut matters
        Graph g = new Graph();
        g.addNode("S", 0, 0);
        g.addNode("A", 1, 2);
        g.addNode("B", 2, 0);
        g.addNode("T", 3, 0);
        g.addEdge("S", "A", 3, true);
        g.addEdge("A", "T", 3, true);
        g.addEdge("S", "B", 2, true);
        g.addEdge("B", "T", 1, true);
        Path p = AStar.shortestPath(g, "S", "T", Heuristic.EUCLIDEAN);
        assertEquals(List.of("S", "B", "T"), p.nodes());
        assertEquals(3.0, p.cost(), 1e-9);
    }

    @Test
    void unreachableReported() {
        Graph g = new Graph();
        g.addNode("A", 0, 0);
        g.addNode("B", 5, 5);
        assertFalse(AStar.shortestPath(g, "A", "B", Heuristic.EUCLIDEAN).isReachable());
    }
}
