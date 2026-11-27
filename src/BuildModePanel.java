import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> BuildModePanel</p>
 * <p><b>Purpose:</b> The side-bar section used for editing the map by hand. The tools are grouped into
 * Stops (add, change priority, delete) and Buses (add, edit route, delete). Choosing a tool highlights
 * it and keeps it active, so several clicks in a row use the same tool; choosing it again, or pressing
 * Esc, puts it down. A hint box always says what the next click will do, and the buttons that matter
 * while a route is being picked (Finish bus, Cancel) are coloured only when they are needed. Every edit
 * is carried out by the {@link BusNetwork}.</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v2.0 11/26/26
 */
public class BuildModePanel {
    // Which build tool the next map click belongs to
    private static final int CLICK_MODE_NONE = 0;
    private static final int CLICK_MODE_ADD_STOP = 1;
    private static final int CLICK_MODE_BUILD_ROUTE = 2;
    private static final int CLICK_MODE_DELETE_STOP = 3;
    private static final int CLICK_MODE_DELETE_BUS = 4;       // also used to pick the bus whose route is edited
    private static final int CLICK_MODE_TOGGLE_PRIORITY = 5;

    private static final String DEFAULT_HINT = "Pick a tool, then click the map. Press the tool again or Esc to put it down. "
            + "Right-click a stop to add 10 waiting passengers.";
    private static final String HIGH_PRIORITY_CHOICE = "High priority";
    private static final String LOW_PRIORITY_CHOICE = "Low priority";

    private static final String HINT_STYLE = "-fx-background-color: " + Palette.css(Palette.HINT_BOX_BACKGROUND) + "; -fx-border-color: "
            + Palette.css(Palette.HINT_BOX_BORDER) + "; "
            + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;";
    private static final String READY_STYLE = "-fx-base: " + Palette.css(Palette.READY_BUTTON) + "; -fx-text-fill: white; -fx-font-weight: bold;";   // Finish bus, when it can be pressed
    private static final String STOP_STYLE = "-fx-base: " + Palette.css(Palette.CANCEL_BUTTON) + "; -fx-text-fill: white;";                           // Cancel, while a route is being picked
    private static final String HEADING_STYLE = "-fx-font-weight: bold; -fx-padding: 4 0 0 0;";

    /**
     * What the panel needs from the rest of the program.
     */
    public interface Host {
        /**
         * Called right before the map is changed, so a run in progress can be reset to t=0.
         */
        void beforeEdit();

        /**
         * Writes a line to the event log.
         *
         * @param message the message to log
         */
        void log(String message);

        /**
         * Called after the map has changed, so labels and the canvas can be refreshed.
         */
        void mapChanged();
    }

    private final GridCanvas canvas;                    // The map area (for picking and drawing aids)
    private final BusNetwork network;                   // The stops and buses being edited (it carries out every edit)
    private final Host host;                            // The rest of the program

    private final VBox panel;
    private final Label hintLabel = new Label(DEFAULT_HINT);
    private final ToggleGroup tools = new ToggleGroup();   // The tools; at most one is active
    private final ToggleButton addStopTool = tool("Add stop");
    private final ToggleButton priorityTool = tool("Change priority");
    private final ToggleButton deleteStopTool = tool("Delete stop");
    private final ToggleButton addBusTool = tool("Add bus");
    private final ToggleButton editRouteTool = tool("Edit route");
    private final ToggleButton deleteBusTool = tool("Delete bus");
    private final ComboBox<String> newStopPriorityBox;
    private final ComboBox<String> newBusTypeBox;
    private final Button finishBusButton = new Button("Finish bus");
    private final Button cancelBusButton = new Button("Cancel");
    private final Label summaryLabel = new Label();
    private final ListView<Bus> busList = new ListView<>();
    private String shownSummary = "";                    // What the summary shows now, so it is only rebuilt when it changes

    private int clickMode = CLICK_MODE_NONE;
    private final List<BusStop> pendingRouteStops = new ArrayList<>();   // Stops picked so far for a route
    private Bus busBeingEdited;                         // Non-null while the user is re-picking this bus's route
    private boolean nextBusClickStartsEdit = false;     // True while the Edit route tool waits for a bus to be clicked

