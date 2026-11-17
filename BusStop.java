
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> BusStop
    * <p> <b>Purpose:</b> .
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.0 11/16/26

    
**/

public class BusStop {
    private int waitingPassengers;
    private long lastRecordedVisit;
    private Location busStopLocation;

    BusStop(Location location){}

    public int updatePassengers(){return 0;}
    public void recordVisit(){}
    public int getPassenger(){return waitingPassengers;}
    public long getLastVisit(){return lastRecordedVisit;}
    public Location getLocation(){return busStopLocation;}



}
