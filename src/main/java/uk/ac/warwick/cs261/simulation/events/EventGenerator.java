package uk.ac.warwick.cs261.simulation.events;

import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import uk.ac.warwick.cs261.simulation.SimulationConstants;
import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;

/**
 * A factory that constructs and enqueues logically coupled groups of
 * {@link SimulationEvent} instances, encapsulating the probabilistic sampling and
 * causal relationships between events so that call sites need only describe
 * <em>what</em> should happen rather than <em>which</em> events to schedule.
 *
 * <p>Each public {@code generate*} method samples one or more
 * {@link org.apache.commons.math3.distribution.RealDistribution} or
 * {@link EnumeratedDistribution} fields, computes derived timestamps, and adds the
 * resulting events directly to the shared {@link PriorityQueue} supplied at construction.
 * Callers never need to compute delay offsets, fuel thresholds, or runway-occupation
 * durations themselves. The key generation methods and the coupled events they produce
 * are:
 * <ul>
 *   <li>{@link #generateLandingEvents(Aircraft, long)}, schedules a
 *       {@link HoldingQueueEnterEvent}, a fuel-exhaustion {@link AircraftStatusChangeEvent},
 *       a {@link LandingDivertedEvent} deadline, and optionally a health-escalation
 *       {@link AircraftStatusChangeEvent} with its own diversion deadline.</li>
 *   <li>{@link #generateTakeoffEvents(Aircraft, long)}, schedules a
 *       {@link TakeoffQueueEnterEvent} and a paired {@link TakeoffCanceledEvent}
 *       deadline.</li>
 *   <li>{@link #generateRunwayFreeEvent(Runway, Aircraft, long)}, computes the
 *       occupation duration from ground speed and runway length, samples the post-use
 *       runway status, and schedules either a {@link RunwayClosureEvent} or a plain
 *       {@link RunwayFreeEvent}.</li>
 *   <li>{@link #generateInitialRunwayFreeEvents(List, long)}, seTS all runways into a
 *       valid state at {@code t = 0}.</li>
 * </ul>
 *
 * <p>All distributions are injectable via constructor or setter, making the generator
 * straightforward to reconfigure or test with deterministic distributions.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@link #eventQueue} must not be {@code null}; it is the sole shared mutable
 *       state and is written to by every generation method.</li>
 *   <li>Distribution fields default to {@code null} when the single-argument constructor
 *       is used; calling any generation method that samples a {@code null} distribution
 *       will throw a {@link NullPointerException}.</li>
 *   <li>{@link #generateInitialRunwayFreeEvents(List, long)} is a one-time setup method
 *       and silently returns without effect if {@code currentTime} is not {@code 0}.</li>
 *   <li>This class is not thread-safe; all generation methods must be called from the
 *       simulation thread.</li>
 * </ul>
 */
public class EventGenerator
{
    /**
     * The shared simulation event queue into which all generated {@link SimulationEvent}
     * instances are inserted; must not be {@code null}.
     */
    private final PriorityQueue<SimulationEvent> eventQueue;

    /**
     * Distribution used to sample arrival or departure delay offsets in simulation ticks;
     * sampled by {@link #generateLandingEvents(Aircraft, long)} and
     * {@link #generateTakeoffEvents(Aircraft, long)}.
     */
    private RealDistribution delayDistribution;

    /**
     * Distribution used to sample an arriving aircraft's remaining fuel in simulation
     * ticks; sampled by {@link #generateLandingEvents(Aircraft, long)} to determine when
     * the aircraft will reach the low-fuel threshold.
     */
    private RealDistribution remainingFuelDistribution;

    /**
     * Distribution used to sample the number of ticks before a
     * {@link AircraftStatus#PASSENGER_HEALTH} condition escalates to
     * {@link AircraftStatus#PASSENGER_HEALTH_SEVERE}; sampled conditionally by
     * {@link #generateLandingEvents(Aircraft, long)}.
     */
    private RealDistribution healthEscalationTimeDistribution;

