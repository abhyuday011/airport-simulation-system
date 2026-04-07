package uk.ac.warwick.cs261.ui.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import uk.ac.warwick.cs261.ui.components.ErrorNotification;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uk.ac.warwick.cs261.App;
import uk.ac.warwick.cs261.datastructures.IndexedPriorityQueue;
import uk.ac.warwick.cs261.datastructures.IndexedQueue;
import uk.ac.warwick.cs261.simulation.FinalSimulationStats;
import uk.ac.warwick.cs261.simulation.Schedule;
import uk.ac.warwick.cs261.simulation.Simulation;
import uk.ac.warwick.cs261.simulation.SimulationConstants;
import uk.ac.warwick.cs261.simulation.SimulationParameters;
import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.threading.SynchronisationState;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.ui.UIUtil;
import uk.ac.warwick.cs261.ui.components.AircraftTableView;
import uk.ac.warwick.cs261.ui.components.AircraftView;
import uk.ac.warwick.cs261.ui.components.RunwayView;
import uk.ac.warwick.cs261.ui.controllers.modals.AircraftQueueModalController;
import uk.ac.warwick.cs261.ui.controllers.modals.RunwayModalController;
import uk.ac.warwick.cs261.ui.controllers.modals.SettingsModalController;
import uk.ac.warwick.cs261.ui.factories.AircraftQueueModalFactory;
import uk.ac.warwick.cs261.ui.factories.MessageViewFactory;
import uk.ac.warwick.cs261.ui.factories.RunwayModalFactory;
import uk.ac.warwick.cs261.ui.factories.SettingsModalFactory;

public class SimulationController 
{
    private long lastTime;

    private double snapshotDisplayDuration;
    private double snapshotDisplayTime;

    private boolean isPaused;
    private boolean uiHasTurn;
    private boolean isFlightTableInitialized;

    private SynchronisationState synchronisation;
    private SimulationParameters parameters;

    private List<RunwayView> runwayViews = new ArrayList<>();

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

    @FXML private Label simulationTime;
    @FXML private Label simulationDuration;

    @FXML private FlowPane runwayBoxes;
    @FXML private VBox holdingQueue;
    @FXML private HBox takeoffQueue;
    @FXML private VBox eventsList;
    @FXML private Label totalAircraft;
    @FXML private Label avgTakeoff;
    @FXML private Label avgHolding;
    @FXML private Label hQueueSize;
    @FXML private Label tQueueSize;
    @FXML private AircraftTableView flightTable;
    @FXML private Button pauseBtn;
    @FXML private Button statsSummary;

    Stage stage;

    private AircraftQueueModalController takeoffQueueModal;
    private AircraftQueueModalController holdingQueueModal;
    private SettingsModalController settingsModal;
    private RunwayModalController runwayModal;

