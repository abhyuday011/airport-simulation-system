package uk.ac.warwick.cs261.simulation.entities.runway;

import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;

/**
 * Represents a single runway in the airport simulation, encapsulating its identity,
 * physical parameters, and the aircraft currently occupying it.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@link #getAircraft()} returns {@code null} when the runway is unoccupied.</li>
 *   <li>This class is not thread-safe.</li>
 * </ul>
 */
public class Runway
{
    /** The human-readable identifier for this runway. */
    private String runwayName;

    /**
     * The physical and operational parameters of this runway.
     * Delegates storage of mode, status, and length to avoid bloating this class.
     */
    private RunwayParameters parameters;

    /**
     * The aircraft currently occupying this runway, or {@code null} if the runway
     * is unoccupied.
     */
    private Aircraft aircraft;

    /**
     * Constructs a {@code Runway} with the given name and parameters, initially unoccupied.
     *
     * @param runwayName the human-readable runway identifier;
     *                   must not be {@code null}
     * @param parameters the physical and operational parameters for this runway;
     *                   must not be {@code null}
     */
    public Runway(String runwayName, RunwayParameters parameters)
    {
        this.runwayName = runwayName;
        this.parameters = parameters;
    }

    /** Returns the human-readable identifier for this runway.*/
    public String getRunwayName() { return runwayName; }

    /**
     * Sets the human-readable identifier for this runway.
     *
     * @param runwayName the new runway identifier, e.g. {@code "27L"}; must not be {@code null}
     */
    public void setRunwayName(String runwayName) { this.runwayName = runwayName; }

    /** Returns the current operational mode of this runway. */
    public RunwayMode getMode() { return parameters.getMode(); }

    /**
     * Sets the operational mode of this runway.
     *
     * @param mode the new {@link RunwayMode}; must not be {@code null}
     */
    public void setMode(RunwayMode mode) { parameters.setMode(mode); }

    /** Returns the current operational status of this runway. */
    public RunwayStatus getStatus() { return parameters.getStatus(); }

    /**
     * Sets the operational status of this runway.
     *
     * @param status the new {@link RunwayStatus}; must not be {@code null}
     */
    public void setStatus(RunwayStatus status) { parameters.setStatus(status); }

    /**
     * Returns the aircraft currently occupying this runway, or {@code null} if the
     * runway is unoccupied.
     */
    public Aircraft getAircraft() { return aircraft; }

    /**
     * Sets the aircraft currently occupying this runway.
     * Pass {@code null} to mark the runway as unoccupied once the aircraft has cleared it.
     *
     * @param aircraft the {@link Aircraft} now on this runway, or {@code null} if vacated
     */
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }

    /** Returns the length of this runway in metres. */
    public long getLength() { return parameters.getLength(); }
}