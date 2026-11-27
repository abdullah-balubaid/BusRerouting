
import java.util.Random;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BatchTest</p>
 * <p><b>Purpose:</b> Tests rerouting on many random networks instead of one. Every network is run
 * through Phase 1 and both rerouting methods (without drawing anything), and how much each method
 * improved on Phase 1 is averaged over all of them. It runs on its own networks, so the map on screen is never touched.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class BatchTest {
    /** Number of random networks in one test. */
    public static final int NETWORKS = 20;

    private static final String[] METRICS = {"Average wait", "Satisfaction", "Distance driven", "Driving cost", "Cost per passenger"};
    private static final int[] METHOD_PHASES = {PhaseRun.SERVICE_FIRST, PhaseRun.COST_SAVING};
    private static final String[] METHOD_NAMES = {RerouteMethod.SERVICE_FIRST.getDisplayName(),
            RerouteMethod.COST_SAVING.getDisplayName()};

    private final int firstSeed;                // The networks use the seeds firstSeed, firstSeed+1, ...
    private final int stopCount;
    private final double totalMinutes;
    private final double acceptableWaitMinutes;
    // comparisons[method][metric]: how much better than Phase 1 that method was, added up over the networks
    private final Comparison[][] comparisons = new Comparison[METHOD_PHASES.length][METRICS.length];

    /**
     * Sets up a test. The first seed is random, so every test uses different networks; it is kept so
     * a result can be traced back to its networks.
     *
     * @param stopCount             stops on each network
     * @param totalMinutes          length of each run in simulated minutes
     * @param acceptableWaitMinutes the wait after which a stop counts as overdue
     */
    public BatchTest(int stopCount, double totalMinutes, double acceptableWaitMinutes) {
        this(1 + new Random().nextInt(1_000_000), stopCount, totalMinutes, acceptableWaitMinutes);
    }

    /**
     * Sets up a test with a chosen first seed (the same first seed always gives the same result).
     *
     * @param firstSeed             seed of the first network
     * @param stopCount             stops on each network
     * @param totalMinutes          length of each run in simulated minutes
     * @param acceptableWaitMinutes the wait after which a stop counts as overdue
     */
    public BatchTest(int firstSeed, int stopCount, double totalMinutes, double acceptableWaitMinutes) {
        this.firstSeed = firstSeed;
        this.stopCount = stopCount;
        this.totalMinutes = totalMinutes;
        this.acceptableWaitMinutes = acceptableWaitMinutes;
        for (Comparison[] row : comparisons) {
            for (int i = 0; i < row.length; i++) {
                row[i] = new Comparison();
            }
        }
    }

    /**
     * Runs the test: every network goes through Phase 1 and both rerouting methods, and the
     * improvements over Phase 1 are added up.
     */
    public void run() {
        for (int i = 0; i < NETWORKS; i++) {
            BusNetwork network = new BusNetwork();
            network.generate(firstSeed + i, stopCount);

            PhaseResult fixedCycle = new PhaseRun(network, PhaseRun.FIXED_CYCLE, totalMinutes, acceptableWaitMinutes).runToEnd();

            for (int m = 0; m < METHOD_PHASES.length; m++) {
                PhaseResult rerouting = new PhaseRun(network, METHOD_PHASES[m], totalMinutes, acceptableWaitMinutes).runToEnd();
                double[] improvements = rerouting.improvementsOver(fixedCycle);
                for (int metric = 0; metric < improvements.length; metric++) {
                    comparisons[m][metric].add(improvements[metric]);
                }
            }
        }
    }

    /**
     * Describes the test and its outcome, ready to show in the log: one line per measure, with both
     * rerouting methods side by side.
     *
     * @param number which test this is (1 for the first of the session)
     * @return the lines of the summary
     */
    public String describe(int number) {
        StringBuilder text = new StringBuilder(String.format(
                "=== Batch test #%d: %d random maps (seeds %d-%d), %d stops, %d min acceptable wait, %d min runs ===",
                number, NETWORKS, firstSeed, firstSeed + NETWORKS - 1, stopCount, (int) acceptableWaitMinutes, (int) totalMinutes));

        for (int metric = 0; metric < METRICS.length; metric++) {
            text.append("\n").append(METRICS[metric]).append(":");
            for (int m = 0; m < METHOD_PHASES.length; m++) {
                text.append(m == 0 ? "  " : "  |  ").append(METHOD_NAMES[m]).append(" ")
                        .append(comparisons[m][metric].describe(metric == PhaseResult.STARS_MEASURE));
            }
        }
        return text.toString();
    }

    /**
     * Adds up the improvements of one measure over all networks.
     */
    private static class Comparison {
        private double sum = 0;    // Sum of the improvements (percent, or stars for satisfaction)
        private int count = 0;     // Networks the improvement could be worked out for
        private int better = 0;    // Networks where the method did better

        /**
         * Adds one network's improvement.
         *
         * @param improvement percent better (or stars more), or {@link Double#NaN} if it could not be worked out
         */
        void add(double improvement) {
            if (Double.isNaN(improvement)) {
                return;
            }
            sum += improvement;
            count++;
            better += (improvement > 0) ? 1 : 0;
        }

        /**
         * Words the average improvement.
         *
         * @param inStars {@code true} if the improvement is a number of stars, {@code false} if a percentage
         * @return text such as {@code "12.3% better (14/20)"}
         */
        String describe(boolean inStars) {
            if (count == 0) {
                return "no result";
            }
            double average = sum / count;
            String change = inStars ? String.format("%+.2f stars", average)
                    : String.format("%.1f%% %s", Math.abs(average), average >= 0 ? "better" : "worse");
            return change + " (" + better + "/" + count + ")";
        }
    }
}
