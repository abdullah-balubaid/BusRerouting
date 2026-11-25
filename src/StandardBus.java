import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> StandardBus</p>
 * <p><b>Purpose:</b> Represents a standard 30-passenger bus (speed 25, prefix {@code "S"})
 * capable of dynamic rerouting with a baseline per-minute operating cost.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public class StandardBus extends Bus implements Reroutable {

    /**
     * Constructs a {@code StandardBus} with a capacity of 30 passengers, speed of 25,
     * prefix {@code "S"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public StandardBus(Color color, ArrayList<Location> route) {
        super(30, 25, color, route, "S");
    }

    /**
     * Redirects this standard bus immediately to {@code newDestination} for dynamic rerouting,
     * provided its route list is initialized and non-empty.
     *
     * @param newDestination target {@link Location} to reroute toward
     * @throws IllegalArgumentException if {@code route} is {@code null} or empty
     */
    @Override
    public void reRoute(Location newDestination) {
        // Guard against rerouting when no route stops exist to return to afterward
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException("New route must not be null or empty.");
        }
        updateDestination(newDestination);
    }

    /**
     * Returns the image file name used for rendering a standard bus icon.
     *
     * @return {@code "StandardBusIcon.png"}
     */
    @Override
    public String getIconFileName() {
        return "StandardBusIcon.png";
    }

    /**
     * Returns the operating cost incurred per simulated minute for running a standard bus.
     *
     * @return operating cost per simulated minute ({@code 1.0})
     */
    // note 1: baseline per-minute operating cost
    @Override
    public double getOperatingCostPerMinute() {
        return 1.0;
    }
}