
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> StopSatisfaction</p>
 * <p><b>Purpose:</b> How satisfied the passengers of one bus stop are. Every passenger rates the wait with
 * 0 to 5 stars in steps of half a star: 5 stars up to the acceptable wait, then falling evenly to 0 stars
 * at {@value #ZERO_STAR_FACTOR} times the acceptable wait (so 60 minutes when the acceptable wait is 15).
 * Passengers who have boarded are rated by the wait they had; passengers still waiting are rated by
 * their wait so far. This class holds the figures of one stop and is also the place of the rating rule.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class StopSatisfaction {
    /** The wait that earns 0 stars, as a multiple of the acceptable wait. */
    public static final double ZERO_STAR_FACTOR = 4.0;
    /** A passenger with at least this many stars counts as happy. */
    public static final double HAPPY_STARS = 4.0;
    /** A stop whose average is below this many stars is shown in red in the Stops tab. */
    public static final double UNHAPPY_BELOW_STARS = 2.0;

    private final String stopName;          // e.g. "Stop 3"
    private final boolean highPriority;     // Kind of stop
    private final int passengers;           // Passengers rated (boarded + still waiting)
    private final double starSum;           // Sum of every passenger's stars
    private final int happyPassengers;      // Passengers with at least HAPPY_STARS
    private final double waitSumMinutes;       // Sum of the waits of every rated passenger
    private final int stillWaiting;         // Passengers waiting at the stop right now

    /**
     * Creates the figures of one stop.
     *
     * @param stopName           the stop's display name
     * @param highPriority       whether it is a high priority stop
     * @param passengers         passengers rated
     * @param starSum            sum of every passenger's stars
     * @param happyPassengers    passengers with at least {@link #HAPPY_STARS}
     * @param waitSumMinutes     sum of the waits of every rated passenger (those still waiting count their wait so far)
     * @param stillWaiting       passengers waiting at the stop right now
     */
    public StopSatisfaction(String stopName, boolean highPriority, int passengers, double starSum,
                            int happyPassengers, double waitSumMinutes, int stillWaiting) {
        this.stopName = stopName;
        this.highPriority = highPriority;
        this.passengers = passengers;
        this.starSum = starSum;
        this.happyPassengers = happyPassengers;
        this.waitSumMinutes = waitSumMinutes;
        this.stillWaiting = stillWaiting;
    }

    /**
     * Rates one wait.
     *
     * @param waitMinutes           how long the passenger waited
     * @param acceptableWaitMinutes the wait that still earns 5 stars
     * @return stars from 0 to 5 in steps of 0.5
     */
    public static double starsFor(double waitMinutes, double acceptableWaitMinutes) {
        double zeroAt = acceptableWaitMinutes * ZERO_STAR_FACTOR;
        if (waitMinutes <= acceptableWaitMinutes) {
            return 5.0;
        }
        if (waitMinutes >= zeroAt) {
            return 0.0;
        }
        double stars = 5.0 * (zeroAt - waitMinutes) / (zeroAt - acceptableWaitMinutes);
        return Math.round(stars * 2) / 2.0;
    }

    /**
     * Returns the stop's name.
     *
     * @return text such as {@code "Stop 3"}
     */
    public String getStopName() {
        return stopName;
    }

    /**
     * Tells whether the stop is high priority.
     *
     * @return {@code true} for high priority
     */
    public boolean isHighPriority() {
        return highPriority;
    }

    /**
     * Returns the number of passengers rated.
     *
     * @return boarded plus still waiting
     */
    public int getPassengers() {
        return passengers;
    }

    /**
     * Returns the sum of all stars given (used to combine stops into one overall figure).
     *
     * @return the star total
     */
    public double getStarSum() {
        return starSum;
    }

    /**
     * Returns the stop's average rating.
     *
     * @return average stars, or {@link Double#NaN} if nobody has been rated
     */
    public double getAverageStars() {
        return passengers == 0 ? Double.NaN : starSum / passengers;
    }

    /**
     * Returns the share of passengers who are happy.
     *
     * @return a fraction from 0 to 1, or {@link Double#NaN} if nobody has been rated
     */
    public double getHappyShare() {
        return passengers == 0 ? Double.NaN : (double) happyPassengers / passengers;
    }

    /**
     * Returns the sum of the waits of the rated passengers.
     *
     * @return minutes (used to combine stops into one overall average)
     */
    public double getWaitSumMinutes() {
        return waitSumMinutes;
    }

    /**
     * Returns the average wait of the rated passengers: the same passengers the stars are given to, so the
     * two figures can be read together.
     *
     * @return average wait in minutes, or {@link Double#NaN} if nobody has been rated
     */
    public double getAverageWaitMinutes() {
        return passengers == 0 ? Double.NaN : waitSumMinutes / passengers;
    }

    /**
     * Returns how many passengers are waiting at the stop right now.
     *
     * @return the number waiting
     */
    public int getStillWaiting() {
        return stillWaiting;
    }

    /**
     * Tells whether this stop's passengers are, on average, unhappy.
     *
     * @return {@code true} if the average is below {@link #UNHAPPY_BELOW_STARS}
     */
    public boolean isUnhappy() {
        return !Double.isNaN(getAverageStars()) && getAverageStars() < UNHAPPY_BELOW_STARS;
    }
}
