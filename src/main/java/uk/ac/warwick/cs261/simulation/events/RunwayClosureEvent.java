package uk.ac.warwick.cs261.simulation.events;

import java.util.PriorityQueue;
import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that closes a runway for a fixed duration and schedules its
 * subsequent re-opening by injecting a {@link RunwayStatusChangeEvent} into the event
 * queue.
 *
 * <p>This event is a specialisation of {@link RunwayStatusChangeEvent} for closure-type
 * statuses: {@link RunwayStatus#INSPECTION}, {@link RunwayStatus#SNOW_CLEARANCE}
 *
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code targetStatus} must not be {@link RunwayStatus#FREE} or
 *       {@link RunwayStatus#OCCUPIED}; passing either will cause the constructor to throw
 *       {@link IllegalArgumentException}.</li>
 *   <li>{@code runway}, {@code targetStatus}, and {@code eventQueue} must not be
 *       {@code null}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class RunwayClosureEvent extends RunwayStatusChangeEvent
{
    /**
     * The number of simulation ticks for which the runway remains closed; a
     * {@link RunwayStatusChangeEvent} targeting {@link RunwayStatus#FREE} is scheduled
     * at {@code time + closureDuration}.
     */
    private long closureDuration;

    /**
     * The shared event queue into which the re-opening {@link RunwayStatusChangeEvent}
     * is injected upon successful closure.
     */
    private PriorityQueue<SimulationEvent> eventQueue;

    /**
     * Constructs a {@code RunwayClosureEvent} that will close the given runway for the
     * specified duration starting at the given tick.
     *
     * @param time           the simulation tick at which the closure should begin; must be
     *                       non-negative
     * @param closureDuration the number of ticks the runway will remain closed; must be
     *                        positive
     * @param runway          the runway to close; must not be {@code null}
     * @param targetStatus    the closure status to apply; must not be
     *                        {@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED}
     * @param eventQueue      the shared simulation event queue into which the re-opening
     *                        event will be injected; must not be {@code null}
     * @throws IllegalArgumentException if {@code targetStatus} is
     *                                  {@link RunwayStatus#FREE} or
     *                                  {@link RunwayStatus#OCCUPIED}
     */
    public RunwayClosureEvent(long time, long closureDuration, Runway runway, RunwayStatus targetStatus, PriorityQueue<SimulationEvent> eventQueue)
    {
        super(time, runway, targetStatus);
        this.order = 0;

        if (targetStatus == RunwayStatus.FREE || targetStatus == RunwayStatus.OCCUPIED)
            throw new IllegalArgumentException("targetStatus for RunwayClosureEvent cannot be RunwayStatus.FREE or RunwayStatus.OCCUPIED");

        this.closureDuration = closureDuration;
        this.eventQueue = eventQueue;
    }

    /**
     * Applies the runway closure, schedules a re-opening event, and updates closure
     * statistics; or, if the closure is refused, clears the aircraft reference and
     * restores the runway to {@link RunwayStatus#FREE}.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>Delegate to
     *       {@link RunwayStatusChangeEvent#execute(SimulationState, SimulationStatistics, Queue)}
     *       to apply {@link #targetStatus} subject to the runway-availability safety
     *       check.</li>
     *   <li>If the parent returns {@code true} (closure accepted):
     *     <ul>
     *       <li>Clear the runway's aircraft reference via
     *           {@link Runway#setAircraft(Aircraft) setAircraft(null)}.</li>
     *       <li>Inject a {@link RunwayStatusChangeEvent} targeting {@link RunwayStatus#FREE}
     *           at {@code time + closureDuration} into {@link #eventQueue} to schedule
     *           the re-opening.</li>
     *       <li>Increment closure-time and closure-type statistics via
     *           {@link SimulationStatistics#incrementClosureTime(RunwayStatus, long)} and
     *           {@link SimulationStatistics#incrementRunwayClosureType(RunwayStatus)}.</li>
     *       <li>Append a {@link MessageSeverity#MEDIUM} log message naming the runway
     *           and the closure reason.</li>
     *     </ul>
     *   </li>
     *   <li>If the parent returns {@code false} (closure refused by safety check):
     *     <ul>
     *       <li>Clear the runway's aircraft reference regardless.</li>
     *       <li>Explicitly set the runway's status to {@link RunwayStatus#FREE} to ensure
     *           the simulation does not remain in a transiently inconsistent state.</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param state        the current simulation state, used by the parent for the
     *                     availability count; must not be {@code null}
     * @param statistics   the statistics collector to update with closure duration and type
     *                     on success; must not be {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#MEDIUM} closure
     *                     message is appended on success; must not be {@code null}
     * @return {@code true} if the runway was successfully closed and a re-opening event
     *         has been scheduled; {@code false} if the safety check refused the closure
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = super.execute(state, statistics, messageQueue);

        if (result)
        {
            runway.setAircraft(null);
            eventQueue.add(new RunwayStatusChangeEvent(time + closureDuration, runway, RunwayStatus.FREE));

            statistics.incrementClosureTime(this.targetStatus, this.closureDuration);
            statistics.incrementRunwayClosureType(this.targetStatus);

            String msg = String.format("%s closed due to %s", runway.getRunwayName(), targetStatus.toString());
            messageQueue.add(new Message(time, "Simulation", msg, MessageSeverity.MEDIUM));
        }
        else
        {
            runway.setAircraft(null);
            runway.setStatus(RunwayStatus.FREE);
        }

        return result;
    }
}