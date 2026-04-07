package uk.ac.warwick.cs261.simulation.entities.runway;

import java.util.List;
import java.util.Queue;

import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.simulation.events.EventGenerator;
import uk.ac.warwick.cs261.threading.messages.Message;
import uk.ac.warwick.cs261.threading.messages.MessageSeverity;

/**
 * Implements a greedy, queue-time-based strategy for allocating aircraft to available runways
 * each simulation tick.
 *
 * <p>On each call to {@link #allocateRunway(SimulationState, SimulationStatistics, Queue<Message>)},
 * the allocator inspects the heads of both the takeoff queue and the holding (landing) queue.
 * When both queues are non-empty, it grants the runway to whichever aircraft entered its
 * queue earliest (the aircraft with the smaller {@code queueEnterTime}). Ties are
 * broken in favour of the landing aircraft. If only one queue is non-empty, the sole
 * candidate is served immediately. When a suitable free runway cannot be found for the
 * preferred aircraft type, the allocator falls back to attempting the opposite type before
 * declaring failure for the current tick.
 *
 * <p>Runway eligibility is determined by {@link RunwayMode}: a runway whose mode is
 * {@link RunwayMode#LANDING} is ineligible for takeoffs, a runway whose mode is
 * {@link RunwayMode#TAKEOFF} is ineligible for landings, and a runway in
 * {@link RunwayMode#MIXED} mode may serve either operation. Only runways whose
 * {@link RunwayStatus} is {@link RunwayStatus#FREE} are considered.
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>{@link #allocateRunway(SimulationState, SimulationStatistics, Queue<Message>)}</td><td>O(r)</td></tr>
 *   <tr><td>{@link #tryAllocateRunway(SimulationState, SimulationStatistics, Queue<Message>, boolean)}</td><td>O(r)</td></tr>
 *   <tr><td>{@link #getFreeRunway(List<Runway>, boolean)}</td><td>O(r)</td></tr>
 * </table>
 * <p>where {@code r} is the number of runways registered in the simulation state. Queue
 * {@code peek} and {@code poll} operations are O(log n) for a priority queue but do not
 * dominate when {@code r} is comparable to or larger than the queue size in practice.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>The {@link EventGenerator} supplied at construction must not be {@code null}.</li>
 *   <li>All {@link SimulationState}, {@link SimulationStatistics}, and message-queue
 *       arguments passed to {@link #allocateRunway} must not be {@code null}.</li>
 *   <li>At most one aircraft is allocated per invocation of
 *       {@link #allocateRunway(SimulationState, SimulationStatistics, Queue<Message>)}; callers
 *       that wish to fill multiple runways in a single tick must invoke the method
 *       repeatedly until it returns {@code false}.</li>
 *   <li>This class is not thread-safe; external synchronisation is required if it is
 *       shared across threads.</li>
 * </ul>
 */
public class RunwayAllocator
{
    /** The event generator used to schedule events after a runway is vacated. */
    private final EventGenerator eventGenerator;

    /**
     * Constructs a new {@code RunwayAllocator} backed by the given event generator.
     *
     * @param eventGenerator the {@link EventGenerator} used to post runway-free events
     *                       after each takeoff or landing completes; must not be {@code null}
     */
    public RunwayAllocator(EventGenerator eventGenerator)
    {
        this.eventGenerator = eventGenerator;
    }

    /**
     * Attempts to allocate a single free runway to the highest-priority waiting aircraft,
     * advancing the simulation by one takeoff or landing event.
     *
     * <p>The selection algorithm proceeds as follows:
     * <ol>
     *   <li>Peek at the head of both the takeoff queue and the holding queue without
     *       removing either aircraft.</li>
     *   <li>If both queues are non-empty, compare {@code queueEnterTime} values. The
     *       aircraft that entered its queue earlier is served first; when times are equal,
     *       the landing aircraft is preferred.</li>
     *   <li>Attempt to allocate a runway for the preferred aircraft type via
     *       {@link #tryAllocateRunway(SimulationState, SimulationStatistics, Queue<Message>, boolean)}.
     *       If that attempt fails (no compatible free runway exists), fall back to the
     *       opposite type.</li>
     *   <li>If only one queue is non-empty, attempt to serve that queue's aircraft
     *       directly with no fallback needed.</li>
     * </ol>
     *
     * <p>Returns {@code false} without modifying any state when both queues are empty, or
     * when no free runway compatible with any waiting aircraft can be found.
     *
     * @param state        the current simulation state, providing access to the takeoff queue,
     *                     holding queue, and runway list; must not be {@code null}
     * @param statistics   the statistics collector that will be updated upon a successful
     *                     allocation; must not be {@code null}
     * @param messageQueue the queue to which a human-readable log message describing the
     *                     takeoff or landing is appended on success; must not be {@code null}
     * @return {@code true} if a runway was successfully allocated and an aircraft has begun
     *         its takeoff or landing; {@code false} if no allocation was possible this tick
     */
    public boolean allocateRunway(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue)
    {
        Aircraft takeoffAircraft = state.getTakeoffQueue().peek();
        Aircraft landingAircraft = state.getHoldingQueue().peek();

        if (takeoffAircraft != null && landingAircraft != null)
        {
            boolean isTakeoff = landingAircraft.getQueueEnterTime() <= takeoffAircraft.getQueueEnterTime();

            boolean result = tryAllocateRunway(state, statistics, messageQueue, isTakeoff);

            if (!result)
                result = tryAllocateRunway(state, statistics, messageQueue, !isTakeoff);

            return result;
        }
        else if (takeoffAircraft != null)
        {
            return tryAllocateRunway(state, statistics, messageQueue, true);
        }
        else if (landingAircraft != null)
        {
            return tryAllocateRunway(state, statistics, messageQueue, false);
        }

        return false;
    }

