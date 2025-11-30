package com.example.trying3.controller.design;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage; // Import yang dibutuhkan

import java.net.URL;
import java.util.Optional; // Import yang dibutuhkan
import java.util.ResourceBundle;

public class DashboardDesignController implements Initializable {

    // Labels untuk Statistik
    @FXML private Label lblMenunggu;
    @FXML private Label lblDisetujui;
    @FXML private Label lblRevisi;

    // Tombol Sidebar
    @FXML private Button btnDashboard;
    @FXML private Button btnKelola;
    @FXML private Button btnTemplate;
    @FXML private Button btnRiwayat;

    // Tombol Logout BARU
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Data Dummy
        lblMenunggu.setText("1");
        lblDisetujui.setText("2");
        lblRevisi.setText("0");

        // Contoh event handler tombol (Opsional)
        btnKelola.setOnAction(e -> System.out.println("Buka Kelola Desain"));
        btnTemplate.setOnAction(e -> System.out.println("Buka Template"));

        // Menghubungkan aksi Logout
        btnLogout.setOnAction(e -> handleLogoutClick());
    }

    // Method untuk menangani KLIK LOGOUT
    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Yakin ingin logout dari sistem?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText("Keluar dari Dashboard Tim Desain");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            // Aksi Logout:
            // 1. Dapatkan Stage (Jendela) saat ini
            Stage stage = (Stage) btnLogout.getScene().getWindow();

            // 2. Di sini kamu bisa menambahkan kode untuk membuka kembali LoginScreen
            // Contoh: Panggil LoginController atau Main Application untuk pindah Scene
            System.out.println("LOGOUT BERHASIL. Pindah ke Scene Login.");

            // Untuk demo: tutup aplikasi
            // stage.close();
        }
    }
}