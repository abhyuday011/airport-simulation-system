package uk.ac.warwick.cs261.simulation;

import java.util.Map;

import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;

/**
 * Class that holds all data about the simulation upon finishing.
 * Provides ease-of-use methods to access various collected data.
 */
public class FinalSimulationStats {

    //TAKEOFF
    private double averageTakeoffDelay;
    private long maxTakeoffDelay;
    private Map<String, Double> takeoffDelayDistribution;

    private double averageTakeoffWait;
    private long maxTakeoffWait;
    private Map<String, Double> takeoffWaitDistribution;

    private int maxTakeoffQueueSize;
    private double[] takeoffQueueSizePerHour;
    private int totalTakeoffAircrafts;

    private int[] takeoffsPerHour;


    //LANDING
    private double averageLandingDelay;
    private long maxLandingDelay;
    private Map<String, Double> landingDelayDistribution;

    private double averageLandingWait;
    private long maxLandingWait;
    private Map<String, Double> landingWaitDistribution;

    private int maxHoldingQueueSize;
    private double[] holdingQueueSizePerHour;
    private int totalLandingAircrafts;

    private int[] landingsPerHour;

    //RUNWAY
    private Map<RunwayMode, Integer> runwayModesInitial;
    private Map<RunwayMode, Integer> runwayModesFinal;

    private Map<RunwayStatus, Integer> runwayClosureCounts;
    private Map<RunwayStatus, Long> runwayClosureTimeRatio;
    private int totalRunwayClosures;

    private double[] averageFreeRunwaysPerHour;
    private double[] averageLandingRunwayOperationalTimePerHour;
    private double[] averageTakeoffRunwayOperationalTimePerHour;
    private double[] averageMixedRunwayOperationalTimePerHour;

    //EMERGENCIES
    private int totalLowFuelEmergencies;
    private int totalHealthEmergencies;
    private int totalMechanicalEmergencies;
    private int totalEmergencies;
    private Map<String, Integer> aircraftEmergencies_Map;

    private int[] lowFuelPerHour;
    private int[] healthEmergencyPerHour;
    private int[] mechanicalEmergencyPerHour;

    //CANCELLATIONS
    private int totalCancellations;
    private int[] cancellationsPerHour;
    private double cancellationRate;

    //DIVERSIONS
    private int totalDiversions;
    private int[] diversionsPerHour;
    private double diversionRate;


    //STABILITY SCORE
    private double stabilityScore;
    private int totalValidAircrafts;

    public FinalSimulationStats() {
    }

    public FinalSimulationStats(SimulationStatistics stats) {
        if (stats == null) {
            return;
        }

        //TAKEOFF
        setAverageTakeoffDelay(stats.getAverageTakeOffDelay());
        setMaxTakeoffDelay(stats.getMaxTakeOffDelay());
        setTakeoffDelayDistribution(stats.getModellingStats_TakeOffDelay());

        setAverageTakeoffWait(stats.getAverageTakeOffWaitTime());
        setMaxTakeoffWait(stats.getMaxTakeOffWaitTime());
        setTakeoffWaitDistribution(stats.getModellingStats_TakeOffWait());

        setMaxTakeoffQueueSize(stats.getMaxTakeOffSize());
        setTakeoffQueueSizePerHour(stats.getAverageTakeoffQueueSizePerHour());
        setTakeoffsPerHour(stats.getTakeoffsPerHour());

        //LANDING
        setAverageLandingDelay(stats.getAverageArrivalDelay());
        setMaxLandingDelay(stats.getMaxArrivalDelay());
        setLandingDelayDistribution(stats.getModellingStats_ArrivalDelay());

        setAverageLandingWait(stats.getAverageArrivalWaitTime());
        setMaxLandingWait(stats.getMaxArrivalWaitTime());
        setLandingWaitDistribution(stats.getModellingStats_ArrivalWait());

        setMaxHoldingQueueSize(stats.getMaxHoldingSize());
        setHoldingQueueSizePerHour(stats.getAverageHoldingQueueSizePerHour());
        setLandingsPerHour(stats.getLandingsPerHour());

        //RUNWAY
        setRunwayModesInitial(stats.getRunwayOpTypeCount_Map_Initial());
        setRunwayModesFinal(stats.getRunwayOpTypeCount_Map_Final());
        setRunwayClosureCounts(stats.getRunwayClosureEventTypeCount_Map());
        setRunwayClosureTimeRatio(stats.getRunwayClosureTime_Map());
        setTotalRunwayClosures(stats.getTotalRunwayClosures());

        setAverageFreeRunwaysPerHour(stats.getAverageFreeRunwaysPerHour());
        setAverageLandingRunwayOperationalTimePerHour(stats.getAverageLandingRunwayCapacityPerHour());
        setAverageTakeoffRunwayOperationalTimePerHour(stats.getAverageTakeoffRunwayCapacityPerHour());
        setAverageMixedRunwayOperationalTimePerHour(stats.getAverageMixedRunwayCapacityPerHour());

        //EMERGENCIES
        setTotalLowFuelEmergencies(stats.getTotalLowFuelEmergencies());
        setTotalHealthEmergencies(stats.getTotalHealthEmergencies());
        setTotalMechanicalEmergencies(stats.getTotalMechanicalEmergencies());
        setTotalEmergencies(stats.getTotalEmergencies());
        setAircraftEmergencies_Map(stats.getAircraftEmergencies_Map());

        setLowFuelPerHour(stats.getLowFuelPerHour());
        setHealthEmergencyPerHour(stats.getHealthEmergencyPerHour());
        setMechanicalEmergencyPerHour(stats.getMechanicalEmergencyPerHour());

        //CANCELLATIONS
        setTotalCancellations(stats.getCancelled());
        setCancellationsPerHour(stats.getCancellationsPerHour());
        setCancellationRate(stats.getCancellationRate());

        //DIVERSIONS
        setTotalDiversions(stats.getDiverted());
        setDiversionsPerHour(stats.getDiversionsPerHour());
        setDiversionRate(stats.getDiversionRate());

        //STABILITY SCORE
        setStabilityScore(stats.getStabilityScore());

        //Additional
        setTotalTakeoffAircrafts(stats.getTotalTakeOffAircrafts());
        setTotalLandingAircrafts(stats.getTotalArrivalAircrafts());
        setTotalValidAircrafts(stats.getTotalValidAircrafts());


    }