    /**
     * Distribution used to sample the duration of a runway inspection closure in
     * simulation ticks; returned by {@link #getRunwayClosureDurationDistribution(RunwayStatus)}
     * for {@link RunwayStatus#INSPECTION}.
     */
    private RealDistribution inspectionDurationDistribution;

    /**
     * Distribution used to sample the duration of a snow-clearance closure in simulation
     * ticks; returned by {@link #getRunwayClosureDurationDistribution(RunwayStatus)} for
     * {@link RunwayStatus#SNOW_CLEARANCE}.
     */
    private RealDistribution snowClearanceDurationDistribution;

    /**
     * Distribution used to sample the duration of an equipment-failure closure in
     * simulation ticks; returned by {@link #getRunwayClosureDurationDistribution(RunwayStatus)}
     * for {@link RunwayStatus#EQUIPMENT_FAILURE}.
     */
    private RealDistribution equipmentFailureDurationDistribution;

    /**
     * Enumerated distribution over {@link AircraftStatus} values used to sample the
     * initial emergency status of an arriving aircraft; sampled by
     * {@link #generateLandingEvents(Aircraft, long)}.
     */
    private EnumeratedDistribution<AircraftStatus> emergencyStatusDistribution;

    /**
     * Enumerated distribution over {@link RunwayStatus} values used to sample the
     * post-operation runway status after each takeoff or landing; sampled by
     * {@link #generateRunwayFreeEvent(Runway, Aircraft, long)}.
     */
    private EnumeratedDistribution<RunwayStatus> runwayStatusDistribution;

    /**
     * The maximum number of simulation ticks beyond a flight's scheduled departure time
     * after which an unserved departing aircraft is cancelled; used as the deadline
     * offset in {@link #generateTakeoffEvents(Aircraft, long)}.
     */
    private long maxDelay;

    /**
     * The number of simulation ticks of remaining fuel at which an aircraft is
     * considered to have low fuel; subtracted from the sampled remaining-fuel value in
     * {@link #generateLandingEvents(Aircraft, long)} to compute the low-fuel trigger tick.
     */
    private long lowFuelThreshold;

    /**
     * Constructs an {@code EventGenerator} with a shared event queue and all distribution
     * fields set to {@code null}.
     *
     * <p>This constructor is provided for contexts where distributions will be injected
     * later via setters. Calling any generation method that samples a distribution before
     * the corresponding field is set will throw a {@link NullPointerException}.
     *
     * @param eventQueue the shared simulation event queue into which generated events will
     *                   be inserted; must not be {@code null}
     */
    public EventGenerator(PriorityQueue<SimulationEvent> eventQueue)
    {
        this(eventQueue, null, null, null, null, null, null, null, null, 0, 0);
    }

