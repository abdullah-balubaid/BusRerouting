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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import java.io.File;


import javafx.scene.image.Image;


/**
 * <p> <b>Project Name:</b> Bus Rerouting
 * <p> <b>Class Name:</b> Map
 * <p> <b>Purpose:</b> Create the map, bus stops, busses, move it on the map to the bus stops by connecting the backend logic to the GUI.
 * @author
 *       <p>- Ahmad Balfakeeh
 *       <p>- Abdullah Balubaid
 *       <p>- Salam Kanno
 *       <p>- Omar AbuSabaah
 * @version v1.1 11/23/26
 */


public class Map extends Application {
    
    // assets folder directory
    private static final String ICON_DIR = "\"C:\\Users\\abdul\\Downloads\\EE364project\\assets\""; 

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

    // step 4
    // StandardBus (speed 30) drives 1.5 cells per second
    private static final double CELLS_PER_SECOND_PER_SPEED = 0.05;     
    // nextStopIndexes.get(i) = which stop of its route bus i is driving to
    private final List<Integer> nextStopIndexes = new ArrayList<>();

    private static final double LOADING_SECONDS = 1.0;


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

        // step 5
        createArrivalRates();

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

        // step 5
        resetLoadingTimers();
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

        // step 3: updates
        for (int i = 0; i < stops.size(); i++) {
            BusStop stop = stops.get(i);
            Location location = stop.getLocation();
            double centerX = pointX(location.getX());
            double centerY = pointY(location.getY());
            stop.draw(drawingBrush, i + 1, centerX, centerY, unit,
                CADETBLUE, ORANGE, NAVY1, NAVY2, WHITE);

        }
    }

    // step 3 - the busses
    private void drawBuses(GraphicsContext drawingBrush) {
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getCurrentLocation();
            double shift = ( (i % 3) - 1) * 6;
            double centerX = pointX(location.getX()) + shift;
            double centerY = pointY(location.getY()) + shift;
            Image icon = busIcons.get(i);
            bus.draw(drawingBrush, i + 1, centerX, centerY, iconSize, icon, NAVY2, WHITE);
        }
    }
    
        // step 3 
    private void buildBusIcons() {
        busIcons.clear();
        int size = (int) iconSize;  
        File workingFolder = new File(System.getProperty("user.dir"));

        for (Bus bus : buses) {
            busIcons.add(bus.makeBusIcon(ICON_DIR, workingFolder, size));
        }
        
        builtIconSize = size;
    
    }

    // step 4
    private double shiftOf(int busIndex) {
        return ( (busIndex % 3) - 1) * 4;
    }

    private void drawRoutes(GraphicsContext drawingBrush, double lineWidth) {
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            double shift = shiftOf(i);
            bus.drawRoute(drawingBrush, lineWidth, shift, this::pointX, this::pointY);
        }
    }

        // step 4
    private void moveBuses(double seconds) {
        addNewPassengers(seconds);

        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);

            if (bus.isLoading(seconds)) {
                continue;
            }

            double distance = bus.getSpeed() * CELLS_PER_SECOND_PER_SPEED * seconds;
            bus.moveTowardDestination(distance);

            Location currentLocation = bus.getCurrentLocation();
            Location destination = bus.getDestination();

            if (currentLocation.getX() == destination.getX()
                    && currentLocation.getY() == destination.getY()) {
                arriveAtStop(i, bus, destination);

                ArrayList<Location> route = bus.getRoute();
                int next = (nextStopIndexes.get(i) + 1) % route.size();
                nextStopIndexes.set(i, next);
                bus.updateDestination(route.get(next));
            }
        }
    }

    // step 5
  private void createArrivalRates() {
        for (BusStop stop : stops) {
            stop.setArrivalRate(0.1 + 0.4 * random.nextDouble());
        }
    }
    private void addNewPassengers(double seconds) {
        for (BusStop stop : stops) {
            stop.advancePassengers(seconds);
        }
    }

    private void resetLoadingTimers() {
        for (Bus bus : buses) {
            bus.startLoading(0.0);
        }
    }

    private int findStopIndexAt(Location location) {
        for (int i = 0; i < stops.size(); i++) {
            Location stopLocation = stops.get(i).getLocation();

            if (stopLocation.getX() == location.getX() && stopLocation.getY() == location.getY()) {
                return i;
                }
            }
        return -1;
        }
        
    private void arriveAtStop(int busIndex, Bus bus, Location stopLocation) {
        int stopIndex = findStopIndexAt(stopLocation);
        if (stopIndex == -1) {
            return;
        }
        BusStop stop = stops.get(stopIndex);

        int gettingOff = bus.getCurrentPassengers();
        bus.arriveAt(stop, LOADING_SECONDS);
        int gettingOn = bus.getCurrentPassengers();

        System.out.println(bus.getBusPrefix() + (busIndex + 1) + " at Stop " + (stopIndex + 1) + ": "
                    + gettingOff + " got off, " + gettingOn + " got on, " + stop.getPassenger() + " still waiting");

        }

    }
