package routecore.graph;

import java.util.Objects;

/**
 * A graph vertex: a stable string id plus 2-D coordinates.
 *
 * <p>Coordinates are stored as plain {@code x}/{@code y}. Treat them as projected
 * planar metres for the Euclidean heuristic, or as {@code (lng, lat)} degrees for
 * the Haversine heuristic — the choice lives in {@link Heuristic}, not here.
 */
public final class Node {
    public final String id;
    public final double x;
    public final double y;

    public Node(String id, double x, double y) {
        this.id = Objects.requireNonNull(id, "id");
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Node n && n.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
