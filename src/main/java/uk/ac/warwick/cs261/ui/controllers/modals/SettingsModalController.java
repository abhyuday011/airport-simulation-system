package uk.ac.warwick.cs261.ui.controllers.modals;

import java.util.function.DoubleConsumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.ac.warwick.cs261.ui.UIUtil;


public class SettingsModalController extends ModalController
{
    private static final String TITLE = "Settings";

    @FXML private Slider speedSlider;
    @FXML private Label speedValueLabel;
    @FXML private Button fastModeBtn;

    private DoubleConsumer updateSpeedValue;
    private Runnable switchToFastMode;
    private Runnable quitSimulation;

    @FXML
    public void initialize()
    {
        speedSlider.valueProperty().addListener(x -> refreshSpeedValue());
        speedSlider.setMax(1.0);
        speedSlider.setMin(0.0);
        speedSlider.setValue(0.5);
    }

    public void setUpdateSpeedValue(DoubleConsumer updateSpeedValue) { this.updateSpeedValue = updateSpeedValue; }
    
    public void setSwitchToFastMode(Runnable switchToFastMode) { this.switchToFastMode = switchToFastMode; }

    public void setQuitSimulation(Runnable quitSimulation) { this.quitSimulation = quitSimulation; }

    @FXML
    private void onSwitchToFastMode()
    {
        close();

        if (switchToFastMode != null)
            switchToFastMode.run();
    }

    @FXML
    private void onQuitSimulation()
    {
        close();

        if (quitSimulation != null)
            quitSimulation.run();
    }

    public void refreshSpeedValue()
    {
        double labelValue = UIUtil.lerp(1, 10, speedSlider.getValue());
        String labelText = String.format("x%.2f", labelValue);

        speedValueLabel.setText(labelText);

        if (updateSpeedValue != null)
            updateSpeedValue.accept(speedSlider.getValue());
    }

    @Override
    public String getTitle() { return TITLE; }
    
    @Override
    public void setTitle(String text) {}
}
