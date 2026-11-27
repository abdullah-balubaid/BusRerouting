import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> MapFile</p>
 * <p><b>Purpose:</b> Turns a build (every stop and every bus route) into plain text and back, so a build
 * can be saved to a {@code .busmap} file or copied and pasted to share it. Reading is all-or-nothing:
 * the text is fully checked first, and a {@link MapFileException} is thrown if anything is wrong, so a
 * bad file can never leave a half-loaded map.</p>
 *
 * <p>The text looks like this (lines starting with {@code #} are ignored):</p>
 * <pre>
 * BUSMAP 1
 * stop,12,3,HIGH,0.2731,8        x, y, priority, arrival rate, starting crowd
 * stop,5,14,LOW,0.4102,5
 * bus,STANDARD,#1e90ff,1 2       type, color, route as stop numbers
 * </pre>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class MapFile {
    private static final String HEADER = "BUSMAP 1";   // First line of every file
    private static final String[] TYPE_WORDS = {"STANDARD", "MINI", "DOUBLE"};   // Bus type words used in the text, in the order of RouteBuilder.BUS_TYPES

    private final List<BusStop> stops = new ArrayList<>();           // Stops read from the text
    private final List<Bus> buses = new ArrayList<>();               // Buses read from the text

    /**
     * Writes a build as text.
     *
     * @param stops the stops of the map
     * @param buses the buses of the map
     * @return the text, ready to save or copy
     */
    public static String toText(List<BusStop> stops, List<Bus> buses) {
        StringBuilder text = new StringBuilder(HEADER).append('\n');

        for (int i = 0; i < stops.size(); i++) {
            BusStop stop = stops.get(i);
            Location location = stop.getLocation();
            text.append("stop,").append((int) Math.round(location.getX()))
                .append(',').append((int) Math.round(location.getY()))
                .append(',').append(stop.isHighPriority() ? "HIGH" : "LOW")
                .append(',').append(stop.getArrivalRate())        // full precision, so results repeat exactly
                .append(',').append(stop.getStartingCrowd()).append('\n');
        }

        for (Bus bus : buses) {
            Color color = bus.getColor();
            text.append("bus,").append(typeWord(bus.getTypeName()))
                .append(',').append(String.format("#%02x%02x%02x", Math.round(color.getRed() * 255),
                        Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255)))
                .append(',');

            ArrayList<Location> route = bus.getRoute();
            boolean first = true;
            for (Location stopLocation : route) {
                int stopIndex = indexOfStopAt(stops, stopLocation);
                if (stopIndex == -1) {
                    continue;   // the stop was deleted; the route simply skips it
                }
                if (!first) {
                    text.append(' ');
                }
                text.append(stopIndex + 1);
                first = false;
            }
            text.append('\n');
        }

        return text.toString();
    }

    /**
     * Reads and fully checks build text.
     *
     * @param text         the text of a saved or pasted build
     * @param routeBuilder makes the buses
     * @return the checked build
     * @throws MapFileException if the text is not a valid build; nothing is created in that case
     */
    public static MapFile parse(String text, RouteBuilder routeBuilder) throws MapFileException {
        if (text == null) {
            throw new MapFileException("There is no text to read.");
        }

        String[] lines = text.split("\\R", -1);
        MapFile result = new MapFile();
        List<String[]> busLines = new ArrayList<>();      // bus lines are checked after all stops are known
        List<Integer> busLineNumbers = new ArrayList<>();
        boolean headerSeen = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (!headerSeen) {
                if (!line.equals(HEADER)) {
                    throw new MapFileException(lineNumber, "this is not a bus map - the first line must be \"" + HEADER + "\".");
                }
                headerSeen = true;
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts[0].equals("stop")) {
                result.readStop(parts, lineNumber);
            } else if (parts[0].equals("bus")) {
                busLines.add(parts);
                busLineNumbers.add(lineNumber);
            } else {
                throw new MapFileException(lineNumber, "unknown line type \"" + parts[0] + "\" (expected stop or bus).");
            }
        }

        if (!headerSeen) {
            throw new MapFileException("This is not a bus map - it is empty or missing the \"" + HEADER + "\" line.");
        }

        for (int i = 0; i < busLines.size(); i++) {
            result.readBus(busLines.get(i), busLineNumbers.get(i), routeBuilder);
        }
        return result;
    }

    /**
     * Returns the stops that were read.
     *
     * @return a copy of the stop list
     */
    public List<BusStop> getStops() {
        return new ArrayList<>(stops);
    }

    /**
     * Returns the buses that were read.
     *
     * @return a copy of the bus list
     */
    public List<Bus> getBuses() {
        return new ArrayList<>(buses);
    }

    /**
     * Checks and stores one {@code stop} line.
     *
     * @param parts      the line split at the commas
     * @param lineNumber 1-based line number, for error messages
     * @throws MapFileException if the line is wrong
     */
    private void readStop(String[] parts, int lineNumber) throws MapFileException {
        if (parts.length != 6) {
            throw new MapFileException(lineNumber, "a stop needs 5 values: x, y, priority, arrival rate, starting crowd.");
        }

        int x = readInt(parts[1], "x", lineNumber);
        int y = readInt(parts[2], "y", lineNumber);
        if (x < 0 || y < 0 || x > Scale.GRID_SIZE || y > Scale.GRID_SIZE) {
            throw new MapFileException(lineNumber, "the stop at (" + x + ", " + y + ") is outside the grid (0 to "
                    + Scale.GRID_SIZE + ").");
        }
        for (BusStop other : stops) {
            if (other.isAt(new Location(x, y))) {
                throw new MapFileException(lineNumber, "there is already a stop at (" + x + ", " + y + ").");
            }
        }

        String priority = parts[3].trim();
        if (!priority.equals("HIGH") && !priority.equals("LOW")) {
            throw new MapFileException(lineNumber, "priority must be HIGH or LOW, not \"" + priority + "\".");
        }

        double rate;
        try {
            rate = Double.parseDouble(parts[4].trim());
        } catch (NumberFormatException e) {
            throw new MapFileException(lineNumber, "\"" + parts[4].trim() + "\" is not a valid arrival rate.");
        }
        if (Double.isNaN(rate) || Double.isInfinite(rate) || rate < 0) {
            throw new MapFileException(lineNumber, "the arrival rate must be a number that is not negative.");
        }

        int crowd = readInt(parts[5], "starting crowd", lineNumber);
        if (crowd < 0) {
            throw new MapFileException(lineNumber, "the starting crowd cannot be negative.");
        }

        BusStop stop = priority.equals("HIGH") ? new HighPriorityStop(x, y) : new LowPriorityStop(x, y);
        stop.setArrivalRate(rate);
        stop.setStartingCrowd(crowd);
        stop.resetForNewRun();
        stops.add(stop);
    }

    /**
     * Checks and stores one {@code bus} line. Every stop number must exist, a route needs at least two
     * stops, and a Double Decker cannot be given a low priority stop.
     *
     * @param parts        the line split at the commas
     * @param lineNumber   1-based line number, for error messages
     * @param routeBuilder makes the bus
     * @throws MapFileException if the line is wrong
     */
    private void readBus(String[] parts, int lineNumber, RouteBuilder routeBuilder) throws MapFileException {
        if (parts.length != 4) {
            throw new MapFileException(lineNumber, "a bus needs 3 values: type, color, route.");
        }

        String typeWord = parts[1].trim();
        int typeIndex = Arrays.asList(TYPE_WORDS).indexOf(typeWord);
        if (typeIndex == -1) {
            throw new MapFileException(lineNumber, "bus type must be STANDARD, MINI or DOUBLE, not \"" + typeWord + "\".");
        }
        String typeName = RouteBuilder.BUS_TYPES[typeIndex];

        Color color;
        try {
            color = Color.web(parts[2].trim());
        } catch (IllegalArgumentException e) {
            throw new MapFileException(lineNumber, "\"" + parts[2].trim() + "\" is not a valid color (use something like #1e90ff).");
        }

        String routeText = parts[3].trim();
        String[] numbers = routeText.isEmpty() ? new String[0] : routeText.split("\\s+");
        if (numbers.length < 2) {
            throw new MapFileException(lineNumber, "a route needs at least 2 stops.");
        }

        ArrayList<Location> route = new ArrayList<>();
        for (String number : numbers) {
            int stopNumber = readInt(number, "route stop number", lineNumber);
            if (stopNumber < 1 || stopNumber > stops.size()) {
                throw new MapFileException(lineNumber, "stop " + stopNumber + " does not exist (there are " + stops.size() + " stops).");
            }
            BusStop stop = stops.get(stopNumber - 1);
            if (typeName.equals(RouteBuilder.DOUBLE_DECKER) && !stop.isHighPriority()) {
                throw new MapFileException(lineNumber, "stop " + stopNumber + ": " + RouteBuilder.DOUBLE_DECKER_LOW_PRIORITY_MESSAGE);
            }
            route.add(stop.getLocation());
        }

        Bus bus = routeBuilder.createBus(typeName, color, route);
        bus.resetForNewRun();
        buses.add(bus);
    }

    /**
     * Reads a whole number from text.
     *
     * @param text       the text to read
     * @param name       what the number is, for the error message
     * @param lineNumber 1-based line number, for the error message
     * @return the number
     * @throws MapFileException if the text is not a whole number
     */
    private static int readInt(String text, String name, int lineNumber) throws MapFileException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new MapFileException(lineNumber, "\"" + text.trim() + "\" is not a valid " + name + ".");
        }
    }

    /**
     * Finds the position of the stop at a location.
     *
     * @param stops    the stops to search
     * @param location the location to look for
     * @return the index of the stop, or {@code -1} if there is none there
     */
    private static int indexOfStopAt(List<BusStop> stops, Location location) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).isAt(location)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts a bus type's display name to the word used in the text.
     *
     * @param typeName one of the {@link RouteBuilder} type names
     * @return {@code STANDARD}, {@code MINI} or {@code DOUBLE}
     */
    private static String typeWord(String typeName) {
        return TYPE_WORDS[Arrays.asList(RouteBuilder.BUS_TYPES).indexOf(typeName)];
    }
}
