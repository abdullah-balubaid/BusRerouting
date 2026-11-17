import java.awt.Color;
import java.util.ArrayList;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> MiniBus
    * <p> <b>Purpose:</b> .
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.0 11/16/26

    
**/

// CHANGE THE CONSTRUCTOR
public class MiniBus extends Bus implements Reroutable {
    MiniBus(int Capacity, int Speed, Color color, ArrayList<Location> route){
        super(Capacity, Speed, color, route);
    }
    
    public void reRoute(){
    }


}
