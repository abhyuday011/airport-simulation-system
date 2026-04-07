package uk.ac.warwick.cs261.ui.factories;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import uk.ac.warwick.cs261.ui.controllers.modals.RunwayModalController;

public class RunwayModalFactory
{
    private static final String FXML_PATH = "/ui/fxml/runway_modal.fxml";
    private static final URL FXML_URL = RunwayModalFactory.class.getResource(FXML_PATH);

    public static RunwayModalController create() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(FXML_URL);
        loader.load();

        RunwayModalController controller = loader.getController();
        return controller;
    }

    private RunwayModalFactory() {}
}