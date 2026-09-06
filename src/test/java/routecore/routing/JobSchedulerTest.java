package routecore.routing;

import org.junit.jupiter.api.Test;
import routecore.graph.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class JobSchedulerTest {

    @Test
    void dispatchesMostUrgentFirst() {
        JobScheduler s = new JobScheduler();
        s.scheduleJob(new Job("low", "X", 9));
        s.scheduleJob(new Job("hi", "X", 1));
        s.scheduleJob(new Job("mid", "X", 5));
        assertEquals("hi", s.getNextJob().id);
        assertEquals("mid", s.getNextJob().id);
        assertEquals("low", s.getNextJob().id);
        assertFalse(s.hasNext());
    }

    @Test
    void equalPriorityKeepsInsertionOrder() {
        JobScheduler s = new JobScheduler();
        s.scheduleJob(new Job("first", "X", 3));
        s.scheduleJob(new Job("second", "X", 3));
        s.scheduleJob(new Job("third", "X", 3));
        assertEquals("first", s.getNextJob().id);
        assertEquals("second", s.getNextJob().id);
        assertEquals("third", s.getNextJob().id);
    }

    @Test
    void emptyQueueThrows() {
        assertThrows(IllegalStateException.class, () -> new JobScheduler().getNextJob());
    }

    @Test
    void assignGivesEachJobToNearestIdleVehicle() {
        Graph g = new Graph();
        g.addNode("depotL", 0, 0);
        g.addNode("depotR", 10, 0);
        g.addNode("jobL", 1, 0);
        g.addNode("jobR", 9, 0);

        JobScheduler s = new JobScheduler();
        s.scheduleJob(new Job("urgentR", "jobR", 1));
        s.scheduleJob(new Job("laterL", "jobL", 5));

        List<Vehicle> vehicles = List.of(new Vehicle("L", "depotL"), new Vehicle("R", "depotR"));
        List<JobScheduler.Assignment> plan = s.assign(new ArrayList<>(vehicles), g);

        assertEquals(2, plan.size());
        // most urgent job dispatched first, to the closer vehicle (R)
        assertEquals("urgentR", plan.get(0).job.id);
        assertEquals("R", plan.get(0).vehicle.id);
        assertEquals("laterL", plan.get(1).job.id);
        assertEquals("L", plan.get(1).vehicle.id);
    }

    @Test
    void assignStopsWhenVehiclesRunOut_leavesRestQueued() {
        Graph g = new Graph();
        g.addNode("d", 0, 0);
        g.addNode("j", 1, 1);
        JobScheduler s = new JobScheduler();
        for (int i = 0; i < 5; i++) s.scheduleJob(new Job("j" + i, "j", i));

        List<JobScheduler.Assignment> plan = s.assign(List.of(new Vehicle("only", "d")), g);
        assertEquals(1, plan.size());
        assertEquals("j0", plan.get(0).job.id);
        assertEquals(4, s.pending());
    }

    @Test
    void heapOrderMatchesSortedPrioritiesOnRandomLoad() {
        JobScheduler s = new JobScheduler();
        Random rnd = new Random(21);
        List<Integer> priorities = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            int p = rnd.nextInt(1_000);
            priorities.add(p);
            s.scheduleJob(new Job("j" + i, "loc", p));
        }
        priorities.sort(Integer::compareTo);
        for (int expected : priorities) {
            assertEquals(expected, s.getNextJob().priority);
        }
    }
}
