/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Location</p>
 * <p><b>Purpose:</b> Represents a 2D coordinate point, compares two locations for equality, and
 * calculates distance between locations.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public class Location {
    private double positionX;   // Horizontal 2D coordinate
    private double positionY;   // Vertical 2D coordinate

    /**
     * Constructs a {@code Location} with the specified X and Y coordinates.
     *
     * @param X initial horizontal coordinate
     * @param Y initial vertical coordinate
     */
    public Location(double X, double Y) {
        positionX = X;
        positionY = Y;
    }

    /**
     * Updates this location's coordinates to the given X and Y values.
     *
     * @param x new horizontal coordinate
     * @param y new vertical coordinate
     */
    public void updateLocation(double x, double y) {
        positionX = x;
        positionY = y;
    }

    /**
     * Copies the coordinates from another {@code Location} into this instance.
     *
     * @param location source location to copy from (must not be {@code null})
     * @throws IllegalArgumentException if {@code location} is {@code null}
     */
    public void updateLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location must not be null.");
        }

        positionX = location.getX();
        positionY = location.getY();
    }

    /**
     * Returns the X coordinate of this location.
     *
     * @return horizontal coordinate {@link #positionX}
     */
    public double getX() {
        return positionX;
    }

    /**
     * Returns the Y coordinate of this location.
     *
     * @return vertical coordinate {@link #positionY}
     */
    public double getY() {
        return positionY;
    }

    /**
     * Computes the grid (Manhattan) distance from this location to another {@code Location}. Buses
     * drive along grid lines (first along X, then along Y), so this is the distance they really travel.
     *
     * @param location target location (must not be {@code null})
     * @return {@code |dx| + |dy|} between this location and {@code location}
     * @throws IllegalArgumentException if {@code location} is {@code null}
     */
    public double gridDistanceTo(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Target location must not be null.");
        }

        return Math.abs(location.getX() - getX()) + Math.abs(location.getY() - getY());
    }

    /**
     * Checks whether another object is a {@code Location} with exactly the same coordinates. This
     * replaces the repeated {@code x == x && y == y} comparisons that used to be written out by hand.
     *
     * @param other the object to compare against
     * @return {@code true} if {@code other} is a {@code Location} at the same X and Y
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Location)) {
            return false;
        }

        Location otherLocation = (Location) other;
        return getX() == otherLocation.getX() && getY() == otherLocation.getY();
    }

    /**
     * Returns a readable string representation of the X and Y coordinates.
     *
     * @return formatted coordinate string
     */
    @Override
    public String toString() {
        return "X:" + positionX + " Y:" + positionY;
    }
}
