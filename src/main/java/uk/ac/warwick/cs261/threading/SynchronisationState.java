package uk.ac.warwick.cs261.threading;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.warwick.cs261.simulation.Schedule;
import uk.ac.warwick.cs261.simulation.SimulationState;
import uk.ac.warwick.cs261.simulation.SimulationStatistics;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.threading.commands.SimulationCommand;
import uk.ac.warwick.cs261.threading.messages.Message;

/**
 * A shared-state container that mediates all communication between the UI thread and
 * the simulation thread, providing the semaphores, atomic flags, and queues required
 * for lock-step synchronisation, user-command delivery, and log-message publishing.
 *
 * <p>The synchronisation model is based on a pair of binary {@link Semaphore} objects
 * that gate each simulation tick:
 * <ul>
 *   <li>{@link #simulationSemaphore} is initialised with one permit. The simulation
 *       thread acquires it at the start of each tick; the UI thread releases it (via
 *       {@link #cancelSimulation()} or by completing its rendering pass) to grant
 *       permission for the next tick.</li>
 *   <li>{@link #uiSemaphore} is initialised with zero permits. The simulation thread
 *       releases one permit at the end of each tick; the UI thread acquires it to block
 *       until that tick's state changes are ready to render.</li>
 * </ul>
 * This producer-consumer arrangement guarantees that the UI observes every simulation
 * tick in order with no ticks skipped or rendered twice, as long as
 * {@link #useSynchronisation} is {@code true}.
 *
 * <p>User interactions that would modify the simulation, such as changing a runway's
 * mode or status, are submitted as {@link SimulationCommand} instances via
 * {@link #commandQueue}. The simulation thread drains this queue at the start of each
 * tick, executing commands against the shared {@link SimulationState} in a controlled,
 * single-threaded context that avoids concurrency hazards.
 *
 * <p>Simulation events append {@link Message} objects to {@link #messageQueue} as
 * side-effects of their execution. The UI thread reads this queue to populate the
 * on-screen log. Neither queue is guarded by a lock; callers must respect the
 * single-producer / single-consumer discipline enforced by the semaphore protocol.
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>{@link #cancelSimulation()}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #disableSynchronisation()}</td><td>O(1)</td></tr>
 *   <tr><td>Command queue offer / poll</td><td>O(1) amortised ({@link ArrayDeque})</td></tr>
 *   <tr><td>Message queue offer / poll</td><td>O(1) amortised ({@link ArrayDeque})</td></tr>
 * </table>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@link #state} and {@link #statistics} are injected at construction and must not
 *       be {@code null}; they are read-only references within this class but the objects
 *       they point to are mutated by the simulation thread.</li>
 *   <li>{@link #commandQueue} and {@link #messageQueue} are {@link ArrayDeque} instances
 *       and are therefore <em>not</em> thread-safe. The semaphore protocol enforces that
 *       the UI thread writes to {@link #commandQueue} only while the simulation is
 *       blocked on {@link #simulationSemaphore}, and the simulation thread writes to
 *       {@link #messageQueue} only while the UI is blocked on {@link #uiSemaphore}.
 *       Callers must not bypass this protocol.</li>
 *   <li>{@link #takeoffSchedule} and {@link #landingSchedule} must be set via their
 *       respective setters before {@link uk.ac.warwick.cs261.simulation.Simulation#run()}
 *       is invoked; they are {@code null} until set.</li>
 * </ul>
 */
public class SynchronisationState
{
    /**
     * Gates the simulation thread's execution of each tick; initialised with one permit
     * so the simulation may proceed immediately on start-up. The UI thread releases a
     * permit after consuming each tick's rendered state, and
     * {@link #cancelSimulation()} and {@link #disableSynchronisation()} each release a
     * permit to unblock the simulation thread if it is waiting.
     */
    private final Semaphore simulationSemaphore = new Semaphore(1);

    /**
     * Gates the UI thread's consumption of each tick's rendered state; initialised with
     * zero permits so the UI blocks until the simulation has completed at least one tick.
     * The simulation thread releases one permit at the end of every tick and one final
     * permit after the event loop terminates.
     */
    private final Semaphore uiSemaphore = new Semaphore(0);

    /**
     * {@code true} once the simulation's event loop has terminated naturally (queue
     * exhausted) or after cancellation; read by the UI thread to determine when to
     * stop polling for new state.
     */
    private final AtomicBoolean isSimulationDone = new AtomicBoolean(false);

    /**
     * Set to {@code true} by {@link #cancelSimulation()} to request that the simulation
     * thread exit its event loop at the next iteration; read by the simulation thread
     * in its loop condition.
     */
    private final AtomicBoolean isSimulationCancelled = new AtomicBoolean(false);

