import javafx.scene.paint.Color;
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> LowPriorityStop</p>
 * <p><b>Purpose:</b> A quieter bus stop that is as active as a high-priority stop at the start and
 * end of the (300-minute) day but dials down towards midday, when people are at work. It never
 * closes, it just gets very few passengers. A {@link DoubleDecker} may not serve it.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/25/26
 */
public class LowPriorityStop extends BusStop {
    private static final Color BOX_COLOR = Color.web("#2d3548");      // Fill of the sign box
    private static final Color ACCENT_COLOR = Color.web("#f5a623");   // Flag, sign border and top bar

    // Shape of the day, in simulated minutes. All of these are easy to tweak.
    private static final double RAMP_DOWN_START = 60;     // fully active until here, then starts dialing down
    private static final double MIDDAY_START = 120;       // reaches the quiet level here
    private static final double MIDDAY_END = 180;         // stays quiet until here, then starts picking up
    private static final double RAMP_UP_END = 240;        // fully active again from here to the end
    private static final double MIDDAY_MULTIPLIER = 0.1;  // arrival rate factor at the quietest point

    /**
     * Constructs a low-priority stop at a copy of the given {@link Location}.
     *
     * @param location stop position
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public LowPriorityStop(Location location) {
        super(location, null);   // no picture yet: pass new Icon("...png", 1.0) here to draw one instead of the sign
    }

    /**
     * Constructs a low-priority stop at the given X and Y coordinates.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public LowPriorityStop(double x, double y) {
        super(x, y, null);   // no picture yet: pass new Icon("...png", 1.0) here to draw one instead of the sign
    }

    /**
     * Identifies this stop as low priority.
     *
     * @return {@code false}
     */
    @Override
    public boolean isHighPriority() {
        return false;
    }

    /**
     * Returns how busy this stop is at the given time: {@code 1.0} in the morning, a straight-line
     * decline to {@link #MIDDAY_MULTIPLIER} by midday, a quiet stretch, then a straight-line climb
     * back to {@code 1.0} before the end of the day.
     *
     * @param nowInSimulatedMinutes current simulated time in minutes
     * @return multiplier applied to the stop's base arrival rate
     */
    @Override
    public double getArrivalMultiplier(double nowInSimulatedMinutes) {
        if (nowInSimulatedMinutes <= RAMP_DOWN_START || nowInSimulatedMinutes >= RAMP_UP_END) {
            return 1.0;
        }

        if (nowInSimulatedMinutes < MIDDAY_START) {
            double progress = (nowInSimulatedMinutes - RAMP_DOWN_START) / (MIDDAY_START - RAMP_DOWN_START);
            return 1.0 - progress * (1.0 - MIDDAY_MULTIPLIER);
        }

        if (nowInSimulatedMinutes <= MIDDAY_END) {
            return MIDDAY_MULTIPLIER;
        }

        double progress = (nowInSimulatedMinutes - MIDDAY_END) / (RAMP_UP_END - MIDDAY_END);
        return MIDDAY_MULTIPLIER + progress * (1.0 - MIDDAY_MULTIPLIER);
    }

    /**
     * Returns the fill colour of this stop's sign box.
     *
     * @return box colour
     */
    @Override
    public Color getBoxColor() {
        return BOX_COLOR;
    }

    /**
     * Returns the accent colour (flag, sign border and top bar) of this stop.
     *
     * @return accent colour
     */
    @Override
    public Color getAccentColor() {
        return ACCENT_COLOR;
    }
}
