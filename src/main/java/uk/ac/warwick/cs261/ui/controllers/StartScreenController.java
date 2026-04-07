package uk.ac.warwick.cs261.ui.controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import uk.ac.warwick.cs261.App;
import uk.ac.warwick.cs261.simulation.SimulationParameters;
import uk.ac.warwick.cs261.simulation.ValidationResult;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.ui.factories.RunwayParametersCardFactory;

public class StartScreenController {
    @FXML
    private VBox runwayCardList;

    // General settings
    @FXML
    private TextField durationField;
    @FXML
    private TextField inboundFlowField;
    @FXML
    private TextField outboundFlowField;

    // Aircraft emergency settings
    @FXML
    private TextField mechanicalFailureField;
    @FXML
    private TextField healthEmergencyField;
    @FXML
    private TextField healthEmergencyEscalationTimeMeanField;

    // Runway closure settings
    @FXML
    private TextField runwayInspectionDurationMeanField;
    @FXML
    private TextField runwaySnowClearanceDurationMeanField;
    @FXML
    private TextField runwayEquipmentFailureDurationMeanField;
    @FXML
    private TextField runwayInspectionProbabilityField;
    @FXML
    private TextField runwaySnowClearanceProbabilityField;
    @FXML
    private TextField runwayEquipmentFailureProbabilityField;

    // Advanced settings
    @FXML
    private TextField rngSeedField;
    @FXML
    private TextField departureDelayField;
    @FXML
    private TextField minFuelField;
    @FXML
    private TextField maxFuelField;
    @FXML
    private TextField maxDelayField;
    @FXML
    private TextField lowFuelField;

    @FXML
    private Label errorLabel;
    @FXML
    private HBox errorBox;

    private final ObjectMapper mapper = new ObjectMapper();
    private SimulationParameters parameters = new SimulationParameters();

    private final LinkedHashMap<RunwayParameters, RunwayParametersCardController> runwayCards = new LinkedHashMap<>();

    private TextField[] textFields;
    private Boolean emptyInputFlag;

    @FXML
    private void initialize() {
        textFields = new TextField[] {durationField, inboundFlowField, outboundFlowField, departureDelayField, minFuelField, maxFuelField, mechanicalFailureField, healthEmergencyField, maxDelayField, lowFuelField, healthEmergencyEscalationTimeMeanField, runwayInspectionDurationMeanField, runwaySnowClearanceDurationMeanField, runwayEquipmentFailureDurationMeanField, runwayInspectionProbabilityField, runwaySnowClearanceProbabilityField, runwayEquipmentFailureProbabilityField};
        emptyInputFlag = false;
    }