    /**
     * Constructs a fully configured {@code EventGenerator} with all distributions and
     * threshold values provided explicitly.
     *
     * @param eventQueue                          the shared simulation event queue;
     *                                            must not be {@code null}
     * @param delayDistribution                   distribution over arrival/departure delay
     *                                            offsets in ticks; may be {@code null} if
     *                                            landing and takeoff generation will not
     *                                            be called
     * @param remainingFuelDistribution           distribution over remaining fuel in ticks
     *                                            for arriving aircraft; may be {@code null}
     *                                            if landing generation will not be called
     * @param healthEscalationTimeDistribution    distribution over health-escalation
     *                                            intervals in ticks; may be {@code null}
     *                                            if no aircraft will arrive with
     *                                            {@link AircraftStatus#PASSENGER_HEALTH}
     * @param inspectionDurationDistribution      distribution over inspection closure
     *                                            durations in ticks; may be {@code null}
     *                                            if inspections will not occur
     * @param snowClearanceDurationDistribution   distribution over snow-clearance closure
     *                                            durations in ticks; may be {@code null}
     *                                            if snow clearance will not occur
     * @param equipmentFailureDurationDistribution distribution over equipment-failure
     *                                            closure durations in ticks; may be
     *                                            {@code null} if equipment failures will
     *                                            not occur
     * @param emergencyStatusDistribution         enumerated distribution over initial
     *                                            {@link AircraftStatus} values for
     *                                            arriving aircraft; may be {@code null}
     *                                            if landing generation will not be called
     * @param runwayStatusDistribution            enumerated distribution over post-operation
     *                                            {@link RunwayStatus} values; may be
     *                                            {@code null} if runway-free generation
     *                                            will not be called
     * @param maxDelay                            maximum ticks beyond scheduled departure
     *                                            time before a flight is cancelled; must
     *                                            be non-negative
     * @param lowFuelThreshold                    remaining fuel level in ticks at which
     *                                            the low-fuel status is triggered; must be
     *                                            non-negative
     */
    public EventGenerator(
        PriorityQueue<SimulationEvent> eventQueue,
        RealDistribution delayDistribution,
        RealDistribution remainingFuelDistribution,
        RealDistribution healthEscalationTimeDistribution,
        RealDistribution inspectionDurationDistribution,
        RealDistribution snowClearanceDurationDistribution,
        RealDistribution equipmentFailureDurationDistribution,
        EnumeratedDistribution<AircraftStatus> emergencyStatusDistribution,
        EnumeratedDistribution<RunwayStatus> runwayStatusDistribution,
        long maxDelay,
        long lowFuelThreshold)
    {
        this.eventQueue = eventQueue;
        this.delayDistribution = delayDistribution;
        this.remainingFuelDistribution = remainingFuelDistribution;
        this.healthEscalationTimeDistribution = healthEscalationTimeDistribution;
        this.inspectionDurationDistribution = inspectionDurationDistribution;
        this.snowClearanceDurationDistribution = snowClearanceDurationDistribution;
        this.equipmentFailureDurationDistribution = equipmentFailureDurationDistribution;
        this.emergencyStatusDistribution = emergencyStatusDistribution;
        this.runwayStatusDistribution = runwayStatusDistribution;
        this.maxDelay = maxDelay;
        this.lowFuelThreshold = lowFuelThreshold;
    }

