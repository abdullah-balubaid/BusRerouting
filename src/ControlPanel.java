import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> ControlPanel</p>
 * <p><b>Purpose:</b> The whole right-hand side bar: a toggle at the top that swaps between the
 * {@link SimulationModePanel} and the {@link BuildModePanel}, so the user only sees the controls that
 * belong to what they are doing.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.1 11/26/26
 */
public class ControlPanel {
    private final ScrollPane view;                      // The side bar, ready to place in the window
    private final VBox simulationBox;                   // Simulation Mode controls
    private final VBox buildBox;                        // Build Mode controls
    private final BuildModePanel buildModePanel;        // Told to stop editing when Build Mode is left
    private final Consumer<Boolean> onModeChanged;      // Told whenever the user switches mode
    private static final String BUILD_TEXT = "Switch to Build Mode (Ctrl+B)";
    private static final String SIMULATION_TEXT = "Switch to Simulation Mode (Ctrl+B)";
    private final ToggleButton modeToggle;              // The Simulation / Build Mode switch

    /** Narrowest the side bar can be dragged to, in pixels. */
    public static final double MIN_WIDTH = 340;
    /** Widest the side bar can be dragged to, in pixels. */
    public static final double MAX_WIDTH = 1100;
    /** Width the side bar starts at, in pixels. */
    public static final double START_WIDTH = 410;

    /**
     * Builds the side bar and starts it in Simulation Mode.
     *
     * @param simulationModePanel the Simulation Mode section
     * @param buildModePanel      the Build Mode section
     * @param onModeChanged       told {@code true} when Build Mode is entered, {@code false} when it is left
     */
    public ControlPanel(SimulationModePanel simulationModePanel, BuildModePanel buildModePanel,
                        Consumer<Boolean> onModeChanged) {
        this.simulationBox = simulationModePanel.getPanel();
        this.buildBox = buildModePanel.getPanel();
        this.buildModePanel = buildModePanel;
        this.onModeChanged = onModeChanged;

        modeToggle = new ToggleButton(BUILD_TEXT);
        modeToggle.setMaxWidth(Double.MAX_VALUE);
        modeToggle.setOnAction(e -> setBuildMode(modeToggle.isSelected()));
        modeToggle.selectedProperty().addListener((obs, was, isNow) ->
                modeToggle.setText(isNow ? SIMULATION_TEXT : BUILD_TEXT));

        VBox box = new VBox(8, modeToggle, simulationBox, buildBox);
        VBox.setVgrow(simulationBox, Priority.ALWAYS);
        box.setPadding(new Insets(12));
        showSection(false);   // start in Simulation Mode

        view = new ScrollPane(box);
        view.setFitToWidth(true);
        view.setFitToHeight(true);   // the tabs use the whole height of the window
        // The user can drag the divider between the map and this bar, within these limits
        view.setMinWidth(MIN_WIDTH);
        view.setPrefWidth(START_WIDTH);
        view.setMaxWidth(MAX_WIDTH);
    }

    /**
     * Returns the side bar to place in the window.
     *
     * @return the side bar's root node
     */
    public ScrollPane getView() {
        return view;
    }

    /**
     * Tells whether Build Mode is showing.
     *
     * @return {@code true} in Build Mode, {@code false} in Simulation Mode
     */
    public boolean isBuildMode() {
        return modeToggle.isSelected();
    }

    /**
     * Switches to the other mode, exactly as if the user had clicked the mode button.
     */
    public void toggleMode() {
        modeToggle.setSelected(!modeToggle.isSelected());
        setBuildMode(modeToggle.isSelected());
    }

    /**
     * Switches the side bar between Simulation Mode and Build Mode, stopping any route editing when
     * returning to Simulation Mode, and tells the program about the change.
     *
     * @param buildMode {@code true} to show Build Mode controls, {@code false} for Simulation Mode
     */
    private void setBuildMode(boolean buildMode) {
        showSection(buildMode);

        if (!buildMode) {
            buildModePanel.cancelRouteBuilding();
        }

        onModeChanged.accept(buildMode);
    }

    /**
     * Shows exactly one of the two sections.
     *
     * @param buildMode {@code true} to show the Build Mode section
     */
    private void showSection(boolean buildMode) {
        simulationBox.setVisible(!buildMode);
        simulationBox.setManaged(!buildMode);
        buildBox.setVisible(buildMode);
        buildBox.setManaged(buildMode);
    }
}
