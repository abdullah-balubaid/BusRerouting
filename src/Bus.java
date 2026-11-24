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
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Bus
    * <p> <b>Purpose:</b> Abstract bus extending Vehicle with route waypoints and last visited stop tracking.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.2 11/23/26

    
**/


public abstract class Bus extends Vehicle {
    protected ArrayList<Location> route = new ArrayList<Location>(); 
    private Location lastVisitedStop; // NEW
    private String busPrefix; // NEW
    private double loadingSecondsRemaining;
    

    protected Bus(int Capacity, int Speed, Color color, ArrayList<Location> route, String busPrefix){
        super(Capacity, Speed, color);

        this.busPrefix = busPrefix;

        if (route == null){
            throw new NullPointerException("route must not be null.");
        }
        else if (route.isEmpty()){
            throw new IllegalArgumentException("route must not be empty.");
        }
        this.route = new ArrayList<>(route);
    }

    public ArrayList<Location> getRoute() {
        return new ArrayList<>(route);
    }

    public void setLastVisitedStop(Location stop){
        if (stop == null){
            throw new NullPointerException("Last visited stop must not be null.");
        }
        lastVisitedStop = new Location(stop.getX(), stop.getY());
    }

    public Location getLastVisitedStop(){
        if (lastVisitedStop == null){
            return null;
        }
        return new Location(lastVisitedStop.getX(), lastVisitedStop.getY());
    } // NEW

    public String getBusPrefix() { return busPrefix; }

    // note 5: by default any bus may serve any stop; DoubleDecker overrides this to restrict
    // itself to high-priority stops only.
    public boolean canServe(BusStop stop) {
        return true;
    }

    
    // Loading the Bus
    public void startLoading(double seconds) {
        loadingSecondsRemaining = seconds;
    }

    public boolean isLoading(double seconds) {
        if (loadingSecondsRemaining <= 0) {
            return false;
        }

        loadingSecondsRemaining -= seconds;
        return true;
    }

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



    // Drawing Section
    public void draw(GraphicsContext drawingBrush, int busNumber, double centerX, double centerY,
                     double iconSize, Image icon, Color labelColor, Color outlineColor) {

    drawingBrush.save();
    drawingBrush.setTextAlign(TextAlignment.CENTER);
    drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 12));
    
    double bottomOfBus;
    if (icon != null) {
        drawingBrush.drawImage(icon, centerX - icon.getWidth() / 2, centerY - icon.getHeight() / 2);
        bottomOfBus = centerY + icon.getHeight() / 2;

    }
    else {
        Color busPlaceHolderRect = getColor(); 
        drawingBrush.setFill(Color.color(busPlaceHolderRect.getRed(), busPlaceHolderRect.getGreen(), busPlaceHolderRect.getBlue()));
        drawingBrush.fillRect(centerX - iconSize / 2, centerY - iconSize / 2, iconSize, iconSize);

        bottomOfBus = centerY + iconSize / 2;
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

    public abstract String getIconFileName();

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
        }
        catch (IOException e) {
            return null;
        }

        if (picture.isError()) {
            return null;
        }
        
        return paintBus(picture, getColor());

    } 

    private Image paintBus(Image picture, Color busColor) {
        int width = (int) picture.getWidth();
        int height = (int) picture.getHeight();
        int totalPixels = width * height;
        int[] pixels = new int[totalPixels];

        picture.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        boolean[] outside = new boolean[totalPixels];
        int[] toVisit = new int[totalPixels];
        int toVisitCount = 0;
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;

            boolean onBorder = (x == 0 || y == 0 || x == width - 1 || y == height - 1);

            if (onBorder && alphaOf(pixels[i]) < 128) {
                outside[i] = true;
                toVisit[toVisitCount++] = i;
            }
        }

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
                newAlpha = alpha;
                shade = 1;
            } else {
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

    private int alphaOf(int pixel) {
        return (pixel >>> 24) & 255;
    }

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

    // drawing route
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

            drawingBrush.strokeLine(fromX, fromY, toX, fromY);
            drawingBrush.strokeLine(toX, fromY, toX, toY);
        }

        drawingBrush.setLineDashes((double[]) null);
        drawingBrush.restore();
    }
  
}