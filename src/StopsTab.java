import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.Collections;
import java.util.function.Function;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> StopsTab</p>
 * <p><b>Purpose:</b> The Stops tab: one row per stop showing how satisfied its passengers are (average stars,
 * share of happy passengers, average wait and people waiting now), so a neglected stop cannot hide behind a busy one.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class StopsTab {
    private final Tab tab = new Tab("Stops");
    private final ObservableList<StopSatisfaction> stopRows = FXCollections.observableArrayList();

    /**
     * Builds the tab.
     */
    public StopsTab() {
        Label hint = new Label("Stars: 5 up to the acceptable wait, falling to 0 at "
                + (int) StopSatisfaction.ZERO_STAR_FACTOR + " times that.");
        hint.setWrapText(true);
        hint.setMinHeight(Region.USE_PREF_SIZE);

        TableView<StopSatisfaction> table = buildTable();
        VBox box = new VBox(8, hint, table);
        box.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
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
     * Refreshes the table.
     *
     * @param active the run that is going or went last, or {@code null} if none has been started
     */
    public void refresh(PhaseRun active) {
        stopRows.setAll(active == null ? Collections.<StopSatisfaction>emptyList() : active.getStopSatisfaction());
    }

    /**
     * Builds the table: one row per stop.
     *
     * @return the table
     */
    @SuppressWarnings("deprecation")   // newer JavaFX renames this policy, but the new names do not exist in older versions
    private TableView<StopSatisfaction> buildTable() {
        TableView<StopSatisfaction> table = new TableView<>(stopRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().add(column("Stop", row -> row.getStopName(), false));
        table.getColumns().add(column("Type", row -> row.isHighPriority() ? "High" : "Low", false));
        table.getColumns().add(column("Avg ★", row -> ResultsTab.formatNumber(row.getAverageStars(), "%.1f"), true));
        table.getColumns().add(column("Happy", row -> Double.isNaN(row.getHappyShare()) ? "-"
                : String.format("%.0f%%", row.getHappyShare() * 100), false));
        table.getColumns().add(column("Avg wait", row -> Double.isNaN(row.getAverageWaitMinutes()) ? "-" : String.format("%.0f min", row.getAverageWaitMinutes()), false));
        table.getColumns().add(column("Waiting", row -> String.valueOf(row.getStillWaiting()), false));
        return table;
    }

    /**
     * Makes one column of the stop table.
     *
     * @param title      the column heading
     * @param text       turns a row into the cell's text
     * @param colourStar {@code true} to colour the text by how happy the stop is
     * @return the column
     */
    private TableColumn<StopSatisfaction, String> column(String title,
            Function<StopSatisfaction, String> text, boolean colourStar) {
        TableColumn<StopSatisfaction, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(text.apply(data.getValue())));
        column.setSortable(false);
        if (colourStar) {
            column.setCellFactory(c -> new TableCell<StopSatisfaction, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    StopSatisfaction row = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                    if (row == null) {
                        setTextFill(Palette.NEUTRAL_RESULT_TEXT);
                    } else if (row.isUnhappy()) {
                        setTextFill(Palette.BAD_RESULT_TEXT);
                    } else if (row.getAverageStars() >= StopSatisfaction.HAPPY_STARS) {
                        setTextFill(Palette.GOOD_RESULT_TEXT);
                    } else {
                        setTextFill(Palette.NEUTRAL_RESULT_TEXT);
                    }
                }
            });
        }
        return column;
    }
}
