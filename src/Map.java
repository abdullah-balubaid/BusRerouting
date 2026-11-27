import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Random;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Map</p>
 * <p><b>Purpose:</b> The program itself: creates the {@link BusNetwork} and the screen parts, connects
 * them to each other, and runs the animation loop that advances the current {@link PhaseRun}. It
 * decides what happens when a button is pressed (start a phase, new map, batch test); how the network
 * behaves is {@link BusNetwork}'s and {@link PhaseRun}'s job, and how things look is the screen
 * classes' ({@link GridCanvas}, {@link ControlPanel}, {@link SimulationModePanel}, {@link BuildModePanel}).</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v2.0 11/26/26
 */
public class Map extends Application {
    private static final int NUMBER_OF_STOPS = 7;                      // Stops of the first map
    private static final int MAX_NUMBER_OF_STOPS = 60;                 // Largest number the stops spinner allows
    private static final double DEFAULT_SIMULATED_MINUTES = 300.0;     // Run length at start-up; the user can change it
    private static final double MAX_FRAME_SECONDS = 0.1;               // Longer frames (window drags) are cut short

    // The model
    private final BusNetwork network = new BusNetwork();
    private long currentSeed = new Random().nextLong();   // Seed of the map on screen
    private double totalSimulatedMinutes = DEFAULT_SIMULATED_MINUTES;   // Length of a run
    private int batchTestsRun = 0;                        // Counts batch tests, so each one is told apart in the log

    // The runs: each phase's latest run (index = phase number - 1), and the one that is going (or went last)
    private final PhaseRun[] runs = new PhaseRun[3];
    private PhaseRun activeRun = null;
    private boolean paused = false;

    // The screen
    private GridCanvas gridCanvas;
    private SimulationModePanel simulationPanel;
    private BuildModePanel buildPanel;
    private ControlPanel controlPanel;

    /**
     * Creates the first random map, the screen parts and the animation loop, then shows the window.
     *
     * @param stage the primary stage for this application
     */
    @Override
    public void start(Stage stage) {
        network.setEventLog(this::log);
        network.generate(currentSeed, NUMBER_OF_STOPS);

        gridCanvas = new GridCanvas(network);
        gridCanvas.setOnClick(this::handleMapClick);
        gridCanvas.setStepFraction(() -> (activeRun == null || activeRun.isFinished()) ? 1.0 : activeRun.getStepFraction());

        simulationPanel = new SimulationModePanel(createSimulationActions(), network, NUMBER_OF_STOPS,
                MAX_NUMBER_OF_STOPS, currentSeed, DEFAULT_SIMULATED_MINUTES);

        buildPanel = new BuildModePanel(gridCanvas, network, new BuildModePanel.Host() {
            @Override
            public void beforeEdit() {
                resetRun();
            }

            @Override
            public void log(String message) {
                Map.this.log(LogKind.EDIT, message);
            }

            @Override
            public void mapChanged() {
                refreshScreen();
            }
        });

        controlPanel = new ControlPanel(simulationPanel, buildPanel, this::onModeChanged);

        BuildSharing sharing = new BuildSharing(stage, network, new BuildSharing.Host() {
            @Override
            public void log(LogKind kind, String message) {
                Map.this.log(kind, message);
            }

            @Override
            public void buildLoaded(String source) {
                onBuildLoaded(source);
            }
        });
        AppMenuBar menuBar = new AppMenuBar(sharing, new GitHubDialog(stage, getHostServices()));

        // The divider between the map and the side bar can be dragged; the side bar keeps its own
        // width when the window is resized, and its limits are set in ControlPanel
        SplitPane split = new SplitPane(gridCanvas.getPane(), controlPanel.getView());
        SplitPane.setResizableWithParent(controlPanel.getView(), false);
        BorderPane root = new BorderPane(split);
        root.setTop(menuBar.getView());

        Scene scene = new Scene(root, 900, 700);
        new KeyboardShortcuts(scene, createShortcutActions(sharing));

        startAnimation();

        stage.setTitle("Bus Rerouting");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(660);
        stage.show();

        // The map is square, so the map area is made as wide as it is tall and the side bar takes the rest
        // (within its own limits). The divider can still be dragged; it snaps back when the window is resized.
        Runnable fitSideBarToMap = () -> {
            double width = split.getWidth();
            double sideBar = Math.max(ControlPanel.MIN_WIDTH, Math.min(ControlPanel.MAX_WIDTH, width - split.getHeight()));
            if (width > 0) {
                split.setDividerPositions(1 - sideBar / width);
            }
        };
        // Applied after the split pane has laid itself out, otherwise it puts its own divider position back
        split.widthProperty().addListener((obs, was, isNow) -> Platform.runLater(fitSideBarToMap));
        split.heightProperty().addListener((obs, was, isNow) -> Platform.runLater(fitSideBarToMap));
        Platform.runLater(fitSideBarToMap);

        refreshScreen();
    }

