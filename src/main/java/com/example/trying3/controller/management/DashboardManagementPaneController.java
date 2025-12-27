package com.example.trying3.controller.management;

import com.example.trying3.dao.PesananDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.Map; // INI YANG WAJIB ADA AGAR TIDAK ERROR
import java.util.ResourceBundle;

public class DashboardManagementPaneController implements Initializable {

    @FXML private Label totalOrderLabel;
    @FXML private Label completionLabel;
    @FXML private Label revenueLabel;
    @FXML private Label avgOrderValueLabel;
    @FXML private PieChart serviceChart;
    @FXML private PieChart statusChart;

    private final PesananDAO pesananDAO = new PesananDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        // Ambil data angka dari DAO
        int total = pesananDAO.getTotalPesananCount();
        double revenue = pesananDAO.getTotalRevenue();
        int selesai = pesananDAO.getSelesaiCount();

        // Hitung Logic Bisnis
        double completionRate = total > 0 ? ((double) selesai / total) * 100 : 0;
        double avgValue = total > 0 ? revenue / total : 0;

        // Tampilkan ke Label
        totalOrderLabel.setText(String.valueOf(total));
        completionLabel.setText(String.format("%.1f%%", completionRate));
        revenueLabel.setText(String.format("Rp %,.0f", revenue));
        avgOrderValueLabel.setText(String.format("Rp %,.0f", avgValue));

        // Setup Grafik
        setupServiceChart();
        setupStatusChart();
    }

    private void setupServiceChart() {
        // 1. Ambil data dari DAO yang sudah diperbaiki query-nya
        Map<String, Integer> data = pesananDAO.getServiceDistribution();

        // 2. Bersihkan data lama di chart
        serviceChart.getData().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        if (data == null || data.isEmpty()) {
            pieData.add(new PieChart.Data("Data Belum Tersedia", 1));
        } else {
            data.forEach((layanan, jumlah) -> {
                // Membuat label yang informatif (Nama Layanan + Jumlah)
                String label = layanan + " (" + jumlah + ")";
                pieData.add(new PieChart.Data(label, jumlah));
            });
        }

        serviceChart.setData(pieData);
    }

    private void setupStatusChart() {
        Map<String, Integer> data = pesananDAO.getStatusDistribution();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (data != null) {
            data.forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
        }
        statusChart.setData(pieData);
    }
}