/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> PhaseResult</p>
 * <p><b>Purpose:</b> Holds the final numbers of one finished simulation phase (average wait, total
 * distance driven, driving cost, passengers served) and compares them against another phase as percentages.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/25/26
 */
public class PhaseResult {
    /** Where satisfaction sits in {@link #improvementsOver}: it is a difference in stars, the others are percentages. */
    public static final int STARS_MEASURE = 1;

    private final double averageWaitMinutes;   // Average wait in minutes of the rated passengers: those who boarded and those still waiting (NaN if there are none)
    private final double distanceKm;            // Total distance driven by all buses over the phase, in km
    private final double totalCost;            // Total driving cost of all buses over the phase
    private final int passengersServed;        // Total number of passengers who boarded a bus
    private final double averageStars;         // Average passenger rating, 0 to 5 (NaN if nobody has been rated)

    /**
     * Constructs a {@code PhaseResult}.
     *
     * @param averageWaitMinutes average passenger wait in minutes
     * @param distanceKm          total distance driven by all buses over the phase, in kilometres
     * @param totalCost          total driving cost of all buses over the phase
     * @param passengersServed   total number of passengers who boarded
     * @param averageStars       average passenger rating from 0 to 5
     */
    public PhaseResult(double averageWaitMinutes, double distanceKm, double totalCost, int passengersServed,
                       double averageStars) {
        this.averageStars = averageStars;
        this.averageWaitMinutes = averageWaitMinutes;
        this.distanceKm = distanceKm;
        this.totalCost = totalCost;
        this.passengersServed = passengersServed;
    }

    /**
     * Returns the average passenger wait.
     *
     * @return average wait in minutes, or {@link Double#NaN} if nobody boarded
     */
    public double getAverageWaitMinutes() {
        return averageWaitMinutes;
    }

    /**
     * Returns the total distance driven.
     *
     * @return kilometres driven by all buses over the phase
     */
    public double getTotalDistanceKm() {
        return distanceKm;
    }

    /**
     * Returns the total driving cost.
     *
     * @return cost of driving all buses over the phase
     */
    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Returns the number of passengers served.
     *
     * @return total boarded passengers
     */
    public int getPassengersServed() {
        return passengersServed;
    }

    /**
     * Returns the average passenger rating.
     *
     * @return stars from 0 to 5, or {@link Double#NaN} if nobody has been rated
     */
    public double getAverageStars() {
        return averageStars;
    }

    /**
     * Returns the driving cost for each passenger carried.
     *
     * @return total cost divided by passengers served, or {@link Double#NaN} if nobody boarded
     */
    public double getCostPerPassenger() {
        if (passengersServed == 0) {
            return Double.NaN;
        }

        return totalCost / passengersServed;
    }

    /**
     * Compares this phase with a baseline on every measure. The wait, distance, cost and cost per passenger are
     * percentages (positive means lower, so better); satisfaction is the difference in stars (positive means
     * happier passengers).
     *
     * @param baseline the phase to compare against (normally Phase 1)
     * @return the improvements, in the order wait, satisfaction, distance, cost, cost per passenger (a measure
     *         that cannot be worked out is {@link Double#NaN})
     */
    public double[] improvementsOver(PhaseResult baseline) {
        return new double[]{
                percentImprovement(baseline.averageWaitMinutes, averageWaitMinutes),
                averageStars - baseline.averageStars,
                percentImprovement(baseline.distanceKm, distanceKm),
                percentImprovement(baseline.totalCost, totalCost),
                percentImprovement(baseline.getCostPerPassenger(), getCostPerPassenger())};
    }

    /**
     * Converts a before/after pair into a percentage reduction relative to {@code before}.
     *
     * @param before the baseline value
     * @param after  the new value
     * @return {@code (before - after) / before * 100}, or {@link Double#NaN} if it cannot be computed
     */
    private double percentImprovement(double before, double after) {
        if (Double.isNaN(before) || Double.isNaN(after) || before == 0) {
            return Double.NaN;
        }

        return (before - after) / before * 100.0;
    }
}
