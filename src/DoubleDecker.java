import javafx.scene.paint.Color;
import java.util.ArrayList;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> DoubleDecker
    * <p> <b>Purpose:</b> .
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/23/26

    
**/

public class DoubleDecker extends Bus {
    public DoubleDecker(Color color, ArrayList<Location> route){
        super(50, 15, color, route, "DD");
    }

    @Override
    public String getIconFileName() {
        return "DoubleDeckerIcon.png";
    }
}
