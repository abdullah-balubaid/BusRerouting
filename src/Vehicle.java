import javafx.scene.paint.Color;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Vehicle</p>
 * <p><b>Purpose:</b> Abstract map item representing a vehicle with capacity, speed, color,
 * passenger count, grid movement state, and the total distance it has driven.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.4 11/25/26
 */
public abstract class Vehicle extends MapItem {
    private final int PASSENGER_CAPACITY;                         // Maximum number of passengers the vehicle can hold
    private final int SPEED;                                      // Vehicle speed rating
    private final Color color;                                          // Display/tinting color of the vehicle
    private int currentPassengers = 0;                            // Current number of passengers aboard
    private Location Destination = new Location(0, 0);            // Target grid destination of the vehicle
    private double distanceTravelled = 0;                         // Total grid distance driven since the last reset

    /**
     * Constructs a {@code Vehicle} with validated capacity, speed, and color.
     *
     * @param Capacity total passenger seating capacity (must be positive)
     * @param Speed    driving speed in km/h (must be positive)
     * @param color    vehicle color (must not be {@code null})
     * @throws IllegalArgumentException if {@code Capacity} or {@code Speed} is less than or equal to 0
     * @throws NullPointerException     if {@code color} is {@code null}
     */
    protected Vehicle(int Capacity, int Speed, Color color) {
        if (Capacity <= 0 || Speed <= 0) {
            throw new IllegalArgumentException("Capacity and speed must be positive.");
        }

        if (color == null) {
            throw new NullPointerException("The chosen color must be not null.");
        }

        SPEED = Speed;
        PASSENGER_CAPACITY = Capacity;
        this.color = color;
    }

    /**
     * Checks whether the vehicle is currently moving toward its destination.
     *
     * @return {@code true} if moving, {@code false} otherwise
     */
    public boolean isMoving() {
        return !isAt(Destination);
    }

    /**
     * Checks whether the vehicle has reached its maximum passenger capacity.
     *
     * @return {@code true} if full, {@code false} otherwise
     */
    public boolean isFull() {
        return currentPassengers == PASSENGER_CAPACITY;
    }

    /**
     * Updates the vehicle's current location from a {@link Location} instance and refreshes
     * its movement state.
     *
     * @param newLocation the new grid position
     */
    public void updateLocation(Location newLocation) {
        setLocation(newLocation);
    }

    /**
     * Updates the vehicle's target destination from a {@link Location} instance and refreshes
     * its movement state.
     *
     * @param newDestination the new target destination
     */
    public void updateDestination(Location newDestination) {
        Destination.updateLocation(newDestination);
    }

    /**
     * Sets the current number of passengers aboard.
     *
     * @param Passengers desired passenger count
     * @throws IllegalArgumentException if the count is negative or more than the capacity
     */
    public void setCurrentPassengers(int Passengers) {
        if (Passengers > PASSENGER_CAPACITY || Passengers < 0) {
            throw new IllegalArgumentException("A " + PASSENGER_CAPACITY + "-passenger vehicle cannot hold " + Passengers + " passengers.");
        }
        currentPassengers = Passengers;
    }

    /**
     * Returns the vehicle's maximum passenger capacity.
     *
     * @return passenger capacity
     */
    public int getPassengerCapacity() {
        return PASSENGER_CAPACITY;
    }

    /**
     * Returns the vehicle's driving speed in km/h.
     *
     * @return speed in km/h
     */
    public int getSpeed() {
        return SPEED;
    }

    /**
     * Returns the vehicle's color.
     *
     * @return vehicle {@link Color}
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the current number of passengers aboard.
     *
     * @return current passenger count
     */
    public int getCurrentPassengers() {
        return currentPassengers;
    }

    /**
     * Returns a defensive copy of the vehicle's current destination.
     *
     * @return copy of {@link #Destination}
     */
    public Location getDestination() {
        return new Location(Destination.getX(), Destination.getY());
    }

    /**
     * Returns the total grid distance this vehicle has driven since {@link #resetDistanceTravelled()}.
     *
     * @return distance travelled in grid cells
     */
    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    /**
     * Sets the distance driven back to zero, called when a new simulation run starts.
     */
    public void resetDistanceTravelled() {
        distanceTravelled = 0;
    }

    /**
     * Moves the vehicle along grid axes toward its destination by up to {@code distance} grid units
     * (first along X, then using any remaining distance along Y), clamping at the target. The
     * distance actually driven is added to {@link #distanceTravelled}.
     *
     * @param distance maximum grid distance to travel in this step
     */
    public void moveTowardDestination(double distance) {
        Location current = getLocation();
        Location target = getDestination();
        double x = current.getX();
        double y = current.getY();

        // Step along X first, snapping directly to target X if within reach
        double differenceX = target.getX() - x;
        double stepX = Math.min(distance, Math.abs(differenceX));
        if (stepX == Math.abs(differenceX)) {
            x = target.getX();
        } else {
            x = x + Math.signum(differenceX) * stepX;
        }

        // Apply remaining movement budget to Y
        distance = distance - stepX;

        double differenceY = target.getY() - y;
        double stepY = Math.min(distance, Math.abs(differenceY));
        if (stepY == Math.abs(differenceY)) {
            y = target.getY();
        } else {
            y = y + Math.signum(differenceY) * stepY;
        }

        distanceTravelled += stepX + stepY;
        setLocation(x, y);
    }
}
