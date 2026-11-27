import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> RouteBuilder</p>
 * <p><b>Purpose:</b> Creates buses and their routes: deals the stops out to the buses of a new map
 * (keeping low-priority stops off the Double Decker's route), and makes a single bus of a chosen
 * type for the manual editing tools.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/25/26
 */
public class RouteBuilder {
    public static final String STANDARD_BUS = StandardBus.TYPE_NAME;   // Display name of a standard bus
    public static final String MINI_BUS = MiniBus.TYPE_NAME;           // Display name of a mini bus
    public static final String DOUBLE_DECKER = DoubleDecker.TYPE_NAME; // Display name of a double decker

    // Shown whenever someone tries to put a low-priority stop on a Double Decker's route
    public static final String DOUBLE_DECKER_LOW_PRIORITY_MESSAGE =
            "A low priority stop can't be part of a Double Decker route.";

    /** The bus types, in the order a new map's buses are built. */
    public static final String[] BUS_TYPES = {STANDARD_BUS, MINI_BUS, DOUBLE_DECKER};
    private static final int NUMBER_OF_BUSES = BUS_TYPES.length;    // A new map always has one bus of each type
    private static final int DOUBLE_DECKER_SLOT = 2; // The slot that is built as the Double Decker

    /**
     * Builds the buses of a new map: deals every stop to a route, makes sure every route has at
     * least two stops, and creates a {@link StandardBus}, a {@link MiniBus} and a {@link DoubleDecker}
     * placed at the start of their routes and heading for their second stop.
     *
     * @param stops  the stops of the map
     * @param random the seeded random generator (used for the bus colors)
     * @return the new buses, in slot order
     */
    public List<Bus> createBuses(List<BusStop> stops, Random random) {
        List<ArrayList<Location>> routes = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            routes.add(new ArrayList<Location>());
        }

        // The Double Decker slot may only receive high-priority stops. Low-priority stops are dealt
        // between the other two slots; high-priority stops are dealt across all three.
        int nextSlotForLowPriority = 0;
        int nextSlotForHighPriority = 0;
        for (BusStop currentStop : stops) {
            Location currentStopLocation = currentStop.getLocation();

            if (currentStop.isHighPriority()) {
                routes.get(nextSlotForHighPriority % NUMBER_OF_BUSES).add(currentStopLocation);
                nextSlotForHighPriority++;
            } else {
                int lowPrioritySlots = NUMBER_OF_BUSES - 1;   // every slot except the DoubleDecker's
                routes.get(nextSlotForLowPriority % lowPrioritySlots).add(currentStopLocation);
                nextSlotForLowPriority++;
            }
        }

        // A route of exactly ONE stop would sit still forever (its only destination is where it
        // already is), so every slot needs at least two stops. The Double Decker can only borrow
        // high-priority stops; the other two slots can borrow from each other.
        while (routes.get(DOUBLE_DECKER_SLOT).size() < 2) {
            Location donorLocation = takeHighPriorityDonor(routes, stops);
            if (donorLocation != null) {
                routes.get(DOUBLE_DECKER_SLOT).add(donorLocation);
            } else if (!routes.get(DOUBLE_DECKER_SLOT).isEmpty()) {
                routes.get(DOUBLE_DECKER_SLOT).add(routes.get(DOUBLE_DECKER_SLOT).get(0));   // duplicate: a valid, if static, 2-stop route
                break;
            } else {
                break;   // no high-priority stops exist anywhere on the map - unusual but possible
            }
        }
        for (int slot = 0; slot < DOUBLE_DECKER_SLOT; slot++) {
            int otherSlot = 1 - slot;
            while (routes.get(slot).size() < 2) {
                if (routes.get(otherSlot).size() > 1) {
                    routes.get(slot).add(routes.get(otherSlot).remove(routes.get(otherSlot).size() - 1));
                } else if (!routes.get(slot).isEmpty()) {
                    routes.get(slot).add(routes.get(slot).get(0));   // duplicate: still a valid 2-stop route
                    break;
                } else {
                    break;   // unreachable given a map with at least 3 stops
                }
            }
        }

        // With very few stops (3 or 4) a bus can still be left with no stop, or with just one. It gets a
        // route that repeats a stop - a valid, if static, 2-stop route - so a bus is never built with
        // a route that is empty or too short to be saved and loaded again.
        for (int slot = 0; slot < NUMBER_OF_BUSES; slot++) {
            if (routes.get(slot).size() == 1) {
                routes.get(slot).add(routes.get(slot).get(0));
            }
            if (!routes.get(slot).isEmpty()) {
                continue;
            }
            for (BusStop stop : stops) {
                if (slot != DOUBLE_DECKER_SLOT || stop.isHighPriority()) {
                    routes.get(slot).add(stop.getLocation());
                    routes.get(slot).add(stop.getLocation());
                    break;
                }
            }
        }

        // Use golden-ratio hue spacing so bus colors remain visually distinct
        List<Bus> buses = new ArrayList<>();
        float firstHue = random.nextFloat();
        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            float hue = (float) ((firstHue + i * 0.618033988749895) % 1.0);
            Color busColor = Color.hsb(hue * 360, 0.80, 0.80);

            buses.add(createBus(BUS_TYPES[i], busColor, routes.get(i)));
        }

        return buses;
    }

    /**
     * Creates one bus of the named type.
     *
     * @param typeName one of {@link #STANDARD_BUS}, {@link #MINI_BUS} or {@link #DOUBLE_DECKER}
     * @param color    display color of the bus
     * @param route    the stops the bus visits, in order
     * @return the new bus
     */
    public Bus createBus(String typeName, Color color, ArrayList<Location> route) {
        if (typeName.equals(STANDARD_BUS)) {
            return new StandardBus(color, route);
        }

        if (typeName.equals(MINI_BUS)) {
            return new MiniBus(color, route);
        }

        return new DoubleDecker(color, route);
    }

    /**
     * Finds and removes a high-priority stop location from route slot 0 or 1 so it can be handed to
     * the Double Decker's route.
     *
     * @param routes   the routes of every bus slot
     * @param allStops all stops, used to look up priorities
     * @return the removed location, or {@code null} if slots 0 and 1 hold no high-priority stop
     */
    private Location takeHighPriorityDonor(List<ArrayList<Location>> routes, List<BusStop> allStops) {
        for (int slot = 0; slot < DOUBLE_DECKER_SLOT; slot++) {
            ArrayList<Location> candidate = routes.get(slot);
            for (int entryIndex = 0; entryIndex < candidate.size(); entryIndex++) {
                Location entryLocation = candidate.get(entryIndex);
                if (isHighPriorityAt(entryLocation, allStops)) {
                    candidate.remove(entryIndex);
                    return entryLocation;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the stop at a location is high priority.
     *
     * @param location the grid location to check
     * @param allStops the stops to search
     * @return {@code true} if a stop is there and it is high priority
     */
    private boolean isHighPriorityAt(Location location, List<BusStop> allStops) {
        for (BusStop stop : allStops) {
            if (stop.isAt(location)) {
                return stop.isHighPriority();
            }
        }
        return false;
    }
}
