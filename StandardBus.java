import java.awt.Color;
import java.util.ArrayList;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> StandardBus
    * <p> <b>Purpose:</b> .
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.0 11/18/26

    
**/


public class StandardBus extends Bus implements Reroutable {
    StandardBus(Color color, ArrayList<Location> route){
        super(30, 30, color, route);
    }

    public void reRoute(){
    }
}