    /**
     * Controls whether the lock-step semaphore protocol is active; when {@code false}
     * the simulation thread skips semaphore acquire and release calls and runs as fast
     * as possible. Toggled by {@link #disableSynchronisation()}.
     */
    private final AtomicBoolean useSynchronisation = new AtomicBoolean(false);

    /**
     * FIFO queue through which the UI thread submits {@link SimulationCommand} instances
     * to be executed by the simulation thread at the start of the next tick; backed by
     * an {@link ArrayDeque} for O(1) amortised offer and poll.
     */
    private final Queue<SimulationCommand> commandQueue = new ArrayDeque<>();

    /**
     * FIFO queue to which simulation events append {@link Message} log entries; consumed
     * by the UI thread to populate the on-screen event log; backed by an
     * {@link ArrayDeque} for O(1) amortised offer and poll.
     */
    private final Queue<Message> messageQueue = new ArrayDeque<>();

    /**
     * Read-only reference to the shared simulation state, exposed to the UI layer for
     * rendering; mutations are performed exclusively by the simulation thread.
     */
    private final SimulationState state;

    /**
     * Read-only reference to the simulation statistics accumulator, exposed to the UI
     * layer for display; mutations are performed exclusively by the simulation thread.
     */
    private final SimulationStatistics statistics;

    /**
     * The pre-computed Poisson-distributed schedule of departing {@link Aircraft}
     * instances; {@code null} until set via {@link #setTakeoffSchedule(Schedule)}.
     * Exposed to the UI layer for display purposes.
     */
    private Schedule<Aircraft> takeoffSchedule;

    /**
     * The pre-computed Poisson-distributed schedule of arriving {@link Aircraft}
     * instances; {@code null} until set via {@link #setLandingSchedule(Schedule)}.
     * Exposed to the UI layer for display purposes.
     */
    private Schedule<Aircraft> landingSchedule;

    /**
     * Constructs a {@code SynchronisationState} with the given simulation state and
     * statistics references, both queues empty, both schedules {@code null}, and
     * synchronisation disabled.
     *
     * @param state      the shared simulation state to expose to the UI; must not be
     *                   {@code null}
     * @param statistics the shared statistics accumulator to expose to the UI; must not
     *                   be {@code null}
     */
    public SynchronisationState(SimulationState state, SimulationStatistics statistics)
    {
        this.state = state;
        this.statistics = statistics;
    }

    /**
     * Signals the simulation thread to abort its event loop at the next iteration and
     * unblocks it if it is currently waiting on {@link #simulationSemaphore}.
     *
     * <p>Sets {@link #isSimulationCancelled} to {@code true} and releases one permit on
     * {@link #simulationSemaphore}. The simulation thread checks
     * {@link #isSimulationCancelled} in its loop condition and will exit cleanly on the
     * next iteration rather than being interrupted mid-event.
     */
    public void cancelSimulation()
    {
        isSimulationCancelled.set(true);
        simulationSemaphore.release();
    }

    /**
     * Disables the lock-step semaphore protocol and unblocks the simulation thread if it
     * is currently waiting on {@link #simulationSemaphore}.
     *
     * <p>Sets {@link #useSynchronisation} to {@code false} so that the simulation thread
     * will skip future semaphore operations, then releases one permit on
     * {@link #simulationSemaphore} to unblock the thread if it is mid-tick. After this
     * call the simulation runs as fast as possible without pausing for the UI.
     */
    public void disableSynchronisation()
    {
        useSynchronisation.set(false);
        simulationSemaphore.release();
    }

    /**
     * Returns the semaphore that gates the simulation thread's execution of each tick.
     *
     * <p>The simulation thread acquires one permit at the start of each tick; the UI
     * thread releases a permit after rendering the previous tick's state. The UI should
     * not acquire this semaphore directly; it is exposed here so that the simulation can
     * retrieve and operate on it.
     *
     * @return the simulation-side semaphore; never {@code null}
     */
    public Semaphore getSimulationSemaphore() { return simulationSemaphore; }

    /**
     * Returns the semaphore that gates the UI thread's consumption of each tick's
     * rendered state.
     *
     * <p>The simulation thread releases one permit after completing each tick; the UI
     * thread acquires a permit before reading the updated state to render it. The
     * simulation should not acquire this semaphore directly.
     *
     * @return the UI-side semaphore; never {@code null}
     */
    public Semaphore getUISemaphore() { return uiSemaphore; }

    /**
     * Returns the atomic flag that becomes {@code true} once the simulation's event loop
     * has terminated, either naturally or via {@link #cancelSimulation()}.
     *
     * <p>The UI thread polls this flag to determine when the simulation has finished and
     * no further state updates will be published.
     *
     * @return the done flag; never {@code null}; {@code false} until the simulation loop
     *         exits
     */
    public AtomicBoolean getIsSimulationDone() { return isSimulationDone; }