    //STABILITY SCORE
    public void setStabilityScore(double stabilityScore){
        this.stabilityScore = stabilityScore;
    }

    public double getStabilityScore(){
        return stabilityScore;}

    //TAKEOFF

    public void setAverageTakeoffDelay(double averageTakeoffDelay) {
        this.averageTakeoffDelay = averageTakeoffDelay; //To get it in minutes
    }

    public double getAverageTakeoffDelay() {
        return averageTakeoffDelay;
    }

    public void setMaxTakeoffDelay(long maxTakeoffDelay) {
        this.maxTakeoffDelay = maxTakeoffDelay;
    }

    public long getMaxTakeoffDelay() {
        return maxTakeoffDelay;
    }

    public void setTakeoffDelayDistribution(Map<String, Double> takeoffDelayDistribution) {
        this.takeoffDelayDistribution = takeoffDelayDistribution;
    }

    public Map<String, Double> getTakeoffDelayDistribution() {
        return takeoffDelayDistribution;
    }

    public void setAverageTakeoffWait(double averageTakeoffWait) {
        this.averageTakeoffWait = averageTakeoffWait;
    }

    public double getAverageTakeoffWait() {
        return averageTakeoffWait;
    }

    public void setMaxTakeoffWait(long maxTakeoffWait) {
        this.maxTakeoffWait = maxTakeoffWait;
    }

    public long getMaxTakeoffWait() {
        return maxTakeoffWait;
    }

    public void setTakeoffWaitDistribution(Map<String, Double> takeoffWaitDistribution) {
        this.takeoffWaitDistribution = takeoffWaitDistribution;
    }

    public Map<String, Double> getTakeoffWaitDistribution() {
        return takeoffWaitDistribution;
    }

    public void setMaxTakeoffQueueSize(int maxTakeoffQueueSize) {
        this.maxTakeoffQueueSize = maxTakeoffQueueSize;
    }

    public int getMaxTakeoffQueueSize() {
        return maxTakeoffQueueSize;
    }

    public void setTakeoffQueueSizePerHour(double[] takeoffQueueSizePerHour) {
        this.takeoffQueueSizePerHour = takeoffQueueSizePerHour;
    }

    public double[] getTakeoffQueueSizePerHour() {
        return takeoffQueueSizePerHour;
    }

    public void setTakeoffsPerHour(int[] takeoffsPerHour) {
        this.takeoffsPerHour = takeoffsPerHour;
    }

    public int[] getTakeoffsPerHour() {
        return takeoffsPerHour;
    }

    //LANDING

