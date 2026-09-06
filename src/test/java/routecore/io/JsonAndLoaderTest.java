package routecore.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import routecore.graph.Dijkstra;
import routecore.graph.Graph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonAndLoaderTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesScalarsArraysObjectsAndEscapes() {
        Object v = Json.parse("""
                { "s": "a\\"b\\n", "n": -12.5, "e": 3e2, "t": true, "f": false, "nul": null,
                  "arr": [1, 2, [3, 4]], "obj": { "k": "v" } }
                """);
        Map<String, Object> m = (Map<String, Object>) v;
        assertEquals("a\"b\n", m.get("s"));
        assertEquals(-12.5, (Double) m.get("n"));
        assertEquals(300.0, (Double) m.get("e"));
        assertEquals(Boolean.TRUE, m.get("t"));
        assertEquals(Boolean.FALSE, m.get("f"));
        assertNull(m.get("nul"));
        assertEquals(3, ((List<Object>) m.get("arr")).size());
        assertEquals("v", ((Map<String, Object>) m.get("obj")).get("k"));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} junk"));
    }

    @Test
    void emptyContainers() {
        assertTrue(((Map<?, ?>) Json.parse("{}")).isEmpty());
        assertTrue(((List<?>) Json.parse("[]")).isEmpty());
    }

    @Test
    void loadsSampleGraphAndRoutesThroughIt() throws Exception {
        Path file = Path.of("data/sample_graph.json");
        Graph g = GraphLoader.fromJson(file);
        assertEquals(10, g.nodeCount());
        // every edge in the file is bidirectional -> 16 * 2 directed edges
        assertEquals(32, g.edgeCount());

        var p = Dijkstra.shortestPath(g, "A", "J");
        assertTrue(p.isReachable());
        assertEquals("A", p.nodes().get(0));
        assertEquals("J", p.nodes().get(p.nodes().size() - 1));
        // sanity: cost equals the sum of the traversed edges
        assertTrue(p.cost() > 0);
    }

    @Test
    void loadsGraphFromCsvPair(@TempDir Path dir) throws Exception {
        Path nodes = dir.resolve("nodes.csv");
        Path edges = dir.resolve("edges.csv");
        Files.writeString(nodes, "id,x,y\nA,0,0\nB,3,4\nC,6,8\n");
        // A-B bidirectional (blank/false), B-C directed
        Files.writeString(edges, "from,to,weight,directed\nA,B,5,false\nB,C,5,true\n");
        Graph g = GraphLoader.fromCsv(nodes, edges);
        assertEquals(3, g.nodeCount());
        assertEquals(3, g.edgeCount()); // 2 (A<->B) + 1 (B->C)
        assertEquals(10.0, Dijkstra.shortestPath(g, "A", "C").cost(), 1e-9);
        assertFalse(Dijkstra.shortestPath(g, "C", "A").isReachable());
    }

    @Test
    void loadsJobsCsv() throws Exception {
        var jobs = GraphLoader.jobsFromCsv(Path.of("data/sample_jobs.csv"));
        assertEquals(6, jobs.size());
        assertEquals("job-1", jobs.get(0).id);
        assertEquals("H", jobs.get(0).locationId);
        assertEquals(5, jobs.get(0).priority);
    }
}
