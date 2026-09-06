package routecore.routing;

import java.util.Objects;

/**
 * A delivery job waiting to be dispatched.
 *
 * @param id         unique job id (identity for heap/map use)
 * @param locationId graph node where the delivery happens
 * @param priority   urgency as a min-heap key: <b>lower value = more urgent</b>
 *                   (0 = drop-everything, 100 = whenever). Ties break by
 *                   {@code sequence} so scheduling is deterministic and FIFO within
 *                   a priority band.
 */
public final class Job {
    public final String id;
    public final String locationId;
    public final int priority;
    public final long sequence;

    private static long counter = 0;

    public Job(String id, String locationId, int priority) {
        this.id = Objects.requireNonNull(id, "id");
        this.locationId = Objects.requireNonNull(locationId, "locationId");
        this.priority = priority;
        this.sequence = counter++;
    }

    /** Composite key so equal priorities keep insertion order. */
    double heapKey() {
        // priority dominates; sequence (<< 1e15 realistically) is the tiebreaker.
        return priority + sequence * 1e-9;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Job j && j.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Job{" + id + " @" + locationId + " p=" + priority + "}";
    }
}
