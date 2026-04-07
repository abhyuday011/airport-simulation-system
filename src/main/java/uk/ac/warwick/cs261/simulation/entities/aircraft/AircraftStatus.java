package uk.ac.warwick.cs261.simulation.entities.aircraft;

/**
 * Represents the operational or emergency status of an {@link Aircraft} at any point
 * during the simulation.
 */
public enum AircraftStatus 
{
    OK,
    MECHANICAL_FAILURE,
    PASSENGER_HEALTH,
    PASSENGER_HEALTH_SEVERE,
    LOW_FUEL,
    ARRIVED,
    DEPARTED,
    CANCELLED,
    DIVERTED
}
