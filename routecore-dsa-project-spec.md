# RouteCore — Route Optimization & Fleet Scheduling Engine (Built From Scratch)
### Technical Specification & Build Document (for Claude Code / self-implementation)

---

## 1. Overview

RouteCore is a from-scratch implementation of a route optimization and fleet scheduling engine — no external routing libraries (no OSRM, no Google Maps Directions API for the core logic). Every core algorithm (shortest path, nearest-neighbor search, route optimization, job scheduling) is hand-built.

**Core purpose:** This is not a product to sell — it's an interview-grade DSA showcase. The goal is to walk into a TCS/Infosys/product-company technical round and be able to explain, whiteboard, and defend every data structure and algorithm used, with real complexity analysis.

**Differentiation from Mokshit's existing projects:** Every other project (Movigo, RAG, Voice Agent, Finance Copilot) is about shipping a usable product with external libraries doing the heavy lifting. RouteCore is the opposite: implement the algorithms yourself. It's the one project explicitly built for algorithmic depth, not product polish.

**Domain tie-in:** Conceptually mirrors Movigo's dispatch problem (matching drivers to deliveries, finding shortest routes) — but instead of using OSRM/production routing services, you build the underlying algorithms yourself, which deepens your understanding of what those production tools are actually doing.

---

## 2. Goals & Success Metrics

| Goal | Metric |
|---|---|
| Correct shortest-path implementation | Matches known-correct results on standard graph test cases |
| Efficient nearest-neighbor queries | Spatial index outperforms brute-force linear search on large datasets (demonstrable benchmark) |
| Working route optimization | 2-opt improvement measurably reduces total route distance vs. naive nearest-neighbor route |
| Interview-readiness | Can explain time/space complexity of every structure used, and why it was chosen over alternatives |
| Clean, testable code | Each data structure/algorithm is a standalone, unit-tested module |

---

## 3. System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      CLI / Visualizer                     │
│         (input: nodes, edges, vehicles, jobs)              │
└───────────────────────┬─────────────────────────────────┘
                         ▼
        ┌────────────────────────────────┐
        │        Core Engine              │
        │                                  │
        │  ┌────────────┐  ┌────────────┐ │
        │  │  Graph      │  │  Spatial   │ │
        │  │  Module     │  │  Index     │ │
        │  │ (adjacency  │  │ (KD-Tree / │ │
        │  │  list, edge │  │  Quadtree) │ │
        │  │  weights)   │  │            │ │
        │  └─────┬──────┘  └─────┬──────┘ │
        │        │                │        │
        │        ▼                ▼        │
        │  ┌────────────┐  ┌────────────┐ │
        │  │ Pathfinding │  │  Nearest-  │ │
        │  │ (Dijkstra,  │  │  Neighbor  │ │
        │  │  A*)        │  │  Query     │ │
        │  └─────┬──────┘  └─────┬──────┘ │
        │        │                │        │
        │        └───────┬────────┘        │
        │                ▼                 │
        │      ┌────────────────────┐      │
        │      │  Route Optimizer     │      │
        │      │  (Nearest-Neighbor   │      │
        │      │   construction +    │      │
        │      │   2-opt improvement)│      │
        │      └──────────┬──────────┘      │
        │                 ▼                 │
        │      ┌────────────────────┐      │
        │      │  Job Scheduler       │      │
        │      │  (Min-Heap based     │      │
        │      │   priority queue)    │      │
        │      └────────────────────┘      │
        └────────────────────────────────┘
