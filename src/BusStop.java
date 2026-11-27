import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BusStop</p>
 * <p><b>Purpose:</b> Abstract map item representing a bus stop with a waiting passenger queue,
 * arrival tracking, wait-time metrics, and canvas rendering. The two kinds of stop,
 * {@link HighPriorityStop} and {@link LowPriorityStop}, differ in how busy they are through the day.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.6 11/26/26
 */
public abstract class BusStop extends MapItem {
    private static final Color POLE_COLOR = Color.web("#a9b4c4");   // The pole beside the sign, the same for both kinds of stop

    private int waitingPassengers;                                    // Current number of passengers waiting at the stop
    private double arrivalRate;                                       // Base passenger arrival rate (passengers per simulated second)
    private double arrivalProgress;                                   // Fractional passenger accumulator across time steps
    private final ArrayDeque<Double> arrivalTimes = new ArrayDeque<>(); // Arrival timestamps (in simulated seconds) of waiting passengers
    private final ArrayList<Double> boardedWaits = new ArrayList<>();   // Wait (seconds) of each passenger who boarded
    private int startingCrowd = 0;                                    // Passengers already waiting when a run starts
    private int number = 0;                                           // 1-based position in the network (shown as "Stop N")
    private final Icon icon;                                          // Picture of the stop, or null to draw the plain sign

    /**
     * Constructs a {@code BusStop} at a copy of the specified {@link Location}.
     *
     * @param location initial stop position
     * @param icon     picture of the stop, or {@code null} to draw the plain sign and pole
     * @throws NullPointerException if {@code location} is {@code null}
     */
    protected BusStop(Location location, Icon icon) {
        if (location == null) {
            throw new NullPointerException("Location must not be null.");
        }

        setLocation(location);
        this.icon = icon;
    }

    /**
     * Constructs a {@code BusStop} at the given X and Y coordinates.
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param icon picture of the stop, or {@code null} to draw the plain sign and pole
     */
    protected BusStop(double x, double y, Icon icon) {
        setLocation(x, y);
        this.icon = icon;
    }

    /**
     * Returns whether this stop is high priority (busy all day, and the only kind a
     * {@link DoubleDecker} may serve).
     *
     * @return {@code true} if high priority, {@code false} if low priority
     */
    public abstract boolean isHighPriority();

    /**
     * Returns how busy this stop is at the given time, as a factor applied to its base arrival rate.
     *
     * @param nowInSimulatedMinutes current simulated time in minutes
     * @return multiplier applied to {@link #arrivalRate} ({@code 1.0} means fully active)
     */
    public abstract double getArrivalMultiplier(double nowInSimulatedMinutes);

    /**
     * Returns the fill colour of this kind of stop's sign box.
     *
     * @return box colour
     */
    public abstract Color getBoxColor();

    /**
     * Returns the accent colour of this kind of stop (flag, sign border and top bar).
     *
     * @return accent colour
     */
    public abstract Color getAccentColor();

    /**
     * Adds waiting passengers who arrive at the given simulated time, queueing a timestamp for each
     * so their wait can be measured when they board.
     *
     * @param count                 number of passengers to add (must not be negative)
     * @param nowInSimulatedSeconds simulated clock time (seconds) at which they arrive
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public void addPassengers(int count, double nowInSimulatedSeconds) {
        if (count < 0) {
            throw new IllegalArgumentException("Passengers cannot be negative.");
        }

        waitingPassengers += count;
        for (int i = 0; i < count; i++) {
            arrivalTimes.addLast(nowInSimulatedSeconds);
        }
    }

    /**
     * Restarts this stop for a new run: its starting crowd is already waiting at time zero, and every
     * wait-time statistic, queued timestamp and fractional arrival is cleared. Without this, the
     * second phase would inherit the first phase's wait totals.
     */
    public void resetForNewRun() {
        waitingPassengers = 0;
        arrivalTimes.clear();
        arrivalProgress = 0;
        boardedWaits.clear();
        addPassengers(startingCrowd, 0);
    }

    /**
     * Sets how many passengers are already waiting at this stop whenever a run starts.
     *
     * @param startingCrowd number of passengers (not negative)
     * @throws IllegalArgumentException if negative
     */
    public void setStartingCrowd(int startingCrowd) {
        if (startingCrowd < 0) {
            throw new IllegalArgumentException("The starting crowd cannot be negative.");
        }
        this.startingCrowd = startingCrowd;
    }

    /**
     * Returns how many passengers are waiting at this stop whenever a run starts.
     *
     * @return the starting crowd
     */
    public int getStartingCrowd() {
        return startingCrowd;
    }

    /**
     * Sets this stop's 1-based position in the network.
     *
     * @param number the position, starting at 1
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Returns this stop's display name.
     *
     * @return text such as {@code "Stop 3"}
     */
    public String getName() {
        return "Stop " + number;
    }

    /**
     * Returns the current number of waiting passengers at this stop.
     *
     * @return waiting passenger count
     */
    public int getWaitingPassengers() {
        return waitingPassengers;
    }

    /**
     * Returns a string representation of this stop's coordinates and waiting passenger count.
     *
     * @return formatted stop description
     */
    @Override
    public String toString() {
        Location location = getLocation();
        return "BusStop[" + location.getX() + ", " + location.getY()
                + ", waiting=" + waitingPassengers + "]";
    }

