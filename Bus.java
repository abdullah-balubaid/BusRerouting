import java.awt.Color;
import java.util.ArrayList;
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Bus
    * <p> <b>Purpose:</b> .
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.0 11/16/26

    
**/


public class Bus extends Vehicle {
    protected ArrayList<Location> route = new ArrayList<Location>();

    private Location lastVisitedStop; // NEW

    Bus(int Capacity, int Speed, Color color, ArrayList<Location> route){
        super(Capacity, Speed, color);
    }

    public ArrayList<Location> getRoute() {
        return route;
    }

    public Location getLastVisitedStop(){return lastVisitedStop;} // NEW


    
}