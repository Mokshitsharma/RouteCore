package routecore.graph;

/**
 * A* heuristic: an estimate of the remaining cost from a node to the goal.
 *
 * <p><b>Admissibility</b> (never overestimate the true remaining cost) is what makes
 * A* return an optimal path. Both built-ins below are admissible <i>provided edge
 * weights are real geographic distances</i>: a straight line is never longer than a
 * road. If your weights are travel times or arbitrary costs, scale the heuristic
 * down accordingly or A* may return a sub-optimal route.
 */
@FunctionalInterface
public interface Heuristic {

    /** Estimated remaining cost from {@code a} to {@code goal}. */
    double estimate(Node a, Node goal);

    /** Zero heuristic — turns A* back into plain Dijkstra. Useful as a baseline. */
    Heuristic ZERO = (a, goal) -> 0.0;

    /** Straight-line distance in the coordinate plane. Use when coords are projected metres. */
    Heuristic EUCLIDEAN = (a, goal) -> {
        double dx = a.x - goal.x;
        double dy = a.y - goal.y;
        return Math.sqrt(dx * dx + dy * dy);
    };

    /**
     * Great-circle distance in metres, treating coordinates as {@code x = longitude},
     * {@code y = latitude} in degrees. Use when the graph carries real lat/lng.
     */
    Heuristic HAVERSINE = (a, goal) -> {
        final double R = 6_371_000.0; // Earth radius, metres
        double lat1 = Math.toRadians(a.y);
        double lat2 = Math.toRadians(goal.y);
        double dLat = Math.toRadians(goal.y - a.y);
        double dLon = Math.toRadians(goal.x - a.x);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(h)));
    };
}