    /**
     * Starts the real-time loop that advances the running phase and redraws the screen every frame.
     */
    private void startAnimation() {
        new AnimationTimer() {
            private long previousTime = 0;

            @Override
            public void handle(long currentTime) {
                if (previousTime == 0) {
                    previousTime = currentTime;
                    return;
                }
                double seconds = (currentTime - previousTime) / 1_000_000_000.0;
                previousTime = currentTime;

                advanceFrame(Math.min(seconds, MAX_FRAME_SECONDS));
                refreshScreen();
            }
        }.start();
    }

    /**
     * Advances the running phase by the real time that passed since the last frame, multiplied by the
     * chosen speed. {@link PhaseRun} uses it in fixed steps, so the result never depends on the speed.
     *
     * @param realSeconds real seconds since the previous frame
     */
    private void advanceFrame(double realSeconds) {
        if (!isRunning() || paused) {
            return;
        }

        activeRun.advance(realSeconds * simulationPanel.getSpeedFactor());

        if (activeRun.isFinished()) {
            PhaseResult result = activeRun.getResult();
            log(LogKind.PHASE, String.format("=== Finished at t=%d === distance driven: %.1f km, driving cost: %.0f, passengers served: %d, satisfaction: %.2f stars",
                    (int) totalSimulatedMinutes, result.getTotalDistanceKm(), result.getTotalCost(), result.getPassengersServed(), result.getAverageStars()));
        }
    }

    /**
     * Tells whether a phase is going (a finished phase is not running any more).
     *
     * @return {@code true} while a phase is running, paused or not
     */
    private boolean isRunning() {
        return activeRun != null && !activeRun.isFinished();
    }

    // ------------------------------------------------------------------ what the buttons do

    /**
     * Starts or restarts a phase on the map on screen. Every phase begins from the same starting state,
     * so they can be compared. A phase that was stopped before it finished is dropped, because its
     * figures would not be final.
     *
     * @param phaseNumber {@link PhaseRun#FIXED_CYCLE}, {@link PhaseRun#SERVICE_FIRST} or {@link PhaseRun#COST_SAVING}
     */
    private void startPhase(int phaseNumber) {
        useChosenDuration();

        for (int i = 0; i < runs.length; i++) {
            if (runs[i] != null && !runs[i].isFinished()) {
                runs[i] = null;
            }
        }

        activeRun = new PhaseRun(network, phaseNumber, totalSimulatedMinutes,
                simulationPanel.getAcceptableWaitMinutes());
        runs[phaseNumber - 1] = activeRun;

        paused = false;
        simulationPanel.setPaused(false);
        log(LogKind.PHASE, "=== " + PhaseRun.nameOf(phaseNumber) + " ===");
        refreshScreen();
    }

