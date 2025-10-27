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
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            e.printStackTrace();
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
    private void handleLaporanClick() {
        loadPane("LaporanPane.fxml");
        setActiveButton(btnLaporan);
    }

    @FXML
    private void handleAnalitikClick() {
        loadPane("AnalitikPane.fxml");
        setActiveButton(btnAnalitik);
    }

    @FXML
    private void handleEksporDataClick() {
        loadPane("EksporDataPane.fxml");
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
