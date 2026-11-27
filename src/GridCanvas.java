import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> GridCanvas</p>
 * <p><b>Purpose:</b> The map area: draws the square grid, routes, stops and buses on a canvas,
 * converts between grid coordinates and pixels, and finds the stop, bus or intersection under a click.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.1 11/26/26
 */
public class GridCanvas {
    private static final int GRID_SIZE = Scale.GRID_SIZE;   // Number of grid cells along each side of the square grid
    private static final int SCALE_BAR_CELLS = 5;      // The scale bar is this many grid cells long
    private static final double SCALE_BAR_OFFSET = 24;  // Pixels between the bottom of the grid and the scale bar
    private static final double MARGIN = 46;      // Minimum pixel space kept free around the grid
    private static final double STOP_SIZE_FACTOR = 0.38;   // Stop sign size relative to the bus icon size (raise to enlarge stops)
    private static final double MIN_STOP_UNIT = 10;        // Smallest stop size unit in pixels

    private final Canvas canvas = new Canvas();   // The drawing surface
    private final Pane pane;                      // Pane holding the canvas, resized with the window
    private final BusNetwork network;             // The stops and buses to draw

    private List<BusStop> highlightedStops = new ArrayList<>();   // Stops ringed while a route is being picked
    private boolean showOpenIntersections = false;                 // True while stops are being added
    private DoubleSupplier stepFraction = () -> 1.0;               // How far through the latest simulation step the display is

    private double cellSize;                      // Pixel size of one (square) grid cell
    private double originX;                       // Pixel X of grid column 0
    private double originY;                       // Pixel Y of grid row 0
    private double iconSize;                      // Pixel size of the bus icons

