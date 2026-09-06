# RouteCore — Results & Outcome

_Captured 2026-09-06. Java 17 (Temurin 17.0.20.1), Apache Maven 3.9.16, Windows 10, amd64._

---

## 1. Project summary

RouteCore is a route-optimization and fleet-scheduling engine written from scratch in
Java — no routing libraries (no OSRM, no Maps API) and no `java.util.PriorityQueue` or
`HashMap` in the core. Every data structure and algorithm (binary min-heap with
decrease-key, separately-chained hash map, k-d tree, Dijkstra, A\*, nearest-neighbour +
2-opt route optimisation, min-heap job scheduling) is hand-built, unit-tested in
isolation, and annotated with its time/space complexity. It is an interview-grade DSA
showcase: the value is being able to explain, whiteboard, and defend every structure and
its Big-O, backed by a benchmark suite that measures the hand-built structures against
naive baselines.

---

## 2. Test results — 47 / 47 passing across 8 suites

| Suite | Tests | Result |
|---|---:|---|
| `routecore.datastructures.CustomHashMapTest` | 5 | ✅ pass |
| `routecore.datastructures.KDTreeTest` | 7 | ✅ pass |
| `routecore.datastructures.MinHeapTest` | 8 | ✅ pass |
| `routecore.graph.AStarTest` | 5 | ✅ pass |
| `routecore.graph.DijkstraTest` | 6 | ✅ pass |
| `routecore.io.JsonAndLoaderTest` | 6 | ✅ pass |
| `routecore.routing.JobSchedulerTest` | 6 | ✅ pass |
| `routecore.routing.RouteOptimizerTest` | 4 | ✅ pass |
| **Total** | **47** | **0 failures, 0 errors, 0 skipped** |

Each suite cross-checks its module against a `java.util` reference implementation on
randomized workloads (built-ins are used *only* in tests, as correctness oracles):
`MinHeap` vs `PriorityQueue`, `CustomHashMap` vs `HashMap`, `KDTree` NN/kNN vs a
brute-force scan, `Dijkstra` vs a reference `PriorityQueue`-based Dijkstra, `AStar` vs
`Dijkstra` for optimal-cost equality, and 2-opt vs an exact brute-force TSP for small
instances.

### Exact `mvn clean test` output (evidence)

```
[INFO] Scanning for projects...
[INFO]
[INFO] ----------------------< com.routecore:routecore >-----------------------
[INFO] Building RouteCore 1.0.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- clean:3.2.0:clean (default-clean) @ routecore ---
[INFO] Deleting E:\new_wp\Routing_engine\target
[INFO]
[INFO] --- resources:3.4.0:resources (default-resources) @ routecore ---
[INFO] skip non existing resourceDirectory E:\new_wp\Routing_engine\src\main\resources
[INFO]
[INFO] --- compiler:3.13.0:compile (default-compile) @ routecore ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 20 source files with javac [debug release 17] to target\classes
[INFO]
[INFO] --- resources:3.4.0:testResources (default-testResources) @ routecore ---
[INFO] skip non existing resourceDirectory E:\new_wp\Routing_engine\src\test\resources
[INFO]
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ routecore ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 8 source files with javac [debug release 17] to target\test-classes
[INFO]
[INFO] --- surefire:3.2.5:test (default-test) @ routecore ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running routecore.datastructures.CustomHashMapTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.151 s -- in routecore.datastructures.CustomHashMapTest
[INFO] Running routecore.datastructures.KDTreeTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.528 s -- in routecore.datastructures.KDTreeTest
[INFO] Running routecore.datastructures.MinHeapTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.222 s -- in routecore.datastructures.MinHeapTest
[INFO] Running routecore.graph.AStarTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.184 s -- in routecore.graph.AStarTest
[INFO] Running routecore.graph.DijkstraTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.045 s -- in routecore.graph.DijkstraTest
[INFO] Running routecore.io.JsonAndLoaderTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.191 s -- in routecore.io.JsonAndLoaderTest
[INFO] Running routecore.routing.JobSchedulerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in routecore.routing.JobSchedulerTest
[INFO] Running routecore.routing.RouteOptimizerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.056 s -- in routecore.routing.RouteOptimizerTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  13.154 s
[INFO] Finished at: 2026-09-06T22:30:27+05:30
[INFO] ------------------------------------------------------------------------
```

---

## 3. Benchmark results

Produced by:

```
java -cp target/classes routecore.Main bench
```

```
== KD-tree nearest-neighbour vs brute-force linear scan ==
         N |   brute (ms) | kd-tree (ms) |   speedup | answers match
       100 |        17.92 |         7.68 |      2.3x | yes
      1000 |        25.31 |         1.52 |     16.7x | yes
     10000 |        48.15 |         1.62 |     29.7x | yes
    100000 |      1601.66 |         3.92 |    408.2x | yes

== A* (Euclidean heuristic) vs Dijkstra on a grid graph ==
    grid |      nodes |  Dijkstra exp. |    A* expanded | less work | same cost
   20x20 |        400 |            400 |             20 |     20.0x | yes
   50x50 |       2500 |           2500 |             50 |     50.0x | yes
 100x100 |      10000 |          10000 |            100 |    100.0x | yes
 200x200 |      40000 |          40000 |            200 |    200.0x | yes
```

