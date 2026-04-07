package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that admits an arriving aircraft into the holding queue and records
 * its initial status and queue-entry time.
 *
 * <p>When this event fires, the aircraft is inserted into the simulation's priority-based
 * holding queue via {@link SimulationState#getHoldingQueue()}. Setting the aircraft's
 * {@link Aircraft#setQueueEnterTime(long) queueEnterTime} immediately after insertion
 * ensures that the timestamp reflects the tick at which the aircraft entered the queue,
 * which is used both for wait-time calculation and for priority tiebreaking. If the
 * aircraft arrives with a non-{@link AircraftStatus#OK} status the emergency is
 * immediately recorded with {@link SimulationStatistics#recordEmergency(AircraftStatus, long)}.
 *
 * <p>If {@link SimulationState#getHoldingQueue()} rejects the aircraft, for example
 * because it is already present, the event returns {@code false} and no state is
 * modified.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code aircraft} and {@code status} must not be {@code null}.</li>
 *   <li>This event sets {@link #order} to the default value of {@code 0}; it therefore
 *       fires after early-order same-tick events such as {@link RunwayFreeEvent}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class HoldingQueueEnterEvent extends SimulationEvent
{
    /** The arriving aircraft to be admitted into the holding queue. */
    private final Aircraft aircraft;

    /**
     * The initial {@link AircraftStatus} to assign to {@link #aircraft} upon admission;
     * a value other than {@link AircraftStatus#OK} causes an emergency to be recorded
     * immediately via {@link SimulationStatistics#recordEmergency(AircraftStatus, long)}.
     */
    private final AircraftStatus status;

    /**
     * Constructs a {@code HoldingQueueEnterEvent} scheduled to fire at the given tick.
     *
     * @param time     the simulation tick at which the aircraft should enter the holding
     *                 queue; must be non-negative
     * @param aircraft the arriving aircraft to admit; must not be {@code null}
     * @param status   the initial operational or emergency status to assign to the aircraft
     *                 upon admission; must not be {@code null}
     */
    public HoldingQueueEnterEvent(long time, Aircraft aircraft, AircraftStatus status)
    {
        super(time);
        this.aircraft = aircraft;
        this.status = status;
    }

    /**
     * Admits the aircraft into the holding queue, stamps its queue-entry time, assigns
     * its initial status, and updates holding-queue statistics.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>Attempt to add {@link #aircraft} to the holding queue. If the queue rejects
     *       the aircraft (e.g. it is already present), return {@code false} immediately
     *       without modifying any further state.</li>
     *   <li>Set the aircraft's {@link Aircraft#setQueueEnterTime(long) queueEnterTime}
     *       to {@link #time} so that downstream wait-time and priority calculations have
     *       an accurate entry timestamp.</li>
     *   <li>Set the aircraft's {@link AircraftStatus} to {@link #status}.</li>
     *   <li>Append a {@link MessageSeverity#LOW} log message confirming the aircraft's
     *       call sign and queue admission.</li>
     *   <li>If {@link #status} is not {@link AircraftStatus#OK}, record the emergency
     *       via {@link SimulationStatistics#recordEmergency(AircraftStatus, long)}.</li>
     *   <li>Update holding-queue statistics via
     *       {@link SimulationStatistics#updateHoldingQueueStats(SimulationState)}.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to access the holding queue;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to update on successful admission;
     *                     must not be {@code null}
     * @param messageQueue the queue to which a confirmation log message is appended on
     *                     success; must not be {@code null}
     * @return {@code true} if the aircraft was successfully admitted into the holding queue;
     *         {@code false} if the queue rejected the aircraft
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = state.getHoldingQueue().add(aircraft);
        aircraft.setQueueEnterTime(time);
        aircraft.setStatus(status);

        if (result)
        {
            String message = String.format("%s entered holding queue", aircraft.getCallSign());
            messageQueue.add(new Message(time, "Simulation", message, MessageSeverity.LOW));

            if (status != AircraftStatus.OK)
                statistics.recordEmergency(status, time);

            statistics.updateHoldingQueueStats(state);
        }

        return result;
    }
}