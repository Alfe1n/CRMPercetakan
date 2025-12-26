package com.example.trying3.controller.production;

import com.example.trying3.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardProductionController implements Initializable {

    @FXML private Label btnDashboard;
    @FXML private Label btnProduksi;
    @FXML private Label btnJadwalProduksi;
    @FXML private Label btnInventori;
    @FXML private Label btnLogout;

    @FXML private Label lblHeaderTitle;
    @FXML private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPane("DashboardProductionPane.fxml");
        setActiveButton(btnDashboard);

        // KUNCI PERBAIKAN: Set judul header SATU KALI saja di awal.
        // Jangan diubah-ubah di method handle click.
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Dashboard Tim Produksi");
        }
    }

    private boolean loadPane(String fxmlFile) {
        String relativePath = "fxml/production/" + fxmlFile;
        URL resource = MainApplication.class.getResource(relativePath);

        if (resource == null) {
            System.err.println("Resource not found: " + relativePath);
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setActiveButton(Label activeButton) {
        btnDashboard.getStyleClass().remove("active");
        btnProduksi.getStyleClass().remove("active");
        btnJadwalProduksi.getStyleClass().remove("active");
        btnInventori.getStyleClass().remove("active");

        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleDashboardClick() {
        if (loadPane("DashboardProductionPane.fxml")) setActiveButton(btnDashboard);
    }

    @FXML
    private void handleProduksiClick() {
        if (loadPane("ProduksiPane.fxml")) setActiveButton(btnProduksi);
    }

    @FXML
    private void handleJadwalProduksiClick() {
        if (loadPane("JadwalProduksiPane.fxml")) setActiveButton(btnJadwalProduksi);
    }

    @FXML
    private void handleInventoriClick() {
        if (loadPane("InventoriPane.fxml")) setActiveButton(btnInventori);
    }

    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // 1. Load file Login.fxml (sesuaikan path foldernya)
                    // Berdasarkan struktur folder di screenshot kamu: fxml/auth/Login.fxml
                    URL loginResource = MainApplication.class.getResource("fxml/auth/Login.fxml");
                    FXMLLoader loader = new FXMLLoader(loginResource);
                    Parent root = loader.load();

                    // 2. Ambil stage (window) yang sekarang sedang terbuka
                    Stage stage = (Stage) btnLogout.getScene().getWindow();

                    // 3. Set scene baru dengan root login
                    Scene scene = new Scene(root);
                    stage.setScene(scene);

                    // 4. (Opsional) Ketengahkan window agar rapi
                    stage.centerOnScreen();
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Gagal memuat halaman login: " + e.getMessage());
                }
            }
        });
    }
}