package routecore.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * Array-backed binary min-heap with O(log n) {@code decreaseKey} support.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Array-based complete binary tree</b>: children of index {@code i} live at
 *       {@code 2i+1} and {@code 2i+2}, parent at {@code (i-1)/2}. No node objects,
 *       no pointer chasing, cache-friendly.</li>
 *   <li><b>Why a heap for Dijkstra's frontier?</b> We repeatedly need "the cheapest
 *       unsettled node". A sorted array gives O(1) min but O(n) insert; an unsorted
 *       array gives O(1) insert but O(n) min. A binary heap balances both at
 *       O(log n), which makes Dijkstra O((V + E) log V).</li>
 *   <li><b>decreaseKey in O(log n)</b>: the naive way to lower a node's priority is
 *       to scan the array for it — O(n), which drags Dijkstra to O(V^2 + E). We keep
 *       a side index {@code position: element -> heap slot} (our own
 *       {@link CustomHashMap}) so we can find the element in O(1) and then sift it
 *       up in O(log n). Every swap updates that index.</li>
 * </ul>
 *
 * <p>Complexity: insert O(log n), extractMin O(log n), decreaseKey O(log n),
 * peek/contains/size O(1). Space O(n) for the heap + O(n) for the position index.
 *
 * <p>Each element may appear at most once (it is a key in the position index), so
 * {@code T} must have meaningful {@code equals}/{@code hashCode}. Not thread-safe.
 *
 * @param <T> element type
 */
public class MinHeap<T> {

    private final List<T> heap = new ArrayList<>();
    private final List<Double> prio = new ArrayList<>();
    private final CustomHashMap<T, Integer> position = new CustomHashMap<>();

    /** Insert {@code item} with the given priority. Duplicate items are rejected. */
    public void insert(T item, double priority) {
        if (position.containsKey(item)) {
            throw new IllegalArgumentException("Item already in heap: " + item
                    + " (use decreaseKey to change its priority)");
        }
        heap.add(item);
        prio.add(priority);
        int i = heap.size() - 1;
        position.put(item, i);
        siftUp(i);
    }

    /** Insert if absent, otherwise decrease the key when the new priority is lower. */
    public void insertOrDecrease(T item, double priority) {
        if (position.containsKey(item)) {
            if (priority < prio.get(position.get(item))) {
                decreaseKey(item, priority);
            }
        } else {
            insert(item, priority);
        }
    }

    /** @return the minimum-priority element, removed from the heap. */
    public T extractMin() {
        if (isEmpty()) {
            throw new IllegalStateException("extractMin on empty heap");
        }
        T min = heap.get(0);
        int last = heap.size() - 1;
        swap(0, last);
        heap.remove(last);
        prio.remove(last);
        position.remove(min);
        if (!isEmpty()) {
            siftDown(0);
        }
        return min;
    }

    /** @return the minimum-priority element without removing it. */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("peek on empty heap");
        }
        return heap.get(0);
    }

    public double peekPriority() {
        if (isEmpty()) {
            throw new IllegalStateException("peekPriority on empty heap");
        }
        return prio.get(0);
    }

    /**
     * Lower the priority of an element already in the heap.
     *
     * @throws IllegalArgumentException if the item is absent or the new priority is
     *                                  not strictly less than the current one
     */
    public void decreaseKey(T item, double newPriority) {
        Integer idx = position.get(item);
        if (idx == null) {
            throw new IllegalArgumentException("decreaseKey on absent item: " + item);
        }
        if (newPriority > prio.get(idx)) {
            throw new IllegalArgumentException(
                    "decreaseKey must not raise priority (" + prio.get(idx) + " -> " + newPriority + ")");
        }
        prio.set(idx, newPriority);
        siftUp(idx);
    }

    public boolean contains(T item) {
        return position.containsKey(item);
    }

    public double priorityOf(T item) {
        Integer idx = position.get(item);
        if (idx == null) {
            throw new IllegalArgumentException("No such item: " + item);
        }
        return prio.get(idx);
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    // --- internals -------------------------------------------------------------

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            if (prio.get(i) < prio.get(parent)) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int i) {
        int n = heap.size();
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;
            if (left < n && prio.get(left) < prio.get(smallest)) smallest = left;
            if (right < n && prio.get(right) < prio.get(smallest)) smallest = right;
            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        if (a == b) return;
        T ta = heap.get(a);
        T tb = heap.get(b);
        heap.set(a, tb);
        heap.set(b, ta);
        double pa = prio.get(a);
        prio.set(a, prio.get(b));
        prio.set(b, pa);
        position.put(ta, b);
        position.put(tb, a);
    }

    /** Test hook: verify the heap property holds at every node. O(n). */
    public boolean isValidHeap() {
        for (int i = 0; i < heap.size(); i++) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            if (left < heap.size() && prio.get(left) < prio.get(i)) return false;
            if (right < heap.size() && prio.get(right) < prio.get(i)) return false;
        }
        return true;
    }
}
