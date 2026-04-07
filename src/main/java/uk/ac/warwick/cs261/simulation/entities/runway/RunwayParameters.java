package uk.ac.warwick.cs261.simulation.entities.runway;

/**
 * A value-object that captures the configuration parameters used to initialise or
 * reconfigure a {@link Runway} instance.
 *
 * <p>This class acts as a simple data-transfer object carrying the three properties that
 * define a runway's initial state: its operational {@link RunwayMode}, its current
 * {@link RunwayStatus}, and its physical length in metres.
 *
 * <p>The no-argument constructor {@link #RunwayParameters()} provides safe defaults,
 * {@link RunwayMode#LANDING}, {@link RunwayStatus#FREE}, and a length of {@code 0}, so
 * that callers may set only the properties they care about.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>No validation is performed on setter arguments; callers are responsible for
 *       supplying non-{@code null} {@link RunwayMode} and {@link RunwayStatus} values.</li>
 *   <li>A {@code length} of {@code 0} is permitted by this class but may be rejected
 *       downstream by consumers that require a positive runway length.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class RunwayParameters
{
    /**
     * The operational mode that determines which flight types the runway may serve.
     * Defaults to {@link RunwayMode#LANDING} when constructed via {@link #RunwayParameters()}.
     */
    private RunwayMode mode;

    /**
     * The current availability status of the runway.
     * Defaults to {@link RunwayStatus#FREE} when constructed via {@link #RunwayParameters()}.
     */
    private RunwayStatus status;

    /**
     * The physical length of the runway in metres.
     * Defaults to {@code 0} when constructed via {@link #RunwayParameters()}.
     */
    private long length;

    /**
     * Constructs a {@code RunwayParameters} instance with default values: mode
     * {@link RunwayMode#LANDING}, status {@link RunwayStatus#FREE}, and length {@code 0}.
     *
     * <p>This constructor is provided for contexts where only a subset of parameters need
     * to be customised; use the setters to override individual fields after construction.
     */
    public RunwayParameters() { this(RunwayMode.LANDING, RunwayStatus.FREE, 0); }

    /**
     * Constructs a {@code RunwayParameters} instance with all fields explicitly specified.
     *
     * @param mode   the operational mode governing which flight types the runway may serve;
     *               must not be {@code null}
     * @param status the initial availability status of the runway; must not be {@code null}
     * @param length the physical length of the runway in metres; must be non-negative
     */
    public RunwayParameters(RunwayMode mode, RunwayStatus status, long length)
    {
        this.mode = mode;
        this.status = status;
        this.length = length;
    }

    /**
     * Returns the operational mode that determines which flight types the runway may serve.
     *
     * @return the current {@link RunwayMode};
     */
    public RunwayMode getMode() { return mode; }

    /**
     * Sets the operational mode that determines which flight types the runway may serve.
     *
     * @param mode the new {@link RunwayMode}; must not be {@code null}
     */
    public void setMode(RunwayMode mode) { this.mode = mode; }

    /**
     * Returns the current availability status of the runway.
     *
     * @return the current {@link RunwayStatus};
     */
    public RunwayStatus getStatus() { return status; }

    /**
     * Sets the current availability status of the runway.
     *
     * @param status the new {@link RunwayStatus}; must not be {@code null}
     */
    public void setStatus(RunwayStatus status) { this.status = status; }

    /**
     * Returns the physical length of the runway in metres.
     *
     * @return the runway length in metres;
     */
    public long getLength() { return length; }

    /**
     * Sets the physical length of the runway in metres.
     *
     * @param length the runway length in metres; must be non-negative
     */
    public void setLength(long length) { this.length = length; }
}