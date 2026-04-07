package uk.ac.warwick.cs261.ui.controllers;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import uk.ac.warwick.cs261.App;
import uk.ac.warwick.cs261.simulation.FinalSimulationStats;
import uk.ac.warwick.cs261.simulation.SimulationConstants;
import uk.ac.warwick.cs261.simulation.entities.runway.RunwayStatus;


public class ReportsController {

    private FinalSimulationStats finalSimulationStats; 
    
    @FXML ToggleButton switchGraphs;

    @FXML Label tMaxWait;
    @FXML Label tAvgWait;
    @FXML Label tMaxDelay;
    @FXML Label tAvgDelay;

    @FXML Label tMaxQueue;
    @FXML Label totalTakeoffAircraft;

    @FXML Label hMaxWait;
    @FXML Label hAvgWait;
    @FXML Label hMaxDelay;
    @FXML Label hAvgDelay;

    @FXML Label hMaxQueue;
    @FXML Label totalLandingAircraft;

    @FXML Label totalCancellations;
    @FXML Label totalDiversions;
    @FXML Label stabilityScore;

    @FXML Label inspections;
    @FXML Label equipFailures;
    @FXML Label snowClearances;
    @FXML Label totalClosures;

    @FXML VBox takeoffGraph1;
    @FXML VBox takeoffGraph2; 

    @FXML VBox holdingGraph1;
    @FXML VBox holdingGraph2;

    @FXML PieChart aircraftPie1;

    @FXML PieChart runwayPie1; 
    @FXML PieChart runwayPie2;

    @FXML LineChart<Number, Number> takeoffQueueSizePerHour;
    @FXML LineChart<Number, Number> holdingQueueSizePerHour;

    @FXML LineChart<Number, Number> diversionsLineGraph;
    @FXML LineChart<Number, Number> cancellationLineGraph;



    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    private void initialize() {
    }

    public void setFinalSimulationStatistics(FinalSimulationStats finalSimulationStats)
    {
        this.finalSimulationStats = finalSimulationStats;

        addScaledPerHourChartSeriesDouble(takeoffQueueSizePerHour, finalSimulationStats.getTakeoffQueueSizePerHour(), "Takeoff Queue Size");
        addScaledPerHourChartSeriesDouble(holdingQueueSizePerHour, finalSimulationStats.getHoldingQueueSizePerHour(), "Holding Queue Size");
        
        addScaledPerHourChartSeriesInt(cancellationLineGraph, finalSimulationStats.getCancellationsPerHour(), "Cancellations");
        addScaledPerHourChartSeriesInt(diversionsLineGraph, finalSimulationStats.getDiversionsPerHour(), "Diversions");


        addBoxPlotSeries(takeoffGraph1, finalSimulationStats.getTakeoffWaitDistribution(), "Takeoff Wait"); //example
        addBoxPlotSeries(takeoffGraph2, finalSimulationStats.getTakeoffDelayDistribution(), "Takeoff Delay"); //example

        addBoxPlotSeries(holdingGraph1, finalSimulationStats.getLandingWaitDistribution(), "Landing Wait"); //example
        addBoxPlotSeries(holdingGraph2, finalSimulationStats.getLandingDelayDistribution(), "Landing Delay"); //example


        addPieChartSeries(runwayPie1, finalSimulationStats.getRunwayModesInitial(), "Runway Modes");
        addPieChartSeries(aircraftPie1, finalSimulationStats.getAircraftEmergencies_Map(), "Aircraft Emergencies");
        addPieChartSeries(runwayPie2, finalSimulationStats.getRunwayClosureTimeRatio(), "Runway Closures Ratio (by time)");


        updateUIFromFinalStats();

    }

    @FXML
    private void help(ActionEvent event) {
        System.out.println("Help");
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to use the simulation:");
        String desc = "Use tabs to navigate categories for statistics";
        String descFormatString = String.format(desc);
        alert.setContentText(descFormatString);
        ImageView icon = new ImageView(new Image(String.valueOf(this.getClass().getResource("/ui/images/plane.png"))));
        icon.setFitHeight(60);
        icon.setFitWidth(60);
        icon.setRotate(315);
        alert.getDialogPane().setGraphic(icon);
        alert.showAndWait();
    }

    @FXML
    private void toStartScreen() {
        try 
        {
            App.goToFXMLScreen(App.START_SCRREN_FXML_PATH);
        }
        catch (IOException er) 
        {
            er.printStackTrace();
        }
    }

    private void addScaledPerHourChartSeriesDouble(LineChart<Number, Number> chart, double[] data, String seriesName) 
    {
        int sizeGroups = 6 * (data.length / 120);

        if (sizeGroups < 1)
            addLineChartSeriesDouble(chart, data, String.format("%s Per Hour", seriesName));
        else
            addLineChartSeriesDouble(chart, condenseHourlyDataDouble(data, sizeGroups), String.format("%s Per %d Hours", seriesName, sizeGroups));
    }

