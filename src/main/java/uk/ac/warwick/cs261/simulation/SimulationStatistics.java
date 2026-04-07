package uk.ac.warwick.cs261.simulation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import uk.ac.warwick.cs261.simulation.entities.aircraft.Aircraft;
import uk.ac.warwick.cs261.simulation.entities.aircraft.AircraftStatus;
import uk.ac.warwick.cs261.simulation.entities.runway.Runway;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayMode;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;

/**
 * Holds all collected data about the simulation. This is passed into events.
 * This is used for later analysis.
 */
public class SimulationStatistics 
{
    //Aircraft stats:
    private int totalValidAircrafts;

    private long totalArrivalDelay;
    private long totalTakeOffDelay;

    private long totalArrivalWaiTime;
    private long totalTakeOffWaiTime;

    private long maxArrivalDelay;
    private long maxTakeOffDelay;

    private long maxArrivalWaiTime;
    private long maxTakeOffWaiTime;

    private int totalArrivalAircrafts;
    private int totalTakeOffAircrafts;

    private int cancelled;
    private int diverted;

    //Queue stats
    private int maxHoldingSize;
    private int avgHoldingSize;

    private int maxTakeoffSize;
    private int avgTakeoffSize;

    //Time Stats
    private int durationHours;

    private int[] takeoffsPerHour;
    private int[] landingsPerHour;
    private int[] cancellationsPerHour;
    private int[] diversionsPerHour;

    private int[] lowFuelPerHour;
    private int[] healthEmergencyPerHour;
    private int[] mechanicalEmergencyPerHour;

    private Map<String, Integer> aircraftEmergencies_Map;

    private long[] holdingQueueSizeTimeSum;
    private long[] takeoffQueueSizeTimeSum;
    private long[] secondsTrackedPerHour;

    private long[] freeRunwayTimeSum;
    private long[] landingRunwayCapacityTimeSum;
    private long[] takeoffRunwayCapacityTimeSum;
    private long[] mixedRunwayCapacityTimeSum;

    //Runway count maps
    private Map<RunwayMode, Integer> runwayOpType_Count_Initial;
    private Map<RunwayMode, Integer> runwayOpType_Count_Final;

    private Map<RunwayStatus, Integer> runwayClosureEventType_Count;
    private Map<RunwayStatus, Long> closureTimeByType;

    //Additional Modelling stats
    private DescriptiveStatistics modellingStats_TakeOffDelay = new DescriptiveStatistics();
    private DescriptiveStatistics modellingStats_ArrivalDelay = new DescriptiveStatistics();

    private DescriptiveStatistics modellingStats_TakeOffWait = new DescriptiveStatistics();
    private DescriptiveStatistics modellingStats_ArrivalWait = new DescriptiveStatistics();


    public void init(long durationSeconds) {
        this.durationHours = (int) Math.ceil(
            (double) durationSeconds / SimulationConstants.HOURS_TO_SECONDS
        );

        takeoffsPerHour = new int[durationHours];
        landingsPerHour = new int[durationHours];
        cancellationsPerHour = new int[durationHours];
        diversionsPerHour = new int[durationHours];

        lowFuelPerHour = new int[durationHours];
        healthEmergencyPerHour = new int[durationHours];
        mechanicalEmergencyPerHour = new int[durationHours];

        holdingQueueSizeTimeSum = new long[durationHours];
        takeoffQueueSizeTimeSum = new long[durationHours];
        secondsTrackedPerHour = new long[durationHours];

        freeRunwayTimeSum = new long[durationHours];
        landingRunwayCapacityTimeSum = new long[durationHours];
        takeoffRunwayCapacityTimeSum = new long[durationHours];
        mixedRunwayCapacityTimeSum = new long[durationHours];

        runwayOpType_Count_Initial = new EnumMap<>(RunwayMode.class);
        runwayOpType_Count_Final = new EnumMap<>(RunwayMode.class);
        runwayClosureEventType_Count = new EnumMap<>(RunwayStatus.class);

        runwayClosureEventType_Count.put(RunwayStatus.INSPECTION, 0);
        runwayClosureEventType_Count.put(RunwayStatus.SNOW_CLEARANCE, 0);
        runwayClosureEventType_Count.put(RunwayStatus.EQUIPMENT_FAILURE, 0);

        closureTimeByType = new HashMap<>();
        closureTimeByType.put(RunwayStatus.INSPECTION, 0L);
        closureTimeByType.put(RunwayStatus.SNOW_CLEARANCE, 0L);
        closureTimeByType.put(RunwayStatus.EQUIPMENT_FAILURE, 0L);

        aircraftEmergencies_Map = new HashMap<>();

    }

//Methods:

    public void onTimeAdvanced(long previousTime, long newTime, SimulationState state) {
        if (state == null || newTime <= previousTime) return;

        long t = previousTime;

        while (t < newTime) {

            int hourIndex = (int)(t / SimulationConstants.HOURS_TO_SECONDS);
            if (hourIndex < 0 || hourIndex >= durationHours) break;

            long hourEnd = (hourIndex + 1L) * SimulationConstants.HOURS_TO_SECONDS;
            long segmentEnd = Math.min(newTime, hourEnd);
            long deltaTime = segmentEnd - t;

            int holdingSize = state.getHoldingQueue().size();
            int takeoffSize = state.getTakeoffQueue().size();

            holdingQueueSizeTimeSum[hourIndex] += (long) holdingSize * deltaTime;
            takeoffQueueSizeTimeSum[hourIndex] += (long) takeoffSize * deltaTime;

          
            int freeRunways = 0;
            int landingCap = 0;
            int takeoffCap = 0;
            int mixedCap = 0;

            for (var runway : state.getRunways()) {

                if (runway.getStatus() == RunwayStatus.FREE)
                    freeRunways++;

                boolean operational =
                    runway.getStatus() == RunwayStatus.FREE ||
                    runway.getStatus() == RunwayStatus.OCCUPIED;

                if (operational) {
                    switch (runway.getMode()) {
                        case LANDING -> landingCap++;
                        case TAKEOFF -> takeoffCap++;
                        case MIXED -> mixedCap++;
                    }
                }
            }

            freeRunwayTimeSum[hourIndex] += (long) freeRunways * deltaTime;
            landingRunwayCapacityTimeSum[hourIndex] += (long) landingCap * deltaTime;
            takeoffRunwayCapacityTimeSum[hourIndex] += (long) takeoffCap * deltaTime;
            mixedRunwayCapacityTimeSum[hourIndex] += (long) mixedCap * deltaTime;

            secondsTrackedPerHour[hourIndex] += deltaTime;

            t = segmentEnd;
        }
    }

    public void recordEmergency(AircraftStatus status, long time) {
        int hour = (int) (time / SimulationConstants.HOURS_TO_SECONDS);
        if (hour < 0 || hour >= durationHours) return;

        switch (status) {
            case LOW_FUEL -> lowFuelPerHour[hour]++;
            case PASSENGER_HEALTH, PASSENGER_HEALTH_SEVERE -> healthEmergencyPerHour[hour]++;
            case MECHANICAL_FAILURE -> mechanicalEmergencyPerHour[hour]++;
            default -> {}
        }
    }

    public void recordTakeoffOrLanding(Aircraft aircraft, long time) {
        int hour = (int) (time / SimulationConstants.HOURS_TO_SECONDS);
        if (hour < 0 || hour >= durationHours) return;

        if (aircraft.getIsTakeoff()) takeoffsPerHour[hour]++;
        else landingsPerHour[hour]++;
    }

    public void recordCancellation(long time) {
        int hour = (int) (time / SimulationConstants.HOURS_TO_SECONDS);
        if (hour >= 0 && hour < durationHours) cancellationsPerHour[hour]++;
        incrementCancelledAircrafts();
    }

    public void recordDiversion(long time) {
        int hour = (int) (time / SimulationConstants.HOURS_TO_SECONDS);
        if (hour >= 0 && hour < durationHours) diversionsPerHour[hour]++;
        incrementDivertedAircrafts();
    }

