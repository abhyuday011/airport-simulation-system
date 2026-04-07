package uk.ac.warwick.cs261.ui.components;

import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.ui.UIUtil;

/**
 * UI component class that displays the airport flight queue.
 */
public class AircraftTableView extends TableView<Aircraft>
{

    private final TableColumn<Aircraft, String> callsignColumn = new TableColumn<>("Callsign");
    private final TableColumn<Aircraft, Long> scheduledTimeColumn = new TableColumn<>("Scheduled Time");
    private final TableColumn<Aircraft, Long> queueEnterTimeColumn = new TableColumn<>("Queue Enter Time");
    private final TableColumn<Aircraft, Long> actualTimeColumn = new TableColumn<>("Actual Time");
    private final TableColumn<Aircraft, AircraftStatus> statusColumn = new TableColumn<>("Status");

    public AircraftTableView()
    {
        callsignColumn.setCellValueFactory(AircraftTableView::getCallSign);
        callsignColumn.setCellFactory(x -> createCell(Function.identity()));
        callsignColumn.setSortable(false);

        scheduledTimeColumn.setCellValueFactory(AircraftTableView::getScheduledTime);
        scheduledTimeColumn.setCellFactory(x -> createCell(UIUtil::timeToString));
        scheduledTimeColumn.setComparator(AircraftTableView::compareTime);

        queueEnterTimeColumn.setCellValueFactory(AircraftTableView::getQueueEnterTime);
        queueEnterTimeColumn.setCellFactory(x -> createCell(UIUtil::timeToString));
        queueEnterTimeColumn.setComparator(AircraftTableView::compareTime);

        actualTimeColumn.setCellValueFactory(AircraftTableView::getActualTime);
        actualTimeColumn.setCellFactory(x -> createCell(UIUtil::timeToString));
        actualTimeColumn.setComparator(AircraftTableView::compareTime);

        statusColumn.setCellValueFactory(AircraftTableView::getStatus);
        statusColumn.setCellFactory(x -> createCell(AircraftStatus::toString));
        statusColumn.setSortable(false);
        //statusColumn.setComparator(Comparator.comparingInt(s -> s.priority));

        getColumns().addAll(callsignColumn, scheduledTimeColumn, queueEnterTimeColumn, actualTimeColumn, statusColumn);

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void sortRowsByScheduledTime(SortType order)
    {
        sortRowByColumn(scheduledTimeColumn, order);
    }

    public void sortRowsByQueueEnterTime(SortType order)
    {
        sortRowByColumn(queueEnterTimeColumn, order);
    }

    public void sortRowsByActualTime(SortType order)
    {
        sortRowByColumn(actualTimeColumn, order);
    }

    private <T> void sortRowByColumn(TableColumn<Aircraft, T> column, SortType order)
    {
        column.setSortType(order);
        getSortOrder().clear();
        getSortOrder().add(column);
        sort();
    }

    private static ReadOnlyStringWrapper getCallSign(CellDataFeatures<Aircraft, String> data)
    {
        String type = data.getValue().getIsTakeoff() ? "DEPARTING" : "ARRIVING";
        return new ReadOnlyStringWrapper(String.format("%s (%s)", data.getValue().getCallSign(), type));
    }

    private static ReadOnlyObjectWrapper<Long> getScheduledTime(CellDataFeatures<Aircraft, Long> data)
    {
        return new ReadOnlyObjectWrapper<>(data.getValue().getScheduledTime());
    }

    private static ReadOnlyObjectWrapper<Long> getQueueEnterTime(CellDataFeatures<Aircraft, Long> data)
    {
        return new ReadOnlyObjectWrapper<>(data.getValue().getQueueEnterTime());
    }

    private static ReadOnlyObjectWrapper<Long> getActualTime(CellDataFeatures<Aircraft, Long> data)
    {
        return new ReadOnlyObjectWrapper<>(data.getValue().getActualTime());
    }

    private static ReadOnlyObjectWrapper<AircraftStatus> getStatus(CellDataFeatures<Aircraft, AircraftStatus> data)
    {
        return new ReadOnlyObjectWrapper<>(data.getValue().getStatus());
    }

    private static <T> TableCell<Aircraft, T> createCell(Function<T, String> toString)
    {
        return new TableCell<Aircraft, T>()
        {
            @Override
            protected void updateItem(T value, boolean empty)
            {
                super.updateItem(value, empty);

                if (empty || value == null)
                {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(toString.apply(value));

                AircraftStatus status = getTableView().getItems().get(getIndex()).getStatus();
                setStyle(getCellStyle(status));
            }
        };
    }

    private static String getCellStyle(AircraftStatus status)
    {
        String colour = switch (status)
        {
            case MECHANICAL_FAILURE,
                PASSENGER_HEALTH,
                PASSENGER_HEALTH_SEVERE,
                LOW_FUEL   -> "#b07800";
            case CANCELLED,
                DIVERTED   -> "#c0392b";
            case ARRIVED,
                DEPARTED   -> "#5cb85c";
            default         -> "#333333";
        };

        return "-fx-text-fill: " + colour + "; -fx-font-weight: " + "bold;";
    }

    private static int compareTime(long lhs, long rhs)
    {
        if (lhs < 0 && rhs < 0)
            return 0;

        if (lhs < 0)
            return 1;

        if (rhs < 0)
            return -1;


        return Long.compare(lhs, rhs);
    }
}
