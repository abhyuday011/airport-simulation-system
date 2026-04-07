package uk.ac.warwick.cs261.ui.factories;

import javafx.fxml.FXMLLoader;
import uk.ac.warwick.cs261.ui.controllers.modals.AircraftQueueModalController;

import java.io.IOException;
import java.net.URL;

public class AircraftQueueModalFactory
{
    private static final String FXML_PATH = "/ui/fxml/aircraft_queue_modal.fxml";
    private static final URL FXML_URL = AircraftQueueModalFactory.class.getResource(FXML_PATH);

    public static AircraftQueueModalController create(String title) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(FXML_URL);
        loader.load();

        AircraftQueueModalController controller = loader.getController();
        controller.setTitle(title);

        return controller;
    }

    private AircraftQueueModalFactory() {}
}