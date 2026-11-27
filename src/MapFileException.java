
/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> MapFileException</p>
 * <p><b>Purpose:</b> Thrown when saved or pasted build text cannot be turned into a map. The message
 * says which line is wrong and why, so it can be shown to the user as it is.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class MapFileException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for a problem on a specific line.
     *
     * @param lineNumber 1-based number of the offending line
     * @param problem    what is wrong with it
     */
    public MapFileException(int lineNumber, String problem) {
        super("Line " + lineNumber + ": " + problem);
    }

    /**
     * Creates an exception for a problem that belongs to the text as a whole.
     *
     * @param problem what is wrong
     */
    public MapFileException(String problem) {
        super(problem);
    }
}
