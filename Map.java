import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * <p> <b>Project Name:</b> Bus Rerouting
 * <p> <b>Class Name:</b> Map
 * <p> <b>Purpose:</b> .
 * @author
 *       <p>- Ahmad Balfakeeh
 *       <p>- Abdullah Balubaid
 *       <p>- Salam Kanno
 *       <p>- Omar AbuSabaah
 * @version v1.0 11/22/26
 */


public class Map extends Application {

    // step 1
    private final Canvas canvas = new Canvas();
    private static final int GRID_SIZE = 20; 
    private static final double MARGIN = 46;
    // step 2
    private double cellWidth;
    private double cellHeight;

    // step 2
    private static final int NUMBER_OF_STOPS = 7; // ??????????

    // step 2
    private final List<BusStop> stops = new ArrayList<>();
    private final Random random = new Random();

    // step 3
    private static final int NUMBER_OF_BUSES = 3;
    private final List<Bus> buses = new ArrayList<>();
    private final List<Image> busIcons = new ArrayList<>();
    private double iconSize;
    private int builtIconSize = -1;
    private File iconFolder;

    // step 4
    // StandardBus (speed 30) drives 1.5 cells per second
    private static final double CELLS_PER_SECOND_PER_SPEED = 0.05;     
    // nextStopIndexes.get(i) = which stop of its route bus i is driving to
    private final List<Integer> nextStopIndexes = new ArrayList<>();



            
    // Color Palette
    private static final Color WHITE = Color.web("#ffffff");
    private static final Color GREY1 = Color.web("#c8c8c8");
    private static final Color GREY2 = Color.web("#b4b4b4");
    private static final Color ORANGE = Color.web("#f5a623");
    private static final Color NAVY1 = Color.web("#2d3548");
    private static final Color NAVY2 = Color.web("#1f2733");
    private static final Color CADETBLUE = Color.web("#a9b4c4");
    
    
    @Override 
    public void start(Stage stage) {
        // step 1
        Pane pane = new Pane(canvas);
    
        DoubleProperty canvasWidth = canvas.widthProperty();
        ReadOnlyDoubleProperty paneWidth = pane.widthProperty();
        canvasWidth.bind(paneWidth);

        DoubleProperty canvasHeight = canvas.heightProperty();
        ReadOnlyDoubleProperty paneHeight = pane.heightProperty();
        canvasHeight.bind(paneHeight);

        paneWidth.addListener((observable, oldWidth, newWidth) -> draw());
        paneHeight.addListener((observable, oldHeight, newHeight) -> draw()); 

        // step 2 + step 3
        pane.setOnMouseClicked(e -> {createRandomStops(NUMBER_OF_STOPS); createBuses(); draw();});

        createRandomStops(NUMBER_OF_STOPS);
        createBuses();  // step 3

        // step 4
        new AnimationTimer() {
            private long previousTime = 0;

            @Override 
            public void handle(long currentTime) {
                if (previousTime == 0) {
                    previousTime = currentTime;
                    return;
                }
                double seconds = (currentTime - previousTime) /  1_000_000_000.0;
                previousTime = currentTime;

                moveBuses(Math.min(seconds, 0.1));
                draw();
            }

        }.start();

        // step 1
        stage.setTitle("Bus Rerouting");
        stage.setScene(new Scene(pane, 900, 700));
        stage.show();

    }
    
        // step 2
    private void createRandomStops(int n) {
        stops.clear();

        List<int[]> intersections = new ArrayList<>();

        for (int x = 0; x <= GRID_SIZE; x++) {
            for (int y = 0; y <= GRID_SIZE; y++) {
                intersections.add(new int[]{x, y});
            }
        }

        Collections.shuffle(intersections, random); // for generating a random bus stop locations

        for (int i = 0; i < n; i++) {
            int[] point = intersections.get(i);
            BusStop stop = new BusStop(point[0], point[1]);
            stop.updatePassengers(3 + random.nextInt(28)); // Random passengers allocation
            stops.add(stop);
        }

    }

    // step 3
    private void createBuses() {
        buses.clear();
        
        // step 4
        nextStopIndexes.clear(); 

        List<ArrayList<Location>> routes = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            routes.add(new ArrayList<Location>());
        }

