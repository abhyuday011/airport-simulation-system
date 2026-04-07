package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that cancels a scheduled departure by removing the aircraft from
 * the takeoff queue and marking it as {@link AircraftStatus#CANCELLED}.
 * 
 * If the aircraft is still waiting in the takeoff queue when this event fires (meaning it 
 * has not yet been allocated a runway) then it is removed via the O(1) average arbitrary-removal 
 * capability of the {@link uk.ac.warwick.cs261.datastructures.IndexedQueue}, its status 
 * set to {@link AircraftStatus#CANCELLED}, and a cancellation is recorded in statistics.
 *
 * <p>If the aircraft is no longer in the takeoff queue when this event fires, because
 * it has already departed, {@link SimulationState#getTakeoffQueue()} will not contain
 * it, the removal will return {@code false}, and no state is modified.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code aircraft} must not be {@code null}.</li>
 *   <li>This event uses the default {@link SimulationEvent#order} of {@code 0}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class TakeoffCanceledEvent extends SimulationEvent
{
    /** The departing aircraft to be removed from the takeoff queue and marked as cancelled. */
    private final Aircraft aircraft;

    /**
     * Constructs a {@code TakeoffCanceledEvent} scheduled to fire at the given tick.
     *
     * @param time     the simulation tick at which the cancellation should be applied,
     *                 typically {@code scheduledTime + maxDelay}; must be non-negative
     * @param aircraft the departing aircraft to cancel; must not be {@code null}
     */
    public TakeoffCanceledEvent(long time, Aircraft aircraft)
    {
        super(time);
        this.aircraft = aircraft;
    }

    /**
     * Removes the aircraft from the takeoff queue, marks it as {@link AircraftStatus#CANCELLED},
     * and records the cancellation in statistics.
     *
     * <p>If the aircraft is not found in the takeoff queue ,because it has already been
     * allocated a runway and departed, the method returns {@code false} immediately
     * without modifying any state. This is the expected outcome for aircraft that depart
     * before their maximum-delay deadline is reached.
     *
     * @param state        the current simulation state, used to access the takeoff queue;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to notify of the cancellation via
     *                     {@link SimulationStatistics#recordCancellation(long)};
     *                     must not be {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#HIGH} cancellation
     *                     message is appended on success; must not be {@code null}
     * @return {@code true} if the aircraft was present in the takeoff queue and has been
     *         cancelled; {@code false} if the aircraft had already departed
     */
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = state.getTakeoffQueue().remove(aircraft);

        if (result)
        {
            String message = String.format("%s takeoff canceled", aircraft.getCallSign());
            messageQueue.add(new Message(time, "Simulation", message, MessageSeverity.HIGH));

            aircraft.setStatus(AircraftStatus.CANCELLED);

            statistics.recordCancellation(time);
        }

        return result;
    }
}