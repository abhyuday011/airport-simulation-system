package uk.ac.warwick.cs261.simulation;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;

import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftFactory;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayAllocator;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;
import uk.ac.warwick.cs261.simulation.events.EventGenerator;
import uk.ac.warwick.cs261.simulation.events.SimulationEvent;
import uk.ac.warwick.cs261.threading.SynchronisationState;
import uk.ac.warwick.cs261.threading.commands.SimulationCommand;

/**
 * The discrete-event airport simulation engine, implementing
 * {@link Runnable} so that it may be executed on a dedicated background thread.
 *
 * <p>The simulation is structured as a discrete-event loop driven by a
 * {@link PriorityQueue} of {@link SimulationEvent} instances ordered by scheduled tick.
 * On each iteration of the loop the {@link #advance()} method:
 * <ol>
 *   <li>Drains any pending {@link SimulationCommand} objects from the command queue,
 *       applying each to the current {@link SimulationState}.</li>
 *   <li>Dequeues the earliest {@link SimulationEvent} and calls its
 *       {@link SimulationEvent#execute} method, which applies side-effects to the state
 *       and statistics and appends log messages to the message queue.</li>
 *   <li>Advances the simulation clock to the event's tick and notifies
 *       {@link SimulationStatistics} of the time step.</li>
 *   <li>Calls {@link RunwayAllocator#allocateRunway} in a loop until no further
 *       allocation is possible in the current tick.</li>
 *   <li>Calls {@link #generateAircraft()} to pull the next aircraft from each
 *       {@link Schedule} and enqueue the corresponding landing or takeoff event cluster
 *       via {@link EventGenerator}.</li>
 * </ol>
 *
 * <p>When {@code useSynchronisation} is {@code true}, the loop operates in lock-step
 * with the UI thread via a pair of {@link java.util.concurrent.Semaphore} objects held
 * in {@link SynchronisationState}: the simulation acquires
 * {@link SynchronisationState#getSimulationSemaphore()} at the start of each step and
 * releases {@link SynchronisationState#getUISemaphore()} at the end, allowing the UI
 * to render each tick before the next begins. When synchronisation is disabled the loop
 * runs as fast as the JVM allows.
 *
 * <p>All stochastic distributions are constructed in the constructor from the supplied
 * {@link SimulationParameters} and injected into the {@link EventGenerator} via its
 * setters. All time-valued parameters in {@link SimulationParameters} are expressed in
 * hours or minutes and are converted to seconds using {@link SimulationConstants} before
 * use.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code parameters} and must not be {@code null}; passing a
 *       {@link SimulationParameters} that has not passed {@link SimulationParameters#validate()}
 *       may produce undefined behaviour.</li>
 *   <li>The simulation must be executed on a single dedicated thread; no internal state
 *       is guarded by locks and concurrent access from multiple threads is not
 *       supported.</li>
 *   <li>The {@link PriorityQueue} used as the event queue is not stable: events sharing
 *       the same tick are dequeued in an unspecified order unless differentiated by
 *       {@link SimulationEvent#order}.</li>
 * </ul>
 */
public class Simulation implements Runnable
{
    /** Encapsulates all mutable runtime data - queues, clock, and runway list - shared across events. */
    private final SimulationState state = new SimulationState();

    /** Accumulates metrics throughout the run; updated by events and by {@link #advance()} on each time step. */
    private final SimulationStatistics statistics = new SimulationStatistics();

    /**
     * Pre-computed Poisson-distributed schedule of departing {@link Aircraft} instances;
     * consumed one aircraft at a time by {@link #generateAircraft()}.
     */
    private final Schedule<Aircraft> takeoffSchedule;

    /**
     * Pre-computed Poisson-distributed schedule of arriving {@link Aircraft} instances;
     * consumed one aircraft at a time by {@link #generateAircraft()}.
     */
    private final Schedule<Aircraft> landingSchedule;

    /**
     * Holds the semaphores, atomic flags, command queue, and message queue used to
     * coordinate between the simulation thread and the UI thread.
     */
    private final SynchronisationState synchronisation = new SynchronisationState(state, statistics);