    /**
     * Takes the run length from the duration spinner. A different run length makes the other phase's
     * numbers incomparable, so any earlier runs are dropped.
     */
    private void useChosenDuration() {
        double chosenMinutes = simulationPanel.getDurationMinutes();
        if (chosenMinutes != totalSimulatedMinutes) {
            totalSimulatedMinutes = chosenMinutes;
            clearRunState();
            log(LogKind.INFO, "Run length is now " + (int) chosenMinutes + " min. Earlier results were cleared so both phases stay comparable.");
        }
    }

    /**
     * Runs the batch test: random maps of the chosen size, both phases on each, without drawing. The
     * test uses its own networks, so the map and run on screen are not touched.
     */
    private void runBatchTest() {
        useChosenDuration();

        BatchTest test = new BatchTest(simulationPanel.getStopCount(), totalSimulatedMinutes,
                simulationPanel.getAcceptableWaitMinutes());
        test.run();

        batchTestsRun++;
        log(LogKind.BATCH, test.describe(batchTestsRun));
        simulationPanel.showLogTab();
    }

    /**
     * Generates a brand-new random seed and rebuilds the map from it.
     */
    // The OLD seed is not reused: a new one is drawn first, so this button always gives variety, and
    // the map can still be reproduced from the seed shown.
    private void newRandomMap() {
        currentSeed = new Random().nextLong();
        rebuildMap("Generated new map with");
    }

    /**
     * Parses the typed seed and rebuilds the map from it, so the SAME seed always reproduces the SAME
     * map (stops, passengers, arrival rates, bus routes and colours).
     *
     * @param seedText the text typed into the seed field
     */
    private void useEnteredSeed(String seedText) {
        try {
            currentSeed = Long.parseLong(seedText.trim());
        } catch (NumberFormatException badNumber) {
            log(LogKind.ERROR, "That seed isn't a valid number - try again.");
            return;
        }
        rebuildMap("Rebuilt map from seed with");
    }

    /**
     * Rebuilds the stops and buses from the current seed, clears the run, and refreshes the screen.
     *
     * @param description start of the log line announcing the new map
     */
    private void rebuildMap(String description) {
        network.generate(currentSeed, simulationPanel.getStopCount());

        clearRunState();
        buildPanel.cancelRouteBuilding();

        log(LogKind.EDIT, description + " " + network.getStops().size() + " stops and " + network.getBuses().size()
                + " buses. Seed: " + currentSeed);
        simulationPanel.showSeed(currentSeed);
        refreshScreen();
    }

    /**
     * Reacts to a build being loaded: the run goes back to the start and the screen is refreshed.
     *
     * @param source where the build came from
     */
    private void onBuildLoaded(String source) {
        clearRunState();
        buildPanel.cancelRouteBuilding();
        simulationPanel.showLoadedBuild();

        log(LogKind.EDIT, "Loaded a build from " + source + ": " + network.getStops().size() + " stops and "
                + network.getBuses().size() + " buses.");
        refreshScreen();
    }

    /**
     * Reacts to the user switching between Simulation Mode and Build Mode. Entering Build Mode pauses
     * a run in progress; the run is only reset once the map is actually changed.
     *
     * @param buildMode {@code true} if Build Mode was entered, {@code false} if it was left
     */
    private void onModeChanged(boolean buildMode) {
        if (buildMode && isRunning() && !paused) {
            paused = true;
            simulationPanel.setPaused(true);
            log(LogKind.INFO, "Paused for editing. Changing the map will reset the run to t=0.");
        }
    }

    // ------------------------------------------------------------------ run state

    /**
     * Forgets the current run: no phase running, no results, not paused.
     */
    private void clearRunState() {
        Arrays.fill(runs, null);
        activeRun = null;
        paused = false;
        simulationPanel.setPaused(false);
    }

    /**
     * Resets the run to t=0 with a fresh starting state. Called right before the map is edited,
     * since a run in progress (or its results) no longer matches an edited map.
     */
    private void resetRun() {
        boolean hadRun = (activeRun != null);

        clearRunState();
        network.restoreStartingState();

        if (hadRun) {
            log(LogKind.INFO, "Run reset because the map was edited.");
        }
    }

