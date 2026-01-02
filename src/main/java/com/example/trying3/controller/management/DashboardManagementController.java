package com.example.trying3.controller.management;

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

/**
 * Controller utama untuk Dashboard Manajemen.
 * Menangani navigasi antar pane dan sidebar menu untuk Manager.
 */
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
            if (MainApplication.class.getResource(relativePath) == null) {
                throw new IOException("File FXML tidak ditemukan: " + relativePath);
            }

            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);

        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Page");
            alert.setHeaderText("Gagal memuat halaman: " + fxmlFile);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Set button sidebar yang aktif dengan menambah style class "active".
     */
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