    /**
     * The central priority queue of pending {@link SimulationEvent} instances, ordered by
     * {@link SimulationEvent#time} and then by {@link SimulationEvent#order}; events are
     * polled by {@link #advance()} and added by {@link EventGenerator}.
     */
    private final PriorityQueue<SimulationEvent> eventQueue = new PriorityQueue<>();

    /**
     * Factory that constructs and enqueues logically coupled groups of
     * {@link SimulationEvent} instances in response to simulation occurrences such as
     * aircraft arrivals, runway clearances, and closure cycles.
     */
    private final EventGenerator eventGenerator = new EventGenerator(eventQueue);

    /**
     * Greedy allocator that assigns waiting aircraft from the holding and takeoff queues
     * to available runways each tick, invoking {@link EventGenerator#generateRunwayFreeEvent}
     * for each successful allocation.
     */
    private final RunwayAllocator runwayAllocator = new RunwayAllocator(eventGenerator);

    /**
     * Factory function that constructs a departing {@link Aircraft} with ground speed
     * {@code 15} knots for the given scheduled departure tick; supplied to the takeoff
     * {@link Schedule} at construction.
     */
    private static final Function<Long, Aircraft> CREATE_TAKEOFF_AIRCRAFT =
        scheduledTime -> AircraftFactory.createTakeoffAircraft(scheduledTime, 15);

    /**
     * Factory function that constructs an arriving {@link Aircraft} with ground speed
     * {@code 15} knots for the given scheduled arrival tick; supplied to the landing
     * {@link Schedule} at construction.
     */
    private static final Function<Long, Aircraft> CREATE_LANDING_AIRCRAFT =
        scheduledTime -> AircraftFactory.createLandingAircraft(scheduledTime, 15);

    /**
     * Constructs a fully initialised {@code Simulation} from the given parameters,
     * building all stochastic distributions, pre-computing aircraft schedules, and
     * wiring everything into the {@link EventGenerator}.
     *
     * <p>Construction proceeds in four phases:
     * <ol>
     *   <li>The runway list is initialised from {@link SimulationParameters#getRunwayParameters()}
     *       via {@link SimulationState#initRunways(List)}.</li>
     *   <li>A {@link Well19937c} RNG is seeded from {@link SimulationParameters#getRngSeed()};
     *       all subsequent distributions share this generator to ensure reproducibility
     *       when the seed is fixed.</li>
     *   <li>Runway-closure distributions and probabilities are configured by
     *       {@link #initRandomRunwayClosures(SimulationParameters, RandomGenerator)}.</li>
     *   <li>Aircraft-behaviour distributions are configured by
     *       {@link #initRandomAircraftParameters(SimulationParameters, RandomGenerator)}.</li>
     * </ol>
     *
     * <p>All duration and rate parameters in {@link SimulationParameters} are stored in
     * hours or minutes; this constructor converts them to seconds using
     * {@link SimulationConstants#HOURS_TO_SECONDS} and
     * {@link SimulationConstants#MINUTES_TO_SECONDS} before passing them to
     * distributions or schedules.
     *
     * @param parameters         the fully validated simulation configuration; must not be
     *                           {@code null} and should have passed
     *                           {@link SimulationParameters#validate()} without errors
     * @param useSynchronisation {@code true} to enable lock-step UI synchronisation via
     *                           the semaphores in {@link SynchronisationState};
     *                           {@code false} to run as fast as possible without pausing
     *                           for the UI
     */
    public Simulation(SimulationParameters parameters, boolean useSynchronisation)
    {
        state.initRunways(parameters.getRunwayParameters());

        RandomGenerator rng = new Well19937c(parameters.getRngSeed());

        long durationSeconds = parameters.getDuration() * SimulationConstants.HOURS_TO_SECONDS;
        statistics.init(durationSeconds);

        statistics.record_RunwayOperationRatio(state.getRunways(), true);

        double outFlowAircraftPerSeconds = ((double) parameters.getOutflow()) / SimulationConstants.HOURS_TO_SECONDS;
        double inFlowAircraftPerSeconds  = ((double) parameters.getInflow())  / SimulationConstants.HOURS_TO_SECONDS;

        takeoffSchedule = new Schedule<>(rng, outFlowAircraftPerSeconds, durationSeconds, CREATE_TAKEOFF_AIRCRAFT);
        landingSchedule = new Schedule<>(rng, inFlowAircraftPerSeconds,  durationSeconds, CREATE_LANDING_AIRCRAFT);

        synchronisation.setTakeoffSchedule(takeoffSchedule);
        synchronisation.setLandingSchedule(landingSchedule);
        synchronisation.getUseSynchronisation().set(useSynchronisation);

        initRandomRunwayClosures(parameters, rng);
        statistics.initRunwayClosureCount(state.getRunways());

        initRandomAircraftParameters(parameters, rng);
    }