    // ------------------------------------------------------------------ screen

    /**
     * Refreshes the side bar and redraws the map.
     */
    private void refreshScreen() {
        if (simulationPanel == null) {
            return;
        }
        simulationPanel.refresh(runs, activeRun, totalSimulatedMinutes, paused);
        if (controlPanel != null && controlPanel.isBuildMode()) {
            buildPanel.refresh();
        }
        gridCanvas.draw();
    }

    /**
     * Appends a line of text to the event log.
     *
     * @param kind    what the line is about (decides its colour)
     * @param message the message to log
     */
    private void log(LogKind kind, String message) {
        if (simulationPanel != null) {
            simulationPanel.log(kind, message);
        }
    }

    /**
     * Handles a click on the map: while a build tool is active the click belongs to the
     * {@link BuildModePanel}; otherwise a right-click on a stop adds 10 waiting passengers. A left
     * click on the map never changes the map.
     *
     * @param event the JavaFX {@link MouseEvent}
     */
    private void handleMapClick(MouseEvent event) {
        if (buildPanel.isEditing()) {
            buildPanel.handleClick(event);
            return;
        }

        if (event.getButton() != MouseButton.SECONDARY) {
            return;
        }

        int[] gridPoint = gridCanvas.pickGridPoint(event.getX(), event.getY());
        if (gridPoint != null) {
            BusStop clickedStop = network.findStopAt(gridPoint[0], gridPoint[1]);
            if (clickedStop != null) {
                double nowInSimulatedSeconds = (activeRun == null) ? 0 : activeRun.getSimulatedMinutes() * 60.0;
                clickedStop.addPassengers(10, nowInSimulatedSeconds);
                log(LogKind.EDIT, "+10 passengers added manually to " + clickedStop.getName() + ".");
            }
        }
        gridCanvas.draw();
    }

    // ------------------------------------------------------------------ wiring

    /**
     * Connects the Simulation Mode buttons to the methods above.
     *
     * @return what the buttons do
     */
    private SimulationModePanel.Actions createSimulationActions() {
        return new SimulationModePanel.Actions() {
            @Override
            public void startPhase(int phaseNumber) {
                Map.this.startPhase(phaseNumber);
            }

            @Override
            public void setPaused(boolean pausedNow) {
                paused = pausedNow;
            }

            @Override
            public void newRandomMap() {
                Map.this.newRandomMap();
            }

            @Override
            public void useSeed(String seedText) {
                useEnteredSeed(seedText);
            }

            @Override
            public void runBatchTest() {
                Map.this.runBatchTest();
            }
        };
    }

    /**
     * Connects the keyboard shortcuts to the same methods the buttons use.
     *
     * @param sharing saves, loads, copies and pastes builds
     * @return what the shortcuts do
     */
    private KeyboardShortcuts.Actions createShortcutActions(BuildSharing sharing) {
        return new KeyboardShortcuts.Actions() {
            @Override
            public boolean isBuildMode() {
                return controlPanel.isBuildMode();
            }

            @Override
            public void togglePause() {
                if (isRunning()) {
                    paused = !paused;
                    simulationPanel.setPaused(paused);
                }
            }

            @Override
            public void startPhase(int phaseNumber) {
                Map.this.startPhase(phaseNumber);
            }

            @Override
            public void newRandomMap() {
                Map.this.newRandomMap();
            }

            @Override
            public void toggleMode() {
                controlPanel.toggleMode();
            }

            @Override
            public void cancelTool() {
                buildPanel.cancelRouteBuilding();
            }

            @Override
            public void saveBuild() {
                sharing.saveToFile();
            }

            @Override
            public void loadBuild() {
                sharing.loadFromFile();
            }

            @Override
            public void copyBuild() {
                sharing.copyToClipboard();
            }

            @Override
            public void pasteBuild() {
                sharing.pasteFromClipboard();
            }
        };
    }

    /**
     * Starts the program.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
