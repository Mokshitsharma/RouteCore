package routecore.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered visiting sequence: {@code depot, stop, stop, ..., depot}.
 *
 * <p>The depot is both the first and last entry (a closed tour). Distances are
 * supplied by a {@link DistanceFn} so the same route works whether "distance" means
 * straight-line, road-network shortest path, or travel time.
 */
public final class Route {

    /** Pluggable pairwise distance between two node ids. */
    @FunctionalInterface
    public interface DistanceFn {
        double distance(String a, String b);
    }

    private final List<String> order; // includes depot at both ends

    public Route(List<String> order) {
        if (order.size() < 2) {
            throw new IllegalArgumentException("A route needs at least depot,depot");
        }
        this.order = new ArrayList<>(order);
    }

    public List<String> order() {
        return Collections.unmodifiableList(order);
    }

    public String depot() {
        return order.get(0);
    }

    /** Interior stops only (depot stripped from both ends). */
    public List<String> stops() {
        return new ArrayList<>(order.subList(1, order.size() - 1));
    }

    public double totalDistance(DistanceFn dist) {
        double sum = 0.0;
        for (int i = 0; i + 1 < order.size(); i++) {
            sum += dist.distance(order.get(i), order.get(i + 1));
        }
        return sum;
    }

    Route withOrder(List<String> newOrder) {
        return new Route(newOrder);
    }

    @Override
    public String toString() {
        return String.join(" -> ", order);
    }
}