```

### Design principle — Correctness & Complexity Transparency (not "dynamic config" like the product specs)
Unlike the product projects, RouteCore's design principle is different: every module must be independently testable and every algorithm choice documented with its Big-O complexity and why it was chosen over an alternative (e.g., "used a binary min-heap for Dijkstra's priority queue — O(log n) decrease-key vs O(n) for a naive array scan"). This documentation-of-reasoning IS the interview prep value.

---

## 4. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Java or C++ (pick whichever matches your DSA interview prep language — Java recommended since TCS/Infosys rounds often expect Java) | Interviewers will ask you to write similar code live; build in the language you'll be tested in |
| Data structures | Hand-built: custom Min-Heap, custom HashMap (open addressing or chaining), custom Graph (adjacency list), KD-Tree | This is the entire point — no `PriorityQueue` or `HashMap` from the standard library for the core modules (use them only to double check correctness in tests) |
| Testing | JUnit (Java) or Catch2 (C++) | Unit test every module in isolation |
| Visualization (optional) | Simple CLI output or a small web visualizer using Claude's Visualizer tool to animate pathfinding on a grid | Great for demos, not required for interview value |
| Benchmarking | Simple timing harness comparing your KD-tree nearest-neighbor vs brute-force linear scan at increasing N | Concrete proof of complexity improvement — strong interview talking point |

---

## 5. Core Modules & Data Structures

```
Graph {
  addNode(id, lat, lng)
  addEdge(fromId, toId, weight)
  getNeighbors(id) -> List<Edge>
  // Backed by: adjacency list using custom HashMap<NodeId, List<Edge>>
}

MinHeap<T> {
  insert(item, priority)
  extractMin() -> T
  decreaseKey(item, newPriority)
  // Backed by: array-based binary heap, O(log n) insert/extract
}

CustomHashMap<K, V> {
  put(key, value)
  get(key) -> V
  // Backed by: separate chaining, custom hash function, load-factor based resizing
}

KDTree {
  insert(point)
  nearestNeighbor(queryPoint) -> Point
  kNearestNeighbors(queryPoint, k) -> List<Point>
  // 2D spatial index for "find nearest driver/node" queries
}

Dijkstra {
  shortestPath(graph, sourceId, destId) -> Path
  // Uses MinHeap for priority queue, O((V+E) log V)
}

AStar {
  shortestPath(graph, sourceId, destId, heuristicFn) -> Path
  // Uses MinHeap + heuristic (Euclidean/Haversine distance), faster in practice than Dijkstra for point-to-point
}

RouteOptimizer {
  nearestNeighborRoute(depot, stops) -> Route
  twoOptImprove(route) -> Route
  // Classic VRP approximation heuristic
}

JobScheduler {
  scheduleJob(job, priority)
  getNextJob() -> Job
  // Min-heap based priority scheduling for delivery jobs
}
```

---

## 6. Core Features (Build Scope)

1. **Graph construction** — load a set of nodes (locations) and weighted edges (roads/distances) from a simple input file (CSV or JSON).
2. **Dijkstra's algorithm** — shortest path between two nodes, built on your own min-heap.
3. **A\* algorithm** — shortest path with a heuristic (straight-line/Haversine distance), benchmarked against Dijkstra to show the speedup.
4. **KD-Tree spatial index** — given a set of driver locations, answer "which driver is nearest to this pickup point" in O(log n) average case instead of O(n) brute force.
5. **Route optimization (VRP-lite)** — given a depot and a list of delivery stops, construct an initial route via nearest-neighbor heuristic, then improve it with 2-opt swaps to reduce total distance.
6. **Job scheduler** — a min-heap based priority queue that assigns jobs (deliveries) to available vehicles by priority/urgency.
7. **Benchmark suite** — compare your custom structures against brute-force/naive approaches at increasing input sizes (N=100, 1000, 10000) and produce a simple table/chart of the results.
8. **(Optional) Visualizer** — animate Dijkstra/A* exploring a grid, and show the before/after route on a 2-opt improvement, using Claude's Visualizer tool for a clean demo.

### Explicitly out of scope
- Real-world map data (OpenStreetMap import) — use synthetic/small real datasets, not production-scale maps
- Multi-vehicle capacity constraints (classic VRP is NP-hard; keep this to single-vehicle TSP-style routing plus simple job assignment, not full VRP solver complexity)
- Any external routing/mapping API — defeats the purpose of this project

---

## 7. Suggested Folder Structure

```
routecore/
├── src/
│   ├── datastructures/
│   │   ├── MinHeap.java
│   │   ├── CustomHashMap.java
│   │   └── KDTree.java
│   ├── graph/
│   │   ├── Graph.java
│   │   ├── Dijkstra.java
│   │   └── AStar.java
│   ├── routing/
│   │   ├── RouteOptimizer.java   (nearest-neighbor + 2-opt)
│   │   └── JobScheduler.java
│   └── Main.java                  (CLI entry point)
├── test/
│   ├── MinHeapTest.java
│   ├── DijkstraTest.java
│   ├── KDTreeTest.java
│   └── RouteOptimizerTest.java
├── benchmarks/
│   └── BenchmarkRunner.java       (timing comparisons vs brute-force)
└── data/
    └── sample_graph.json
