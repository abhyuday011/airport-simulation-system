package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that updates the emergency status of an arriving aircraft currently
 * in the holding queue, triggering a re-insertion so that the queue's priority ordering
 * reflects the new severity.
 *
 * <p>Because the holding queue is a priority queue ordered by {@link AircraftStatus}
 * severity (see {@link Aircraft#compareTo(Aircraft)}), simply mutating the aircraft's
 * status field in place would corrupt the queue's heap invariant. This event therefore
 * removes the aircraft from the queue, updates its status, and re-inserts it, forcing
 * the queue to reposition it according to the new priority.
 *
 * <p>If the aircraft is no longer present in the holding queue when this event fires,
 * because it has already landed or been diverted, the event is treated as a no-op and
 * a {@link MessageSeverity#MEDIUM} informational message is appended to the message queue.
 *
 * <p>This event is assigned {@link #order} {@code 0}
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code aircraft} and {@code status} must not be {@code null}.</li>
 *   <li>This event is only meaningful for arriving aircraft managed by the holding queue;
 *       applying it to a departing aircraft will produce no useful state change.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class AircraftStatusChangeEvent extends SimulationEvent
{
    /** The arriving aircraft whose {@link AircraftStatus} is to be updated. */
    private final Aircraft aircraft;

    /** The new {@link AircraftStatus} to assign to {@link #aircraft} upon execution. */
    private final AircraftStatus status;

    /**
     * Constructs an {@code AircraftStatusChangeEvent} scheduled to fire at the given tick.
     *
     * @param time     the simulation tick at which to apply the status change; must be
     *                 non-negative
     * @param aircraft the aircraft whose status is to be changed; must not be {@code null}
     * @param status   the new status to assign; must not be {@code null}
     */
    public AircraftStatusChangeEvent(long time, Aircraft aircraft, AircraftStatus status)
    {
        super(time);
        this.aircraft = aircraft;
        this.status = status;
        this.order = 0;
    }

    /**
     * Removes the aircraft from the holding queue, updates its status, and re-inserts it
     * so that the queue's priority ordering reflects the new {@link AircraftStatus} severity.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>Check whether {@link #aircraft} is currently present in the holding queue via
     *       {@link uk.ac.warwick.cs261.datastructures.IndexedPriorityQueue#contains}.</li>
     *   <li>If present: remove it, set its status to {@link #status}, re-add it to force
     *       repositioning within the priority queue, record the emergency with
     *       {@link SimulationStatistics#recordEmergency(AircraftStatus, long)}, and append
     *       a status-specific {@link Message} via {@link #getMessage()} if one is defined
     *       for the new status. Return {@code true}.</li>
     *   <li>If absent: append a {@link MessageSeverity#MEDIUM} message indicating the
     *       aircraft has already landed or been diverted, and return {@code false} without
     *       modifying any state.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to access the holding queue;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to notify of the emergency;
     *                     must not be {@code null}
     * @param messageQueue the queue to which a status or informational message is appended;
     *                     must not be {@code null}
     * @return {@code true} if the aircraft was present in the holding queue and its status
     *         was updated; {@code false} if the aircraft was no longer in the queue
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        if (state.getHoldingQueue().contains(aircraft))
        {
            state.getHoldingQueue().remove(aircraft);
            aircraft.setStatus(status);
            state.getHoldingQueue().add(aircraft);

            statistics.recordEmergency(status, time);

            Message message = getMessage();

            if (message != null)
                messageQueue.add(message);

            return true;
        }

        String message = String.format("%s has landed or diverted", aircraft.getCallSign());
        messageQueue.add(new Message(time, "Simulation: ", message, MessageSeverity.MEDIUM));

        return false;
    }

    /**
     * Constructs a human-readable {@link Message} appropriate for the new
     * {@link AircraftStatus}, or returns {@code null} for statuses that do not warrant
     * a dedicated notification.
     *
     * <p>A {@link MessageSeverity#MEDIUM} message is produced for
     * {@link AircraftStatus#LOW_FUEL} and {@link AircraftStatus#PASSENGER_HEALTH_SEVERE};
     * all other statuses return {@code null} and therefore produce no message entry.
     *
     * @return a {@link Message} describing the emergency condition, or {@code null} if
     *         the new status does not have an associated notification
     */
    private Message getMessage()
    {
        String message;

        switch (status)
        {
            case LOW_FUEL:
                message = String.format("%s fuel low", aircraft.getCallSign());
                return new Message(time, "Simulation", message, MessageSeverity.MEDIUM);
            case PASSENGER_HEALTH_SEVERE:
                message = String.format("%s severe passenger health emergency", aircraft.getCallSign());
                return new Message(time, "Simulation", message, MessageSeverity.MEDIUM);
            default:
                return null;
        }
    }
}