    /**
     * Constructs the map area and redraws it whenever it is resized.
     *
     * @param network the stops and buses to draw
     */
    public GridCanvas(BusNetwork network) {
        this.network = network;

        // Bind canvas size dynamically to its pane and redraw on resize
        pane = new Pane(canvas);
        pane.setMinWidth(300);   // The side bar can be dragged, but never squeezes the map below this
        // Clicking the map takes the keyboard focus away from any text box, so shortcuts like Space work again
        pane.setFocusTraversable(true);
        pane.setOnMousePressed(event -> pane.requestFocus());
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());
        pane.widthProperty().addListener((observable, oldWidth, newWidth) -> draw());
        pane.heightProperty().addListener((observable, oldHeight, newHeight) -> draw());
    }

    /**
     * Returns the pane to place in the window.
     *
     * @return the pane holding the canvas
     */
    public Pane getPane() {
        return pane;
    }

    /**
     * Sets what happens when the map is clicked.
     *
     * @param handler receives every mouse click on the map
     */
    public void setOnClick(Consumer<MouseEvent> handler) {
        pane.setOnMouseClicked(handler::accept);
    }

    /**
     * Changes the mouse cursor shown over the map.
     *
     * @param cursor the cursor to show
     */
    public void setCursor(Cursor cursor) {
        pane.setCursor(cursor);
    }

    /**
     * Chooses whether every empty intersection is marked with a dot (used while adding stops).
     *
     * @param show {@code true} to mark the empty intersections
     */
    public void setShowOpenIntersections(boolean show) {
        showOpenIntersections = show;
    }

    /**
     * Sets the list of stops that are ringed while a bus route is being picked. The list is kept
     * by reference, so the caller can keep adding to it.
     *
     * @param stopsToHighlight the stops to ring (empty for none)
     */
    public void setHighlightedStops(List<BusStop> stopsToHighlight) {
        highlightedStops = stopsToHighlight;
    }

    /**
     * Sets where the map asks how far through the latest simulation step the display is, so buses can
     * be drawn smoothly between steps.
     *
     * @param stepFraction gives a fraction from 0 to 1
     */
    public void setStepFraction(DoubleSupplier stepFraction) {
        this.stepFraction = stepFraction;
    }

    /**
     * Renders the whole map: background, grid, bus routes, stops, buses, and the visual aids shown
     * while editing.
     */
    public void draw() {
        // Validate canvas bounds and compute the (square) cell size, centering the grid in the canvas
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) { return; }

        double available = Math.min(width, height) - 2 * MARGIN;
        if (available <= 0) { return; }

        cellSize = available / GRID_SIZE;
        originX = (width - cellSize * GRID_SIZE) / 2;
        originY = (height - cellSize * GRID_SIZE) / 2;

        double lineWidth = 7.5;

        // Scale bus icon size cleanly with cell size
        iconSize = Math.max(26, Math.min(56, cellSize * 0.5));

        GraphicsContext drawingBrush = canvas.getGraphicsContext2D();
        drawingBrush.setFill(Palette.MAP_BACKGROUND);
        drawingBrush.fillRect(0, 0, width, height);

        // Draw layers in back-to-front order so buses appear above stops and routes
        drawGrid(drawingBrush, lineWidth);
        drawScaleBar(drawingBrush);
        drawRoutes(drawingBrush, lineWidth);
        drawStops(drawingBrush);
        drawBuses(drawingBrush);
        drawEditingAids(drawingBrush);
    }

    /**
     * Converts a grid X coordinate into a canvas pixel X coordinate.
     *
     * @param gridX grid column coordinate
     * @return pixel X coordinate
     */
    public double pointX(double gridX) {
        return originX + gridX * cellSize;
    }

    /**
     * Converts a grid Y coordinate into a canvas pixel Y coordinate.
     *
     * @param gridY grid row coordinate
     * @return pixel Y coordinate
     */
    public double pointY(double gridY) {
        return originY + gridY * cellSize;
    }

    /**
     * Snaps a canvas pixel position to a valid grid intersection if within snap tolerance.
     *
     * @param pixelX X position in canvas pixels
     * @param pixelY Y position in canvas pixels
     * @return {@code [gridX, gridY]} if within bounds and tolerance, or {@code null} otherwise
     */
    public int[] pickGridPoint(double pixelX, double pixelY) {
        if (cellSize <= 0) {
            return null;
        }

        int gridX = (int) Math.round((pixelX - originX) / cellSize);
        int gridY = (int) Math.round((pixelY - originY) / cellSize);

        if (gridX < 0 || gridY < 0 || gridX > GRID_SIZE || gridY > GRID_SIZE) {
            return null;
        }

        double snapTolerance = Math.max(14, cellSize * 0.4);
        if (Math.abs(pixelX - pointX(gridX)) > snapTolerance || Math.abs(pixelY - pointY(gridY)) > snapTolerance) {
            return null;
        }
        return new int[]{gridX, gridY};
    }

    /**
     * Hit-tests a pixel coordinate against each bus's drawn icon position, using the same pixel
     * math as {@link #drawBuses(GraphicsContext)}.
     *
     * @param pixelX click X coordinate in canvas pixels
     * @param pixelY click Y coordinate in canvas pixels
     * @return the matching {@link Bus}, or {@code null} if no bus icon is within hit radius
     */
    public Bus findBusAt(double pixelX, double pixelY) {
        double hitRadius = Math.max(16, iconSize / 2.0);
        List<Bus> buses = network.getBuses();
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getDrawLocation(stepFraction.getAsDouble());
            double shift = ((i % 3) - 1) * 6;
            double centerX = pointX(location.getX()) + shift;
            double centerY = pointY(location.getY()) + shift;
            double distance = Math.hypot(pixelX - centerX, pixelY - centerY);
            if (distance <= Math.max(hitRadius, iconSize * bus.getIconScale() / 2.0)) {
                return bus;
            }
        }
        return null;
    }

    /**
     * Draws the grid lines and outer border on the canvas.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     * @param lineWidth    stroke width for grid lines and border
     */
    private void drawGrid(GraphicsContext drawingBrush, double lineWidth) {
        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setStroke(Palette.GRID_LINE);
        drawingBrush.setLineWidth(lineWidth);
        for (int i = 0; i <= GRID_SIZE; i++) {
            drawingBrush.strokeLine(pointX(i), pointY(0), pointX(i), pointY(GRID_SIZE)); // Vertical line
            drawingBrush.strokeLine(pointX(0), pointY(i), pointX(GRID_SIZE), pointY(i)); // Horizontal line
        }

        drawingBrush.setLineJoin(StrokeLineJoin.ROUND);
        drawingBrush.setStroke(Palette.GRID_BORDER);
        drawingBrush.setLineWidth(lineWidth);
        drawingBrush.strokeRect(pointX(0), pointY(0), pointX(GRID_SIZE) - pointX(0), pointY(GRID_SIZE) - pointY(0));
    }

    /**
     * Draws a scale bar under the bottom-left corner of the grid, so distances on the map can be read in
     * kilometres.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    private void drawScaleBar(GraphicsContext drawingBrush) {
        double left = pointX(0);
        double right = pointX(SCALE_BAR_CELLS);
        double y = pointY(GRID_SIZE) + SCALE_BAR_OFFSET;

        drawingBrush.save();
        drawingBrush.setStroke(Palette.LABEL_TEXT);
        drawingBrush.setFill(Palette.LABEL_TEXT);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeLine(left, y, right, y);
        drawingBrush.strokeLine(left, y - 5, left, y + 5);
        drawingBrush.strokeLine(right, y - 5, right, y + 5);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 12));
        drawingBrush.fillText(String.format("%.0f km", SCALE_BAR_CELLS * Scale.CELL_KM), right + 8, y + 4);
        drawingBrush.restore();
    }

    /**
     * Draws every stop; each stop draws itself in its own colours.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    private void drawStops(GraphicsContext drawingBrush) {
        double unit = Math.max(MIN_STOP_UNIT, iconSize * STOP_SIZE_FACTOR);

        for (BusStop stop : network.getStops()) {
            Location location = stop.getLocation();
            stop.draw(drawingBrush, pointX(location.getX()), pointY(location.getY()), unit);
        }
    }

    /**
     * Delegates drawing of all buses to {@link Bus#draw} with slight per-slot offset so overlapping
     * buses at the same intersection remain visible.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    private void drawBuses(GraphicsContext drawingBrush) {
        List<Bus> buses = network.getBuses();
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getDrawLocation(stepFraction.getAsDouble());
            double shift = ((i % 3) - 1) * 6;
            bus.draw(drawingBrush, pointX(location.getX()) + shift, pointY(location.getY()) + shift, iconSize,
                    Palette.LABEL_TEXT, Palette.LABEL_OUTLINE, Palette.REROUTE_TAG);
        }
    }

    /**
     * Computes a small pixel offset for a bus's route lines based on its index so parallel
     * route segments do not completely overlap.
     *
     * @param busIndex index of the bus in the bus list
     * @return offset in pixels
     */
    private double shiftOf(int busIndex) {
        return ((busIndex % 3) - 1) * 4;
    }

    /**
     * Delegates route rendering to each {@link Bus#drawRoute}.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     * @param lineWidth    base stroke width used to derive route line width
     */
    private void drawRoutes(GraphicsContext drawingBrush, double lineWidth) {
        List<Bus> buses = network.getBuses();
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            double shift = shiftOf(i);
            bus.drawRoute(drawingBrush, lineWidth, shift, this::pointX, this::pointY);
        }
    }

    /**
     * Draws visual aids for editing: open intersection dots while adding stops, and highlight
     * rings around the stops picked for a new or edited bus route.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    private void drawEditingAids(GraphicsContext drawingBrush) {
        if (showOpenIntersections) {
            drawingBrush.save();
            drawingBrush.setFill(Palette.OPEN_INTERSECTION);
            for (int gx = 0; gx <= GRID_SIZE; gx++) {
                for (int gy = 0; gy <= GRID_SIZE; gy++) {
                    if (network.findStopAt(gx, gy) == null) {
                        double x = pointX(gx);
                        double y = pointY(gy);
                        drawingBrush.fillOval(x - 4, y - 4, 8, 8);
                    }
                }
            }
            drawingBrush.restore();
        }

        if (!highlightedStops.isEmpty()) {
            drawingBrush.save();
            drawingBrush.setStroke(Palette.PICKED_STOP_RING);
            drawingBrush.setLineWidth(3);
            for (BusStop stop : highlightedStops) {
                Location location = stop.getLocation();
                double x = pointX(location.getX());
                double y = pointY(location.getY());
                double ringSize = iconSize * 0.9;
                drawingBrush.strokeOval(x - ringSize, y - ringSize, ringSize * 2, ringSize * 2);
            }
            drawingBrush.restore();
        }
    }
}
