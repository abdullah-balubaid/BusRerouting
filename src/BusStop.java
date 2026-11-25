import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayDeque;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BusStop</p>
 * <p><b>Purpose:</b> Represents a bus stop with a 2D location, waiting passenger queue and arrival
 * tracking, priority operating schedule, wait-time metrics, and canvas rendering.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.4 11/25/26
 */
public class BusStop {
    private int waitingPassengers;                                    // Current number of passengers waiting at the stop
    private long lastRecordedVisit;                                   // Wall-clock timestamp (ms) of most recent bus visit
    private Location busStopLocation;                                 // Grid position of the stop
    private double arrivalRate;                                       // Passenger arrival rate (passengers per simulated second)
    private double arrivalProgress;                                   // Fractional passenger accumulator across time steps
    private final ArrayDeque<Double> arrivalTimes = new ArrayDeque<>(); // Arrival timestamps (in simulated seconds) of waiting passengers
    private double totalWaitSeconds = 0;                              // Cumulative wait time (seconds) of boarded passengers
    private int totalBoarded = 0;                                     // Total number of passengers who have boarded at this stop

    // note 5: priority. HIGH stops are active all day and are the only ones a DoubleDecker may
    // serve; LOW stops are active only part of the day (e.g. a morning/evening window).
    private boolean highPriority = true;   // defaults to high priority, so existing callers are unaffected

    /**
     * Constructs a {@code BusStop} at a copy of the specified {@link Location}.
     *
     * @param location initial stop position
     * @throws NullPointerException if {@code location} is {@code null}
     */
    public BusStop(Location location) {
        if (location != null) {
            busStopLocation = new Location(location.getX(), location.getY());
        } else {
            throw new NullPointerException("Location must not be null.");
        }
    }

    /**
     * Constructs a {@code BusStop} at the given X and Y coordinates.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    BusStop(double x, double y) {
        busStopLocation = new Location(x, y);
    }

    /**
     * Returns whether this stop is marked as high priority.
     *
     * @return {@code true} if high priority, {@code false} if low priority
     */
    // note 5
    public boolean isHighPriority() {
        return highPriority;
    }

    /**
     * Sets whether this stop is high priority.
     *
     * @param highPriority {@code true} for high priority, {@code false} for low priority
     */
    public void setHighPriority(boolean highPriority) {
        this.highPriority = highPriority;
    }

    /**
     * Updates the waiting passenger count at this stop.
     *
     * @param Passengers new non-negative waiting passenger count
     * @throws IllegalArgumentException if {@code Passengers} is negative
     */
    public void updatePassengers(int Passengers) {
        if (Passengers < 0) {
            throw new IllegalArgumentException("Passengers cannot be negative.");
        }

        waitingPassengers = Passengers;
    }

    /**
     * Records the current system wall-clock time as the most recent bus visit timestamp.
     */
    public void recordVisit() {
        lastRecordedVisit = System.currentTimeMillis();
    }

    /**
     * Returns the current number of waiting passengers at this stop.
     *
     * @return waiting passenger count
     */
    public int getPassenger() {
        return waitingPassengers;
    }

    /**
     * Returns the wall-clock timestamp (in milliseconds) of the last recorded visit.
     *
     * @return last visit timestamp in milliseconds
     */
    public long getLastVisit() {
        return lastRecordedVisit;
    }

    /**
     * Returns a defensive copy of this stop's {@link Location}.
     *
     * @return copy of {@link #busStopLocation}
     */
    public Location getLocation() {
        return new Location(busStopLocation.getX(), busStopLocation.getY());
    }

    /**
     * Returns a string representation of this stop's coordinates and waiting passenger count.
     *
     * @return formatted stop description
     */
    @Override
    public String toString() {
        return "BusStop[" + busStopLocation.getX() + ", " + busStopLocation.getY()
                + ", waiting=" + waitingPassengers + "]";
    }

