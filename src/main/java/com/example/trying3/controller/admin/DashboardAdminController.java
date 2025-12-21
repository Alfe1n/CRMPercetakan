package com.example.trying3.controller.admin;

import com.example.trying3.MainApplication;
import com.example.trying3.model.User;
import com.example.trying3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    // FXML BINDINGS - SIDEBAR
    @FXML private Label btnDashboard;
    @FXML private Label btnKelolaPesanan;
    @FXML private Label btnPembayaran;
    @FXML private Label btnManagementUser;
    @FXML private Label btnPengaturan;
    @FXML private Label btnNotifikasi;
    @FXML private Label btnLogout;
    @FXML private StackPane contentArea;

    // FXML BINDINGS - ICONS
    @FXML private ImageView dashboardIcon;
    @FXML private ImageView kelolaPesananIcon;
    @FXML private ImageView pembayaranIcon;
    @FXML private ImageView manajemenUserIcon;
    @FXML private ImageView pengaturanIcon;
    @FXML private ImageView notifikasiIcon;

    // FXML BINDINGS - HEADER
    @FXML private Circle onlineIndicator;
    @FXML private Label headerUserName;
    @FXML private Label headerUserRole;
    @FXML private Label userAvatarInitial;

    // ICON IMAGES
    private Image iconDashboardDark, iconDashboardLight;
    private Image kelolaPesananIconDark, kelolaPesananIconLight;
    private Image pembayaranIconDark, pembayaranIconLight;
    private Image manajemenUserIconDark, manajemenUserIconLight;
    private Image pengaturanIconDark, pengaturanIconLight;
    private Image notifikasiIconDark, notifikasiIconLight;

    // NAVIGATION STATE
    private final Map<String, Node> panes = new HashMap<>();
    private final Map<String, Object> controllers = new HashMap<>();
    private Node activePane = null;
    private Label activeButton = null;

    // INITIALIZATION
    @Override
    public void initialize(URL location, ResourceBundle resource) {
        // Load semua icon
        loadIcons();

        // Setup navigasi
        setupNavigationStyles();

        // Display user info di header
        displayUserInfo();

        // Set online indicator
        setupOnlineIndicator();

        // Load halaman dashboard sebagai default
        loadPane("DashboardPane.fxml");
        setActiveButton(btnDashboard);
    }

    /**
     * Menampilkan informasi user yang sedang login di header
     */
    private void displayUserInfo() {
        SessionManager session = SessionManager.getInstance();

        if (session.isLoggedIn()) {
            User currentUser = session.getCurrentUser();
            String roleName = getRoleName(currentUser.getIdRole());

            // Update header labels
            if (headerUserName != null) {
                headerUserName.setText(currentUser.getNamaLengkap());
            }
            if (headerUserRole != null) {
                headerUserRole.setText(roleName);
            }
            if (userAvatarInitial != null) {
                // Ambil initial dari nama
                String initial = currentUser.getNamaLengkap().substring(0, 1).toUpperCase();
                userAvatarInitial.setText(initial);
            }
        } else {
            // Default jika tidak ada session
            if (headerUserName != null) headerUserName.setText("Administrator");
            if (headerUserRole != null) headerUserRole.setText("Administrator");
            if (userAvatarInitial != null) userAvatarInitial.setText("A");
        }
    }

    /**
     * Setup online indicator (hijau = online)
     */
    private void setupOnlineIndicator() {
        if (onlineIndicator != null) {
            onlineIndicator.setFill(Color.web("#27ae60"));
        }
    }

    /**
     * Mendapatkan nama role berdasarkan id_role
     */
    private String getRoleName(int roleId) {
        return switch (roleId) {
            case 1 -> "Administrator";
            case 2 -> "Designer";
            case 3 -> "Manager";
            case 4 -> "Operator Produksi";
            default -> "Unknown";
        };
    }

    /**
     * Load semua icon untuk navigasi
     */
    private void loadIcons() {
        try {
            iconDashboardDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/dashboardDark.png"));
            iconDashboardLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/dashboardLight.png"));
            kelolaPesananIconDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/shopping-cartDark.png"));
            kelolaPesananIconLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/shopping-cartLight.png"));
            pembayaranIconDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/credit-cardDark.png"));
            pembayaranIconLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/credit-cardLight.png"));
            manajemenUserIconDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/usersDark.png"));
            manajemenUserIconLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/usersLight.png"));
            pengaturanIconDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/settingsDark.png"));
            pengaturanIconLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/settingsLight.png"));
            notifikasiIconDark = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/bellDark.png"));
            notifikasiIconLight = new Image(getClass().getResourceAsStream("/com/example/trying3/pictures/bellLight.png"));
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup hover effects untuk navigasi
     */
    private void setupNavigationStyles() {
        Label[] navButtons = {btnDashboard, btnKelolaPesanan, btnPembayaran, btnManagementUser, btnPengaturan, btnNotifikasi};
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

        // Style khusus untuk logout
        if (btnLogout != null) {
            btnLogout.setOnMouseEntered(e -> btnLogout.getStyleClass().add("logout-hover"));
            btnLogout.setOnMouseExited(e -> btnLogout.getStyleClass().remove("logout-hover"));
        }
    }

    // PANE LOADING
    /**
     * Load panel secara dinamis dengan caching
     */
    private void loadPane(String fxmlFile) {
        try {
            // Sembunyikan panel yang aktif
            if (activePane != null) {
                activePane.setVisible(false);
            }

            // Cek cache
            if (panes.containsKey(fxmlFile)) {
                activePane = panes.get(fxmlFile);
                activePane.setVisible(true);

                // Refresh data jika DashboardPane
                if ("DashboardPane.fxml".equals(fxmlFile)) {
                    Object controller = controllers.get(fxmlFile);
                    if (controller instanceof DashboardPaneController dashboardController) {
                        dashboardController.refresh();
                    }
                }
            } else {
                // Load dari FXML
                String relativePath = "fxml/admin/" + fxmlFile;
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));

                if (loader.getLocation() == null) {
                    throw new IOException("Cannot find FXML file: " + relativePath);
                }

                activePane = loader.load();

                // Simpan ke cache
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
     * Set tombol aktif dan update icon
     */
    private void setActiveButton(Label button) {
        // Reset semua tombol
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }

        // Reset semua icon ke dark
        resetAllIcons();

        // Set active button
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("active");
        }

        // Update icon untuk tombol aktif
        updateActiveIcon(button);
    }

    /**
     * Reset semua icon ke versi dark
     */
    private void resetAllIcons() {
        if (dashboardIcon != null && iconDashboardDark != null) dashboardIcon.setImage(iconDashboardDark);
        if (kelolaPesananIcon != null && kelolaPesananIconDark != null) kelolaPesananIcon.setImage(kelolaPesananIconDark);
        if (pembayaranIcon != null && pembayaranIconDark != null) pembayaranIcon.setImage(pembayaranIconDark);
        if (manajemenUserIcon != null && manajemenUserIconDark != null) manajemenUserIcon.setImage(manajemenUserIconDark);
        if (pengaturanIcon != null && pengaturanIconDark != null) pengaturanIcon.setImage(pengaturanIconDark);
        if (notifikasiIcon != null && notifikasiIconDark != null) notifikasiIcon.setImage(notifikasiIconDark);
    }

    /**
     * Update icon tombol aktif ke versi light
     */
    private void updateActiveIcon(Label button) {
        if (button == btnDashboard && dashboardIcon != null && iconDashboardLight != null) {
            dashboardIcon.setImage(iconDashboardLight);
        } else if (button == btnKelolaPesanan && kelolaPesananIcon != null && kelolaPesananIconLight != null) {
            kelolaPesananIcon.setImage(kelolaPesananIconLight);
        } else if (button == btnPembayaran && pembayaranIcon != null && pembayaranIconLight != null) {
            pembayaranIcon.setImage(pembayaranIconLight);
        } else if (button == btnManagementUser && manajemenUserIcon != null && manajemenUserIconLight != null) {
            manajemenUserIcon.setImage(manajemenUserIconLight);
        } else if (button == btnPengaturan && pengaturanIcon != null && pengaturanIconLight != null) {
            pengaturanIcon.setImage(pengaturanIconLight);
        } else if (button == btnNotifikasi && notifikasiIcon != null && notifikasiIconLight != null) {
            notifikasiIcon.setImage(notifikasiIconLight);
        }
    }

    /**
     * Menampilkan alert error
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // EVENT HANDLERS
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
    private void handlePengaturanClick() {
        loadPane("SettingsPane.fxml");
        setActiveButton(btnPengaturan);
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
                // Clear session
                SessionManager.getInstance().clearSession();
                System.exit(0);
            }
        });
    }
}