
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Interface Name:</b> Reroutable
    * <p> <b>Purpose:</b> A base interface that the StandardBus and MiniBus classes implements for re-routing updating.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/20/26

    
**/

public interface Reroutable {

    void reRoute(Location newDestination);
    
}