    /**
     * Generates and enqueues the complete set of events required to model an arriving
     * aircraft from holding-queue entry through to its diversion deadline.
     *
     * <p>The following events are always produced:
     * <ol>
     *   <li>A {@link HoldingQueueEnterEvent} at
     *       {@code max(scheduledTime + delay, currentTime)}, where {@code delay} is
     *       sampled from {@link #delayDistribution}. This establishes the tick at which
     *       the aircraft enters the holding queue.</li>
     *   <li>An {@link AircraftStatusChangeEvent} targeting {@link AircraftStatus#LOW_FUEL}
     *       at {@code holdingQueueEnterTime + remainingFuel - lowFuelThreshold}, where
     *       {@code remainingFuel} is sampled from {@link #remainingFuelDistribution}.
     *       This event fires only if the aircraft is still in the queue at that tick.</li>
     *   <li>A {@link LandingDivertedEvent} at the same tick as the low-fuel event, acting
     *       as the aircraft's final diversion deadline should it not have landed by then.
     *       Because {@link LandingDivertedEvent} has {@link SimulationEvent#order}
     *       {@code 1}, it always fires after the status-change event at the same tick.</li>
     * </ol>
     *
     * <p>If the sampled initial status is {@link AircraftStatus#PASSENGER_HEALTH}, two
     * additional events are produced:
     * <ol>
     *   <li>An {@link AircraftStatusChangeEvent} targeting
     *       {@link AircraftStatus#PASSENGER_HEALTH_SEVERE} at
     *       {@code holdingQueueEnterTime + healthEscalationTime}, where
     *       {@code healthEscalationTime} is sampled from
     *       {@link #healthEscalationTimeDistribution}.</li>
     *   <li>A second {@link LandingDivertedEvent} at the same escalation tick, giving the
     *       aircraft an earlier diversion deadline tied to the severity escalation.</li>
     * </ol>
     *
     * @param aircraft    the arriving aircraft for which events are to be generated;
     *                    must not be {@code null}
     * @param currentTime the current simulation tick, used as a lower bound on the
     *                    computed queue-entry time; must be non-negative
     * @throws NullPointerException if {@link #delayDistribution},
     *                              {@link #remainingFuelDistribution}, or
     *                              {@link #emergencyStatusDistribution} is {@code null}
     */
    public void generateLandingEvents(Aircraft aircraft, long currentTime)
    {
        long scheduledTime = aircraft.getScheduledTime();

        double delay = delayDistribution.sample();
        long holdingQueueEnterTime = Math.max(scheduledTime + Math.round(delay), currentTime);

        double remainingFuel = remainingFuelDistribution.sample();
        long lowFuelTime = holdingQueueEnterTime + (Math.round(remainingFuel) - lowFuelThreshold);

        AircraftStatus status = emergencyStatusDistribution.sample();

        eventQueue.add(new HoldingQueueEnterEvent(holdingQueueEnterTime, aircraft, status));
        eventQueue.add(new AircraftStatusChangeEvent(lowFuelTime, aircraft, AircraftStatus.LOW_FUEL));
        eventQueue.add(new LandingDivertedEvent(lowFuelTime, aircraft));

        if (status == AircraftStatus.PASSENGER_HEALTH)
        {
            double healthEscalationTime = healthEscalationTimeDistribution.sample();
            long passengerHealthSevereTime = holdingQueueEnterTime + Math.round(healthEscalationTime);

            eventQueue.add(new AircraftStatusChangeEvent(passengerHealthSevereTime, aircraft, AircraftStatus.PASSENGER_HEALTH_SEVERE));
            eventQueue.add(new LandingDivertedEvent(passengerHealthSevereTime, aircraft));
        }
    }

    /**
     * Generates and enqueues the complete set of events required to model a departing
     * aircraft from takeoff-queue entry through to its cancellation deadline.
     *
     * <p>Two events are always produced:
     * <ol>
     *   <li>A {@link TakeoffQueueEnterEvent} at
     *       {@code max(scheduledTime + delay, currentTime)}, where {@code delay} is
     *       sampled from {@link #delayDistribution}.</li>
     *   <li>A {@link TakeoffCanceledEvent} at {@code scheduledTime + maxDelay}, acting as
     *       the aircraft's cancellation deadline. If the aircraft is still in the takeoff
     *       queue when this fires it will be removed and marked
     *       {@link AircraftStatus#CANCELLED}; if it has already departed the event is a
     *       no-op.</li>
     * </ol>
     *
     * @param aircraft    the departing aircraft for which events are to be generated;
     *                    must not be {@code null}
     * @param currentTime the current simulation tick, used as a lower bound on the
     *                    computed queue-entry time; must be non-negative
     * @throws NullPointerException if {@link #delayDistribution} is {@code null}
     */
    public void generateTakeoffEvents(Aircraft aircraft, long currentTime)
    {
        long scheduledTime = aircraft.getScheduledTime();

        double delay = delayDistribution.sample();
        long takeoffQueueEnterTime = Math.max(scheduledTime + Math.round(delay), currentTime);
        long takeoffCanceledTime = scheduledTime + maxDelay;

        eventQueue.add(new TakeoffQueueEnterEvent(takeoffQueueEnterTime, aircraft));
        eventQueue.add(new TakeoffCanceledEvent(takeoffCanceledTime, aircraft));
    }

