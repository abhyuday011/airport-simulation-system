package uk.ac.warwick.cs261.simulation.entities.runway;

/**
 * Represents the states that a runway may occupy at any point during
 * the simulation.
 */
public enum RunwayStatus
{
    FREE,
    OCCUPIED,
    INSPECTION,
    SNOW_CLEARANCE,
    EQUIPMENT_FAILURE
}