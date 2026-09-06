package routecore.datastructures;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CustomHashMapTest {

    @Test
    void putGetRemoveBasics() {
        CustomHashMap<String, Integer> m = new CustomHashMap<>();
        assertNull(m.put("a", 1));
        assertEquals(1, m.get("a"));
        assertEquals(1, m.put("a", 2)); // returns previous
        assertEquals(2, m.get("a"));
        assertTrue(m.containsKey("a"));
        assertEquals(1, m.size());

        assertEquals(2, m.remove("a"));
        assertNull(m.get("a"));
        assertFalse(m.containsKey("a"));
        assertEquals(0, m.size());
        assertNull(m.remove("a"));
    }

    @Test
    void nullValuesAllowedNullKeysRejected() {
        CustomHashMap<String, String> m = new CustomHashMap<>();
        m.put("k", null);
        assertTrue(m.containsKey("k"));
        assertNull(m.get("k"));
        assertEquals("dflt", new CustomHashMap<String, String>().getOrDefault("missing", "dflt"));
        assertThrows(IllegalArgumentException.class, () -> m.put(null, "x"));
    }

    @Test
    void resizesAndKeepsAllEntries() {
        CustomHashMap<Integer, Integer> m = new CustomHashMap<>(4);
        for (int i = 0; i < 5_000; i++) {
            m.put(i, i * i);
        }
        assertEquals(5_000, m.size());
        assertTrue(m.capacity() >= 5_000 / 0.75);
        for (int i = 0; i < 5_000; i++) {
            assertEquals(i * i, m.get(i));
        }
    }

    @Test
    void matchesJavaHashMapUnderRandomOps() {
        CustomHashMap<Integer, Integer> mine = new CustomHashMap<>();
        Map<Integer, Integer> ref = new HashMap<>();
        Random rnd = new Random(7);
        for (int i = 0; i < 50_000; i++) {
            int key = rnd.nextInt(500);
            int op = rnd.nextInt(3);
            if (op == 0) {
                Integer v = rnd.nextInt();
                assertEquals(ref.put(key, v), mine.put(key, v));
            } else if (op == 1) {
                assertEquals(ref.remove(key), mine.remove(key));
            } else {
                assertEquals(ref.get(key), mine.get(key));
            }
            assertEquals(ref.size(), mine.size());
        }
        for (Integer k : mine.keys()) {
            assertEquals(ref.get(k), mine.get(k));
        }
    }

    @Test
    void collidingKeysStillWork() {
        // Strings whose hashCode collides deliberately: "Aa" and "BB" both hash to 2112.
        CustomHashMap<String, Integer> m = new CustomHashMap<>();
        m.put("Aa", 1);
        m.put("BB", 2);
        assertEquals(1, m.get("Aa"));
        assertEquals(2, m.get("BB"));
        assertEquals(1, m.remove("Aa"));
        assertEquals(2, m.get("BB"));
    }
}
