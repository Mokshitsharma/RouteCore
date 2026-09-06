package routecore.routing;

import routecore.datastructures.MinHeap;
import routecore.graph.Graph;
import routecore.routing.Route.DistanceFn;

import java.util.ArrayList;
import java.util.List;

/**
 * Priority dispatch queue for delivery jobs, backed by our {@link MinHeap}.
 *
 * <p>Jobs are ordered by urgency ({@link Job#priority}, lower = more urgent), with
 * insertion order as a deterministic tiebreaker. {@link #getNextJob()} always
 * returns the most urgent outstanding job in O(log n).
 *
 * <p>Design choices / interview talking points:
 * <ul>
 *   <li><b>Why a heap, not a sorted list?</b> Jobs arrive continuously. A sorted
 *       structure costs O(n) per insert; a heap costs O(log n) for both insert and
 *       extract-min — the right trade for a stream with interleaved pushes and
 *       pops.</li>
 *   <li><b>Assignment policy</b>: {@link #assign} is deliberately simple — drain the
 *       queue most-urgent-first and give each job to the nearest idle vehicle. It is
 *       greedy, not an optimal assignment (that would be the Hungarian algorithm,
 *       O(n^3)); the point here is the priority queue, not the matching.</li>
 * </ul>
 */
public final class JobScheduler {

    private final MinHeap<Job> queue = new MinHeap<>();

    /** Enqueue a job using its own {@link Job#priority}. O(log n). */
    public void scheduleJob(Job job) {
        queue.insert(job, job.heapKey());
    }

    /** Enqueue with an explicit priority override. O(log n). */
    public void scheduleJob(Job job, int priorityOverride) {
        Job copy = new Job(job.id, job.locationId, priorityOverride);
        queue.insert(copy, copy.heapKey());
    }

    /** @return the most urgent outstanding job, removed from the queue. O(log n). */
    public Job getNextJob() {
        if (queue.isEmpty()) {
            throw new IllegalStateException("No jobs scheduled");
        }
        return queue.extractMin();
    }

    public boolean hasNext() {
        return !queue.isEmpty();
    }

    public int pending() {
        return queue.size();
    }

    /** One vehicle assigned to one job. */
    public static final class Assignment {
        public final Vehicle vehicle;
        public final Job job;
        public final double travelCost;

        Assignment(Vehicle vehicle, Job job, double travelCost) {
            this.vehicle = vehicle;
            this.job = job;
            this.travelCost = travelCost;
        }

        @Override
        public String toString() {
            return vehicle.id + " <- " + job.id + " (cost " + String.format("%.2f", travelCost) + ")";
        }
    }

    /**
     * Drain the queue most-urgent-first, assigning each job to the still-idle
     * vehicle closest to it under {@code dist}. Stops when either the queue or the
     * pool of idle vehicles is empty. Jobs left unassigned stay in the queue.
     *
     * @return the assignments made, in the order jobs were dispatched
     */
    public List<Assignment> assign(List<Vehicle> vehicles, DistanceFn dist) {
        List<Vehicle> idle = new ArrayList<>(vehicles);
        List<Assignment> out = new ArrayList<>();
        while (hasNext() && !idle.isEmpty()) {
            Job job = getNextJob();
            Vehicle bestV = null;
            double bestCost = Double.POSITIVE_INFINITY;
            for (Vehicle v : idle) {
                double c = dist.distance(v.locationId, job.locationId);
                if (c < bestCost) {
                    bestCost = c;
                    bestV = v;
                }
            }
            idle.remove(bestV);
            out.add(new Assignment(bestV, job, bestCost));
        }
        return out;
    }

    /** Convenience overload using straight-line distance on the graph's coordinates. */
    public List<Assignment> assign(List<Vehicle> vehicles, Graph graph) {
        return assign(vehicles, RouteOptimizer.straightLineDistance(graph));
    }
}
