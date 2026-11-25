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
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Map</p>
 * <p><b>Purpose:</b> Create the map, bus stops, buses, move buses on the map to stops,
 * support manual map editing and seed reproduction, and connect backend simulation logic to the JavaFX GUI.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.2 11/25/26
 */
public class Map extends Application {
    
    // Assets folder directory containing bus icon images
    private static final String ICON_DIR = "\"C:\\Users\\abdul\\Downloads\\EE364project\\assets\""; 

    // Step 1: Grid canvas and layout constants
    private final Canvas canvas = new Canvas();
    private static final int GRID_SIZE = 20; 
    private static final double MARGIN = 46;
    
    // Step 8: Interactive pane and default status hint
    private Pane mapPane;
    private static final String DEFAULT_EDIT_HINT = "Right-click a stop = +10 waiting passengers.";

    // Step 2: Computed pixel dimensions of each grid cell
    private double cellWidth;
    private double cellHeight;

    // Step 2: Default and maximum stop counts
    private static final int NUMBER_OF_STOPS = 7;
    private static final int MAX_NUMBER_OF_STOPS = 60;

    // Step 2 & Step 6: Stops list, seeded RNG, and snapshot of initial passenger counts per stop
    private final List<BusStop> stops = new ArrayList<>();
    // note 4/8: `random` is now explicitly seeded on every map generation, and the seed used is
    // remembered in currentSeed so it can be shown to the user and re-entered later to reproduce
    // the exact same stops, passenger counts, and bus colors.
    private Random random = new Random();
    private long currentSeed = random.nextLong();
    private final List<Integer> initialStopPassengers = new ArrayList<>();

    // Step 3: Fleet configuration and cached rendered bus icons
    private static final int NUMBER_OF_BUSES = 3;
    private final List<Bus> buses = new ArrayList<>();
    private final List<Image> busIcons = new ArrayList<>();
    private double iconSize;
    private int builtIconSize = -1;

    // Step 4: Movement scaling and route index tracking
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

    // note 5: high-priority stops draw black/blue, low-priority stops keep the original navy/orange look
    private static final Color HIGH_PRIORITY_BOX = Color.web("#000000");
    private static final Color HIGH_PRIORITY_ACCENT = Color.web("#1565c0");

    // Step 6: Simulation clock, phase state, and wait metrics
    private static final double TOTAL_SIMULATED_MINUTES = 300.0;
    private static final double SIMULATED_MINUTES_PER_SECOND = 6.0;
    private int phase = 0; // 0: no run, 1: phase 1 (fixed cycle), 2: phase 2 (rerouting)
    private boolean running = false;
    private boolean paused = false;
    private boolean relocationOn = false;
    private double simulatedMinutes = 0;
    private double phase1AverageWait = Double.NaN;
    private double phase2AverageWait = Double.NaN;

    // note 1: running operating cost of the current run, and each phase's final total, so cost
    // can be weighed against average wait (customer satisfaction) once a phase finishes.
    private double totalOperatingCost = 0;
    private double phase1TotalCost = Double.NaN;
    private double phase2TotalCost = Double.NaN;
    private Label costLabel;

    // Step 7: Rerouting threshold, active reroute targets, and search radius
    private double allottedWaitMinutes = 5;
    private final List<Integer> reroutedTarget = new ArrayList<>();
// Overhead cost charged each time a reroutable bus is dynamically rerouted in Phase 2
    private static final double REROUTE_OVERHEAD_COST = 5.0;

    // note 2: a bus is only considered for rerouting if the overdue stop is within this many
    // grid cells of its current location - stops this measure of distance from abandoning a
    // route to chase a stop clear across the map.
    private static final double MAX_REROUTE_DISTANCE = 12.0;
    

    // Step 8: Click interaction modes and pending route stops
    private static final int CLICK_MODE_NONE = 0;
    private static final int CLICK_MODE_ADD_STOP = 1;
    private static final int CLICK_MODE_BUILD_ROUTE = 2;
    private static final int CLICK_MODE_DELETE_STOP = 3;   // additional note: click a stop to delete it
    private static final int CLICK_MODE_DELETE_BUS = 4;    // additional note: click a bus's icon to delete it
    private int clickMode = CLICK_MODE_NONE;
    private final List<BusStop> pendingRouteStops = new ArrayList<>();

    private ToggleButton addStopButton;
    private Button addBusButton;
    private Button finishBusButton;
    private Button cancelBusButton;
    private ComboBox<String> newBusTypeBox;
    private Label editHintLabel;
    private ListView<Bus> busStatusList; 
    

    // note 6: the two swappable side-panel sections
    private VBox simulationModePanel;
    private VBox buildModePanel;

    // additional notes: delete / edit / clear controls
    private Button deleteStopButton;
    private Button deleteBusButton;
    private Button editRouteButton;
    private Button clearMapButton;
    private Bus busBeingEdited;   // non-null while the user is re-picking this bus's route
    private boolean nextBusClickStartsEdit = false;   // true right after "Edit a bus's route" is pressed
    
