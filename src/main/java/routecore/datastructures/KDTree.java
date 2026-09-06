package routecore.datastructures;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 2-D k-d tree: a binary spatial index that answers nearest-neighbour queries in
 * O(log n) average time instead of the O(n) of a linear scan.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Alternating split dimension</b>: depth 0 splits on x, depth 1 on y,
 *       depth 2 on x, ... Each node partitions space with an axis-aligned plane.</li>
 *   <li><b>Nearest-neighbour = depth-first + branch-and-bound</b>: descend to the
 *       leaf the query would occupy (that gives a good first candidate), then unwind.
 *       At each node we only cross to the far subtree if the splitting plane is
 *       closer than the best distance found so far — {@code |query[axis] - node[axis]|
 *       < bestDist}. That pruning is what buys the average-case speedup.</li>
 *   <li><b>Squared distances everywhere</b>: comparisons never need the real Euclidean
 *       distance, so we skip {@code Math.sqrt} in the hot path and only take the root
 *       when returning a human-facing number.</li>
 *   <li><b>Degenerate case</b>: if points are inserted in sorted order the tree becomes
 *       a linked list and queries degrade to O(n). A balanced build (recursively split
 *       on the median) fixes this; {@link #build(List)} does exactly that.</li>
 * </ul>
 *
 * <p>Complexity: insert O(log n) average / O(n) worst; nearestNeighbor and
 * kNearestNeighbors O(log n) average / O(n) worst; build O(n log^2 n) with this
 * simple median-by-sort. Space O(n).
 */
public class KDTree {

    /** An indexed 2-D point. Identity for map/heap use is the {@code id}. */
    public static final class Point {
        public final String id;
        public final double x;
        public final double y;

        public Point(String id, double x, double y) {
            this.id = Objects.requireNonNull(id, "id");
            this.x = x;
            this.y = y;
        }

        double coord(int axis) {
            return axis == 0 ? x : y;
        }

        public double dist2(double qx, double qy) {
            double dx = x - qx;
            double dy = y - qy;
            return dx * dx + dy * dy;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Point p && p.id.equals(id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id + "(" + x + ", " + y + ")";
        }
    }

    private static final class KDNode {
        final Point point;
        final int axis;
        KDNode left, right;

        KDNode(Point point, int axis) {
            this.point = point;
            this.axis = axis;
        }
    }

    private KDNode root;
    private int size;

    public KDTree() {
    }

    /** Bulk balanced build from a point list. Clears any existing contents. */
    public static KDTree build(List<Point> points) {
        KDTree t = new KDTree();
        t.root = t.buildRec(new ArrayList<>(points), 0);
        t.size = points.size();
        return t;
    }

    private KDNode buildRec(List<Point> pts, int depth) {
        if (pts.isEmpty()) return null;
        int axis = depth % 2;
        pts.sort((a, b) -> Double.compare(a.coord(axis), b.coord(axis)));
        int mid = pts.size() / 2;
        KDNode node = new KDNode(pts.get(mid), axis);
        node.left = buildRec(new ArrayList<>(pts.subList(0, mid)), depth + 1);
        node.right = buildRec(new ArrayList<>(pts.subList(mid + 1, pts.size())), depth + 1);
        return node;
    }

    public void insert(Point p) {
        Objects.requireNonNull(p, "point");
        root = insertRec(root, p, 0);
    }

    private KDNode insertRec(KDNode node, Point p, int depth) {
        if (node == null) {
            size++;
            return new KDNode(p, depth % 2);
        }
        int axis = node.axis;
        if (p.coord(axis) < node.point.coord(axis)) {
            node.left = insertRec(node.left, p, depth + 1);
        } else {
            node.right = insertRec(node.right, p, depth + 1);
        }
        return node;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /** @return the stored point closest to {@code (qx, qy)}, or null if the tree is empty. */
    public Point nearestNeighbor(double qx, double qy) {
        if (root == null) return null;
        Best best = new Best();
        nnRec(root, qx, qy, best);
        return best.point;
    }

    private static final class Best {
        Point point;
        double dist2 = Double.POSITIVE_INFINITY;
    }

    private void nnRec(KDNode node, double qx, double qy, Best best) {
        if (node == null) return;
        double d2 = node.point.dist2(qx, qy);
        if (d2 < best.dist2) {
            best.dist2 = d2;
            best.point = node.point;
        }
        int axis = node.axis;
        double diff = (axis == 0 ? qx : qy) - node.point.coord(axis);
        KDNode near = diff < 0 ? node.left : node.right;
        KDNode far = diff < 0 ? node.right : node.left;

        nnRec(near, qx, qy, best);
        // Only cross the splitting plane if it could hide something closer.
        if (diff * diff < best.dist2) {
            nnRec(far, qx, qy, best);
        }
    }

    /**
     * @return up to {@code k} stored points nearest to {@code (qx, qy)}, ordered
     *         nearest-first.
     */
    public List<Point> kNearestNeighbors(double qx, double qy, int k) {
        if (k <= 0 || root == null) return new ArrayList<>();
        // Bounded max-heap: priority = -dist2, so extractMin() drops the current
        // farthest once we exceed k. Reuses our own MinHeap.
        MinHeap<Point> worst = new MinHeap<>();
        knnRec(root, qx, qy, k, worst);
        List<Point> out = new ArrayList<>(worst.size());
        while (!worst.isEmpty()) {
            out.add(worst.extractMin());
        }
        java.util.Collections.reverse(out); // farthest-first -> nearest-first
        return out;
    }

    private void knnRec(KDNode node, double qx, double qy, int k, MinHeap<Point> worst) {
        if (node == null) return;
        double d2 = node.point.dist2(qx, qy);
        if (worst.size() < k) {
            worst.insert(node.point, -d2);
        } else if (-d2 > worst.peekPriority()) { // closer than the current farthest
            worst.extractMin();
            worst.insert(node.point, -d2);
        }
        int axis = node.axis;
        double diff = (axis == 0 ? qx : qy) - node.point.coord(axis);
        KDNode near = diff < 0 ? node.left : node.right;
        KDNode far = diff < 0 ? node.right : node.left;

        knnRec(near, qx, qy, k, worst);
        double farthest = worst.size() < k ? Double.POSITIVE_INFINITY : -worst.peekPriority();
        if (diff * diff < farthest) {
            knnRec(far, qx, qy, k, worst);
        }
    }

    /** Straight-line distance helper for callers that want the real number. */
    public static double euclidean(Point a, double qx, double qy) {
        return Math.sqrt(a.dist2(qx, qy));
    }
}
