import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BusesAndLogTab</p>
 * <p><b>Purpose:</b> The Buses &amp; Log tab: what every bus is doing, and the event log, whose lines are coloured
 * by what kind of event they describe (see {@link LogKind}).</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class BusesAndLogTab {
    private static final int MAX_LOG_LINES = 2000;   // Oldest lines are dropped beyond this

    /**
     * One line of the event log: its text and what kind of line it is.
     */
    private static class LogLine {
        private final LogKind kind;
        private final String text;

        private LogLine(LogKind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private final Tab tab = new Tab("Buses & Log");
    private final BusNetwork network;                   // Where bus descriptions come from
    private final ListView<Bus> busStatusList = new ListView<>();
    private final ObservableList<LogLine> logLines = FXCollections.observableArrayList();
    private final ListView<LogLine> logView = new ListView<>(logLines);
    private boolean scrollQueued = false;                // True while a scroll-to-newest-line is already waiting

    /**
     * Builds the tab.
     *
     * @param network the network whose buses are listed
     */
    public BusesAndLogTab(BusNetwork network) {
        this.network = network;

        busStatusList.setPrefHeight(120);
        busStatusList.setMinHeight(90);
        busStatusList.setCellFactory(list -> new ListCell<Bus>() {
            @Override
            protected void updateItem(Bus bus, boolean empty) {
                super.updateItem(bus, empty);
                if (empty || bus == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setGraphic(new Circle(6, bus.getColor()));
                setText(network.describe(bus));
                setTextFill(bus.isRerouting() ? Palette.REROUTING_TEXT : Palette.ON_ROUTE_TEXT);
            }
        });
        logView.setCellFactory(list -> new ListCell<LogLine>() {
            {
                setWrapText(true);
                prefWidthProperty().bind(list.widthProperty().subtract(24));
            }

            @Override
            protected void updateItem(LogLine line, boolean empty) {
                super.updateItem(line, empty);
                if (empty || line == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(line.text);
                setTextFill(line.kind.getColor());
                setStyle("-fx-font-size: 11; -fx-padding: 1 4 1 4;" + (line.kind.isBold() ? " -fx-font-weight: bold;" : ""));
            }
        });
        VBox.setVgrow(logView, Priority.ALWAYS);

        VBox box = new VBox(8, new Label("Bus status:"), busStatusList, new Label("Event log:"), logView);
        box.setPadding(new Insets(10));
        tab.setContent(box);
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
     * Refreshes the list of what every bus is doing.
     */
    public void refresh() {
        busStatusList.setItems(FXCollections.observableArrayList(network.getBuses()));
    }

    /**
     * Appends a line to the event log, in the colour of its kind. A message with several lines is split
     * into several log lines of the same kind.
     *
     * @param kind    what the message is about (decides its colour)
     * @param message the message to log
     */
    public void log(LogKind kind, String message) {
        for (String text : message.split("\n")) {
            logLines.add(new LogLine(kind, text));
        }
        if (logLines.size() > MAX_LOG_LINES) {
            logLines.remove(0, logLines.size() - MAX_LOG_LINES);
        }

        // Scroll down once the list has laid out the new line; scrolling right away is too early
        if (!scrollQueued) {
            scrollQueued = true;
            Platform.runLater(() -> {
                logView.scrollTo(logLines.size() - 1);
                scrollQueued = false;
            });
        }
    }
}
