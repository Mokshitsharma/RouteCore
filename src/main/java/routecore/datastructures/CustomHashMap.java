package routecore.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-rolled hash map using separate chaining.
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Separate chaining</b> (array of singly linked buckets) over open addressing:
 *       simpler deletion (no tombstones), degrades gracefully past load factor 1,
 *       and the chain length stays ~O(1) as long as we resize.</li>
 *   <li><b>Hash mixing</b>: {@code key.hashCode()} alone clusters badly when the low
 *       bits are similar (e.g. sequential ids). We spread entropy with
 *       {@code h ^ (h >>> 16)} — the same trick {@code java.util.HashMap} uses —
 *       then mask with {@code (capacity - 1)} (capacity is always a power of two).</li>
 *   <li><b>Load-factor resize</b>: when {@code size > 0.75 * capacity} we double the
 *       table and rehash every entry. Amortized O(1) put; a single put that triggers
 *       a resize is O(n).</li>
 * </ul>
 *
 * <p>Complexity: put / get / remove / containsKey are O(1) average, O(n) worst case
 * (all keys collide). Space: O(n + capacity).
 *
 * <p>Not thread-safe. Null keys are rejected; null values are allowed.
 *
 * @param <K> key type (must have sane {@code hashCode}/{@code equals})
 * @param <V> value type
 */
public class CustomHashMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final double MAX_LOAD_FACTOR = 0.75;

    private static final class Entry<K, V> {
        final K key;
        final int hash;
        V value;
        Entry<K, V> next;

        Entry(K key, int hash, V value, Entry<K, V> next) {
            this.key = key;
            this.hash = hash;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<K, V>[] buckets;
    private int size;

    public CustomHashMap() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public CustomHashMap(int initialCapacity) {
        int cap = tableSizeFor(Math.max(initialCapacity, 1));
        this.buckets = (Entry<K, V>[]) new Entry[cap];
        this.size = 0;
    }

    /** Smallest power of two >= c. */
    private static int tableSizeFor(int c) {
        int n = 1;
        while (n < c) n <<= 1;
        return n;
    }

    /** Spread the hash so low-bit patterns do not all land in the same bucket. */
    private static int spread(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private int indexFor(int hash) {
        return hash & (buckets.length - 1);
    }

    /** @return the previous value bound to {@code key}, or null if it was unbound. */
    public V put(K key, V value) {
        requireKey(key);
        int hash = spread(key);
        int idx = indexFor(hash);
        for (Entry<K, V> e = buckets[idx]; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                V old = e.value;
                e.value = value;
                return old;
            }
        }
        buckets[idx] = new Entry<>(key, hash, value, buckets[idx]);
        size++;
        if (size > MAX_LOAD_FACTOR * buckets.length) {
            resize(buckets.length << 1);
        }
        return null;
    }

    public V get(K key) {
        requireKey(key);
        int hash = spread(key);
        for (Entry<K, V> e = buckets[indexFor(hash)]; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                return e.value;
            }
        }
        return null;
    }

    public V getOrDefault(K key, V defaultValue) {
        V v = get(key);
        return v != null || containsKey(key) ? v : defaultValue;
    }

    public boolean containsKey(K key) {
        requireKey(key);
        int hash = spread(key);
        for (Entry<K, V> e = buckets[indexFor(hash)]; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** @return the removed value, or null if the key was absent. */
    public V remove(K key) {
        requireKey(key);
        int hash = spread(key);
        int idx = indexFor(hash);
        Entry<K, V> prev = null;
        for (Entry<K, V> e = buckets[idx]; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                if (prev == null) {
                    buckets[idx] = e.next;
                } else {
                    prev.next = e.next;
                }
                size--;
                return e.value;
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Snapshot of all keys. O(n). Order is unspecified. */
    public List<K> keys() {
        List<K> out = new ArrayList<>(size);
        for (Entry<K, V> bucket : buckets) {
            for (Entry<K, V> e = bucket; e != null; e = e.next) {
                out.add(e.key);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
        Entry<K, V>[] old = buckets;
        Entry<K, V>[] next = (Entry<K, V>[]) new Entry[newCapacity];
        for (Entry<K, V> bucket : old) {
            for (Entry<K, V> e = bucket; e != null; ) {
                Entry<K, V> nextEntry = e.next;
                int idx = e.hash & (newCapacity - 1);
                e.next = next[idx];
                next[idx] = e;
                e = nextEntry;
            }
        }
        this.buckets = next;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("CustomHashMap does not permit null keys");
        }
    }

    /** Test/diagnostic hook: current bucket-array length. */
    public int capacity() {
        return buckets.length;
    }
}
