package com.example.trying3.controller.management;

import com.example.trying3.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardManagementController implements Initializable {

    @FXML private Label btnDashboard;
    @FXML private Label btnLaporan;
    @FXML private Label btnAnalitik;
    @FXML private Label btnEksporData;
    @FXML private Label btnLogout;
    @FXML private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPane("DashboardManagementPane.fxml");
        setActiveButton(btnDashboard);
    }

    private void loadPane(String fxmlFile) {
        try {
            String relativePath = "fxml/manajemen/" + fxmlFile;
            // Cek apakah file benar-benar ditemukan
            if (MainApplication.class.getResource(relativePath) == null) {
                throw new IOException("File FXML tidak ditemukan: " + relativePath);
            }

            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);

        } catch (IOException e) {
            // INI PENTING: Tampilkan error lengkap ke console
            System.err.println("=== ERROR SAAT MEMBUKA " + fxmlFile + " ===");
            e.printStackTrace();

            // Opsional: Tampilkan pesan ke layar pengguna
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Page");
            alert.setHeaderText("Gagal memuat halaman: " + fxmlFile);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void setActiveButton(Label button) {
        btnDashboard.getStyleClass().remove("active");
        btnLaporan.getStyleClass().remove("active");
        btnAnalitik.getStyleClass().remove("active");
        btnEksporData.getStyleClass().remove("active");
        button.getStyleClass().add("active");
    }

    @FXML
    private void handleDashboardClick() {
        loadPane("DashboardManagementPane.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void openLaporan() {
        loadPane("Laporan.fxml");
        setActiveButton(btnLaporan);
    }

    @FXML
    private void openAnalitik() {
        loadPane("Analitik.fxml");
        setActiveButton(btnAnalitik);
    }


    @FXML
    private void handleEksporDataClick() {
        loadPane("EksporData.fxml");
        setActiveButton(btnEksporData);
    }

    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) System.exit(0);
        });
    }
}
