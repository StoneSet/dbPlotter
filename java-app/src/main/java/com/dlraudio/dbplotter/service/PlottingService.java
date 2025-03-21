package com.dlraudio.dbplotter.service;

import com.dlraudio.dbplotter.model.FrequencyData;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;

public class PlottingService {

    private final LineChart<Number, Number> lineChart;
    private List<FrequencyData> originalData;  // données originales
    private List<FrequencyData> currentData;   // données affichées

    public PlottingService(LineChart<Number, Number> lineChart) {
        this.lineChart = lineChart;
        this.originalData = new ArrayList<>();
        this.currentData = new ArrayList<>();
    }

    public void initializePlot(String xAxisLabel, String yAxisLabel) {
        lineChart.getXAxis().setLabel(xAxisLabel);
        lineChart.getYAxis().setLabel(yAxisLabel);
        lineChart.setAnimated(false);
    }

    // Ajoute les données au graphique
    public void plotData(List<FrequencyData> dataPoints) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Frequency Response");

        for (FrequencyData data : dataPoints) {
            series.getData().add(new XYChart.Data<>(data.getFrequency(), data.getMagnitude()));
        }

        lineChart.getData().clear();
        lineChart.getData().add(series);

        currentData = new ArrayList<>(dataPoints);
    }

    public void clearPlot() {
        lineChart.getData().clear();
    }

    public void setCurrentData(List<FrequencyData> dataPoints) {
        this.originalData = new ArrayList<>(dataPoints);
        this.currentData = new ArrayList<>(dataPoints);  // Initialement, les données actuelles sont les mêmes
    }

    public List<FrequencyData> getOriginalData() {
        return new ArrayList<>(originalData);  // Retourne une copie pour éviter les modifications
    }

    public List<FrequencyData> getCurrentData() {
        return new ArrayList<>(currentData);
    }
}
