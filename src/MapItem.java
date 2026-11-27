/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> MapItem</p>
 * <p><b>Purpose:</b> Abstract base class for everything that sits on the map grid (bus stops and
 * vehicles). Holds the grid position once, so {@link BusStop} and {@link Vehicle} no longer each
 * keep their own copy of the same location code.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/25/26
 */
public abstract class MapItem {
    private final Location location = new Location(0, 0);   // Current grid position of this item

    /**
     * Returns a defensive copy of this item's grid position.
     *
     * @return copy of {@link #location}
     */
    public Location getLocation() {
        return new Location(location.getX(), location.getY());
    }

    /**
     * Moves this item to the given grid coordinates.
     *
     * @param x new X coordinate
     * @param y new Y coordinate
     */
    protected final void setLocation(double x, double y) {
        location.updateLocation(x, y);
    }

    /**
     * Moves this item to a copy of the given {@link Location}.
     *
     * @param newLocation the new grid position (must not be {@code null})
     * @throws IllegalArgumentException if {@code newLocation} is {@code null}
     */
    protected final void setLocation(Location newLocation) {
        location.updateLocation(newLocation);
    }

    /**
     * Checks whether this item is currently at the given grid position.
     *
     * @param other the position to compare against
     * @return {@code true} if this item's position equals {@code other}
     */
    public boolean isAt(Location other) {
        return location.equals(other);
    }
}