    /**
     * Builds the panel and its controls.
     *
     * @param canvas  the map area
     * @param network the stops and buses; every edit made here is carried out by it
     * @param host    the rest of the program
     */
    public BuildModePanel(GridCanvas canvas, BusNetwork network, Host host) {
        this.canvas = canvas;
        this.network = network;
        this.host = host;

        canvas.setHighlightedStops(pendingRouteStops);

        hintLabel.setWrapText(true);
        VBox hintBox = new VBox(hintLabel);
        hintBox.setStyle(HINT_STYLE);

        // Stops: the kind of the next stop sits right beside the button that adds it
        newStopPriorityBox = new ComboBox<>(FXCollections.observableArrayList(HIGH_PRIORITY_CHOICE, LOW_PRIORITY_CHOICE));
        newStopPriorityBox.getSelectionModel().selectFirst();
        HBox addStopRow = pairRow(newStopPriorityBox, addStopTool);
        HBox stopToolsRow = pairRow(priorityTool, deleteStopTool);

        // Buses: the type of the next bus sits right beside the button that adds it
        newBusTypeBox = new ComboBox<>(FXCollections.observableArrayList(
                RouteBuilder.STANDARD_BUS, RouteBuilder.MINI_BUS, RouteBuilder.DOUBLE_DECKER));
        newBusTypeBox.getSelectionModel().selectFirst();
        HBox addBusRow = pairRow(newBusTypeBox, addBusTool);
        HBox finishRow = pairRow(finishBusButton, cancelBusButton);
        HBox busToolsRow = pairRow(editRouteTool, deleteBusTool);

        finishBusButton.setOnAction(e -> finishNewBus());
        cancelBusButton.setOnAction(e -> cancelRouteBuilding());

        addStopTool.setTooltip(new Tooltip("Place stops on empty intersections"));
        priorityTool.setTooltip(new Tooltip("Switch a stop between high and low priority"));
        deleteStopTool.setTooltip(new Tooltip("Delete stops. Buses routed through one skip it afterwards."));
        addBusTool.setTooltip(new Tooltip("Pick the stops of a new bus's route, then press Finish bus"));
        editRouteTool.setTooltip(new Tooltip("Click a bus, then click its new stops"));
        deleteBusTool.setTooltip(new Tooltip("Delete buses"));

        tools.selectedToggleProperty().addListener((observable, oldTool, newTool) -> activateTool((ToggleButton) newTool));
        updateRouteButtons();

        // The map as it is now
        busList.setPrefHeight(110);
        busList.setMinHeight(70);
        busList.setCellFactory(list -> new ListCell<Bus>() {
            @Override
            protected void updateItem(Bus bus, boolean empty) {
                super.updateItem(bus, empty);
                if (empty || bus == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setGraphic(new Circle(6, bus.getColor()));
                setText(bus.getName() + "  " + bus.getTypeName() + ":  " + network.describeRoute(bus));
            }
        });
        VBox.setVgrow(busList, Priority.ALWAYS);

        Button clearMapButton = new Button("Clear map (remove every stop and bus)");
        clearMapButton.setMaxWidth(Double.MAX_VALUE);
        clearMapButton.setOnAction(e -> clearMap());

        panel = new VBox(8,
                hintBox,
                heading("Stops"), addStopRow, stopToolsRow,
                new Separator(),
                heading("Buses"), addBusRow, finishRow, busToolsRow,
                new Separator(),
                heading("This map"), summaryLabel, busList,
                new Separator(),
                clearMapButton);
        panel.setPadding(new Insets(2, 0, 0, 0));
        refresh();
    }

    // ------------------------------------------------------------------ building the parts

    /**
     * Makes a tool button that stays pressed while its tool is active.
     *
     * @param text the button text
     * @return the button, already part of the tool group
     */
    private ToggleButton tool(String text) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(tools);
        return button;
    }

    /**
     * Puts two controls side by side, sharing the width.
     *
     * @param first  the left control
     * @param second the right control
     * @return the row
     */
    private HBox pairRow(Region first, Region second) {
        first.setMaxWidth(Double.MAX_VALUE);
        second.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(6, first, second);
        HBox.setHgrow(first, Priority.ALWAYS);
        HBox.setHgrow(second, Priority.ALWAYS);
        return row;
    }

