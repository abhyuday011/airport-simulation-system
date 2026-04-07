package uk.ac.warwick.cs261.ui.controllers;

import java.io.IOException;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import uk.ac.warwick.cs261.App;
import uk.ac.warwick.cs261.simulation.FinalSimulationStats;
import uk.ac.warwick.cs261.threading.SynchronisationState;
import uk.ac.warwick.cs261.ui.UIUtil;

public class FastModeScreenController 
{
    @FXML
    Button viewStatisticsButton;

    @FXML
    ProgressIndicator spinner;

    @FXML
    Label messageLabel;

    SynchronisationState synchronisation;

    long lastTime;

    private final AnimationTimer tickAnimationTimer = new AnimationTimer() 
    {
        @Override
        public void handle(long currentTime)
        {
            if (lastTime == -1)
            {
                lastTime = currentTime;
                return;
            }

            double deltaTime = (currentTime - lastTime) / UIUtil.NANO_SECONDS_TO_SECONDS;
            tick(deltaTime);
            lastTime = currentTime;
        }
    };

    @FXML
    public void initialize()
    {
        viewStatisticsButton.setDisable(true);
        tickAnimationTimer.start();
    }

    @FXML
    private void quitToStartScreen()
    {
        try
        {
            if (synchronisation == null)
                return;

            App.goToFXMLScreen(App.START_SCRREN_FXML_PATH);
            synchronisation.cancelSimulation();
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void viewSimulationStatistics()
    {
        try
        {
            if (synchronisation == null)
                return;

            if (!synchronisation.getIsSimulationCancelled().get())
            {
                ReportsController controller = App.goToFXMLScreen(App.REPORT_SCRREN_FXML_PATH);
                FinalSimulationStats finalSimulationStats = new FinalSimulationStats(synchronisation.getStatistics());
                controller.setFinalSimulationStatistics(finalSimulationStats);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void tick(double deltaTime)
    {
        if (synchronisation == null)
            return;

        if (synchronisation.getIsSimulationDone().get()) 
        {
            tickAnimationTimer.stop();

            if (!synchronisation.getIsSimulationCancelled().get())
                onSimulationFinished();

            return;
        }
    }

    public void setSynchronisation(SynchronisationState synchronisation)
    {
        this.synchronisation = synchronisation;
    }

    private void onSimulationFinished()
    {
        spinner.setVisible(false);
        viewStatisticsButton.setDisable(false);
        messageLabel.setText("Simulation Completed");
    }
}
