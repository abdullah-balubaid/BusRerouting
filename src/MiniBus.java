import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> MiniBus</p>
 * <p><b>Purpose:</b> Represents a compact 15-passenger bus (speed 35, prefix {@code "M"})
 * capable of dynamic rerouting with a lower per-minute operating cost.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public class MiniBus extends Bus implements Reroutable {

    /**
     * Constructs a {@code MiniBus} with a capacity of 15 passengers, speed of 35,
     * prefix {@code "M"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public MiniBus(Color color, ArrayList<Location> route) {
        super(15, 35, color, route, "M");
    }

    /**
     * Redirects this mini-bus immediately to {@code newDestination} for dynamic rerouting,
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
     * Returns the image file name used for rendering a mini-bus icon.
     *
     * @return {@code "MiniBusIcon.png"}
     */
    @Override
    public String getIconFileName() {
        return "MiniBusIcon.png";
    }

    /**
     * Returns the operating cost incurred per simulated minute for running a mini-bus.
     *
     * @return operating cost per simulated minute ({@code 0.6})
     */
    // note 1: lower per-minute cost reflecting smaller capacity
    @Override
    public double getOperatingCostPerMinute() {
        return 0.6;
    }
}