
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Scale</p>
 * <p><b>Purpose:</b> Ties the simulation to real-world units, so that distances, speeds, waits and
 * arrival rates mean something. The map is a grid of streets; every grid cell is {@link #CELL_KM} long (so the 20 x 20 map is a 20 km square),
 * and the simulation clock runs {@link #SIMULATED_MINUTES_PER_SECOND} simulated minutes for every real
 * second at 1x speed. Everything that needs these relations goes through here.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public final class Scale {
    /** Symbol shown next to money amounts. */
    public static final String CURRENCY = "⃁";
    /** Number of grid cells along each side of the square map; stops sit on its intersections. */
    public static final int GRID_SIZE = 20;
    /** Length of one grid cell (one block between streets) in kilometres. */
    public static final double CELL_KM = 1.0;
    /** How many simulated minutes pass in one second of the simulation clock. */
    public static final double SIMULATED_MINUTES_PER_SECOND = 6.0;
    /** How long a bus stays at a stop to let people off and on, in simulated minutes. */
    public static final double STOP_DWELL_MINUTES = 1.0;
    /** The fewest passengers that arrive at a stop per hour (before the quiet-hours multiplier). */
    public static final double MIN_ARRIVALS_PER_HOUR = 2;
    /** The most passengers that arrive at a stop per hour (before the quiet-hours multiplier). */
    public static final double MAX_ARRIVALS_PER_HOUR = 6;

    /**
     * Converts a bus speed to how far it drives per simulated second.
     *
     * @param speedKmPerHour the speed in km/h
     * @return grid cells per simulated second
     */
    public static double cellsPerSimulatedSecond(double speedKmPerHour) {
        return speedKmPerHour / 60.0 * SIMULATED_MINUTES_PER_SECOND / CELL_KM;
    }

    /**
     * Converts a rate per hour to a rate per simulated second.
     *
     * @param perHour events per hour
     * @return events per simulated second
     */
    public static double perSimulatedSecond(double perHour) {
        return perHour * SIMULATED_MINUTES_PER_SECOND / 60.0;
    }

    /**
     * Converts grid cells to kilometres.
     *
     * @param cells a distance in grid cells
     * @return the distance in kilometres
     */
    public static double cellsToKm(double cells) {
        return cells * CELL_KM;
    }

    /**
     * Not meant to be instantiated: this class only holds constants and conversions.
     */
    private Scale() {
    }
}