    public void updateStats(Aircraft aircraft){

        if (aircraft == null) return;

        long delay = aircraft.calculateDelay();
        long wait = aircraft.calculateWaitTime();

        if(aircraft.getIsTakeoff()){
            totalTakeOffAircrafts++;
            totalTakeOffDelay += delay;
            totalTakeOffWaiTime += wait;

            //Adds to descriptive stats - modelling delay
            modellingStats_TakeOffDelay.addValue(delay);
            modellingStats_TakeOffWait.addValue(wait);

            if (delay > maxTakeOffDelay) maxTakeOffDelay = delay;
            if (wait > maxTakeOffWaiTime) maxTakeOffWaiTime = wait;
        }
        else { //Landing
            totalArrivalAircrafts++;
            totalArrivalDelay += delay;
            totalArrivalWaiTime += wait;

            //Adds to descriptive stats - modelling arrival
            modellingStats_ArrivalDelay.addValue(delay);
            modellingStats_ArrivalWait.addValue(wait);

            if (delay > maxArrivalDelay) maxArrivalDelay = delay;
            if (wait > maxArrivalWaiTime) maxArrivalWaiTime = wait;
        }
    }

    public void updateHoldingQueueStats(SimulationState state){
        if (state == null) return;
        int size = state.getHoldingQueue().size();
        
        if (size > maxHoldingSize) maxHoldingSize = size;
    }

    public void updateTakeOffQueueStats(SimulationState state){

        if (state == null) return;
        int size = state.getTakeoffQueue().size();
        if (size > maxTakeoffSize) maxTakeoffSize = size;
    }

    public void incrementDivertedAircrafts(){ this.diverted = this.diverted+1; }

    public void incrementCancelledAircrafts(){ this.cancelled = this.cancelled+1; }

    public void initRunwayClosureCount(List<Runway> runways){
        runwayClosureEventType_Count.put(RunwayStatus.INSPECTION, 0);
        runwayClosureEventType_Count.put(RunwayStatus.SNOW_CLEARANCE, 0);
        runwayClosureEventType_Count.put(RunwayStatus.EQUIPMENT_FAILURE, 0);

        for (Runway runway : runways){
            this.incrementRunwayClosureType(runway.getStatus()); }
    }

    public void incrementRunwayClosureType(RunwayStatus targetStatus){
        if (runwayClosureEventType_Count.containsKey(targetStatus)) { //automatically skips over non-closure statuses
            int currentCount = runwayClosureEventType_Count.get(targetStatus);
            runwayClosureEventType_Count.put(targetStatus, currentCount + 1);
        }
    }

    public void incrementClosureTime(RunwayStatus targetStatus, long closureDuration){
        if (closureTimeByType.containsKey(targetStatus)) { //automatically skips over non-closure statuses
            long currentCount = closureTimeByType.get(targetStatus);
            closureTimeByType.put(targetStatus, currentCount + closureDuration);
        }
    }


    public void record_RunwayOperationRatio(List<Runway> runways, boolean isInitial){
        Map<RunwayMode, Integer> currentMap = isInitial ? runwayOpType_Count_Initial : runwayOpType_Count_Final;

        for (RunwayMode mode : RunwayMode.values()) { // Reset all counts to 0
            currentMap.put(mode, 0);}

        for (Runway runway : runways) { //Counting each runway
            RunwayMode mode = runway.getMode();
            currentMap.put(mode, currentMap.get(mode) + 1);
        }
    }

    public int getTotalLowFuelEmergencies() {
        int total = 0;
        for (int count : lowFuelPerHour) {
            total += count;
        }
        return total;
    }

    public int getTotalHealthEmergencies() {
        int total = 0;
        for (int count : healthEmergencyPerHour) {
            total += count;
        }
        return total;
    } 

    public int getTotalMechanicalEmergencies() {
        int total = 0;
        for (int count : mechanicalEmergencyPerHour) {
            total += count;
        }
        return total;
    }

    public Map<String, Integer> getAircraftEmergencies_Map(){
        aircraftEmergencies_Map.put("LOW_FUEL", this.getTotalLowFuelEmergencies());
        aircraftEmergencies_Map.put("MECHANICAL_FAILURE", this.getTotalMechanicalEmergencies());
        aircraftEmergencies_Map.put("PASSENGER_HEALTH", this.getTotalHealthEmergencies());

        return aircraftEmergencies_Map;
    }

    public int getTotalEmergencies() {
        return getTotalLowFuelEmergencies()
            + getTotalHealthEmergencies()
            + getTotalMechanicalEmergencies();
    }  

    //Getter methods for the Maps - Made umodifiable to prevent unwanted changes
    public Map<RunwayMode, Integer> getRunwayOpTypeCount_Map_Initial() {
        return Collections.unmodifiableMap(runwayOpType_Count_Initial);
    }

