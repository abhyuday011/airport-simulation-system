package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.threading.messages.Message;

/**
 * Abstract base class for all discrete events in the airport simulation, providing
 * scheduled-time ordering and a uniform execution contract.
 *
 * <p>The simulation operates as a discrete-event system driven by a
 * {@link java.util.PriorityQueue} of {@code SimulationEvent} instances. On each
 * iteration the scheduler dequeues the event with the smallest {@link #time} value and
 * calls its {@link #execute(SimulationState, SimulationStatistics, Queue)} method, which
 * applies the event's side-effects to the shared {@link SimulationState}. Ordering is
 * determined first by {@link #time} and, when two events share the same tick, by
 * {@link #order}. Subclasses that must fire before or after other same-tick events set
 * {@link #order} to a negative or positive value respectively in their constructors;
 * the default value of {@code 0} places an event between early-order ({@code order < 0})
 * and late-order ({@code order > 0}) events at the same tick.
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>{@link #compareTo(SimulationEvent)}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #getTime()}</td><td>O(1)</td></tr>
 * </table>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>Subclasses must implement {@link #execute(SimulationState, SimulationStatistics, Queue)}
 *       and must not retain references to the {@link SimulationState} beyond the duration
 *       of a single {@code execute} call.</li>
 *   <li>{@link #time} is immutable after construction; {@link #order} may be set once by
 *       the subclass constructor and should not be mutated thereafter.</li>
 *   <li>This class is not thread-safe; all events must be executed on the simulation
 *       thread.</li>
 * </ul>
 */
public abstract class SimulationEvent implements Comparable<SimulationEvent>
{
    /**
     * The simulation tick at which this event is scheduled to fire; used as the primary
     * sort key in {@link #compareTo(SimulationEvent)}.
     */
    protected final long time;

    /**
     * A secondary sort key that breaks ties between events scheduled at the same
     * {@link #time}. A lower value causes this event to be dequeued before events with
     * higher values at the same tick; the default is {@code 0}.
     */
    protected long order;

    /**
     * Constructs a {@code SimulationEvent} scheduled to fire at the given simulation tick,
     * with a default {@link #order} of {@code 0}.
     *
     * @param time the simulation tick at which this event should execute; must be
     *             non-negative
     */
    public SimulationEvent(long time)
    {
        this.time = time;
        this.order = 0;
    }

    /**
     * Compares this event to another for priority-queue ordering, using {@link #time} as
     * the primary key and {@link #order} as the tiebreaker.
     *
     * @param event the event to compare against; must not be {@code null}
     * @return a negative integer if this event should be processed before {@code event},
     *         zero if they have equal priority, or a positive integer if this event should
     *         be processed after {@code event}
     */
    @Override
    public int compareTo(SimulationEvent event)
    {
        if (Long.compare(time, event.time) == 0)
            return Long.compare(order, event.order);
        else
            return Long.compare(time, event.time);
    }

    /**
     * Returns the simulation tick at which this event is scheduled to fire.
     *
     * <p>This value is set at construction and never changes; it serves as the primary
     * ordering key used by the simulation's {@link java.util.PriorityQueue} to determine
     * which event to process next.
     *
     * @return the scheduled simulation tick for this event; always non-negative
     */
    public long getTime() { return time; }

    /**
     * Executes this event, applying its side-effects to the simulation state for the
     * current tick.
     *
     * <p>Implementations should modify {@code state} and {@code statistics} as required
     * by the event's semantics, and append one or more human-readable {@link Message}
     * objects to {@code messageQueue} to inform the UI or log of what occurred.
     * Implementations must return {@code true} if the event was applied successfully and
     * {@code false} if it was a no-op.
     *
     * @param state        the current simulation state to read from and write to;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to update upon a successful execution;
     *                     must not be {@code null}
     * @param messageQueue the queue to which log or status messages are appended;
     *                     must not be {@code null}
     * @return {@code true} if the event produced a state change; {@code false} if the
     *         event was inapplicable and no state was modified
     */
    public abstract boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue);
}