package routecore;

import routecore.bench.BenchmarkRunner;
import routecore.datastructures.KDTree;
import routecore.graph.AStar;
import routecore.graph.Dijkstra;
import routecore.graph.Graph;
import routecore.graph.Heuristic;
import routecore.graph.Node;
import routecore.graph.SearchResult;
import routecore.io.GraphLoader;
import routecore.routing.Job;
import routecore.routing.JobScheduler;
import routecore.routing.Route;
import routecore.routing.RouteOptimizer;
import routecore.routing.Vehicle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * One CLI entry point that can demo any module standalone.
 *
 * <pre>
 *   dijkstra  &lt;graph.json&gt; &lt;src&gt; &lt;dst&gt;
 *   astar     &lt;graph.json&gt; &lt;src&gt; &lt;dst&gt; [euclidean|haversine|zero]
 *   nn        &lt;graph.json&gt; &lt;x&gt; &lt;y&gt; [k]
 *   route     &lt;graph.json&gt; &lt;depot&gt; &lt;stop,stop,...&gt; [straight|road]
 *   schedule  &lt;graph.json&gt; &lt;jobs.csv&gt; &lt;vehicleNode,vehicleNode,...&gt;
 *   bench     [all|kdtree|astar]
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        String cmd = args[0].toLowerCase();
        switch (cmd) {
            case "dijkstra" -> dijkstra(args);
            case "astar" -> astar(args);
            case "nn" -> nn(args);
            case "route" -> route(args);
            case "schedule" -> schedule(args);
            case "bench" -> BenchmarkRunner.main(slice(args, 1));
            default -> {
                System.err.println("Unknown command: " + cmd);
                usage();
            }
        }
    }

    private static void dijkstra(String[] a) throws Exception {
        require(a, 4, "dijkstra <graph.json> <src> <dst>");
        Graph g = GraphLoader.fromJson(Path.of(a[1]));
        SearchResult r = Dijkstra.search(g, a[2], a[3]);
        System.out.println(r.path);
        System.out.println("nodes expanded: " + r.nodesExpanded);
    }

    private static void astar(String[] a) throws Exception {
        require(a, 4, "astar <graph.json> <src> <dst> [euclidean|haversine|zero]");
        Graph g = GraphLoader.fromJson(Path.of(a[1]));
        Heuristic h = switch (a.length > 4 ? a[4].toLowerCase() : "euclidean") {
            case "haversine" -> Heuristic.HAVERSINE;
            case "zero" -> Heuristic.ZERO;
            default -> Heuristic.EUCLIDEAN;
        };
        SearchResult r = AStar.search(g, a[2], a[3], h);
        System.out.println(r.path);
        System.out.println("nodes expanded: " + r.nodesExpanded);

        SearchResult d = Dijkstra.search(g, a[2], a[3]);
        System.out.printf("(Dijkstra expanded %d for the same cost %.3f -> %.1fx less work)%n",
                d.nodesExpanded, d.path.cost(), (double) d.nodesExpanded / Math.max(1, r.nodesExpanded));
    }

    private static void nn(String[] a) throws Exception {
        require(a, 4, "nn <graph.json> <x> <y> [k]");
        Graph g = GraphLoader.fromJson(Path.of(a[1]));
        double x = Double.parseDouble(a[2]);
        double y = Double.parseDouble(a[3]);
        int k = a.length > 4 ? Integer.parseInt(a[4]) : 1;

        List<KDTree.Point> pts = new ArrayList<>();
        for (String id : g.nodeIds()) {
            Node n = g.getNode(id);
            pts.add(new KDTree.Point(id, n.x, n.y));
        }
        KDTree tree = KDTree.build(pts);
        System.out.println("query (" + x + ", " + y + ")");
        int rank = 1;
        for (KDTree.Point p : tree.kNearestNeighbors(x, y, k)) {
            System.out.printf("  %d. %s  dist=%.3f%n", rank++, p.id, KDTree.euclidean(p, x, y));
        }
    }

    private static void route(String[] a) throws Exception {
        require(a, 4, "route <graph.json> <depot> <stop,stop,...> [straight|road]");
        Graph g = GraphLoader.fromJson(Path.of(a[1]));
        String depot = a[2];
        List<String> stops = List.of(a[3].split(","));
        Route.DistanceFn dist = (a.length > 4 && a[4].equalsIgnoreCase("road"))
                ? RouteOptimizer.dijkstraDistance(g)
                : RouteOptimizer.straightLineDistance(g);

        Route nn = RouteOptimizer.nearestNeighborRoute(depot, stops, dist);
        Route opt = RouteOptimizer.twoOptImprove(nn, dist);
        System.out.printf("nearest-neighbour : %s  (%.3f)%n", nn, nn.totalDistance(dist));
        System.out.printf("after 2-opt       : %s  (%.3f)%n", opt, opt.totalDistance(dist));
        double improvement = 100.0 * (nn.totalDistance(dist) - opt.totalDistance(dist)) / nn.totalDistance(dist);
        System.out.printf("improvement       : %.1f%%%n", improvement);
    }

    private static void schedule(String[] a) throws Exception {
        require(a, 4, "schedule <graph.json> <jobs.csv> <vehicleNode,vehicleNode,...>");
        Graph g = GraphLoader.fromJson(Path.of(a[1]));
        List<Job> jobs = GraphLoader.jobsFromCsv(Path.of(a[2]));
        JobScheduler scheduler = new JobScheduler();
        jobs.forEach(scheduler::scheduleJob);

        List<Vehicle> vehicles = new ArrayList<>();
        String[] vnodes = a[3].split(",");
        for (int i = 0; i < vnodes.length; i++) {
            vehicles.add(new Vehicle("V" + (i + 1), vnodes[i].trim()));
        }

        System.out.println("dispatch order (most urgent first):");
        for (JobScheduler.Assignment as : scheduler.assign(vehicles, g)) {
            System.out.println("  " + as);
        }
        if (scheduler.hasNext()) {
            System.out.println("unassigned (no idle vehicle left): " + scheduler.pending());
        }
    }

    private static void require(String[] a, int n, String form) {
        if (a.length < n) {
            System.err.println("usage: " + form);
            System.exit(2);
        }
    }

    private static String[] slice(String[] a, int from) {
        String[] out = new String[Math.max(0, a.length - from)];
        System.arraycopy(a, from, out, 0, out.length);
        return out;
    }

    private static void usage() {
        System.out.println("""
                RouteCore CLI
                  dijkstra <graph.json> <src> <dst>
                  astar    <graph.json> <src> <dst> [euclidean|haversine|zero]
                  nn       <graph.json> <x> <y> [k]
                  route    <graph.json> <depot> <stop,stop,...> [straight|road]
                  schedule <graph.json> <jobs.csv> <vehicleNode,vehicleNode,...>
                  bench    [all|kdtree|astar]
                """);
    }
}
