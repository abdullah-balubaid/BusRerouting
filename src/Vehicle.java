import javafx.scene.paint.Color;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Vehicle</p>
 * <p><b>Purpose:</b> Abstract base class representing a vehicle with capacity, speed, color,
 * passenger count, and grid movement state.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public abstract class Vehicle {
    private final int PASSENGER_CAPACITY;                         // Maximum number of passengers the vehicle can hold
    private final int SPEED;                                      // Vehicle speed rating
    private Color color;                                          // Display/tinting color of the vehicle
    private boolean isMoving = false;                             // True when current location differs from destination
    private int currentPassengers = 0;                            // Current number of passengers aboard
    private Location currentLocation = new Location(0, 0);        // Current grid position of the vehicle
    private Location Destination = new Location(0, 0);            // Target grid destination of the vehicle

    /**
     * Constructs a {@code Vehicle} with validated capacity, speed, and color.
     *
     * @param Capacity total passenger seating capacity (must be positive)
     * @param Speed    movement speed rating (must be positive)
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
     * Checks whether the vehicle currently has zero passengers.
     *
     * @return {@code true} if no passengers are aboard, {@code false} otherwise
     */
    public boolean isEmpty() {
        return currentPassengers == 0;
    }

    /**
     * Checks whether the vehicle is currently moving toward its destination.
     *
     * @return {@code true} if moving, {@code false} otherwise
     */
    public boolean isMoving() {
        return isMoving;
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
        currentLocation.updateLocation(newLocation);
        updateMoving();
    }

    /**
     * Updates the vehicle's current location using explicit X and Y coordinates and refreshes
     * its movement state.
     *
     * @param x new X coordinate
     * @param y new Y coordinate
     */
    public void updateLocation(double x, double y) {
        currentLocation.updateLocation(x, y);
        updateMoving();
    }

    /**
     * Updates the vehicle's target destination from a {@link Location} instance and refreshes
     * its movement state.
     *
     * @param newDestination the new target destination
     */
    public void updateDestination(Location newDestination) {
        Destination.updateLocation(newDestination);
        updateMoving();
    }

    /**
     * Updates the vehicle's target destination using explicit X and Y coordinates and refreshes
     * its movement state.
     *
     * @param x target X coordinate
     * @param y target Y coordinate
     */
    public void updateDestination(double x, double y) {
        Destination.updateLocation(x, y);
        updateMoving();
    }

    /**
     * Sets the current number of passengers aboard if within {@code [0, PASSENGER_CAPACITY]};
     * otherwise prints a validation message without changing the count.
     *
     * @param Passengers desired passenger count
     */
    public void setCurrentPassengers(int Passengers) {
        if (Passengers <= PASSENGER_CAPACITY && Passengers >= 0) {
            currentPassengers = Passengers;
        } else if (Passengers > PASSENGER_CAPACITY) {
            System.out.println("The vehicle is overfilled.");
        } else {
            System.out.println("Invalid number of passengers.");
        }
    }

    /**
     * Recomputes {@link #isMoving} by comparing {@link #currentLocation} against {@link #Destination}.
     */
    public void updateMoving() {
        isMoving = currentLocation.getX() != Destination.getX()
                || currentLocation.getY() != Destination.getY();
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
     * Returns the vehicle's speed rating.
     *
     * @return speed rating
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
     * Returns a defensive copy of the vehicle's current location.
     *
     * @return copy of {@link #currentLocation}
     */
    public Location getCurrentLocation() {
        return new Location(currentLocation.getX(), currentLocation.getY());
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
     * Moves the vehicle along grid axes toward its destination by up to {@code distance} grid units
     * (first along X, then using any remaining distance along Y), clamping at the target.
     *
     * @param distance maximum grid distance to travel in this step
     */
    public void moveTowardDestination(double distance) {
        Location current = getCurrentLocation();
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

        updateLocation(x, y);
    }
}