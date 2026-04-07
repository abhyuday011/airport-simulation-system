package uk.ac.warwick.cs261.threading.commands;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.events.EventGenerator;
import uk.ac.warwick.cs261.threading.messages.Message;

/**
 * Abstract base class for user-initiated commands that modify the simulation's runway
 * configuration during a running simulation, decoupling the UI thread from the
 * simulation thread via a command-queue pattern.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>Subclasses must not retain references to the {@link SimulationState},
 *       {@link EventGenerator}, or message queue beyond the duration of a single
 *       {@link #execute} call.</li>
 *   <li>Commands are consumed exactly once; placing the same command instance on the
 *       queue a second time will execute it twice.</li>
 *   <li>This class is not thread-safe; individual instances are intended to be
 *       constructed on the UI thread, transferred via the command queue, and executed
 *       exclusively on the simulation thread.</li>
 * </ul>
 */
public abstract class SimulationCommand
{
    /**
     * Executes this command against the current simulation state, optionally enqueuing
     * one or more {@link uk.ac.warwick.cs261.simulation.events.SimulationEvent} instances
     * via the {@link EventGenerator} and appending status messages to the message queue.
     *
     * <p>Implementations should read the current simulation clock from
     * {@link SimulationState#getCurrentTime()} and pass it as the scheduled time when
     * calling generator methods, so that the resulting events take effect at the current
     * tick rather than being deferred. If the command cannot be fulfilled, for example
     * because a runway is in an incompatible state, an error
     * {@link uk.ac.warwick.cs261.threading.messages.Message} should be appended to
     * {@code messageQueue} to inform the operator.
     *
     * @param state        the current simulation state, used to read the clock and any
     *                     other runtime data required to fulfil the command; must not be
     *                     {@code null}
     * @param messageQueue the queue to which status or error messages may be appended;
     *                     must not be {@code null}
     * @param generator    the event generator used to schedule the simulation events that
     *                     implement this command's effect; must not be {@code null}
     */
    public abstract void execute(SimulationState state, Queue<Message> messageQueue, EventGenerator generator);
}