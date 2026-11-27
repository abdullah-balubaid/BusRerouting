import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> SimulationModePanel</p>
 * <p><b>Purpose:</b> The side-bar section used while running the simulation. It puts together the {@link PhaseBar}
 * that is always visible at the top and four tabs below it: {@link ResultsTab}, {@link StopsTab},
 * {@link SettingsTab} and {@link BusesAndLogTab}. The rest of the program talks only to this class, which
 * passes each request on to the part it belongs to and refreshes only the tab that is showing.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v4.0 11/26/26
 */
public class SimulationModePanel {
    private static final int SLOW_REFRESH_FRAMES = 8;    // The stop table and bus list are redrawn every this many frames

    /**
     * What the panel's buttons ask the rest of the program to do.
     */
    public interface Actions {
        /**
         * Starts a simulation phase.
         *
         * @param phaseNumber {@link PhaseRun#FIXED_CYCLE}, {@link PhaseRun#SERVICE_FIRST} or {@link PhaseRun#COST_SAVING}
         */
        void startPhase(int phaseNumber);

        /**
         * Pauses or resumes the running phase.
         *
         * @param paused {@code true} to pause
         */
        void setPaused(boolean paused);

        /**
         * Generates a brand-new random map.
         */
        void newRandomMap();

        /**
         * Rebuilds the map from a typed seed.
         *
         * @param seedText the text typed into the seed field
         */
        void useSeed(String seedText);

        /**
         * Runs the phases on many generated maps and logs the average improvement.
         */
        void runBatchTest();
    }

    private final TabPane tabs = new TabPane();
    private final VBox panel;                           // Everything in this section, ready to place in the window
    private final PhaseBar phaseBar;
    private final ResultsTab resultsTab;
    private final StopsTab stopsTab = new StopsTab();
    private final SettingsTab settingsTab;
    private final BusesAndLogTab logTab;
    private int frameCount = 0;

    /**
     * Builds the panel.
     *
     * @param actions      what the buttons ask the program to do
     * @param network      the network whose buses are listed
     * @param defaultStops starting value of the stops spinner
     * @param maxStops     largest value of the stops spinner
     * @param initialSeed  seed of the map shown at start-up
     * @param totalMinutes length of a full run in simulated minutes (width of the chart's time axis)
     */
    public SimulationModePanel(Actions actions, BusNetwork network, int defaultStops, int maxStops,
                               long initialSeed, double totalMinutes) {
        phaseBar = new PhaseBar(actions);
        resultsTab = new ResultsTab(totalMinutes);
        settingsTab = new SettingsTab(actions, defaultStops, maxStops, initialSeed, totalMinutes);
        logTab = new BusesAndLogTab(network);

        tabs.getTabs().addAll(resultsTab.getTab(), stopsTab.getTab(), settingsTab.getTab(), logTab.getTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        panel = new VBox(10, phaseBar.getView(), tabs);
    }

    /**
     * Returns the panel to place in the side bar.
     *
     * @return the panel's root node
     */
    public VBox getPanel() {
        return panel;
    }

    // ------------------------------------------------------------------ what the user chose

    /**
     * Returns the number of stops chosen in the Settings tab.
     *
     * @return number of stops for the next generated map
     */
    public int getStopCount() {
        return settingsTab.getStopCount();
    }

    /**
     * Returns the acceptable passenger wait chosen in the Settings tab.
     *
     * @return acceptable wait in minutes
     */
    public int getAcceptableWaitMinutes() {
        return settingsTab.getAcceptableWaitMinutes();
    }

    /**
     * Returns the run length chosen in the Settings tab.
     *
     * @return simulated run length in minutes
     */
    public int getDurationMinutes() {
        return settingsTab.getDurationMinutes();
    }

    /**
     * Returns the simulation speed chosen in the Settings tab.
     *
     * @return speed factor
     */
    public double getSpeedFactor() {
        return settingsTab.getSpeedFactor();
    }

    // ------------------------------------------------------------------ showing things

    /**
     * Shows the seed of the map currently on screen.
     *
     * @param seed the seed to show
     */
    public void showSeed(long seed) {
        settingsTab.showSeed(seed);
    }

    /**
     * Shows that the map on screen came from a loaded build, not from a seed.
     */
    public void showLoadedBuild() {
        settingsTab.showLoadedBuild();
    }

    /**
     * Makes the pause button show whether the simulation is paused.
     *
     * @param paused {@code true} if the simulation is paused
     */
    public void setPaused(boolean paused) {
        phaseBar.setPaused(paused);
    }

    /**
     * Brings the Buses &amp; Log tab to the front (used after a batch test, whose summary is written there).
     */
    public void showLogTab() {
        tabs.getSelectionModel().select(logTab.getTab());
    }

    /**
     * Appends a line to the event log, in the colour of its kind.
     *
     * @param kind    what the message is about (decides its colour)
     * @param message the message to log
     */
    public void log(LogKind kind, String message) {
        logTab.log(kind, message);
    }

    /**
     * Refreshes everything that changes while a run goes on. Only the tab that is showing is redrawn.
     *
     * @param runs         the latest run of each phase (index 0 = Phase 1, 1 = service-first, 2 = cost-saving),
     *                     each {@code null} if that phase has not been started
     * @param active       the run that is going or went last, or {@code null} if none has been started
     * @param totalMinutes length of a full run in minutes
     * @param paused       whether the run is paused
     */
    public void refresh(PhaseRun[] runs, PhaseRun active, double totalMinutes, boolean paused) {
        frameCount++;
        phaseBar.refresh(active, totalMinutes, paused);

        if (resultsTab.getTab().isSelected()) {
            resultsTab.refresh(runs, totalMinutes);
        }
        boolean slowFrame = frameCount % SLOW_REFRESH_FRAMES == 0;
        if (stopsTab.getTab().isSelected() && slowFrame) {
            stopsTab.refresh(active);
        }
        if (logTab.getTab().isSelected() && slowFrame) {
            logTab.refresh();
        }
    }
}
