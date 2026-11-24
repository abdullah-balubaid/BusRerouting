import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;


import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;   // note 4/8: seed entry

import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;


/**
 * <p> <b>Project Name:</b> Bus Rerouting
 * <p> <b>Class Name:</b> Map
 * <p> <b>Purpose:</b> Create the map, bus stops, busses, move it on the map to the bus stops by connecting the backend logic to the GUI.
 * @author
 *       <p>- Ahmad Balfakeeh
 *       <p>- Abdullah Balubaid
 *       <p>- Salam Kanno
 *       <p>- Omar AbuSabaah
 * @version v1.2 11/24/26
 */


public class Map extends Application {
    
    // assets folder directory
    private static final String ICON_DIR = "\"C:\\Users\\abdul\\Downloads\\EE364project\\assets\""; 

    // step 1
    private final Canvas canvas = new Canvas();
    private static final int GRID_SIZE = 20; 
    private static final double MARGIN = 46;
    
    // step 8
    private Pane mapPane;
    private static final String DEFAULT_EDIT_HINT = "Right-click a stop = +10 waiting passengers.";

    // step 2
    private double cellWidth;
    private double cellHeight;

    // step 2
    private static final int NUMBER_OF_STOPS = 7; // ??????????
    private static final int MAX_NUMBER_OF_STOPS = 60;

    // step 2
    private final List<BusStop> stops = new ArrayList<>();
    // note 4/8: `random` is now explicitly seeded on every map generation, and the seed used is
    // remembered in currentSeed so it can be shown to the user and re-entered later to reproduce
    // the exact same stops, passenger counts, and bus colors.
    private Random random = new Random();
    private long currentSeed = random.nextLong();
    // step 6
    private final List<Integer> initialStopPassengers = new ArrayList<>();

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
    // note 3: together with Vehicle.getDestination()/updateDestination() (the bus's current,
    // possibly-rerouted target) this already acts as a "next destination" field: Destination is
    // WHERE the bus is driving right now, nextStopIndexes is where its own fixed route sends it
    // AFTER that. A bus only reads these two, never another bus's, so there is no interference.
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
    private static final Color RED = Color.web("#c62828");
    private static final Color GREEN = Color.web("#2e7d32");

    // step 6
    private static final double TOTAL_SIMULATED_MINUTES = 300.0; // modified
    private static final double SIMULATED_MINUTES_PER_SECOND = 6.0;
    private int phase = 0; // to determine which phase is running 0:no run, 1:phase1, 2:phase
    private boolean running = false;
    private boolean paused = false;
    private boolean relocationOn = false;
    private double simulatedMinutes = 0;
    private double phase1AverageWait = Double.NaN;
    private double phase2AverageWait = Double.NaN;

    // step 7
    private double allottedWaitMinutes = 5;
    private final List<Integer> reroutedTarget = new ArrayList<>();

    // step 8
    private static final int CLICK_MODE_NONE = 0;
    private static final int CLICK_MODE_ADD_STOP = 1;
    private static final int CLICK_MODE_BUILD_ROUTE = 2;
    private int clickMode = CLICK_MODE_NONE;
    private final List<BusStop> pendingRouteStops = new ArrayList<>();

    private ToggleButton addStopButton;
    private Button addBusButton;
    private Button finishBusButton;
    private Button cancelBusButton;
    private ComboBox<String> newBusTypeBox;
    private Label editHintLabel;
    private ListView<Bus> busStatusList; 
    
    // labels
    private Label timeLabel;
    private Label averageWaitLabel;
    private Label phase1Label;
    private Label phase2Label;
    private TextArea logArea;
    private Slider speedSlider;
    private Spinner<Integer> stopsSpinner;
    private Spinner<Integer> waitSpinner;

