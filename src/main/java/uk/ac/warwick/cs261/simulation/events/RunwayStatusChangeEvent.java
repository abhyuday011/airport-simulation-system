package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.threading.messages.Message;

/**
 * A simulation event that transitions a runway to a new {@link RunwayStatus}, subject to
 * a safety check that prevents the last operationally available runway of a given type
 * from being closed.
 *
 * <p>Transitions to {@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED} are
 * applied unconditionally, as these represent normal operational states that the
 * simulation always permits. Transitions to any other status, representing closures such
 * as {@link RunwayStatus#INSPECTION}, {@link RunwayStatus#SNOW_CLEARANCE}, or
 * {@link RunwayStatus#EQUIPMENT_FAILURE} ,are first validated against the current runway
 * list. The validation counts the number of runways that are both operationally compatible
 * with the target runway's {@link RunwayMode} and currently in a usable state
 * ({@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED}). If only one such runway
 * exists, the closure is refused and the method returns {@code false} to prevent the
 * simulation from reaching a state where no runway can serve a given flight type.
 *
 * <p>Mode compatibility for the availability count is determined as follows: a runway is
 * considered of the same type as the target if either is {@link RunwayMode#MIXED}, or
 * if both share the same non-mixed mode.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code runway} and {@code targetStatus} must not be {@code null}.</li>
 *   <li>Subclasses {@link RunwayFreeEvent} and {@link RunwayClosureEvent} extend this
 *       class and may override {@link #execute} to add further side-effects after
 *       delegating to {@code super.execute()}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class RunwayStatusChangeEvent extends SimulationEvent
{
    /** The runway whose {@link RunwayStatus} is to be updated. */
    protected final Runway runway;

    /** The {@link RunwayStatus} to transition {@link #runway} to upon execution. */
    protected final RunwayStatus targetStatus;

    /**
     * Constructs a {@code RunwayStatusChangeEvent} scheduled to fire at the given tick.
     *
     * @param time         the simulation tick at which the status change should be applied;
     *                     must be non-negative
     * @param runway       the runway whose status is to be changed; must not be {@code null}
     * @param targetStatus the status to transition the runway to; must not be {@code null}
     */
    public RunwayStatusChangeEvent(long time, Runway runway, RunwayStatus targetStatus)
    {
        super(time);
        this.runway = runway;
        this.targetStatus = targetStatus;
        this.order = -1;
    }

    /**
     * Transitions the runway to {@link #targetStatus}, subject to a safety check for
     * closure-type transitions.
     *
     * <p>The execution proceeds as follows:
     * <ol>
     *   <li>If {@link #targetStatus} is {@link RunwayStatus#FREE} or
     *       {@link RunwayStatus#OCCUPIED}, apply the transition unconditionally and return
     *       {@code true}.</li>
     *   <li>Otherwise, iterate over all runways in {@link SimulationState#getRunways()} and
     *       count those that are both mode-compatible with {@link #runway} and currently in
     *       a usable state ({@link RunwayStatus#FREE} or {@link RunwayStatus#OCCUPIED}).
     *       Mode compatibility holds when either runway is {@link RunwayMode#MIXED} or both
     *       share the same non-mixed mode.</li>
     *   <li>If the count is exactly {@code 1}, refuse the transition and return
     *       {@code false} to prevent stranding aircraft of the affected type.</li>
     *   <li>Otherwise apply the transition and return {@code true}.</li>
     * </ol>
     *
     * @param state        the current simulation state, used to access the full runway list
     *                     for the availability count; must not be {@code null}
     * @param statistics   the statistics collector; not used directly by this base
     *                     implementation but available to subclasses; must not be
     *                     {@code null}
     * @param messageQueue the message queue; not used directly by this base implementation
     *                     but available to subclasses; must not be {@code null}
     * @return {@code true} if the runway's status was updated to {@link #targetStatus};
     *         {@code false} if the transition was refused because it would leave no
     *         available runway for the affected flight type
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        if (targetStatus == RunwayStatus.FREE || targetStatus == RunwayStatus.OCCUPIED)
        {
            runway.setStatus(targetStatus);
            return true;
        }

        int availableRunwaysOfSameType = 0;

        for (Runway currentRunway : state.getRunways())
        {
            boolean isSameType = currentRunway.getMode() == RunwayMode.MIXED
                              || runway.getMode() == RunwayMode.MIXED
                              || runway.getMode() == currentRunway.getMode();

            boolean isAvailable = currentRunway.getStatus() == RunwayStatus.FREE
                               || currentRunway.getStatus() == RunwayStatus.OCCUPIED;

            if (isSameType && isAvailable)
                availableRunwaysOfSameType++;
        }

        if (availableRunwaysOfSameType == 1)
            return false;

        runway.setStatus(targetStatus);

        return true;
    }
}