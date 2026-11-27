import javafx.scene.paint.Color;
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> HighPriorityStop</p>
 * <p><b>Purpose:</b> A busy bus stop that is fully active all day. It is the only kind of stop a
 * {@link DoubleDecker} may serve.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/25/26
 */
public class HighPriorityStop extends BusStop {
    private static final Color BOX_COLOR = Color.web("#8cacd0");      // Fill of the sign box
    private static final Color ACCENT_COLOR = Color.web("#000000");   // Flag, sign border and top bar


    /**
     * Constructs a high-priority stop at a copy of the given {@link Location}.
     *
     * @param location stop position
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public HighPriorityStop(Location location) {
        super(location, null);   // no picture yet: pass new Icon("...png", 1.0) here to draw one instead of the sign
    }

    /**
     * Constructs a high-priority stop at the given X and Y coordinates.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public HighPriorityStop(double x, double y) {
        super(x, y, null);   // no picture yet: pass new Icon("...png", 1.0) here to draw one instead of the sign
    }

    /**
     * Identifies this stop as high priority.
     *
     * @return {@code true}
     */
    @Override
    public boolean isHighPriority() {
        return true;
    }

    /**
     * A high-priority stop is equally busy at every time of day.
     *
     * @param nowInSimulatedMinutes current simulated time in minutes (not used)
     * @return {@code 1.0}
     */
    @Override
    public double getArrivalMultiplier(double nowInSimulatedMinutes) {
        return 1.0;
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
