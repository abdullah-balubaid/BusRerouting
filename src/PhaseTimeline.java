import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> PhaseTimeline</p>
 * <p><b>Purpose:</b> Remembers how one phase went over time: every few simulated minutes the average wait,
 * the distance driven and driving cost so far and the passengers served are written down. The {@link ResultsChart} draws
 * them as lines, and the live "Phase 2 compared with Phase 1 at this time" figures look up what
 * Phase 1 had reached at the same minute.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class PhaseTimeline {
    /**
     * One snapshot of a phase at a moment in time.
     */
    private static final class Sample {
        private final double minute;     // Simulated minute of the snapshot
        private final PhaseResult result;   // Average wait, distance and passengers served so far

        /**
         * Creates a snapshot.
         *
         * @param minute simulated minute of the snapshot
         * @param result figures reached by then
         */
        private Sample(double minute, PhaseResult result) {
            this.minute = minute;
            this.result = result;
        }
    }

    private final List<Sample> samples = new ArrayList<>();   // In time order

    /**
     * Writes down how a run stands at a moment.
     *
     * @param minute the simulated minute
     * @param result the figures at that minute
     */
    public void record(double minute, PhaseResult result) {
        samples.add(new Sample(minute, result));
    }

    /**
     * Forgets every snapshot.
     */
    public void clear() {
        samples.clear();
    }

    /**
     * Returns how many snapshots there are.
     *
     * @return the number of snapshots
     */
    public int size() {
        return samples.size();
    }

    /**
     * Returns the simulated minute of a snapshot.
     *
     * @param index 0-based snapshot number
     * @return the minute
     */
    public double getMinute(int index) {
        return samples.get(index).minute;
    }

    /**
     * Returns the figures of a snapshot.
     *
     * @param index 0-based snapshot number
     * @return average wait, distance and passengers served at that time
     */
    public PhaseResult getResult(int index) {
        return samples.get(index).result;
    }

    /**
     * Finds what this phase had reached by a given minute. Between two snapshots the figures are
     * worked out proportionally, so the answer is not out of date by up to a whole sampling interval.
     *
     * @param minute simulated minute to look up
     * @return the figures at that time, or {@code null} if there is no snapshot that early
     */
    public PhaseResult resultAt(double minute) {
        Sample before = null;
        Sample after = null;
        for (Sample sample : samples) {
            if (sample.minute <= minute) {
                before = sample;
            } else {
                after = sample;
                break;
            }
        }

        if (before == null) {
            return null;
        }
        if (after == null || after.minute <= before.minute) {
            return before.result;
        }

        double fraction = (minute - before.minute) / (after.minute - before.minute);
        PhaseResult from = before.result;
        PhaseResult to = after.result;
        int served = (int) Math.round(blend(from.getPassengersServed(), to.getPassengersServed(), fraction));
        return new PhaseResult(blend(from.getAverageWaitMinutes(), to.getAverageWaitMinutes(), fraction),
                blend(from.getTotalDistanceKm(), to.getTotalDistanceKm(), fraction),
                blend(from.getTotalCost(), to.getTotalCost(), fraction), served,
                blend(from.getAverageStars(), to.getAverageStars(), fraction));
    }

    /**
     * Moves part of the way from one figure to the next. A figure that does not exist yet (not a number) stays as it was.
     *
     * @param from     the earlier figure
     * @param to       the later figure
     * @param fraction how far to go, from 0 to 1
     * @return the figure in between
     */
    private static double blend(double from, double to, double fraction) {
        return (Double.isNaN(from) || Double.isNaN(to)) ? from : from + (to - from) * fraction;
    }
}
