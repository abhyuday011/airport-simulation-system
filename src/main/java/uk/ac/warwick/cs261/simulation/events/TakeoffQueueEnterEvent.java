package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that admits a departing aircraft into the takeoff queue and stamps
 * its queue-entry time.
 *
 * <p>When this event fires, the aircraft is inserted into the simulation's FIFO takeoff
 * queue via {@link SimulationState#getTakeoffQueue()}.Statistics are updated immediately 
 * after a successful admission via {@link SimulationStatistics#updateTakeOffQueueStats(SimulationState)}.
 *
 * <p>If the queue rejects the aircraft, for example because it is already present,
 * the event returns {@code false} and no state is modified.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code aircraft} must not be {@code null}.</li>
 *   <li>This event uses the default {@link SimulationEvent#order} of {@code 0}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class TakeoffQueueEnterEvent extends SimulationEvent
{
    /** The departing aircraft to be admitted into the takeoff queue. */
    private final Aircraft aircraft;

    /**
     * Constructs a {@code TakeoffQueueEnterEvent} scheduled to fire at the given tick.
     *
     * @param time     the simulation tick at which the aircraft should enter the takeoff
     *                 queue; must be non-negative
     * @param aircraft the departing aircraft to admit; must not be {@code null}
     */
    public TakeoffQueueEnterEvent(long time, Aircraft aircraft)
    {
        super(time);
        this.aircraft = aircraft;
    }

    /**
     * Admits the aircraft into the takeoff queue, stamps its queue-entry time, and
     * updates takeoff-queue statistics.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>Attempt to add {@link #aircraft} to the takeoff queue. If the queue rejects
     *       the aircraft, return {@code false} immediately without modifying any further
     *       state.</li>
     *   <li>Set the aircraft's {@link Aircraft#setQueueEnterTime(long) queueEnterTime}
     *       to {@link #time} so that its FIFO priority within the takeoff queue is
     *       correctly established.</li>
     *   <li>Append a {@link MessageSeverity#LOW} log message confirming the aircraft's
     *       call sign and admission.</li>
     *   <li>Update takeoff-queue statistics via
     *       {@link SimulationStatistics#updateTakeOffQueueStats(SimulationState)}.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to access the takeoff queue;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to update on successful admission;
     *                     must not be {@code null}
     * @param messageQueue the queue to which a confirmation log message is appended on
     *                     success; must not be {@code null}
     * @return {@code true} if the aircraft was successfully admitted into the takeoff
     *         queue; {@code false} if the queue rejected the aircraft
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = state.getTakeoffQueue().add(aircraft);
        aircraft.setQueueEnterTime(time);

        if (result)
        {
            String message = String.format("%s entered takeoff queue", aircraft.getCallSign());
            messageQueue.add(new Message(time, "Simulation", message, MessageSeverity.LOW));

            statistics.updateTakeOffQueueStats(state);
        }

        return result;
    }
}