    public Map<RunwayMode, Integer> getRunwayOpTypeCount_Map_Final() {
        return Collections.unmodifiableMap(runwayOpType_Count_Final);
    }

    public Map<RunwayStatus, Integer> getRunwayClosureEventTypeCount_Map() {
        return Collections.unmodifiableMap(runwayClosureEventType_Count);
    }

    public Map<RunwayStatus, Long> getRunwayClosureTime_Map(){ return closureTimeByType;}

    public double getAverageArrivalDelay() {
        return totalArrivalAircrafts == 0 ? 0 : ((double) totalArrivalDelay) / totalArrivalAircrafts;
    }

    public double getAverageTakeOffDelay() {
        return totalTakeOffAircrafts == 0 ? 0 : ((double) totalTakeOffDelay) / totalTakeOffAircrafts;
    }

    public double getAverageArrivalWaitTime() {
        return totalArrivalAircrafts == 0 ? 0 : ((double) totalArrivalWaiTime) / totalArrivalAircrafts;
    }

    public double getAverageTakeOffWaitTime() {
        return totalTakeOffAircrafts == 0 ? 0 : ((double) totalTakeOffWaiTime) / totalTakeOffAircrafts;
    }

    public long getMaxArrivalDelay() { return maxArrivalDelay; }
    public long getMaxTakeOffDelay() { return maxTakeOffDelay; }

    public long getMaxArrivalWaitTime() { return maxArrivalWaiTime; }
    public long getMaxTakeOffWaitTime() { return maxTakeOffWaiTime; }

    public int getMaxHoldingSize() { return maxHoldingSize; }
    public int getMaxTakeOffSize() { return maxTakeoffSize; }

    public int getCancelled() { return cancelled; }
    public int getDiverted() { return diverted; }

    public int getTotalArrivalAircrafts() {  return totalArrivalAircrafts; }
    public int getTotalTakeOffAircrafts() { return totalTakeOffAircrafts; }

    public int getTotalValidAircrafts () { return totalArrivalAircrafts + totalTakeOffAircrafts; }

    //Modelling stats are stored as 4 hashmaps, for each takeoff/arrival's delay/wait.
    //Derive values by doing mapName.get("attributeName"), e.g. stats.get("max")
    public Map<String, Double> getModellingStats_TakeOffDelay(){
        Map<String, Double> stats = new HashMap<>();
            stats.put("mean", modellingStats_TakeOffDelay.getMean());
            stats.put("p25", modellingStats_TakeOffDelay.getPercentile(25));
            stats.put("p50", modellingStats_TakeOffDelay.getPercentile(50));
            stats.put("p75", modellingStats_TakeOffDelay.getPercentile(75));
            stats.put("p90", modellingStats_TakeOffDelay.getPercentile(90));
            stats.put("min", modellingStats_TakeOffDelay.getMin());
            stats.put("max", modellingStats_TakeOffDelay.getMax());
            stats.put("range", (double) stats.get("max") - (double) stats.get("min"));
            stats.put("variance", modellingStats_TakeOffDelay.getVariance());
        return stats;
    }
    public Map<String, Double> getModellingStats_ArrivalDelay(){
        Map<String, Double> stats = new HashMap<>();

        double min = modellingStats_ArrivalDelay.getMin();
        double max = modellingStats_ArrivalDelay.getMax();

        stats.put("mean", modellingStats_ArrivalDelay.getMean());
        stats.put("p25", modellingStats_ArrivalDelay.getPercentile(25));
        stats.put("p50", modellingStats_ArrivalDelay.getPercentile(50));
        stats.put("p75", modellingStats_ArrivalDelay.getPercentile(75));
        stats.put("p90", modellingStats_ArrivalDelay.getPercentile(90));
        stats.put("min", min);
        stats.put("max", max);
        stats.put("range", max - min);
        stats.put("variance", modellingStats_ArrivalDelay.getVariance());

        return stats;
    }

    public Map<String, Double> getModellingStats_TakeOffWait(){
        Map<String, Double> stats = new HashMap<>();

        double min = modellingStats_TakeOffWait.getMin();
        double max = modellingStats_TakeOffWait.getMax();

        stats.put("mean", modellingStats_TakeOffWait.getMean());
        stats.put("p25", modellingStats_TakeOffWait.getPercentile(25));
        stats.put("p50", modellingStats_TakeOffWait.getPercentile(50));
        stats.put("p75", modellingStats_TakeOffWait.getPercentile(75));
        stats.put("p90", modellingStats_TakeOffWait.getPercentile(90));
        stats.put("min", min);
        stats.put("max", max);
        stats.put("range", max - min);
        stats.put("variance", modellingStats_TakeOffWait.getVariance());

        return stats;
    }