    // GUI labels, log output, and input controls
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
    
    
    /**
     * Initializes the JavaFX scene, canvas bindings, click handlers, initial stops/buses,
     * animation loop, and side control panel.
     *
     * @param stage the primary stage for this application
     */
    @Override 
    public void start(Stage stage) {
        // Step 1: Bind canvas size dynamically to mapPane and redraw on resize
        mapPane = new Pane(canvas);
    
        DoubleProperty canvasWidth = canvas.widthProperty();
        ReadOnlyDoubleProperty paneWidth = mapPane.widthProperty();
        canvasWidth.bind(paneWidth);

        DoubleProperty canvasHeight = canvas.heightProperty();
        ReadOnlyDoubleProperty paneHeight = mapPane.heightProperty();
        canvasHeight.bind(paneHeight);

        paneWidth.addListener((observable, oldWidth, newWidth) -> draw());
        paneHeight.addListener((observable, oldHeight, newHeight) -> draw()); 

        // Step 8: Route map clicks to interactive editing or default actions
        mapPane.setOnMouseClicked(event -> handleMapClick(event));   

        // Step 2 & Step 3: Populate initial stops and buses
        createRandomStops(NUMBER_OF_STOPS);
        createBuses();

        // Step 4 & Step 7: Real-time animation loop advancing simulation time, movement, and rerouting
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
                // Clamp frame delta so window drags or pauses do not cause huge simulation jumps
                seconds = Math.min(seconds, 0.1);

                if (running && !paused && simulatedMinutes < TOTAL_SIMULATED_MINUTES) {
                    double speedFactor = speedSlider.getValue();
                    double simulatedSeconds = seconds * speedFactor;
                    
                    // Step 7: Pass elapsed simulated clock seconds for passenger wait tracking
                    double simulatedSecondsOfClock = simulatedMinutes * 60.0;

                    moveBuses(simulatedSeconds, simulatedSecondsOfClock);

                    simulatedMinutes += seconds * speedFactor * SIMULATED_MINUTES_PER_SECOND;

                    // Step 7: Trigger dynamic rerouting only in Phase 2
                    if (relocationOn) {
                        rerouteEmptyBuses();
                    }

                    // Step 6: Finish phase once total simulated duration is reached
                    if (simulatedMinutes >= TOTAL_SIMULATED_MINUTES) {
                        simulatedMinutes = TOTAL_SIMULATED_MINUTES;
                        finishCurrentPhase();
                    }
                }
                refreshLabels();
                draw();
            }

        }.start();


        // Step 6: Build collapsible/swappable side panel
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

        // Step 1: Configure and show primary window
        stage.setTitle("Bus Rerouting");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();

        refreshLabels();

    }

    /**
     * Builds the side control panel containing Simulation Mode and Build Mode controls,
     * metrics labels, bus status list, and the event log.
     *
     * @return the root {@link VBox} for the control panel
     */
    private VBox buildControlPanel() {

        Button phase1Button = new Button("Run Phase 1 (Fixed Cycle)");
        Button phase2Button = new Button("Run Phase 2 (Rerouting)");
        ToggleButton pauseButton = new ToggleButton("Pause / Resume");
        Button newMapButton = new Button("New random map");
        
        phase1Button.setMaxWidth(Double.MAX_VALUE);
        phase2Button.setMaxWidth(Double.MAX_VALUE);
        pauseButton.setMaxWidth(Double.MAX_VALUE);
        newMapButton.setMaxWidth(Double.MAX_VALUE);
        
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

        costLabel = new Label("Cost so far: 0.0  |  Phase 1: -  |  Phase 2: -");   // note 1
        costLabel.setWrapText(true);
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        // Step 8: Add-stop and route-building controls
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

        // Step 9: Live bus status list showing capacity, passengers, and active destination/reroute state
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

        // additional notes: delete stop / delete bus / edit route / clear map
        deleteStopButton = new Button("Delete bus stop (click one)");
        deleteStopButton.setMaxWidth(Double.MAX_VALUE);
        deleteStopButton.setOnAction(e -> {
            cancelRouteBuilding();
            clickMode = CLICK_MODE_DELETE_STOP;
            editHintLabel.setText("Click a stop to delete it. Any bus routed through it skips it afterward.");
        });

        deleteBusButton = new Button("Delete bus (click one)");
        deleteBusButton.setMaxWidth(Double.MAX_VALUE);
        deleteBusButton.setOnAction(e -> {
            cancelRouteBuilding();
            clickMode = CLICK_MODE_DELETE_BUS;
            editHintLabel.setText("Click a bus icon to delete it.");
        });

        editRouteButton = new Button("Edit a bus's route (click the bus, then its new stops)");
        editRouteButton.setMaxWidth(Double.MAX_VALUE);
        editRouteButton.setWrapText(true);
        editRouteButton.setOnAction(e -> {
            cancelRouteBuilding();
            clickMode = CLICK_MODE_DELETE_BUS;   // reuses "click a bus" targeting, see handleMapClick
            nextBusClickStartsEdit = true;
            editHintLabel.setText("Click the bus whose route you want to change.");
        });

        HBox deleteRow = new HBox(6, deleteStopButton, deleteBusButton);
        HBox.setHgrow(deleteStopButton, Priority.ALWAYS);
        HBox.setHgrow(deleteBusButton, Priority.ALWAYS);

        clearMapButton = new Button("Clear map (remove every stop and bus)");
        clearMapButton.setMaxWidth(Double.MAX_VALUE);
        clearMapButton.setOnAction(e -> clearMap());

        // note 6: everything above still builds the same controls as before - the only change is
        // how they're grouped. "Simulation" holds the controls for running/watching a phase;
        // "Build" holds the controls for editing the map. A toggle at the top swaps which one
        // is visible, so the user only sees the controls relevant to what they're doing.
        simulationModePanel = new VBox(8,
            phase1Button, phase2Button, pauseButton, newMapButton,
            seedLabel, seedRow,
            new Label("Number of stops:"), stopsSpinner,
            new Label("Allotted wait time (min):"), waitSpinner,
            new Label("Speed:"), speedSlider,
            timeLabel, averageWaitLabel, phase1Label, phase2Label, costLabel,
            new Separator(),
            new Label("Bus status:"), busStatusList,
            logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        buildModePanel = new VBox(8,
            new Label("Manual editing:"), addStopButton, newBusTypeBox, addBusButton, finishRow,
            deleteRow, editRouteButton, editHintLabel,
            new Separator(),
            clearMapButton);

        ToggleButton modeToggle = new ToggleButton("Switch to Build Mode");
        modeToggle.setMaxWidth(Double.MAX_VALUE);
        modeToggle.setOnAction(e -> setBuildMode(modeToggle.isSelected()));
        modeToggle.selectedProperty().addListener((obs, was, isNow) ->
                modeToggle.setText(isNow ? "Switch to Simulation Mode" : "Switch to Build Mode"));

        VBox box = new VBox(8, modeToggle, simulationModePanel, buildModePanel);
        box.setPadding(new Insets(12));
        setBuildMode(false);   // note 6: start in Simulation Mode
        return box;
    }

    /**
     * Switches the side panel between Simulation Mode and Build Mode, canceling any active
     * route editing when returning to Simulation Mode.
     *
     * @param buildMode {@code true} to show Build Mode controls, {@code false} for Simulation Mode
     */
    // note 6: shows exactly one of the two panels, and leaves build-editing mode (clickMode) if
    // the user switches back to Simulation Mode mid-edit.
    private void setBuildMode(boolean buildMode) {
        if (simulationModePanel == null || buildModePanel == null) { return; }
        simulationModePanel.setVisible(!buildMode);
        simulationModePanel.setManaged(!buildMode);
        buildModePanel.setVisible(buildMode);
        buildModePanel.setManaged(buildMode);
        if (!buildMode) {
            cancelRouteBuilding();
            if (addStopButton != null) { addStopButton.setSelected(false); }
        }
    }

    /**
     * Starts or resets a simulation phase (1 for fixed cycle, 2 for dynamic rerouting),
     * restoring initial stop passenger counts and resetting bus positions and costs.
     *
     * @param phaseNumber the phase number to start (1 or 2)
     */
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
        totalOperatingCost = 0;   // note 1

        waitSpinner.increment(0);
        allottedWaitMinutes = waitSpinner.getValue();
        reroutedTarget.clear();
        for (int i = 0; i < buses.size(); i++) {
            reroutedTarget.add(-1);
        }

        for (Bus bus : buses) {
            bus.updateLocation(bus.getRoute().get(0));
            bus.setCurrentPassengers(0);
        }

        // Restore each stop to its initial passenger count so Phase 2 starts from the same baseline
        for (BusStop stop : stops) {
            stop.updatePassengers(initialStopPassengers.get(stops.indexOf(stop)));
        }

        if (phaseNumber == 1) {
            phase1AverageWait = Double.NaN;
            phase1TotalCost   = Double.NaN;
        } else {
            phase2AverageWait = Double.NaN;
            phase2TotalCost   = Double.NaN;
        }
        log("=== phase " + phaseNumber + ": " + (phaseNumber == 1 ? "fixed cycle" : "rerouting") + " ===");
        refreshLabels();
    }

    /**
     * Completes the active simulation phase, records average wait time and total operating cost,
     * and logs the phase summary.
     */
    // step 6
    private void finishCurrentPhase() {
        running = false;

        // step 7
        double average = computeOverallAverageWaitMinutes();

        if (phase == 1) {
            phase1AverageWait = average;
            phase1TotalCost = totalOperatingCost;   // note 1
        } else if (phase == 2) {
            phase2AverageWait = average;
            phase2TotalCost = totalOperatingCost;   // note 1
        }

        log(String.format("=== Finished at t=%d === total operating cost: %.1f",
                (int) TOTAL_SIMULATED_MINUTES, totalOperatingCost));
    }

    /**
     * Generates a brand-new random seed, rebuilds stops and buses, resets simulation state,
     * and redraws the map.
     */
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
        phase1TotalCost = Double.NaN;
        phase2TotalCost = Double.NaN;
        totalOperatingCost = 0.0;
        reroutedTarget.clear(); // step 7
        cancelRouteBuilding();
        if (addStopButton != null) { addStopButton.setSelected(false); }

        log("Generated new map with " + stops.size() + " stops and " + buses.size() + " buses. Seed: " + currentSeed);
        refreshLabels();
        updateSeedDisplay();
        draw();
    }

    /**
     * Synchronizes the seed label and seed text field with {@link #currentSeed}.
     */
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

    /**
     * Parses the seed entered in {@link #seedField} and rebuilds the map deterministically
     * from that seed.
     */
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

    /**
     * Refreshes simulation time, average wait, phase comparison, operating cost, and bus list labels.
     */
    // step 6
    private void refreshLabels() {
        if (timeLabel == null) { return; }

        timeLabel.setText(String.format("Time: %d / %d min", (int) simulatedMinutes, (int) TOTAL_SIMULATED_MINUTES));
        // bug fix (pre-existing, unrelated to this batch): this was hardcoded to Double.NaN and
        // always displayed "-" even while a phase was actively running. computeOverallAverageWaitMinutes()
        // already existed and already worked correctly for the phase1/phase2 FINAL averages below -
        // it just wasn't being called here for the LIVE value too.
        averageWaitLabel.setText("Avg wait: " + formatAverage(computeOverallAverageWaitMinutes()));
        phase1Label.setText("Phase 1 avg wait: " + formatAverage(phase1AverageWait));
        phase2Label.setText("Phase 2 avg wait: " + formatAverage(phase2AverageWait));

        // note 1: cost so far this run, plus each phase's final total once it has finished
        if (costLabel != null) {
            costLabel.setText(String.format("Cost so far: ⃁ %.1f   |  Phase 1: ⃁ %s  |  Phase 2: ⃁ %s ",
                    totalOperatingCost, formatCost(phase1TotalCost), formatCost(phase2TotalCost)));
        }

        // step 9
        if (busStatusList != null) {
            busStatusList.setItems(FXCollections.observableArrayList(buses));
        }
    }

    /**
     * Formats an operating cost value for UI display.
     *
     * @param cost the operating cost value
     * @return {@code "-"} if {@link Double#NaN}, otherwise formatted to one decimal place
     */
    private String formatCost(double cost) {
        return Double.isNaN(cost) ? "-" : String.format("%.1f", cost);
    }

    /**
     * Formats an average wait duration in minutes for UI display.
     *
     * @param minutes the average wait time in minutes
     * @return {@code "-"} if {@link Double#NaN}, otherwise formatted as {@code "%.2f min"}
     */
    private String formatAverage(double minutes) {
        return Double.isNaN(minutes) ? "-" : String.format("%.2f min", minutes);
    }

    /**
     * Appends a line of text to the simulation event log area.
     *
     * @param message the message to log
     */
    private void log(String message) {
        if (logArea != null) {
            logArea.appendText(message + "\n");
        }
    }
    
    /**
     * Creates {@code n} random bus stops across distinct grid intersections using {@link #currentSeed},
     * initializes passenger counts and priority flags, records initial passenger counts, and sets
     * passenger arrival rates.
     *
     * @param n number of stops to generate
     */
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

        Collections.shuffle(intersections, random); // Randomize intersection order for stop placement

        for (int i = 0; i < n; i++) {
            int[] point = intersections.get(i);
            BusStop stop = new BusStop(point[0], point[1]);
            int passengers = 3 + random.nextInt(10);
            stop.updatePassengers(passengers); // Random initial passenger allocation
            stop.setHighPriority(random.nextDouble() < 0.45);   // note 5: ~45% of stops are high priority
            stops.add(stop);
            initialStopPassengers.add(passengers);
        }

        // Step 5: Assign random passenger arrival rates to each stop
        createArrivalRates();
    }

    /**
     * Finds and removes a high-priority stop {@link Location} from route slot 0 or 1 to donate
     * to another route (such as the Double Decker's route).
     *
     * @param routes   list of bus routes indexed by bus slot
     * @param allStops all current bus stops used to check priority flags
     * @return the removed high-priority {@link Location}, or {@code null} if none is available in slots 0 or 1
     */
    // step 3
    // note 5 (bug fix helper): finds and REMOVES a high-priority stop from slot 0 or 1's route
    // (wherever in that list it happens to be, not just the end) so it can be handed to the
    // DoubleDecker's route without ever giving it a low-priority stop. Returns the Location that
    // was removed, or null if slots 0/1 hold no high-priority stop at all (meaning the map has
    // fewer than 2 high-priority stops in total).
    private Location takeHighPriorityDonor(List<ArrayList<Location>> routes, List<BusStop> allStops) {
        for (int slot = 0; slot < 2; slot++) {
            ArrayList<Location> candidate = routes.get(slot);
            for (int entryIndex = 0; entryIndex < candidate.size(); entryIndex++) {
                Location entryLocation = candidate.get(entryIndex);
                if (isHighPriorityAt(entryLocation, allStops)) {
                    candidate.remove(entryIndex);
                    return entryLocation;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the stop at the given {@link Location} is marked as high priority.
     *
     * @param location the grid location to check
     * @param allStops list of stops to search
     * @return {@code true} if a matching stop exists and is high priority, {@code false} otherwise
     */
    private boolean isHighPriorityAt(Location location, List<BusStop> allStops) {
        for (BusStop stop : allStops) {
            Location stopLocation = stop.getLocation();
            if (stopLocation.getX() == location.getX() && stopLocation.getY() == location.getY()) {
                return stop.isHighPriority();
            }
        }
        return false;
    }

    /**
     * Builds routes, ensures every bus gets at least two stops while respecting priority rules,
     * instantiates buses ({@link StandardBus}, {@link MiniBus}, {@link DoubleDecker}), and initializes
     * destinations, loading timers, and reroute state.
     */
    private void createBuses() {
        buses.clear();
        
        // Step 4: Reset next-stop index tracking
        nextStopIndexes.clear(); 

        List<ArrayList<Location>> routes = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            routes.add(new ArrayList<Location>());
        }

        // note 5: bus slot 2 below is always built as a DoubleDecker (see the i % 3 == 2 branch),
        // so it must only receive high-priority stops. Low-priority stops are dealt between the
        // other two slots only; high-priority stops are dealt across all three, same round-robin
        // as before, just skipping slot 2 for anything the DoubleDecker can't serve.
        int nextSlotForLowPriority = 0;
        int nextSlotForHighPriority = 0;
        for (BusStop currentStop : stops) {
            Location currentStopLocation = currentStop.getLocation();

            if (currentStop.isHighPriority()) {
                routes.get(nextSlotForHighPriority % NUMBER_OF_BUSES).add(currentStopLocation);
                nextSlotForHighPriority++;
            } else {
                int lowPrioritySlots = NUMBER_OF_BUSES - 1;   // every slot except the DoubleDecker's
                routes.get(nextSlotForLowPriority % lowPrioritySlots).add(currentStopLocation);
                nextSlotForLowPriority++;
            }
        }

        // note 5 (bug fix): a route of exactly ONE stop passes Bus's "not empty" check but then
        // sits still forever - its only destination IS where it already is, so it never moves
        // (this is what caused "double decker isn't moving", and the same risk exists for slots
        // 0/1 too if too few stops land there). Every slot needs AT LEAST TWO stops: slot 2
        // (DoubleDecker) can only borrow HIGH-priority stops, so it never ends up with one it
        // can't serve; slots 0/1 can borrow from each other, high or low priority either way.
        while (routes.get(2).size() < 2) {
            Location donorLocation = takeHighPriorityDonor(routes, stops);
            if (donorLocation != null) {
                routes.get(2).add(donorLocation);
            } else if (!routes.get(2).isEmpty()) {
                routes.get(2).add(routes.get(2).get(0));   // duplicate: a valid, if static, 2-stop route
                break;
            } else {
                break;   // no high-priority stops exist anywhere on the map - unusual but possible
            }
        }
        for (int slot = 0; slot < 2; slot++) {
            int otherSlot = 1 - slot;
            while (routes.get(slot).size() < 2) {
                if (routes.get(otherSlot).size() > 1) {
                    routes.get(slot).add(routes.get(otherSlot).remove(routes.get(otherSlot).size() - 1));
                } else if (!routes.get(slot).isEmpty()) {
                    routes.get(slot).add(routes.get(slot).get(0));   // duplicate: still a valid 2-stop route
                    break;
                } else {
                    break;   // unreachable given createRandomStops' n >= 3 guarantee across 3 slots
                }
            }
        }

        // Use golden-ratio hue spacing so bus colors remain visually distinct
        float firstHue = random.nextFloat();
        for (int i = 0; i < NUMBER_OF_BUSES; i++) {
            float hue = (float) ( (firstHue + i * 0.618033988749895) % 1.0);
            Color busColor = Color.hsb(hue * 360, 0.80 , 0.80);

            Bus bus;

            if (i % 3 == 0) {
                bus = new StandardBus(busColor, routes.get(i));
            } else if (i % 3 == 1) {
                bus = new MiniBus(busColor, routes.get(i));
            } else {
                bus = new DoubleDecker(busColor, routes.get(i));
            }

            ArrayList<Location> temporaryRoute = routes.get(i);
            Location temporaryLocation = temporaryRoute.get(0);

            bus.updateLocation(temporaryLocation);

            // Step 4: Set initial destination to the second stop in the route (or index 0 if size 1)
            int firstDestination = 1 % temporaryRoute.size();
            bus.updateDestination(temporaryRoute.get(firstDestination));
            nextStopIndexes.add(firstDestination);
            buses.add(bus);
        }

        builtIconSize = -1;

        // Step 5: Clear any active loading timers
        resetLoadingTimers();

        // Step 7: Initialize reroute tracking (-1 means not currently rerouted)
        reroutedTarget.clear();
        for (int i = 0; i < buses.size(); i++) {
            reroutedTarget.add(-1);
        }
    }
    
    /**
     * Renders the entire simulation canvas: background, grid, bus routes, stops, buses,
     * and active build-mode visual indicators.
     */
    private void draw() {
        // Step 1: Validate canvas bounds and compute cell dimensions
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if ( width <= 0 || height <= 0 ) { return; }

        cellWidth = ( width - 2 * MARGIN ) / GRID_SIZE;
        cellHeight = ( height - 2 * MARGIN ) / GRID_SIZE;

        double lineWidth = 7.5;

        // Step 3: Scale bus icon size cleanly with cell size
        iconSize = Math.max(26, Math.min(56, Math.min(cellWidth, cellHeight) * 0.5));

        // note 3 / step 3: rebuild cached colored bus icons only when icon size changes
        if ( (int) iconSize != builtIconSize) {
            buildBusIcons();
        }

        GraphicsContext drawingBrush = canvas.getGraphicsContext2D();
        drawingBrush.setFill(WHITE);
        drawingBrush.fillRect(0, 0, width, height);
        
        // Draw layers in back-to-front order so buses appear above stops and routes
        drawGrid(drawingBrush, lineWidth);
        drawRoutes(drawingBrush, lineWidth);
        drawStops(drawingBrush);
        drawBuses(drawingBrush);
        drawBuildModeIndicators(drawingBrush);
    }

    /**
     * Draws visual aids for active build modes, such as open intersection dots when adding stops
     * or highlight rings around stops selected for a new/edited bus route.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    // note 7: visual help while editing - open intersections in Add-Stop mode, and a highlight
    // ring around stops already picked while building a new bus's route.
    private void drawBuildModeIndicators(GraphicsContext drawingBrush) {
        if (clickMode == CLICK_MODE_ADD_STOP) {
            drawingBrush.save();
            drawingBrush.setFill(Color.color(0.1, 0.5, 0.9, 0.35));
            for (int gx = 0; gx <= GRID_SIZE; gx++) {
                for (int gy = 0; gy <= GRID_SIZE; gy++) {
                    if (findStopAt(gx, gy) == null) {
                        double x = pointX(gx);
                        double y = pointY(gy);
                        drawingBrush.fillOval(x - 4, y - 4, 8, 8);
                    }
                }
            }
            drawingBrush.restore();
        }

        if (clickMode == CLICK_MODE_BUILD_ROUTE && !pendingRouteStops.isEmpty()) {
            drawingBrush.save();
            drawingBrush.setStroke(Color.web("#1565c0"));
            drawingBrush.setLineWidth(3);
            for (int order = 0; order < pendingRouteStops.size(); order++) {
                BusStop stop = pendingRouteStops.get(order);
                Location location = stop.getLocation();
                double x = pointX(location.getX());
                double y = pointY(location.getY());
                double ringSize = iconSize * 0.9;
                drawingBrush.strokeOval(x - ringSize, y - ringSize, ringSize * 2, ringSize * 2);
            }
            drawingBrush.restore();
        }
    }

    /**
     * Draws the grid lines and outer border on the canvas.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     * @param lineWidth    stroke width for grid lines and border
     */
    // step 2
    private void drawGrid(GraphicsContext drawingBrush, double lineWidth) {
        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setStroke(GREY1);
        drawingBrush.setLineWidth(lineWidth);
        for (int i = 0; i <= GRID_SIZE; i++) {
            drawingBrush.strokeLine(pointX(i), pointY(0), pointX(i), pointY(GRID_SIZE)); // Vertical line
            drawingBrush.strokeLine(pointX(0), pointY(i), pointX(GRID_SIZE), pointY(i)); // Horizontal line
        }

        drawingBrush.setLineJoin(StrokeLineJoin.ROUND);
        drawingBrush.setStroke(GREY2);
        drawingBrush.setLineWidth(lineWidth);
        drawingBrush.strokeRect(pointX(0), pointY(0), pointX(GRID_SIZE) - pointX(0), pointY(GRID_SIZE) - pointY(0));
    }

    /**
     * Converts a grid X coordinate into canvas pixel X coordinate.
     *
     * @param gridX grid column coordinate
     * @return pixel X coordinate
     */
    private double pointX(double gridX) {
        return MARGIN + gridX * cellWidth;
    }
        
    /**
     * Converts a grid Y coordinate into canvas pixel Y coordinate.
     *
     * @param gridY grid row coordinate
     * @return pixel Y coordinate
     */
    private double pointY(double gridY) {
        return MARGIN + gridY * cellHeight;
    }

    /**
     * Delegates drawing of all bus stops to {@link BusStop#draw}, applying high-priority styling
     * when applicable.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    private void drawStops(GraphicsContext drawingBrush) {
        double unit = Math.max(7, iconSize * 0.26);

        // step 3 + note 5: high-priority stops draw black/blue, low-priority keep the original look
        for (int i = 0; i < stops.size(); i++) {
            BusStop stop = stops.get(i);
            Location location = stop.getLocation();
            double centerX = pointX(location.getX());
            double centerY = pointY(location.getY());
            Color boxFill = stop.isHighPriority() ? HIGH_PRIORITY_BOX : NAVY1;
            Color accent = stop.isHighPriority() ? HIGH_PRIORITY_ACCENT : ORANGE;
            stop.draw(drawingBrush, i + 1, centerX, centerY, unit,
                CADETBLUE, accent, boxFill, NAVY2, WHITE);
        }
    }

    /**
     * Delegates drawing of all buses to {@link Bus#draw} with slight per-slot offset so overlapping
     * buses at the same intersection remain visible.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     */
    // step 3 - the busses
    private void drawBuses(GraphicsContext drawingBrush) {
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getCurrentLocation();
            double shift = ( (i % 3) - 1) * 6;
            double centerX = pointX(location.getX()) + shift;
            double centerY = pointY(location.getY()) + shift;
            Image icon = busIcons.get(i);
            boolean rerouting = isBusRerouting(i);
            bus.draw(drawingBrush, i + 1, centerX, centerY, iconSize, icon, NAVY2, WHITE, rerouting, RED);
        }
    }
    
    /**
     * Rebuilds the cached {@link Image} icons for every bus at the current {@link #iconSize}.
     */
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

    /**
     * Computes a small pixel offset for a bus's route lines based on its index so parallel
     * route segments do not completely overlap.
     *
     * @param busIndex index of the bus in {@link #buses}
     * @return offset in pixels
     */
    // step 4
    private double shiftOf(int busIndex) {
        return ( (busIndex % 3) - 1) * 4;
    }

    /**
     * Delegates route rendering to each {@link Bus#drawRoute}.
     *
     * @param drawingBrush the canvas {@link GraphicsContext}
     * @param lineWidth    base stroke width used to derive route line width
     */
    private void drawRoutes(GraphicsContext drawingBrush, double lineWidth) {
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            double shift = shiftOf(i);
            bus.drawRoute(drawingBrush, lineWidth, shift, this::pointX, this::pointY);
        }
    }

    /**
     * Advances passenger arrivals, accumulates operating costs, moves non-loading buses toward
     * their destinations, and handles stop arrivals and route progression.
     *
     * @param seconds              elapsed simulated seconds for this frame
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     */
    // step 4 + step 7 (modified the parameter)
    private void moveBuses(double seconds, double nowInSimulatedSeconds) {
        addNewPassengers(seconds, nowInSimulatedSeconds); // step 7

        // note 1: every bus that exists costs money to run for this slice of simulated time,
        // whether it's loading passengers or driving. `seconds` here is the same speed-scaled
        // value the caller uses to advance simulatedMinutes (seconds * SIMULATED_MINUTES_PER_SECOND),
        // so this uses the identical conversion to stay consistent with the simulated clock.
        double simulatedMinutesThisFrame = seconds * SIMULATED_MINUTES_PER_SECOND;
        for (Bus bus : buses) {
            totalOperatingCost += bus.getOperatingCostPerMinute() * simulatedMinutesThisFrame;
        }

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

    /**
     * Assigns a random passenger arrival rate to each stop in {@link #stops}.
     */
    // step 5
    private void createArrivalRates() {
        for (BusStop stop : stops) {
            stop.setArrivalRate(0.1 + 0.4 * random.nextDouble());
        }
    }

    /**
     * Advances passenger arrivals at every stop over the elapsed simulated time slice.
     *
     * @param seconds              elapsed simulated seconds for this frame
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     */
    private void addNewPassengers(double seconds, double nowInSimulatedSeconds) {
        for (BusStop stop : stops) {
            stop.advancePassengers(seconds, nowInSimulatedSeconds);
        }
    }

    /**
     * Resets loading timers to {@code 0.0} for all buses.
     */
    private void resetLoadingTimers() {
        for (Bus bus : buses) {
            bus.startLoading(0.0);
        }
    }

    /**
     * Finds the index in {@link #stops} of a stop located at {@code location}.
     *
     * @param location target grid location
     * @return matching stop index, or {@code -1} if no stop is at that location
     */
    private int findStopIndexAt(Location location) {
        for (int i = 0; i < stops.size(); i++) {
            Location stopLocation = stops.get(i).getLocation();

            if (stopLocation.getX() == location.getX() && stopLocation.getY() == location.getY()) {
                return i;
            }
        }
        return -1;
    }
        
    /**
     * Handles a bus arriving at a stop: checks serviceability and operating hours, transfers
     * passengers, starts the loading timer, logs activity, and clears completed reroutes.
     *
     * @param busIndex             index of the arriving bus in {@link #buses}
     * @param bus                  the arriving {@link Bus}
     * @param stopLocation         the {@link Location} reached
     * @param nowInSimulatedSeconds current simulated clock time in seconds
     */
    private void arriveAtStop(int busIndex, Bus bus, Location stopLocation, double nowInSimulatedSeconds) {
        int stopIndex = findStopIndexAt(stopLocation);
        if (stopIndex == -1) {
            return;
        }
        BusStop stop = stops.get(stopIndex);

        // note 5: a bus that can't serve this stop (e.g. a DoubleDecker at a low-priority stop,
        // which shouldn't normally happen given how routes are built, but can via manual editing),
        // or a low-priority stop that is currently closed, gets no boarding at all - everyone stays
        // on board, nobody waiting gets picked up, and the bus does not pause to load.
        if (!bus.canServe(stop) || !stop.isOpenAt(simulatedMinutes)) {
            log(bus.getBusPrefix() + (busIndex + 1) + " passed Stop " + (stopIndex + 1)
                    + " without stopping (closed or not servable by this bus type).");
        } else {
            int gettingOff = bus.getCurrentPassengers();
            bus.setCurrentPassengers(0);
            int gettingOn = stop.boardPassengers(bus.getPassengerCapacity(), nowInSimulatedSeconds);
            bus.setCurrentPassengers(gettingOn);
            bus.setLastVisitedStop(stop.getLocation());
            bus.startLoading(LOADING_SECONDS);

            log(bus.getBusPrefix() + (busIndex + 1) + " at Stop " + (stopIndex + 1) + ": "
                        + gettingOff + " got off, " + gettingOn + " got on, " + stop.getPassenger() + " still waiting");
        }

        // Step 7: Clear reroute status if this stop was the rerouted target
        if (reroutedTarget.get(busIndex) == stopIndex) {
            reroutedTarget.set(busIndex, -1);
            log(bus.getBusPrefix() + (busIndex + 1) + " finished its reroute to stop " + 
                (stopIndex + 1) + ".");
        }
    }

    /**
     * Phase 2 dynamic rerouting: finds the untargeted stop exceeding {@link #allottedWaitMinutes}
     * with the longest waiting passenger time, and dispatches the nearest eligible empty
     * {@link Reroutable} bus within {@link #MAX_REROUTE_DISTANCE}.
     */
    // step 7 - phase 2
    private void rerouteEmptyBuses() {
        int worstStopIndex = -1;
        double worstWaitSeconds = -1;
        boolean targetedByAnotherBus = false;

        for (BusStop stop : stops) {
            targetedByAnotherBus = false;
            // Skip stops already targeted as a current destination by any bus
            for (Bus bus : buses) {
                Location stopLoc = stop.getLocation();
                Location destLoc = bus.getDestination();
                if (destLoc.getX() == stopLoc.getX()
                        && destLoc.getY() == stopLoc.getY()) {
                    targetedByAnotherBus = true;
                    break;
                }
            }

            if (targetedByAnotherBus) { continue; }
            double oldestWaitSeconds = stop.getOldestWaitSeconds(simulatedMinutes * 60.0);
            if (oldestWaitSeconds > allottedWaitMinutes * 60.0 && oldestWaitSeconds > worstWaitSeconds) {
                worstWaitSeconds = oldestWaitSeconds;
                worstStopIndex = stops.indexOf(stop);
            }
        }

        if (worstStopIndex == -1) {
            return;
        }

        BusStop worstStop = stops.get(worstStopIndex);
        Location target = worstStop.getLocation();

        // note 2 + note 5: among every EMPTY, idle, Reroutable bus that can actually serve this
        // stop, pick the NEAREST one - not just the first match in list order - and only if it
        // is within MAX_REROUTE_DISTANCE cells, so a bus never abandons its own route to chase
        // a stop clear across the map.
        int nearestBusIndex = -1;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            boolean eligible = bus.getCurrentPassengers() == 0
                    && reroutedTarget.get(i) == -1
                    && bus instanceof Reroutable
                    && bus.canServe(worstStop);

            if (!eligible) { continue; }

            double distance = bus.getCurrentLocation().distanceTo(target);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestBusIndex = i;
            }
        }

        if (nearestBusIndex != -1 && nearestDistance <= MAX_REROUTE_DISTANCE) {
            Bus bus = buses.get(nearestBusIndex);
            ((Reroutable) bus).reRoute(target);
            reroutedTarget.set(nearestBusIndex, worstStopIndex);
            totalOperatingCost += REROUTE_OVERHEAD_COST;
            log(String.format("t=%.0fm: Rerouted %s%d -> Stop %d (%.1f cells away, overdue %.1f min)",
                    simulatedMinutes, bus.getBusPrefix(), nearestBusIndex + 1, worstStopIndex + 1,
                    REROUTE_OVERHEAD_COST, nearestDistance, worstWaitSeconds / 60.0));
        }
    }

    /**
     * Computes the overall average passenger wait time in minutes across all stops based on
     * total wait seconds and total boarded passengers.
     *
     * @return average wait time in minutes, or {@link Double#NaN} if no passengers have boarded
     */
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


    /**
     * Handles mouse clicks on the map pane according to the current {@link #clickMode} and
     * mouse button (adding/deleting stops or buses, building/editing routes, adding passengers,
     * or generating a new random map).
     *
     * @param event the JavaFX {@link MouseEvent}
     */
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
                if (clickedStop != null) {
                    if (!pendingRouteStops.contains(clickedStop)) {
                        pendingRouteStops.add(clickedStop);
                    } else if (pendingRouteStops.get(pendingRouteStops.size() - 1) == clickedStop) {
                        // Clicking the last-added stop toggles it back off
                        pendingRouteStops.remove(clickedStop);
                    }
                }
                editHintLabel.setText("Route so far: " + pendingRouteStops.size() + " stop(s). Click more, or press Finish bus.");
            }
            draw();
            return;
        }

        // additional note: delete a stop - click any existing stop while this mode is active
        if (clickMode == CLICK_MODE_DELETE_STOP && event.getButton() == MouseButton.PRIMARY) {
            if (gridPoint != null) {
                BusStop clickedStop = findStopAt(gridPoint[0], gridPoint[1]);
                if (clickedStop != null) {
                    deleteStop(clickedStop);
                }
            }
            draw();
            return;
        }

        // additional note: delete a bus, OR (if "Edit route" was pressed first) pick which
        // bus's route to start re-picking. Both start the same way: click a bus's icon.
        if (clickMode == CLICK_MODE_DELETE_BUS && event.getButton() == MouseButton.PRIMARY) {
            Bus clickedBus = findBusAt(event.getX(), event.getY());
            if (clickedBus != null) {
                if (nextBusClickStartsEdit) {
                    startEditingRoute(clickedBus);
                } else {
                    deleteBus(clickedBus);
                }
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

    /**
     * Hit-tests a pixel coordinate against each bus's rendered icon position.
     *
     * @param pixelX click X coordinate in canvas pixels
     * @param pixelY click Y coordinate in canvas pixels
     * @return the matching {@link Bus}, or {@code null} if no bus icon is within hit radius
     */
    // additional note: hit-tests a click against every bus's drawn icon position, using the
    // same pixel math as drawBuses(), so clicking directly on a bus picture selects it.
    private Bus findBusAt(double pixelX, double pixelY) {
        double hitRadius = Math.max(16, iconSize / 2.0);
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            Location location = bus.getCurrentLocation();
            double shift = ((i % 3) - 1) * 6;
            double centerX = pointX(location.getX()) + shift;
            double centerY = pointY(location.getY()) + shift;
            double distance = Math.hypot(pixelX - centerX, pixelY - centerY);
            if (distance <= hitRadius) {
                return bus;
            }
        }
        return null;
    }

    /**
     * Removes a stop from {@link #stops} and its corresponding entry in {@link #initialStopPassengers}.
     *
     * @param stop the {@link BusStop} to remove
     */
    // additional note: removes a stop entirely. Any bus whose fixed route passes through it
    // keeps driving its existing route list (Bus.route is unchanged here - removing a stop
    // from every bus's route List<Location> would need Bus/Vehicle changes we are avoiding per
    // note 13), but boarding simply can't happen there any more since the stop object itself
    // (and its passengers) are gone from `stops`; a bus passing that location just finds no
    // BusStop via findStopIndexAt() and continues, exactly like the manual-stop "no owner" case.
    private void deleteStop(BusStop stop) {
        int index = stops.indexOf(stop);
        if (index == -1) { return; }
        stops.remove(index);
        initialStopPassengers.remove(index);
        log("Deleted Stop " + (index + 1) + ".");
    }

    /**
     * Removes a bus and keeps parallel per-bus lists ({@link #nextStopIndexes},
     * {@link #reroutedTarget}, and icon cache) synchronized.
     *
     * @param bus the {@link Bus} to remove
     */
    // additional note: removes a bus and all of its per-bus bookkeeping in one place, so the
    // parallel lists (busIcons, nextStopIndexes, reroutedTarget) never fall out of sync with
    // `buses` - the same three lists createBuses()/finishNewBus() keep in lockstep elsewhere.
    private void deleteBus(Bus bus) {
        int index = buses.indexOf(bus);
        if (index == -1) { return; }
        buses.remove(index);
        if (index < nextStopIndexes.size()) { nextStopIndexes.remove(index); }
        if (index < reroutedTarget.size()) { reroutedTarget.remove(index); }
        builtIconSize = -1;   // rebuild the icon list/cache to match the shorter buses list
        log("Deleted " + bus.getBusPrefix() + (index + 1) + ".");
    }

    /**
     * Enters route-editing mode for an existing bus so the user can click new stops in order.
     *
     * @param bus the {@link Bus} whose route is being edited
     */
    // additional note: begins re-picking a route for an existing bus, reusing the same
    // "click stops in order" flow as creating a new bus (pendingRouteStops / Finish bus).
    private void startEditingRoute(Bus bus) {
        busBeingEdited = bus;
        nextBusClickStartsEdit = false;
        clickMode = CLICK_MODE_BUILD_ROUTE;
        pendingRouteStops.clear();
        finishBusButton.setDisable(false);
        cancelBusButton.setDisable(false);
        editHintLabel.setText("Click the new stops for " + bus.getBusPrefix()
                + (buses.indexOf(bus) + 1) + "'s route, in order (at least 2), then press Finish.");
    }

    /**
     * Clears all stops, buses, passenger snapshots, and simulation state back to an empty grid.
     */
    // additional note: removes every stop and every bus, leaving a blank grid - a quick way
    // to start from nothing rather than a random layout.
    private void clearMap() {
        stops.clear();
        initialStopPassengers.clear();
        buses.clear();
        nextStopIndexes.clear();
        reroutedTarget.clear();
        busIcons.clear();
        builtIconSize = -1;

        phase = 0;
        running = false;
        paused = false;
        simulatedMinutes = 0;
        phase1AverageWait = Double.NaN;
        phase2AverageWait = Double.NaN;
        cancelRouteBuilding();
        if (addStopButton != null) { addStopButton.setSelected(false); }

        log("Map cleared.");
        refreshLabels();
        draw();
    }

    /**
     * Snaps a canvas pixel position to a valid grid intersection if within snap tolerance.
     *
     * @param pixelX X position in canvas pixels
     * @param pixelY Y position in canvas pixels
     * @return {@code [gridX, gridY]} if within bounds and tolerance, or {@code null} otherwise
     */
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

    /**
     * Finds an existing {@link BusStop} at the given integer grid coordinates.
     *
     * @param gridX grid X coordinate
     * @param gridY grid Y coordinate
     * @return the {@link BusStop} at {@code (gridX, gridY)}, or {@code null} if none exists
     */
    private BusStop findStopAt(int gridX, int gridY) {
        for (BusStop stop : stops) {
            Location location = stop.getLocation();
            if ( (int) Math.round(location.getX()) == gridX && (int) Math.round(location.getY()) == gridY) {
                return stop;
            }
        }
        return null;
    }

    /**
     * Manually places a new {@link BusStop} at {@code (gridX, gridY)} with a random arrival rate
     * and initial crowd, then logs the addition.
     *
     * @param gridX grid X coordinate
     * @param gridY grid Y coordinate
     */
    private void addStopManually(int gridX, int gridY) {
        BusStop newStop = new BusStop(gridX, gridY);
        newStop.setArrivalRate(0.1 + 0.4 * random.nextDouble());
        newStop.updatePassengers(1 + random.nextInt(6));   // a small starting crowd, smaller than a fresh map's
        stops.add(newStop);
        log("Added a new stop at (" + gridX + ", " + gridY + "). It has no bus route yet"
                + " - add a bus through it, or it will only ever be reached by Phase 2 relocation.");
    }

    /**
     * Validates {@link #pendingRouteStops}, enforces Double Decker stop priority rules, and either
     * creates a new bus or replaces the edited bus with a new instance preserving type and color.
     */
    private void finishNewBus() {
        if (pendingRouteStops.size() < 2) {
            editHintLabel.setText("Pick at least 2 stops before finishing.");
            return;
        }

        // additional note: editing an existing bus's route reuses the SAME bus TYPE and COLOR
        // as the bus being edited, so only its stops change, not what kind of bus it is.
        boolean isEditing = (busBeingEdited != null);
        String chosenType = isEditing ? typeNameOf(busBeingEdited) : newBusTypeBox.getValue();

        // note 5: a DoubleDecker may only be given high-priority stops
        if (chosenType.equals("Double Decker")) {
            for (BusStop stop : pendingRouteStops) {
                if (!stop.isHighPriority()) {
                    editHintLabel.setText("A Double Decker can only serve high-priority (black/blue) stops."
                            + " Remove the low-priority stop from the route, or choose a different bus type.");
                    return;
                }
            }
        }

        ArrayList<Location> route = new ArrayList<>();
        for (BusStop stop : pendingRouteStops) {
            route.add(stop.getLocation());
        }

        Color busColor = isEditing ? busBeingEdited.getColor() : Color.hsb(random.nextFloat() * 360, 0.80, 0.80);

        Bus newBus;
        if (chosenType.equals("Standard Bus")) {
            newBus = new StandardBus(busColor, route);
        } else if (chosenType.equals("Mini Bus")) {
            newBus = new MiniBus(busColor, route);
        } else {
            newBus = new DoubleDecker(busColor, route);
        }

        newBus.updateLocation(route.get(0));
        newBus.updateDestination(route.get(1 % route.size()));

        if (isEditing) {
            // Bus has no route setter (by design - see note 13), so editing a route means
            // replacing the old bus object with a new one of the same type/color at the same
            // list position, which keeps busIcons/nextStopIndexes/reroutedTarget all in sync.
            int index = buses.indexOf(busBeingEdited);
            if (index != -1) {
                buses.set(index, newBus);
                nextStopIndexes.set(index, 1 % route.size());
                reroutedTarget.set(index, -1);
            }
            log("Updated " + newBus.getBusPrefix() + (index + 1) + "'s route (" + pendingRouteStops.size() + " stops).");
        } else {
            buses.add(newBus);
            nextStopIndexes.add(1 % route.size());
            reroutedTarget.add(-1);
            log("Added " + newBus.getBusPrefix() + buses.size() + " (" + chosenType + ") with a "
                    + pendingRouteStops.size() + "-stop route.");
        }

        builtIconSize = -1;   // step 3's icon cache needs rebuilding either way
        busBeingEdited = null;
        cancelRouteBuilding();
    }

    /**
     * Returns the display label matching {@link #newBusTypeBox} for a given {@link Bus} instance.
     *
     * @param bus the {@link Bus} to inspect
     * @return {@code "Standard Bus"}, {@code "Mini Bus"}, or {@code "Double Decker"}
     */
    // additional note: the display name matching newBusTypeBox's options, read off an existing bus
    private String typeNameOf(Bus bus) {
        if (bus instanceof StandardBus) { return "Standard Bus"; }
        if (bus instanceof MiniBus) { return "Mini Bus"; }
        return "Double Decker";
    }

    /**
     * Checks whether the bus at {@code busIndex} is currently heading toward a destination that
     * differs from its scheduled route stop at {@link #nextStopIndexes}.
     *
     * @param busIndex index of the bus in {@link #buses}
     * @return {@code true} if rerouted off its scheduled stop, {@code false} otherwise
     */
    private boolean isBusRerouting(int busIndex) {
        Bus bus = buses.get(busIndex);
        Location destination = bus.getDestination();
        Location expectedNextStop = bus.getRoute().get(nextStopIndexes.get(busIndex));
        return destination.getX() != expectedNextStop.getX() || destination.getY() != expectedNextStop.getY();
    }

    /**
     * Formats the bus's current destination as {@code "Stop N"} if it matches a stop in {@link #stops},
     * or {@code "?"} if no stop is at that location.
     *
     * @param bus the {@link Bus} whose destination is described
     * @return human-readable destination label
     */
    private String describeDestination(Bus bus) {
        Location destination = bus.getDestination();
        int stopIndex = findStopIndexAt(destination);
        return (stopIndex == -1) ? "?" : ("Stop " + (stopIndex + 1));
    }

    /**
     * Cancels any in-progress route creation or route editing, resets click/cursor state,
     * disables finish/cancel buttons, and redraws the map.
     */
    private void cancelRouteBuilding() {
        pendingRouteStops.clear();
        clickMode = CLICK_MODE_NONE;
        busBeingEdited = null;              // additional note: cancel also drops a route-edit in progress
        nextBusClickStartsEdit = false;
        if (finishBusButton != null) { finishBusButton.setDisable(true); }
        if (cancelBusButton != null) { cancelBusButton.setDisable(true); }
        if (addStopButton != null && !addStopButton.isSelected()) {
            mapPane.setCursor(Cursor.DEFAULT);
        }
        if (editHintLabel != null) { editHintLabel.setText(DEFAULT_EDIT_HINT); }
        draw();
    }

}