    public void setAverageLandingDelay(double averageLandingDelay) {
        this.averageLandingDelay = averageLandingDelay;
    }

    public double getAverageLandingDelay() {
        return averageLandingDelay;
    }

    public void setMaxLandingDelay(long maxLandingDelay) {
        this.maxLandingDelay = maxLandingDelay;
    }

    public long getMaxLandingDelay() {
        return maxLandingDelay;
    }

    public void setLandingDelayDistribution(Map<String, Double> landingDelayDistribution) {
        this.landingDelayDistribution = landingDelayDistribution;
    }

    public Map<String, Double> getLandingDelayDistribution() {
        return landingDelayDistribution;
    }

    public void setAverageLandingWait(double averageLandingWait) {
        this.averageLandingWait = averageLandingWait;
    }

    public double getAverageLandingWait() {
        return averageLandingWait;
    }

    public void setMaxLandingWait(long maxLandingWait) {
        this.maxLandingWait = maxLandingWait;
    }

    public long getMaxLandingWait() {
        return maxLandingWait;
    }

    public void setLandingWaitDistribution(Map<String, Double> landingWaitDistribution) {
        this.landingWaitDistribution = landingWaitDistribution;
    }

    public Map<String, Double> getLandingWaitDistribution() {
        return landingWaitDistribution;
    }

    public void setMaxHoldingQueueSize(int maxHoldingQueueSize) {
        this.maxHoldingQueueSize = maxHoldingQueueSize;
    }

    public int getMaxHoldingQueueSize() {
        return maxHoldingQueueSize;
    }

    public void setHoldingQueueSizePerHour(double[] holdingQueueSizePerHour) {
        this.holdingQueueSizePerHour = holdingQueueSizePerHour;
    }

    public double[] getHoldingQueueSizePerHour() {
        return holdingQueueSizePerHour;
    }

    public void setLandingsPerHour(int[] landingsPerHour) {
        this.landingsPerHour = landingsPerHour;
    }

    public int[] getLandingsPerHour() {
        return landingsPerHour;
    }

    //RUNWAY

    public void setRunwayModesInitial(Map<RunwayMode, Integer> runwayModesInitial) {
        this.runwayModesInitial = runwayModesInitial;
    }

    public Map<RunwayMode, Integer> getRunwayModesInitial() {
        return runwayModesInitial;
    }

    public void setRunwayModesFinal(Map<RunwayMode, Integer> runwayModesFinal) {
        this.runwayModesFinal = runwayModesFinal;
    }

    public Map<RunwayMode, Integer> getRunwayModesFinal() {
        return runwayModesFinal;
    }

    public void setRunwayClosureCounts(Map<RunwayStatus, Integer> runwayClosureCounts) {
        this.runwayClosureCounts = runwayClosureCounts; //Bug - this just passes reference not copy of actual map?
    }

    public Map<RunwayStatus, Integer> getRunwayClosureCounts() {
        return runwayClosureCounts;
    }

    public void setRunwayClosureTimeRatio(Map<RunwayStatus, Long> runwayClosureTimeRatio){
        this.runwayClosureTimeRatio = runwayClosureTimeRatio;
    }

    public Map<RunwayStatus, Long> getRunwayClosureTimeRatio() {
        return runwayClosureTimeRatio;
    }

    public void setTotalRunwayClosures(int totalRunwayClosures) {
        this.totalRunwayClosures = totalRunwayClosures;
    }

    public int getTotalRunwayClosures() {
        return totalRunwayClosures;
    }

    public void setAverageFreeRunwaysPerHour(double[] averageFreeRunwaysPerHour) {
        this.averageFreeRunwaysPerHour = averageFreeRunwaysPerHour;
    }

    public double[] getAverageFreeRunwaysPerHour() {
        return averageFreeRunwaysPerHour;
    }

    public void setAverageLandingRunwayOperationalTimePerHour(double[] averageLandingRunwayOperationalTimePerHour) {
        this.averageLandingRunwayOperationalTimePerHour = averageLandingRunwayOperationalTimePerHour;
    }

    public double[] getAverageLandingRunwayOperationalTimePerHour() {
        return averageLandingRunwayOperationalTimePerHour;
    }

    public void setAverageTakeoffRunwayOperationalTimePerHour(double[] averageTakeoffRunwayOperationalTimePerHour) {
        this.averageTakeoffRunwayOperationalTimePerHour = averageTakeoffRunwayOperationalTimePerHour;
    }

    public double[] getAverageTakeoffRunwayOperationalTimePerHour() {
        return averageTakeoffRunwayOperationalTimePerHour;
    }