    /**
     * Constructs and injects the runway-closure distributions and the post-operation
     * {@link RunwayStatus} enumerated distribution into the {@link EventGenerator}.
     *
     * <p>Three {@link ExponentialDistribution} instances are constructed - one each for
     * inspection, snow-clearance, and equipment-failure closures - using mean durations
     * converted from minutes to seconds. The four closure probabilities (including the
     * residual {@link RunwayStatus#FREE} probability) are assembled into an
     * {@link EnumeratedDistribution} that the event generator samples after each
     * completed runway operation to determine whether a closure follows.
     *
     * @param parameters the simulation parameters supplying mean durations and
     *                   probabilities; must not be {@code null}
     * @param rng        the shared random number generator; must not be {@code null}
     */
    private void initRandomRunwayClosures(SimulationParameters parameters, RandomGenerator rng)
    {
        double meanInspectionDurationSeconds      = parameters.getRunwayInspectionDurationMean()      * SimulationConstants.MINUTES_TO_SECONDS;
        double meanSnowClearanceDurationSeconds    = parameters.getRunwaySnowClearanceDurationMean()    * SimulationConstants.MINUTES_TO_SECONDS;
        double meanEquipmentFailureDurationSeconds = parameters.getRunwayEquipmentFailureDurationMean() * SimulationConstants.MINUTES_TO_SECONDS;

        ExponentialDistribution inspectionDurationDistribution      = new ExponentialDistribution(rng, meanInspectionDurationSeconds);
        ExponentialDistribution snowClearanceDurationDistribution    = new ExponentialDistribution(rng, meanSnowClearanceDurationSeconds);
        ExponentialDistribution equipmentFailureDurationDistribution = new ExponentialDistribution(rng, meanEquipmentFailureDurationSeconds);

        double pInspection       = parameters.getRunwayInspectionProbability();
        double pSnowClearance    = parameters.getRunwaySnowClearanceProbability();
        double pEquipmentFailure = parameters.getRunwayEquipmentFailureProbability();
        double pFree             = 1.0 - (pInspection + pSnowClearance + pEquipmentFailure);

        List<Pair<RunwayStatus, Double>> runwayClosures = Arrays.asList(
            new Pair<>(RunwayStatus.FREE,              pFree),
            new Pair<>(RunwayStatus.INSPECTION,        pInspection),
            new Pair<>(RunwayStatus.SNOW_CLEARANCE,    pSnowClearance),
            new Pair<>(RunwayStatus.EQUIPMENT_FAILURE, pEquipmentFailure)
        );

        EnumeratedDistribution<RunwayStatus> runwayStatusDistribution = new EnumeratedDistribution<>(rng, runwayClosures);

        eventGenerator.setInspectionDurationDistribution(inspectionDurationDistribution);
        eventGenerator.setSnowClearanceDurationDistribution(snowClearanceDurationDistribution);
        eventGenerator.setEquipmentFailureDurationDistribution(equipmentFailureDurationDistribution);
        eventGenerator.setRunwayStatusDistribution(runwayStatusDistribution);
    }

