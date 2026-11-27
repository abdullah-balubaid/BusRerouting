import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> DoubleDecker</p>
 * <p><b>Purpose:</b> High-capacity double-decker bus subclass (50 seats, 18 km/h, prefix {@code "DD"})
 * restricted exclusively to high-priority stops, with a higher cost per grid cell driven.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.5 11/26/26
 */
public class DoubleDecker extends Bus {
    /** Name of this kind of bus, as shown in menus and saved builds. */
    public static final String TYPE_NAME = "Double Decker";
    private static final String ICON_FILE = "DoubleDeckerIcon.png";    // Picture of this kind of bus
    private static final double COST_PER_KM = 6.0;   // Costs the most to run, reflecting its size
    private static final int SPEED_KMH = 18;   // Large and slow
    private static final double ICON_SCALE = 1.3;      // Its picture looks small at the standard size, so it is drawn bigger

    /**
     * Constructs a {@code DoubleDecker} bus with a capacity of 50 passengers, speed of 18 km/h,
     * prefix {@code "DD"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public DoubleDecker(Color color, ArrayList<Location> route) {
        super(50, SPEED_KMH, color, route, "DD", COST_PER_KM, new Icon(ICON_FILE, ICON_SCALE, color));
    }

    /**
     * Returns the name of this kind of bus.
     *
     * @return {@code "Double Decker"}
     */
    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Determines whether this double-decker bus can serve the given stop; double-deckers
     * only serve high-priority stops.
     *
     * @param stop the {@link BusStop} to check
     * @return {@code true} if {@code stop} is high priority, {@code false} otherwise
     */
    @Override
    public boolean canServe(BusStop stop) {
        return stop.isHighPriority();
    }
}
