import java.util.ArrayList;

import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import java.util.function.DoubleUnaryOperator;
import javafx.scene.shape.StrokeLineCap;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Bus</p>
 * <p><b>Purpose:</b> Abstract bus extending {@link Vehicle} with route waypoints, last-visited stop
 * tracking, loading state, icon loading/recoloring, and route/bus canvas rendering.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.3 11/25/26
 */
public abstract class Bus extends Vehicle {
    protected ArrayList<Location> route = new ArrayList<Location>(); // Ordered sequence of route stop locations
    private Location lastVisitedStop;                                // Copy of the most recently visited stop location
    private String busPrefix;                                        // Short display prefix for this bus type (e.g., "S", "M", "DD")
    private double loadingSecondsRemaining;                          // Remaining simulated loading pause in seconds

    /**
     * Constructs a {@code Bus} extending {@link Vehicle} with validated non-null, non-empty route waypoints.
     *
     * @param Capacity  maximum passenger capacity
     * @param Speed     movement speed rating
     * @param color     display/tinting color of the bus
     * @param route     non-null, non-empty list of route {@link Location} stops
     * @param busPrefix short label prefix identifying the bus type
     * @throws NullPointerException     if {@code route} is {@code null} (or if {@code color} is {@code null} in {@code super})
     * @throws IllegalArgumentException if {@code route} is empty (or if capacity/speed are non-positive in {@code super})
     */
    protected Bus(int Capacity, int Speed, Color color, ArrayList<Location> route, String busPrefix) {
        super(Capacity, Speed, color);

        this.busPrefix = busPrefix;

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
     * Records a defensive copy of the most recently visited stop location.
     *
     * @param stop location of the visited stop (must not be {@code null})
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    public void setLastVisitedStop(Location stop) {
        if (stop == null) {
            throw new NullPointerException("Last visited stop must not be null.");
        }
        lastVisitedStop = new Location(stop.getX(), stop.getY());
    }

    /**
     * Returns a defensive copy of the last visited stop location, or {@code null} if none has been recorded.
     *
     * @return copy of {@link #lastVisitedStop}, or {@code null}
     */
    public Location getLastVisitedStop() {
        if (lastVisitedStop == null) {
            return null;
        }
        return new Location(lastVisitedStop.getX(), lastVisitedStop.getY());
    }

    /**
     * Returns the short display prefix for this bus type.
     *
     * @return bus prefix string
     */
    public String getBusPrefix() {
        return busPrefix;
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
     * Returns the operating cost incurred per simulated minute for running this bus type.
     *
     * @return operating cost per simulated minute
     */
    // note 1: running cost per simulated minute, in whatever cost unit the project uses. Each
    // subclass overrides this with its own figure - a bigger bus costs more to operate.
    public abstract double getOperatingCostPerMinute();

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
    public boolean isLoading(double seconds) {
        if (loadingSecondsRemaining <= 0) {
            return false;
        }

        loadingSecondsRemaining -= seconds;
        return true;
    }

    /**
     * Performs a standard stop arrival: unloads current passengers, boards waiting passengers up to
     * capacity, records the visited stop, and starts the loading timer.
     *
     * @param stop           the {@link BusStop} arrived at (must not be {@code null})
     * @param loadingSeconds loading pause duration in seconds
     * @throws NullPointerException if {@code stop} is {@code null}
     */
    public void arriveAt(BusStop stop, double loadingSeconds) {
        if (stop == null) {
            throw new NullPointerException("Stop must not be null.");
        }

        setCurrentPassengers(0);
        int gettingOn = stop.boardPassengers(getPassengerCapacity());
        setCurrentPassengers(gettingOn);
        setLastVisitedStop(stop.getLocation());
        startLoading(loadingSeconds);
    }

    /**
     * Renders the bus icon (or a colored fallback rectangle if {@code icon} is {@code null}) and its
     * outlined status label below the vehicle on the canvas.
     *
     * @param drawingBrush canvas {@link GraphicsContext}
     * @param busNumber    1-based display number for the bus
     * @param centerX      center X coordinate in canvas pixels
     * @param centerY      center Y coordinate in canvas pixels
     * @param iconSize     fallback square size in pixels
     * @param icon         pre-rendered/tinted bus {@link Image}, or {@code null} for fallback box
     * @param labelColor   fill color for the status text
     * @param outlineColor stroke outline color for the status text
     * @param rerouting    Indicate whether current bus is reoruting or not
     * @param rerouteColor text color for the rerouting status
     */
    public void draw(GraphicsContext drawingBrush, int busNumber, double centerX, double centerY,
                     double iconSize, Image icon, Color labelColor, Color outlineColor, boolean rerouting, Color rerouteColor) {
        drawingBrush.save();
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 12));

        double topOfBus;
        double bottomOfBus;
        if (icon != null) {
            drawingBrush.drawImage(icon, centerX - icon.getWidth() / 2, centerY - icon.getHeight() / 2);
            topOfBus    = centerY - icon.getHeight() / 2;
            bottomOfBus = centerY + icon.getHeight() / 2;
            
        } else {
            Color busPlaceHolderRect = getColor();
            drawingBrush.setFill(Color.color(busPlaceHolderRect.getRed(), busPlaceHolderRect.getGreen(), busPlaceHolderRect.getBlue()));
            drawingBrush.fillRect(centerX - iconSize / 2, centerY - iconSize / 2, iconSize, iconSize);
            topOfBus    = centerY + iconSize / 2;
            bottomOfBus = centerY + iconSize / 2;
            
        }

        if (rerouting) {
            double badgeY = topOfBus - 5;
            drawingBrush.setLineWidth(3);
            drawingBrush.setStroke(outlineColor);
            drawingBrush.strokeText("REROUTE", centerX, badgeY);
            drawingBrush.setFill(rerouteColor);
            drawingBrush.fillText("REROUTE", centerX, badgeY);
        }

        String text = getBusPrefix() + (busNumber) + " " + getCurrentPassengers() + "/" + getPassengerCapacity();
        double labelY = bottomOfBus + 13;
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(outlineColor);
        drawingBrush.strokeText(text, centerX, labelY);
        drawingBrush.setFill(labelColor);
        drawingBrush.fillText(text, centerX, labelY);

        drawingBrush.restore();
    }

