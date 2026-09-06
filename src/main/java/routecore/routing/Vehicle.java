package routecore.routing;

import java.util.Objects;

/** A delivery vehicle, identified by id and parked at some graph node. */
public final class Vehicle {
    public final String id;
    public final String locationId;

    public Vehicle(String id, String locationId) {
        this.id = Objects.requireNonNull(id, "id");
        this.locationId = Objects.requireNonNull(locationId, "locationId");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Vehicle v && v.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Vehicle{" + id + " @" + locationId + "}";
    }
}