    /**
     * Generates and enqueues the runway-free or runway-closure event that follows a
     * completed takeoff or landing, computing the occupation duration from the aircraft's
     * ground speed and the runway's physical length.
     *
     * <p>The occupation duration is derived as
     * {@code runwayLength / round(groundSpeed * KNOTS_TO_METERS_PER_SECOND)}, giving the
     * number of simulation ticks until the runway is vacated. A post-operation
     * {@link RunwayStatus} is then sampled from {@link #runwayStatusDistribution}:
     * <ul>
     *   <li>If the sampled status maps to a non-{@code null} duration distribution via
     *       {@link #getRunwayClosureDurationDistribution(RunwayStatus)}, a
     *       {@link RunwayClosureEvent} is scheduled at {@code currentTime + occupiedDuration}
     *       with a closure duration sampled from that distribution.</li>
     *   <li>Otherwise, when the sampled status is {@link RunwayStatus#FREE}, a plain
     *       {@link RunwayFreeEvent} is scheduled at the same tick, which simply releases
     *       the runway without a subsequent closure period.</li>
     * </ul>
     *
     * @param runway      the runway that has just been used; must not be {@code null}
     * @param aircraft    the aircraft that just completed its operation; its
     *                    {@link Aircraft#getGroundSpeed() groundSpeed} is used to compute
     *                    the occupation duration; must not be {@code null}
     * @param currentTime the simulation tick at which the operation began; the free or
     *                    closure event is scheduled at {@code currentTime + occupiedDuration};
     *                    must be non-negative
     * @throws NullPointerException if {@link #runwayStatusDistribution} is {@code null}
     */
    public void generateRunwayFreeEvent(Runway runway, Aircraft aircraft, long currentTime)
    {
        double groundSpeedMetersPerSecond = ((double) aircraft.getGroundSpeed()) * SimulationConstants.KNOTS_TO_METERS_PER_SECOND;

        long occupiedDuration = runway.getLength() / Math.round(groundSpeedMetersPerSecond);

        RunwayStatus runwayStatus = runwayStatusDistribution.sample();

        RealDistribution runwayClosureDurationDistribution = getRunwayClosureDurationDistribution(runwayStatus);

        if (runwayClosureDurationDistribution != null)
        {
            long runwayClosureTime = currentTime + occupiedDuration;
            long runwayClosureDuration = Math.round(runwayClosureDurationDistribution.sample());

            eventQueue.add(new RunwayClosureEvent(runwayClosureTime, runwayClosureDuration, runway, runwayStatus, eventQueue));
        }
        else
        {
            long runwayFreeTime = currentTime + occupiedDuration;
            eventQueue.add(new RunwayFreeEvent(runwayFreeTime, runway));
        }
    }

    /**
     * Seeds all runways into a valid initial state at the start of the simulation by
     * enqueuing a {@link RunwayFreeEvent} and, where appropriate, a paired
     * {@link RunwayClosureEvent} for each runway at {@code t = 0}.
     *
     * <p>For every runway in {@code runways}:
     * <ol>
     *   <li>A {@link RunwayFreeEvent} at tick {@code 0} is always added first, ensuring
     *       no runway begins the simulation in an {@link RunwayStatus#OCCUPIED} state that
     *       has no corresponding free event.</li>
     *   <li>If the runway's configured {@link RunwayStatus} maps to a non-{@code null}
     *       duration distribution via {@link #getRunwayClosureDurationDistribution(RunwayStatus)},
     *       a {@link RunwayClosureEvent} at tick {@code 0} is also added, causing the
     *       runway to begin in a closed state immediately.</li>
     * </ol>
     *
     * <p>This method is a one-time initialisation step and returns immediately without
     * modifying the event queue if {@code currentTime} is not {@code 0}.
     *
     * @param runways     the list of all runways to initialise; must not be {@code null}
     * @param currentTime the current simulation tick; this method is a no-op unless
     *                    {@code currentTime == 0}
     */
    public void generateInitialRunwayFreeEvents(List<Runway> runways, long currentTime)
    {
        if (currentTime != 0)
            return;

        for (Runway runway : runways)
        {
            RunwayStatus runwayStatus = runway.getStatus();
            RealDistribution runwayClosureDurationDistribution = getRunwayClosureDurationDistribution(runwayStatus);

            eventQueue.add(new RunwayFreeEvent(currentTime, runway));

            if (runwayClosureDurationDistribution != null)
            {
                long runwayClosureDuration = Math.round(runwayClosureDurationDistribution.sample());
                eventQueue.add(new RunwayClosureEvent(currentTime, runwayClosureDuration, runway, runwayStatus, eventQueue));
            }
        }
    }

