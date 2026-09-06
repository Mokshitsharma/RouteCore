package routecore.graph;

import java.util.Objects;

/**
 * A directed weighted edge {@code from -> to} with a non-negative {@code weight}
 * (road length / travel cost).
 *
 * <p>Undirected roads are modelled as two opposing directed edges; see
 * {@link Graph#addEdge}.
 *
 * <p>Non-negative weights matter: Dijkstra's correctness proof relies on the fact
 * that once a node is settled, no cheaper path to it can appear later. A negative
 * edge breaks that invariant (you would need Bellman-Ford instead).
 */
public final class Edge {
    public final String from;
    public final String to;
    public final double weight;

    public Edge(String from, String to, double weight) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        if (weight < 0 || Double.isNaN(weight)) {
            throw new IllegalArgumentException("Edge weight must be non-negative and finite: " + weight);
        }
        this.weight = weight;
    }

    @Override
    public String toString() {
        return from + "->" + to + "(" + weight + ")";
    }
}
