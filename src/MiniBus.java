import javafx.scene.paint.Color;
import java.util.ArrayList;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> MiniBus
    * <p> <b>Purpose:</b> Represents a compact 15-passenger bus capable of dynamic rerouting.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.2 11/23/26

    
**/

public class MiniBus extends Bus implements Reroutable {

    public MiniBus(Color color, ArrayList<Location> route){
        super(15, 35, color, route, "M");
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
        return "MiniBusIcon.png";
    }


}
