import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> KeyboardShortcuts</p>
 * <p><b>Purpose:</b> Listens for key presses anywhere in the window and turns them into the same actions
 * as the buttons. Single keys (Space, 1, 2, 3, N, Esc) are ignored while the user is typing in a text
 * box, and Ctrl+C / Ctrl+V are left to the text box so normal copy and paste keep working there.</p>
 *
 * <p>Shortcuts: Space pause/resume, 1 / 2 / 3 run Phase 1 / Phase 2 service-first / Phase 2 cost-saving, N new random map, Ctrl+B switch between
 * Simulation and Build Mode, Esc cancel the active build tool, Ctrl+S save, Ctrl+O load, Ctrl+C copy
 * build, Ctrl+V paste build.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class KeyboardShortcuts {
    /**
     * What the shortcuts ask the rest of the program to do.
     */
    public interface Actions {
        /**
         * Tells whether Build Mode is showing (the simulation keys are off then).
         *
         * @return {@code true} in Build Mode
         */
        boolean isBuildMode();

        /** Pauses or resumes the running phase. */
        void togglePause();

        /**
         * Starts a phase.
         *
         * @param phaseNumber 1, 2 or 3
         */
        void startPhase(int phaseNumber);

        /** Generates a new random map. */
        void newRandomMap();

        /** Switches between Simulation Mode and Build Mode. */
        void toggleMode();

        /** Cancels the active build tool. */
        void cancelTool();

        /** Saves the build to a file. */
        void saveBuild();

        /** Loads a build from a file. */
        void loadBuild();

        /** Copies the build to the clipboard. */
        void copyBuild();

        /** Pastes a build from the clipboard. */
        void pasteBuild();
    }

    private final Scene scene;       // The window's scene, asked which control has the keyboard focus
    private final Actions actions;   // What the shortcuts do

    /**
     * Starts listening for shortcuts in a scene. The listener runs before any button sees the key, so
     * Space pauses instead of "clicking" whichever button happens to have focus.
     *
     * @param scene   the window's scene
     * @param actions what the shortcuts do
     */
    public KeyboardShortcuts(Scene scene, Actions actions) {
        this.scene = scene;
        this.actions = actions;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handle);
    }

    /**
     * Works out which shortcut (if any) a key press is, and runs it.
     *
     * @param event the key press
     */
    private void handle(KeyEvent event) {
        // A spinner or drop-down holds the focus while its text is edited, so digits typed there are not shortcuts
        Node focused = scene.getFocusOwner();
        boolean typing = focused instanceof TextInputControl || focused instanceof Spinner || focused instanceof ComboBoxBase;
        KeyCode key = event.getCode();

        if (event.isShortcutDown()) {
            if (key == KeyCode.B) {
                actions.toggleMode();
            } else if (key == KeyCode.S) {
                actions.saveBuild();
            } else if (key == KeyCode.O) {
                actions.loadBuild();
            } else if (key == KeyCode.C && !typing) {
                actions.copyBuild();
            } else if (key == KeyCode.V && !typing) {
                actions.pasteBuild();
            } else {
                return;   // not ours (e.g. Ctrl+C while typing): leave it alone
            }
            event.consume();
            return;
        }

        if (typing || event.isAltDown() || event.isMetaDown()) {
            return;
        }

        boolean buildMode = actions.isBuildMode();
        boolean onSlider = focused instanceof Slider;   // the speed slider: digits and N are not shortcuts there
        if (key == KeyCode.ESCAPE && buildMode) {
            actions.cancelTool();
        } else if (key == KeyCode.SPACE && !buildMode) {
            actions.togglePause();
        } else if ((key == KeyCode.DIGIT1 || key == KeyCode.NUMPAD1) && !buildMode && !onSlider) {
            actions.startPhase(PhaseRun.FIXED_CYCLE);
        } else if ((key == KeyCode.DIGIT2 || key == KeyCode.NUMPAD2) && !buildMode && !onSlider) {
            actions.startPhase(PhaseRun.SERVICE_FIRST);
        } else if ((key == KeyCode.DIGIT3 || key == KeyCode.NUMPAD3) && !buildMode && !onSlider) {
            actions.startPhase(PhaseRun.COST_SAVING);
        } else if (key == KeyCode.N && !buildMode && !onSlider) {
            actions.newRandomMap();
        } else {
            return;
        }
        event.consume();
    }
}
