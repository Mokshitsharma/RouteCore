# RouteCore

![Java](https://img.shields.io/badge/Java-17-orange)
![Build](https://img.shields.io/badge/build-Maven-blue)
![Tests](https://img.shields.io/badge/tests-47%2F47%20passing-brightgreen)
![Runtime deps](https://img.shields.io/badge/runtime%20dependencies-0-success)
![Scope](https://img.shields.io/badge/scope-DSA%20showcase-lightgrey)

A **route-optimization and fleet-scheduling engine built from scratch in Java** — no
routing libraries (no OSRM, no Maps API), and no `java.util.PriorityQueue` or `HashMap`
in the core. Every data structure and algorithm is hand-built, unit-tested in isolation
against a `java.util` reference, and annotated with its Big-O. It exists to demonstrate
algorithmic depth, not to ship a product.

**Implemented from scratch:** binary min-heap with true O(log n) decrease-key ·
separately-chained hash map with load-factor resize · 2-D k-d tree (NN + k-NN) ·
adjacency-list graph · Dijkstra · A\* (Euclidean / Haversine heuristics) ·
nearest-neighbour + 2-opt route optimiser · min-heap job scheduler · recursive-descent
JSON parser.

**Verified:** 47 / 47 unit tests passing across 8 suites. Benchmarks show the k-d tree
running **~400x faster than brute-force nearest-neighbour at N = 100,000**, and A\*
settling far fewer nodes than Dijkstra for the same optimal path.

### Benchmarks

Produced by `java -cp target/classes routecore.Main bench`:

**K-d tree nearest-neighbour vs brute-force linear scan** (2,000 queries per size, answers verified identical)

| N | brute (ms) | k-d tree (ms) | speedup | answers match |
|---:|---:|---:|---:|:--:|
| 100 | 17.92 | 7.68 | 2.3x | yes |
| 1,000 | 25.31 | 1.52 | 16.7x | yes |
| 10,000 | 48.15 | 1.62 | 29.7x | yes |
| 100,000 | 1601.66 | 3.92 | **408.2x** | yes |

_Small-N timings are JIT-warm-up-dominated and noisy; the N = 100,000 row is the load-bearing evidence for the O(n) → O(log n) curve._

**A\* (Euclidean heuristic) vs Dijkstra** — nodes expanded to find the same-cost shortest path

| grid | nodes | Dijkstra expanded | A\* expanded | less work | same cost |
|---:|---:|---:|---:|---:|:--:|
| 20×20 | 400 | 400 | 20 | 20.0x | yes |
| 50×50 | 2,500 | 2,500 | 50 | 50.0x | yes |
| 100×100 | 10,000 | 10,000 | 100 | 100.0x | yes |
| 200×200 | 40,000 | 40,000 | 200 | 200.0x | yes |

_These are **best-case** numbers: an open, obstacle-free grid with a tight Euclidean
heuristic, where A\* expands only the straight-line corridor (`≈ grid side`) while
Dijkstra expands the whole `side²` lattice — so the ratio equals the grid side by
construction. With walls, one-way segments, or turn penalties the heuristic gets misled
and the advantage narrows toward a small constant (typically 2–10x on real road
networks). A\* is never worse than Dijkstra in expansions given an admissible heuristic._

Full write-up, including the exact `mvn test` output and engineering-decision rationale:
[`docs/RESULTS.md`](docs/RESULTS.md).

### How to run

```bash
# prerequisites: JDK 17+ and Maven on PATH

mvn clean test                                   # run all 8 suites — 47/47 passing
mvn -q compile                                   # compile to target/classes

java -cp target/classes routecore.Main bench     # both benchmark tables

# individual module demos
java -cp target/classes routecore.Main dijkstra data/sample_graph.json A J
java -cp target/classes routecore.Main astar    data/sample_graph.json A J euclidean
java -cp target/classes routecore.Main nn       data/sample_graph.json 5 5 3
java -cp target/classes routecore.Main route    data/sample_graph.json A C,F,H,J,E road
java -cp target/classes routecore.Main schedule data/sample_graph.json data/sample_jobs.csv A,H
```

Or run the CLI through Maven without managing the classpath:

```bash
mvn -q compile exec:java -Dexec.args="dijkstra data/sample_graph.json A J"
mvn -q compile exec:java -Dexec.args="bench all"
```

---

## Project reference (detailed)

A route-optimization and fleet-scheduling engine built **from scratch** in Java — no
routing libraries, no `java.util.PriorityQueue` or `HashMap` in the core. Every data
structure and algorithm is hand-rolled and unit-tested, with a Big-O rationale in a
comment block above it. It's an interview-grade DSA showcase, not a product.

## What's inside

| Module | File | Core idea | Complexity |
|---|---|---|---|
| Custom hash map | `datastructures/CustomHashMap.java` | separate chaining, hash mixing, load-factor resize | O(1) avg put/get/remove |
| Binary min-heap | `datastructures/MinHeap.java` | array heap + position index for real `decreaseKey` | O(log n) insert/extract/decreaseKey |
| k-d tree | `datastructures/KDTree.java` | 2-D spatial index, branch-and-bound NN search | O(log n) avg NN / kNN |
| Graph | `graph/Graph.java` | adjacency list over `CustomHashMap` | O(V+E) space |
| Dijkstra | `graph/Dijkstra.java` | heap frontier + `decreaseKey`, early exit | O((V+E) log V) |
| A* | `graph/AStar.java` | Dijkstra + admissible heuristic (Euclidean / Haversine) | O((V+E) log V), fewer expansions |
| Route optimizer | `routing/RouteOptimizer.java` | nearest-neighbour construction + 2-opt local search | O(n²) per 2-opt pass |
| Job scheduler | `routing/JobScheduler.java` | min-heap priority dispatch + greedy vehicle assignment | O(log n) per job |
| JSON reader | `io/Json.java` | ~120-line recursive-descent parser (zero deps) | — |
| Benchmarks | `bench/BenchmarkRunner.java` | KD-tree vs brute force, A* vs Dijkstra | prints tables |

## Build & test

```
mvn test           # run every module's unit suite
mvn -q compile      # compile only
```

## Run the CLI

```
mvn -q compile exec:java -Dexec.args="dijkstra data/sample_graph.json A J"
mvn -q compile exec:java -Dexec.args="astar    data/sample_graph.json A J euclidean"
mvn -q compile exec:java -Dexec.args="nn       data/sample_graph.json 5 5 3"
mvn -q compile exec:java -Dexec.args="route    data/sample_graph.json A C,F,H,J,E road"
mvn -q compile exec:java -Dexec.args="schedule data/sample_graph.json data/sample_jobs.csv A,H"
mvn -q compile exec:java -Dexec.args="bench    all"
```

## Interview talking points (short version)

- **Why a binary heap for Dijkstra?** balances O(1)-min vs O(1)-insert into O(log n) both,
  giving O((V+E) log V) on sparse road graphs vs O(V²+E) for the array version.
- **Real `decreaseKey`, not lazy deletion:** a side index `element → heap slot` keeps the
  heap at O(V) entries instead of letting it grow to O(E).
- **k-d tree NN = DFS + branch-and-bound:** only cross the splitting plane when
  `|q[axis] − node[axis]| < bestDistanceSoFar`; skip `sqrt` by comparing squared distances.
- **A* = Dijkstra ordered by `g + h`:** with an admissible `h` the path stays optimal but
  the search aims at the goal — the benchmark shows the expansion-count drop.
- **TSP is NP-hard:** nearest-neighbour gives a fast ~25%-over tour; 2-opt local search
  removes crossings and recovers most of the gap in O(n²) per pass.

## Project layout

```
src/main/java/routecore/
  datastructures/  MinHeap, CustomHashMap, KDTree
  graph/           Graph, Node, Edge, Path, Dijkstra, AStar, Heuristic
  routing/         RouteOptimizer, JobScheduler, Route, Job, Vehicle
  io/              Json, GraphLoader
  bench/           BenchmarkRunner
  Main.java        CLI entry point
src/test/java/routecore/   one suite per module
data/                      sample_graph.json, sample_jobs.csv
```

## License

[MIT](LICENSE) © 2026 Mokshit Sharma
