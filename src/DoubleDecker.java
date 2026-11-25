import javafx.scene.paint.Color;
import java.util.ArrayList;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> DoubleDecker</p>
 * <p><b>Purpose:</b> High-capacity double-decker bus subclass (50 seats, speed 15, prefix {@code "DD"})
 * restricted exclusively to high-priority stops with a higher per-minute operating cost.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public class DoubleDecker extends Bus {

    /**
     * Constructs a {@code DoubleDecker} bus with a capacity of 50 passengers, speed of 15,
     * prefix {@code "DD"}, the given display color, and the specified route.
     *
     * @param color display/tinting color of the bus
     * @param route list of {@link Location} stops along the bus's route
     */
    public DoubleDecker(Color color, ArrayList<Location> route) {
        super(50, 15, color, route, "DD");
    }

    /**
     * Returns the image file name used for rendering a double-decker bus icon.
     *
     * @return {@code "DoubleDeckerIcon.png"}
     */
    @Override
    public String getIconFileName() {
        return "DoubleDeckerIcon.png";
    }

    /**
     * Determines whether this double-decker bus can serve the given stop; double-deckers
     * only serve high-priority stops.
     *
     * @param stop the {@link BusStop} to check
     * @return {@code true} if {@code stop} is high priority, {@code false} otherwise
     */
    // note 5: double-deckers only serve high-priority stops
    @Override
    public boolean canServe(BusStop stop) {
        return stop.isHighPriority();
    }

    /**
     * Returns the operating cost incurred per simulated minute for running a double-decker bus.
     *
     * @return operating cost per simulated minute ({@code 1.8})
     */
    // note 1: higher per-minute cost reflecting larger capacity
    @Override
    public double getOperatingCostPerMinute() {
        return 1.8;
    }
}