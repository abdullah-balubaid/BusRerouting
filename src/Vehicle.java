import javafx.scene.paint.Color;
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Vehicle
    * <p> <b>Purpose:</b> Abstract base class representing a vehicle with capacity, speed, color, passenger count, and movement state.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.1 11/20/26

    
**/

public abstract class Vehicle {
    private final int PASSENGER_CAPACITY; 
    private final int SPEED;  
    private Color color;                                            // Used to determine the color of the vehicle.
    private boolean isMoving = false;                               // Used to check whether the vehicle is moving (T) or not (F).
    private int currentPassengers = 0;                              // Used to determine the current number of passengers in the vehicle.
    private Location currentLocation = new Location(0, 0);     // Used to determine the current location of the vehicle.
    private Location Destination = new Location(0, 0);         // Used to determine the final   location of the vehicle.


        /**
        * Constructor that intiailizes an object of the Vehicle class that takes the details regarding the vehicle status.
        * @param Capacity Used to determine the total number of vehicle's seats.
        * @param Speed Used to determine the speed of the vehicle.
        * Pre-condition: PASSENGER_CAPCITY and SPEED must not hold a value prior creation of the object.
        * Post-condition: The vehicle object contains the major info regarding its status.
        **/

    protected Vehicle(int Capacity , int Speed, Color color){
        if (Capacity <= 0 || Speed <= 0){
            throw new IllegalArgumentException("Capacity and speed must be positive.");
        }

        if (color == null){
            throw new NullPointerException("The chosen color must be not null.");
        }
        
        SPEED = Speed;
        PASSENGER_CAPACITY = Capacity;
        this.color = color;
            
    }

    // Checkers
    public boolean isEmpty(){ return currentPassengers == 0; }
    public boolean isMoving(){ return isMoving; }
    public boolean isFull(){ return currentPassengers == PASSENGER_CAPACITY; }
    

    // Setters
    public void updateLocation(Location newLocation){
        currentLocation.updateLocation(newLocation);
        updateMoving();
    }

    public void updateLocation(double x, double y){
        currentLocation.updateLocation(x, y);
        updateMoving();
    } // NEW

    public void updateDestination(Location newDestination){
        Destination.updateLocation(newDestination);
        updateMoving();
    }

    public void updateDestination(double x, double y){
        Destination.updateLocation(x, y);
        updateMoving();
    } // NEW
    
    public void setCurrentPassengers(int Passengers){ 
        if (Passengers <= PASSENGER_CAPACITY && Passengers >= 0){
        currentPassengers = Passengers;
    }
        else if (Passengers > PASSENGER_CAPACITY) {
            System.out.println("The vehicle is overfilled.");
        }
        else {
            System.out.println("Invalid number of passengers.");
        }
        
    } // NEW

    public void updateMoving(){
        isMoving = currentLocation.getX() != Destination.getX()
                || currentLocation.getY() != Destination.getY();
    } // NEW


    // Getters
    public int getPassengerCapacity(){ return PASSENGER_CAPACITY; }
    public int getSpeed(){ return SPEED; }
    public Color getColor(){ return color; }
    public int getCurrentPassengers(){ return currentPassengers; }
    
    public Location getCurrentLocation(){
        return new Location(currentLocation.getX(), currentLocation.getY());
    }
    
    public Location getDestination(){
        return new Location(Destination.getX(), Destination.getY());
    }
    
}


