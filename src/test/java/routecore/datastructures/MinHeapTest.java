package routecore.datastructures;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {

    @Test
    void extractsInPriorityOrder() {
        MinHeap<String> h = new MinHeap<>();
        h.insert("c", 3);
        h.insert("a", 1);
        h.insert("b", 2);
        h.insert("d", 4);
        assertEquals("a", h.extractMin());
        assertEquals("b", h.extractMin());
        assertEquals("c", h.extractMin());
        assertEquals("d", h.extractMin());
        assertTrue(h.isEmpty());
    }

    @Test
    void peekDoesNotRemove() {
        MinHeap<Integer> h = new MinHeap<>();
        h.insert(10, 10);
        h.insert(5, 5);
        assertEquals(5, h.peek());
        assertEquals(5.0, h.peekPriority());
        assertEquals(2, h.size());
    }

    @Test
    void decreaseKeyReordersAndValidates() {
        MinHeap<String> h = new MinHeap<>();
        h.insert("x", 10);
        h.insert("y", 20);
        h.insert("z", 30);
        h.decreaseKey("z", 1);
        assertEquals("z", h.peek());
        assertEquals(1.0, h.priorityOf("z"));
        assertThrows(IllegalArgumentException.class, () -> h.decreaseKey("z", 5)); // raising rejected
        assertThrows(IllegalArgumentException.class, () -> h.decreaseKey("absent", 0));
    }

    @Test
    void rejectsDuplicateInsert() {
        MinHeap<String> h = new MinHeap<>();
        h.insert("a", 1);
        assertThrows(IllegalArgumentException.class, () -> h.insert("a", 2));
    }

    @Test
    void insertOrDecreaseBehavesLikeDijkstraFrontier() {
        MinHeap<String> h = new MinHeap<>();
        h.insertOrDecrease("a", 10);
        h.insertOrDecrease("a", 4);  // lower -> applied
        h.insertOrDecrease("a", 8);  // higher -> ignored
        assertEquals(4.0, h.priorityOf("a"));
    }

    @Test
    void emptyOpsThrow() {
        MinHeap<Integer> h = new MinHeap<>();
        assertThrows(IllegalStateException.class, h::extractMin);
        assertThrows(IllegalStateException.class, h::peek);
    }

    @Test
    void matchesJavaPriorityQueueOnRandomWorkload() {
        MinHeap<Integer> mine = new MinHeap<>();
        // Reference: java.util.PriorityQueue of priorities only (built-ins allowed in tests).
        PriorityQueue<Double> ref = new PriorityQueue<>();
        Map<Integer, Double> priorityOf = new HashMap<>();
        Random rnd = new Random(99);
        int next = 0;
        for (int i = 0; i < 20_000; i++) {
            if (mine.isEmpty() || rnd.nextBoolean()) {
                double prio = rnd.nextInt(10_000);
                mine.insert(next, prio);
                ref.add(prio);
                priorityOf.put(next, prio);
                next++;
            } else {
                int got = mine.extractMin();
                double gotPrio = priorityOf.remove(got);
                double expectedMin = ref.poll();
                assertEquals(expectedMin, gotPrio, 1e-9, "extracted element is not the minimum");
            }
            assertTrue(mine.isValidHeap());
        }
    }

    @Test
    void stressExtractYieldsNonDecreasingSequence() {
        MinHeap<Integer> h = new MinHeap<>();
        Random rnd = new Random(1);
        for (int i = 0; i < 5_000; i++) {
            h.insert(i, rnd.nextInt(1_000_000));
        }
        double prev = Double.NEGATIVE_INFINITY;
        while (!h.isEmpty()) {
            double p = h.peekPriority();
            assertTrue(p >= prev, "heap returned out-of-order priority");
            prev = p;
            h.extractMin();
        }
    }
}