        for (int i = 0; i < stops.size(); i++) {
            BusStop currentStop = stops.get(i);
            Location currentStopLocation = currentStop.getLocation();
            ArrayList<Location> route = routes.get(i % NUMBER_OF_BUSES);
            route.add(currentStopLocation);

        }

        float firstHue = random.nextFloat();
        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            float hue = (float) ( (firstHue + i * 0.618033988749895) % 1.0);
            Color busColor = Color.hsb(hue * 360, 0.80 , 0.80);

            Bus bus;

            if (i % 3 == 0) {
                bus = new StandardBus(busColor, routes.get(i));

            }

            else if (i % 3 == 1) {
                bus = new MiniBus(busColor, routes.get(i));
            }

            else {
                bus = new DoubleDecker(busColor, routes.get(i));
            }

            ArrayList<Location> temporaryRoute = routes.get(i);
            Location temporaryLocation = temporaryRoute.get(0);

            bus.updateLocation(temporaryLocation);

            // step 4
            int firstDestination = 1 % temporaryRoute.size();
            bus.updateDestination(temporaryRoute.get(firstDestination));
            nextStopIndexes.add(firstDestination);


            buses.add(bus);

        }

        builtIconSize = -1;
    }
    
    private void draw() {
        // step 1
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if ( width <= 0 || height <= 0 ) { return; }

        // // Size of one grid
        cellWidth = ( width - 2 * MARGIN ) / GRID_SIZE;
        cellHeight = ( height - 2 * MARGIN ) / GRID_SIZE;

        double lineWidth = 7.5;

        // step 3
        iconSize = Math.max(26, Math.min(56, Math.min(cellWidth, cellHeight) * 0.5));

         // step 3: make the coloured bus pictures again, but only if the buses or the icon size changed
        if ( (int) iconSize != builtIconSize) {
            buildBusIcons();
        }


        GraphicsContext drawingBrush = canvas.getGraphicsContext2D();
        drawingBrush.setFill(WHITE);
        drawingBrush.fillRect(0, 0, width, height);
        
        // step 2
        drawGrid(drawingBrush, lineWidth);
        // step 4
        drawRoutes(drawingBrush, lineWidth);

        drawStops(drawingBrush);

        // step 3 (drawn last, so busses are on top of the stops)
        drawBuses(drawingBrush);

        

        }

        // step 2
        private void drawGrid(GraphicsContext drawingBrush, double lineWidth) {
        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setStroke(GREY1);
        drawingBrush.setLineWidth(lineWidth);
        // loop over the entire page
        for (int i = 0; i <= GRID_SIZE; i++) {
            drawingBrush.strokeLine(pointX(i), pointY(0), pointX(i), pointY(GRID_SIZE)); // Vertical Line Drawing
            drawingBrush.strokeLine(pointX(0), pointY(i), pointX(GRID_SIZE), pointY(i)); // Horizontal Line Drawing
        }

        drawingBrush.setLineJoin(StrokeLineJoin.ROUND);
        drawingBrush.setStroke(GREY2);
        drawingBrush.setLineWidth(lineWidth);
        drawingBrush.strokeRect(pointX(0), pointY(0), pointX(GRID_SIZE) - pointX(0), pointY(GRID_SIZE) - pointY(0));
    }

     private double pointX(double gridX) {
            return MARGIN + gridX * cellWidth;
        }
        
    private double pointY(double gridY) {
            return MARGIN + gridY * cellHeight;
        }

    private void drawStops(GraphicsContext drawingBrush) {
        double unit = Math.max(7, iconSize * 0.26);

        
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 13)); // we can change the font here
        

        // step 3: updates
        for (int i = 0; i < stops.size(); i++) {
            BusStop stop = stops.get(i);
            Location location = stop.getLocation();

            double centerX = pointX(location.getX());
            double centerY = pointY(location.getY());
            drawStopIcon(drawingBrush, centerX, centerY, unit);

            String text = "Stop" + (i+1) + " [" + stop.getPassenger() + "]";
            stopLabel(drawingBrush, text, centerX, centerY - unit * 1.95, NAVY2);
        }

    }
        // step 2
    private void drawStopIcon(GraphicsContext drawingBrush, double centerX, double centerY, double unit) {
        double poleX = centerX - unit - 3;

        // pole
        drawingBrush.setLineCap(StrokeLineCap.BUTT);
        drawingBrush.setStroke(CADETBLUE);
        drawingBrush.setLineWidth(Math.max(2, unit * 0.2));
        drawingBrush.strokeLine(poleX, centerY - unit * 1.5, poleX, centerY + unit * 0.9);

        // little orange flag on top of the pole
        drawingBrush.setFill(ORANGE);
        drawingBrush.fillRect(poleX - unit * 0.3, centerY - unit * 1.55, unit * 0.65, unit * 0.55);

        // dark sign box with orange border
        drawingBrush.setFill(NAVY1);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);
        drawingBrush.setStroke(ORANGE);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);

        // thicker orange bar along the top of the box
        drawingBrush.setFill(ORANGE);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, unit * 0.4);
    }
        // step 2
    private void stopLabel(GraphicsContext drawingBrush, String text, double x, double y, Color color) {
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(WHITE);
        drawingBrush.strokeText(text, x, y);
        drawingBrush.setFill(color);
        drawingBrush.setFill(color);
        drawingBrush.fillText(text, x, y);
    }

    // step 3 - the busses

    private void drawBuses(GraphicsContext drawingBrush) {
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 12));

        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getCurrentLocation();

            double shift = ( (i % 3) - 1) * 6;
            double centerX = pointX(location.getX()) + shift;
            double centerY = pointY(location.getY()) + shift;

            Image icon = busIcons.get(i);

            double bottomOfBus;
            if (icon != null) {
                drawingBrush.drawImage(icon, centerX - icon.getWidth() / 2, centerY - icon.getHeight() / 2);
                bottomOfBus = centerY + icon.getHeight() / 2;

            }
            else {
                Color busPlaceHolderRect = bus.getColor(); 
                drawingBrush.setFill(Color.color(busPlaceHolderRect.getRed(), busPlaceHolderRect.getGreen(), busPlaceHolderRect.getBlue()));
                drawingBrush.fillRect(centerX - iconSize / 2, centerY - iconSize / 2, iconSize, iconSize);

                bottomOfBus = centerY + iconSize / 2;
            }

            String text = busPrefix(bus) + (i + 1) + " " + bus.getCurrentPassengers() + "/" + bus.getPassengerCapacity();
            stopLabel(drawingBrush, text, centerX, bottomOfBus + 13, NAVY2);
        }
    }
        // step 3
    private String busPrefix(Bus bus) {
        if (bus instanceof StandardBus)  { return "B"; }
        if (bus instanceof MiniBus)      { return "M"; }
        if (bus instanceof DoubleDecker) { return "D"; }
        return "N/A";
    }
        // step 3
    private String iconFileName(Bus bus) {
        if (bus instanceof StandardBus)  { return "StandardBusIcon.png"; }
        if (bus instanceof MiniBus)      { return "MiniBusIcon.png"; }
        if (bus instanceof DoubleDecker) { return "DoubleDeckerIcon.png"; }
        return "N/A";
    }
        // step 3 
    private void buildBusIcons() {
        busIcons.clear();
        int size = (int) iconSize;  
     
        for (Bus bus : buses) {
            busIcons.add(makeBusIcon(bus, size));
        }
        builtIconSize = size;
    
    }

        // step 3
    private Image makeBusIcon(Bus bus, int size) {
        File file = findFile(iconFileName(bus));
        if (file == null) {
            System.out.println("Icon not found: " + iconFileName(bus));
            return null;
        }

        Image picture;
        // try with resource
        try (InputStream stream = new FileInputStream(file)) {

        picture = new Image(stream, size, size, true, true);
        
        } 

        catch (IOException e) {
            return null;
        }

        if (picture.isError()) { return null; }

        return paintBus(picture, bus.getColor());

    }

    private Image paintBus(Image picture, Color busColor) {
        int   width =  (int) picture.getWidth();
        int   height = (int) picture.getHeight();
        int totalPixels = width * height;
        int[] pixels = new int[totalPixels];

        picture.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        

        boolean[] outside = new boolean[totalPixels]; 
        int[] toVisit = new int[totalPixels];
        int toVisitCount = 0;
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;

            boolean onBorder = ( x == 0 || y == 0 || x == width - 1 || y == height - 1);
            
            if ( onBorder && alphaOf(pixels[i]) < 128) {
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
            }

            else {
                newAlpha = 255;
                shade = alpha / 255.0;
            }

            int red =   (int) (busColor.getRed()   * shade * 255);
            int green = (int) (busColor.getGreen() * shade * 255);
            int blue =  (int) (busColor.getBlue()  * shade * 255);
            painted[i] = (newAlpha << 24) | (red << 16) | (green << 8) | blue;

         }

         WritableImage result = new WritableImage(width, height);
         result.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), painted, 0, width);
         return result;
    
    }

    private int alphaOf(int pixel) {
        return (pixel >>> 24) & 255;
    }

    
