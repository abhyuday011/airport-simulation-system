package uk.ac.warwick.cs261.ui.controllers;

import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;

import java.util.function.Consumer;

public class RunwayParametersCardController
{
    @FXML
    private VBox root;

    @FXML
    private Label runwayIndexLabel;

    @FXML
    private Label runwayTitleLabel;

    @FXML
    private Label runwaySubtitleLabel;

    @FXML
    private TextField lengthField;

    @FXML
    private ChoiceBox<RunwayMode> modeChoice;

    @FXML
    private ChoiceBox<RunwayStatus> statusChoice;

    private RunwayParameters runwayParameters;
    private Consumer<RunwayParameters> onDeleteCallback;
    private int index = 1;

    @FXML
    private void initialize()
    {
        modeChoice.getItems().addAll(RunwayMode.values());
        statusChoice.getItems().addAll(RunwayStatus.FREE, RunwayStatus.INSPECTION, RunwayStatus.SNOW_CLEARANCE, RunwayStatus.EQUIPMENT_FAILURE);

        lengthField.textProperty().addListener(this::onLengthChanged);
        modeChoice.valueProperty().addListener(this::onModeChanged);
        statusChoice.valueProperty().addListener(this::onStatusChanged);
    }

    public void setRunwayParameters(RunwayParameters parameters, int index)
    {
        this.runwayParameters = parameters;
        this.index = index;

        lengthField.setText(String.valueOf(parameters.getLength()));
        modeChoice.setValue(parameters.getMode());
        statusChoice.setValue(parameters.getStatus());

        updateHeader();
        updateSubtitle();
    }

    public RunwayParameters getRunwayParameters() { return runwayParameters; }

    public void setOnDeleteCallback(Consumer<RunwayParameters> callback)
    {
        this.onDeleteCallback = callback;
    }

    public VBox getRoot() { return root; }

    public void setIndex(int index)
    {
        this.index = index;
        updateHeader();
        updateSubtitle();
    }

    private void updateHeader()
    {
        runwayIndexLabel.setText("R" + index);
        runwayTitleLabel.setText("Runway " + index);
    }

    private void updateSubtitle()
    {
        if (runwayParameters == null) return;

        String length = formatLength(runwayParameters.getLength());
        String mode   = runwayParameters.getMode()   != null ? runwayParameters.getMode().toString()   : "-";
        String status = runwayParameters.getStatus() != null ? runwayParameters.getStatus().toString() : "-";

        runwaySubtitleLabel.setText(length + " · " + mode + " · " + status);
    }

    private static String formatLength(long metres)
    {
        if (metres >= 1000)
            return String.format("%,.0f", metres / 1000.0) + " km";
        return metres + " m";
    }

    private void onLengthChanged(Observable observable, String oldVal, String newVal)
    {
        if (runwayParameters == null) return;

        try
        {
            runwayParameters.setLength(Long.parseLong(newVal.trim()));
            updateSubtitle();
        }
        catch (NumberFormatException ignored) {}
    }

    private void onModeChanged(Observable observable, RunwayMode oldVal, RunwayMode newVal)
    {
        if (runwayParameters != null && newVal != null)
        {
            runwayParameters.setMode(newVal);
            updateSubtitle();
        }
    }

    private void onStatusChanged(Observable observable, RunwayStatus oldVal, RunwayStatus newVal)
    {
        if (runwayParameters != null && newVal != null)
        {
            runwayParameters.setStatus(newVal);
            updateSubtitle();
        }
    }

    @FXML
    private void onDelete()
    {
        if (onDeleteCallback != null)
            onDeleteCallback.accept(runwayParameters);
    }
}
