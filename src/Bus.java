import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Bus</p>
 * <p><b>Purpose:</b> Abstract bus extending {@link Vehicle} with route waypoints, its place on that
 * route (including whether it is on a reroute), last-visited stop tracking, loading state, its own
 * name ("S1"), its {@link Icon}, and route/bus canvas rendering.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.5 11/26/26
 */
public abstract class Bus extends Vehicle {
    private final ArrayList<Location> route;                         // Ordered sequence of route stop locations
    private final String busPrefix;                                  // Short display prefix for this bus type (e.g., "S", "M", "DD")
    private int number = 0;                                          // 1-based position in the network (shown as "S1")
    private int nextStopIndex = 0;                                   // Index in the route of the stop this bus is scheduled to visit next
    private double previousX;                                      // Where the bus was before its latest step (for smooth drawing)
    private double previousY;
    private final double costPerKm;                                 // What it costs to drive this bus one kilometre
    private final Icon icon;                                        // This bus's picture, tinted with its colour
    private boolean heldBack;                                       // True while the bus waits for passengers instead of driving
    private double loadingSecondsRemaining;                          // Remaining simulated loading pause in seconds

    /**
     * Constructs a {@code Bus} extending {@link Vehicle} with validated non-null, non-empty route waypoints.
     *
     * @param Capacity  maximum passenger capacity
     * @param Speed     driving speed in km/h
     * @param color     display/tinting color of the bus
     * @param route     non-null, non-empty list of route {@link Location} stops
     * @param busPrefix short label prefix identifying the bus type
     * @param costPerKm what it costs to drive this bus one kilometre (must not be negative)
     * @param icon      this bus's picture (a tinted {@link Icon} of its own)
     * @throws NullPointerException     if {@code route} is {@code null} (or if {@code color} is {@code null} in {@code super})
     * @throws IllegalArgumentException if {@code route} is empty or {@code costPerKm} is negative (or if capacity/speed are non-positive in {@code super})
     */
    protected Bus(int Capacity, int Speed, Color color, ArrayList<Location> route, String busPrefix,
                  double costPerKm, Icon icon) {
        super(Capacity, Speed, color);

        if (costPerKm < 0) {
            throw new IllegalArgumentException("costPerKm must not be negative.");
        }

        this.busPrefix = busPrefix;
        this.costPerKm = costPerKm;
        this.icon = icon;

        if (route == null) {
            throw new NullPointerException("route must not be null.");
        } else if (route.isEmpty()) {
            throw new IllegalArgumentException("route must not be empty.");
        }
        this.route = new ArrayList<>(route);
    }

    /**
     * Returns a shallow copy of this bus's route waypoint list.
     *
     * @return copy of {@link #route}
     */
    public ArrayList<Location> getRoute() {
        return new ArrayList<>(route);
    }

    /**
     * Returns the name of this kind of bus, as shown in the Build Mode drop-down and used in saved builds.
     *
     * @return text such as {@code "Standard Bus"}
     */
    public abstract String getTypeName();

    /**
     * Sets this bus's 1-based position in the network.
     *
     * @param number the position, starting at 1
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Returns this bus's short name.
     *
     * @return text such as {@code "S1"} or {@code "DD3"}
     */
    public String getName() {
        return busPrefix + number;
    }

    /**
     * Returns how much bigger or smaller than the standard icon size this bus is drawn.
     *
     * @return the icon's size multiplier
     */
    public double getIconScale() {
        return icon.getScale();
    }

    /**
     * Remembers where the bus is now. Called before every simulation step, so the picture can be
     * drawn smoothly somewhere between the last step's position and this one.
     */
    public void rememberPosition() {
        previousX = getLocation().getX();
        previousY = getLocation().getY();
    }

    /**
     * Returns where to draw the bus: somewhere between its position before the latest step and its
     * position now. Drawing between steps keeps the motion smooth at slow speeds.
     *
     * @param fraction how far through the latest step the display is (0 = before it, 1 = after it)
     * @return the position to draw at
     */
    public Location getDrawLocation(double fraction) {
        Location now = getLocation();
        return new Location(previousX + (now.getX() - previousX) * fraction,
                previousY + (now.getY() - previousY) * fraction);
    }

