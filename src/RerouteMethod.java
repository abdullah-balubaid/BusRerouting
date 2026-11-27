
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> RerouteMethod</p>
 * <p><b>Purpose:</b> The two ways Phase 2 can decide on a reroute. Both use the same idea, a score for every
 * (stop, bus) pair where the best positive score wins, and differ only in their weights and in which
 * stops they consider at all:</p>
 * <ul>
 *   <li><b>Service-first</b> looks after the passengers who have waited too long: a stop is considered only
 *   once its oldest passenger has waited longer than the acceptable wait, and the longer the overdue wait
 *   the better.</li>
 *   <li><b>Cost-saving</b> looks after the distance driven per passenger carried: buses with nobody on board wait where they are until
 *   a few people are waiting at their next stop, and a stop is considered for a reroute only if enough people are waiting to fill a good
 *   share of a bus, with the score being the passengers the bus would pick up against the distance to them.</li>
 * </ul>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public enum RerouteMethod {
    /** Chase the stops whose passengers have waited longest. */
    SERVICE_FIRST("Service-first", 1.0, 0.5, 1.0, 0, true, false, 0),
    /** Chase the biggest crowds that are cheap to reach. */
    COST_SAVING("Cost-saving", 0.0, 1.0, 0.5, RerouteMethod.COST_SAVING_MIN_CROWD, false, true,
            RerouteMethod.COST_SAVING_DEPART_AT);

    /** A stop needs this many people waiting before cost-saving sends a bus off its route to it. */
    private static final int COST_SAVING_MIN_CROWD = 8;
    /** A cost-saving bus with nobody on board waits until this many people are waiting at its next stop. */
    private static final int COST_SAVING_DEPART_AT = 5;

    private final String displayName;       // Shown on buttons and in the log
    private final double overdueWeight;     // Score per minute the oldest passenger is overdue
    private final double passengerWeight;   // Score per passenger counted
    private final double distanceWeight;    // Score lost per grid cell driven
    private final int minPassengers;        // Fewest waiting passengers a stop needs to be considered
    private final boolean needsOverdue;     // Whether the stop must be overdue to be considered
    private final boolean costAware;        // Whether only passengers that fit are counted
    private final int minWaitingToDepart;   // An empty bus waits until this many people wait at its next stop (0 = never waits)

    RerouteMethod(String displayName, double overdueWeight, double passengerWeight, double distanceWeight,
                  int minPassengers, boolean needsOverdue, boolean costAware, int minWaitingToDepart) {
        this.displayName = displayName;
        this.overdueWeight = overdueWeight;
        this.passengerWeight = passengerWeight;
        this.distanceWeight = distanceWeight;
        this.minPassengers = minPassengers;
        this.needsOverdue = needsOverdue;
        this.costAware = costAware;
        this.minWaitingToDepart = minWaitingToDepart;
    }

    /**
     * Returns the method's name.
     *
     * @return text such as {@code "Service-first"}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns how many people must be waiting at its next stop before a bus with nobody on board drives
     * there. Waiting saves kilometres but makes the first people to arrive wait longer.
     *
     * @return the number of waiting people, or 0 if an empty bus never waits
     */
    public int getMinWaitingToDepart() {
        return minWaitingToDepart;
    }

    /**
     * Tells whether a stop is worth sending a bus to at all.
     *
     * @param overdueMinutes how far past the acceptable wait the stop's oldest passenger is (negative if not yet)
     * @param waiting        passengers waiting at the stop
     * @return {@code true} if the stop may be considered
     */
    public boolean considers(double overdueMinutes, int waiting) {
        return (!needsOverdue || overdueMinutes > 0) && waiting >= minPassengers;
    }

    /**
     * Scores sending a bus to a stop.
     *
     * @param overdueMinutes how far past the acceptable wait the stop's oldest passenger is
     * @param waiting        passengers waiting at the stop
     * @param bus            the bus that would go
     * @param distance       grid cells the bus would have to drive
     * @return the score (only a positive score is worth acting on)
     */
    public double score(double overdueMinutes, int waiting, Bus bus, double distance) {
        int counted = costAware ? Math.min(waiting, bus.getPassengerCapacity() - bus.getCurrentPassengers()) : waiting;
        double distanceCost = distance * distanceWeight;
        return overdueWeight * overdueMinutes + passengerWeight * counted - distanceCost;
    }
}