```

---

## 8. Build Phases

| Phase | Scope | Est. Time |
|---|---|---|
| 1 | Custom MinHeap + CustomHashMap + unit tests | 2 days |
| 2 | Graph module + Dijkstra's algorithm + tests | 2 days |
| 3 | A* algorithm + benchmark vs Dijkstra | 1–2 days |
| 4 | KD-Tree + nearest-neighbor benchmark vs brute-force | 2–3 days |
| 5 | Route optimizer (nearest-neighbor + 2-opt) | 2–3 days |
| 6 | Job scheduler (min-heap based) | 1 day |
| 7 | (Optional) Visualizer demo | 1–2 days |
| **Total** | | **~10–14 days** |

---

## 9. Instructions for Claude Code

1. Build and unit-test each data structure in complete isolation before wiring anything together — `MinHeap` and `CustomHashMap` must pass their own test suites before `Dijkstra` is written.
2. Do not use Java's built-in `PriorityQueue` or `HashMap` inside the core modules — the entire point is hand-rolled implementations. It's fine to use built-ins only in test files to verify correctness against a known-good reference.
3. After each algorithm is implemented, add a comment block above it stating: time complexity, space complexity, and why this approach was chosen over the most obvious alternative (this becomes your interview talking points — don't skip it).
4. Build the benchmark suite early (after Phase 1) and re-run it after each new structure is added — this produces the concrete "my KD-tree beats brute force at N=10,000" evidence that's compelling in interviews.
5. Keep `Main.java` as a simple CLI that can run any module standalone (e.g., `java Main dijkstra sample_graph.json A B`) so each piece can be demoed independently.
6. If building the optional visualizer, keep it entirely separate from the core engine (call the engine as a library) — don't let visualization code leak into the algorithm implementations.

---

## 10. Prerequisites / Setup Checklist

- [ ] Java 17+ (or C++17 if choosing C++) and a build tool (Maven/Gradle, or CMake)
- [ ] JUnit 5 (or Catch2) set up for testing
- [ ] Comfort with Big-O analysis (review before starting — this project is as much about being able to explain it as building it)
- [ ] A small sample dataset of nodes/edges (can be synthetic — e.g., a 20–50 node grid) to test against

---

## 11. Interview Prep Angle (why this project specifically helps with TCS/Infosys rounds)

- **Dijkstra/A\*** — extremely common whiteboard question; having built it from scratch means you can write it cold under pressure.
- **Heaps** — "implement a priority queue" is a frequent DSA round question; you'll have already done it with decrease-key support, which is more advanced than the typical ask.
- **Hashing** — custom hash map with collision handling is a classic "design a data structure" round question.
- **Trees (KD-Tree)** — less common but shows depth beyond binary search trees; strong differentiator if it comes up in a senior/product-based round.
- **Greedy + local search (2-opt)** — demonstrates algorithmic thinking beyond textbook problems, good for "tell me about a project" conversation starters.

---

## 12. Future Extensions (optional, beyond interview scope)

- Multi-vehicle capacity-constrained VRP (introduces dynamic programming/branch-and-bound)
- Real OpenStreetMap data import for a more realistic demo
- Genetic algorithm-based route optimization as an alternative to 2-opt