    /**
     * Describes the bus by its name and how full it is.
     *
     * @return text such as {@code "S1 12/30"}
     */
    @Override
    public String toString() {
        return getName() + " " + getCurrentPassengers() + "/" + getPassengerCapacity();
    }

    /**
     * Tells whether the bus is waiting for passengers instead of driving on (cost-saving runs only).
     *
     * @return {@code true} while the bus is held back
     */
    public boolean isHeldBack() {
        return heldBack;
    }

    /**
     * Sets whether the bus is waiting for passengers.
     *
     * @param heldBack {@code true} to hold the bus where it is
     */
    public void setHeldBack(boolean heldBack) {
        this.heldBack = heldBack;
    }

    /**
     * Tells whether this bus is heading for a stop that is not its next scheduled one, which means
     * it was rerouted and has not arrived yet.
     *
     * @return {@code true} while on a reroute
     */
    public boolean isRerouting() {
        return !getDestination().equals(route.get(nextStopIndex));
    }

    /**
     * Puts the bus back at the start of its route, empty, with its driving distance cleared and the
     * second stop of the route as its destination. Both phases start from this state.
     */
    public void resetForNewRun() {
        updateLocation(route.get(0));
        rememberPosition();
        nextStopIndex = 1 % route.size();
        updateDestination(route.get(nextStopIndex));
        setCurrentPassengers(0);
        startLoading(0.0);
        heldBack = false;
        resetDistanceTravelled();
    }

    /**
     * Sets the next destination after a stop has been reached. A bus that has just finished a reroute
     * goes back to the stop it was scheduled for; otherwise it moves on to the next stop of its route.
     *
     * @param finishedReroute {@code true} if the stop just reached was a reroute detour
     */
    public void headForNextStop(boolean finishedReroute) {
        if (!finishedReroute) {
            nextStopIndex = (nextStopIndex + 1) % route.size();
        }
        updateDestination(route.get(nextStopIndex));
    }

    /**
     * Returns the stop this bus is scheduled to visit next on its own route.
     *
     * @return the location of that stop
     */

    public Location getNextScheduledStop() {
        return route.get(nextStopIndex);
    }

    /**
     * Checks whether this bus is permitted to serve the given {@link BusStop}.
     *
     * @param stop the {@link BusStop} to check
     * @return {@code true} by default; subclasses such as {@link DoubleDecker} may restrict this
     */
    // note 5: by default any bus may serve any stop; DoubleDecker overrides this to restrict
    // itself to high-priority stops only.
    public boolean canServe(BusStop stop) {
        return true;
    }

    /**
     * Returns the driving cost this bus has run up since its distance was last reset.
     *
     * @return kilometres driven times the cost per kilometre
     */
    public double getTravelCost() {
        return Scale.cellsToKm(getDistanceTravelled()) * costPerKm;
    }

    /**
     * Starts or sets the remaining loading duration in simulated seconds.
     *
     * @param seconds loading duration in seconds
     */
    public void startLoading(double seconds) {
        loadingSecondsRemaining = seconds;
    }

    /**
     * Advances the loading timer by {@code seconds} if the bus is currently loading.
     *
     * @param seconds elapsed simulated seconds in this step
     * @return {@code true} if the bus was still loading during this step, {@code false} if done or not loading
     */
    public boolean advanceLoading(double seconds) {
        if (loadingSecondsRemaining <= 0) {
            return false;
        }

        loadingSecondsRemaining -= seconds;
        return true;
    }

