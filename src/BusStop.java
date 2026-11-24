import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayDeque;

/** 
    * <p> <b>Project Name:</b> Bus Rerouting
    * <p> <b>Class Name:</b> BusStop
    * <p> <b>Purpose:</b> Represents a bus stop with a 2D location, waiting passenger count, and last visit timestamp.
    * @author 
    *       <p>- Ahmad Balfakeeh  
    *       <p>- Abdullah Balubaid
    *       <p>- Salam Kanno
    *       <p>- Omar AbuSabaah
    * 
    * @version v1.3 11/24/26

    
**/



public class BusStop {
    private int waitingPassengers;
    private long lastRecordedVisit; 
    private Location busStopLocation;
    private double arrivalRate;
    private double arrivalProgress;
    private final ArrayDeque<Double> arrivalTimes = new ArrayDeque<>();
    private double totalWaitSeconds = 0;
    private int totalBoarded = 0;

    // note 5: priority. HIGH stops are active all day and are the only ones a DoubleDecker may
    // serve; LOW stops are active only part of the day (e.g. a morning/evening window).
    private boolean highPriority = true;   // defaults to high priority, so existing callers are unaffected

    public BusStop(Location location){
        if (location != null){
        busStopLocation = new Location(location.getX(), location.getY());
        }
        else{
            throw new NullPointerException("Location must not be null.");
        }
    }

    BusStop(double x, double y){
        busStopLocation = new Location(x, y);
        
    }

    // note 5
    public boolean isHighPriority() { return highPriority; }
    public void setHighPriority(boolean highPriority) { this.highPriority = highPriority; }

    public void updatePassengers(int Passengers){
        if (Passengers < 0){
            throw new IllegalArgumentException("Passengers cannot be negative.");
        }

        waitingPassengers = Passengers;

    }

    public void recordVisit(){
       lastRecordedVisit = System.currentTimeMillis();
    }

    
    // Getters
    public int getPassenger(){ return waitingPassengers; }
    
    public long getLastVisit(){ return lastRecordedVisit; } 
    
    public Location getLocation(){
        return new Location(busStopLocation.getX(), busStopLocation.getY());
    }


    @Override 
    public String toString(){
        return "BusStop[" + busStopLocation.getX() + ", " + busStopLocation.getY()
                + ", waiting=" + waitingPassengers + "]";
    }



    // Drawing Section
    public void draw(GraphicsContext drawingBrush, int stopNumber, double centerX, double centerY,
                     double unit, Color poleColor, Color accentColor, Color boxFill, Color labelColor,
                     Color outlineColor) {

                    
        drawingBrush.save();
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 13)); // we can change the font here  
        double poleX = centerX - unit - 3;

        // Pole
        drawingBrush.setLineCap(StrokeLineCap.BUTT);
        drawingBrush.setStroke(poleColor);
        drawingBrush.setLineWidth(Math.max(2, unit * 0.2));
        drawingBrush.strokeLine(poleX, centerY - unit * 1.5, poleX, centerY + unit * 0.9);

        // Flag
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(poleX - unit * 0.3, centerY - unit * 1.55, unit * 0.65, unit * 0.55);

        // Sign box
        drawingBrush.setFill(boxFill);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);
        drawingBrush.setStroke(accentColor);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);

        // Top bar
        drawingBrush.setFill(accentColor);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, unit * 0.4);  

        // Label
        String text = "Stop " + (stopNumber) + " (" + getPassenger() + ")";
        double labelY = centerY - unit * 1.95;
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(outlineColor);
        drawingBrush.strokeText(text, centerX, labelY);
        drawingBrush.setFill(labelColor);
        drawingBrush.fillText(text, centerX, labelY);

        drawingBrush.restore();
         
    }
    

    // Arrival of passengers to the bus stop
    public double getArrivalRate() {
        return arrivalRate;
    }

    public void setArrivalRate(double arrivalRate) {
        this.arrivalRate = arrivalRate;
    }

    public void advancePassengers(double seconds, double nowInSimulatedSeconds) {
        arrivalProgress += arrivalRate * seconds;
        int newPassengers = (int) arrivalProgress;
        arrivalProgress -= newPassengers;

        if (newPassengers > 0) {
            updatePassengers(waitingPassengers + newPassengers);
        }

        for (int i = 0; i <newPassengers; i++) {
            arrivalTimes.addLast(nowInSimulatedSeconds);
        }
    }
    

    // note 5: a low-priority stop is only "open" during part of the simulated day. The 300-minute
    // run stands in for a full day, so it is split into three bands: morning (0-60 min), midday
    // (60-240 min, closed for low-priority stops), evening (240-300 min). High-priority stops are
    // always open.
    public boolean isOpenAt(double nowInSimulatedMinutes) {
        if (highPriority) {
            return true;
        }
        boolean morning = nowInSimulatedMinutes < 60;
        boolean evening = nowInSimulatedMinutes >= 240;
        return morning || evening;
    }

    public int boardPassengers(int maxPassengers) {
        return boardPassengers(maxPassengers, 0);
    }

    public int boardPassengers(int maxPassengers, double nowInSiumlatedSeconds) {
        int boarding = Math.min(waitingPassengers, maxPassengers);
        updatePassengers(waitingPassengers - boarding);
        recordVisit();

        for (int i = 0; i < boarding; i++) {
            Double arrivalTime = arrivalTimes.pollFirst();
            if (arrivalTime != null) {
                totalWaitSeconds += (nowInSiumlatedSeconds - arrivalTime);
                totalBoarded++;
            }
        }

        return boarding;
    }

    public double getAverageWaitMinutes() {
        if (totalBoarded == 0) {
            return Double.NaN;
        }

        return (totalWaitSeconds / totalBoarded) / 60.0;
    }

    public double getTotalWaitSeconds() {
        return totalWaitSeconds;
    }

    public int getTotalBoarded() {
        return totalBoarded;
    }
    
    public double getOldestWaitSeconds(double nowInSimulatedSeconds) {
        Double oldestArrival = arrivalTimes.peekFirst();

        if (oldestArrival == null) {
            return 0;
        }

        return nowInSimulatedSeconds - oldestArrival;
    }

}