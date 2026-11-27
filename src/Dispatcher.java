import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Dispatcher</p>
 * <p><b>Purpose:</b> Decides Phase 2 reroutes. Every pairing of an overdue bus stop and an eligible
 * bus is given a score, and the best positive score wins. A reroute therefore always needs both a
 * stop that needs help and a bus that can reasonably go there.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.1 11/26/26
 */
public class Dispatcher {
    // The weights of the score belong to the RerouteMethod chosen; these limits apply to every method.

    // A bus never abandons its own route to chase a stop further away than this many grid cells
    private static final double MAX_REROUTE_DISTANCE = 12.0;

    // A bus is only pulled off its route if its own next scheduled stop has at most this many people
    // waiting, so its own passengers are picked up first. 0 means "nobody at all", which in testing
    // almost never happened, so nothing was ever rerouted. Raise it for more reroutes, lower it to be stricter.
    private static final int NEXT_STOP_WAITING_LIMIT = 4;

    /**
     * The outcome of a successful dispatch decision: which bus goes to which stop, and why.
     */
    public static class Dispatch {
        private final Bus bus;               // The chosen bus
        private final BusStop stop;          // The stop it is sent to
        private final double score;          // Score that won the comparison
        private final double distance;       // Grid distance the bus must drive to the stop

        /**
         * Constructs a {@code Dispatch}.
         *
         * @param bus            the chosen bus
         * @param stop           the target stop
         * @param score          the winning score
         * @param distance       grid distance from the bus to the stop
         */
        public Dispatch(Bus bus, BusStop stop, double score, double distance) {
            this.bus = bus;
            this.stop = stop;
            this.score = score;
            this.distance = distance;
        }

        /**
         * @return the chosen bus
         */
        public Bus getBus() {
            return bus;
        }

        /**
         * @return the target stop
         */
        public BusStop getStop() {
            return stop;
        }

        /**
         * @return the winning score
         */
        public double getScore() {
            return score;
        }

        /**
         * @return grid distance from the bus to the stop
         */
        public double getDistance() {
            return distance;
        }
    }

    /**
     * Picks the best (stop, eligible bus) pairing for a {@link RerouteMethod}, or nothing if no pairing is worth it.
     *
     * @param stops               all bus stops
     * @param buses               all buses
     * @param nowInSimulatedMin   current simulated time in minutes
     * @param acceptableWaitMinutes the wait after which a stop counts as overdue
     * @param method              which weights and rules to score with
     * @return the best {@link Dispatch}, or {@code null} if nothing should be rerouted right now
     */
    public Dispatch chooseDispatch(List<BusStop> stops, List<Bus> buses, double nowInSimulatedMin,
                                   double acceptableWaitMinutes, RerouteMethod method) {
        Dispatch best = null;

        for (BusStop stop : stops) {

            if (isTargetedByAnyBus(stop, buses)) {
                continue;
            }

            double overdueMinutes = stop.getOldestWaitSeconds(nowInSimulatedMin * 60.0) / 60.0
                    - acceptableWaitMinutes;
            if (!method.considers(overdueMinutes, stop.getWaitingPassengers())) {
                continue;
            }

            for (Bus bus : buses) {
                if (!isEligible(bus, stop, stops)) {
                    continue;
                }

                double distance = bus.getLocation().gridDistanceTo(stop.getLocation());
                if (distance > MAX_REROUTE_DISTANCE) {
                    continue;
                }

                double score = method.score(overdueMinutes, stop.getWaitingPassengers(), bus, distance);
                boolean beatsBest = best == null || score > best.getScore();
                if (score > 0 && beatsBest) {
                    best = new Dispatch(bus, stop, score, distance);
                }
            }
        }

        return best;
    }

    /**
     * Checks whether any bus is already driving to the given stop.
     *
     * @param stop  the stop to check
     * @param buses all buses
     * @return {@code true} if some bus's current destination is this stop
     */
    private boolean isTargetedByAnyBus(BusStop stop, List<Bus> buses) {
        for (Bus bus : buses) {
            if (stop.isAt(bus.getDestination())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether a bus may be pulled off its route to serve {@code target}. The bus must be
     * able to be rerouted, not already be on a reroute, be allowed to serve the stop, and have room
     * for more passengers. Its own next scheduled stop must also be nearly clear (at most
     * {@link #NEXT_STOP_WAITING_LIMIT} waiting), so passengers on a bus's own route are picked up first.
     *
     * @param bus             the bus to check
     * @param target          the stop it would be sent to
     * @param stops           all bus stops
     * @return {@code true} if the bus is eligible
     */
    private boolean isEligible(Bus bus, BusStop target, List<BusStop> stops) {
        boolean basicallyEligible = bus instanceof Reroutable
                && !bus.isFull()
                && !bus.isRerouting()
                && bus.canServe(target);
        if (!basicallyEligible) {
            return false;
        }

        Location nextScheduled = bus.getNextScheduledStop();
        for (BusStop stop : stops) {
            if (stop.isAt(nextScheduled)) {
                return stop.getWaitingPassengers() <= NEXT_STOP_WAITING_LIMIT;
            }
        }

        return true;
    }
}