    public void setAverageMixedRunwayOperationalTimePerHour(double[] averageMixedRunwayOperationalTimePerHour) {
        this.averageMixedRunwayOperationalTimePerHour = averageMixedRunwayOperationalTimePerHour;
    }

    public double[] getAverageMixedRunwayOperationalTimePerHour() {
        return averageMixedRunwayOperationalTimePerHour;
    }

    //EMERGENCIES 

    public void setTotalLowFuelEmergencies(int totalLowFuelEmergencies) {
        this.totalLowFuelEmergencies = totalLowFuelEmergencies;
    }

    public int getTotalLowFuelEmergencies() {
        return totalLowFuelEmergencies;
    }

    public void setTotalHealthEmergencies(int totalHealthEmergencies) {
        this.totalHealthEmergencies = totalHealthEmergencies;
    }

    public int getTotalHealthEmergencies() {
        return totalHealthEmergencies;
    }

    public void setTotalMechanicalEmergencies(int totalMechanicalEmergencies) {
        this.totalMechanicalEmergencies = totalMechanicalEmergencies;
    }

    public int getTotalMechanicalEmergencies() {
        return totalMechanicalEmergencies;
    }

    public void setTotalEmergencies(int totalEmergencies) {
        this.totalEmergencies = totalEmergencies;
    }

    public int getTotalEmergencies() {
        return totalEmergencies;
    }

    public void setLowFuelPerHour(int[] lowFuelPerHour) {
        this.lowFuelPerHour = lowFuelPerHour;
    }

    public int[] getLowFuelPerHour() {
        return lowFuelPerHour;
    }

    public void setHealthEmergencyPerHour(int[] healthEmergencyPerHour) {
        this.healthEmergencyPerHour = healthEmergencyPerHour;
    }

    public int[] getHealthEmergencyPerHour() {
        return healthEmergencyPerHour;
    }

    public void setMechanicalEmergencyPerHour(int[] mechanicalEmergencyPerHour) {
        this.mechanicalEmergencyPerHour = mechanicalEmergencyPerHour;
    }

    public int[] getMechanicalEmergencyPerHour() {
        return mechanicalEmergencyPerHour;
    }

    public void setAircraftEmergencies_Map(Map<String, Integer> aircraftEmergencies_Map) {
        this.aircraftEmergencies_Map = aircraftEmergencies_Map;
    }

    public Map<String, Integer> getAircraftEmergencies_Map() {
        return aircraftEmergencies_Map;
    }

    //CANCELLATIONS

    public void setTotalCancellations(int totalCancellations) {
        this.totalCancellations = totalCancellations;
    }

    public int getTotalCancellations() {
        return totalCancellations;
    }

    public void setCancellationsPerHour(int[] cancellationsPerHour) {
        this.cancellationsPerHour = cancellationsPerHour;
    }

    public int[] getCancellationsPerHour() {
        return cancellationsPerHour;
    }

    public void setCancellationRate(double cancellationRate) {
        this.cancellationRate = cancellationRate;
    }

    public double getCancellationRate() {
        return cancellationRate;
    }

    //DIVERSIONS

    public void setTotalDiversions(int totalDiversions) {
        this.totalDiversions = totalDiversions;
    }

    public int getTotalDiversions() {
        return totalDiversions;
    }

    public void setDiversionsPerHour(int[] diversionsPerHour) {
        this.diversionsPerHour = diversionsPerHour;
    }

    public int[] getDiversionsPerHour() {
        return diversionsPerHour;
    }

    public void setDiversionRate(double diversionRate) {
        this.diversionRate = diversionRate;
    }

    public double getDiversionRate() {
        return diversionRate;
    }


    //TOTAL AIRCRAFT COUNTS

    public void setTotalTakeoffAircrafts(int totalTakeoffAircrafts) {
        this.totalTakeoffAircrafts = totalTakeoffAircrafts;
    }

    public int getTotalTakeoffAircrafts() {
        return totalTakeoffAircrafts;
    }

    public void setTotalLandingAircrafts(int totalLandingAircrafts) {
        this.totalLandingAircrafts = totalLandingAircrafts;
    }

    public int getTotalLandingAircrafts() {
        return totalLandingAircrafts;
    }

    public void setTotalValidAircrafts(int totalValidAircrafts) {
        this.totalValidAircrafts = totalValidAircrafts;
    }

    public int getTotalValidAircrafts() {
        return totalValidAircrafts;
    }
}
