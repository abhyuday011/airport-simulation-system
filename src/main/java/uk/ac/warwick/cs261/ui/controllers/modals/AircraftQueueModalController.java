package uk.ac.warwick.cs261.ui.controllers.modals;

import java.util.Collection;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn.SortType;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.ui.components.AircraftTableView;

public class AircraftQueueModalController extends ModalController
{
    @FXML 
    private AircraftTableView flightTable;

    @FXML
    private Label title;
    
    @FXML 
    private Label queueSizeLabel;

    public void setAircraftQueue(Collection<? extends Aircraft> queue)
    {
        flightTable.getItems().setAll(queue);
        flightTable.sortRowsByQueueEnterTime(SortType.ASCENDING);

        int size = queue.size();
        queueSizeLabel.setText(String.format("%d aircraft", size));
    }

    @Override
    public String getTitle() { return title.getText(); }

    @Override
    public void setTitle(String text) { title.setText(text); }
}