    public Map<String, Double> getModellingStats_ArrivalWait(){
        Map<String, Double> stats = new HashMap<>();

        double min = modellingStats_ArrivalWait.getMin();
        double max = modellingStats_ArrivalWait.getMax();

        stats.put("mean", modellingStats_ArrivalWait.getMean());
        stats.put("p25", modellingStats_ArrivalWait.getPercentile(25));
        stats.put("p50", modellingStats_ArrivalWait.getPercentile(50));
        stats.put("p75", modellingStats_ArrivalWait.getPercentile(75));
        stats.put("p90", modellingStats_ArrivalWait.getPercentile(90));
        stats.put("min", min);
        stats.put("max", max);
        stats.put("range", max - min);
        stats.put("variance", modellingStats_ArrivalWait.getVariance());

        return stats;
    }


    public double[] getAverageHoldingQueueSizePerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) holdingQueueSizeTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }

    public double[] getAverageTakeoffQueueSizePerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) takeoffQueueSizeTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }

    public double[] getAverageFreeRunwaysPerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) freeRunwayTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }

    public double[] getAverageLandingRunwayCapacityPerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) landingRunwayCapacityTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }

    public double[] getAverageTakeoffRunwayCapacityPerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) takeoffRunwayCapacityTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }

    public double[] getAverageMixedRunwayCapacityPerHour() {
        double[] averages = new double[durationHours];

        for (int i = 0; i < durationHours; i++) {
            if (secondsTrackedPerHour[i] == 0) {
                averages[i] = 0.0;
            } else {
                averages[i] = (double) mixedRunwayCapacityTimeSum[i] / secondsTrackedPerHour[i];
            }
        }

        return averages;
    }


    public int[] getTakeoffsPerHour() {
        return takeoffsPerHour.clone();
    }

    public int[] getLandingsPerHour() {
        return landingsPerHour.clone();
    }

    public int[] getCancellationsPerHour() {
        return cancellationsPerHour.clone();
    }

    public int[] getDiversionsPerHour() {
        return diversionsPerHour.clone();
    }

    public int[] getLowFuelPerHour() {
        return lowFuelPerHour.clone();
    }

    public int[] getHealthEmergencyPerHour() {
        return healthEmergencyPerHour.clone();
    }

    public int[] getMechanicalEmergencyPerHour() {
        return mechanicalEmergencyPerHour.clone();
    }

    public double getCancellationRate() {
        int denominator = cancelled + totalTakeOffAircrafts;

        if (denominator == 0) {
            return 0.0;
        }

        return (double) cancelled / denominator;
    }

    public double getDiversionRate() {
        int denominator = diverted + totalArrivalAircrafts;

        if (denominator == 0) {
            return 0.0;
        }

        return (double) diverted / denominator;
    }

    public double getCancellationRatePercent() {
        return getCancellationRate() * 100.0;
    }

    public double getDiversionRatePercent() {
        return getDiversionRate() * 100.0;
    }

    public int getTotalRunwayClosures() {
        int total = 0;
        for (int value : runwayClosureEventType_Count.values()) {
            total += value;
        }
        return total;
    }

    public double getStabilityScore(){
        double instabilityScore =  (this.getCancellationRatePercent() + this.getDiversionRatePercent())/2;
        double stabilityScore = 100 - instabilityScore;
        return Math.round(stabilityScore * 100.0) / 100.0; //To 2 d.p.
    }


    // AVERAGES
    public double getAverageHoldingQueueSize()
    {
        int hours = getAverageHoldingQueueSizePerHour().length;
        double sum = 0;
        for (double val : getAverageHoldingQueueSizePerHour())
        {
            sum += val;
        }

        return sum / hours;
    }

    public double getAverageTakeoffQueueSize()
    {
        int hours = getAverageTakeoffQueueSizePerHour().length;
        double sum = 0;
        for (double val : getAverageTakeoffQueueSizePerHour())
        {
            sum += val;
        }

        return sum / hours;
    }
}

