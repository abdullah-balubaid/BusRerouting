import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.Arrays;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> ResultsTab</p>
 * <p><b>Purpose:</b> The Results tab: a table with a column per phase, how much better or worse each rerouting
 * method is than Phase 1 (green or red), and the chart of a chosen measure over time. A running phase shows
 * its figures so far; a finished one shows its final figures.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class ResultsTab {
    // The results table: its rows, and the phase columns
    private static final String[] ROW_NAMES = {"Avg wait (min)", "Satisfaction (★)",
            "Distance driven (km)", "Driving cost (" + Scale.CURRENCY + ")", "Cost per passenger (" + Scale.CURRENCY + ")", "Passengers served"};
    private static final String[] COLUMN_NAMES = {"P1\nFixed Cycle", "P2\nService-first", "P2\nCost-saving"};
    private static final String[] METHOD_NAMES = {RerouteMethod.SERVICE_FIRST.getDisplayName(),
            RerouteMethod.COST_SAVING.getDisplayName()};   // The two rerouting methods
    private static final String[] CHANGE_NAMES = {"Average wait", "Satisfaction", "Distance driven", "Driving cost", "Cost per passenger"};

    private final Tab tab = new Tab("Results");
    private final Label[][] cells = new Label[ROW_NAMES.length][COLUMN_NAMES.length];
    private final Label compareCaptionLabel = new Label();
    private final Label[][] changeCells = new Label[CHANGE_NAMES.length][2];   // [measure][method]
    private final ResultsChart resultsChart;
    private final PhaseTimeline noTimeline = new PhaseTimeline();   // Stands in for a phase that has not run

    /**
     * Builds the tab.
     *
     * @param totalMinutes length of a full run in simulated minutes (width of the chart's time axis)
     */
    public ResultsTab(double totalMinutes) {
        compareCaptionLabel.setWrapText(true);
        compareCaptionLabel.setMinHeight(Region.USE_PREF_SIZE);
        compareCaptionLabel.setStyle("-fx-font-weight: bold;");
        resultsChart = new ResultsChart(totalMinutes);
        VBox.setVgrow(resultsChart.getView(), Priority.ALWAYS);

        VBox box = new VBox(10, buildGrid(ROW_NAMES, COLUMN_NAMES, cells, 5, false), compareCaptionLabel,
                buildGrid(CHANGE_NAMES, METHOD_NAMES, changeCells, 4, true), resultsChart.getView());
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
     * Refreshes the results table, the comparison with Phase 1, and the chart.
     *
     * @param runs         the latest run of each phase (index 0 = Phase 1, 1 = service-first, 2 = cost-saving),
     *                     each {@code null} if that phase has not been started
     * @param totalMinutes length of a full run in minutes
     */
    public void refresh(PhaseRun[] runs, double totalMinutes) {
        for (int phase = 0; phase < runs.length; phase++) {
            showColumn(phase, runs[phase] == null ? null : runs[phase].getCurrentResult());
        }

        // The comparison needs Phase 1 to be finished. A rerouting phase that is still running is
        // compared with what Phase 1 had reached at the same minute.
        PhaseRun phase1 = runs[0];
        boolean anyLive = false;
        for (int method = 0; method < 2; method++) {
            PhaseRun run = runs[method + 1];
            PhaseResult from = null;
            PhaseResult to = null;
            if (run != null && phase1 != null && phase1.isFinished()) {
                to = run.getCurrentResult();
                from = run.isFinished() ? phase1.getResult() : phase1.getTimeline().resultAt(run.getSimulatedMinutes());
                anyLive = anyLive || !run.isFinished();
            }
            showChange(method, from, to);
        }
        compareCaptionLabel.setText(anyLive ? "Phase 2 Compared with Phase 1 at the same minute (live):"
                : "Phase 2 Compared with Phase 1:");

        resultsChart.setTotalMinutes(totalMinutes);
        PhaseTimeline[] timelines = new PhaseTimeline[runs.length];
        for (int phase = 0; phase < runs.length; phase++) {
            timelines[phase] = (runs[phase] != null) ? runs[phase].getTimeline() : noTimeline;
        }
        resultsChart.refresh(timelines);
    }

    /**
     * Formats a number for a table.
     *
     * @param value  the number
     * @param format the {@code printf} format to use
     * @return {@code "-"} if the number is {@link Double#NaN}, otherwise the formatted number
     */
    public static String formatNumber(double value, String format) {
        return Double.isNaN(value) ? "-" : String.format(format, value);
    }

     /**
     * Formats a number for a table.
     *
     * @param value  the number
     * @param format the {@code printf} format to use
     * @return {@code "-"} if the number is {@link value < 0}, otherwise the formatted number
     */

    public static String formatNumber(int value, String format) {
        return value < 0 ? "-" : String.format(format, value);
    }

    /**
     * Builds a grid of text cells: a heading for each column, a name for each row and a "-" in every cell. The
     * row names take the room they need and the columns share the rest. Used for both result tables.
     *
     * @param rowNames    the name of each row
     * @param columnNames the heading of each column
     * @param cells       the cells to create, indexed [row][column]
     * @param rowGap      pixels between the rows
     * @param boldCells   {@code true} to show the cells in bold
     * @return the grid
     */
    private GridPane buildGrid(String[] rowNames, String[] columnNames, Label[][] cells, double rowGap, boolean boldCells) {
        GridPane table = new GridPane();
        table.setHgap(12);
        table.setVgap(rowGap);

        ColumnConstraints nameColumn = new ColumnConstraints();
        nameColumn.setMinWidth(Region.USE_PREF_SIZE);
        table.getColumnConstraints().add(nameColumn);
        for (int column = 0; column < columnNames.length; column++) {
            ColumnConstraints valueColumn = new ColumnConstraints();
            valueColumn.setMinWidth(Region.USE_PREF_SIZE);
            valueColumn.setHgrow(Priority.ALWAYS);
            table.getColumnConstraints().add(valueColumn);

            Label heading = new Label(columnNames[column]);
            heading.setStyle("-fx-font-weight: bold;");
            table.add(heading, column + 1, 0);
        }

        for (int row = 0; row < rowNames.length; row++) {
            table.add(new Label(rowNames[row]), 0, row + 1);
            for (int column = 0; column < columnNames.length; column++) {
                cells[row][column] = new Label("-");
                if (boldCells) {
                    cells[row][column].setStyle("-fx-font-weight: bold;");
                }
                table.add(cells[row][column], column + 1, row + 1);
            }
        }
        return table;
    }

    /**
     * Fills one column of the results table.
     *
     * @param column 0 for Phase 1, 1 for service-first, 2 for cost-saving
     * @param result the figures, or {@code null} to show dashes
     */
    private void showColumn(int column, PhaseResult result) {
        cells[0][column].setText(result == null ? "-" : formatNumber(result.getAverageWaitMinutes(), "%.2f"));
        cells[1][column].setText(result == null ? "-" : formatNumber(result.getAverageStars(), "%.2f"));
        cells[2][column].setText(result == null ? "-" : formatNumber(result.getTotalDistanceKm(), "%.1f"));
        cells[3][column].setText(result == null ? "-" : formatNumber(result.getTotalCost(), "%.0f"));
        cells[4][column].setText(result == null ? "-" : formatNumber(result.getCostPerPassenger(), "%.2f"));
        cells[5][column].setText(result == null ? "-" : formatNumber(result.getPassengersServed(), "%d"));
    }

    /**
     * Fills one column of the comparison with Phase 1.
     *
     * @param method 0 for service-first, 1 for cost-saving
     * @param from   Phase 1's figures to compare with, or {@code null} to show dashes
     * @param to     the rerouting phase's figures
     */
    private void showChange(int method, PhaseResult from, PhaseResult to) {
        double[] improvements = new double[CHANGE_NAMES.length];
        if (from != null && to != null) {
            improvements = to.improvementsOver(from);
        } else {
            Arrays.fill(improvements, Double.NaN);
        }

        for (int measure = 0; measure < improvements.length; measure++) {
            if (measure == PhaseResult.STARS_MEASURE) {
                showStars(changeCells[measure][method], improvements[measure]);
            } else {
                showPercent(changeCells[measure][method], improvements[measure]);
            }
        }
    }

    /**
     * Shows a percentage change: the size of it, in green if better and red if worse.
     *
     * @param label              the table cell
     * @param improvementPercent percentage improvement; positive means better, negative means worse
     */
    private void showPercent(Label label, double improvementPercent) {
        if (Double.isNaN(improvementPercent)) {
            label.setText("-");
            label.setTextFill(Palette.NEUTRAL_RESULT_TEXT);
            return;
        }
        boolean better = improvementPercent >= 0;
        label.setText(String.format("%.1f%% %s", Math.abs(improvementPercent), better ? "better" : "worse"));
        label.setTextFill(better ? Palette.GOOD_RESULT_TEXT : Palette.BAD_RESULT_TEXT);
    }

    /**
     * Shows a change in stars, in green if higher and red if lower.
     *
     * @param label the table cell
     * @param stars the change in stars (positive means happier)
     */
    private void showStars(Label label, double stars) {
        if (Double.isNaN(stars)) {
            label.setText("-");
            label.setTextFill(Palette.NEUTRAL_RESULT_TEXT);
            return;
        }
        label.setText(String.format("%+.2f ★", stars));
        label.setTextFill(stars >= 0 ? Palette.GOOD_RESULT_TEXT : Palette.BAD_RESULT_TEXT);
    }
}