    @FXML
    public void initialize() throws IOException
    {   
        takeoffQueueModal = AircraftQueueModalFactory.create("Takeoff Queue");
        holdingQueueModal = AircraftQueueModalFactory.create("Holding Queue");

        settingsModal = SettingsModalFactory.create();

        settingsModal.setQuitSimulation(this::quitToStartScreen);
        settingsModal.setSwitchToFastMode(this::transitionToFastMode);
        settingsModal.setUpdateSpeedValue(this::onSpeedValueChange);
        settingsModal.refreshSpeedValue();

        runwayModal = RunwayModalFactory.create();

        runwayBoxes.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) 
            {
                stage = (Stage) newScene.getWindow();
                stage.setOnHidden(x -> onWindowClose());
            }
            else if (stage != null)
            {
                stage.setOnHidden(null);
            }
        });
    }

    public void setSimulationParameters(SimulationParameters parameters)
    {
        this.parameters = parameters;
    }

    public void startSimulation()
    {
        lastTime = -1;
        snapshotDisplayDuration = 0.1;
        snapshotDisplayTime = 0.0;
        isPaused = false;
        uiHasTurn = false;

        long simulationDurationSeconds = parameters.getDuration() * SimulationConstants.HOURS_TO_SECONDS;

        simulationDuration.setText(UIUtil.timeToString(simulationDurationSeconds));
        tickAnimationTimer.start();

        Simulation simulation = new Simulation(parameters, true);
        synchronisation = simulation.getSynchronisation();

        Thread simulationThread = new Thread(simulation);
        simulationThread.start();
    }

    private void tick(double deltaTime)
    {
        if (synchronisation.getIsSimulationDone().get()) 
        {
            tickAnimationTimer.stop();

            if (!synchronisation.getIsSimulationCancelled().get())
                Platform.runLater(this::finishSimulation);

            return;
        }
        
        if (synchronisation.getUISemaphore().tryAcquire())
        {
            update();
            uiHasTurn = true;
        }

        if (!uiHasTurn)
            return;
        
        if (snapshotDisplayTime > snapshotDisplayDuration)
        {
            uiHasTurn = false;
            snapshotDisplayTime = 0.0;
            synchronisation.getSimulationSemaphore().release();
        }
        else if (!isPaused)
        {
            snapshotDisplayTime += deltaTime;
        }
    }


    private void update() 
    {
        SimulationState state = synchronisation.getState();

        simulationTime.setText(UIUtil.timeToString(state.getCurrentTime()));
        
        updateRunways(state.getRunways());
        updateEventsList(synchronisation.getMessageQueue());
        updateTakeoffQueue(state.getTakeoffQueue());
        updateHoldingQueue(state.getHoldingQueue());
        updateFlightTable(synchronisation.getTakeoffSchedule(), synchronisation.getLandingSchedule());
        updateLiveStats(synchronisation.getStatistics());
    }

    private void updateEventsList(Queue<Message> messageQueue)
    {
        Message message = messageQueue.poll();

        while (message != null) 
        {
            if (eventsList.getChildren().size() >= 100)
                eventsList.getChildren().remove(eventsList.getChildren().size()-1);

            switch (message.getSource()) 
            {
                case "Simulation":
                    eventsList.getChildren().add(0, MessageViewFactory.create(message));       
                    break;
                case "Simulation Error":
                    ErrorNotification error = new ErrorNotification();
                    error.show(stage, message.getMessage(), 3000);
                    break;
                default:
                    break;
            }

            message = messageQueue.poll();    
        }
    }

    private void updateLiveStats(SimulationStatistics statsObj) {
        String[] stats = {String.format("%.2f", statsObj.getAverageHoldingQueueSize()), String.format("%.2f", statsObj.getAverageTakeoffQueueSize()), String.valueOf(statsObj.getTotalValidAircrafts()), String.valueOf(statsObj.getMaxHoldingSize()), String.valueOf(statsObj.getMaxTakeOffSize())};
        Label[] labels = {avgHolding, avgTakeoff, totalAircraft, hQueueSize, tQueueSize};
        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(stats[i]);
        }
    }

    private void updateTakeoffQueue(IndexedQueue<Aircraft> queue)
    {
        takeoffQueue.getChildren().clear();

        for (Aircraft aircraft : queue)
            takeoffQueue.getChildren().add(new AircraftView(aircraft, 270));

        takeoffQueueModal.setAircraftQueue(queue);
    }

    private void updateHoldingQueue(IndexedPriorityQueue<Aircraft> priorityQueue)
    {
        holdingQueue.getChildren().clear();

        ArrayList<Aircraft> priorityQueueCopy = new ArrayList<>(priorityQueue);

        priorityQueueCopy.sort(Comparator.naturalOrder());

        for (Aircraft aircraft : priorityQueueCopy)
            holdingQueue.getChildren().add(new AircraftView(aircraft, 0));

        holdingQueueModal.setAircraftQueue(priorityQueueCopy);
    }

    private void updateRunways(List<Runway> runways)
    {
        int currentSize = runwayViews.size();
        int newSize = runways.size();

        if (newSize > currentSize)
        {
            for (int i = currentSize; i < newSize; i++)
            {
                Runway runway = runways.get(i);
                String runwayName = runway.getRunwayName();

                RunwayView runwayView = new RunwayView(runway, runwayName);
                
                runwayView.setOnMouseClicked(e -> showRunwayModal(runway, runwayName));
                
                runwayViews.add(runwayView);
                runwayBoxes.getChildren().add(runwayView);
            }
        }
        else if (newSize < currentSize)
        {
            for (int i = newSize; i < currentSize; i++)
            {
                runwayViews.remove(runwayViews.size() - 1);
                runwayBoxes.getChildren().remove(runwayViews.size() - 1);
            }
        }

        for (int i = 0; i < newSize; i++)
            runwayViews.get(i).setRunway(runways.get(i));
    }

    private void updateFlightTable(Schedule<Aircraft> takeoffSchedule, Schedule<Aircraft> landingSchedule)
    {
        if (isFlightTableInitialized)
        {
            flightTable.refresh();
            return;
        }
        List<Aircraft> takeoffAircrafts = takeoffSchedule.getSchedule();
        List<Aircraft> landingAircrafts = landingSchedule.getSchedule();

        flightTable.getItems().clear();
        flightTable.getItems().addAll(takeoffAircrafts);
        flightTable.getItems().addAll(landingAircrafts);
        flightTable.sortRowsByScheduledTime(SortType.ASCENDING);

        isFlightTableInitialized = true;
    }

    private void onSpeedValueChange(double value)
    {
        double duration = Math.pow(1 - value, 3);
        snapshotDisplayDuration = Math.max(duration, 0);
    }

    private void quitToStartScreen()
    {
        try
        {
            App.goToFXMLScreen(App.START_SCRREN_FXML_PATH);
            tickAnimationTimer.stop();
            synchronisation.cancelSimulation();
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void transitionToFastMode()
    {
        try
        {
            FastModeScreenController controller = App.goToFXMLScreen(App.FAST_MODE_SCRREN_FXML_PATH);
            tickAnimationTimer.stop();
            controller.setSynchronisation(synchronisation);
            synchronisation.disableSynchronisation();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void showHoldingQueueModal(MouseEvent event) {
        holdingQueueModal.showAndWait();
    }

    @FXML
    private void showTakeoffQueueModal(MouseEvent event) {
        takeoffQueueModal.showAndWait();
    }

    @FXML
    private void showSettingsModal()
    {
        settingsModal.showAndWait();
    }

    private void showRunwayModal(Runway runway, String runwayName)
    {
        if (isPaused)
        {
            runwayModal.setRunway(runway);
            runwayModal.setTitle(runwayName);
            runwayModal.setCommandQueue(synchronisation.getCommandQueue());
            runwayModal.showAndWait();
        }
    }

    @FXML
    private void pause(ActionEvent event) 
    {
        isPaused = !isPaused;

        String text = isPaused ? "Unpause" : "  Pause  ";
        String style = isPaused ? "unpause" : "pause";

        pauseBtn.setText(text);
        pauseBtn.getStyleClass().setAll(style);

        for (RunwayView runwayView : runwayViews)
            runwayView.setIsPaused(isPaused);
    }

    @FXML
    private void help(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to use the simulation:");
        alert.setContentText(
                "Events and statistics show live updates of current simulation status\n" +
                        "Flight Queue shows details about aircraft entering the queues\n" +
                        "Click on queues to view aircraft details\n" +
                        "Click on runways to modify mode and status\n" +
                        "Use settings to change simulation speed\n " +
                        "Red and yellow aircraft indicate severe and mild emergancies respectively\n"
                        );
        ImageView icon = new ImageView(new Image(String.valueOf(this.getClass().getResource("/ui/images/plane.png"))));
        icon.setFitHeight(60);
        icon.setFitWidth(60);
        icon.setRotate(315);
        alert.getDialogPane().setGraphic(icon);
        alert.showAndWait();
    }

    private void finishSimulation() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Simulation Complete");
        alert.setHeaderText("Simulation Complete");
        alert.setContentText(null);
        ButtonType getStats = new ButtonType("Get Statistics");

        alert.getButtonTypes().setAll(getStats);
        Button finish = (Button) alert.getDialogPane().lookupButton(getStats);
        finish.setOnAction(e -> toReportScreen());
        alert.showAndWait();

        statsSummary.setVisible(true);
    }

    @FXML
    private void statSummary() {
        toReportScreen();
    }

    private void toReportScreen() {
        try 
        {
            ReportsController controller = App.goToFXMLScreen(App.REPORT_SCRREN_FXML_PATH);
            FinalSimulationStats finalSimulationStats = new FinalSimulationStats(synchronisation.getStatistics());
            controller.setFinalSimulationStatistics(finalSimulationStats);
        }
        catch (IOException er) 
        {
            er.printStackTrace();
        }
    }

    private void onWindowClose()
    {
        if (synchronisation != null)
            synchronisation.cancelSimulation();
    }
}