    /**
     * Performs a stop arrival: unloads current passengers, boards waiting passengers up to
     * capacity (recording how long they waited) and starts the loading timer.
     *
     * @param stop                  the {@link BusStop} arrived at (must not be {@code null})
     * @param loadingSeconds        loading pause duration in seconds
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     * @return number of passengers who boarded
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    public int arriveAt(BusStop stop, double loadingSeconds, double nowInSimulatedSeconds) {
        if (stop == null) {
            throw new NullPointerException("Stop must not be null.");
        }

        setCurrentPassengers(0);
        int gettingOn = stop.boardPassengers(getPassengerCapacity(), nowInSimulatedSeconds);
        setCurrentPassengers(gettingOn);
        startLoading(loadingSeconds);
        return gettingOn;
    }

    /**
     * Draws the bus (its {@link Icon} picture, or a plain coloured square if the picture is not found),
     * its "S1 12/30" label below it and, while it is on a reroute, a "REROUTE" tag above it.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param centerX      centre X in canvas pixels
     * @param centerY      centre Y in canvas pixels
     * @param iconSize     standard icon size in pixels
     * @param labelColor   fill colour of the label text
     * @param outlineColor outline colour of the label text
     * @param rerouteColor fill colour of the "REROUTE" tag
     */
    public void draw(GraphicsContext drawingBrush, double centerX, double centerY, double iconSize,
                     Color labelColor, Color outlineColor, Color rerouteColor) {
        drawingBrush.save();
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 12));

        double topOfBus;
        double bottomOfBus;
        Image picture = icon.getImage(iconSize);
        if (picture != null) {
            drawingBrush.drawImage(picture, centerX - picture.getWidth() / 2, centerY - picture.getHeight() / 2);
            topOfBus = centerY - picture.getHeight() / 2;
            bottomOfBus = centerY + picture.getHeight() / 2;
        } else {
            Color busColor = getColor();
            drawingBrush.setFill(Color.color(busColor.getRed(), busColor.getGreen(), busColor.getBlue()));
            double side = iconSize * getIconScale();
            drawingBrush.fillRect(centerX - side / 2, centerY - side / 2, side, side);
            topOfBus = centerY - side / 2;
            bottomOfBus = centerY + side / 2;
        }

        if (isRerouting()) {
            double badgeY = topOfBus - 5;
            drawingBrush.setLineWidth(3);
            drawingBrush.setStroke(outlineColor);
            drawingBrush.strokeText("REROUTE", centerX, badgeY);
            drawingBrush.setFill(rerouteColor);
            drawingBrush.fillText("REROUTE", centerX, badgeY);
        }

        String text = toString();
        double labelY = bottomOfBus + 13;
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(outlineColor);
        drawingBrush.strokeText(text, centerX, labelY);
        drawingBrush.setFill(labelColor);
        drawingBrush.fillText(text, centerX, labelY);

        drawingBrush.restore();
    }

    /**
     * Draws orthogonal grid-aligned route segments connecting consecutive stops in {@link #route}
     * (wrapping around to index 0), using dashed strokes for {@link Reroutable} buses.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param lineWidth    base stroke width before scaling
     * @param shift        pixel offset applied to both X and Y coordinates to separate overlapping routes
     * @param toScreenX    function mapping grid X to canvas pixel X
     * @param toScreenY    function mapping grid Y to canvas pixel Y
     */
    public void drawRoute(GraphicsContext drawingBrush,
                          double lineWidth,
                          double shift,
                          DoubleUnaryOperator toScreenX,
                          DoubleUnaryOperator toScreenY) {
        drawingBrush.save();
        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setLineWidth(lineWidth * 0.5);

        Color busColor = getColor();
        drawingBrush.setStroke(Color.color(
                busColor.getRed(), busColor.getGreen(), busColor.getBlue(), 0.8));

        // Visual cue distinguishing buses capable of dynamic rerouting
        if (this instanceof Reroutable) {
            drawingBrush.setLineDashes(12, 8);
        }

        for (int j = 0; j < route.size(); j++) {
            Location from = route.get(j);
            Location to = route.get((j + 1) % route.size());

            double fromX = toScreenX.applyAsDouble(from.getX()) + shift;
            double fromY = toScreenY.applyAsDouble(from.getY()) + shift;
            double toX = toScreenX.applyAsDouble(to.getX()) + shift;
            double toY = toScreenY.applyAsDouble(to.getY()) + shift;

            // Draw L-shaped Manhattan segments (horizontal then vertical) along the grid
            drawingBrush.strokeLine(fromX, fromY, toX, fromY);
            drawingBrush.strokeLine(toX, fromY, toX, toY);
        }

        drawingBrush.setLineDashes((double[]) null);
        drawingBrush.restore();
    }
}