    /**
     * Constructs and injects the per-aircraft stochastic distributions - delay, fuel,
     * health escalation, and emergency status - into the {@link EventGenerator}.
     *
     * <p>A zero-mean {@link NormalDistribution} is constructed for departure delays using
     * the standard deviation from parameters (converted to seconds). A
     * {@link UniformRealDistribution} models remaining fuel between the configured
     * minimum and maximum (both converted to seconds). An {@link ExponentialDistribution}
     * models health-escalation intervals. An {@link EnumeratedDistribution} over
     * {@link AircraftStatus} values combines the mechanical-failure and passenger-health
     * probabilities with the residual {@link AircraftStatus#OK} probability.
     *
     * @param parameters the simulation parameters supplying distribution moments and
     *                   probabilities; must not be {@code null}
     * @param rng        the shared random number generator; must not be {@code null}
     */
    private void initRandomAircraftParameters(SimulationParameters parameters, RandomGenerator rng)
    {
        double delayStdDevSeconds = parameters.getDelayStdDev() * SimulationConstants.MINUTES_TO_SECONDS;
        NormalDistribution delayDistribution = new NormalDistribution(rng, 0, delayStdDevSeconds);

        double minRemainingFuelSeconds = parameters.getMinRemainingFuel() * SimulationConstants.MINUTES_TO_SECONDS;
        double maxRemainingFuelSeconds = parameters.getMaxRemainingFuel() * SimulationConstants.MINUTES_TO_SECONDS;

        UniformRealDistribution remainingFuelDistribution = new UniformRealDistribution(
            rng, minRemainingFuelSeconds, maxRemainingFuelSeconds
        );

        double meanHealthEscalationTimeSeconds = parameters.getHealthEscalationTimeMean() * SimulationConstants.MINUTES_TO_SECONDS;
        ExponentialDistribution healthEscalationTimeDistribution = new ExponentialDistribution(rng, meanHealthEscalationTimeSeconds);

        double pMechanicalFailure = parameters.getMechanicalFailureProbability();
        double pPassengerHealth   = parameters.getPassengerHealthProbability();
        double pOK                = 1.0 - (pMechanicalFailure + pPassengerHealth);

        List<Pair<AircraftStatus, Double>> emergencies = Arrays.asList(
            new Pair<>(AircraftStatus.OK,                 pOK),
            new Pair<>(AircraftStatus.MECHANICAL_FAILURE, pMechanicalFailure),
            new Pair<>(AircraftStatus.PASSENGER_HEALTH,   pPassengerHealth)
        );

        EnumeratedDistribution<AircraftStatus> emergencyStatusDistribution = new EnumeratedDistribution<>(rng, emergencies);

        long maxDelaySeconds         = Math.round(parameters.getMaxDelay())         * SimulationConstants.MINUTES_TO_SECONDS;
        long lowFuelThresholdSeconds = Math.round(parameters.getLowFuelThreshold()) * SimulationConstants.MINUTES_TO_SECONDS;

        eventGenerator.setDelayDistribution(delayDistribution);
        eventGenerator.setRemainingFuelDistribution(remainingFuelDistribution);
        eventGenerator.setHealthEscalationTimeDistribution(healthEscalationTimeDistribution);
        eventGenerator.setEmergencyStatusDistribution(emergencyStatusDistribution);
        eventGenerator.setMaxDelay(maxDelaySeconds);
        eventGenerator.setLowFuelThreshold(lowFuelThresholdSeconds);
    }

    /**
     * Returns the {@link SynchronisationState} used to coordinate between the simulation
     * thread and the UI thread.
     *
     * <p>The returned object exposes the semaphores, atomic flags, command queue, and
     * message queue that the UI layer uses to pause, resume, cancel, and observe the
     * simulation.
     *
     * @return the synchronisation state; never {@code null}
     */
    public SynchronisationState getSynchronisation() { return synchronisation; }