    /**
     * Attempts to allocate a free runway of the requested operation type and, if successful,
     * transitions the head aircraft from its queue onto that runway.
     *
     * <p>The method first searches the runway list for a runway that is both compatible with
     * {@code isTakeoff} and currently {@link RunwayStatus#FREE} via
     * {@link #getFreeRunway(List<Runway>, boolean)}. If no such runway exists, or if the
     * corresponding queue is empty, the method returns {@code false} immediately without
     * modifying any state.
     *
     * <p>On a successful allocation the following side-effects occur atomically within the
     * current tick:
     * <ul>
     *   <li>The aircraft is removed from its queue via {@code poll()}.</li>
     *   <li>The runway's {@link RunwayStatus} is set to {@link RunwayStatus#OCCUPIED} and
     *       its aircraft reference updated.</li>
     *   <li>The aircraft's {@code actualTime} is stamped with the current simulation tick
     *       and its {@link AircraftStatus} updated to either
     *       {@link AircraftStatus#DEPARTED} or {@link AircraftStatus#ARRIVED}.</li>
     *   <li>Statistics are recorded and updated via
     *       {@link SimulationStatistics#recordTakeoffOrLanding(Aircraft, long)} and
     *       {@link SimulationStatistics#updateStats(Aircraft)}.</li>
     *   <li>A runway-free event is scheduled via
     *       {@link EventGenerator#generateRunwayFreeEvent(Runway, Aircraft, long)}.</li>
     *   <li>A {@link MessageSeverity#LOW} log message is appended to {@code messageQueue}.</li>
     * </ul>
     *
     * @param state        the current simulation state; must not be {@code null}
     * @param statistics   the statistics collector to update on success; must not be {@code null}
     * @param messageQueue the message queue to which the activity log entry is appended;
     *                     must not be {@code null}
     * @param isTakeoff    {@code true} to attempt a takeoff allocation; {@code false} to
     *                     attempt a landing allocation
     * @return {@code true} if a runway was allocated and the aircraft has begun its
     *         operation; {@code false} if no compatible free runway exists or the
     *         relevant queue is empty
     */
    private boolean tryAllocateRunway(SimulationState state, SimulationStatistics statistics, Queue<Message> messageQueue, boolean isTakeoff)
    {
        Runway runway = getFreeRunway(state.getRunways(), isTakeoff);

        if (runway == null)
            return false;

        Aircraft aircraft = isTakeoff ? state.getTakeoffQueue().poll() : state.getHoldingQueue().poll();

        if (aircraft == null)
            return false;

        runway.setAircraft(aircraft);
        runway.setStatus(RunwayStatus.OCCUPIED);

        AircraftStatus status = isTakeoff ? AircraftStatus.DEPARTED : AircraftStatus.ARRIVED;

        aircraft.setActualTime(state.getCurrentTime());
        aircraft.setStatus(status);

        statistics.recordTakeoffOrLanding(aircraft, state.getCurrentTime());
        statistics.updateStats(aircraft);

        eventGenerator.generateRunwayFreeEvent(runway, aircraft, state.getCurrentTime());

        String message;

        if (isTakeoff)
            message = String.format("%s is taking off", aircraft.getCallSign());
        else
            message = String.format("%s is landing", aircraft.getCallSign());

        messageQueue.add(new Message(state.getCurrentTime(), "Simulation", message, MessageSeverity.LOW));

        return true;
    }

    /**
     * Scans the runway list and returns the first runway that is both operationally
     * compatible with the requested flight type and currently unoccupied.
     *
     * <p>A runway is considered compatible if its {@link RunwayMode} is not the exclusive
     * opposite of the requested operation: runways in {@link RunwayMode#LANDING} mode are
     * excluded when {@code isTakeoff} is {@code true}, and runways in
     * {@link RunwayMode#TAKEOFF} mode are excluded when {@code isTakeoff} is {@code false}.
     * Runways in {@link RunwayMode#MIXED} mode are eligible for both operations. In
     * addition to mode compatibility, only runways whose {@link RunwayStatus} is exactly
     * {@link RunwayStatus#FREE} are returned;
     *
     * <p>The list is traversed in iteration order; the first eligible runway encountered is
     * returned without examining the remainder, making this a greedy O(r) scan.
     *
     * @param runways   the list of all runways in the simulation; must not be {@code null}
     * @param isTakeoff {@code true} to find a runway suitable for takeoff;
     *                  {@code false} to find a runway suitable for landing
     * @return the first {@link Runway} that is compatible with {@code isTakeoff} and has
     *         status {@link RunwayStatus#FREE}, or {@code null} if no such runway exists
     */
    private Runway getFreeRunway(List<Runway> runways, boolean isTakeoff)
    {
        RunwayMode excludedMode = isTakeoff ? RunwayMode.LANDING : RunwayMode.TAKEOFF;

        for (Runway runway : runways)
        {
            boolean isRunwayFree = runway.getMode() != excludedMode
                                && runway.getStatus() == RunwayStatus.FREE;

            if (isRunwayFree)
                return runway;
        }

        return null;
    }
}