import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BusNetwork</p>
 * <p><b>Purpose:</b> The bus stops and the buses that serve them, as one network. It makes random
 * networks from a seed, moves the buses and brings passengers to the stops, lets the
 * {@link Dispatcher} reroute buses, adds up how well the network is doing, and carries out every
 * edit made by hand (used by Build Mode and by loading a saved build). It knows nothing about
 * the screen, so the batch test can run many networks without drawing any of them.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class BusNetwork {
    private int minWaitingToDepart;   // An empty bus waits until this many people wait at its next stop; 0 = never waits (set per run)
    private static final double LOADING_SECONDS = Scale.STOP_DWELL_MINUTES / Scale.SIMULATED_MINUTES_PER_SECOND;   // Simulated seconds a bus stays at a stop
    private static final double HIGH_PRIORITY_SHARE = 0.45;          // About this share of generated stops are high priority
    private static final int MIN_HIGH_PRIORITY_STOPS = 2;            // A generated map always has at least this many, for the Double Decker's route

    private final List<BusStop> stops = new ArrayList<>();
    private final List<Bus> buses = new ArrayList<>();
    private final RouteBuilder routeBuilder = new RouteBuilder();   // Makes the buses
    private final Dispatcher dispatcher = new Dispatcher();         // Decides which bus is rerouted to which stop
    private Random random = new Random();                           // Re-seeded whenever a map is generated
    private BiConsumer<LogKind, String> eventLog = (kind, message) -> { };   // Told about arrivals and reroutes (silent by default)

    /**
     * Sets who is told about events such as a bus arriving at a stop or being rerouted. Leave it unset
     * for a silent network.
     *
     * @param eventLog receiver of event text, or {@code null} for none
     */
    public void setEventLog(BiConsumer<LogKind, String> eventLog) {
        this.eventLog = (eventLog == null) ? (kind, message) -> { } : eventLog;
    }

    /**
     * Returns the stops (the list cannot be changed from outside).
     *
     * @return the stops in order
     */
    public List<BusStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    /**
     * Returns the buses (the list cannot be changed from outside).
     *
     * @return the buses in order
     */
    public List<Bus> getBuses() {
        return Collections.unmodifiableList(buses);
    }

    // ------------------------------------------------------------------ making a network

    /**
     * Replaces the network with a new random one. The same seed always gives the same stops, crowds,
     * arrival rates, bus routes and bus colours.
     *
     * @param seed      the seed to generate from
     * @param stopCount number of stops to place on distinct intersections
     */
    public void generate(long seed, int stopCount) {
        random = new Random(seed);
        stops.clear();

        List<int[]> intersections = new ArrayList<>();
        for (int x = 0; x <= Scale.GRID_SIZE; x++) {
            for (int y = 0; y <= Scale.GRID_SIZE; y++) {
                intersections.add(new int[]{x, y});
            }
        }
        Collections.shuffle(intersections, random);

        for (int i = 0; i < stopCount; i++) {
            int[] point = intersections.get(i);
            int crowd = 3 + random.nextInt(10);
            boolean highPriority = random.nextDouble() < HIGH_PRIORITY_SHARE;

            BusStop stop = highPriority ? new HighPriorityStop(point[0], point[1])
                                        : new LowPriorityStop(point[0], point[1]);
            stop.setStartingCrowd(crowd);
            stops.add(stop);
        }

        promoteToHighPriorityIfTooFew();

        for (BusStop stop : stops) {
            stop.setArrivalRate(randomArrivalRate());
        }

        buses.clear();
        buses.addAll(routeBuilder.createBuses(stops, random));
        renumber();
        restoreStartingState();
    }

    /**
     * The Double Decker only serves high priority stops and needs at least two for a route, so a map
     * that came out with fewer gets its first low priority stops promoted.
     */
    private void promoteToHighPriorityIfTooFew() {
        int highCount = getHighPriorityStopCount();

        for (int i = 0; i < stops.size() && highCount < MIN_HIGH_PRIORITY_STOPS; i++) {
            if (!stops.get(i).isHighPriority()) {
                stops.set(i, withPriority(stops.get(i), true));
                highCount++;
            }
        }
    }

    /**
     * Counts the high priority stops.
     *
     * @return how many stops are high priority
     */
    public int getHighPriorityStopCount() {
        int count = 0;
        for (BusStop stop : stops) {
            count += stop.isHighPriority() ? 1 : 0;
        }
        return count;
    }

    /**
     * Replaces the contents of the network (used when a saved build is loaded).
     *
     * @param newStops the stops of the new network
     * @param newBuses the buses of the new network
     */
    public void replaceWith(List<BusStop> newStops, List<Bus> newBuses) {
        stops.clear();
        stops.addAll(newStops);
        buses.clear();
        buses.addAll(newBuses);
        renumber();
        restoreStartingState();
    }

    /**
     * Chooses how many people must wait at its next stop before a bus with nobody on board drives there (cost-saving runs).
     *
     * @param minWaiting the number of waiting people; 0 means empty buses never wait
     */
    public void setMinWaitingToDepart(int minWaiting) {
        minWaitingToDepart = minWaiting;
    }

    /**
     * Puts every bus back at the start of its route and every stop back to its starting crowd, with
     * all wait statistics and driving distances cleared. Both phases start from this state, so
     * Phase 2 neither inherits Phase 1's numbers nor starts from where Phase 1 ended.
     */
    public void restoreStartingState() {
        for (Bus bus : buses) {
            bus.resetForNewRun();
        }
        for (BusStop stop : stops) {
            stop.resetForNewRun();
        }
    }

    /**
     * Gives every stop and bus the number that matches its place in the lists, so names such as
     * "Stop 3" and "S1" stay right after anything is added or deleted.
     */
    private void renumber() {
        for (int i = 0; i < stops.size(); i++) {
            stops.get(i).setNumber(i + 1);
        }
        for (int i = 0; i < buses.size(); i++) {
            buses.get(i).setNumber(i + 1);
        }
    }

    /**
     * Describes the network (stops and bus routes) as text in the {@link MapFile} format.
     *
     * @return the build text
     */
    public String toBuildText() {
        return MapFile.toText(stops, buses);
    }

    /**
     * Replaces the network with a saved build. The text is checked completely first, so if it is wrong
     * the network is left untouched.
     *
     * @param text the build text
     * @throws MapFileException if the text is not a valid build; the message says which line to fix
     */
    public void loadBuildText(String text) throws MapFileException {
        MapFile loaded = MapFile.parse(text, routeBuilder);
        replaceWith(loaded.getStops(), loaded.getBuses());
    }

    // ------------------------------------------------------------------ running

    /**
     * Moves the network forward in time: passengers arrive at every stop, and every bus that is not
     * loading drives toward its destination and, on arriving, swaps passengers and sets off for its
     * next stop. A bus that finishes a reroute goes back to the stop it was heading for.
     *
     * @param seconds               simulated seconds to advance
     * @param nowInSimulatedSeconds simulated clock time at the start of this step
     */
    public void advance(double seconds, double nowInSimulatedSeconds) {
        for (BusStop stop : stops) {
            stop.advancePassengers(seconds, nowInSimulatedSeconds);
        }

        for (Bus bus : buses) {
            bus.rememberPosition();
            if (bus.advanceLoading(seconds)) {
                continue;
            }

            boolean held = isHeldBack(bus);
            if (held != bus.isHeldBack()) {
                logHold(bus, held);
            }
            bus.setHeldBack(held);
            if (held) {
                continue;
            }

            bus.moveTowardDestination(Scale.cellsPerSimulatedSecond(bus.getSpeed()) * seconds);

            if (bus.isMoving()) {
                continue;
            }

            // Asked BEFORE arriving: was this stop a reroute detour, or the bus's own next route stop?
            boolean finishedReroute = bus.isRerouting();
            arrive(bus, bus.getDestination(), finishedReroute, nowInSimulatedSeconds);
            bus.headForNextStop(finishedReroute);
        }
    }

    /**
     * In a run that holds empty buses, a bus with nobody on board stays where it is until enough people are waiting at
     * the stop it is heading for, which saves the kilometres it would have driven.
     *
     * @param bus the bus to check
     * @return {@code true} if the bus stays put this step
     */
    private boolean isHeldBack(Bus bus) {
        if (minWaitingToDepart == 0 || bus.isRerouting() || bus.getCurrentPassengers() > 0) {
            return false;
        }

        BusStop next = findStopAt(bus.getNextScheduledStop());
        return next != null && bus.canServe(next) && next.getWaitingPassengers() < minWaitingToDepart;
    }

    /**
     * Writes to the event log that a cost-saving bus has started or stopped waiting for passengers, so a bus
     * standing still is not mistaken for a fault. A bus that stops waiting because it was rerouted has its own log line.
     *
     * @param bus     the bus
     * @param nowHeld {@code true} if it has just started waiting, {@code false} if it has just set off
     */
    private void logHold(Bus bus, boolean nowHeld) {
        BusStop next = findStopAt(bus.getNextScheduledStop());
        if (next == null || (!nowHeld && bus.isRerouting())) {
            return;
        }

        eventLog.accept(LogKind.INFO, nowHeld
                ? bus.getName() + " waits until " + minWaitingToDepart + " people are waiting at " + next.getName() + "."
                : bus.getName() + " sets off for " + next.getName() + " (" + next.getWaitingPassengers() + " waiting).");
    }

    /**
     * Handles a bus arriving at a stop: checks that it can serve it, swaps the passengers and starts
     * the loading pause.
     *
     * @param bus                   the arriving bus
     * @param stopLocation          where it arrived
     * @param finishedReroute       whether this stop was a reroute detour
     * @param nowInSimulatedSeconds simulated clock time
     */
    private void arrive(Bus bus, Location stopLocation, boolean finishedReroute, double nowInSimulatedSeconds) {
        BusStop stop = findStopAt(stopLocation);
        if (stop == null) {
            return;
        }

        // A bus that can't serve this stop (a DoubleDecker at a low priority stop) gets no boarding at
        // all: everyone stays on board, nobody waiting is picked up, and the bus does not pause to load.
        if (!bus.canServe(stop)) {
            eventLog.accept(LogKind.PASSED, bus.getName() + " passed " + stop.getName() + " without stopping (not servable by this bus type).");
        } else {
            int gettingOff = bus.getCurrentPassengers();
            int gettingOn = bus.arriveAt(stop, LOADING_SECONDS, nowInSimulatedSeconds);
            eventLog.accept(LogKind.ARRIVAL, bus.getName() + " at " + stop.getName() + ": " + gettingOff + " got off, "
                    + gettingOn + " got on, " + stop.getWaitingPassengers() + " still waiting");
        }

        if (finishedReroute) {
            eventLog.accept(LogKind.REROUTE, bus.getName() + " finished its reroute to " + stop.getName() + ".");
        }
    }

    /**
     * Phase 2 rerouting: asks the {@link Dispatcher} for the best pairing of a stop and an eligible bus
     * under the chosen method and, if there is one, sends that bus to that stop.
     *
     * @param nowInSimulatedMinutes simulated clock time in minutes
     * @param acceptableWaitMinutes the wait after which a stop counts as overdue
     * @param method                how the dispatcher scores the pairings
     */
    public void reroute(double nowInSimulatedMinutes, double acceptableWaitMinutes, RerouteMethod method) {
        Dispatcher.Dispatch dispatch = dispatcher.chooseDispatch(stops, buses, nowInSimulatedMinutes,
                acceptableWaitMinutes, method);

        if (dispatch == null) {
            return;
        }

        ((Reroutable) dispatch.getBus()).reRoute(dispatch.getStop().getLocation());
        eventLog.accept(LogKind.REROUTE, String.format("t=%.0fm: Rerouted %s -> %s (%.1f cells away, %d waiting, score %.1f)",
                nowInSimulatedMinutes, dispatch.getBus().getName(), dispatch.getStop().getName(),
                dispatch.getDistance(), dispatch.getStop().getWaitingPassengers(), dispatch.getScore()));
    }

    // ------------------------------------------------------------------ how well it is doing

    /**
     * Adds up the driving cost of every bus in the current run.
     *
     * @return total driving cost
     */
    public double getTotalCost() {
        double total = 0;
        for (Bus bus : buses) {
            total += bus.getTravelCost();
        }
        return total;
    }

    /**
     * Adds up the distance driven by every bus in the current run.
     *
     * @return total distance in kilometres
     */
    public double getTotalDistanceKm() {
        double total = 0;
        for (Bus bus : buses) {
            total += Scale.cellsToKm(bus.getDistanceTravelled());
        }
        return total;
    }

    /**
     * Adds up the passengers who have boarded a bus at any stop in the current run.
     *
     * @return total boarded passengers
     */
    public int getTotalBoarded() {
        int total = 0;
        for (BusStop stop : stops) {
            total += stop.getTotalBoarded();
        }
        return total;
    }

    /**
     * Bundles how the network is doing right now: average wait, driving cost, passengers served and
     * how satisfied the passengers are.
     *
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @param acceptableWaitMinutes the wait that still earns 5 stars
     * @return the current figures
     */
    public PhaseResult getCurrentResult(double nowInSimulatedSeconds, double acceptableWaitMinutes) {
        double starSum = 0;
        double waitSum = 0;
        int rated = 0;
        for (StopSatisfaction stop : getSatisfaction(nowInSimulatedSeconds, acceptableWaitMinutes)) {
            starSum += stop.getStarSum();
            waitSum += stop.getWaitSumMinutes();
            rated += stop.getPassengers();
        }
        double averageStars = (rated == 0) ? Double.NaN : starSum / rated;
        double averageWait = (rated == 0) ? Double.NaN : waitSum / rated;
        return new PhaseResult(averageWait, getTotalDistanceKm(), getTotalCost(), getTotalBoarded(), averageStars);
    }

    /**
     * Rates the passengers of every stop.
     *
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @param acceptableWaitMinutes the wait that still earns 5 stars
     * @return one set of figures per stop, in stop order
     */
    public List<StopSatisfaction> getSatisfaction(double nowInSimulatedSeconds, double acceptableWaitMinutes) {
        List<StopSatisfaction> result = new ArrayList<>();
        for (BusStop stop : stops) {
            result.add(stop.getSatisfaction(nowInSimulatedSeconds, acceptableWaitMinutes));
        }
        return result;
    }

    // ------------------------------------------------------------------ looking things up

    /**
     * Finds the stop at a location.
     *
     * @param location where to look
     * @return the stop, or {@code null} if there is none there
     */
    public BusStop findStopAt(Location location) {
        for (BusStop stop : stops) {
            if (stop.isAt(location)) {
                return stop;
            }
        }
        return null;
    }

    /**
     * Finds the stop at a grid intersection.
     *
     * @param gridX grid column
     * @param gridY grid row
     * @return the stop, or {@code null} if there is none there
     */
    public BusStop findStopAt(int gridX, int gridY) {
        return findStopAt(new Location(gridX, gridY));
    }

    /**
     * Describes a bus for the bus list: its name and load, and where it is heading.
     *
     * @param bus the bus to describe
     * @return text such as {@code "S1 12/30  on route -> Stop 3"}
     */
    public String describe(Bus bus) {
        BusStop destination = findStopAt(bus.getDestination());
        String where = (destination == null) ? "?" : destination.getName();
        return bus + "  " + (bus.isRerouting() ? "REROUTING" : bus.isHeldBack() ? "waiting for passengers" : "on route") + " -> " + where;
    }

    /**
     * Describes the route of a bus as stop numbers.
     *
     * @param bus the bus
     * @return text such as {@code "1 → 3 → 5"}
     */
    public String describeRoute(Bus bus) {
        StringBuilder text = new StringBuilder();
        for (Location location : bus.getRoute()) {
            BusStop stop = findStopAt(location);
            text.append(text.length() == 0 ? "" : " → ").append(stop == null ? "?" : stop.getName().replace("Stop ", ""));
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ editing (used by Build Mode)

    /**
     * Adds a stop at an intersection with a random arrival rate and a small starting crowd (smaller
     * than a freshly generated map's).
     *
     * @param gridX        grid column
     * @param gridY        grid row
     * @param highPriority {@code true} for a high priority stop
     * @return the new stop
     */
    public BusStop addStop(int gridX, int gridY, boolean highPriority) {
        BusStop stop = highPriority ? new HighPriorityStop(gridX, gridY) : new LowPriorityStop(gridX, gridY);
        stop.setArrivalRate(randomArrivalRate());
        stop.setStartingCrowd(1 + random.nextInt(6));
        stop.resetForNewRun();
        stops.add(stop);
        renumber();
        return stop;
    }

    /**
     * Deletes a stop. Buses whose routes pass through it simply skip it afterwards.
     *
     * @param stop the stop to delete
     */
    public void deleteStop(BusStop stop) {
        stops.remove(stop);
        renumber();
    }

    /**
     * Switches a stop between high and low priority, keeping its place, arrival rate and starting
     * crowd. A stop on a Double Decker's route cannot become low priority; check
     * {@link #isOnDoubleDeckerRoute(BusStop)} first.
     *
     * @param stop the stop to switch
     * @return the replacement stop
     */
    public BusStop togglePriority(BusStop stop) {
        int index = stops.indexOf(stop);
        BusStop replacement = withPriority(stop, !stop.isHighPriority());
        stops.set(index, replacement);
        renumber();
        return replacement;
    }

    /**
     * Tells whether a Double Decker's route passes through a stop.
     *
     * @param stop the stop to check
     * @return {@code true} if some Double Decker visits it
     */
    public boolean isOnDoubleDeckerRoute(BusStop stop) {
        for (Bus bus : buses) {
            if (bus instanceof DoubleDecker && bus.getRoute().contains(stop.getLocation())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes a bus of the given type (it is not added to the network yet).
     *
     * @param typeName one of the {@link RouteBuilder} type names
     * @param color    the bus colour
     * @param route    the stops it visits, in order (at least 2)
     * @return the bus, standing at the start of its route
     */
    public Bus createBus(String typeName, Color color, ArrayList<Location> route) {
        Bus bus = routeBuilder.createBus(typeName, color, route);
        bus.resetForNewRun();
        return bus;
    }

    /**
     * Picks a random bus colour for a bus added by hand.
     *
     * @return the colour
     */
    public Color randomBusColor() {
        return Color.hsb(random.nextFloat() * 360, 0.80, 0.80);
    }

    /**
     * Adds a bus.
     *
     * @param bus the bus to add
     */
    public void addBus(Bus bus) {
        buses.add(bus);
        renumber();
    }

    /**
     * Replaces a bus by another at the same place in the list (a route is edited by replacing the bus).
     *
     * @param oldBus the bus to replace
     * @param newBus the bus that takes its place
     */
    public void replaceBus(Bus oldBus, Bus newBus) {
        int index = buses.indexOf(oldBus);
        if (index != -1) {
            buses.set(index, newBus);
            renumber();
        }
    }

    /**
     * Deletes a bus.
     *
     * @param bus the bus to delete
     */
    public void deleteBus(Bus bus) {
        buses.remove(bus);
        renumber();
    }

    /**
     * Removes every stop and every bus.
     */
    public void clear() {
        stops.clear();
        buses.clear();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Makes a stop of the other priority at the same place, with the same arrival rate and starting crowd.
     *
     * @param stop         the stop to copy
     * @param highPriority the priority the copy gets
     * @return the new stop
     */
    private BusStop withPriority(BusStop stop, boolean highPriority) {
        BusStop copy = highPriority ? new HighPriorityStop(stop.getLocation()) : new LowPriorityStop(stop.getLocation());
        copy.setArrivalRate(stop.getArrivalRate());
        copy.setStartingCrowd(stop.getStartingCrowd());
        copy.resetForNewRun();
        return copy;
    }

    /**
     * Draws a random passenger arrival rate.
     *
     * @return a rate between the minimum and maximum arrivals per hour, as passengers per simulated second
     */
    private double randomArrivalRate() {
        return Scale.perSimulatedSecond(Scale.MIN_ARRIVALS_PER_HOUR
                + (Scale.MAX_ARRIVALS_PER_HOUR - Scale.MIN_ARRIVALS_PER_HOUR) * random.nextDouble());
    }
}