    /**
     * Enqueues a {@link RunwayClosureEvent} for the given runway if it is currently
     * {@link RunwayStatus#FREE} and the target status is a recognised closure type.
     *
     * <p>The method first validates that the runway is in {@link RunwayStatus#FREE} state,
     * since scheduling a closure for an occupied or already-closed runway could produce
     * conflicting events. If valid, the appropriate duration distribution is retrieved via
     * {@link #getRunwayClosureDurationDistribution(RunwayStatus)} and a
     * {@link RunwayClosureEvent} is added at {@code currentTime}.
     *
     * @param runway       the runway to close; must not be {@code null}
     * @param targetStatus the closure status to apply; must map to a non-{@code null}
     *                     duration distribution in
     *                     {@link #getRunwayClosureDurationDistribution(RunwayStatus)}
     * @param currentTime  the simulation tick at which the closure should begin; must be
     *                     non-negative
     * @return {@code true} if a {@link RunwayClosureEvent} was successfully enqueued;
     *         {@code false} if the runway's current status is not {@link RunwayStatus#FREE}
     *         and the closure was therefore not scheduled
     */
    public boolean generateRunwayClosureEvent(Runway runway, RunwayStatus targetStatus, long currentTime)
    {
        RunwayStatus runwayStatus = runway.getStatus();

        if (runwayStatus != RunwayStatus.FREE)
            return false;

        RealDistribution runwayClosureDurationDistribution = getRunwayClosureDurationDistribution(targetStatus);

        if (runwayClosureDurationDistribution != null)
        {
            long runwayClosureDuration = Math.round(runwayClosureDurationDistribution.sample());
            eventQueue.add(new RunwayClosureEvent(currentTime, runwayClosureDuration, runway, targetStatus, eventQueue));
        }

        return true;
    }

    /**
     * Enqueues a {@link RunwayModeChangeEvent} that will transition the given runway to
     * the specified mode at the current simulation tick.
     *
     * <p>The mode change is subject to the safety check in
     * {@link RunwayModeChangeEvent#execute}, which will refuse the transition if it would
     * leave no available runway for a flight type. The outcome is therefore not guaranteed
     * at call time.
     *
     * @param runway      the runway whose mode is to be changed; must not be {@code null}
     * @param targetMode  the mode to transition the runway to; must not be {@code null}
     * @param currentTime the simulation tick at which the mode change should be applied;
     *                    must be non-negative
     */
    public void generateRunwayModeChangeEvent(Runway runway, RunwayMode targetMode, long currentTime)
    {
        eventQueue.add(new RunwayModeChangeEvent(currentTime, runway, targetMode));
    }

    /**
     * Returns the delay distribution used to sample arrival and departure delay offsets.
     *
     * @return the current {@link #delayDistribution}; may be {@code null} if not yet set
     */
    public RealDistribution getDelayDistribution() { return delayDistribution; }

    /**
     * Sets the delay distribution used to sample arrival and departure delay offsets.
     *
     * @param delayDistribution the new delay distribution; must not be {@code null} before
     *                          calling {@link #generateLandingEvents(Aircraft, long)} or
     *                          {@link #generateTakeoffEvents(Aircraft, long)}
     */
    public void setDelayDistribution(RealDistribution delayDistribution) { this.delayDistribution = delayDistribution; }

