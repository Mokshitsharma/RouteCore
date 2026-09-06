package routecore.datastructures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class KDTreeTest {

    private static KDTree.Point brute(List<KDTree.Point> pts, double qx, double qy) {
        return pts.stream().min(Comparator.comparingDouble(p -> p.dist2(qx, qy))).orElseThrow();
    }

    @Test
    void emptyTree() {
        KDTree t = new KDTree();
        assertTrue(t.isEmpty());
        assertNull(t.nearestNeighbor(0, 0));
        assertTrue(t.kNearestNeighbors(0, 0, 3).isEmpty());
    }

    @Test
    void singlePoint() {
        KDTree t = new KDTree();
        t.insert(new KDTree.Point("only", 3, 4));
        assertEquals("only", t.nearestNeighbor(0, 0).id);
        assertEquals(5.0, KDTree.euclidean(t.nearestNeighbor(0, 0), 0, 0), 1e-9);
    }

    @Test
    void nearestNeighborMatchesBruteForce_incrementalInsert() {
        Random rnd = new Random(2024);
        List<KDTree.Point> pts = new ArrayList<>();
        KDTree t = new KDTree();
        for (int i = 0; i < 3_000; i++) {
            KDTree.Point p = new KDTree.Point("p" + i, rnd.nextDouble() * 100, rnd.nextDouble() * 100);
            pts.add(p);
            t.insert(p);
        }
        for (int q = 0; q < 1_000; q++) {
            double qx = rnd.nextDouble() * 100;
            double qy = rnd.nextDouble() * 100;
            assertEquals(brute(pts, qx, qy).id, t.nearestNeighbor(qx, qy).id);
        }
    }

    @Test
    void nearestNeighborMatchesBruteForce_balancedBuild() {
        Random rnd = new Random(11);
        List<KDTree.Point> pts = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            pts.add(new KDTree.Point("p" + i, rnd.nextGaussian() * 50, rnd.nextGaussian() * 50));
        }
        KDTree t = KDTree.build(pts);
        assertEquals(5_000, t.size());
        for (int q = 0; q < 1_000; q++) {
            double qx = rnd.nextGaussian() * 60;
            double qy = rnd.nextGaussian() * 60;
            assertEquals(brute(pts, qx, qy).id, t.nearestNeighbor(qx, qy).id);
        }
    }

    @Test
    void kNearestNeighborsAreCorrectAndOrdered() {
        Random rnd = new Random(5);
        List<KDTree.Point> pts = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            pts.add(new KDTree.Point("p" + i, rnd.nextDouble() * 200, rnd.nextDouble() * 200));
        }
        KDTree t = KDTree.build(pts);

        for (int q = 0; q < 300; q++) {
            double qx = rnd.nextDouble() * 200;
            double qy = rnd.nextDouble() * 200;
            int k = 1 + rnd.nextInt(10);

            List<KDTree.Point> expected = new ArrayList<>(pts);
            expected.sort(Comparator.comparingDouble(p -> p.dist2(qx, qy)));
            List<KDTree.Point> got = t.kNearestNeighbors(qx, qy, k);

            assertEquals(k, got.size());
            // ordered nearest-first
            for (int i = 1; i < got.size(); i++) {
                assertTrue(got.get(i - 1).dist2(qx, qy) <= got.get(i).dist2(qx, qy));
            }
            // same distance-multiset as the true top-k (ids can differ only on ties)
            for (int i = 0; i < k; i++) {
                assertEquals(expected.get(i).dist2(qx, qy), got.get(i).dist2(qx, qy), 1e-9);
            }
        }
    }

    @Test
    void kLargerThanTreeReturnsAll() {
        KDTree t = new KDTree();
        for (int i = 0; i < 5; i++) t.insert(new KDTree.Point("p" + i, i, i));
        assertEquals(5, t.kNearestNeighbors(0, 0, 50).size());
    }

    @Test
    void degenerateSortedInsertStillCorrect() {
        KDTree t = new KDTree();
        List<KDTree.Point> pts = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            KDTree.Point p = new KDTree.Point("p" + i, i, i); // strictly increasing -> skewed tree
            pts.add(p);
            t.insert(p);
        }
        assertEquals("p123", t.nearestNeighbor(123.4, 122.9).id);
        assertEquals(brute(pts, 400.1, 399.6).id, t.nearestNeighbor(400.1, 399.6).id);
    }
}