    /**
     * Draws the stop and its "Stop N (waiting)" label. If the stop has an {@link Icon} whose picture
     * was found, that is drawn; otherwise the plain pole, flag and sign box are drawn in this stop's
     * own colours.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param centerX      center X coordinate in canvas pixels
     * @param centerY      center Y coordinate in canvas pixels
     * @param unit         base size unit scaled to the grid cell/icon
     */
    public void draw(GraphicsContext drawingBrush, double centerX, double centerY, double unit) {
        drawingBrush.save();
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 13));

        Image picture = (icon == null) ? null : icon.getImage(unit * 2);
        if (picture != null) {
            drawingBrush.drawImage(picture, centerX - picture.getWidth() / 2, centerY - picture.getHeight() / 2);
        } else {
            drawSign(drawingBrush, centerX, centerY, unit);
        }

        // Label
        String text = getName() + " (" + getWaitingPassengers() + ")";
        double labelY = centerY - unit * 1.95;
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(Palette.LABEL_OUTLINE);
        drawingBrush.strokeText(text, centerX, labelY);
        drawingBrush.setFill(Palette.LABEL_TEXT);
        drawingBrush.fillText(text, centerX, labelY);

        drawingBrush.restore();
    }

    /**
     * Draws the plain stop: a pole with a flag, and a sign box with a top bar, in this stop's colours.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param centerX      center X coordinate in canvas pixels
     * @param centerY      center Y coordinate in canvas pixels
     * @param unit         base size unit scaled to the grid cell/icon
     */
    private void drawSign(GraphicsContext drawingBrush, double centerX, double centerY, double unit) {
        Color accentColor = getAccentColor();
        double poleX = centerX - unit - 3;

        // Pole
        drawingBrush.setLineCap(StrokeLineCap.BUTT);
        drawingBrush.setStroke(POLE_COLOR);
        drawingBrush.setLineWidth(Math.max(2, unit * 0.2));
        drawingBrush.strokeLine(poleX, centerY - unit * 1.5, poleX, centerY + unit * 0.9);

        // Flag
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(poleX - unit * 0.3, centerY - unit * 1.55, unit * 0.65, unit * 0.55);

        // Sign box
        drawingBrush.setFill(getBoxColor());
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);
        drawingBrush.setStroke(accentColor);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);

        // Top bar
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, unit * 0.4);
    }

    /**
     * Returns the base passenger arrival rate at this stop.
     *
     * @return arrival rate in passengers per simulated second
     */
    public double getArrivalRate() {
        return arrivalRate;
    }

    /**
     * Sets the base passenger arrival rate at this stop.
     *
     * @param arrivalRate arrival rate in passengers per simulated second
     */
    public void setArrivalRate(double arrivalRate) {
        this.arrivalRate = arrivalRate;
    }

    /**
     * Accumulates fractional passenger arrivals over {@code seconds} (scaled by this stop's
     * {@link #getArrivalMultiplier(double)} for the current time), adds the whole arrivals to the
     * waiting passengers, and records each new passenger's arrival timestamp.
     *
     * @param seconds               elapsed simulated seconds in this step
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     */
    public void advancePassengers(double seconds, double nowInSimulatedSeconds) {
        double multiplier = getArrivalMultiplier(nowInSimulatedSeconds / 60.0);
        arrivalProgress += arrivalRate * multiplier * seconds;
        int newPassengers = (int) arrivalProgress;
        arrivalProgress -= newPassengers;

        if (newPassengers > 0) {
            addPassengers(newPassengers, nowInSimulatedSeconds);
        }
    }

    /**
     * Boards up to {@code maxPassengers} waiting passengers, updates the visit timestamp, and
     * accumulates wait time and boarded count using {@code nowInSimulatedSeconds}.
     *
     * @param maxPassengers         maximum number of passengers that can board
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @return number of passengers who boarded
     */
    public int boardPassengers(int maxPassengers, double nowInSimulatedSeconds) {
        int boarding = Math.min(waitingPassengers, maxPassengers);
        waitingPassengers -= boarding;

        // Dequeue matching arrival timestamps to tally actual wait durations
        for (int i = 0; i < boarding; i++) {
            Double arrivalTime = arrivalTimes.pollFirst();
            if (arrivalTime != null) {
                boardedWaits.add(nowInSimulatedSeconds - arrivalTime);
            }
        }

        return boarding;
    }

    /**
     * Rates this stop's passengers: those who boarded by the wait they had, and those still waiting by
     * their wait so far.
     *
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @param acceptableWaitMinutes the wait that still earns 5 stars
     * @return the stop's satisfaction figures
     */
    public StopSatisfaction getSatisfaction(double nowInSimulatedSeconds, double acceptableWaitMinutes) {
        List<Double> waitsInMinutes = new ArrayList<>();
        for (double waitSeconds : boardedWaits) {
            waitsInMinutes.add(waitSeconds / 60.0);
        }
        for (double arrival : arrivalTimes) {
            waitsInMinutes.add((nowInSimulatedSeconds - arrival) / 60.0);
        }

        int happy = 0;
        double starSum = 0;
        double waitSum = 0;
        for (double minutes : waitsInMinutes) {
            double stars = StopSatisfaction.starsFor(minutes, acceptableWaitMinutes);
            starSum += stars;
            happy += (stars >= StopSatisfaction.HAPPY_STARS) ? 1 : 0;
            waitSum += minutes;
        }

        return new StopSatisfaction(getName(), isHighPriority(), waitsInMinutes.size(), starSum, happy, waitSum, waitingPassengers);
    }

    /**
     * Returns the total number of passengers who have boarded at this stop.
     *
     * @return total boarded passenger count
     */
    public int getTotalBoarded() {
        return boardedWaits.size();
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