    /**
     * Returns the distribution used to sample remaining fuel for arriving aircraft.
     *
     * @return the current {@link #remainingFuelDistribution}; may be {@code null} if not yet set
     */
    public RealDistribution getRemainingFuelDistribution() { return remainingFuelDistribution; }

    /**
     * Sets the distribution used to sample remaining fuel for arriving aircraft.
     *
     * @param remainingFuelDistribution the new distribution; must not be {@code null}
     *                                  before calling
     *                                  {@link #generateLandingEvents(Aircraft, long)}
     */
    public void setRemainingFuelDistribution(RealDistribution remainingFuelDistribution) { this.remainingFuelDistribution = remainingFuelDistribution; }

    /**
     * Returns the distribution used to sample health-escalation intervals for aircraft
     * arriving with {@link AircraftStatus#PASSENGER_HEALTH}.
     *
     * @return the current {@link #healthEscalationTimeDistribution}; may be {@code null}
     *         if not yet set
     */
    public RealDistribution getHealthEscalationTimeDistribution() { return healthEscalationTimeDistribution; }

    /**
     * Sets the distribution used to sample health-escalation intervals.
     *
     * @param healthEscalationTimeDistribution the new distribution; must not be
     *                                         {@code null} if aircraft with
     *                                         {@link AircraftStatus#PASSENGER_HEALTH}
     *                                         are expected
     */
    public void setHealthEscalationTimeDistribution(RealDistribution healthEscalationTimeDistribution) { this.healthEscalationTimeDistribution = healthEscalationTimeDistribution; }

    /**
     * Returns the distribution used to sample runway-inspection closure durations.
     *
     * @return the current {@link #inspectionDurationDistribution}; may be {@code null}
     *         if not yet set
     */
    public RealDistribution getInspectionDurationDistribution() { return inspectionDurationDistribution; }

    /**
     * Sets the distribution used to sample runway-inspection closure durations.
     *
     * @param inspectionDurationDistribution the new distribution; must not be {@code null}
     *                                       if inspection closures may occur
     */
    public void setInspectionDurationDistribution(RealDistribution inspectionDurationDistribution) { this.inspectionDurationDistribution = inspectionDurationDistribution; }

    /**
     * Returns the distribution used to sample snow-clearance closure durations.
     *
     * @return the current {@link #snowClearanceDurationDistribution}; may be {@code null}
     *         if not yet set
     */
    public RealDistribution getSnowClearanceDurationDistribution() { return snowClearanceDurationDistribution; }

    /**
     * Sets the distribution used to sample snow-clearance closure durations.
     *
     * @param snowClearanceDurationDistribution the new distribution; must not be
     *                                          {@code null} if snow-clearance closures
     *                                          may occur
     */
    public void setSnowClearanceDurationDistribution(RealDistribution snowClearanceDurationDistribution) { this.snowClearanceDurationDistribution = snowClearanceDurationDistribution; }

    /**
     * Returns the distribution used to sample equipment-failure closure durations.
     *
     * @return the current {@link #equipmentFailureDurationDistribution}; may be
     *         {@code null} if not yet set
     */
    public RealDistribution getEquipmentFailureDurationDistribution() { return equipmentFailureDurationDistribution; }

    /**
     * Sets the distribution used to sample equipment-failure closure durations.
     *
     * @param equipmentFailureDurationDistribution the new distribution; must not be
     *                                             {@code null} if equipment-failure
     *                                             closures may occur
     */
    public void setEquipmentFailureDurationDistribution(RealDistribution equipmentFailureDurationDistribution) { this.equipmentFailureDurationDistribution = equipmentFailureDurationDistribution; }

    /**
     * Returns the enumerated distribution over initial emergency statuses for arriving
     * aircraft.
     *
     * @return the current {@link #emergencyStatusDistribution}; may be {@code null} if
     *         not yet set
     */
    public EnumeratedDistribution<AircraftStatus> getEmergencyStatusDistribution() { return emergencyStatusDistribution; }

