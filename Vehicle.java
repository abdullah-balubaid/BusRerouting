import java.awt.Color;
/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> Vehicle
    * <p> <b>Purpose:</b> Work as an initial mapping of the subclass bus that contains the most important and common datafields / methods.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.0 11/16/26

    
**/

public abstract class Vehicle {
    private final int PASSEGNER_CAPACITY; 
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

    Vehicle(int Capacity , int Speed, Color color){
        PASSEGNER_CAPACITY = Capacity;
        SPEED = Speed;
        this.color = color;
        
    }

    // Checkers
    public boolean isEmpty(){return currentPassengers == 0;}
    public boolean isMoving(){return isMoving;}
    public boolean isFull(){return currentPassengers == PASSEGNER_CAPACITY;}
    

    // Setters
    public void updateLocation(Location newLocation){currentLocation.updateLocation(newLocation);}
    public void updateDestination(Location newDestination){Destination.updateLocation(newDestination);}
    public void setCurrentPassengers(int Passengers){currentPassengers = Passengers;}
    public void updateMoving(){isMoving = currentLocation != Destination; } // NEW


    // Getters
    public int getPassengerCapacity(){return PASSEGNER_CAPACITY;}
    public int getSpeed(){return SPEED;}
    public Color getColor(){return this.color;}
    public int getCurrentPassengers(){return currentPassengers;}
    public Location getCurrentLocation(){return currentLocation;}
    public Location getDestination(){return new Location(Destination.getX(), Destination.getY());}


    
}


    /**
     * Finds the smallest element in the range from start to end and returns its position.
     * @param start The starting index for the search.
     * @param end The ending index (exclusive) for the search.
     * Pre-condition: start and end must be valid indices within the array bounds.
     * Post-condition: Returns the index of the minimum value in the specified range.
     * @return The index of the smallest element.
     */