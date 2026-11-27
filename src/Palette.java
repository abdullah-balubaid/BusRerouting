import javafx.scene.paint.Color;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Palette</p>
 * <p><b>Purpose:</b> The colours used across the map and the side panels, named after what they colour
 * (not after what colour they are), so a colour can be changed in one place and it is clear what changes.
 * Colours that belong to one kind of thing live with it instead: a stop's own colours are in
 * {@link HighPriorityStop} and {@link LowPriorityStop}, a bus's colour is chosen per bus, and the log's
 * colours are in {@link LogKind}.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v2.0 11/26/26
 */
public final class Palette {
    // ---- The map
    /** Background of the map area. */
    public static final Color MAP_BACKGROUND = Color.web("#ffffff");
    /** The streets of the grid. */
    public static final Color GRID_LINE = Color.web("#c8c8c8");
    /** The outer edge of the grid. */
    public static final Color GRID_BORDER = Color.web("#b4b4b4");
    /** Text drawn on the map ("Stop 3 (5)", "S1 12/30"). */
    public static final Color LABEL_TEXT = Color.web("#1f2733");
    /** The thin outline around map text, so it stays readable over the streets. */
    public static final Color LABEL_OUTLINE = Color.web("#ffffff");
    /** The "REROUTE" tag above a bus that is on a detour. */
    public static final Color REROUTE_TAG = Color.web("#c62828");

    // ---- Build Mode aids on the map
    /** The dots marking free intersections while stops are being added. */
    public static final Color OPEN_INTERSECTION = Color.color(0.1, 0.5, 0.9, 0.35);
    /** The ring around the stops picked so far for a bus route. */
    public static final Color PICKED_STOP_RING = Color.web("#1565c0");

    // ---- Build Mode buttons and hint box
    /** The Finish bus button once the route has enough stops. */
    public static final Color READY_BUTTON = Color.web("#2e7d32");
    /** The Cancel button while a route is being picked. */
    public static final Color CANCEL_BUTTON = Color.web("#c62828");
    /** Background of the hint box that says what the next click does. */
    public static final Color HINT_BOX_BACKGROUND = Color.web("#eef3fb");
    /** Border of the hint box. */
    public static final Color HINT_BOX_BORDER = Color.web("#9fb3d1");

    // ---- The side panels
    /** The line under the group headings above the phase buttons. */
    public static final Color HEADING_UNDERLINE = Color.web("#9aa5b5");
    /** A result that is better than Phase 1, and a satisfied stop. */
    public static final Color GOOD_RESULT_TEXT = Color.web("#2e7d32");
    /** A result that is worse than Phase 1, and an unhappy stop. */
    public static final Color BAD_RESULT_TEXT = Color.web("#c62828");
    /** A result with nothing to praise or blame yet (no figure, or a middling one). */
    public static final Color NEUTRAL_RESULT_TEXT = Color.web("#1f2733");
    /** A bus in the bus list that is on its own route. */
    public static final Color ON_ROUTE_TEXT = Color.web("#2e7d32");
    /** A bus in the bus list that is on a reroute. */
    public static final Color REROUTING_TEXT = Color.web("#c62828");

    /**
     * Writes a colour the way JavaFX style strings expect it, so a colour from this class can be used in {@code setStyle}.
     *
     * @param color the colour
     * @return text such as {@code "#2e7d32"}
     */
    public static String css(Color color) {
        return String.format("#%02x%02x%02x", Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    /**
     * Not meant to be instantiated: this class only holds constants.
     */
    private Palette() {
    }
}
