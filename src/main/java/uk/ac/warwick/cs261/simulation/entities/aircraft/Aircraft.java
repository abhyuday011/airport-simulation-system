package uk.ac.warwick.cs261.simulation.entities.aircraft;

/**
 * Represents a single aircraft in the airport simulation, either arriving or departing.
 *
 * <p>Each instance carries the scheduling and timing data needed to model an aircraft's
 * journey through the simulation: its scheduled time, the moment it joins a queue, and
 * the moment it actually takes off or lands. These three timestamps drive the two derived
 * metrics exposed by this class, wait time and delay.
 *
 * <p>Aircraft are totally ordered via {@link #compareTo(Aircraft)} so they can be managed
 * by a priority queue. The ordering differs by flight type:
 * <ul>
 *   <li><b>Departing</b> aircraft are ordered by {@code queueEnterTime} alone (FIFO).</li>
 *   <li><b>Arriving</b> aircraft are ordered first by {@link AircraftStatus} severity
 *       (higher severity = higher priority), then by {@code queueEnterTime} as a
 *       tiebreaker (FIFO within the same severity band).</li>
 * </ul>
 *
 * <p><b>Timing sentinel values:</b> both {@code queueEnterTime} and {@code actualTime}
 * are initialised to {@code -1}, indicating "not yet set".
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code status} defaults to {@link AircraftStatus#OK} at construction and may be
 *       updated at any point via {@link #setStatus(AircraftStatus)}.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class Aircraft implements Comparable<Aircraft> 
{   
    /** {@code true} if this aircraft is departing; {@code false} if arriving. */
    private final boolean isTakeoff;

     /** The call sign that uniquely identifies this flight, e.g. {@code "BA118"}. */
    private final String callSign;

    /** The aircraft's ground speed in knots. */
    private final long groundSpeed;

    /** The scheduled departure or arrival time, in simulation ticks. */
    private long scheduledTime;

    /**
     * The simulation tick at which this aircraft joined the holding or takeoff queue.
     * Initialised to {@code -1}; a value of {@code -1} at the time of landing or takeoff
     * indicates the aircraft bypassed the queue entirely and went straight to the runway.
     */
    private long queueEnterTime;

    /**
     * The simulation tick at which this aircraft actually took off or landed.
     * Remains {@code -1} until the event occurs; a value of {@code -1} indicates the
     * flight is still in progress, cancelled, or diverted.
     */
    private long actualTime;

    /**
     * The current emergency or operational status of this aircraft.
     * Affects landing priority for arriving aircraft via {@link #compareTo(Aircraft)}.
     */
    private AircraftStatus status;

    /**
     * Constructs a new {@code Aircraft} with default status {@link AircraftStatus#OK} and
     * uninitialised queue and actual times (both set to {@code -1}).
     *
     * @param callSign      the flight's call sign, e.g. {@code "BA118"}; must not be {@code null}
     * @param scheduledTime the scheduled departure or arrival time, in simulation ticks
     * @param groundSpeed   the aircraft's ground speed in knots
     * @param isTakeoff     {@code true} if this is a departing flight; {@code false} if arriving
     */
    public Aircraft(String callSign, long scheduledTime, long groundSpeed, boolean isTakeoff)
    {
        this.callSign = callSign;
        this.scheduledTime = scheduledTime;
        this.groundSpeed = groundSpeed;
        this.isTakeoff = isTakeoff;

        this.queueEnterTime = -1;
        this.actualTime = -1;
        this.status = AircraftStatus.OK;
    }

    /**
     * Compares this aircraft to another for priority-queue ordering.
     *
     * <p>The comparison strategy depends on flight type:
     * <ul>
     *   <li><b>Departing ({@code isTakeoff == true}):</b> ordered by {@code queueEnterTime}
     *       ascending (earliest-queued first, i.e. FIFO).</li>
     *   <li><b>Arriving ({@code isTakeoff == false}):</b> ordered by {@link AircraftStatus}
     *       severity descending (highest emergency first). When severities are equal,
     *       {@code queueEnterTime} ascending is used as the tiebreaker.</li>
     * </ul>
     *
     * @param aircraft the aircraft to compare against; must not be {@code null}
     * @return a negative integer if this aircraft has higher priority than {@code aircraft},
     *         zero if they have equal priority, or a positive integer if lower priority
     */
    @Override
    public int compareTo(Aircraft aircraft) 
    {
        if (isTakeoff)
            return Long.compare(queueEnterTime, aircraft.queueEnterTime);

        int ourSeverity = getAircraftStatusSeverity(status);
        int otherSeverity = getAircraftStatusSeverity(aircraft.status);

        int result = otherSeverity - ourSeverity;

        if (result == 0)
            return Long.compare(queueEnterTime, aircraft.queueEnterTime);

        return Math.max(-1, Math.min(1, result));
    }

    /** Returns {@code true} if this is a departing flight; {@code false} if arriving. */
    public boolean getIsTakeoff() { return isTakeoff; }

    /** Returns the call sign that uniquely identifies this flight, e.g. {@code "BA118"}. */
    public String getCallSign() { return callSign; }

    /** Returns the ground speed of this aircraft in knots. */
    public long getGroundSpeed() { return groundSpeed; }

    /** Returns the scheduled departure or arrival time, in simulation ticks. */
    public long getScheduledTime() { return scheduledTime; }

    /**
     * Sets the scheduled departure or arrival time, in simulation ticks.
     *
     * @param scheduledTime the new scheduled time, in simulation ticks
     */
    public void setScheduledTime(long scheduledTime) { this.scheduledTime = scheduledTime; }

    /**
     * Returns the simulation tick at which this aircraft joined the queue, or {@code -1}
     * if it has not yet entered a queue.
     */
    public long getQueueEnterTime() { return queueEnterTime; }

    /**
     * Sets the simulation tick at which this aircraft joined the queue.
     *
     * @param queueEnterTime the simulation tick at which the aircraft entered the queue
     */
    public void setQueueEnterTime(long queueEnterTime) { this.queueEnterTime = queueEnterTime; }

    /**
     * Returns the simulation tick at which this aircraft actually took off or landed,
     * or {@code -1} if the event has not yet occurred or the flight was cancelled/diverted.
     */
    public long getActualTime() { return actualTime; }

    /**
     * Sets the simulation tick at which this aircraft actually took off or landed.
     *
     * @param actualTime the simulation tick at which the aircraft took off or landed
     */
    public void setActualTime(long actualTime) { this.actualTime = actualTime; }

    /** Returns the current operational or emergency status of this aircraft. */
    public AircraftStatus getStatus() { return status; }
    
    /**
     * Sets the current operational or emergency status of this aircraft.
     *
     * @param status the new {@link AircraftStatus}; must not be {@code null}
     */
    public void setStatus(AircraftStatus status) { this.status = status; }

    /**
     * Calculates the total time this aircraft spent waiting in the holding or takeoff queue.
     *
     * <p>Returns {@code 0} if {@code queueEnterTime} is {@code -1}, meaning the aircraft
     * has not yet entered a queue. Note that this may mask the case where the aircraft
     * genuinely has not been queued yet; callers should verify {@code queueEnterTime} is
     * set before relying on this value.
     *
     * @return the number of simulation ticks between joining the queue and the actual
     *         takeoff or landing time, or {@code 0} if the aircraft has not yet entered a queue
     * @throws IllegalStateException if {@code actualTime} is {@code -1}, indicating the
     *         flight has not yet completed, or was cancelled or diverted
     */
    public long calculateWaitTime() 
    {
        if (actualTime == -1)
            throw new IllegalStateException("Cannot calculate wait time for cancelled/diverted flight, or flight still in queue");

        if (queueEnterTime == -1)
            return 0;

        return actualTime - queueEnterTime;
    }

    /**
     * Calculates how late this aircraft departed or arrived relative to its schedule.
     *
     * <p>Returns {@code 0} rather than a negative value if the aircraft actually departed
     * or landed ahead of schedule, since early operations are not counted as delay.
     *
     * @return the number of simulation ticks by which the flight was delayed, or {@code 0}
     *         if it departed or landed on time or early
     * @throws IllegalStateException if {@code actualTime} is {@code -1}, indicating the
     *         flight is still in progress, cancelled, or diverted
     */
    public long calculateDelay()
    {
        if (actualTime == -1)
            throw new IllegalStateException("Cannot calculate delay for cancelled/diverted flight, or flight still in queue");

        long delayTime = actualTime - scheduledTime;
        
        return Math.max(delayTime, 0);
    }

    /**
     * Returns a human-readable summary of this aircraft's current state.
     *
     * <p>Timing fields that have not yet been set will appear as {@code -1}.
     *
     * @return a formatted string containing the call sign, flight type, status,
     *         ground speed, and all three timing fields
     */
    @Override
    public String toString() 
    {
        String type = isTakeoff ? "departing" : "arriving";
        return String.format(
            "Aircraft [callSign=%s, flightType=%s, status=%s, groundSpeed=%d, scheduledTime=%d, queueEnterTime=%d, actualTime=%d]",
            callSign, type, status, groundSpeed, scheduledTime, queueEnterTime, actualTime
        );
    }

    /**
     * Maps an {@link AircraftStatus} value to a numeric severity used for landing priority.
     *
     * <p>Higher values represent greater urgency. Statuses not explicitly listed return
     * {@code 0} (no elevated priority).
     *
     * @param status the status to evaluate; must not be {@code null}
     * @return {@code 2} for {@code LOW_FUEL} or {@code PASSENGER_HEALTH_SEVERE};
     *         {@code 1} for {@code MECHANICAL_FAILURE} or {@code PASSENGER_HEALTH};
     *         {@code 0} for all other statuses
     */
    private int getAircraftStatusSeverity(AircraftStatus status)
    {
        return switch(status)
        {
            case MECHANICAL_FAILURE, PASSENGER_HEALTH -> 1;
            case PASSENGER_HEALTH_SEVERE, LOW_FUEL -> 2;
            default -> 0;
        };
    }
}

