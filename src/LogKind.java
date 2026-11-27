import javafx.scene.paint.Color;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> LogKind</p>
 * <p><b>Purpose:</b> The kinds of line the event log can show, each with the colour it is written in, so
 * what a line is about can be told at a glance. To change a colour, change it here and nowhere else.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public enum LogKind {
    /** A bus arrived at a stop and passengers got off and on (quiet grey, there are many of these). */
    ARRIVAL(Color.web("#6b7280"), false),
    /** A bus drove past a stop it may not serve. */
    PASSED(Color.web("#c62828"), false),
    /** A bus was sent to a stop, or finished such a detour. */
    REROUTE(Color.web("#e65100"), true),
    /** A phase started or finished. */
    PHASE(Color.web("#1565c0"), true),
    /** The map was changed, saved, loaded or copied. */
    EDIT(Color.web("#2e7d32"), false),
    /** A batch test result. */
    BATCH(Color.web("#6a1b9a"), true),
    /** Something went wrong. */
    ERROR(Color.web("#c62828"), true),
    /** Anything else. */
    INFO(Color.web("#1f2733"), false);

    private final Color color;   // Colour of the text
    private final boolean bold;  // Whether the text is bold

    LogKind(Color color, boolean bold) {
        this.color = color;
        this.bold = bold;
    }

    /**
     * Returns the colour lines of this kind are written in.
     *
     * @return the text colour
     */
    public Color getColor() {
        return color;
    }

    /**
     * Tells whether lines of this kind are written in bold.
     *
     * @return {@code true} if bold
     */
    public boolean isBold() {
        return bold;
    }
}