    @FXML
    private void onSave(ActionEvent e) {
        try {
            updateSimulationParametersFromUI();
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Simulation Config");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName("simulation_config.json");

            File file = chooser.showSaveDialog(null);
            if (file != null) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, parameters);
                showInfo("Saved", "Configuration saved successfully.");
            }
        } catch (Exception ex) {
            showError("Save Failed", ex.getMessage());
        }
    }

    @FXML
    private void onLoad(ActionEvent e) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Load Simulation Config");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

            File file = chooser.showOpenDialog(null);
            if (file != null) {
                parameters = mapper.readValue(file, SimulationParameters.class);
                updateUIFromSimulationParameters();
                showInfo("Loaded", "Configuration restored.");
            }
        } catch (IOException ex) {
            showError("Load Failed", ex.getMessage());
        }
    }


    @FXML
    private void onStart() {
        try {
            updateSimulationParametersFromUI();

            ValidationResult errorInfo = parameters.validate();
            ArrayList<String> errorMessages = errorInfo.messages;
            ArrayList<Integer> fieldIndex = errorInfo.textFieldIndex;

            // When the user enters accepted values, start simulation  
            if (emptyInputFlag == false && errorMessages.size() == 0) {
                SimulationController controller = App.goToFXMLScreen(App.SIMULATION_SCRREN_FXML_PATH);
                controller.setSimulationParameters(parameters);
                controller.startSimulation();
            } else {
            // User enters unaccepted values, displays error message
                errorLabel.setText("\u2023 " + String.join("\n\u2023", errorMessages));
                if (errorMessages.size() > 0) errorBox.setVisible(true);
                for (int i=0; i<textFields.length; i++) {
                    if (fieldIndex.contains(i)) {
                        textFields[i].setStyle("-fx-border-color: red;");
                    } else {
                        textFields[i].setStyle("-fx-border-color:  #88C1EB;");
                    }
                }
            }

            // User enters empty value, continue to simulation prompt
            if (emptyInputFlag == true && errorMessages.size() == 0) {
                errorBox.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Empty Values Detected");
                alert.setContentText("Default values used where empty values detected");
                ButtonType continueBtn = new ButtonType("Continue To Simulation");
                alert.getButtonTypes().setAll(ButtonType.CANCEL, continueBtn);
                Button continueToSim = (Button) alert.getDialogPane().lookupButton(continueBtn);
                continueToSim.setOnAction(e -> toSim());
                alert.showAndWait();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toSim() {
        try {
        SimulationController controller = App.goToFXMLScreen(App.SIMULATION_SCRREN_FXML_PATH);
            controller.setSimulationParameters(parameters);
            controller.startSimulation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddRunway(ActionEvent e) {
        if (runwayCards.size() < 10) {
            RunwayParameters runwayParameters = new RunwayParameters(RunwayMode.MIXED, RunwayStatus.FREE, 2000);
            addCard(runwayParameters, runwayCards.size() + 1);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Cannot Add Runway");
            alert.setContentText("Maximum number of runways reached");
            alert.showAndWait();
        }
    }

    @FXML
    private void onHelp(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to use the simulation");
        alert.setContentText(
                "Add runways on the left.\n" +
                        "Enter parameters on the right.\n" +
                        "Use Save/Load to persist configs.\n" +
                        "Press Start to run the simulation.");

        ImageView icon = new ImageView(
                new Image(String.valueOf(getClass().getResource("/ui/images/plane.png"))));
        icon.setFitWidth(60);
        icon.setFitHeight(60);
        icon.setRotate(315);
        alert.getDialogPane().setGraphic(icon);
        alert.showAndWait();
    }

    private void updateUIFromSimulationParameters() {
        // General
        durationField.setText(String.valueOf(parameters.getDuration()));
        inboundFlowField.setText(String.valueOf(parameters.getInflow()));
        outboundFlowField.setText(String.valueOf(parameters.getOutflow()));

        // Aircraft emergencies
        mechanicalFailureField.setText(String.valueOf(parameters.getMechanicalFailureProbability() * 100.0));
        healthEmergencyField.setText(String.valueOf(parameters.getPassengerHealthProbability() * 100.0));
        healthEmergencyEscalationTimeMeanField.setText(String.valueOf(parameters.getHealthEscalationTimeMean()));

        // Runway closures
        runwayInspectionDurationMeanField.setText(String.valueOf(parameters.getRunwayInspectionDurationMean()));
        runwaySnowClearanceDurationMeanField.setText(String.valueOf(parameters.getRunwaySnowClearanceDurationMean()));
        runwayEquipmentFailureDurationMeanField
                .setText(String.valueOf(parameters.getRunwayEquipmentFailureDurationMean()));
        runwayInspectionProbabilityField.setText(String.valueOf(parameters.getRunwayInspectionProbability() * 100.0));
        runwaySnowClearanceProbabilityField
                .setText(String.valueOf(parameters.getRunwaySnowClearanceProbability() * 100.0));
        runwayEquipmentFailureProbabilityField
                .setText(String.valueOf(parameters.getRunwayEquipmentFailureProbability() * 100.0));

        // Advanced
        rngSeedField.setText(String.valueOf(parameters.getRngSeed()));
        departureDelayField.setText(String.valueOf(parameters.getDelayStdDev()));
        minFuelField.setText(String.valueOf(parameters.getMinRemainingFuel()));
        maxFuelField.setText(String.valueOf(parameters.getMaxRemainingFuel()));
        maxDelayField.setText(String.valueOf(parameters.getMaxDelay()));
        lowFuelField.setText(String.valueOf(parameters.getLowFuelThreshold()));

        // Runways
        runwayCardList.getChildren().clear();
        runwayCards.clear();

        List<RunwayParameters> loaded = parameters.getRunwayParameters();
        if (loaded != null) {
            for (int i = 0; i < loaded.size(); i++)
                addCard(loaded.get(i), i + 1);
        }
    }

    private void updateSimulationParametersFromUI() {
        // General
        parameters.setDuration(getTextOrDefaultLong(durationField));
        parameters.setInflow(getTextOrDefaultLong(inboundFlowField));
        parameters.setOutflow(getTextOrDefaultLong(outboundFlowField));

        // Aircraft emergencies
        parameters.setMechanicalFailureProbability(getTextOrDefaultDouble(mechanicalFailureField) / 100.0);
        parameters.setPassengerHealthProbability(getTextOrDefaultDouble(healthEmergencyField) / 100.0);
        parameters.setHealthEscalationTimeMean(getTextOrDefaultDouble(healthEmergencyEscalationTimeMeanField));

        // Runway closures
        parameters.setRunwayInspectionDurationMean(
            getTextOrDefaultDouble(runwayInspectionDurationMeanField));
        parameters.setRunwaySnowClearanceDurationMean(
            getTextOrDefaultDouble(runwaySnowClearanceDurationMeanField));
        parameters.setRunwayEquipmentFailureDurationMean(
            getTextOrDefaultDouble(runwayEquipmentFailureDurationMeanField));
        parameters.setRunwayInspectionProbability(
            getTextOrDefaultDouble(runwayInspectionProbabilityField) / 100.0);
        parameters.setRunwaySnowClearanceProbability(
            getTextOrDefaultDouble(runwaySnowClearanceProbabilityField) / 100.0);
        parameters.setRunwayEquipmentFailureProbability(
            getTextOrDefaultDouble(runwayEquipmentFailureProbabilityField) / 100.0);

        // Advanced
        parameters.setRngSeed(getTextOrDefaultLong(rngSeedField));
        parameters.setDelayStdDev(getTextOrDefaultDouble(departureDelayField));
        parameters.setMinRemainingFuel(getTextOrDefaultDouble(minFuelField));
        parameters.setMaxRemainingFuel(getTextOrDefaultDouble(maxFuelField));
        parameters.setMaxDelay(getTextOrDefaultLong(maxDelayField));
        parameters.setLowFuelThreshold(getTextOrDefaultLong(lowFuelField));

        parameters.setRunwayParameters(new ArrayList<>(runwayCards.keySet()));
    }

    private long getTextOrDefaultLong(TextField text)
    {
        try
        {
            if (text.getText().equals("")) {
                emptyInputFlag = true;
                return Long.parseLong(text.getPromptText());
            }
            else return Long.parseLong(text.getText());
        }
        catch (Exception e)
        {
            // -1 is invalid for all parameters so this allows us to give back an arror message
            emptyInputFlag = true;
            return (long) -1;
        }
       
    }

    private double getTextOrDefaultDouble(TextField text)
    {
        try
        {
            if (text.getText().equals("")) {
                emptyInputFlag = true;
                return Double.parseDouble(text.getPromptText());
            }
            else return Double.parseDouble(text.getText());
        }
        catch (Exception e)
        {
            // -1 is invalid for all parameters so this allows us to give back an arror message
            return (double) -1.0;
        }
       
    }


    private void addCard(RunwayParameters runwayParameters, int index) {
        try {
            RunwayParametersCardController controller = RunwayParametersCardFactory.create(runwayParameters, index,
                    this::deleteRunway);
            runwayCards.put(runwayParameters, controller);
            runwayCardList.getChildren().add(controller.getRoot());
        } catch (IOException ex) {
            showError("UI Error", "Could not load runway card: " + ex.getMessage());
        }
    }

    private void deleteRunway(RunwayParameters runwayParameters) {
        RunwayParametersCardController controller = runwayCards.remove(runwayParameters);

        if (controller == null)
            return;

        runwayCardList.getChildren().remove(controller.getRoot());

        int i = 1;
        for (RunwayParametersCardController c : runwayCards.values())
            c.setIndex(i++);
    }

    @FXML
    private void toReportScreen() {
        try 
        {
            App.goToFXMLScreen(App.REPORT_SCRREN_FXML_PATH);
        }
        catch (IOException er) 
        {
            er.printStackTrace();
        }
    }

    private void showInfo(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
