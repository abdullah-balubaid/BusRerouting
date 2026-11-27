import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BuildSharing</p>
 * <p><b>Purpose:</b> Moves a build (the stops and bus routes of the {@link BusNetwork}) in and out of
 * the program: save to a {@code .busmap} file, load from one, copy as text to the clipboard, and load
 * from text on the clipboard. It owns the file dialogs, the clipboard and the error messages; the
 * format itself is {@link MapFile}'s job.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class BuildSharing {
    /**
     * What this class tells the rest of the program.
     */
    public interface Host {
        /**
         * Writes a line to the event log.
         *
         * @param kind    what the line is about (decides its colour)
         * @param message the message to log
         */
        void log(LogKind kind, String message);

        /**
         * Called after a build was loaded, so the run can be reset and the screen refreshed.
         *
         * @param source where the build came from (a file name, or "the clipboard")
         */
        void buildLoaded(String source);
    }

    private final Stage owner;           // Window the dialogs belong to
    private final BusNetwork network;    // The network that is saved and loaded
    private final Host host;             // The rest of the program

    /**
     * Creates the sharing helper.
     *
     * @param owner   the main window, which the dialogs belong to
     * @param network the network that is saved and loaded
     * @param host    the rest of the program
     */
    public BuildSharing(Stage owner, BusNetwork network, Host host) {
        this.owner = owner;
        this.network = network;
        this.host = host;
    }

    /**
     * Asks the user for a file name and saves the build there as a {@code .busmap} file.
     */
    public void saveToFile() {
        File file = createFileChooser("Save build").showSaveDialog(owner);
        if (file == null) {
            return;
        }

        if (!file.getName().toLowerCase().endsWith(".busmap")) {
            file = new File(file.getPath() + ".busmap");
        }

        try {
            Files.write(file.toPath(), network.toBuildText().getBytes(StandardCharsets.UTF_8));
            host.log(LogKind.EDIT, "Saved the build to " + file.getName() + ".");
        } catch (IOException e) {
            showError("Could not save the file", e.getMessage());
        }
    }

    /**
     * Asks the user for a {@code .busmap} file and loads the build from it.
     */
    public void loadFromFile() {
        File file = createFileChooser("Load build").showOpenDialog(owner);
        if (file == null) {
            return;
        }

        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            load(text, file.getName());
        } catch (IOException e) {
            showError("Could not read the file", e.getMessage());
        }
    }

    /**
     * Puts the build, as text, on the clipboard so it can be pasted into a message.
     */
    public void copyToClipboard() {
        ClipboardContent content = new ClipboardContent();
        content.putString(network.toBuildText());
        Clipboard.getSystemClipboard().setContent(content);
        host.log(LogKind.EDIT, "Copied the build to the clipboard.");
    }

    /**
     * Loads a build from the text on the clipboard.
     */
    public void pasteFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasString()) {
            showError("Nothing to paste", "The clipboard does not hold any text.");
            return;
        }
        load(clipboard.getString(), "the clipboard");
    }

    /**
     * Checks the text completely and only then replaces the network with it. If the text is wrong the
     * current map is left untouched and the user is told which line to fix.
     *
     * @param text   the build text
     * @param source where the text came from (for the log)
     */
    private void load(String text, String source) {
        try {
            network.loadBuildText(text);
        } catch (MapFileException problem) {
            host.log(LogKind.ERROR, "Could not load the build from " + source + ". " + problem.getMessage());
            showError("Could not load the build", problem.getMessage());
            return;
        }
        host.buildLoaded(source);
    }

    /**
     * Creates a file dialog that shows {@code .busmap} files.
     *
     * @param title the dialog's title
     * @return the dialog
     */
    private FileChooser createFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bus map (*.busmap)", "*.busmap"));
        return chooser;
    }

    /**
     * Shows an error message box.
     *
     * @param title   the heading
     * @param message what went wrong
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("Bus Rerouting");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
