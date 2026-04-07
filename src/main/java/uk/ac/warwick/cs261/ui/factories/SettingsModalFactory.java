package uk.ac.warwick.cs261.ui.factories;

import javafx.fxml.FXMLLoader;
import uk.ac.warwick.cs261.ui.controllers.modals.SettingsModalController;

import java.io.IOException;
import java.net.URL;

public class SettingsModalFactory
{
    private static final String FXML_PATH = "/ui/fxml/settings_modal.fxml";
    private static final URL FXML_URL = SettingsModalFactory.class.getResource(FXML_PATH);

    public static SettingsModalController create() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(FXML_URL);
        loader.load();

        return loader.getController();
    }

    private SettingsModalFactory() {}
}
