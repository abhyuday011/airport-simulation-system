package uk.ac.warwick.cs261.ui.components;

import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.ui.UIUtil;

/**
 * UI component class that contains an aircraft, displaying various images depending on aircraft emergency status.
 * Also determines orientation based on takeoff / landing status.
 */
public class AircraftView extends ImageView
{
    private static final double IMAGE_SIZE = 60.0;
    private static final Duration TOOLTIP_SHOW_DELAY = Duration.millis(125);
    private static final Duration TOOLTIP_HIDE_DELAY = Duration.millis(250);

    private static final String PLANE_PATH = "/ui/images/plane.png";
    private static final String PLANE_EMERGENCY_PATH = "/ui/images/plane_emergency.png";
    private static final String PLANE_SEVERE_EMERGENCY_PATH = "/ui/images/plane_severe_emergency.png";

    private static final String PLANE_URL = AircraftView.class.getResource(PLANE_PATH).toString();
    private static final String PLANE_EMERGENCY_URL = AircraftView.class.getResource(PLANE_EMERGENCY_PATH).toString();
    private static final String PLANE_SEVERE_EMERGENCY_URL = AircraftView.class.getResource(PLANE_SEVERE_EMERGENCY_PATH).toString();

    private static final Image PLANE_IMAGE = new Image(PLANE_URL);
    private static final Image PLANE_EMERGENCY_IMAGE = new Image(PLANE_EMERGENCY_URL);
    private static final Image PLANE_SEVERE_EMERGENCY_IMAGE = new Image(PLANE_SEVERE_EMERGENCY_URL);

    private final Tooltip tooltip = new Tooltip();

    public AircraftView() { this(null, 0); }
    
    public AircraftView(Aircraft aircraft) { this(aircraft, 0); }

    public AircraftView(Aircraft aircraft, int rotation)
    {
        setFitWidth(IMAGE_SIZE);
        setFitHeight(IMAGE_SIZE);
        setPreserveRatio(true);

        tooltip.setShowDelay(TOOLTIP_SHOW_DELAY);
        tooltip.setHideDelay(TOOLTIP_HIDE_DELAY);
        tooltip.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        Tooltip.install(this, tooltip);

        setAircraft(aircraft);
        setRotate(rotation);
    }

    public void setAircraft(Aircraft aircraft)
    {
        Image image = aircraft != null ? statusToImage(aircraft.getStatus()) : null;
        String tooltipString = aircraft != null ? buildTooltipText(aircraft) : "NULL AIRCRAFT";

        setImage(image);
        tooltip.setText(tooltipString);
    }

    private static Image statusToImage(AircraftStatus status)
    {
        return switch (status)
        {
            case MECHANICAL_FAILURE, PASSENGER_HEALTH -> PLANE_EMERGENCY_IMAGE;
            case PASSENGER_HEALTH_SEVERE, LOW_FUEL    -> PLANE_SEVERE_EMERGENCY_IMAGE;
            default                                   -> PLANE_IMAGE;
        };
    }

    private static String buildTooltipText(Aircraft aircraft)
    {
        String type = aircraft.getIsTakeoff() ? "Departing" : "Arriving";
        String scheduledTime = UIUtil.timeToString(aircraft.getScheduledTime());
        String queueEnterTime = aircraft.getQueueEnterTime() >= 0 ? UIUtil.timeToString(aircraft.getQueueEnterTime()) : "N/A";

        return String.format(
            "Call Sign:    %s\n" +
            "Type:         %s\n" +
            "Status:       %s\n" +
            "Scheduled:    %s\n" +
            "Queue Entry:  %s",
            aircraft.getCallSign(),
            type,
            aircraft.getStatus(),
            scheduledTime,
            queueEnterTime
        );
    }
}
