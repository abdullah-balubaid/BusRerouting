import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> StandardBus</p>
 * <p><b>Purpose:</b> Represents a standard 30-passenger bus (30 km/h, prefix {@code "S"})
 * capable of dynamic rerouting with a baseline cost per grid cell driven.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.5 11/26/26
 */
public class StandardBus extends Bus implements Reroutable {
    /** Name of this kind of bus, as shown in menus and saved builds. */
    public static final String TYPE_NAME = "Standard Bus";
    private static final String ICON_FILE = "StandardBusIcon.png";    // Picture of this kind of bus
    private static final double COST_PER_KM = 4.0;   // Fuel, driver and upkeep per kilometre, in the map's currency
    private static final int SPEED_KMH = 30;   // Average driving speed in city traffic

    /**
     * Constructs a {@code StandardBus} with a capacity of 30 passengers, speed of 30 km/h,
     * prefix {@code "S"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public StandardBus(Color color, ArrayList<Location> route) {
        super(30, SPEED_KMH, color, route, "S", COST_PER_KM, new Icon(ICON_FILE, 1.0, color));
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
        updateDestination(newDestination);
    }

    /**
     * Returns the name of this kind of bus.
     *
     * @return {@code "Standard Bus"}
     */
    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }
}