    /**
     * Renders the bus stop pole, flag, sign box, top accent bar, and stop/passenger label on the canvas.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param stopNumber   1-based display number for the stop
     * @param centerX      center X coordinate in canvas pixels
     * @param centerY      center Y coordinate in canvas pixels
     * @param unit         base size unit scaled to the grid cell/icon
     * @param poleColor    color of the stop pole
     * @param accentColor  color of the flag, sign border, and top bar
     * @param boxFill      fill color of the central sign box
     * @param labelColor   fill color of the stop label text
     * @param outlineColor outline stroke color of the stop label text
     */
    public void draw(GraphicsContext drawingBrush, int stopNumber, double centerX, double centerY,
                     double unit, Color poleColor, Color accentColor, Color boxFill, Color labelColor,
                     Color outlineColor) {
        drawingBrush.save();
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 13));
        double poleX = centerX - unit - 3;

        // Pole
        drawingBrush.setLineCap(StrokeLineCap.BUTT);
        drawingBrush.setStroke(poleColor);
        drawingBrush.setLineWidth(Math.max(2, unit * 0.2));
        drawingBrush.strokeLine(poleX, centerY - unit * 1.5, poleX, centerY + unit * 0.9);

        // Flag
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(poleX - unit * 0.3, centerY - unit * 1.55, unit * 0.65, unit * 0.55);

        // Sign box
        drawingBrush.setFill(boxFill);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);
        drawingBrush.setStroke(accentColor);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);

        // Top bar
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, unit * 0.4);

        // Label
        String text = "Stop " + (stopNumber) + " (" + getPassenger() + ")";
        double labelY = centerY - unit * 1.95;
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(outlineColor);
        drawingBrush.strokeText(text, centerX, labelY);
        drawingBrush.setFill(labelColor);
        drawingBrush.fillText(text, centerX, labelY);

        drawingBrush.restore();
    }

    /**
     * Returns the passenger arrival rate at this stop.
     *
     * @return arrival rate in passengers per simulated second
     */
    public double getArrivalRate() {
        return arrivalRate;
    }

    /**
     * Sets the passenger arrival rate at this stop.
     *
     * @param arrivalRate arrival rate in passengers per simulated second
     */
    public void setArrivalRate(double arrivalRate) {
        this.arrivalRate = arrivalRate;
    }

    /**
     * Accumulates fractional passenger arrivals over {@code seconds}, increments {@link #waitingPassengers}
     * by whole arrivals, and records each new passenger's arrival timestamp in {@link #arrivalTimes}.
     *
     * @param seconds               elapsed simulated seconds in this step
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     */
    public void advancePassengers(double seconds, double nowInSimulatedSeconds) {
        arrivalProgress += arrivalRate * seconds;
        int newPassengers = (int) arrivalProgress;
        arrivalProgress -= newPassengers;

        if (newPassengers > 0) {
            updatePassengers(waitingPassengers + newPassengers);
        }

        // Record each newly arrived passenger's timestamp for wait-time calculations
        for (int i = 0; i < newPassengers; i++) {
            arrivalTimes.addLast(nowInSimulatedSeconds);
        }
    }

    /**
     * Checks whether this stop is open at the given simulated minute. High-priority stops are always
     * open; low-priority stops are open during morning ({@code < 60}) and evening ({@code >= 240})
     * windows of a 300-minute day.
     *
     * @param nowInSimulatedMinutes current simulated time in minutes
     * @return {@code true} if the stop is open, {@code false} if closed
     */
    // note 5: a low-priority stop is only "open" during part of the simulated day. The 300-minute
    // run stands in for a full day, so it is split into three bands: morning (0-60 min), midday
    // (60-240 min, closed for low-priority stops), evening (240-300 min). High-priority stops are
    // always open.
    public boolean isOpenAt(double nowInSimulatedMinutes) {
        if (highPriority) {
            return true;
        }
        boolean morning = nowInSimulatedMinutes < 60;
        boolean evening = nowInSimulatedMinutes >= 240;
        return morning || evening;
    }

    /**
     * Boards up to {@code maxPassengers} waiting passengers using {@code 0} as the simulated
     * arrival reference time.
     *
     * @param maxPassengers maximum number of passengers that can board
     * @return number of passengers who boarded
     */
    public int boardPassengers(int maxPassengers) {
        return boardPassengers(maxPassengers, 0);
    }

    /**
     * Boards up to {@code maxPassengers} waiting passengers, updates the visit timestamp, and
     * accumulates wait time and boarded count using {@code nowInSiumlatedSeconds}.
     *
     * @param maxPassengers        maximum number of passengers that can board
     * @param nowInSiumlatedSeconds current simulated clock time in seconds
     * @return number of passengers who boarded
     */
    public int boardPassengers(int maxPassengers, double nowInSiumlatedSeconds) {
        int boarding = Math.min(waitingPassengers, maxPassengers);
        updatePassengers(waitingPassengers - boarding);
        recordVisit();

        // Dequeue matching arrival timestamps to tally actual wait durations
        for (int i = 0; i < boarding; i++) {
            Double arrivalTime = arrivalTimes.pollFirst();
            if (arrivalTime != null) {
                totalWaitSeconds += (nowInSiumlatedSeconds - arrivalTime);
                totalBoarded++;
            }
        }

        return boarding;
    }

    /**
     * Computes the average passenger wait time in minutes for passengers who have boarded at this stop.
     *
     * @return average wait time in minutes, or {@link Double#NaN} if {@link #totalBoarded} is {@code 0}
     */
    public double getAverageWaitMinutes() {
        if (totalBoarded == 0) {
            return Double.NaN;
        }

        return (totalWaitSeconds / totalBoarded) / 60.0;
    }

    /**
     * Returns the cumulative wait time in seconds across all boarded passengers at this stop.
     *
     * @return total wait time in seconds
     */
    public double getTotalWaitSeconds() {
        return totalWaitSeconds;
    }

    /**
     * Returns the total number of passengers who have boarded at this stop.
     *
     * @return total boarded passenger count
     */
    public int getTotalBoarded() {
        return totalBoarded;
    }

    /**
     * Returns how long (in simulated seconds) the longest-waiting passenger currently at this stop
     * has been waiting as of {@code nowInSimulatedSeconds}.
     *
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @return oldest passenger wait duration in seconds, or {@code 0} if no arrival time is queued
     */
    public double getOldestWaitSeconds(double nowInSimulatedSeconds) {
        Double oldestArrival = arrivalTimes.peekFirst();

        if (oldestArrival == null) {
            return 0;
        }

        return nowInSimulatedSeconds - oldestArrival;
    }
}