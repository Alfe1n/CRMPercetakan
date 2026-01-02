package com.example.trying3.controller.management;

import com.example.trying3.dao.PesananDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller untuk halaman Analitik.
 * Menampilkan tren pesanan dan distribusi pendapatan dalam bentuk grafik.
 */
public class AnalitikController implements Initializable {

    @FXML private LineChart<String, Number> lineChart;
    @FXML private PieChart revenuePieChart;

    private final PesananDAO pesananDAO = new PesananDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadTrendData();
        loadRevenueData();
    }

    private void loadTrendData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Integer> data = pesananDAO.getOrderTrend();

        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
        } else {
            data.forEach((tgl, jml) -> series.getData().add(new XYChart.Data<>(tgl, jml)));
        }
        lineChart.getData().add(series);
    }

    private void loadRevenueData() {
        Map<String, Double> data = pesananDAO.getRevenueDistribution();
        revenuePieChart.getData().clear();

        if (data == null || data.isEmpty()) {
            revenuePieChart.getData().add(new PieChart.Data("Data Belum Tersedia", 1));
            System.out.println("Peringatan: Tidak ada data pendapatan ditemukan di DB.");
        } else {
            data.forEach((layanan, total) -> {
                revenuePieChart.getData().add(new PieChart.Data(layanan, total));
            });
        }
    }
}