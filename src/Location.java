/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Location</p>
 * <p><b>Purpose:</b> Represents a 2D coordinate point and calculates distance between locations.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.2 11/25/26
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
    Location(double X, double Y) {
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
     * Computes the Euclidean distance from this location to another {@code Location}.
     *
     * @param location target location (must not be {@code null})
     * @return straight-line distance between this location and {@code location}
     * @throws IllegalArgumentException if {@code location} is {@code null}
     */
    public double distanceTo(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Target location must not be null.");
        }

        double x2 = location.getX();
        double y2 = location.getY();
        double x1 = getX();
        double y1 = getY();

        // Standard 2D Euclidean distance formula: sqrt((x2 - x1)^2 + (y2 - y1)^2)
        double distance = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));

        return distance;
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