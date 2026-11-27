import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> SettingsTab</p>
 * <p><b>Purpose:</b> The Settings tab: the map (seed, number of stops, new random map), the run (acceptable
 * passenger wait, simulation duration, speed) and the batch test. It keeps the limits of every setting, shows
 * them in the labels, and hands back the chosen values with any impossible entry put back inside its limits.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class SettingsTab {
    // Limits of the settings, shown in their labels
    private static final int MIN_STOPS = 3;
    private static final int MIN_WAIT_MINUTES = 1;
    private static final int MAX_WAIT_MINUTES = 120;
    private static final int DEFAULT_WAIT_MINUTES = 15;
    private static final int MIN_DURATION_MINUTES = 30;
    private static final int MAX_DURATION_MINUTES = 1440;   // one full day
    private static final int DURATION_STEP_MINUTES = 30;    // Size of one click on the duration arrows

    // The speeds the slider snaps to, and the one it starts on
    private static final double[] SPEEDS = {0.5, 1, 2, 5, 10, 20, 30};
    private static final int DEFAULT_SPEED_INDEX = 1;

    private final Tab tab;
    private final Label seedLabel;
    private final TextField seedField;
    private final Spinner<Integer> stopsSpinner;
    private final Spinner<Integer> waitSpinner;
    private final Spinner<Integer> durationSpinner;
    private final Slider speedSlider;
    private final Label speedLabel = new Label();
    private final int maxStops;

    /**
     * Builds the tab.
     *
     * @param actions      where the seed, new map and batch test buttons send their presses
     * @param defaultStops starting value of the stops spinner
     * @param maxStops     largest value of the stops spinner
     * @param initialSeed  seed of the map shown at start-up
     * @param totalMinutes starting value of the duration spinner
     */
    public SettingsTab(SimulationModePanel.Actions actions, int defaultStops, int maxStops, long initialSeed,
                       double totalMinutes) {
        this.maxStops = maxStops;

        seedLabel = new Label("Current seed: " + initialSeed);
        seedField = new TextField(String.valueOf(initialSeed));
        Button useSeedButton = new Button("Use seed");
        useSeedButton.setOnAction(e -> actions.useSeed(seedField.getText()));
        HBox seedRow = new HBox(6, seedField, useSeedButton);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        Button newMapButton = new Button("New random map (N)");
        Button batchButton = new Button("Run batch test (" + BatchTest.NETWORKS + " maps)");
        newMapButton.setMaxWidth(Double.MAX_VALUE);
        batchButton.setMaxWidth(Double.MAX_VALUE);
        newMapButton.setOnAction(e -> actions.newRandomMap());
        batchButton.setOnAction(e -> actions.runBatchTest());

        stopsSpinner = makeSpinner(MIN_STOPS, maxStops, defaultStops, 1);
        waitSpinner = makeSpinner(MIN_WAIT_MINUTES, MAX_WAIT_MINUTES, DEFAULT_WAIT_MINUTES, 1);
        durationSpinner = makeSpinner(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES, (int) totalMinutes, DURATION_STEP_MINUTES);
        waitSpinner.setTooltip(new Tooltip("A passenger who waits this long or less is fully satisfied. "
                + "After this wait a stop counts as overdue and may get a bus."));

        speedSlider = new Slider(0, SPEEDS.length - 1, DEFAULT_SPEED_INDEX);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setSnapToTicks(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double index) {
                return speedName(SPEEDS[(int) Math.round(index)]);
            }

            @Override
            public Double fromString(String text) {
                return 0.0;
            }
        });
        speedLabel.setText("Speed: " + speedName(getSpeedFactor()));
        speedSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                speedLabel.setText("Speed: " + speedName(getSpeedFactor())));

        VBox box = new VBox(8,
                heading("Map"), seedLabel, seedRow,
                new Label("Number of stops (" + MIN_STOPS + "–" + maxStops + "):"), stopsSpinner,
                newMapButton,
                heading("Run"),
                new Label("Acceptable passenger wait (" + MIN_WAIT_MINUTES + "–" + MAX_WAIT_MINUTES + " min):"), waitSpinner,
                new Label("Simulation duration (" + MIN_DURATION_MINUTES + "–" + MAX_DURATION_MINUTES + " min):"), durationSpinner,
                speedLabel, speedSlider,
                heading("Test"),
                batchButton);
        box.setPadding(new Insets(10));
        tab = new Tab("Settings", box);
    }

    /**
     * Returns the tab to place in the tab pane.
     *
     * @return the tab
     */
    public Tab getTab() {
        return tab;
    }

    /**
     * Returns the number of stops chosen in the spinner.
     *
     * @return number of stops for the next generated map
     */
    public int getStopCount() {
        return readSpinner(stopsSpinner, MIN_STOPS, maxStops);
    }

    /**
     * Returns the acceptable passenger wait chosen in the spinner.
     *
     * @return acceptable wait in minutes
     */
    public int getAcceptableWaitMinutes() {
        return readSpinner(waitSpinner, MIN_WAIT_MINUTES, MAX_WAIT_MINUTES);
    }

    /**
     * Returns the run length chosen in the spinner.
     *
     * @return simulated run length in minutes
     */
    public int getDurationMinutes() {
        return readSpinner(durationSpinner, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES);
    }

    /**
     * Returns the simulation speed chosen on the slider.
     *
     * @return speed factor
     */
    public double getSpeedFactor() {
        return SPEEDS[(int) Math.round(speedSlider.getValue())];
    }

    /**
     * Shows the seed of the map currently on screen, in both the label and the entry field.
     *
     * @param seed the seed to show
     */
    public void showSeed(long seed) {
        seedLabel.setText("Current seed: " + seed);
        seedField.setText(String.valueOf(seed));
    }

    /**
     * Shows that the map on screen came from a loaded build, not from a seed.
     */
    public void showLoadedBuild() {
        seedLabel.setText("Current seed: none (loaded build)");
    }

    /**
     * Reads a spinner, including text typed but not yet confirmed. A value outside the limits is put
     * back inside them (and shown that way), so the user cannot enter an impossible setting.
     *
     * @param spinner the spinner
     * @param min     smallest allowed value
     * @param max     largest allowed value
     * @return the value, between {@code min} and {@code max}
     */
    private int readSpinner(Spinner<Integer> spinner, int min, int max) {
        int value;
        try {
            spinner.increment(0);   // commits any typed text
            value = spinner.getValue();
        } catch (RuntimeException notANumber) {
            value = min;
        }
        value = Math.max(min, Math.min(max, value));
        spinner.getValueFactory().setValue(value);
        return value;
    }

    /**
     * Words a speed.
     *
     * @param speed the speed factor
     * @return text such as {@code "2×"} or {@code "0.5×"}
     */
    private String speedName(double speed) {
        return (speed == Math.floor(speed) ? String.valueOf((int) speed) : String.valueOf(speed)) + "×";
    }

    /**
     * Makes a bold heading that starts a group in the tab.
     *
     * @param text the heading
     * @return the label
     */
    private Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        return label;
    }

    /**
     * Makes an editable spinner that fills the width.
     *
     * @param min     smallest value
     * @param max     largest value
     * @param initial starting value
     * @param step    size of one click on the arrows
     * @return the spinner
     */
    private Spinner<Integer> makeSpinner(int min, int max, int initial, int step) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }
}
