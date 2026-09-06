package routecore.graph;

import routecore.datastructures.CustomHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed weighted graph backed by an <b>adjacency list</b>:
 * {@code CustomHashMap<NodeId, List<Edge>>}.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Adjacency list vs adjacency matrix</b>: road networks are sparse
 *       (|E| ~ c|V|, c small). A matrix costs O(V^2) memory and makes
 *       "iterate a node's neighbours" O(V); the list costs O(V + E) memory and
 *       iterates neighbours in O(deg(v)). Every traversal here wants the latter.</li>
 *   <li><b>Custom hash map for the index</b>: node ids are strings; we need O(1)
 *       "give me this node's edges". Our {@link CustomHashMap} provides it without
 *       pulling in {@code java.util.HashMap} for the core path.</li>
 * </ul>
 *
 * <p>Complexity: addNode O(1) amortised, addEdge O(1) amortised,
 * getNeighbors O(1) to fetch the list. Space O(V + E).
 */
public class Graph {

    private final CustomHashMap<String, Node> nodes = new CustomHashMap<>();
    private final CustomHashMap<String, List<Edge>> adjacency = new CustomHashMap<>();
    private int edgeCount;

    /** Register a node. Re-adding the same id overwrites its coordinates. */
    public void addNode(String id, double x, double y) {
        Node node = new Node(id, x, y);
        nodes.put(id, node);
        if (!adjacency.containsKey(id)) {
            adjacency.put(id, new ArrayList<>());
        }
    }

    public void addNode(Node node) {
        addNode(node.id, node.x, node.y);
    }

    /**
     * Add a directed edge {@code from -> to}. Both endpoints must already exist.
     *
     * @param bidirectional when true, also adds the reverse edge {@code to -> from}
     *                      with the same weight (an undirected road)
     */
    public void addEdge(String from, String to, double weight, boolean bidirectional) {
        requireNode(from);
        requireNode(to);
        adjacency.get(from).add(new Edge(from, to, weight));
        edgeCount++;
        if (bidirectional) {
            adjacency.get(to).add(new Edge(to, from, weight));
            edgeCount++;
        }
    }

    /** Convenience: directed edge only. */
    public void addEdge(String from, String to, double weight) {
        addEdge(from, to, weight, false);
    }

    /** @return this node's outgoing edges (empty list if the node has none; never null for a known node). */
    public List<Edge> getNeighbors(String id) {
        List<Edge> edges = adjacency.get(id);
        if (edges == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        return edges;
    }

    public Node getNode(String id) {
        Node n = nodes.get(id);
        if (n == null) {
            throw new IllegalArgumentException("Unknown node: " + id);
        }
        return n;
    }

    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    public List<String> nodeIds() {
        return nodes.keys();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    private void requireNode(String id) {
        if (!nodes.containsKey(id)) {
            throw new IllegalArgumentException("Edge endpoint not in graph: " + id);
        }
    }
}
