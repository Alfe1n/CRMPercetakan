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

    private void loadPane(String fxmlFile) {
        try {
            String relativePath = "fxml/production/" + fxmlFile;
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));
            Node pane = loader.load();
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Label button) {
        button.getStyleClass().add("active");
    }

    @FXML
    private void handleDashboardClick() {
        loadPane("DashboardProductionPane.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleProduksiClick() {
        loadPane("ProduksiPane.fxml");
        setActiveButton(btnProduksi);
    }

    @FXML
    private void handleJadwalProduksiClick() {
        loadPane("JadwalProduksiPane.fxml");
        setActiveButton(btnJadwalProduksi);
    }

    @FXML
    private void handleInventoriClick() {
        loadPane("InventoriPane.fxml");
        setActiveButton(btnInventori);
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