    /**
     * Executes the simulation event loop on the calling thread, processing all events
     * until the queue is exhausted or a cancellation is requested.
     *
     * <p>Before entering the loop, all aircraft are generated into the event queue via
     * {@link #generateAircraft()} and initial runway-free events are seeded via
     * {@link EventGenerator#generateInitialRunwayFreeEvents(List, long)}.
     *
     * <p>Each loop iteration:
     * <ol>
     *   <li>If synchronisation is enabled, acquires
     *       {@link SynchronisationState#getSimulationSemaphore()} to block until the UI
     *       signals readiness.</li>
     *   <li>Calls {@link #advance()} to process the next event.</li>
     *   <li>If synchronisation is enabled, releases
     *       {@link SynchronisationState#getUISemaphore()} to allow the UI to render the
     *       updated state.</li>
     * </ol>
     *
     * <p>After the loop completes - whether by queue exhaustion or cancellation -
     * {@link SynchronisationState#getIsSimulationDone()} is set to {@code true} and the
     * UI semaphore is released one final time to unblock any waiting UI thread.
     */
    @Override
    public void run()
    {
        generateAircraft();
        eventGenerator.generateInitialRunwayFreeEvents(state.getRunways(), state.getCurrentTime());

        while (!eventQueue.isEmpty() && !synchronisation.getIsSimulationCancelled().get())
        {
            try
            {
                if (synchronisation.getUseSynchronisation().get())
                    synchronisation.getSimulationSemaphore().acquire();

                advance();

                if (synchronisation.getUseSynchronisation().get())
                    synchronisation.getUISemaphore().release();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        synchronisation.getIsSimulationDone().set(true);

        if (!synchronisation.getIsSimulationCancelled().get())
            statistics.record_RunwayOperationRatio(state.getRunways(), false);

        synchronisation.getUISemaphore().release();
    }

    /**
     * Processes one simulation step by draining the command queue, executing the next
     * event, advancing the clock, allocating runways, and generating the next aircraft.
     *
     * <p>The method proceeds as follows:
     * <ol>
     *   <li>All pending {@link SimulationCommand} objects are polled from
     *       {@link SynchronisationState#getCommandQueue()} and executed against the
     *       current state and event generator.</li>
     *   <li>The earliest {@link SimulationEvent} is polled from {@link #eventQueue}.
     *       If the queue is empty, the method returns immediately.</li>
     *   <li>{@link SimulationEvent#execute} is called. If it returns {@code false},
     *       indicating the event was inapplicable, {@link #generateAircraft()} is called
     *       and the method returns early without advancing the clock.</li>
     *   <li>The simulation clock is advanced to the event's tick via
     *       {@link SimulationState#setCurrentTime(long)} and
     *       {@link SimulationStatistics#onTimeAdvanced(long, long, SimulationState)} is
     *       notified.</li>
     *   <li>{@link RunwayAllocator#allocateRunway} is called in a loop until it returns
     *       {@code false}, exhausting all possible allocations for the current tick.</li>
     *   <li>{@link #generateAircraft()} is called to pull the next scheduled aircraft
     *       from each {@link Schedule} and enqueue their event clusters.</li>
     * </ol>
     */
    private void advance()
    {
        SimulationCommand command = synchronisation.getCommandQueue().poll();
        while (command != null)
        {
            command.execute(state, synchronisation.getMessageQueue(), eventGenerator);
            command = synchronisation.getCommandQueue().poll();
        }

        SimulationEvent event = eventQueue.poll();

        if (event == null)
            return;

        long previousTime = state.getCurrentTime();
        boolean processed = event.execute(state, statistics, synchronisation.getMessageQueue());

        if (!processed)
        {
            generateAircraft();
            return;
        }

        long newTime = event.getTime();
        statistics.onTimeAdvanced(previousTime, newTime, state);

        state.setCurrentTime(newTime);

        while (runwayAllocator.allocateRunway(state, statistics, synchronisation.getMessageQueue())) {}

        generateAircraft();
    }

    /**
     * Pulls the next aircraft from each {@link Schedule} and, for each non-{@code null}
     * result, enqueues the corresponding event cluster via the {@link EventGenerator}.
     *
     * <p>At most one arriving and one departing aircraft are generated per call.
     * {@link EventGenerator#generateLandingEvents(Aircraft, long)} is called for the
     * arriving aircraft and
     * {@link EventGenerator#generateTakeoffEvents(Aircraft, long)} for the departing
     * aircraft, each with the current simulation time as the lower bound on queue-entry
     * time. If either schedule is exhausted, its slot is silently skipped.
     */
    private void generateAircraft()
    {
        Aircraft landingAircraft = landingSchedule.getNext();
        Aircraft takeoffAircraft = takeoffSchedule.getNext();

        if (landingAircraft != null)
            eventGenerator.generateLandingEvents(landingAircraft, state.getCurrentTime());

        if (takeoffAircraft != null)
            eventGenerator.generateTakeoffEvents(takeoffAircraft, state.getCurrentTime());
    }
}