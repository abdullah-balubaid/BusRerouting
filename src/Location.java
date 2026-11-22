/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Location
    * <p> <b>Purpose:</b> Represents a 2D coordinate point and calculates distance between locations.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/20/26

    
**/

public class Location {
    private double positionX;
    private double positionY;

    Location(double X, double Y){
        positionX = X;
        positionY = Y;
    }
    

    public void updateLocation(double x, double y){
        positionX = x;
        positionY = y;

    }

    // NEW
    public void updateLocation(Location location){ 
        if (location == null) {
            throw new IllegalArgumentException("Location must not be null.");
        }

        positionX = location.getX();
        positionY = location.getY();

    }

    // Getters
    public double getX(){ return positionX; }
    public double getY(){ return positionY; }


    // Distance Calculation
    public double distanceTo(Location location){
        if (location == null){
            throw new IllegalArgumentException("Target location must not be null.");
        }

        double x2 = location.getX();
        double y2 = location.getY();
        double x1 = getX();
        double y1 = getY();

        double distance = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));

        return distance;
    }

    @Override
    public String toString(){
        return "X:" + positionX + " Y:" + positionY;
    }
}
