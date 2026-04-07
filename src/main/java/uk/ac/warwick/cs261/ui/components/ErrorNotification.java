package uk.ac.warwick.cs261.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Error notification class for failed runway changes.
 */
public class ErrorNotification 
{

    private Stage stage;

    public void show(Stage ownerWindow, String message, int displayMs) {
        stage = new Stage();
        stage.initOwner(ownerWindow);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        Label icon = new Label("\u26A0");
        icon.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 20px;");

        Label title = new Label("Error");
        title.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox titleRow = new HBox(6, icon, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #FFCCCC; -fx-font-size: 13px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(230);

        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #FF6B6B;
            -fx-font-size: 12px;
            -fx-cursor: hand;
            -fx-padding: 0 0 8 0;
        """);
        closeBtn.setOnAction(e -> dismiss());

        HBox topBar = new HBox(titleRow, closeBtn);
        HBox.setHgrow(titleRow, Priority.ALWAYS);
        topBar.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(6, topBar, msgLabel);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setMaxWidth(280);
        card.setStyle("""
            -fx-background-color: #1E0A0A;
            -fx-background-radius: 8;
            -fx-border-color: #8B0000;
            -fx-border-width: 1.5;
            -fx-border-radius: 8;
            -fx-effect: dropshadow(gaussian, rgba(180,0,0,0.35), 12, 0, 2, 4);
        """);

        Region accentBar = new Region();
        accentBar.setPrefWidth(5);
        accentBar.setStyle("""
            -fx-background-color: #CC0000;
            -fx-background-radius: 8 0 0 8;
        """);

        HBox wrapper = new HBox(accentBar, card);
        wrapper.setStyle("-fx-background-color: transparent;");

        StackPane root = new StackPane(wrapper);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.setX(ownerWindow.getX() + 10);
        stage.setY(ownerWindow.getY() + 10);

        ownerWindow.xProperty().addListener((obs, o, n) ->
            stage.setX(n.doubleValue() + 10));
        ownerWindow.yProperty().addListener((obs, o, n) ->
            stage.setY(n.doubleValue() + 10));

        stage.show();

        PauseTransition pause = new PauseTransition(Duration.millis(displayMs));
        pause.setOnFinished(e -> dismiss());
        pause.play();
    }

    private void dismiss() {
        if (stage == null || !stage.isShowing()) return;

        FadeTransition fade = new FadeTransition(Duration.millis(400),
                                                  stage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> stage.close());
        fade.play();
    }
}