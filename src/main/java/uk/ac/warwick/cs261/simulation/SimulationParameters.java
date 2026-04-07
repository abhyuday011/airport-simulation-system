package uk.ac.warwick.cs261.simulation;

import java.util.ArrayList;
import java.util.List;

import uk.ac.warwick.cs261.simulation.entities.runway.RunwayParameters;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;

/**
 * Describes the settings for a simulation.
 */
public class SimulationParameters {
    /**
     * Controls how much departure times can randomly vary from schedule.
     * Higher values = less predictable departures.
     * Units: minutes
     */
    private long rngSeed;

    private long duration;
    private long inflow;
    private long outflow;

    private double departureDelayStdDev;

    private double minRemainingFuel;
    private double maxRemainingFuel;

    private double mechanicalFailureProbability;
    private double passengerHealthProbability;

    private long maxDelay;
    private long lowFuelThreshold;

    private double healthEscalationTimeMean;

    // Runway closure duration parameters (mean time in minutes)
    private double runwayInspectionDurationMean;
    private double runwaySnowClearanceDurationMean;
    private double runwayEquipmentFailureDurationMean;

    // Runway closure probability parameters (probability per unit time)
    private double runwayInspectionProbability;
    private double runwaySnowClearanceProbability;
    private double runwayEquipmentFailureProbability;

    private List<RunwayParameters> runwayParameters;

    public SimulationParameters() {
        this(0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, new ArrayList<>());
    }

    public SimulationParameters(
            long rngSeed,
            long duration,
            long inflow,
            long outflow,
            double departureDelayStdDev,
            double minRemainingFuel,
            double maxRemainingFuel,
            double mechanicalFailureProbability,
            double passengerHealthProbability,
            long maxDelay,
            long lowFuelThreshold,
            double healthEscalationTimeMean,
            double runwayInspectionDurationMean,
            double runwaySnowClearanceDurationMean,
            double runwayEquipmentFailureDurationMean,
            double runwayInspectionProbability,
            double runwaySnowClearanceProbability,
            double runwayEquipmentFailureProbability,
            List<RunwayParameters> runwayParameters) {
        this.rngSeed = rngSeed;
        this.duration = duration;
        this.inflow = inflow;
        this.outflow = outflow;
        this.departureDelayStdDev = departureDelayStdDev;
        this.minRemainingFuel = minRemainingFuel;
        this.maxRemainingFuel = maxRemainingFuel;
        this.mechanicalFailureProbability = mechanicalFailureProbability;
        this.passengerHealthProbability = passengerHealthProbability;
        this.maxDelay = maxDelay;
        this.lowFuelThreshold = lowFuelThreshold;
        this.healthEscalationTimeMean = healthEscalationTimeMean;
        this.runwayInspectionDurationMean = runwayInspectionDurationMean;
        this.runwaySnowClearanceDurationMean = runwaySnowClearanceDurationMean;
        this.runwayEquipmentFailureDurationMean = runwayEquipmentFailureDurationMean;
        this.runwayInspectionProbability = runwayInspectionProbability;
        this.runwaySnowClearanceProbability = runwaySnowClearanceProbability;
        this.runwayEquipmentFailureProbability = runwayEquipmentFailureProbability;
        this.runwayParameters = runwayParameters;
    }

    public long getRngSeed() {
        return rngSeed;
    }

