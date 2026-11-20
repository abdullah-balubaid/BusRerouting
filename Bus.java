import java.awt.Color;
import java.util.ArrayList;
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Bus
    * <p> <b>Purpose:</b> Abstract bus extending Vehicle with route waypoints and last visited stop tracking.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/20/26

    
**/


public abstract class Bus extends Vehicle {
    protected ArrayList<Location> route = new ArrayList<Location>(); 
    private Location lastVisitedStop; // NEW

    protected Bus(int Capacity, int Speed, Color color, ArrayList<Location> route){
        super(Capacity, Speed, color);

        if (route == null){
            throw new NullPointerException("route must not be null.");
        }
        else if (route.isEmpty()){
            throw new IllegalArgumentException("route must not be empty.");
        }
        this.route = new ArrayList<>(route);
    }

    public ArrayList<Location> getRoute() {
        return new ArrayList<>(route);
    }

    public void setLastVisitedStop(Location stop){
        if (stop == null){
            throw new NullPointerException("Last visited stop must not be null.");
        }
        lastVisitedStop = new Location(stop.getX(), stop.getY());
    }

    public Location getLastVisitedStop(){
        if (lastVisitedStop == null){
            return null;
        }
        return new Location(lastVisitedStop.getX(), lastVisitedStop.getY());
    } // NEW


    
}