Both tables are self-verifying: the KD-tree column is only meaningful because every one
of the 2,000 queries per size returned the **same** nearest point as the brute-force
scan (`answers match = yes`), and the A\* row is only meaningful because A\* returned a
path of the **same cost** as Dijkstra (`same cost = yes`) — it is doing less work, not
cutting a corner.

### KD-tree — reading the numbers

- The wins compound with N: ~2–3x at N=100, ~30x at N=10,000, **~400x at N=100,000**.
  That is the O(n) → O(log n) per-query curve made concrete.
- The small-N timings (N=100, N=1,000) are dominated by JVM JIT warm-up and are noisy
  run to run; the N=100,000 row and the A\* expansion counts are the load-bearing
  evidence. Absolute milliseconds vary by machine and run; the **shape** (super-linear
  divergence) is stable.

### A\* vs Dijkstra — Interpretation

**These speedups are best-case and should be read as an upper bound, not a typical
figure.**

- The benchmark grid is **open and obstacle-free**, and the Euclidean heuristic is a
  *tight* estimate of the true (Euclidean-weighted) path cost. Under those conditions A\*
  expands almost exactly the nodes lying on the straight-line corridor between start and
  goal — roughly `grid side` nodes — while Dijkstra expands the entire `side × side`
  lattice. The ratio is therefore `side² / side = side`, which is exactly why the "less
  work" column reads 20x / 50x / 100x / 200x in lock-step with the grid side. It is a
  geometric identity of this idealised setup, not a promise about real maps.
- **On graphs with obstacles, walls, one-way segments, or turn penalties**, the
  heuristic is repeatedly misled: it points the search straight at the goal, the search
  runs into a barrier, and A\* has to fan out and re-expand around it. The advantage
  degrades toward a small constant — commonly **2–10x** on real road networks, and it
  approaches 1x in maze-like worst cases where the heuristic gives almost no usable
  guidance.
- The honest takeaway: A\* is never *worse* than Dijkstra in expansions given an
  admissible heuristic (the test `aStarExpandsNoMoreThanDijkstra` asserts this), and the
  better and more "obstacle-free" the heuristic's view of the world, the larger the win.
  The 200x number is what a clean room looks like; production is messier.

---

## 4. What was built

| Module | File | One-line description | Complexity |
|---|---|---|---|
| **MinHeap** | `datastructures/MinHeap.java` | Array-backed binary min-heap with a side index (`element → slot`) enabling a *real* O(log n) `decreaseKey`. | insert / extractMin / decreaseKey **O(log n)**; peek O(1); space O(n) |
| **CustomHashMap** | `datastructures/CustomHashMap.java` | Separate-chaining hash map, power-of-two table, hash bit-mixing, load-factor 0.75 resize. | put / get / remove / containsKey **O(1) avg**, O(n) worst; space O(n + capacity) |
| **KDTree** | `datastructures/KDTree.java` | 2-D spatial index; alternating split axis; nearest-neighbour and k-NN via depth-first descent + branch-and-bound pruning on squared distances. | build O(n log²n); insert O(log n) avg; **nearestNeighbor / kNN O(log n) avg**, O(n) worst; space O(n) |
| **Graph** | `graph/Graph.java` | Directed weighted graph as an adjacency list over `CustomHashMap<NodeId, List<Edge>>`; undirected roads = two opposing edges. | addNode / addEdge O(1) amortised; getNeighbors O(1); space **O(V + E)** |
| **Dijkstra** | `graph/Dijkstra.java` | Single-source shortest path on the hand-built heap; `decreaseKey` frontier, early-exit on target, plus a full SSSP mode for distance matrices. | **O((V + E) log V)** time, O(V) space |
| **AStar** | `graph/AStar.java` | Dijkstra ordered by `f = g + h`; pluggable admissible `Heuristic` (Euclidean / Haversine / Zero). Same frontier machinery, different key. | O((V + E) log V) worst; ≈ **O(path length)** expansions with a good heuristic; O(V) space |
| **RouteOptimizer** | `routing/RouteOptimizer.java` | Nearest-neighbour tour construction then 2-opt local search (first-improvement) until no shortening swap remains; straight-line or memoised road-distance modes. | NN route **O(n²)**; each 2-opt pass **O(n²)**; space O(n) |
| **JobScheduler** | `routing/JobScheduler.java` | Min-heap priority dispatch queue for delivery jobs (lower key = more urgent, FIFO tiebreak) + greedy nearest-idle-vehicle assignment. | scheduleJob / getNextJob **O(log n)**; assign O(J·V + J log J); space O(J) |
| **JSON parser** | `io/Json.java` | ~120-line recursive-descent reader (objects, arrays, strings with escapes, numbers, bool, null) so the project has **zero runtime dependencies**. | **O(n)** in input length; space O(n) |
| **GraphLoader** | `io/GraphLoader.java` | Loads a `Graph` from JSON (`nodes`/`edges`) or a `nodes.csv` + `edges.csv` pair; loads job lists from CSV. | **O(V + E)** over the file; space O(V + E) |

