package com.example.trying3.controller.management;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class EksporDataController {

    @FXML
    public void initialize() {
        // Init jika diperlukan
    }

    @FXML
    private void handleEksporSemua() {
        showSuccessAlert("Ekspor Semua Pesanan", "Data sedang diunduh...");
    }

    @FXML
    private void handleEksporPelanggan() {
        showSuccessAlert("Ekspor Data Pelanggan", "Data pelanggan berhasil diekspor.");
    }

    @FXML
    private void handleLaporanKeuangan() {
        showSuccessAlert("Laporan Keuangan", "Laporan keuangan sedang disiapkan.");
    }

    @FXML
    private void handleLaporanProduksi() {
        showSuccessAlert("Laporan Produksi", "Laporan produksi berhasil diunduh.");
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ekspor Data");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}