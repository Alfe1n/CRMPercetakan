package com.example.trying3.controller.admin;

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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    // FXML Components - Navigation Sidebar
    @FXML private Label btnDashboard;
    @FXML private Label btnKelolaPesanan;
    @FXML private Label btnPembayaran;
    @FXML private Label btnManagementUser;
    @FXML private Label btnNotifikasi;
    @FXML private Label btnLogout;
    @FXML private StackPane contentArea;

    // Navigation state management
    private final Map<String, Node> panes = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    private Node activePane = null;
    private Label activeButton = null;

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        setupNavigationStyles();
        loadPane("DashboardPane.fxml");
        setActiveButton(btnDashboard);
    }

    /**
     * Mengatur hover effects untuk tombol navigasi sidebar
     */
    private void setupNavigationStyles() {
        Label[] navButtons = {btnDashboard, btnKelolaPesanan, btnPembayaran, btnManagementUser, btnNotifikasi};
        for (Label button : navButtons) {
            if (button != null) {
                button.setOnMouseEntered(e -> {
                    if (button != activeButton) {
                        button.getStyleClass().add("hover");
                    }
                });
                button.setOnMouseExited(e -> button.getStyleClass().remove("hover"));
            }
        }

        if (btnLogout != null) {
            btnLogout.setOnMouseEntered(e -> btnLogout.getStyleClass().add("logout-hover"));
            btnLogout.setOnMouseExited(e -> btnLogout.getStyleClass().remove("logout-hover"));
        }
    }

    /**
     * Load panel secara dinamis dengan caching untuk performa optimal
     *
     * @param fxmlFile Nama file FXML yang akan dimuat
     */
    private void loadPane(String fxmlFile) {
        try {
            if (activePane != null) {
                activePane.setVisible(false);
            }

            if (panes.containsKey(fxmlFile)) {
                activePane = panes.get(fxmlFile);
                activePane.setVisible(true);

                if ("DashboardPane.fxml".equals(fxmlFile)) {
                    Object controller = controllers.get(fxmlFile);
                    if (controller instanceof DashboardPaneController dashboardController) {
                        dashboardController.refresh();
                    }
                }
            } else {
                String relativePath = "fxml/admin/" + fxmlFile;
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));

                if (loader.getLocation() == null) {
                    throw new IOException("Cannot find FXML file: " + relativePath);
                }

                activePane = loader.load();

                panes.put(fxmlFile, activePane);
                controllers.put(fxmlFile, loader.getController());
                contentArea.getChildren().add(activePane);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Memuat Halaman", "Gagal memuat file: " + fxmlFile, e.getMessage());
        }
    }

    /**
     * Mengatur tombol navigasi yang sedang aktif
     *
     * @param button Tombol yang akan diset sebagai aktif
     */
    private void setActiveButton(Label button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }

        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }
    }

    /**
     * Menampilkan dialog error
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Navigation event handlers

    @FXML
    private void handleDashboardClick() {
        loadPane("DashboardPane.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void handleKelolaPesananClick() {
        loadPane("KelolaPesananPane.fxml");
        setActiveButton(btnKelolaPesanan);
    }

    @FXML
    private void handlePembayaranClick() {
        loadPane("KelolaPembayaranPane.fxml");
        setActiveButton(btnPembayaran);
    }

    @FXML
    private void handleManajemenUserClick() {
        loadPane("ManajemenUserPane.fxml");
        setActiveButton(btnManagementUser);
    }

    @FXML
    private void handleNotifikasiClick() {
        loadPane("NotifikasiPane.fxml");
        setActiveButton(btnNotifikasi);
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