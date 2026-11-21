import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;




/**
 * <p> <b>Project Name:</b> Bus Rerouting
 * <p> <b>Class Name:</b> Map
 * <p> <b>Purpose:</b> .
 * @author
 *       <p>- Ahmad Balfakeeh
 *       <p>- Abdullah Balubaid
 *       <p>- Salam Kanno
 *       <p>- Omar AbuSabaah
 * @version v1.0 11/21/26
 */


public class Map extends Application {

    // step 1
    private final Canvas canvas = new Canvas();
    private static final int GRID_SIZE = 20; 
    private static final double MARGIN = 46;
    // step 2
    private double cellWidth;
    private double cellHeight;

    // step 2
    private static final int NUMBER_OF_STOPS = 21; // ??????????

    // step 2
    private final List<BusStop> stops = new ArrayList<>();
    private final Random random = new Random();

            
    // Color Palette
    private static final Color WHITE = Color.web("#ffffff");
    private static final Color GREY1 = Color.web("#c8c8c8");
    private static final Color GREY2 = Color.web("#b4b4b4");
    private static final Color ORANGE = Color.web("#f5a623");
    private static final Color NAVY1 = Color.web("#2d3548");
    private static final Color NAVY2 = Color.web("#1f2733");
    private static final Color CADETBLUE = Color.web("#a9b4c4");
    
    @Override 
    public void start(Stage stage) {
        // step 1
        Pane pane = new Pane(canvas);
    
        DoubleProperty canvasWidth = canvas.widthProperty();
        ReadOnlyDoubleProperty paneWidth = pane.widthProperty();
        canvasWidth.bind(paneWidth);

        DoubleProperty canvasHeight = canvas.heightProperty();
        ReadOnlyDoubleProperty paneHeight = pane.heightProperty();
        canvasHeight.bind(paneHeight);

        paneWidth.addListener((observable, oldWidth, newWidth) -> draw());
        paneHeight.addListener((observable, oldHeight, newHeight) -> draw()); 

        // step 2
        pane.setOnMouseClicked(e -> {createRandomStops(NUMBER_OF_STOPS); draw();});

        createRandomStops(NUMBER_OF_STOPS);

        // step 1
        stage.setTitle("Bus Rerouting");
        stage.setScene(new Scene(pane, 900, 700));
        stage.show();

    }
    
        // step 2
    private void createRandomStops(int n) {
        stops.clear();

        List<int[]> intersections = new ArrayList<>();

        for (int x = 0; x <= GRID_SIZE; x++) {
            for (int y = 0; y <= GRID_SIZE; y++) {
                intersections.add(new int[]{x, y});
            }
        }

        Collections.shuffle(intersections, random); // for generating a random bus stop locations

        for (int i = 0; i < n; i++) {
            int[] point = intersections.get(i);
            BusStop stop = new BusStop(point[0], point[1]);
            stop.updatePassengers(3 + random.nextInt(28)); // Random passengers allocation
            stops.add(stop);
        }

    }
    
    private void draw() {
        // step 1
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if ( width <= 0 || height <= 0 ) { return; }

        // // Size of one grid
        cellWidth = ( width - 2 * MARGIN ) / GRID_SIZE;
        cellHeight = ( height - 2 * MARGIN ) / GRID_SIZE;

        double lineWidth = 7.5;

        GraphicsContext drawingBrush = canvas.getGraphicsContext2D();

        
        drawingBrush.setFill(WHITE);
        drawingBrush.fillRect(0, 0, width, height);
        
        // step 2
        drawGrid(drawingBrush, lineWidth);
        drawStops(drawingBrush);

        }

        // step 2
        private void drawGrid(GraphicsContext drawingBrush, double lineWidth) {
        drawingBrush.setLineCap(StrokeLineCap.ROUND);
        drawingBrush.setStroke(GREY1);
        drawingBrush.setLineWidth(lineWidth);
        // loop over the entire page
        for (int i = 0; i <= GRID_SIZE; i++) {
            drawingBrush.strokeLine(pointX(i), pointY(0), pointX(i), pointY(GRID_SIZE)); // Vertical Line Drawing
            drawingBrush.strokeLine(pointX(0), pointY(i), pointX(GRID_SIZE), pointY(i)); // Horizontal Line Drawing
        }

        drawingBrush.setLineJoin(StrokeLineJoin.ROUND);
        drawingBrush.setStroke(GREY2);
        drawingBrush.setLineWidth(lineWidth);
        drawingBrush.strokeRect(pointX(0), pointY(0), pointX(GRID_SIZE) - pointX(0), pointY(GRID_SIZE) - pointY(0));
    }

     private double pointX(double gridX) {
            return MARGIN + gridX * cellWidth;
        }
        
    private double pointY(double gridY) {
            return MARGIN + gridY * cellHeight;
        }

    private void drawStops(GraphicsContext drawingBrush) {
        double iconSize = Math.max(26, Math.min(56, Math.min(cellWidth, cellHeight) * 0.5));
        double unit = Math.max(7, iconSize * 0.26);

        
        drawingBrush.setTextAlign(TextAlignment.CENTER);
        drawingBrush.setFont(Font.font("System", FontWeight.BOLD, 13)); // we can change the font here
        
        for (int i = 0; i <= GRID_SIZE; i++) {
            BusStop stop = stops.get(i);
            Location location = stop.getLocation();

            double centerX = pointX(location.getX());
            double centerY = pointY(location.getY());
            drawStopIcon(drawingBrush, centerX, centerY, unit);

            String text = "Stop" + (i+1) + " [" + stop.getPassenger() + "]";
            stopLabel(drawingBrush, text, centerX, centerY - unit * 1.95, NAVY2);
        }

    }
        // step 2
    private void drawStopIcon(GraphicsContext drawingBrush, double centerX, double centerY, double unit) {
        double poleX = centerX - unit - 3;

        // pole
        drawingBrush.setLineCap(StrokeLineCap.BUTT);
        drawingBrush.setStroke(CADETBLUE);
        drawingBrush.setLineWidth(Math.max(2, unit * 0.2));
        drawingBrush.strokeLine(poleX, centerY - unit * 1.5, poleX, centerY + unit * 0.9);

        // little orange flag on top of the pole
        drawingBrush.setFill(ORANGE);
        drawingBrush.fillRect(poleX - unit * 0.3, centerY - unit * 1.55, unit * 0.65, unit * 0.55);

        // dark sign box with orange border
        drawingBrush.setFill(NAVY1);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);
        drawingBrush.setStroke(ORANGE);
        drawingBrush.setLineWidth(2);
        drawingBrush.strokeRect(centerX - unit, centerY - unit, 2 * unit, 2 * unit);

        // thicker orange bar along the top of the box
        drawingBrush.setFill(ORANGE);
        drawingBrush.fillRect(centerX - unit, centerY - unit, 2 * unit, unit * 0.4);
    }
        // step 2
    private void stopLabel(GraphicsContext drawingBrush, String text, double x, double y, Color color) {
        drawingBrush.setLineWidth(3);
        drawingBrush.setStroke(WHITE);
        drawingBrush.strokeText(text, x, y);
        drawingBrush.setFill(color);
        drawingBrush.setFill(color);
        drawingBrush.fillText(text, x, y);
    }

}