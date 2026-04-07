package uk.ac.warwick.cs261.threading.commands;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.events.EventGenerator;
import uk.ac.warwick.cs261.threading.messages.Message;

/**
 * A {@link SimulationCommand} that requests a change to a runway's operational mode,
 * translating a UI-initiated action into a scheduled
 * {@link uk.ac.warwick.cs261.simulation.events.RunwayModeChangeEvent} at the current
 * simulation tick.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code mode} and {@code runway} must not be {@code null}.</li>
 *   <li>The outcome of the mode change is not guaranteed at the time this command is
 *       constructed; it is subject to the availability check performed by
 *       {@link uk.ac.warwick.cs261.simulation.events.RunwayModeChangeEvent#execute}
 *       when the event fires.</li>
 *   <li>This class is not thread-safe; instances are constructed on the UI thread and
 *       executed on the simulation thread.</li>
 * </ul>
 */
public class RunwayModeChangeCommand extends SimulationCommand
{
    /** The {@link RunwayMode} to assign to {@link #runway} when the generated event executes. */
    private final RunwayMode mode;

    /** The runway whose {@link RunwayMode} is to be changed. */
    private final Runway runway;

    /**
     * Constructs a {@code RunwayModeChangeCommand} that will request a mode change for
     * the specified runway when executed.
     *
     * @param mode   the target {@link RunwayMode} to assign to the runway; must not be
     *               {@code null}
     * @param runway the runway whose mode is to be changed; must not be {@code null}
     */
    public RunwayModeChangeCommand(RunwayMode mode, Runway runway)
    {
        this.mode = mode;
        this.runway = runway;
    }

    /**
     * Reads the current simulation tick and enqueues a
     * {@link uk.ac.warwick.cs261.simulation.events.RunwayModeChangeEvent} for immediate
     * execution at that tick.
     *
     * <p>The event is scheduled at {@link SimulationState#getCurrentTime()} so that it
     * fires within the same simulation step in which this command is drained from the
     * command queue. The actual mode transition (and its safety check) are deferred to
     * the event's own {@code execute} method; this command performs no direct state
     * modification.
     *
     * @param state        the current simulation state, used solely to read the current
     *                     tick via {@link SimulationState#getCurrentTime()}; must not be
     *                     {@code null}
     * @param messageQueue the message queue; not written to by this command directly, but
     *                     passed through to satisfy the {@link SimulationCommand} contract;
     *                     must not be {@code null}
     * @param generator    the event generator used to enqueue the mode-change event;
     *                     must not be {@code null}
     */
    @Override
    public void execute(SimulationState state, Queue<Message> messageQueue, EventGenerator generator)
    {
        long currentTime = state.getCurrentTime();
        generator.generateRunwayModeChangeEvent(runway, mode, currentTime);
    }
}