    /**
     * Sets the enumerated distribution over initial emergency statuses for arriving
     * aircraft.
     *
     * @param emergencyStatusDistribution the new distribution; must not be {@code null}
     *                                    before calling
     *                                    {@link #generateLandingEvents(Aircraft, long)}
     */
    public void setEmergencyStatusDistribution(EnumeratedDistribution<AircraftStatus> emergencyStatusDistribution) { this.emergencyStatusDistribution = emergencyStatusDistribution; }

    /**
     * Returns the enumerated distribution over post-operation runway statuses.
     *
     * @return the current {@link #runwayStatusDistribution}; may be {@code null} if not
     *         yet set
     */
    public EnumeratedDistribution<RunwayStatus> getRunwayStatusDistribution() { return runwayStatusDistribution; }

    /**
     * Sets the enumerated distribution over post-operation runway statuses.
     *
     * @param runwayStatusDistribution the new distribution; must not be {@code null}
     *                                 before calling
     *                                 {@link #generateRunwayFreeEvent(Runway, Aircraft, long)}
     */
    public void setRunwayStatusDistribution(EnumeratedDistribution<RunwayStatus> runwayStatusDistribution) { this.runwayStatusDistribution = runwayStatusDistribution; }

    /**
     * Returns the maximum delay in ticks beyond a flight's scheduled departure time
     * before an unserved departing aircraft is cancelled.
     *
     * @return the current {@link #maxDelay} value; {@code 0} if not explicitly set
     */
    public long getMaxDelay() { return maxDelay; }

    /**
     * Sets the maximum delay in ticks beyond scheduled departure time before cancellation.
     *
     * @param maxDelay the new maximum delay; must be non-negative
     */
    public void setMaxDelay(long maxDelay) { this.maxDelay = maxDelay; }

    /**
     * Returns the remaining-fuel threshold in ticks at which an aircraft triggers a
     * low-fuel status change.
     *
     * @return the current {@link #lowFuelThreshold} value; {@code 0} if not explicitly set
     */
    public long getLowFuelThreshold() { return lowFuelThreshold; }

    /**
     * Sets the remaining-fuel threshold in ticks at which a low-fuel status change is
     * triggered.
     *
     * @param lowFuelThreshold the new threshold; must be non-negative
     */
    public void setLowFuelThreshold(long lowFuelThreshold) { this.lowFuelThreshold = lowFuelThreshold; }

    /**
     * Maps a {@link RunwayStatus} to the {@link RealDistribution} that governs the
     * duration of the corresponding runway closure, or returns {@code null} for statuses
     * that do not represent a closure.
     *
     * <p>This mapping drives the branching logic in
     * {@link #generateRunwayFreeEvent(Runway, Aircraft, long)} and
     * {@link #generateInitialRunwayFreeEvents(List, long)}: a non-{@code null} return
     * value indicates that a {@link RunwayClosureEvent} should be scheduled, while
     * {@code null} indicates that a plain {@link RunwayFreeEvent} suffices.
     *
     * @param runwayStatus the runway status to look up; must not be {@code null}
     * @return {@link #inspectionDurationDistribution} for {@link RunwayStatus#INSPECTION};
     *         {@link #snowClearanceDurationDistribution} for {@link RunwayStatus#SNOW_CLEARANCE};
     *         {@link #equipmentFailureDurationDistribution} for
     *         {@link RunwayStatus#EQUIPMENT_FAILURE}; {@code null} for all other statuses
     */
    private RealDistribution getRunwayClosureDurationDistribution(RunwayStatus runwayStatus)
    {
        return switch (runwayStatus)
        {
            case INSPECTION        -> inspectionDurationDistribution;
            case SNOW_CLEARANCE    -> snowClearanceDurationDistribution;
            case EQUIPMENT_FAILURE -> equipmentFailureDurationDistribution;
            default                -> null;
        };
    }
}