private static final String ICON_DIR = "\"C:\\Users\\abdul\\Downloads\\EE364project\\assets\""; 

    private File findFile(String fileName) {
        File folder = (iconFolder != null) ? iconFolder : new File(ICON_DIR);
        File file = new File(folder, fileName);

        if (file.isFile()) {
            iconFolder = folder;
            return file;
        }

        File workingFolder = new File(System.getProperty("user.dir"));
        File rootFile = new File(workingFolder, fileName);
        if (rootFile.isFile()) {
            iconFolder = workingFolder;
            return rootFile;
        }

        File found = searchFolder(workingFolder, fileName, 4);
        if (found != null) {
            iconFolder = found.getParentFile();
            return found;
        }
        return null;
    
    }

    private File searchFolder(File folder, String fileName, int depthLeft) {
        File[] items = (folder == null) ? null: folder.listFiles();

        if (items == null || depthLeft < 0) { return null; };

        for (File item : items) {
            if (item.isFile() && item.getName().equalsIgnoreCase(fileName)) { return item; }
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

    // step 4
    private double shiftOf(int busIndex) {
        return ( (busIndex % 3) - 1) * 4;
    }

    private void drawRoutes(GraphicsContext drawingBrush, double lineWidth) {

        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setLineWidth(lineWidth * 0.5);

        for (int i = 0; i < buses.size(); i++){
            Bus bus = buses.get(i);
            ArrayList<Location> route = bus.getRoute();
            
            double shift = shiftOf(i);
            Color busColor = bus.getColor();

            drawingBrush.setStroke(Color.color(busColor.getRed(), busColor.getGreen(), busColor.getBlue(), 0.8));

            if (bus instanceof Reroutable) {
                drawingBrush.setLineDashes(12, 8);
            }

            for (int j = 0; j < route.size(); j ++) {
                Location from = route.get(j);
                Location to   = route.get((j + 1) % route.size());

                double fromX = pointX(from.getX()) + shift;
                double fromY = pointY(from.getY()) + shift;
                double toX   = pointX(to.getX())   + shift;
                double toY   = pointY(to.getY())   + shift;

                drawingBrush.strokeLine(fromX, fromY, toX, fromY);
                drawingBrush.strokeLine(toX,   fromY, toX, toY);
            }

            drawingBrush.setLineDashes((double[]) null);

        }
    }

        // step 4
    private void moveBuses(double seconds) {
        for (int i = 0; i < buses.size(); i++) { 
            Bus bus = buses.get(i);

            double distance = bus.getSpeed() * CELLS_PER_SECOND_PER_SPEED * seconds;

            Location currentLocation = bus.getCurrentLocation();
            Location destination     = bus.getDestination();
            double x = currentLocation.getX();
            double y = currentLocation.getY();

            // ?????
            double differenceX = destination.getX() - x;
            double stepX = Math.min(distance, Math.abs(differenceX));
            if (stepX == Math.abs(differenceX)) { // CHECK LATER IF ISSUE OCCURRED
                x = destination.getX();
            }
            else {
                x = x + Math.signum(differenceX) * stepX; // if differenceX < 0 --> x = x - stepX
                                                          // if differenceX > 0 --> x = x + stepX
            }
            
            distance = distance - stepX;

            double differenceY = destination.getY() - y;
            double stepY = Math.min(distance, Math.abs(differenceY));
            if (stepY == Math.abs(differenceY)) {
                y = destination.getY();
            }
            else {
                y = y + Math.signum(differenceY) * stepY;
            }

            bus.updateLocation(x, y);


            if (x == destination.getX() && y == destination.getY()) {
                ArrayList<Location> route = bus.getRoute();
                int next = (nextStopIndexes.get(i) + 1) % route.size();
                nextStopIndexes.set(i, next);
                bus.updateDestination(route.get(next));
                
            }

        }
    }

}

