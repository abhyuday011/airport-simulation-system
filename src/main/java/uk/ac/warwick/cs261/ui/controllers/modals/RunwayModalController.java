package uk.ac.warwick.cs261.ui.controllers.modals;

import java.util.Queue;

import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.commands.RunwayModeChangeCommand;
import uk.ac.warwick.cs261.threading.commands.RunwayStatusChangeCommand;
import uk.ac.warwick.cs261.threading.commands.SimulationCommand;

public class RunwayModalController extends ModalController
{
    private Runway runway;
    private Queue<SimulationCommand> commandQueue;
    private RunwayMode newMode;
    private RunwayStatus newStatus;

    @FXML
    private Label title;

    @FXML
    private ChoiceBox<RunwayMode> modeChoice;

    @FXML
    private ChoiceBox<RunwayStatus> statusChoice;

    @FXML
    private Button updateBtn;

    @FXML
    private void initialize()
    {
        modeChoice.getItems().addAll(RunwayMode.values());
        modeChoice.getItems().add(null);
        statusChoice.getItems().add(RunwayStatus.INSPECTION);
        statusChoice.getItems().add(RunwayStatus.SNOW_CLEARANCE);
        statusChoice.getItems().add(RunwayStatus.EQUIPMENT_FAILURE);
        statusChoice.getItems().add(null);


        modeChoice.valueProperty().addListener(this::onModeChanged);
        statusChoice.valueProperty().addListener(this::onStatusChanged);
    }

    private void onModeChanged(Observable observable, RunwayMode oldVal, RunwayMode newVal)
    {
        newMode = newVal;
        return;
    }

    private void onStatusChanged(Observable observable, RunwayStatus oldVal, RunwayStatus newVal)
    {
        newStatus = newVal;
    }

    @FXML
    private void updateRunway(ActionEvent event)
    {
        if (newMode != null)
        {
            RunwayModeChangeCommand modeCommand = new RunwayModeChangeCommand(newMode, runway);
            commandQueue.add(modeCommand);
        }

        if (newStatus != null)
        {
            RunwayStatusChangeCommand statusCommand = new RunwayStatusChangeCommand(newStatus, runway);
            commandQueue.add(statusCommand);
        }
        close();
    }


    public void setRunway(Runway runway)
    {
        this.runway = runway;

        // runwayMode.setText(runway.getMode().toString());
        // runwayStatus.setText(runway.getStatus().toString());
    }

    public void setCommandQueue(Queue<SimulationCommand> queue)
    {
        this.commandQueue = queue;
    }

    // Line to tell if runway is open
    // boolean runwayOpen = runway.getStatus() == RunwayStatus.FREE;

    @Override
    public String getTitle() { return title.getText(); }

    @Override
    public void setTitle(String text) { title.setText(text); }
}