package uk.ac.warwick.cs261.ui.controllers.modals;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class ModalController
{
    @FXML
    private Parent root;

    private Stage stage;
    private Scene scene;

    public boolean getIsShowing()
    {
        return stage != null && stage.isShowing();
    }

    public void showAndWait()
    {
        if (stage != null)
            return;

        if (scene == null)
            scene = new Scene(root);

        stage = new Stage();
        stage.setTitle(getTitle());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnHidden(e -> stage = null);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.showAndWait();
    }

    public void close()
    {
        if (stage == null)
            return;

        stage.close();
        stage = null;
    }

    public abstract String getTitle();
    public abstract void setTitle(String title);
}