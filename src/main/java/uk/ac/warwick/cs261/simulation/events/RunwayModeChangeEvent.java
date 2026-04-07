package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that changes the operational mode of a runway, subject to a safety
 * check that prevents the last available runway serving a given flight type from being
 * reassigned away from that type.
 *
 * <p>The safety check logic differs depending on the runway's current mode:
 * <ul>
 *   <li>If the runway is currently {@link RunwayMode#MIXED} and is being narrowed to a
 *       specific mode, the check counts runways that serve the <em>opposite</em> mode,
 *       the one that would be lost ,including any other {@link RunwayMode#MIXED}
 *       runways and runways already dedicated to that mode.</li>
 *   <li>If the runway is currently a dedicated mode and is being changed to another
 *       dedicated mode or to {@link RunwayMode#MIXED}, the check counts runways that
 *       share the runway's current mode, including {@link RunwayMode#MIXED} runways.</li>
 * </ul>
 *
 * <p>Changing any runway to {@link RunwayMode#MIXED} always succeeds without a safety
 * check, since doing so can only add capacity rather than remove it.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code runway} and {@code targetMode} must not be {@code null}.</li>
 *   <li>Only runways with {@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED} are
 *       counted as available in the safety check; runways under closure are excluded.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class RunwayModeChangeEvent extends SimulationEvent
{
    /** The runway whose {@link RunwayMode} is to be updated. */
    private final Runway runway;

    /** The {@link RunwayMode} to transition {@link #runway} to upon execution. */
    private final RunwayMode targetMode;

    /**
     * Constructs a {@code RunwayModeChangeEvent} scheduled to fire at the given tick.
     *
     * @param time       the simulation tick at which the mode change should be applied;
     *                   must be non-negative
     * @param runway     the runway whose mode is to be changed; must not be {@code null}
     * @param targetMode the mode to transition the runway to; must not be {@code null}
     */
    public RunwayModeChangeEvent(long time, Runway runway, RunwayMode targetMode)
    {
        super(time);
        this.runway = runway;
        this.targetMode = targetMode;
        this.order = -1;
    }

    /**
     * Transitions the runway to {@link #targetMode}, subject to a safety check that
     * ensures at least one runway will remain available for the flight type that would
     * be lost by the mode change.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>If {@link #targetMode} is {@link RunwayMode#MIXED}, apply the mode
     *       unconditionally and return {@code true}, since broadening a runway's
     *       eligibility cannot remove capacity for any flight type.</li>
     *   <li>Otherwise, determine the mode that would be lost by the change:
     *     <ul>
     *       <li>If the runway is currently {@link RunwayMode#MIXED}, the lost mode is the
     *           opposite of {@link #targetMode}, the flight type that the runway currently
     *           serves but will no longer serve after the change.</li>
     *       <li>If the runway has a dedicated mode, the lost mode is the runway's current
     *           mode.</li>
     *     </ul>
     *   </li>
     *   <li>Count all runways whose mode is either {@link RunwayMode#MIXED} or equal to
     *       the lost mode, and whose status is {@link RunwayStatus#FREE} or
     *       {@link RunwayStatus#OCCUPIED}.</li>
     *   <li>If the count is exactly {@code 1}, refuse the change: append a
     *       {@link MessageSeverity#HIGH} error message and return {@code false}.</li>
     *   <li>Otherwise apply the mode change and return {@code true}.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to iterate over all runways
     *                     for the availability count; must not be {@code null}
     * @param statistics   the statistics collector; not used by this implementation but
     *                     required by the {@link SimulationEvent} contract;
     *                     must not be {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#HIGH} error message
     *                     is appended if the change is refused; must not be {@code null}
     * @return {@code true} if the runway's mode was updated to {@link #targetMode};
     *         {@code false} if the change was refused because it would leave no available
     *         runway for the flight type that would be lost
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        if (targetMode == RunwayMode.MIXED)
        {
            runway.setMode(targetMode);
            return true;
        }

        int availableRunwaysOfSameType = 0;

        for (Runway currentRunway : state.getRunways())
        {
            boolean isSameType;

            if (runway.getMode() == RunwayMode.MIXED)
            {
                RunwayMode lostMode = targetMode == RunwayMode.LANDING ? RunwayMode.TAKEOFF : RunwayMode.LANDING;
                isSameType = currentRunway.getMode() == RunwayMode.MIXED
                              || currentRunway.getMode() == lostMode;
            }
            else
            {
                isSameType = currentRunway.getMode() == RunwayMode.MIXED
                              || runway.getMode() == currentRunway.getMode();
            }

            boolean isAvailable = currentRunway.getStatus() == RunwayStatus.FREE
                               || currentRunway.getStatus() == RunwayStatus.OCCUPIED;

            if (isSameType && isAvailable)
                availableRunwaysOfSameType++;
        }

        if (availableRunwaysOfSameType == 1)
        {
            String msg = String.format("Failed to change mode for %s", runway.getRunwayName());
            messageQueue.add(new Message(time, "Simulation Error", msg, MessageSeverity.HIGH));

            return false;
        }

        runway.setMode(targetMode);
        return true;
    }
}