    /**
     * Makes a bold group heading.
     *
     * @param text the heading
     * @return the label
     */
    private Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(HEADING_STYLE);
        return label;
    }

    /**
     * Returns the panel to place in the side bar.
     *
     * @return the panel's root node
     */
    public VBox getPanel() {
        return panel;
    }

    /**
     * Checks whether a build tool is active, so the next map click belongs to it.
     *
     * @return {@code true} if a build tool is waiting for a click
     */
    public boolean isEditing() {
        return clickMode != CLICK_MODE_NONE;
    }

    /**
     * Refreshes the "This map" summary and the bus list. Nothing is rebuilt unless something changed.
     */
    public void refresh() {
        int highCount = network.getHighPriorityStopCount();
        StringBuilder signature = new StringBuilder();
        signature.append(network.getStops().size()).append(' ').append(highCount);
        for (Bus bus : network.getBuses()) {
            signature.append('|').append(bus.getName()).append(network.describeRoute(bus));
        }

        if (signature.toString().equals(shownSummary)) {
            return;
        }
        shownSummary = signature.toString();
        summaryLabel.setText(network.getStops().size() + " stops (" + highCount + " high priority), "
                + network.getBuses().size() + " buses");
        busList.setItems(FXCollections.observableArrayList(network.getBuses()));
    }

    // ------------------------------------------------------------------ tools

    /**
     * Puts the chosen tool to work (or, if {@code null}, puts every tool down): sets what the next click
     * does, the cursor, the hint, and the colours of Finish bus and Cancel.
     *
     * @param tool the tool that was chosen, or {@code null} if none is active
     */
    private void activateTool(ToggleButton tool) {
        pendingRouteStops.clear();
        busBeingEdited = null;
        nextBusClickStartsEdit = false;

        if (tool == null) {
            setClickMode(CLICK_MODE_NONE);
            hintLabel.setText(DEFAULT_HINT);
        } else if (tool == addStopTool) {
            setClickMode(CLICK_MODE_ADD_STOP);
            hintLabel.setText("Click an empty intersection to place a stop. The kind is chosen in the drop-down.");
        } else if (tool == priorityTool) {
            setClickMode(CLICK_MODE_TOGGLE_PRIORITY);
            hintLabel.setText("Click a stop to switch it between high and low priority.");
        } else if (tool == deleteStopTool) {
            setClickMode(CLICK_MODE_DELETE_STOP);
            hintLabel.setText("Click a stop to delete it. Any bus routed through it skips it afterwards.");
        } else if (tool == deleteBusTool) {
            setClickMode(CLICK_MODE_DELETE_BUS);
            hintLabel.setText("Click a bus to delete it.");
        } else if (tool == editRouteTool) {
            setClickMode(CLICK_MODE_DELETE_BUS);   // reuses "click a bus" targeting, see handleClick
            nextBusClickStartsEdit = true;
            hintLabel.setText("Click the bus whose route you want to change.");
        } else {
            setClickMode(CLICK_MODE_BUILD_ROUTE);
            hintLabel.setText("Click the stops this bus should visit, in order (at least 2), then press Finish bus.");
        }

        updateRouteButtons();
        canvas.draw();
    }

    /**
     * Records which build tool the next click belongs to, and tells the canvas whether to mark the
     * empty intersections and what the cursor looks like.
     *
     * @param newMode one of the {@code CLICK_MODE_} constants
     */
    private void setClickMode(int newMode) {
        clickMode = newMode;
        canvas.setShowOpenIntersections(newMode == CLICK_MODE_ADD_STOP);
        canvas.setCursor(newMode == CLICK_MODE_ADD_STOP ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }

    /**
     * Colours Finish bus and Cancel only when they are needed: Cancel while a route is being picked, and
     * Finish bus (green) once the route has the two stops it needs. Otherwise they are plain and disabled.
     */
    private void updateRouteButtons() {
        boolean picking = (clickMode == CLICK_MODE_BUILD_ROUTE);
        boolean ready = picking && pendingRouteStops.size() >= 2;

        finishBusButton.setDisable(!picking);
        cancelBusButton.setDisable(!picking);
        finishBusButton.setStyle(ready ? READY_STYLE : "");
        cancelBusButton.setStyle(picking ? STOP_STYLE : "");
    }

    /**
     * Puts every tool down and forgets any route being picked or edited.
     */
    public void cancelRouteBuilding() {
        tools.selectToggle(null);
        activateTool(null);   // also when no tool was selected, so the screen is always in the same state
    }

    /**
     * Handles a map click for whichever build tool is active (adding or deleting stops and buses,
     * changing stop priority, building or editing routes).
     *
     * @param event the mouse click on the map
     */
    public void handleClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        int[] gridPoint = canvas.pickGridPoint(event.getX(), event.getY());
        BusStop clickedStop = (gridPoint == null) ? null : network.findStopAt(gridPoint[0], gridPoint[1]);

        if (clickMode == CLICK_MODE_ADD_STOP) {
            if (gridPoint != null && clickedStop == null) {
                addStop(gridPoint[0], gridPoint[1]);
            }
        } else if (clickMode == CLICK_MODE_BUILD_ROUTE) {
            pickRouteStop(clickedStop);
        } else if (clickMode == CLICK_MODE_DELETE_STOP) {
            if (clickedStop != null) {
                deleteStop(clickedStop);
            }
        } else if (clickMode == CLICK_MODE_TOGGLE_PRIORITY) {
            if (clickedStop != null) {
                togglePriority(clickedStop);
            }
        } else if (clickMode == CLICK_MODE_DELETE_BUS) {
            // Delete a bus, OR (if the Edit route tool is active) pick which bus's route to re-pick
            Bus clickedBus = canvas.findBusAt(event.getX(), event.getY());
            if (clickedBus != null) {
                if (nextBusClickStartsEdit) {
                    startEditingRoute(clickedBus);
                } else {
                    deleteBus(clickedBus);
                }
            }
        }

        canvas.draw();
    }

    /**
     * Returns the bus type that a route being picked is for: the edited bus's own type, or the one
     * chosen in the drop-down.
     *
     * @return the bus type's display name
     */
    private String currentBusType() {
        return (busBeingEdited != null) ? busBeingEdited.getTypeName() : newBusTypeBox.getValue();
    }

    // ------------------------------------------------------------------ edits

    /**
     * Adds a clicked stop to the route being picked (or takes it off again if it is the last one
     * picked). A low-priority stop is refused when the route is for a Double Decker.
     *
     * @param clickedStop the stop that was clicked, or {@code null} if the click was not on a stop
     */
    private void pickRouteStop(BusStop clickedStop) {
        if (clickedStop == null) {
            return;
        }

        boolean alreadyPicked = pendingRouteStops.contains(clickedStop);
        boolean isDoubleDeckerRoute = currentBusType().equals(RouteBuilder.DOUBLE_DECKER);
        if (!alreadyPicked && isDoubleDeckerRoute && !clickedStop.isHighPriority()) {
            hintLabel.setText(RouteBuilder.DOUBLE_DECKER_LOW_PRIORITY_MESSAGE);
            return;
        }

        if (!alreadyPicked) {
            pendingRouteStops.add(clickedStop);
        } else if (pendingRouteStops.get(pendingRouteStops.size() - 1) == clickedStop) {
            // Clicking the last-added stop toggles it back off
            pendingRouteStops.remove(clickedStop);
        }
        hintLabel.setText("Route so far: " + pendingRouteStops.size() + " stop(s). Click more, or press Finish bus.");
        updateRouteButtons();
    }

    /**
     * Places a new stop of the chosen priority at {@code (gridX, gridY)}, then logs the addition.
     *
     * @param gridX grid X coordinate
     * @param gridY grid Y coordinate
     */
    private void addStop(int gridX, int gridY) {
        host.beforeEdit();

        boolean highPriority = HIGH_PRIORITY_CHOICE.equals(newStopPriorityBox.getValue());
        BusStop stop = network.addStop(gridX, gridY, highPriority);

        host.log("Added " + stop.getName() + " (" + (highPriority ? "high" : "low") + " priority) at (" + gridX + ", "
                + gridY + "). It has no bus route yet - add a bus through it, or it will only ever be reached by rerouting.");
        host.mapChanged();
    }

    /**
     * Switches a stop between high and low priority. Refused if the stop would become low priority
     * while it is on a Double Decker's route.
     *
     * @param stop the stop to switch
     */
    private void togglePriority(BusStop stop) {
        boolean becomesLow = stop.isHighPriority();
        if (becomesLow && network.isOnDoubleDeckerRoute(stop)) {
            hintLabel.setText(stop.getName() + " is on a Double Decker's route. "
                    + RouteBuilder.DOUBLE_DECKER_LOW_PRIORITY_MESSAGE
                    + " Edit that bus's route first.");
            return;
        }

        host.beforeEdit();
        BusStop replacement = network.togglePriority(stop);

        host.log(replacement.getName() + " is now " + (becomesLow ? "low" : "high") + " priority.");
        host.mapChanged();
    }

    /**
     * Removes a stop. Buses whose route passes through it keep driving, but nobody boards there any more.
     *
     * @param stop the stop to remove
     */
    private void deleteStop(BusStop stop) {
        host.beforeEdit();
        String name = stop.getName();   // asked before the numbers shift
        network.deleteStop(stop);
        host.log("Deleted " + name + ".");
        host.mapChanged();
    }

    /**
     * Removes a bus.
     *
     * @param bus the bus to remove
     */
    private void deleteBus(Bus bus) {
        host.beforeEdit();
        String name = bus.getName();   // asked before the numbers shift
        network.deleteBus(bus);
        host.log("Deleted " + name + ".");
        host.mapChanged();
    }

    /**
     * Enters route-editing for an existing bus so the user can click its new stops in order.
     *
     * @param bus the bus whose route is being edited
     */
    private void startEditingRoute(Bus bus) {
        busBeingEdited = bus;
        nextBusClickStartsEdit = false;
        pendingRouteStops.clear();
        setClickMode(CLICK_MODE_BUILD_ROUTE);
        updateRouteButtons();
        hintLabel.setText("Click the new stops for " + bus.getName()
                + "'s route, in order (at least 2), then press Finish bus.");
    }

    /**
     * Removes every stop and bus, leaving an empty grid.
     */
    private void clearMap() {
        host.beforeEdit();
        network.clear();

        cancelRouteBuilding();
        host.log("Map cleared.");
        host.mapChanged();
    }

    /**
     * Validates the picked route (at least 2 stops, and no low-priority stop on a Double Decker),
     * then either creates a new bus or replaces the edited bus with a new one of the same type and color.
     */
    private void finishNewBus() {
        if (pendingRouteStops.size() < 2) {
            hintLabel.setText("Pick at least 2 stops before finishing.");
            return;
        }

        // Editing an existing bus's route keeps the SAME bus TYPE and COLOR, so only its stops change
        boolean isEditing = (busBeingEdited != null);
        String chosenType = currentBusType();

        // A Double Decker may only be given high-priority stops
        if (chosenType.equals(RouteBuilder.DOUBLE_DECKER)) {
            for (BusStop stop : pendingRouteStops) {
                if (!stop.isHighPriority()) {
                    hintLabel.setText(RouteBuilder.DOUBLE_DECKER_LOW_PRIORITY_MESSAGE
                            + " Remove it from the route, or choose a different bus type.");
                    return;
                }
            }
        }

        host.beforeEdit();

        ArrayList<Location> route = new ArrayList<>();
        for (BusStop stop : pendingRouteStops) {
            route.add(stop.getLocation());
        }

        Color busColor = isEditing ? busBeingEdited.getColor() : network.randomBusColor();
        Bus newBus = network.createBus(chosenType, busColor, route);

        if (isEditing) {
            // A bus has no route setter, so editing a route means replacing the old bus with a new
            // one of the same type and colour at the same place in the list.
            network.replaceBus(busBeingEdited, newBus);
            host.log("Updated " + newBus.getName() + "'s route (" + pendingRouteStops.size() + " stops).");
        } else {
            network.addBus(newBus);
            host.log("Added " + newBus.getName() + " (" + chosenType + ") with a "
                    + pendingRouteStops.size() + "-stop route.");
        }

        cancelRouteBuilding();
        host.mapChanged();
    }
}
