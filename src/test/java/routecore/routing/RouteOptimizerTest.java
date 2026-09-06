package routecore.routing;

import org.junit.jupiter.api.Test;
import routecore.graph.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizerTest {

    /** Graph of isolated nodes at given coordinates; distance = straight line. */
    private static Graph points(double[][] xy) {
        Graph g = new Graph();
        for (int i = 0; i < xy.length; i++) g.addNode("n" + i, xy[i][0], xy[i][1]);
        return g;
    }

    @Test
    void nearestNeighborRouteVisitsEveryStopOnceAndClosesLoop() {
        Graph g = points(new double[][]{{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}});
        Route r = RouteOptimizer.nearestNeighborRoute("n0",
                List.of("n1", "n2", "n3", "n4"), RouteOptimizer.straightLineDistance(g));
        assertEquals("n0", r.order().get(0));
        assertEquals("n0", r.order().get(r.order().size() - 1));
        assertEquals(List.of("n1", "n2", "n3", "n4"), r.stops());
        // collinear points: NN already optimal, out and back = 8
        assertEquals(8.0, r.totalDistance(RouteOptimizer.straightLineDistance(g)), 1e-9);
    }

    @Test
    void twoOptRemovesACrossing() {
        // Square corners; the given order self-intersects.
        Graph g = points(new double[][]{{0, 0}, {10, 0}, {0, 10}, {10, 10}});
        Route.DistanceFn d = RouteOptimizer.straightLineDistance(g);
        Route crossing = new Route(List.of("n0", "n1", "n2", "n3", "n0"));
        double before = crossing.totalDistance(d);
        Route fixed = RouteOptimizer.twoOptImprove(crossing, d);
        double after = fixed.totalDistance(d);
        assertTrue(after < before);
        assertEquals(40.0, after, 1e-6); // perimeter of the 10x10 square
    }

    @Test
    void twoOptNeverWorsensTheRoute() {
        Random rnd = new Random(17);
        for (int trial = 0; trial < 50; trial++) {
            int n = 6 + rnd.nextInt(12);
            double[][] xy = new double[n][2];
            for (int i = 0; i < n; i++) {
                xy[i] = new double[]{rnd.nextDouble() * 100, rnd.nextDouble() * 100};
            }
            Graph g = points(xy);
            Route.DistanceFn d = RouteOptimizer.straightLineDistance(g);
            List<String> stops = new ArrayList<>();
            for (int i = 1; i < n; i++) stops.add("n" + i);

            Route nn = RouteOptimizer.nearestNeighborRoute("n0", stops, d);
            Route opt = RouteOptimizer.twoOptImprove(nn, d);
            assertTrue(opt.totalDistance(d) <= nn.totalDistance(d) + 1e-9);
            assertEquals(nn.stops().size(), opt.stops().size());
        }
    }

    @Test
    void optimizeGetsCloseToBruteForceOptimalForSmallInstances() {
        Random rnd = new Random(4);
        for (int trial = 0; trial < 20; trial++) {
            int n = 7; // depot + 6 stops -> 720 permutations to brute force
            double[][] xy = new double[n][2];
            for (int i = 0; i < n; i++) {
                xy[i] = new double[]{rnd.nextDouble() * 50, rnd.nextDouble() * 50};
            }
            Graph g = points(xy);
            Route.DistanceFn d = RouteOptimizer.straightLineDistance(g);
            List<String> stops = new ArrayList<>();
            for (int i = 1; i < n; i++) stops.add("n" + i);

            double heuristic = RouteOptimizer.optimize("n0", stops, d).totalDistance(d);
            double optimal = bruteForceOptimal("n0", stops, d);

            assertTrue(heuristic >= optimal - 1e-6);
            assertTrue(heuristic <= optimal * 1.25 + 1e-6,
                    "2-opt tour " + heuristic + " too far from optimal " + optimal);
        }
    }

    private static double bruteForceOptimal(String depot, List<String> stops, Route.DistanceFn d) {
        List<String> perm = new ArrayList<>(stops);
        return permute(depot, perm, 0, d, Double.POSITIVE_INFINITY);
    }

    private static double permute(String depot, List<String> a, int k, Route.DistanceFn d, double best) {
        if (k == a.size()) {
            List<String> order = new ArrayList<>();
            order.add(depot);
            order.addAll(a);
            order.add(depot);
            return Math.min(best, new Route(order).totalDistance(d));
        }
        for (int i = k; i < a.size(); i++) {
            Collections.swap(a, k, i);
            best = permute(depot, a, k + 1, d, best);
            Collections.swap(a, k, i);
        }
        return best;
    }
}
