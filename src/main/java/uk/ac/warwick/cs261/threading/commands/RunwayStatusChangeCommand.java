package uk.ac.warwick.cs261.threading.commands;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.simulation.events.EventGenerator;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A {@link SimulationCommand} that requests a closure-type status change for a runway,
 * translating a UI-initiated action into a scheduled
 * {@link uk.ac.warwick.cs261.simulation.events.RunwayClosureEvent} at the current
 * simulation tick, or reporting failure immediately if the runway is ineligible.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code status} must be a closure-type {@link RunwayStatus} value; it must not be
 *       {@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED}.</li>
 *   <li>{@code runway} must not be {@code null}.</li>
 *   <li>The command only succeeds if the runway is currently {@link RunwayStatus#FREE};
 *       any other current status causes immediate failure and an error message.</li>
 *   <li>This class is not thread-safe; instances are constructed on the UI thread and
 *       executed on the simulation thread.</li>
 * </ul>
 */
public class RunwayStatusChangeCommand extends SimulationCommand
{
    /** The closure-type {@link RunwayStatus} to apply to {@link #runway}; must not be {@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED}. */
    private final RunwayStatus status;

    /** The runway whose status is to be changed to a closure type. */
    private final Runway runway;

    /**
     * Constructs a {@code RunwayStatusChangeCommand} that will request a closure-type
     * status change for the specified runway when executed.
     *
     * @param status the target closure status to assign; must be
     *               {@link RunwayStatus#INSPECTION}, {@link RunwayStatus#SNOW_CLEARANCE},
     *               or {@link RunwayStatus#EQUIPMENT_FAILURE}, not {@link RunwayStatus#FREE}
     *               or {@link RunwayStatus#OCCUPIED}
     * @param runway the runway whose status is to be changed; must not be {@code null}
     */
    public RunwayStatusChangeCommand(RunwayStatus status, Runway runway)
    {
        this.status = status;
        this.runway = runway;
    }

    /**
     * Attempts to enqueue a runway closure event at the current simulation tick, and
     * appends a {@link MessageSeverity#HIGH} error message if the runway is ineligible.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>Read the current simulation tick from
     *       {@link SimulationState#getCurrentTime()}.</li>
     *   <li>Delegate to
     *       {@link EventGenerator#generateRunwayClosureEvent(Runway, RunwayStatus, long)},
     *       which checks that the runway is currently {@link RunwayStatus#FREE} before
     *       enqueuing a {@link uk.ac.warwick.cs261.simulation.events.RunwayClosureEvent}.
     *       If eligible, the closure event is scheduled and {@code true} is returned.</li>
     *   <li>If {@code generateRunwayClosureEvent} returns {@code false}, a 
     *       {@link MessageSeverity#HIGH} error message is appended to {@code messageQueue}.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to read the clock via
     *                     {@link SimulationState#getCurrentTime()}; must not be
     *                     {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#HIGH} error message
     *                     is appended if the runway is ineligible; must not be
     *                     {@code null}
     * @param generator    the event generator used to enqueue the closure event on
     *                     success; must not be {@code null}
     */
    @Override
    public void execute(SimulationState state, Queue<Message> messageQueue, EventGenerator generator)
    {
        long currentTime = state.getCurrentTime();
        boolean result = generator.generateRunwayClosureEvent(runway, status, currentTime);

        if (!result)
        {
            String msg = String.format("Failed to change %s status to %s", runway.getRunwayName(), status.toString());
            messageQueue.add(new Message(state.getCurrentTime(), "Simulation Error", msg, MessageSeverity.HIGH));
        }
    }
}