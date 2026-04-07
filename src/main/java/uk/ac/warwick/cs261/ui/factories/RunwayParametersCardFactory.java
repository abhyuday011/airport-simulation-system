package uk.ac.warwick.cs261.ui.factories;

import javafx.fxml.FXMLLoader;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;
import uk.ac.warwick.cs261.ui.controllers.RunwayParametersCardController;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class RunwayParametersCardFactory
{
    private static final String FXML_PATH = "/ui/fxml/runway_parameters_card.fxml";
    private static final URL FXML_URL = RunwayParametersCardFactory.class.getResource(FXML_PATH);

    public static RunwayParametersCardController create(RunwayParameters parameters, int index, Consumer<RunwayParameters> onDeleteCallback) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(FXML_URL);
        loader.load();

        RunwayParametersCardController controller = loader.getController();
        controller.setRunwayParameters(parameters, index);
        controller.setOnDeleteCallback(onDeleteCallback);

        return controller;
    }

    private RunwayParametersCardFactory() {}
}
