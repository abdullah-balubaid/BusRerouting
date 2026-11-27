import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> AppMenuBar</p>
 * <p><b>Purpose:</b> The menu bar along the top of the window. File holds Save, Load, Copy and Paste of
 * the build; Help opens the GitHub page. The menu only shows the choices; what they do is
 * {@link BuildSharing} and {@link GitHubDialog}.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class AppMenuBar {
    private final MenuBar bar;   // The menu bar, ready to place at the top of the window

    /**
     * Builds the menu bar.
     *
     * @param sharing      what the File items do
     * @param gitHubDialog what the Help item opens
     */
    public AppMenuBar(BuildSharing sharing, GitHubDialog gitHubDialog) {
        Menu file = new Menu("File");
        file.getItems().addAll(
                item("Save build…  (Ctrl+S)", sharing::saveToFile),
                item("Load build…  (Ctrl+O)", sharing::loadFromFile),
                item("Copy build to clipboard  (Ctrl+C)", sharing::copyToClipboard),
                item("Paste build from clipboard  (Ctrl+V)", sharing::pasteFromClipboard));

        Menu help = new Menu("Help");
        MenuItem gitHub = new MenuItem("GitHub page…");
        gitHub.setOnAction(e -> gitHubDialog.show());
        help.getItems().add(gitHub);

        bar = new MenuBar(file, help);
    }

    /**
     * Returns the menu bar to place in the window.
     *
     * @return the menu bar
     */
    public MenuBar getView() {
        return bar;
    }

    /**
     * Makes a menu item. Its shortcut is written in the text only; the shortcut itself is handled by
     * {@link KeyboardShortcuts}, so Ctrl+C and Ctrl+V still copy and paste text while typing in a box.
     *
     * @param text   the item's text, including the shortcut
     * @param action what the item does
     * @return the item
     */
    private MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }
}
