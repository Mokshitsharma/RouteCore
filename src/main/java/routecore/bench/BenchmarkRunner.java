package routecore.bench;

import routecore.datastructures.KDTree;
import routecore.graph.AStar;
import routecore.graph.Dijkstra;
import routecore.graph.Graph;
import routecore.graph.Heuristic;
import routecore.graph.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Concrete evidence that the hand-built structures beat the naive baselines.
 * Prints plain tables to stdout — copy/paste straight into interview notes.
 *
 * <ol>
 *   <li><b>KD-tree nearest-neighbour vs brute-force linear scan</b> at N = 100 /
 *       1 000 / 10 000 / 100 000. Both are checked to return the same answer; the
 *       table reports the speedup.</li>
 *   <li><b>A* vs Dijkstra</b> on a grid graph: same optimal path length, but A*
 *       with a Euclidean heuristic settles far fewer nodes.</li>
 * </ol>
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        String which = args.length > 0 ? args[0] : "all";
        if (which.equals("all") || which.equals("kdtree")) benchKdTree();
        if (which.equals("all") || which.equals("astar")) benchAStarVsDijkstra();
    }

    // --- 1. KD-tree vs brute force ----------------------------------------

    public static void benchKdTree() {
        System.out.println("== KD-tree nearest-neighbour vs brute-force linear scan ==");
        System.out.printf("%10s | %12s | %12s | %9s | %s%n",
                "N", "brute (ms)", "kd-tree (ms)", "speedup", "answers match");
        int queries = 2_000;
        long seed = 42;
        for (int n : new int[]{100, 1_000, 10_000, 100_000}) {
            Random rnd = new Random(seed);
            List<KDTree.Point> pts = new ArrayList<>(n);
            for (int k = 0; k < n; k++) {
                pts.add(new KDTree.Point("p" + k, rnd.nextDouble() * 1_000, rnd.nextDouble() * 1_000));
            }
            KDTree tree = KDTree.build(pts);

            double[][] qs = new double[queries][2];
            for (int q = 0; q < queries; q++) {
                qs[q][0] = rnd.nextDouble() * 1_000;
                qs[q][1] = rnd.nextDouble() * 1_000;
            }

            boolean match = true;
            long t0 = System.nanoTime();
            String[] bruteAns = new String[queries];
            for (int q = 0; q < queries; q++) {
                bruteAns[q] = bruteNearest(pts, qs[q][0], qs[q][1]).id;
            }
            long bruteNs = System.nanoTime() - t0;

            t0 = System.nanoTime();
            for (int q = 0; q < queries; q++) {
                String kd = tree.nearestNeighbor(qs[q][0], qs[q][1]).id;
                if (!kd.equals(bruteAns[q])) match = false;
            }
            long kdNs = System.nanoTime() - t0;

            System.out.printf("%10d | %12.2f | %12.2f | %8.1fx | %s%n",
                    n, bruteNs / 1e6, kdNs / 1e6, (double) bruteNs / kdNs, match ? "yes" : "NO");
        }
        System.out.println();
    }

    private static KDTree.Point bruteNearest(List<KDTree.Point> pts, double qx, double qy) {
        KDTree.Point best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (KDTree.Point p : pts) {
            double d = p.dist2(qx, qy);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    // --- 2. A* vs Dijkstra ----------------------------------------------

    public static void benchAStarVsDijkstra() {
        System.out.println("== A* (Euclidean heuristic) vs Dijkstra on a grid graph ==");
        System.out.printf("%8s | %10s | %14s | %14s | %9s | %s%n",
                "grid", "nodes", "Dijkstra exp.", "A* expanded", "less work", "same cost");
        for (int side : new int[]{20, 50, 100, 200}) {
            Graph g = grid(side);
            String src = "0_0";
            String dst = (side - 1) + "_" + (side - 1);

            SearchResult d = Dijkstra.search(g, src, dst);
            SearchResult a = AStar.search(g, src, dst, Heuristic.EUCLIDEAN);

            boolean sameCost = Math.abs(d.path.cost() - a.path.cost()) < 1e-6;
            System.out.printf("%8s | %10d | %14d | %14d | %8.1fx | %s%n",
                    side + "x" + side, g.nodeCount(), d.nodesExpanded, a.nodesExpanded,
                    (double) d.nodesExpanded / a.nodesExpanded, sameCost ? "yes" : "NO");
        }
        System.out.println();
    }

    /**
     * side x side lattice, node ids "row_col", 8-connected with <b>true Euclidean
     * edge weights</b> (1 for orthogonal, sqrt(2) for diagonal).
     *
     * <p>The weights matter for the A* demo: when the graph metric really is
     * geometric distance, the straight-line heuristic is both admissible and a
     * tight estimate, so A* prunes hard. On a 4-connected unit grid the true metric
     * is Manhattan, the Euclidean heuristic barely prunes, and A* ~= Dijkstra --
     * which would make the benchmark misleading.
     */
    public static Graph grid(int side) {
        Graph g = new Graph();
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                g.addNode(r + "_" + c, c, r);
            }
        }
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int r = 0; r < side; r++) {
            for (int c = 0; c < side; c++) {
                for (int[] d : dirs) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr < 0 || nr >= side || nc < 0 || nc >= side) continue;
                    double w = (d[0] != 0 && d[1] != 0) ? Math.sqrt(2) : 1.0;
                    g.addEdge(r + "_" + c, nr + "_" + nc, w, true);
                }
            }
        }
        return g;
    }
}
