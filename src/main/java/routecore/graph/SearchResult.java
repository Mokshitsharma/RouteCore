package routecore.graph;

/**
 * A shortest-path result plus the search effort it took to find it.
 *
 * <p>{@code nodesExpanded} = how many nodes were popped from the frontier and
 * settled. It is the honest measure of work done and the number to quote when
 * comparing Dijkstra against A* — wall-clock time is noisy, expansions are not.
 */
public final class SearchResult {
    public final Path path;
    public final int nodesExpanded;

    public SearchResult(Path path, int nodesExpanded) {
        this.path = path;
        this.nodesExpanded = nodesExpanded;
    }

    @Override
    public String toString() {
        return path + " [expanded=" + nodesExpanded + "]";
    }
}
