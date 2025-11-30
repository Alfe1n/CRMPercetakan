package com.example.trying3.controller.design;

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

public class DashboardDesignController implements Initializable {

    @FXML private Label btnDashboard;
    @FXML private Label btnAntrianDesign;
    @FXML private Label btnRevisi;
    @FXML private Label btnAsset;
    @FXML private Label btnLogout;

    @FXML private Label lblHeaderTitle;
    @FXML private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load default pane saat pertama kali buka
        loadPane("DashboardDesignPane.fxml");
        setActiveButton(btnDashboard);

        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText("Dashboard Tim Design");
        }
    }

    private boolean loadPane(String fxmlFile) {
        String relativePath = "fxml/designer/" + fxmlFile;
        URL resource = MainApplication.class.getResource(relativePath);

        if (resource == null) {
            System.err.println("❌ Resource not found: " + relativePath);
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Error loading pane: " + fxmlFile);
            e.printStackTrace();
            return false;
        }
    }

    private void setActiveButton(Label activeButton) {
        btnDashboard.getStyleClass().remove("active");
        btnAntrianDesign.getStyleClass().remove("active");
        btnRevisi.getStyleClass().remove("active");
        btnAsset.getStyleClass().remove("active");

        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML private void handleDashboardClick() {
        if (loadPane("DashboardDesignPane.fxml")) setActiveButton(btnDashboard);
    }

    @FXML private void handleAntrianClick() {
        // Pastikan file AntrianDesignPane.fxml nanti dibuat, sementara pakai dashboard dulu kalau belum ada
        if (loadPane("KelolaDesain.fxml")) setActiveButton(btnAntrianDesign);
    }

    @FXML private void handleRevisiClick() {
        if (loadPane("RevisiPane.fxml")) setActiveButton(btnRevisi);
    }

    @FXML private void handleAssetClick() {
        if (loadPane("AssetPane.fxml")) setActiveButton(btnAsset);
    }

    @FXML private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) System.exit(0);
        });
    }
}