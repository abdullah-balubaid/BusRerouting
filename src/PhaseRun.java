import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> PhaseRun</p>
 * <p><b>Purpose:</b> One run of one phase on a {@link BusNetwork}: Phase 1 (buses keep their fixed
 * cycle) or Phase 2 (the {@link Dispatcher} may reroute buses). It owns the simulated clock, advances
 * the network in small fixed steps (so the result never depends on the speed or the frame rate),
 * writes down how the run is going for the chart, and holds the result once the run length is reached.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class PhaseRun {
    /** Phase number of the fixed cycle. */
    public static final int FIXED_CYCLE = 1;
    /** Phase number of the run with service-first rerouting. */
    public static final int SERVICE_FIRST = 2;
    /** Phase number of the run with cost-saving rerouting. */
    public static final int COST_SAVING = 3;

    private static final double STEP_SECONDS = 0.05;                // Fixed slice of simulated time every update advances by
    private static final int SAMPLES_PER_RUN = 60;                   // Chart points per run, whatever its length

    private final BusNetwork network;
    private final int phaseNumber;
    private final double totalMinutes;            // Length of the run in simulated minutes
    private final double acceptableWaitMinutes;   // The wait after which a stop counts as overdue (and a passenger stops being fully satisfied)
    private final RerouteMethod method;           // How reroutes are decided, or null in the fixed cycle
    private final PhaseTimeline timeline = new PhaseTimeline();
    private final double sampleIntervalMinutes;   // Simulated minutes between chart points
    private double nextSampleMinutes;
    private double simulatedMinutes = 0;
    private double pendingSimulatedSeconds = 0;   // Time handed in but not yet used (less than one step)
    private PhaseResult result = null;            // Set when the run is finished

    /**
     * Starts a run: the network is put back to its starting state and the first chart point is written.
     *
     * @param network             the network to run on
     * @param phaseNumber         {@link #FIXED_CYCLE}, {@link #SERVICE_FIRST} or {@link #COST_SAVING}
     * @param totalMinutes        length of the run in simulated minutes
     * @param acceptableWaitMinutes the wait after which a stop counts as overdue
     */
    public PhaseRun(BusNetwork network, int phaseNumber, double totalMinutes, double acceptableWaitMinutes) {
        this.network = network;
        this.phaseNumber = phaseNumber;
        this.totalMinutes = totalMinutes;
        this.acceptableWaitMinutes = acceptableWaitMinutes;
        this.method = methodOf(phaseNumber);
        this.sampleIntervalMinutes = totalMinutes / SAMPLES_PER_RUN;
        this.nextSampleMinutes = sampleIntervalMinutes;

        network.restoreStartingState();
        network.setMinWaitingToDepart(method == null ? 0 : method.getMinWaitingToDepart());
        recordSample();
    }

    /**
     * Names a phase the way it is shown on screen.
     *
     * @param phaseNumber {@link #FIXED_CYCLE}, {@link #SERVICE_FIRST} or {@link #COST_SAVING}
     * @return text such as {@code "Phase 2: service-first"}
     */
    public static String nameOf(int phaseNumber) {
        return phaseNumber == FIXED_CYCLE ? "Phase 1: fixed cycle"
                : "Phase 2: " + methodOf(phaseNumber).getDisplayName().toLowerCase();
    }

    /**
     * Finds the reroute method a phase uses.
     *
     * @param phaseNumber {@link #FIXED_CYCLE}, {@link #SERVICE_FIRST} or {@link #COST_SAVING}
     * @return the method, or {@code null} for the fixed cycle
     */
    private static RerouteMethod methodOf(int phaseNumber) {
        return phaseNumber == SERVICE_FIRST ? RerouteMethod.SERVICE_FIRST
                : phaseNumber == COST_SAVING ? RerouteMethod.COST_SAVING : null;
    }

    /**
     * Returns which phase this run is.
     *
     * @return {@link #FIXED_CYCLE}, {@link #SERVICE_FIRST} or {@link #COST_SAVING}
     */
    public int getPhaseNumber() {
        return phaseNumber;
    }

    /**
     * Returns the simulated time that has passed.
     *
     * @return simulated minutes
     */
    public double getSimulatedMinutes() {
        return simulatedMinutes;
    }

    /**
     * Returns the length of the run.
     *
     * @return simulated minutes
     */
    public double getTotalMinutes() {
        return totalMinutes;
    }

    /**
     * Returns how far the run has got.
     *
     * @return a fraction from 0 to 1
     */
    public double getProgress() {
        return simulatedMinutes / totalMinutes;
    }

    /**
     * Tells whether the run has reached its length.
     *
     * @return {@code true} once finished
     */
    public boolean isFinished() {
        return result != null;
    }

    /**
     * Returns how the network stands right now (the final result once the run is finished).
     *
     * @return the current average wait, cost, passengers served and satisfaction
     */
    public PhaseResult getCurrentResult() {
        if (result != null) {
            return result;   // finished: the network may have moved on to another run since
        }
        return network.getCurrentResult(simulatedMinutes * 60.0, acceptableWaitMinutes);
    }

    /**
     * Rates the passengers of every stop right now.
     *
     * @return one set of figures per stop
     */
    public List<StopSatisfaction> getStopSatisfaction() {
        return network.getSatisfaction(simulatedMinutes * 60.0, acceptableWaitMinutes);
    }

    /**
     * Returns how far the display is through the latest step, so buses can be drawn smoothly between
     * steps.
     *
     * @return a fraction from 0 to 1
     */
    public double getStepFraction() {
        return Math.min(1.0, pendingSimulatedSeconds / STEP_SECONDS);
    }

    /**
     * Returns the numbers written down along the run, for the chart.
     *
     * @return the timeline
     */
    public PhaseTimeline getTimeline() {
        return timeline;
    }

    /**
     * Returns the final result.
     *
     * @return the result, or {@code null} while the run is still going
     */
    public PhaseResult getResult() {
        return result;
    }

    /**
     * Moves the run forward. The time is used in fixed steps, and any part of a step left over is kept
     * for the next call, so the outcome is the same however the time is split up.
     *
     * @param simulatedSeconds simulated seconds to move forward
     */
    public void advance(double simulatedSeconds) {
        pendingSimulatedSeconds += simulatedSeconds;
        while (pendingSimulatedSeconds >= STEP_SECONDS && !isFinished()) {
            step();
            pendingSimulatedSeconds -= STEP_SECONDS;
        }
    }

    /**
     * Runs to the end as fast as possible (used by the batch test).
     *
     * @return the final result
     */
    public PhaseResult runToEnd() {
        while (!isFinished()) {
            step();
        }
        return result;
    }

    /**
     * One fixed step: the network moves, the clock advances, chart points are written, and in Phase 2
     * the dispatcher may reroute a bus. The run ends when its length is reached.
     */
    private void step() {
        network.advance(STEP_SECONDS, simulatedMinutes * 60.0);
        simulatedMinutes += STEP_SECONDS * Scale.SIMULATED_MINUTES_PER_SECOND;

        while (simulatedMinutes >= nextSampleMinutes && nextSampleMinutes < totalMinutes) {
            recordSample();
            nextSampleMinutes += sampleIntervalMinutes;
        }

        if (method != null) {
            network.reroute(simulatedMinutes, acceptableWaitMinutes, method);
        }

        if (simulatedMinutes >= totalMinutes) {
            simulatedMinutes = totalMinutes;
            recordSample();   // the end point of the line on the chart
            result = getCurrentResult();
        }
    }

    /**
     * Writes down how the run is going right now.
     */
    private void recordSample() {
        timeline.record(simulatedMinutes, getCurrentResult());
    }
}
