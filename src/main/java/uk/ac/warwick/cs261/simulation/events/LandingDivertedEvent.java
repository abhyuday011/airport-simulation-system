package uk.ac.warwick.cs261.simulation.events;

import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * A simulation event that diverts an arriving aircraft by removing it from the holding
 * queue and marking it as {@link AircraftStatus#DIVERTED}.
 *
 * <p>Each execution first checks whether the aircraft is still present in the holding 
 * queue before acting. If it is absent, because it has already landed or been diverted 
 * by an earlier event, the method returns {@code false} without modifying any state.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code aircraft} must not be {@code null}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class LandingDivertedEvent extends SimulationEvent
{
    /** The arriving aircraft to be diverted and removed from the holding queue. */
    private final Aircraft aircraft;

    /**
     * Constructs a {@code LandingDivertedEvent} scheduled to fire at the given tick.
     *
     * @param time     the simulation tick at which the diversion should be applied;
     *                 must be non-negative
     * @param aircraft the arriving aircraft to divert; must not be {@code null}
     */
    public LandingDivertedEvent(long time, Aircraft aircraft)
    {
        super(time);
        this.aircraft = aircraft;
        this.order = 1;
    }

    /**
     * Removes the aircraft from the holding queue, marks it as
     * {@link AircraftStatus#DIVERTED}, and records the diversion in statistics.
     *
     * <p>If the aircraft is not present in the holding queue when this event fires 
     * because it has already landed or been diverted by a prior event at the same or
     * earlier tick ,the method returns {@code false} immediately without modifying any
     * state. This is the normal outcome when a landing occurs before the diversion
     * deadline is reached.
     *
     * @param state        the current simulation state, used to access the holding queue;
     *                     must not be {@code null}
     * @param statistics   the statistics collector to notify of the diversion via
     *                     {@link SimulationStatistics#recordDiversion(long)};
     *                     must not be {@code null}
     * @param messageQueue the queue to which a {@link MessageSeverity#HIGH} diversion
     *                     message is appended on success; must not be {@code null}
     * @return {@code true} if the aircraft was present in the holding queue and has been
     *         diverted; {@code false} if the aircraft had already landed or been diverted
     */
    @Override
    public boolean execute(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        boolean result = state.getHoldingQueue().remove(aircraft);

        if (result)
        {
            String message = String.format("%s landing diverted", aircraft.getCallSign());
            messageQueue.add(new Message(time, "Simulation", message, MessageSeverity.HIGH));
            aircraft.setStatus(AircraftStatus.DIVERTED);

            statistics.recordDiversion(time);
        }

        return result;
    }
}