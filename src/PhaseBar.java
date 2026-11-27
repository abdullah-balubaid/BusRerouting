import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> PhaseBar</p>
 * <p><b>Purpose:</b> The bar at the top of Simulation Mode that is always visible: the buttons that run Phase 1
 * and the two Phase 2 rerouting methods, the pause button, what is happening now, and how far the run has got.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class PhaseBar {
    private final VBox view;                                       // The whole bar, ready to place in the panel
    private final ToggleButton pauseButton;                        // Shows whether the run is paused
    private final Label statusLabel = new Label();                 // What is happening now
    private final ProgressBar progressBar = new ProgressBar(0);    // How far the run has got
    private final Label timeLabel = new Label();                   // "120 / 300 min"

    /**
     * Builds the bar: Phase 1 on the left, the two Phase 2 methods on the right.
     *
     * @param actions where the phase and pause buttons send their presses
     */
    public PhaseBar(SimulationModePanel.Actions actions) {
        Button phase1Button = phaseButton("Fixed cycle (1)", "Phase 1: the buses keep their fixed cycle",
                PhaseRun.FIXED_CYCLE, actions);
        Button serviceButton = phaseButton(RerouteMethod.SERVICE_FIRST.getDisplayName() + " (2)",
                "Phase 2: buses are sent to the stops whose passengers wait longest", PhaseRun.SERVICE_FIRST, actions);
        Button costButton = phaseButton(RerouteMethod.COST_SAVING.getDisplayName() + " (3)",
                "Phase 2: empty buses wait for passengers, and big crowds nearby get a bus", PhaseRun.COST_SAVING, actions);

        GridPane phaseButtons = new GridPane();
        phaseButtons.setHgap(6);
        phaseButtons.setVgap(3);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints third = new ColumnConstraints();
            third.setPercentWidth(100.0 / 3);
            phaseButtons.getColumnConstraints().add(third);
        }
        phaseButtons.add(groupHeading("Phase 1"), 0, 0);
        phaseButtons.add(groupHeading("Phase 2: rerouting"), 1, 0, 2, 1);
        phaseButtons.add(phase1Button, 0, 1);
        phaseButtons.add(serviceButton, 1, 1);
        phaseButtons.add(costButton, 2, 1);

        pauseButton = new ToggleButton("Pause (Space)");
        pauseButton.setOnAction(e -> actions.setPaused(pauseButton.isSelected()));
        statusLabel.setStyle("-fx-font-weight: bold;");
        statusLabel.setText("Ready. Choose a phase to run.");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(6, statusLabel, spacer, pauseButton);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        HBox progressRow = new HBox(8, progressBar, timeLabel);

        view = new VBox(6, phaseButtons, statusRow, progressRow);
    }

    /**
     * Returns the bar to place in the panel.
     *
     * @return the bar's root node
     */
    public VBox getView() {
        return view;
    }

    /**
     * Makes the pause button show whether the simulation is paused.
     *
     * @param paused {@code true} if the simulation is paused
     */
    public void setPaused(boolean paused) {
        pauseButton.setSelected(paused);
    }

    /**
     * Refreshes the bar: what is happening, and how far the run has got.
     *
     * @param active       the run that is going or went last, or {@code null}
     * @param totalMinutes length of a full run in minutes
     * @param paused       whether the run is paused
     */
    public void refresh(PhaseRun active, double totalMinutes, boolean paused) {
        if (active == null) {
            statusLabel.setText("Ready. Choose a phase to run.");
            progressBar.setProgress(0);
            timeLabel.setText(String.format("0 / %d min", (int) totalMinutes));
            return;
        }

        String phaseName = PhaseRun.nameOf(active.getPhaseNumber());
        if (active.isFinished()) {
            statusLabel.setText(phaseName + " finished");
        } else {
            statusLabel.setText((paused ? "Paused: " : "Running ") + phaseName);
        }
        progressBar.setProgress(active.getProgress());
        timeLabel.setText(String.format("%d / %d min", (int) active.getSimulatedMinutes(), (int) active.getTotalMinutes()));
    }

    /**
     * Makes one of the phase buttons.
     *
     * @param text        the button text
     * @param tooltip     what the phase does
     * @param phaseNumber the phase it starts
     * @param actions     where the press is sent
     * @return the button
     */
    private Button phaseButton(String text, String tooltip, int phaseNumber, SimulationModePanel.Actions actions) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(e -> actions.startPhase(phaseNumber));
        return button;
    }

    /**
     * Makes the small centred heading above a group of phase buttons.
     *
     * @param text the heading
     * @return the label
     */
    private Label groupHeading(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-font-weight: bold; -fx-border-color: transparent transparent "
                + Palette.css(Palette.HEADING_UNDERLINE) + " transparent;");
        return label;
    }
}
