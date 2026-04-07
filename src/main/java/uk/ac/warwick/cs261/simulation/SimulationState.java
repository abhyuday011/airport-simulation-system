package uk.ac.warwick.cs261.simulation;

import java.util.List;

import uk.ac.warwick.cs261.datastructures.IndexedPriorityQueue;
import uk.ac.warwick.cs261.datastructures.IndexedQueue;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;

/**
 * A mutable snapshot of all runtime data that collectively describe the airport
 * simulation at any given simulation tick.
 *
 * <p>The state object owns three principal data structures:
 * <ul>
 *   <li>A {@link #currentTime} clock representing the simulation's current tick,
 *       advanced by {@link Simulation} after each event is processed.</li>
 *   <li>An {@link IndexedPriorityQueue} ({@link #holdingQueue}) that stores arriving
 *       aircraft.</li>
 *   <li>An {@link IndexedQueue} ({@link #takeoffQueue}) that stores departing aircraft
 *       in FIFO order.</li>
 *   <li>A {@link List} of {@link Runway} objects initialised from
 *       {@link RunwayParameters} via {@link #initRunways(List)}.</li>
 * </ul>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@link #initRunways(List)} must be called exactly once before any event accesses
 *       {@link #getRunways()}; accessing {@code runways} before initialisation will
 *       return {@code null}.</li>
 *   <li>The {@link #holdingQueue} and {@link #takeoffQueue} are created at construction
 *       and are never replaced; their contents are modified by events via the
 *       {@link #getHoldingQueue()} and {@link #getTakeoffQueue()} accessors.</li>
 *   <li>This class is not thread-safe; all access must occur on the simulation thread.</li>
 * </ul>
 */
public class SimulationState
{
    /**
     * The current simulation clock in ticks (seconds)
     */
    private long currentTime;

    /**
     * Priority queue holding arriving aircraft waiting for a landing runway
     */
    private final IndexedPriorityQueue<Aircraft> holdingQueue;

    /**
     * FIFO queue holding departing aircraft waiting for a takeoff runway.
     */
    private final IndexedQueue<Aircraft> takeoffQueue;

    /**
     * The list of all runways in the simulation; if not set in the constructor then this is
     * {@code null} until {@link #initRunways(List)} is called. Events access this list via
     * {@link #getRunways()} to inspect runway status and mode.
     */
    private List<Runway> runways;

    /**
     * Constructs a {@code SimulationState} with {@link #currentTime} set to {@code 0}
     * and {@link #runways} set to {@code null}.
     *
     * <p>{@link #initRunways(List)} must be called before any event accesses the runway
     * list.
     */
    public SimulationState() { this(0, null); }

    /**
     * Constructs a {@code SimulationState} with the given initial clock value and runway
     * list, initialising empty holding and takeoff queues.
     *
     * @param currentTime the initial simulation tick; must be non-negative
     * @param runways     the initial runway list, or {@code null} if runways will be
     *                    initialised later via {@link #initRunways(List)}
     */
    public SimulationState(long currentTime, List<Runway> runways)
    {
        this.currentTime = currentTime;
        holdingQueue = new IndexedPriorityQueue<>();
        takeoffQueue = new IndexedQueue<>();
        this.runways = runways;
    }

    /**
     * Initialises the runway list by constructing a {@link Runway} instance for each
     * {@link RunwayParameters} entry and assigning sequentially generated names of the
     * form {@code "Runway N"} starting from {@code 1}.
     *
     * <p>This method must be called exactly once after construction and before any event
     * that accesses {@link #getRunways()}. Calling it a second time replaces the existing
     * runway list without warning.
     *
     * @param runwayParameters the list of configuration objects from which runways are
     *                         constructed; must not be {@code null} or empty
     */
    public void initRunways(List<RunwayParameters> runwayParameters)
    {
        int[] i = {0};
        this.runways = runwayParameters.stream()
                                       .map(x -> new Runway(String.format("Runway %d", ++i[0]), x))
                                       .toList();
    }

    /**
     * Returns the current simulation clock value in ticks.
     *
     * @return the current simulation tick; always non-negative
     */
    public long getCurrentTime() { return currentTime; }

    /**
     * Advances the simulation clock to the given tick value.
     *
     * @param currentTime the new clock value in ticks; must be greater than or equal to
     *                    the previous value
     */
    public void setCurrentTime(long currentTime) { this.currentTime = currentTime; }

    /**
     * Returns the priority queue holding arriving aircraft that are waiting for a landing
     * runway.
     *
     * @return the holding queue; never {@code null}
     */
    public IndexedPriorityQueue<Aircraft> getHoldingQueue() { return holdingQueue; }

    /**
     * Returns the FIFO queue holding departing aircraft that are waiting for a takeoff
     * runway.
     *
     * @return the takeoff queue; never {@code null}
     */
    public IndexedQueue<Aircraft> getTakeoffQueue() { return takeoffQueue; }

    /**
     * Returns the list of all runways in the simulation.
     *
     * @return the runway list; {@code null} if {@link #initRunways(List)} has not yet
     *         been called
     */
    public List<Runway> getRunways() { return runways; }
}