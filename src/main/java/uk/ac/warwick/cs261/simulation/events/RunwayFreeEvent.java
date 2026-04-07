package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that releases an occupied runway back to {@link RunwayStatus#FREE}
 * after a takeoff or landing has completed.
 *
 * <p>This event is a specialisation of {@link RunwayStatusChangeEvent} for
 * {@link RunwayStatus#FREE}
 * 
 * <p>On a successful transition the runway's current aircraft reference is cleared via
 * {@link Runway#setAircraft(Aircraft) setAircraft(null)}, making the runway ready for the
 * next allocation cycle. A {@link MessageSeverity#LOW} message is appended to confirm the
 * runway's availability.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code runway} must not be {@code null}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class RunwayFreeEvent extends RunwayStatusChangeEvent
{
    /**
     * Constructs a {@code RunwayFreeEvent} that will release the given runway at the
     * specified simulation tick.
     *
     * @param time   the simulation tick at which the runway should be freed; must be
     *               non-negative
     * @param runway the runway to release; must not be {@code null}
     */
    public RunwayFreeEvent(long time, Runway runway)
    {
        super(time, runway, RunwayStatus.FREE);
        this.order = -1;
    }

    /**
     * Transitions the runway to {@link RunwayStatus#FREE}, clears its aircraft reference,
     * and appends a confirmation message.
     *
     * <p>The status transition is delegated to
     * {@link RunwayStatusChangeEvent#execute(SimulationState, SimulationStatistics, Queue)}. 
     * If this returns {@code true}, the runway's aircraft reference is nulled out so that
     * subsequent calls to {@link Runway#getAircraft()} correctly reflect that the runway
     * is unoccupied.
     *
     * @param state        the current simulation state; must not be {@code null}
     * @param statistics   the statistics collector; passed through to the parent
     *                     implementation; must not be {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#LOW} availability
     *                     message is appended on success; must not be {@code null}
     * @return {@code true} if the runway was successfully transitioned to
     *         {@link RunwayStatus#FREE}; {@code false} if the parent's execution failed,
     *         which should not occur for free-status transitions under normal conditions
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = super.execute(state, statistics, messageQueue);

        if (result)
        {
            runway.setAircraft(null);

            String msg = String.format("%s is now free", runway.getRunwayName());
            messageQueue.add(new Message(time, "Simulation", msg, MessageSeverity.LOW));
        }

        return result;
    }
}