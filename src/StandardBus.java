import javafx.scene.paint.Color;
import java.util.ArrayList;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> StandardBus
    * <p> <b>Purpose:</b> Represents a standard 30-passenger bus capable of dynamic rerouting.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.2 11/23/26

    
**/


public class StandardBus extends Bus implements Reroutable {

    public StandardBus(Color color, ArrayList<Location> route){
        super(30, 25, color, route, "S");
    }

    @Override 
    public void reRoute(Location newDestination){
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException("New route must not be null or empty.");
        }
        updateDestination(newDestination);
    }

    @Override
    public String getIconFileName() {
        return "StandardBusIcon.png";
    }
}