Supporting types: `Node`, `Edge`, `Path`, `SearchResult`, `Heuristic`, `Route`, `Job`,
`Vehicle`, `BenchmarkRunner`, `Main` (CLI). 20 main source files, 8 test files.

---

## 5. Key engineering decisions

### Real `decreaseKey` heap vs a naive array scan

Dijkstra's relaxation step needs to *lower* a node's tentative distance while it sits in
the frontier. The obvious implementation scans the frontier array to find the node —
O(n) per relaxation, which drags Dijkstra to **O(V² + E)**. Instead, `MinHeap` maintains
a side index `element → current heap slot` (built on the project's own `CustomHashMap`),
so the element is located in O(1) and then sifted up in O(log n). Every internal swap
keeps that index in sync. This is what buys the textbook **O((V + E) log V)** bound.
The common shortcut — push a duplicate entry and skip "stale" pops — is simpler but lets
the heap grow to O(E) entries and needs a settled-check guard on every pop; the indexed
approach keeps the heap at O(V) and models the algorithm as usually taught.

### 8-connected Euclidean grid vs 4-connected Manhattan grid for the A\* benchmark

The A\* benchmark deliberately uses an **8-connected grid with true Euclidean edge
weights** (1 for orthogonal moves, √2 for diagonals). On a **4-connected unit grid** the
true graph metric is Manhattan distance, and for any node the Euclidean heuristic
satisfies `g + h ≤ optimal` *everywhere* — so it prunes almost nothing and A\* expands
essentially the same node set as Dijkstra, making the comparison look pointless even
though the implementation is correct. With Euclidean weights the heuristic is a tight,
consistent estimate of the actual path cost, so A\* prunes hard and the expansion-count
gap becomes visible and explainable. (Rationale is also in the `BenchmarkRunner.grid`
doc-comment.) The trade-off, spelled out in §3, is that this is a *best-case* geometry;
the Interpretation section makes the obstacle caveat explicit so the numbers read as
informed rather than inflated.

### Hand-rolled data structures instead of `java.util`

The entire point of the project is algorithmic depth, not shipping speed. `PriorityQueue`
and `HashMap` do the interesting work in a normal codebase; here they are banned from the
core so that every complexity claim is backed by code the author actually wrote and can
whiteboard under interview pressure. `java.util` equivalents are still used inside the
test suites as independent correctness oracles — the hand-built structures are validated
against them on randomized workloads — which gives the best of both: owned implementations
plus a trusted reference to prove they behave identically. A secondary benefit is **zero
runtime dependencies** (even JSON parsing is hand-written), so the jar is self-contained.

---

## 6. LinkedIn-ready summary

I built **RouteCore**, a route-optimization and fleet-scheduling engine written
completely from scratch in Java — no routing libraries, and deliberately no
`java.util.PriorityQueue` or `HashMap` in the core. I hand-implemented a binary min-heap
with true O(log n) decrease-key, a separately-chained hash map with load-factor
resizing, a 2-D k-d tree, Dijkstra and A\* shortest-path, a nearest-neighbour + 2-opt
route optimiser, a min-heap job scheduler, and even a small recursive-descent JSON
parser so the project has zero runtime dependencies. Every module is unit-tested in
isolation — **47 tests, all passing** — and cross-checked against a `java.util`
reference implementation on randomized inputs. I also wrote a benchmark harness to prove
the complexity improvements rather than just assert them: the k-d tree nearest-neighbour
search runs about **400x faster than a brute-force linear scan at 100,000 points**, and
A\* with a Euclidean heuristic settles far fewer nodes than Dijkstra for the same
optimal path (up to ~200x fewer on an open grid, narrowing to a small constant once
obstacles enter the picture). The goal was to be able to explain and defend the Big-O of
every structure I used — and now I can.

---

## 7. Reproduce

```bash
# from the repo root, with JDK 17 and Maven on PATH
mvn clean test                                   # 47/47 tests
mvn -q compile                                   # produces target/classes
java -cp target/classes routecore.Main bench     # both benchmark tables

# individual module demos (see README)
java -cp target/classes routecore.Main dijkstra data/sample_graph.json A J
java -cp target/classes routecore.Main astar    data/sample_graph.json A J euclidean
java -cp target/classes routecore.Main nn       data/sample_graph.json 5 5 3
java -cp target/classes routecore.Main route    data/sample_graph.json A C,F,H,J,E road
java -cp target/classes routecore.Main schedule data/sample_graph.json data/sample_jobs.csv A,H
```
