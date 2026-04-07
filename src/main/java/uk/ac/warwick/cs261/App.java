package uk.ac.warwick.cs261;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main App class for the Dorset Software Airport Simulation.
 * The software begins execution from here.
 */
public class App extends Application 
{
    public static final String START_SCRREN_FXML_PATH = "/ui/fxml/start_screen.fxml";
    public static final String SIMULATION_SCRREN_FXML_PATH = "/ui/fxml/simulation_screen.fxml";
    public static final String FAST_MODE_SCRREN_FXML_PATH = "/ui/fxml/fast_mode_screen.fxml";
    public static final String REPORT_SCRREN_FXML_PATH = "/ui/fxml/report_screen.fxml"; 

    // JavaFX Scene
    private static Scene SCENE;

    @Override
    public void start(Stage stage) throws IOException 
    {
        goToFXMLScreen(START_SCRREN_FXML_PATH);
        
        // Configure the stage
        stage.setTitle("Dorset Software Airport Simulator");
        stage.setScene(SCENE);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.show();
    }

    /**
     * Switches the root of the JavaFX Scene graph to change screens
     * 
     * @param fxml FXML file address string
     * @throws IOException
     */
    public static <T> T goToFXMLScreen(String fxml) throws IOException 
    {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
        Parent root = loader.load();

        if (SCENE == null)
            SCENE = new Scene(root, 1200, 700);
        else    
            SCENE.setRoot(root);

        return loader.getController();
    }
}