    private void addScaledPerHourChartSeriesInt(LineChart<Number, Number> chart, int[] data, String seriesName) 
    {
        int sizeGroups = 6 * (data.length / 120);

        if (sizeGroups < 1)
            addLineChartSeriesInt(chart, data, String.format("%s Per Hour", seriesName));
        else
            addLineChartSeriesDouble(chart, condenseHourlyDataInt(data, sizeGroups), String.format("%s Per %d Hours", seriesName, sizeGroups));
    }

    private void addLineChartSeriesDouble(LineChart<Number, Number> chart, double[] data, String seriesName) 
    {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (int i = 0; i < data.length; i++)
            series.getData().add(new XYChart.Data<>(i, data[i]));

        chart.getData().add(series);
        series.getNode().setStyle("-fx-stroke: coral;");
    }

    private void addLineChartSeriesInt(LineChart<Number, Number> chart, int[] data, String seriesName) 
    {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        for (int i = 0; i < data.length; i++)
            series.getData().add(new XYChart.Data<>(i, data[i]));

        chart.getData().add(series);
        series.getNode().setStyle("-fx-stroke: coral;");
    }

    private <K, V extends Number> void addPieChartSeries(PieChart chart, Map<K, V> data, String title){
        chart.setTitle(title);
        chart.getData().clear();

        for (Map.Entry<K, V> entry : data.entrySet()){
            chart.getData().add(new PieChart.Data(
                formatEnumName(entry.getKey()),
                entry.getValue().doubleValue()
            ));
            //Converting entry value double to use Pie Charts in JavaFX does not pose an issue as aircrafts would have to exceed 9^15 in a single aimulation, or
            // simulation time would have to exceed 285 million years, which is not possible in real life.
        }
    }

    private void addBoxPlotSeries(VBox container, Map<String, Double> data, String title) {
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

        //Create BoxAndWhiskerItem from statistics
        BoxAndWhiskerItem item = new BoxAndWhiskerItem(
                data.get("mean"),   // mean
                data.get("p50"),    // median
                data.get("p25"),    // Q1
                data.get("p75"),    // Q3
                data.get("min"),    // min
                data.get("max"),    // max
                data.get("min"),
                data.get("max"),
                null           
        );

        //Add the item to the dataset
        dataset.add(item, "Series", "Data");

        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                title,
                null,
                null,
                dataset,
                false );

