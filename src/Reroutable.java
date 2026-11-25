/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Interface Name:</b> Reroutable</p>
 * <p><b>Purpose:</b> Interface implemented by bus types (such as {@link StandardBus} and
 * {@link MiniBus}) that support dynamic destination rerouting during simulation.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.2 11/25/26
 */
public interface Reroutable {

    /**
     * Redirects the implementing bus to a new target {@link Location}.
     *
     * @param newDestination target grid location to reroute toward
     */
    void reRoute(Location newDestination);
}