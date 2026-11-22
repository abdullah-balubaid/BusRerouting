
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> BusStop
    * <p> <b>Purpose:</b> Represents a bus stop with a 2D location, waiting passenger count, and last visit timestamp.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/20/26

    
**/

public class BusStop {
    private int waitingPassengers;
    private long lastRecordedVisit; 
    private Location busStopLocation;

    public BusStop(Location location){
        if (location != null){
        busStopLocation = new Location(location.getX(), location.getY());
        }
        else{
            throw new NullPointerException("Location must not be null.");
        }
    }

    BusStop(double x, double y){
        busStopLocation = new Location(x, y);
        
    }

    public void updatePassengers(int Passengers){
        if (Passengers < 0){
            throw new IllegalArgumentException("Passengers cannot be negative.");
        }

        waitingPassengers = Passengers;

    }

    public void recordVisit(){
       lastRecordedVisit = System.currentTimeMillis();
    }

    

    // Getters
    public int getPassenger(){ return waitingPassengers; }
    
    public long getLastVisit(){ return lastRecordedVisit; } 
    
    public Location getLocation(){
        return new Location(busStopLocation.getX(), busStopLocation.getY());
    }


    @Override 
    public String toString(){
        return "BusStop[" + busStopLocation.getX() + ", " + busStopLocation.getY()
                + ", waiting=" + waitingPassengers + "]";
    }



}
