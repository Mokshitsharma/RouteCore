package routecore.graph;

import java.util.Collections;
import java.util.List;

/**
 * Result of a shortest-path query: the ordered node ids from source to
 * destination and the total accumulated edge weight.
 *
 * <p>{@link #isReachable()} is false (and {@code cost} is +Inf, {@code nodes} empty)
 * when no path exists.
 */
public final class Path {
    private final List<String> nodes;
    private final double cost;

    public Path(List<String> nodes, double cost) {
        this.nodes = Collections.unmodifiableList(nodes);
        this.cost = cost;
    }

    public static Path unreachable() {
        return new Path(List.of(), Double.POSITIVE_INFINITY);
    }

    public List<String> nodes() {
        return nodes;
    }

    public double cost() {
        return cost;
    }

    public boolean isReachable() {
        return !nodes.isEmpty() && cost != Double.POSITIVE_INFINITY;
    }

    public int hops() {
        return Math.max(0, nodes.size() - 1);
    }

    @Override
    public String toString() {
        if (!isReachable()) {
            return "Path{unreachable}";
        }
        return "Path{" + String.join(" -> ", nodes) + " | cost=" + cost + "}";
    }
}
