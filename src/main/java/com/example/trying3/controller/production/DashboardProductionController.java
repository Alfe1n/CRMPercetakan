package com.example.trying3.controller.production;

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

public class DashboardProductionController implements Initializable {

    @FXML private Label btnDashboard;
    @FXML private Label btnProduksi;
    @FXML private Label btnJadwalProduksi;
    @FXML private Label btnInventori;
    @FXML private Label btnLogout;
    @FXML private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPane("DashboardProductionPane.fxml");
        setActiveButton(btnDashboard);
    }

    private boolean loadPane(String fxmlFile) {
        String relativePath = "fxml/production/" + fxmlFile;
        System.out.println("[DashboardProductionController] Loading FXML: " + relativePath);
        URL resource = MainApplication.class.getResource(relativePath);
        if (resource == null) {
            System.out.println("[DashboardProductionController] Resource NOT FOUND: " + relativePath);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Resource not found: " + relativePath, javafx.scene.control.ButtonType.OK);
            alert.setTitle("Load Error");
            alert.setHeaderText(null);
            alert.showAndWait();
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);
            System.out.println("[DashboardProductionController] Loaded successfully: " + fxmlFile);
            return true;
        } catch (IOException e) {
            System.out.println("[DashboardProductionController] Failed to load FXML: " + fxmlFile + " error: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load FXML: " + fxmlFile + "\n" + e.getMessage(), javafx.scene.control.ButtonType.OK);
            alert.setTitle("Load Error");
            alert.setHeaderText(null);
            alert.showAndWait();
            return false;
        }
    }

    private void setActiveButton(Label activeButton) {
        // Hapus "active" dari semua tombol sidebar
        btnDashboard.getStyleClass().remove("active");
        btnProduksi.getStyleClass().remove("active");
        btnJadwalProduksi.getStyleClass().remove("active");
        btnInventori.getStyleClass().remove("active");

        // Tambahkan "active" ke tombol yang sedang diklik
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleDashboardClick() {
        System.out.println("[DashboardProductionController] handleDashboardClick");
        if (loadPane("DashboardProductionPane.fxml")) setActiveButton(btnDashboard);
    }

    @FXML
    private void handleProduksiClick() {
        System.out.println("[DashboardProductionController] handleProduksiClick");
        // Tampilkan UI 'Kelola Produksi' sesuai desain: gunakan ProduksiPane.fxml
        if (loadPane("ProduksiPane.fxml")) setActiveButton(btnProduksi);
    }

    @FXML
    private void handleJadwalProduksiClick() {
        System.out.println("[DashboardProductionController] handleJadwalProduksiClick");
        if (loadPane("JadwalProduksiPane.fxml")) setActiveButton(btnJadwalProduksi);
    }

    @FXML
    private void handleInventoriClick() {
        System.out.println("[DashboardProductionController] handleInventoriClick");
        if (loadPane("InventoriPane.fxml")) setActiveButton(btnInventori);
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