    /**
     * Returns the image file name for this bus subclass's icon.
     *
     * @return icon file name
     */
    public abstract String getIconFileName();

    /**
     * Locates, loads, scales, and tints this bus's icon image with {@link #getColor()}.
     *
     * @param iconDir       configured icon directory path (may include quotes or be {@code null})
     * @param workingFolder fallback working directory to search if not found in {@code iconDir}
     * @param size          target width/height in pixels (aspect ratio preserved)
     * @return tinted {@link Image}, or {@code null} if missing or unreadable
     */
    public Image makeBusIcon(String iconDir, File workingFolder, int size) {
        String fileName = getIconFileName();

        File file = findFile(fileName, iconDir, workingFolder);
        if (file == null) {
            System.out.println("Icon not found " + fileName);
            return null;
        }

        Image picture;
        try (InputStream stream = new FileInputStream(file)) {
            picture = new Image(stream, size, size, true, true);
        } catch (IOException e) {
            return null;
        }

        if (picture.isError()) {
            return null;
        }

        return paintBus(picture, getColor());
    }

    /**
     * Flood-fills transparent exterior pixels from the image border so interior/body pixels can be
     * recolored with {@code busColor} while preserving anti-aliased edge shading and transparency.
     *
     * @param picture  source icon {@link Image}
     * @param busColor target tint {@link Color}
     * @return newly created tinted {@link WritableImage}
     */
    private Image paintBus(Image picture, Color busColor) {
        int width = (int) picture.getWidth();
        int height = (int) picture.getHeight();
        int totalPixels = width * height;
        int[] pixels = new int[totalPixels];

        picture.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        boolean[] outside = new boolean[totalPixels];
        int[] toVisit = new int[totalPixels];
        int toVisitCount = 0;

        // Seed flood fill from semi-/fully transparent border pixels
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;

            boolean onBorder = (x == 0 || y == 0 || x == width - 1 || y == height - 1);

            if (onBorder && alphaOf(pixels[i]) < 128) {
                outside[i] = true;
                toVisit[toVisitCount++] = i;
            }
        }