    public void setRngSeed(long rngSeed) {
        this.rngSeed = rngSeed;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getInflow() {
        return inflow;
    }

    public void setInflow(long inflow) {
        this.inflow = inflow;
    }

    public long getOutflow() {
        return outflow;
    }

    public void setOutflow(long outflow) {
        this.outflow = outflow;
    }

    public double getDelayStdDev() {
        return departureDelayStdDev;
    }

    public void setDelayStdDev(double departureDelayStdDev) {
        this.departureDelayStdDev = departureDelayStdDev;
    }

    public double getMinRemainingFuel() {
        return minRemainingFuel;
    }

    public void setMinRemainingFuel(double minRemainingFuel) {
        this.minRemainingFuel = minRemainingFuel;
    }

    public double getMaxRemainingFuel() {
        return maxRemainingFuel;
    }

    public void setMaxRemainingFuel(double maxRemainingFuel) {
        this.maxRemainingFuel = maxRemainingFuel;
    }

    public double getMechanicalFailureProbability() {
        return mechanicalFailureProbability;
    }

    public void setMechanicalFailureProbability(double mechanicalFailureProbability) {
        this.mechanicalFailureProbability = mechanicalFailureProbability;
    }

    public double getPassengerHealthProbability() {
        return passengerHealthProbability;
    }

    public void setPassengerHealthProbability(double passengerHealthProbability) {
        this.passengerHealthProbability = passengerHealthProbability;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
    }

    public long getLowFuelThreshold() {
        return lowFuelThreshold;
    }

    public void setLowFuelThreshold(long lowFuelThreshold) {
        this.lowFuelThreshold = lowFuelThreshold;
    }

    public double getHealthEscalationTimeMean() {
        return healthEscalationTimeMean;
    }

    public void setHealthEscalationTimeMean(double healthEscalationTimeMean) {
        this.healthEscalationTimeMean = healthEscalationTimeMean;
    }

    public double getRunwayInspectionDurationMean() {
        return runwayInspectionDurationMean;
    }

    public void setRunwayInspectionDurationMean(double runwayInspectionDurationMean) {
        this.runwayInspectionDurationMean = runwayInspectionDurationMean;
    }

    public double getRunwaySnowClearanceDurationMean() {
        return runwaySnowClearanceDurationMean;
    }

    public void setRunwaySnowClearanceDurationMean(double runwaySnowClearanceDurationMean) {
        this.runwaySnowClearanceDurationMean = runwaySnowClearanceDurationMean;
    }

    public double getRunwayEquipmentFailureDurationMean() {
        return runwayEquipmentFailureDurationMean;
    }

    public void setRunwayEquipmentFailureDurationMean(double runwayEquipmentFailureDurationMean) {
        this.runwayEquipmentFailureDurationMean = runwayEquipmentFailureDurationMean;
    }

    public double getRunwayInspectionProbability() {
        return runwayInspectionProbability;
    }

    public void setRunwayInspectionProbability(double runwayInspectionProbability) {
        this.runwayInspectionProbability = runwayInspectionProbability;
    }

    public double getRunwaySnowClearanceProbability() {
        return runwaySnowClearanceProbability;
    }

    public void setRunwaySnowClearanceProbability(double runwaySnowClearanceProbability) {
        this.runwaySnowClearanceProbability = runwaySnowClearanceProbability;
    }

    public double getRunwayEquipmentFailureProbability() {
        return runwayEquipmentFailureProbability;
    }

    public void setRunwayEquipmentFailureProbability(double runwayEquipmentFailureProbability) {
        this.runwayEquipmentFailureProbability = runwayEquipmentFailureProbability;
    }

    public List<RunwayParameters> getRunwayParameters() {
        return runwayParameters;
    }

    public void setRunwayParameters(List<RunwayParameters> runwayParameters) {
        this.runwayParameters = runwayParameters;
    }

    public ValidationResult validate() {
        ArrayList<String> errorMessages = new ArrayList<>();
        ArrayList<Integer> textFieldIndex = new ArrayList<>();
        if (duration < 1 || duration > 10000){
            errorMessages.add("Invalid duration: Value must be between 1 and 10000");
            textFieldIndex.add(0);
        }
        if (inflow < 1 || inflow > 60){
            errorMessages.add("Invalid Inbound flow: Value must be between 1 and 60");
            textFieldIndex.add(1);
        }
        if (outflow < 1 || outflow > 60) {
            errorMessages.add("Invalid Outbound flow: Value must be between 1 and 60");
            textFieldIndex.add(2);
        }
        if (departureDelayStdDev < 1 || departureDelayStdDev > 10) {
            errorMessages.add("Invalid Delay standard deviation: Value must be between 1 and 10");
            textFieldIndex.add(3);
        }
        if (minRemainingFuel < lowFuelThreshold || minRemainingFuel < 10){
            errorMessages.add("Invalid min fuel: Value must be greater than 10 and the low fuel threshold");
            textFieldIndex.add(4);
        }
        if (maxRemainingFuel < minRemainingFuel || maxRemainingFuel > 90) {
            errorMessages.add("Invalid max fuel: Value must be greater than the min remaining fuel and 90");
            textFieldIndex.add(5);
        }
        if (mechanicalFailureProbability < 0 || mechanicalFailureProbability >1 ) {
            errorMessages.add("Invalid mechanical failure probability: Value must be between 0 and 100");
            textFieldIndex.add(6);
        }
        if (passengerHealthProbability < 0 || passengerHealthProbability > 1) {
            errorMessages.add("Invalid passenger health probability: Value must be between 0 and 100");
            textFieldIndex.add(7);
        }
        if (maxDelay < 0 || maxDelay > 60) {
            errorMessages.add("Invalid max delay: Value must be between 0 and 60");
            textFieldIndex.add(8);
        }
        if (lowFuelThreshold < 0 || lowFuelThreshold > 60) {
            errorMessages.add("Invalid low threshold: Value must be between 0 and 60");
            textFieldIndex.add(9);
        }
        if (healthEscalationTimeMean < 1 || healthEscalationTimeMean > maxRemainingFuel) {
            errorMessages.add("Invalid health escalation mean time: Value must be between 1 and max remaining fuel");
            textFieldIndex.add(10);
        }
        if (runwayInspectionDurationMean < 1 || runwayInspectionDurationMean > 30) {
            errorMessages.add("Invalid runway inspection duration mean: Value must be between 1 and 30");
            textFieldIndex.add(11);
        }
        if (runwaySnowClearanceDurationMean < 1 || runwaySnowClearanceDurationMean > 30) {
            errorMessages.add("Invalid snow clearance duration mean: Value must be between 1 and 30");
            textFieldIndex.add(12);
        }
        if (runwayEquipmentFailureDurationMean < 1 || runwayEquipmentFailureDurationMean > 30) {
            errorMessages.add("Invalid equipment failure duration mean: Value must be between 1 and 30");
            textFieldIndex.add(13);
        }
        if (runwayInspectionProbability < 0 || runwayInspectionProbability > 1) {
            errorMessages.add("Invalid runway inspection probability: Value must be between 0 and 100");
            textFieldIndex.add(14);
        }
        if (runwaySnowClearanceProbability < 0 || runwaySnowClearanceProbability > 1) {
            errorMessages.add("Invalid snow clearance probaility: Value must be between 0 and 100");
            textFieldIndex.add(15);
        }
        if (runwayEquipmentFailureProbability < 0 || runwayEquipmentFailureProbability > 1) {
            errorMessages.add("Invalid runway equipment probability failure: Value must be between 0 and 100");
            textFieldIndex.add(16);
        }

        // TODO: Make sure runways are checked properly to ensure proper available runway for takeoff and landing exist.
        if (runwayParameters.isEmpty())
        {
            errorMessages.add("There must be atleast one runway available for either take-off or landing.");
        }

        for (RunwayParameters param : runwayParameters)
        {
            if (param.getLength() < 500 || param.getLength() > 4000) 
                errorMessages.add("Invalid runway length: Value must be between 500 and 4000");
            if (param.getStatus() == RunwayStatus.OCCUPIED) 
                errorMessages.add("Invalid runway status: Cannot start as occupied");
        }

        return new ValidationResult(errorMessages, textFieldIndex);
    }
}
