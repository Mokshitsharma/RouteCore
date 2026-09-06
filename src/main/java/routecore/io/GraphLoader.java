package routecore.io;

import routecore.graph.Graph;
import routecore.routing.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads graphs and job lists from the {@code data/} directory.
 *
 * <p>Graph JSON shape:
 * <pre>{@code
 * {
 *   "nodes": [ { "id": "A", "x": 0, "y": 0 }, ... ],
 *   "edges": [ { "from": "A", "to": "B", "weight": 4, "bidirectional": true }, ... ]
 * }
 * }</pre>
 * {@code bidirectional} defaults to {@code true} (roads run both ways unless stated).
 *
 * <p>Also accepts a two-file CSV form via {@link #fromCsv}:
 * {@code nodes.csv} = {@code id,x,y} and {@code edges.csv} = {@code from,to,weight[,directed]}.
 */
public final class GraphLoader {

    private GraphLoader() {
    }

    @SuppressWarnings("unchecked")
    public static Graph fromJson(Path jsonFile) throws IOException {
        Map<String, Object> root = Json.parseObject(Files.readString(jsonFile));
        Graph g = new Graph();

        List<Object> nodes = (List<Object>) root.getOrDefault("nodes", List.of());
        for (Object o : nodes) {
            Map<String, Object> n = (Map<String, Object>) o;
            g.addNode(Json.str(n, "id"), Json.num(n, "x"), Json.num(n, "y"));
        }

        List<Object> edges = (List<Object>) root.getOrDefault("edges", List.of());
        for (Object o : edges) {
            Map<String, Object> e = (Map<String, Object>) o;
            g.addEdge(Json.str(e, "from"), Json.str(e, "to"),
                    Json.num(e, "weight"), Json.boolOr(e, "bidirectional", true));
        }
        return g;
    }

    public static Graph fromCsv(Path nodesCsv, Path edgesCsv) throws IOException {
        Graph g = new Graph();
        for (String line : Files.readAllLines(nodesCsv)) {
            String[] p = splitRow(line);
            if (p == null || p[0].equalsIgnoreCase("id")) continue;
            g.addNode(p[0].trim(), Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()));
        }
        for (String line : Files.readAllLines(edgesCsv)) {
            String[] p = splitRow(line);
            if (p == null || p[0].equalsIgnoreCase("from")) continue;
            boolean directed = p.length >= 4 && p[3].trim().equalsIgnoreCase("true");
            g.addEdge(p[0].trim(), p[1].trim(), Double.parseDouble(p[2].trim()), !directed);
        }
        return g;
    }

    /** Job CSV: {@code id,locationId,priority} (header row optional). */
    public static List<Job> jobsFromCsv(Path csv) throws IOException {
        List<Job> out = new ArrayList<>();
        for (String line : Files.readAllLines(csv)) {
            String[] p = splitRow(line);
            if (p == null || p[0].equalsIgnoreCase("id")) continue;
            out.add(new Job(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim())));
        }
        return out;
    }

    private static String[] splitRow(String line) {
        if (line == null) return null;
        String t = line.trim();
        if (t.isEmpty() || t.startsWith("#")) return null;
        return t.split(",");
    }
}