        // Propagate exterior mask across connected low-alpha pixels in 4-neighborhood
        while (toVisitCount > 0) {
            int i = toVisit[--toVisitCount];
            int x = i % width;
            int y = i / width;
            int next;

            next = i - 1;
            if (x > 0 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i + 1;
            if (x < width - 1 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i - width;
            if (y > 0 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i + width;
            if (y < height - 1 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }
        }

        int[] painted = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;
            int alpha = alphaOf(pixels[i]);

            boolean touchesOutside = (x > 0 && outside[i - 1] || (x < width - 1 && outside[i + 1])
                    || (y > 0 && outside[i - width]) || (y < height - 1 && outside[i + width]));

            int newAlpha;
            double shade;
            if (outside[i] || touchesOutside) {
                // Keep exterior and outer edge anti-aliasing transparent
                newAlpha = alpha;
                shade = 1;
            } else {
                // Solidify interior pixels and modulate brightness by original alpha
                newAlpha = 255;
                shade = alpha / 255.0;
            }

            int red = (int) (busColor.getRed() * shade * 255);
            int green = (int) (busColor.getGreen() * shade * 255);
            int blue = (int) (busColor.getBlue() * shade * 255);
            painted[i] = (newAlpha << 24) | (red << 16) | (green << 8) | blue;
        }

        WritableImage result = new WritableImage(width, height);
        result.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), painted, 0, width);
        return result;
    }

    /**
     * Extracts the 8-bit alpha channel ({@code 0..255}) from an ARGB packed integer pixel.
     *
     * @param pixel packed ARGB pixel value
     * @return alpha component in {@code [0, 255]}
     */
    private int alphaOf(int pixel) {
        return (pixel >>> 24) & 255;
    }

    /**
     * Searches for {@code fileName} in {@code iconDir}, then {@code workingFolder}, and finally up to
     * 4 levels deep inside {@code workingFolder}.
     *
     * @param fileName      target file name
     * @param iconDir       primary directory path (may be {@code null})
     * @param workingFolder fallback root folder (may be {@code null})
     * @return resolved {@link File}, or {@code null} if not found
     */
    private File findFile(String fileName, String iconDir, File workingFolder) {
        if (iconDir != null) {
            File folder = new File(iconDir.replace("\"", ""));
            File file = new File(folder, fileName);
            if (file.isFile()) {
                return file;
            }
        }

        if (workingFolder != null) {
            File rootFile = new File(workingFolder, fileName);
            if (rootFile.isFile()) {
                return rootFile;
            }

            File found = searchFolder(workingFolder, fileName, 4);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Recursively searches {@code folder} up to {@code depthLeft} subdirectories for a file matching
     * {@code fileName} (case-insensitive), skipping hidden/dot directories.
     *
     * @param folder    directory to search
     * @param fileName  target file name
     * @param depthLeft remaining recursion depth
     * @return matching {@link File}, or {@code null} if not found within depth limit
     */
    private File searchFolder(File folder, String fileName, int depthLeft) {
        File[] items = (folder == null) ? null : folder.listFiles();

        if (items == null || depthLeft < 0) {
            return null;
        }

        for (File item : items) {
            if (item.isFile() && item.getName().equalsIgnoreCase(fileName)) {
                return item;
            }
        }

        for (File item : items) {
            if (item.isDirectory() && !item.getName().startsWith(".")) {
                File found = searchFolder(item, fileName, depthLeft - 1);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
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
        if (route == null || route.isEmpty()) {
            return;
        }

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