    // note 4: seed controls
    private Label seedLabel;
    private TextField seedField;
    
    
    @Override 
    public void start(Stage stage) {
        // step 1
        mapPane = new Pane(canvas);
    
        DoubleProperty canvasWidth = canvas.widthProperty();
        ReadOnlyDoubleProperty paneWidth = mapPane.widthProperty();
        canvasWidth.bind(paneWidth);

        DoubleProperty canvasHeight = canvas.heightProperty();
        ReadOnlyDoubleProperty paneHeight = mapPane.heightProperty();
        canvasHeight.bind(paneHeight);

        paneWidth.addListener((observable, oldWidth, newWidth) -> draw());
        paneHeight.addListener((observable, oldHeight, newHeight) -> draw()); 

        // step 8
        mapPane.setOnMouseClicked(event -> handleMapClick(event));   

        // step 2 + step 3
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
                seconds = Math.min(seconds, 0.1);

            if (running && !paused && simulatedMinutes < TOTAL_SIMULATED_MINUTES) {
                double speedFactor = speedSlider.getValue();
                double simulatedSeconds = seconds * speedFactor;
                
                // step 7
                double simulatedSecondsOfClock = simulatedMinutes * 60.0;

                moveBuses(simulatedSeconds, simulatedSecondsOfClock);

                simulatedMinutes += seconds * speedFactor * SIMULATED_MINUTES_PER_SECOND;

                // step 7
                if (relocationOn) {
                    rerouteEmptyBuses();
                }

                // step 6
                if (simulatedMinutes >= TOTAL_SIMULATED_MINUTES) {
                    simulatedMinutes = TOTAL_SIMULATED_MINUTES;
                    finishCurrentPhase();
                }
            }
                refreshLabels();
                draw();
            }

        }.start();


        // step 6
        VBox controls = buildControlPanel();
        ScrollPane sidePanel = new ScrollPane(controls);
        sidePanel.setFitToWidth(true);
        sidePanel.setPrefWidth(300);
        sidePanel.setMinWidth(300);

        BorderPane root = new BorderPane();
        root.setCenter(mapPane);
        root.setRight(sidePanel);


        // note 4: show the seed of the very first map (createRandomStops ran before the panel existed)
        updateSeedDisplay();

        // step 1
        stage.setTitle("Bus Rerouting");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();

        refreshLabels();

    }

    // step 6
    private VBox buildControlPanel() {

        Button phase1Button = new Button("Run Phase 1 (Fixed Cycle)");
        Button phase2Button = new Button("Run Phase 2 (Rerouting)");
        ToggleButton pauseButton = new ToggleButton("Pause / Resume");
        Button newMapButton = new Button("New random map");
        
        phase1Button.setMaxWidth(Double.MAX_VALUE);
        phase2Button.setMaxWidth(Double.MAX_VALUE);
        pauseButton.setMaxWidth(Double.MAX_VALUE);
        newMapButton.setMaxWidth(Double.MAX_VALUE);
        
        // e stands for event
        phase1Button.setOnAction(e -> startPhase(1));
        phase2Button.setOnAction(e -> startPhase(2));
        pauseButton.setOnAction(e -> paused = pauseButton.isSelected());
        newMapButton.setOnAction(e -> newRandomMap());

        // note 4: seed display + entry
        seedLabel = new Label("Current seed: " + currentSeed);
        seedField = new TextField(String.valueOf(currentSeed));
        Button useSeedButton = new Button("Use seed");
        useSeedButton.setOnAction(e -> useEnteredSeed());
        HBox seedRow = new HBox(6, seedField, useSeedButton);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        stopsSpinner = new Spinner<>(3, MAX_NUMBER_OF_STOPS, NUMBER_OF_STOPS);
        stopsSpinner.setEditable(true);
        stopsSpinner.setMaxWidth(Double.MAX_VALUE);

        waitSpinner = new Spinner<>(1, 120, 5);
        waitSpinner.setEditable(true);
        waitSpinner.setMaxWidth(Double.MAX_VALUE);

        speedSlider = new Slider(0.5, 5, 1.5);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1.5);

        timeLabel = new Label();
        averageWaitLabel = new Label();
        phase1Label = new Label();
        phase2Label = new Label();
        phase1Label.setStyle("-fx-font-weight: bold;");
        phase2Label.setStyle("-fx-font-weight: bold;");
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        // step 8
        addStopButton = new ToggleButton("Add bus stop (click grid)");
        addStopButton.setMaxWidth(Double.MAX_VALUE);
        addStopButton.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) { cancelRouteBuilding(); }
            clickMode = isNow ? CLICK_MODE_ADD_STOP : CLICK_MODE_NONE;
            mapPane.setCursor(isNow ? Cursor.CROSSHAIR : Cursor.DEFAULT);
            editHintLabel.setText(isNow ? "Click an empty intersection to place a stop."
                                         : DEFAULT_EDIT_HINT);
        });

        newBusTypeBox = new ComboBox<>(FXCollections.observableArrayList("Standard Bus", "Mini Bus", "Double Decker"));
        newBusTypeBox.getSelectionModel().selectFirst();
        newBusTypeBox.setMaxWidth(Double.MAX_VALUE);

        addBusButton = new Button("Add bus (pick its route)");
        addBusButton.setMaxWidth(Double.MAX_VALUE);
        finishBusButton = new Button("Finish bus");
        cancelBusButton = new Button("Cancel");
        finishBusButton.setDisable(true);
        cancelBusButton.setDisable(true);

        addBusButton.setOnAction(e -> {
        addStopButton.setSelected(false);
        clickMode = CLICK_MODE_BUILD_ROUTE;
        pendingRouteStops.clear();
        finishBusButton.setDisable(false);
        cancelBusButton.setDisable(false);
        editHintLabel.setText("Click the stops this bus should visit, in order (at least 2).");
        
    });
        finishBusButton.setOnAction(e -> finishNewBus());
        cancelBusButton.setOnAction(e -> cancelRouteBuilding());

        HBox finishRow = new HBox(6, finishBusButton, cancelBusButton);
        finishBusButton.setMaxWidth(Double.MAX_VALUE);
        cancelBusButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(finishBusButton, Priority.ALWAYS);
        HBox.setHgrow(cancelBusButton, Priority.ALWAYS);

        editHintLabel = new Label(DEFAULT_EDIT_HINT);
        editHintLabel.setWrapText(true);

        // step 9
        busStatusList = new ListView<>();
        busStatusList.setPrefHeight(140);
        busStatusList.setCellFactory(list -> new ListCell<Bus>() { 
        
            @Override
            protected void updateItem(Bus bus, boolean empty) {
                super.updateItem(bus, empty);
                if (empty || bus == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
        
            int busIndex = buses.indexOf(bus);
            boolean reRouting = isBusRerouting(busIndex);

            Color busColor = bus.getColor();
            Circle colorDot = new Circle(6, busColor);
            setGraphic(colorDot);

            String destinationText = reRouting
                        ? ("REROUTING -> " + describeDestination(bus))
                        : ("on route -> " + describeDestination(bus));
            setText(String.format("%s%d  %d/%d  %s", bus.getBusPrefix(), busIndex + 1,
                        bus.getCurrentPassengers(), bus.getPassengerCapacity(), destinationText));
                setTextFill(reRouting ? RED : GREEN);
            }
        });

        VBox box = new VBox(8,
            phase1Button, phase2Button, pauseButton, newMapButton,
            seedLabel, seedRow,
            new Label("Number of stops:"), stopsSpinner,
            new Label("Allotted wait time (min):"), waitSpinner,
            new Label("Speed:"), speedSlider,
            timeLabel, averageWaitLabel, phase1Label, phase2Label,
            new Separator(),
            new Label("Manual editing:"), addStopButton, newBusTypeBox, addBusButton, finishRow, editHintLabel,
            new Separator(),
            new Label("Bus status:"), busStatusList,
            logArea);
        
            VBox.setVgrow(logArea, Priority.ALWAYS);
            box.setPadding(new Insets(12));
            return box;
    }

    // step 6 - phase control
    private void startPhase(int phaseNumber) {
         if (paused) {
            return;
        }

        stopsSpinner.increment(0);
        phase = phaseNumber;
        relocationOn = (phaseNumber == 2);
        running = true;
        paused = false;
        simulatedMinutes = 0;

        waitSpinner.increment(0);
        allottedWaitMinutes = waitSpinner.getValue();
        reroutedTarget.clear();
        for (int i = 0; i < buses.size(); i++) {
            reroutedTarget.add(-1);
        }


        for (Bus bus: buses) {
            bus.updateLocation(bus.getRoute().get(0));
            bus.setCurrentPassengers(0);
        }

      
        for (BusStop stop: stops) {
            stop.updatePassengers(initialStopPassengers.get(stops.indexOf(stop)));
        }
       

        if (phaseNumber == 1) {
            phase1AverageWait = Double.NaN;
        }
        else {
            phase2AverageWait = Double.NaN;
        }
        log("=== phase " + phaseNumber + ": " + (phaseNumber == 1 ? "fixed cycle" : "rerouting") + " ===");
        refreshLabels();
    }

    // step 6 
    private void finishCurrentPhase() {
        running = false;

        // step 7
        double average = computeOverallAverageWaitMinutes();

        if (phase == 1) {
            phase1AverageWait = average;
        }
        else if (phase == 2) {
            phase2AverageWait = average;
        }

        log("=== Finished at t=" + (int) TOTAL_SIMULATED_MINUTES + " ===");

    }

    // step 6 (note 4: draws a brand new seed before generating, so this button still gives variety -
    // the OLD random object is only used here to pick the new seed, nothing else)
    private void newRandomMap() {
        stopsSpinner.increment(0);
        int n = (stopsSpinner == null) ? NUMBER_OF_STOPS : stopsSpinner.getValue();

        currentSeed = new Random().nextLong();
        createRandomStops(n);
        createBuses();

        phase = 0;
        running = false;
        paused = false;
        simulatedMinutes = 0;
        phase1AverageWait = Double.NaN;
        phase2AverageWait = Double.NaN;
        reroutedTarget.clear(); // step 7
        cancelRouteBuilding();;
        if (addStopButton != null) { addStopButton.setSelected(false); }

        log("Generated new map with " + stops.size() + " stops and " + buses.size() + " buses. Seed: " + currentSeed);
        refreshLabels();
        updateSeedDisplay();
        draw();

    }

    // note 4: shows the seed of the map currently on screen, and pre-fills the text field with
    // it so the user can copy it out, or just press "Use seed" again later to reproduce this map.
    private void updateSeedDisplay() {
        if (seedLabel != null) {
            seedLabel.setText("Current seed: " + currentSeed);
        }
        if (seedField != null) {
            seedField.setText(String.valueOf(currentSeed));
        }
    }

    // note 4/8: reads whatever is typed in seedField, regenerates the stops/buses from it,
    // so the SAME typed seed always reproduces the SAME map (stops, passengers, bus colors).
    private void useEnteredSeed() {
        String typed = seedField.getText().trim();
        long seed;
        try {
            seed = Long.parseLong(typed);
        } catch (NumberFormatException badNumber) {
            log("That seed isn't a valid number - try again.");
            return;
        }

        stopsSpinner.increment(0);
        int n = stopsSpinner.getValue();

        currentSeed = seed;
        createRandomStops(n);
        createBuses();

        phase = 0;
        running = false;
        paused = false;
        simulatedMinutes = 0;
        phase1AverageWait = Double.NaN;
        phase2AverageWait = Double.NaN;
        reroutedTarget.clear();
        cancelRouteBuilding();
        if (addStopButton != null) { addStopButton.setSelected(false); }

        log("Rebuilt map from seed " + currentSeed + ": " + stops.size() + " stops and " + buses.size() + " buses.");
        refreshLabels();
        updateSeedDisplay();
        draw();
    }

    // step 6
    private void refreshLabels() {
        if (timeLabel == null) { return; }

        timeLabel.setText(String.format("Time: %d / %d min", (int) simulatedMinutes, (int) TOTAL_SIMULATED_MINUTES));
        averageWaitLabel.setText("Avg wait: " + formatAverage(Double.NaN));
        phase1Label.setText("Phase 1 avg wait: " + formatAverage(phase1AverageWait));
        phase2Label.setText("Phase 2 avg wait: " + formatAverage(phase2AverageWait));

        // step 9
        if (busStatusList != null) {
            busStatusList.setItems(FXCollections.observableArrayList(buses));
        }
    }
    

    private String formatAverage(double minutes) {
        return Double.isNaN(minutes) ? "-" : String.format("%.2f min", minutes);
    }

    private void log(String message) {
        if (logArea != null) {
            logArea.appendText(message + "\n");
        }
    }
    
    // step 2 (note 4/8: now re-seeds `random` from currentSeed first, so the SAME seed always
    // produces the same stop placement, the same initial passenger counts, the same arrival
    // rates, AND (since createBuses() runs right after and keeps drawing from this same
    // `random`) the same bus colors - the whole map is reproducible from one number.)
    private void createRandomStops(int n) {
        random = new Random(currentSeed);
        stops.clear();
        initialStopPassengers.clear();

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
            int passengers = 3 + random.nextInt(28);
            stop.updatePassengers(passengers); // Random passengers allocation
            stops.add(stop);
            initialStopPassengers.add(passengers);
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

        // step 7
        reroutedTarget.clear();
        for (int i = 0; i < buses.size(); i++) {
            reroutedTarget.add(-1);
        }
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

        // step 4 + step 7 (modified the parameter)
    private void moveBuses(double seconds, double nowInSimulatedSeconds) {
        addNewPassengers(seconds, nowInSimulatedSeconds); // step 7

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
                arriveAtStop(i, bus, destination, nowInSimulatedSeconds); // step 7

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
    private void addNewPassengers(double seconds, double nowInSimulatedSeconds) {
        for (BusStop stop : stops) {
            stop.advancePassengers(seconds, nowInSimulatedSeconds);
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
        
    private void arriveAtStop(int busIndex, Bus bus, Location stopLocation, double nowInSimulatedSeconds) {
        int stopIndex = findStopIndexAt(stopLocation);
        if (stopIndex == -1) {
            return;
        }
        BusStop stop = stops.get(stopIndex);

        int gettingOff = bus.getCurrentPassengers();
        bus.setCurrentPassengers(0);
        int gettingOn = stop.boardPassengers(bus.getPassengerCapacity(), nowInSimulatedSeconds);
        bus.setCurrentPassengers(gettingOn);
        bus.setLastVisitedStop(stop.getLocation());
        bus.startLoading(LOADING_SECONDS);

        // step 7
        if (reroutedTarget.get(busIndex) == stopIndex) {
            reroutedTarget.set(busIndex, -1);
            log(bus.getBusPrefix() + (busIndex + 1) + " finished its reroute to stop " + 
                (stopIndex + 1) + ".");
        }

        log(bus.getBusPrefix() + (busIndex + 1) + " at Stop " + (stopIndex + 1) + ": "
                    + gettingOff + " got off, " + gettingOn + " got on, " + stop.getPassenger() + " still waiting");

        }

        // step 7 - phase 2
        private void rerouteEmptyBuses() {
            int worstStopIndex = -1;
            double worstWaitSeconds = -1;
            boolean skipIter = false;

            for (BusStop stop: stops) {
                skipIter = false;
                for (Bus bus: buses) {
                    Location stopLoc = stop.getLocation();
                    Location destLoc = bus.getDestination();
                    if ( destLoc.getX() == stopLoc.getX()
                      && destLoc.getY() == stopLoc.getY() ) {
                        skipIter = true;
                        break;
                    }
                }

                if (skipIter) { continue; }
                double oldestWaitSeconds = stop.getOldestWaitSeconds(simulatedMinutes * 60.0);
                if (oldestWaitSeconds > allottedWaitMinutes * 60.0 && oldestWaitSeconds > worstWaitSeconds) {
                    worstWaitSeconds = oldestWaitSeconds;
                    worstStopIndex = stops.indexOf(stop);
                }
                
            }

            if (worstStopIndex == -1) {
                return;
            }

            for (int i = 0; i < buses.size(); i++) {
                Bus bus = buses.get(i);

                if (bus.getCurrentPassengers() == 0 && reroutedTarget.get(i) == -1 && bus instanceof Reroutable) {
                    Location target = stops.get(worstStopIndex).getLocation();
                    ((Reroutable) bus).reRoute(target);
                    reroutedTarget.set(i, worstStopIndex);
                    log(String.format("t=%.0fm: Rerouted %s%d -> Stop %d (overdue %.1f min)",
                        simulatedMinutes, bus.getBusPrefix(), i + 1, worstStopIndex + 1, worstWaitSeconds / 60.0));
                    return; 
                }
            }
        }

        private double computeOverallAverageWaitMinutes() {
            double totalWaitSeconds = 0;
            int totalBoarded = 0;

            for (BusStop stop : stops) {
                totalWaitSeconds += stop.getTotalWaitSeconds();
                totalBoarded += stop.getTotalBoarded();
            }

            if (totalBoarded == 0) {
                return Double.NaN;
            }
            return (totalWaitSeconds / totalBoarded) / 60.0;
        }


        // step 8
        private void handleMapClick(MouseEvent event) {
            int[] gridPoint = pickGridPoint(event.getX(), event.getY());

            if (clickMode == CLICK_MODE_ADD_STOP && event.getButton() == MouseButton.PRIMARY) {
                if (gridPoint != null && findStopAt(gridPoint[0], gridPoint[1]) == null) {
                    addStopManually(gridPoint[0], gridPoint[1]);
                }
                draw();
                return;
            }

            if (clickMode == CLICK_MODE_BUILD_ROUTE && event.getButton() == MouseButton.PRIMARY) {
                if (gridPoint != null) {
                    BusStop clickedStop = findStopAt(gridPoint[0], gridPoint[1]);
                    if(clickedStop != null) {
                        if (!pendingRouteStops.contains(clickedStop)) {
                            pendingRouteStops.add(clickedStop);
                        }
                        else if (pendingRouteStops.get(pendingRouteStops.size() - 1) == clickedStop) {
                            pendingRouteStops.remove(clickedStop);
                        }
                    } editHintLabel.setText("Route so far: " + pendingRouteStops.size() + " stop(s). Click more, or press Finish bus.");

                }
                draw();
                return;
            }

            if (clickMode == CLICK_MODE_NONE) {
                if (event.getButton() == MouseButton.SECONDARY && gridPoint != null) {
                    BusStop clickedStop = findStopAt(gridPoint[0], gridPoint[1]);
                    if (clickedStop != null) {
                        clickedStop.updatePassengers(clickedStop.getPassenger() + 10);
                        log("+10 passengers added manually to a stop.");
                    }
                    draw();
                    return;
                }
                if (event.getButton() == MouseButton.PRIMARY) {
                    newRandomMap();
                }
            }
        }

        private int[] pickGridPoint(double pixelX, double pixelY) {
            int gridX = (int) Math.round( (pixelX - MARGIN) / cellWidth);
            int gridY = (int) Math.round( (pixelY - MARGIN) / cellHeight);

            if (gridX < 0 || gridY < 0 || gridX > GRID_SIZE || gridY > GRID_SIZE) {
                return null;
            }
            double snapTolerance = Math.max(14, Math.min(cellWidth, cellHeight) * 0.4);
            if (Math.abs(pixelX - pointX(gridX)) > snapTolerance || Math.abs(pixelY - pointY(gridY)) > snapTolerance) {
                return null;
            }
            return new int[]{gridX, gridY};
        }

        private BusStop findStopAt(int gridX, int gridY) {
            for (BusStop stop : stops) {
                Location location = stop.getLocation();
                if ( (int) Math.round(location.getX()) == gridX && (int) Math.round(location.getY()) == gridY) {
                    return stop;
                }
            }

            return null;
        }

        private void addStopManually(int gridX, int gridY) {
            BusStop newStop = new BusStop(gridX, gridY);
            newStop.setArrivalRate(0.1 + 0.4 * random.nextDouble());
            newStop.updatePassengers(1 + random.nextInt(6));   // a small starting crowd, smaller than a fresh map's
            stops.add(newStop);
            log("Added a new stop at (" + gridX + ", " + gridY + "). It has no bus route yet"
                    + " - add a bus through it, or it will only ever be reached by Phase 2 relocation.");
        }

        private void finishNewBus() {
            if (pendingRouteStops.size() < 2) {
                editHintLabel.setText("Pick at least 2 stops before finishing.");
                return;
            }

            ArrayList<Location> route = new ArrayList<>();
            for (BusStop stop : pendingRouteStops) {
                route.add(stop.getLocation());
            }

            String chosenType = newBusTypeBox.getValue();
            float hue = random.nextFloat() * 360;
            Color busColor = Color.hsb(hue, 0.80, 0.80);

            Bus newBus;
            if (chosenType.equals("Standard Bus")) {
                newBus = new StandardBus(busColor, route);
            } 
            else if (chosenType.equals("Mini Bus")) {
                newBus = new MiniBus(busColor, route);
            } 
            else {
                newBus = new DoubleDecker(busColor, route);
            }

            newBus.updateLocation(route.get(0));
            newBus.updateDestination(route.get(1 % route.size()));
            buses.add(newBus);
            nextStopIndexes.add(1 % route.size());
            reroutedTarget.add(-1);
            builtIconSize = -1;   // step 3's icon cache needs rebuilding for the new bus

            log("Added " + newBus.getBusPrefix() + buses.size() + " (" + chosenType + ") with a "
                    + pendingRouteStops.size() + "-stop route.");

            cancelRouteBuilding();
    }

    private boolean isBusRerouting(int busIndex) {
            Bus bus = buses.get(busIndex);
            Location destination = bus.getDestination();
            Location expectedNextStop = bus.getRoute().get(nextStopIndexes.get(busIndex));
            return destination.getX() != expectedNextStop.getX() || destination.getY() != expectedNextStop.getY();
    }

    private String describeDestination(Bus bus) {
            Location destination = bus.getDestination();
            int stopIndex = findStopIndexAt(destination);
            return (stopIndex == -1) ? "?" : ("Stop " + (stopIndex + 1));
    }

    private void cancelRouteBuilding() {
        pendingRouteStops.clear();
        clickMode = CLICK_MODE_NONE;
        if (finishBusButton != null) { finishBusButton.setDisable(true); }
        if (cancelBusButton != null) { cancelBusButton.setDisable(true); }
        if (addStopButton != null && !addStopButton.isSelected()) {
            mapPane.setCursor(Cursor.DEFAULT);
        }
        if (editHintLabel != null) { editHintLabel.setText(DEFAULT_EDIT_HINT); }
        draw();
    }


}