import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> MiniBus</p>
 * <p><b>Purpose:</b> Represents a compact 15-passenger bus (42 km/h, prefix {@code "M"})
 * capable of dynamic rerouting with a lower cost per grid cell driven.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.5 11/26/26
 */
public class MiniBus extends Bus implements Reroutable {
    /** Name of this kind of bus, as shown in menus and saved builds. */
    public static final String TYPE_NAME = "Mini Bus";
    private static final String ICON_FILE = "MiniBusIcon.png";    // Picture of this kind of bus
    private static final double COST_PER_KM = 2.5;   // Cheaper to run than a standard bus
    private static final int SPEED_KMH = 42;   // Smaller, so quicker through traffic

    /**
     * Constructs a {@code MiniBus} with a capacity of 15 passengers, speed of 42 km/h,
     * prefix {@code "M"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public MiniBus(Color color, ArrayList<Location> route) {
        super(15, SPEED_KMH, color, route, "M", COST_PER_KM, new Icon(ICON_FILE, 1.0, color));
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
        updateDestination(newDestination);
    }

    /**
     * Returns the name of this kind of bus.
     *
     * @return {@code "Mini Bus"}
     */
    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }
}
