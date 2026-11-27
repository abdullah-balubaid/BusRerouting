import javafx.collections.FXCollections;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> ResultsChart</p>
 * <p><b>Purpose:</b> A line chart of the three phases (fixed cycle, service-first and cost-saving rerouting)
 * over simulated time, with a drop-down to show the average wait, the satisfaction, the distance driven or the driving cost.
 * Phase 1 stays on the chart as the reference line while the others grow beside it.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v2.0 11/26/26
 */
public class ResultsChart {
    private static final String WAIT_CHOICE = "Average wait (min)";   // Drop-down choices
    private static final String STARS_CHOICE = "Satisfaction (stars)";
    private static final String DISTANCE_CHOICE = "Distance driven (km)";
    private static final String COST_CHOICE = "Driving cost (" + Scale.CURRENCY + ")";

    private final VBox view;                                       // The drop-down and chart, ready to place in the side bar
    private final ComboBox<String> metricBox;                      // Chooses what the chart shows
    private final NumberAxis xAxis;                                // Time axis, resized to the run length
    private final NumberAxis yAxis;                                // Vertical axis, relabelled with the choice
    private final List<XYChart.Series<Number, Number>> series = new ArrayList<>();   // One line per phase
    private final int[] drawnSamples = {-1, -1, -1};               // What is currently drawn, so the chart is
    private String drawnMetric = "";                               // only rebuilt when something changed

    /**
     * Builds the chart.
     *
     * @param totalMinutes length of a full run in simulated minutes (the width of the time axis)
     */
    public ResultsChart(double totalMinutes) {
        xAxis = new NumberAxis(0, totalMinutes, totalMinutes / 5);
        xAxis.setLabel("Simulated minutes");
        yAxis = new NumberAxis();
        yAxis.setLabel(WAIT_CHOICE);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(230);
        chart.setMinHeight(180);

        String[] names = {PhaseRun.nameOf(PhaseRun.FIXED_CYCLE), PhaseRun.nameOf(PhaseRun.SERVICE_FIRST),
                PhaseRun.nameOf(PhaseRun.COST_SAVING)};
        for (String name : names) {
            XYChart.Series<Number, Number> line = new XYChart.Series<>();
            line.setName(name);
            series.add(line);
            chart.getData().add(line);
        }

        metricBox = new ComboBox<>(FXCollections.observableArrayList(WAIT_CHOICE, STARS_CHOICE, DISTANCE_CHOICE, COST_CHOICE));
        metricBox.getSelectionModel().selectFirst();
        metricBox.setMaxWidth(Double.MAX_VALUE);

        view = new VBox(6, metricBox, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    /**
     * Returns the chart section to place in the side bar.
     *
     * @return the section's root node
     */
    public VBox getView() {
        return view;
    }

    /**
     * Makes the time axis as long as the run.
     *
     * @param totalMinutes length of the run in simulated minutes
     */
    public void setTotalMinutes(double totalMinutes) {
        if (xAxis.getUpperBound() != totalMinutes) {
            xAxis.setUpperBound(totalMinutes);
            xAxis.setTickUnit(totalMinutes / 5);
        }
    }

    /**
     * Redraws the lines if the timelines or the drop-down choice have changed since the last call.
     *
     * @param timelines the timelines of Phase 1, service-first and cost-saving, in that order
     */
    public void refresh(PhaseTimeline[] timelines) {
        String metric = metricBox.getValue();
        boolean unchanged = metric.equals(drawnMetric);
        for (int i = 0; i < timelines.length; i++) {
            unchanged = unchanged && timelines[i].size() == drawnSamples[i];
        }
        if (unchanged) {
            return;
        }

        yAxis.setLabel(metric);
        for (int i = 0; i < timelines.length; i++) {
            fill(series.get(i), timelines[i], metric);
            drawnSamples[i] = timelines[i].size();
        }
        drawnMetric = metric;
    }

    /**
     * Replaces the points of one line with the snapshots of a timeline. Snapshots with no average wait
     * yet (nobody had boarded) are left out of the wait line.
     *
     * @param series   the line to fill
     * @param timeline the snapshots to draw
     * @param metric   which drop-down choice to draw
     */
    private void fill(XYChart.Series<Number, Number> series, PhaseTimeline timeline, String metric) {
        series.getData().clear();
        for (int i = 0; i < timeline.size(); i++) {
            PhaseResult result = timeline.getResult(i);
            double value = metric.equals(WAIT_CHOICE) ? result.getAverageWaitMinutes()
                    : metric.equals(STARS_CHOICE) ? result.getAverageStars()
                    : metric.equals(DISTANCE_CHOICE) ? result.getTotalDistanceKm() : result.getTotalCost();
            if (!Double.isNaN(value)) {
                series.getData().add(new XYChart.Data<Number, Number>(timeline.getMinute(i), value));
            }
        }
    }
}