        // Decrease title size
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 12));


        //Box Plot looks customisation
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setOrientation(PlotOrientation.HORIZONTAL);

        CategoryAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();
        BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

        xAxis.setVisible(false);

        // Set initial values axis viewer size (i.e. how zoomed it is)
        double min = data.get("min");
        double max = data.get("max");
        double padding = (max - min) * 0.2;
        yAxis.setRange(min - padding, max + padding);
        
        // Set the padding between the box plot and the chart viwever walls
        xAxis.setLowerMargin(0.25);
        xAxis.setUpperMargin(0.25);
        xAxis.setCategoryMargin(0.4);
        
        // General improvements to visual elements
        renderer.setFillBox(true);       
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setMedianVisible(true);
        renderer.setMeanVisible(false);

        chart.setBackgroundPaint(new Color(245, 245, 245));
        plot.setBackgroundPaint(new Color(235, 235, 235));
        renderer.setSeriesPaint(0, new Color(66, 135, 245)); 
        renderer.setSeriesOutlinePaint(0, Color.DARK_GRAY);
        chart.setBorderVisible(false);
        plot.setOutlineVisible(false);

        ChartViewer viewer = new ChartViewer(chart);
        viewer.setPrefSize(440, 240);

        container.getChildren().clear();
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(viewer);
    }

    private void updateUIFromFinalStats() {
        // Takeoff statistics
        tMaxWait.setText(String.format("%.2f", (double) finalSimulationStats.getMaxTakeoffWait()/ SimulationConstants.MINUTES_TO_SECONDS));
        tAvgWait.setText(String.format("%.2f", (double) finalSimulationStats.getAverageTakeoffWait() / SimulationConstants.MINUTES_TO_SECONDS));
        tAvgDelay.setText(String.format("%.2f", (double) finalSimulationStats.getAverageTakeoffDelay() / SimulationConstants.MINUTES_TO_SECONDS));
        tMaxDelay.setText(String.format("%.2f", (double) finalSimulationStats.getMaxTakeoffDelay() / SimulationConstants.MINUTES_TO_SECONDS));

        tMaxQueue.setText(String.valueOf(finalSimulationStats.getMaxTakeoffQueueSize()));
        totalTakeoffAircraft.setText(String.valueOf(finalSimulationStats.getTotalTakeoffAircrafts()));

        // tAvgQueue.setText(String.valueOf(finalSimulationStats.get()));

        // Holding statistics :
        hMaxWait.setText(String.format("%.2f", (double) finalSimulationStats.getMaxLandingWait() / SimulationConstants.MINUTES_TO_SECONDS));
        hAvgWait.setText(String.format("%.2f", (double) finalSimulationStats.getAverageLandingWait() / SimulationConstants.MINUTES_TO_SECONDS));
        hAvgDelay.setText(String.format("%.2f",(double) finalSimulationStats.getAverageLandingDelay() / SimulationConstants.MINUTES_TO_SECONDS));
        hMaxDelay.setText(String.format("%.2f", (double) finalSimulationStats.getMaxLandingDelay() / SimulationConstants.MINUTES_TO_SECONDS));

        hMaxQueue.setText(String.valueOf(finalSimulationStats.getMaxHoldingQueueSize()));
        // hMaxQueue.setText(String.valueOf(finalSimulationStats.getMaxHoldingQueueSize()));
        totalLandingAircraft.setText(String.valueOf(finalSimulationStats.getTotalLandingAircrafts()));


        //Aircraft statistics: emergencies, cancellations, diversions
        totalCancellations.setText(String.valueOf(finalSimulationStats.getTotalCancellations()));
        totalDiversions.setText(String.valueOf(finalSimulationStats.getTotalDiversions()));
        // System stability meaning
        stabilityScore.setText(String.valueOf(finalSimulationStats.getStabilityScore()));

        // Failures
        inspections.setText(String.valueOf(finalSimulationStats.getRunwayClosureCounts().get(RunwayStatus.INSPECTION)));
        equipFailures.setText(String.valueOf(finalSimulationStats.getRunwayClosureCounts().get(RunwayStatus.EQUIPMENT_FAILURE)));
        snowClearances.setText(String.valueOf(finalSimulationStats.getRunwayClosureCounts().get(RunwayStatus.SNOW_CLEARANCE)));
        totalClosures.setText(String.valueOf(finalSimulationStats.getTotalRunwayClosures()));


    }

    @FXML
    private void onSave(ActionEvent e) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Simulation Config");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName("simulation_stats.json");

            File file = chooser.showSaveDialog(null);

            if (file != null) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, finalSimulationStats);
                showInfo("Saved", "Statistics saved successfully.");
            }
        } catch (Exception ex) {
            showError("Save Failed", ex.getMessage());
        }
    }

    @FXML
    private void onLoad(ActionEvent e) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Load Simulation Config");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

            File file = chooser.showOpenDialog(null);
            if (file != null) {
                FinalSimulationStats stats = mapper.readValue(file, FinalSimulationStats.class);
                setFinalSimulationStatistics(stats);
                showInfo("Loaded", "Statistics restored.");
            }
        } catch (IOException ex) {
            showError("Load Failed", ex.getMessage());
        }
    }

    private int toggleCount;
    @FXML
    private void togglePie(ActionEvent e) {
        toggleCount++;
        if (toggleCount%2 ==1){
            switchGraphs.setText("In | Final");
            addPieChartSeries(runwayPie1, finalSimulationStats.getRunwayModesFinal(), "Runway Modes (Final)");
        } else {
            switchGraphs.setText("Initial | Fi");
            addPieChartSeries(runwayPie1, finalSimulationStats.getRunwayModesInitial(), "Runway Modes (Initial)");
        }
    }

    private void showInfo(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    double[] condenseHourlyDataDouble(double[] data, int groupSize)
    {
        if (groupSize <= 0)
            return null;

        int length = (data.length + (groupSize  -1)) / groupSize;
        double[] dailyData = new double[length];

        for (int i = 0; i < length; i++)
        {
            double sum = 0; 
            int base = i * groupSize;

            int count = 0;
            int max_count = Math.min(groupSize, data.length - base);

            for (; count < max_count; count++)
               sum += data[base + count];

            dailyData[i] = sum / count;
        }

        return dailyData;
    }

    double[] condenseHourlyDataInt(int[] data, int groupSize)
    {
        if (groupSize <= 0)
            return null;

        int length = (data.length + (groupSize  -1)) / groupSize;
        double[] dailyData = new double[length];

        for (int i = 0; i < length; i++)
        {
            double sum = 0; 
            int base = i * groupSize;

            int count = 0;
            int max_count = Math.min(groupSize, data.length - base);

            for (; count < max_count; count++)
               sum += data[base + count];

            dailyData[i] = sum / count;
        }

        return dailyData;
    }

    private String formatEnumName(Object key) {
        String text = key.toString().replace("_", " ").toLowerCase();

        String[] words = text.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }
}
