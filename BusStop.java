
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
    * @version v1.0 11/17/26

    
**/

public class BusStop {
    private int waitingPassengers;
    private long lastRecordedVisit; // UPDATED
    private Location busStopLocation;

    BusStop(Location location){
        busStopLocation = location;
    }
    BusStop(double x, double y){
        busStopLocation = new Location(x, y);
        
    }

    public void updatePassengers(int Passengers){
        waitingPassengers = Passengers;

    }

    public void recordVisit(){
       lastRecordedVisit = System.currentTimeMillis();
    }

    // Getters
    public int getPassenger(){return waitingPassengers;}
    public long getLastVisit(){return lastRecordedVisit;} // Might be changed
    public Location getLocation(){return busStopLocation;}



}