    /**
     * Returns the atomic flag that, when {@code true}, causes the simulation thread to
     * exit its event loop at the next iteration.
     *
     * <p>This flag is set by {@link #cancelSimulation()} and read by the simulation
     * thread in its loop condition. It is exposed here so that the simulation can
     * check it without holding a reference to this object's private fields.
     *
     * @return the cancellation flag; never {@code null}; {@code false} until
     *         {@link #cancelSimulation()} is called
     */
    public AtomicBoolean getIsSimulationCancelled() { return isSimulationCancelled; }

    /**
     * Returns the atomic flag that controls whether the lock-step semaphore protocol
     * is active for the current simulation run.
     *
     * <p>When {@code true}, the simulation thread acquires {@link #simulationSemaphore}
     * and releases {@link #uiSemaphore} on each tick. When {@code false}, both semaphore
     * operations are skipped and the simulation runs at maximum speed.
     *
     * @return the synchronisation flag; never {@code null}; {@code false} by default
     */
    public AtomicBoolean getUseSynchronisation() { return useSynchronisation; }

    /**
     * Returns the command queue through which the UI thread submits
     * {@link SimulationCommand} instances for execution by the simulation thread.
     *
     * <p>The simulation thread drains this queue at the start of each tick via
     * {@link Queue#poll()}; the UI thread appends commands via {@link Queue#add(Object)}.
     * Both operations must respect the single-producer / single-consumer discipline
     * enforced by the semaphore protocol.
     *
     * @return the command queue; never {@code null}; empty until commands are added
     */
    public Queue<SimulationCommand> getCommandQueue() { return commandQueue; }

    /**
     * Returns the message queue to which simulation events append {@link Message} log
     * entries for consumption by the UI thread.
     *
     * <p>The simulation thread appends messages via {@link Queue#add(Object)} as a
     * side-effect of event execution; the UI thread drains the queue via
     * {@link Queue#poll()} during each rendering pass.
     *
     * @return the message queue; never {@code null}; empty until messages are appended
     */
    public Queue<Message> getMessageQueue() { return messageQueue; }

    /**
     * Returns the shared {@link SimulationState}, which the UI thread reads to render
     * the current simulation snapshot.
     *
     * <p>Mutations to the state are performed exclusively by the simulation thread;
     * the UI should only read this object and must not invoke any mutating methods on it.
     *
     * @return the simulation state; never {@code null}
     */
    public SimulationState getState() { return state; }

    /**
     * Returns the shared {@link SimulationStatistics} accumulator, which the UI thread
     * reads to display current metrics.
     *
     * <p>Mutations to the statistics are performed exclusively by the simulation thread;
     * the UI should only read this object.
     *
     * @return the statistics accumulator; never {@code null}
     */
    public SimulationStatistics getStatistics() { return statistics; }

    /**
     * Returns the pre-computed takeoff {@link Schedule}, exposing it to the UI layer for
     * display.
     *
     * @return the takeoff schedule; {@code null} if not yet set via
     *         {@link #setTakeoffSchedule(Schedule)}
     */
    public Schedule<Aircraft> getTakeoffSchedule() { return takeoffSchedule; }

    /**
     * Sets the pre-computed takeoff {@link Schedule} so that the UI layer can display
     * scheduled departure times.
     *
     * <p>This method must be called before {@link uk.ac.warwick.cs261.simulation.Simulation#run()}
     * is invoked; it is called by the {@link uk.ac.warwick.cs261.simulation.Simulation}
     * constructor immediately after the schedule is built.
     *
     * @param takeoffSchedule the takeoff schedule to expose; must not be {@code null}
     */
    public void setTakeoffSchedule(Schedule<Aircraft> takeoffSchedule) { this.takeoffSchedule = takeoffSchedule; }

    /**
     * Returns the pre-computed landing {@link Schedule}, exposing it to the UI layer for
     * display.
     *
     * @return the landing schedule; {@code null} if not yet set via
     *         {@link #setLandingSchedule(Schedule)}
     */
    public Schedule<Aircraft> getLandingSchedule() { return landingSchedule; }

    /**
     * Sets the pre-computed landing {@link Schedule} so that the UI layer can display
     * scheduled arrival times.
     *
     * <p>This method must be called before {@link uk.ac.warwick.cs261.simulation.Simulation#run()}
     * is invoked; it is called by the {@link uk.ac.warwick.cs261.simulation.Simulation}
     * constructor immediately after the schedule is built.
     *
     * @param landingSchedule the landing schedule to expose; must not be {@code null}
     */
    public void setLandingSchedule(Schedule<Aircraft> landingSchedule) { this.landingSchedule = landingSchedule; }
}