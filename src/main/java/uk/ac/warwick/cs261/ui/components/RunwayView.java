package uk.ac.warwick.cs261.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;

public class RunwayView extends VBox
{
    private static final String BOX_CSS_CLASS          = "queue-box";
    private static final String BOX_UNPAUSED_CSS_CLASS = "queue-box--no-hover";
    private static final String HEADER_CSS_CLASS       = "queue-box__header";
    private static final String LABEL_CSS_CLASS        = "queue-box__label";
    private static final String MODE_CSS_CLASS         = "runway-view__mode";
    private static final String BODY_CSS_CLASS         = "runway-view__body";
    private static final String STATUS_CSS_CLASS       = "runway-view__status";
    private static final String STATUS_WARN_CSS_CLASS  = "runway-view__status--warn";
    private static final String STATUS_ERROR_CSS_CLASS = "runway-view__status--error";

    private Runway runway;
    private boolean isPaused = false;

    private final HBox headerBox = new HBox();
    private final Label nameLabel = new Label();

    private final VBox bodyBox = new VBox();
    private final Label modeLabel = new Label();
    private final Label statusLabel = new Label();
    private final AircraftView aircraftView = new AircraftView();

    public RunwayView() { this(null, ""); }

    public RunwayView(Runway runway, String name)
    {
        getStyleClass().add(BOX_CSS_CLASS);
        // Hover disabled by default, simulation runs on start
        getStyleClass().add(BOX_UNPAUSED_CSS_CLASS);
        setSpacing(0);
        setPrefWidth(90);
        setMinWidth(90);
        setMaxWidth(90);
        setPrefHeight(170);
        setMinHeight(170);
        setMaxHeight(170);

        nameLabel.getStyleClass().add(LABEL_CSS_CLASS);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        headerBox.getStyleClass().add(HEADER_CSS_CLASS);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setMaxWidth(Double.MAX_VALUE);
        headerBox.getChildren().add(nameLabel);

        modeLabel.getStyleClass().add(MODE_CSS_CLASS);
        modeLabel.setWrapText(true);
        modeLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        modeLabel.setMaxWidth(Double.MAX_VALUE);

        statusLabel.getStyleClass().add(STATUS_CSS_CLASS);
        statusLabel.setWrapText(true);
        statusLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        bodyBox.getStyleClass().add(BODY_CSS_CLASS);
        bodyBox.setSpacing(4);
        bodyBox.setAlignment(Pos.TOP_CENTER);
        bodyBox.getChildren().addAll(modeLabel, statusLabel, aircraftView);
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        getChildren().addAll(headerBox, bodyBox);

        setRunway(runway);
        setRunwayName(name);
    }

    public void setRunwayName(String name)
    {
        nameLabel.setText(name);
    }

    public void setRunway(Runway runway)
    {
        this.runway = runway;

        String modeText   = runway != null ? toDisplayString(runway.getMode())   : "-";
        String statusText = runway != null ? toDisplayString(runway.getStatus()) : "-";

        modeLabel.setText(modeText);
        statusLabel.setText(statusText);

        applyStatusStyle(runway != null ? runway.getStatus() : null);

        Aircraft aircraft = runway != null ? runway.getAircraft() : null;
        int rotation = aircraft != null && aircraft.getIsTakeoff() ? 180 : 0;
        aircraftView.setRotate(rotation);
        aircraftView.setAircraft(aircraft);
    }

    private void applyStatusStyle(RunwayStatus status)
    {
        statusLabel.getStyleClass().removeAll(
            STATUS_WARN_CSS_CLASS,
            STATUS_ERROR_CSS_CLASS
        );

        switch (status)
        {
            case INSPECTION, SNOW_CLEARANCE:
                statusLabel.getStyleClass().add(STATUS_WARN_CSS_CLASS);
                break;
            case EQUIPMENT_FAILURE:
                statusLabel.getStyleClass().add(STATUS_ERROR_CSS_CLASS);
                break;
            default:
                break;
        }
    }

    private static String toDisplayString(RunwayMode mode)
    {
        return switch (mode)
        {
            case LANDING  -> "Landing";
            case TAKEOFF  -> "Takeoff";
            case MIXED -> "Mixed";
        };
    }

    private static String toDisplayString(RunwayStatus status)
    {
        return switch (status)
        {
            case FREE              -> "Free";
            case OCCUPIED          -> "Occupied";
            case INSPECTION        -> "Inspection";
            case SNOW_CLEARANCE    -> "Snow Clearance";
            case EQUIPMENT_FAILURE -> "Equipment Failure";
        };
    }

    public Runway getRunway() { return runway; }

    public void setIsPaused(boolean isPaused)
    {
        this.isPaused = isPaused;

        if (isPaused)
        {
            getStyleClass().remove(BOX_UNPAUSED_CSS_CLASS);
        }
        else
        {
            if (!getStyleClass().contains(BOX_UNPAUSED_CSS_CLASS))
                getStyleClass().add(BOX_UNPAUSED_CSS_CLASS);
        }
    }

    public boolean getIsPaused() { return isPaused; }
}