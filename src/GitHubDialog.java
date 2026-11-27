import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> GitHubDialog</p>
 * <p><b>Purpose:</b> The small window opened from Help: a QR code that leads to the project's GitHub
 * page, and the same address as a link that opens in the browser. The QR code is stored as rows of
 * 0 and 1 and drawn as squares, so no picture file is needed.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class GitHubDialog {
    /** The project's page. */
    public static final String URL = "https://github.com/abdullah-balubaid/BusRerouting";

    private static final int QUIET_ZONE = 4;     // Empty squares kept around the code, as the QR standard asks
    private static final double SQUARE = 7;      // Size of one QR square in pixels

    // The QR code of URL: one row of text per row of squares, "1" is a dark square
    private static final String[] QR_ROWS = {
            "111111100001111101110011001111111",
            "100000100110111110100101101000001",
            "101110100101110110110001001011101",
            "101110100001011111111011101011101",
            "101110100010111000111011001011101",
            "100000101100111110110000101000001",
            "111111101010101010101010101111111",
            "000000000000001111000110100000000",
            "100101101101011010000110010100000",
            "010011000011010011101100111100011",
            "011010100000111010110010101110101",
            "111100000000010110111100001011010",
            "011011100100001110101101111101001",
            "100101010001101101101010111011011",
            "011001100100101111010101111000110",
            "000100000001000011101100001100010",
            "000110111010011101001110101010010",
            "110111000111110001000001110101010",
            "011101110001010011000010101000111",
            "001110001001010110011101111100010",
            "111010100001101101010101010010010",
            "000000011000001100010100100001111",
            "111011100110101010011100001111101",
            "011001010011111100011110100011001",
            "101011110110111011001100111110001",
            "000000001000010001010010100011011",
            "111111100101110001101100101011110",
            "100000101111111101111100100010010",
            "101110100101010000101110111111000",
            "101110101110001111111001001011000",
            "101110100010100110000100000010101",
            "100000100110100001011101100001000",
            "111111101101001110000100101011010"
    };

    private final Stage stage;   // The window

    /**
     * Builds the window (it is not shown until {@link #show()}).
     *
     * @param owner        the main window, which this one stays on top of
     * @param hostServices opens the link in the user's browser
     */
    public GitHubDialog(Stage owner, HostServices hostServices) {
        Label heading = new Label("Source code and documentation");
        heading.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label hint = new Label("Scan the code with a phone, or open the link:");

        Hyperlink link = new Hyperlink(URL);
        link.setOnAction(e -> hostServices.showDocument(URL));

        VBox content = new VBox(10, heading, hint, drawQr(), link);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(18));

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Project on GitHub");
        stage.setResizable(false);
        stage.setScene(new Scene(content));
    }

    /**
     * Shows the window.
     */
    public void show() {
        stage.show();
    }

    /**
     * Draws the QR code: dark squares on a white background with an empty border around them.
     *
     * @return the canvas holding the code
     */
    private Canvas drawQr() {
        int squares = QR_ROWS.length + 2 * QUIET_ZONE;
        Canvas canvas = new Canvas(squares * SQUARE, squares * SQUARE);
        GraphicsContext brush = canvas.getGraphicsContext2D();

        brush.setFill(Color.WHITE);
        brush.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        brush.setFill(Color.BLACK);
        for (int row = 0; row < QR_ROWS.length; row++) {
            for (int column = 0; column < QR_ROWS[row].length(); column++) {
                if (QR_ROWS[row].charAt(column) == '1') {
                    brush.fillRect((column + QUIET_ZONE) * SQUARE, (row + QUIET_ZONE) * SQUARE, SQUARE, SQUARE);
                }
            }
        }
        return canvas;
    }
}
