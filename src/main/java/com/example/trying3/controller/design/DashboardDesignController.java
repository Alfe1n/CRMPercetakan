package com.example.trying3.controller.design;

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

public class DashboardDesignController implements Initializable {

    @FXML private Label btnDashboard;
    @FXML private Label btnAntrianDesign;
    @FXML private Label btnTemplate;
    @FXML private Label btnRiwayat;
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

    /**
     * DIPERBAIKI: Menghapus class "active" dari SEMUA button sidebar
     */
    private void setActiveButton(Label activeButton) {
        // Hapus "active" dari SEMUA button
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("active");
        if (btnAntrianDesign != null) btnAntrianDesign.getStyleClass().remove("active");
        if (btnTemplate != null) btnTemplate.getStyleClass().remove("active");
        if (btnRiwayat != null) btnRiwayat.getStyleClass().remove("active");

        // Tambahkan "active" ke button yang dipilih
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void handleDashboardClick() {
        if (loadPane("DashboardDesignPane.fxml")) setActiveButton(btnDashboard);
    }

    @FXML
    private void handleAntrianClick() {
        if (loadPane("KelolaDesain.fxml")) setActiveButton(btnAntrianDesign);
    }

    @FXML
    private void handleTemplateClick() {
        if (loadPane("TemplateDesainPane.fxml")) setActiveButton(btnTemplate);
    }

    @FXML
    private void handleRiwayatClick() {
        if (loadPane("RiwayatDesainPane.fxml")) setActiveButton(btnRiwayat);
    }

    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    URL loginResource = MainApplication.class.getResource("fxml/auth/Login.fxml");
                    FXMLLoader loader = new FXMLLoader(loginResource);
                    Parent root = loader.load();

                    Stage stage = (Stage) btnLogout.getScene().getWindow();

                    Scene scene = new Scene(root);
                    stage.setScene(scene);

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