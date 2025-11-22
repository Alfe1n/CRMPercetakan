package com.example.trying3.controller.admin;

import com.example.trying3.MainApplication;
import com.example.trying3.model.Order;
import com.example.trying3.model.User;
import com.example.trying3.util.OrderItemCell;
import com.example.trying3.dao.UserDAO;
import com.example.trying3.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    // --- Elemen FXML dari Dashboard.fxml ---
    @FXML private Label btnDashboard;
    @FXML private Label btnKelolaPesanan;
    @FXML private Label btnPembayaran;
    @FXML private Label btnManagementUser;
    @FXML private Label btnPengaturan;
    @FXML private Label btnNotifikasi;
    @FXML private Label btnLogout;
    @FXML private StackPane contentArea;
    @FXML private ImageView dashboardIcon;
    @FXML private ImageView kelolaPesananIcon;
    @FXML private ImageView pembayaranIcon;
    @FXML private ImageView manajemenUserIcon;
    @FXML private ImageView pengaturanIcon;
    @FXML private ImageView notifikasiIcon;

    // Siapkan objek Image di awal agar tidak perlu load berulang kali
    private Image iconDashboardDark;
    private Image iconDashboardLight;
    private Image kelolaPesananIconDark;
    private Image kelolaPesananIconLight;
    private Image pembayaranIconDark;
    private Image pembayaranIconLight;
    private Image manajemenUserIconDark;
    private Image manajemenUserIconLight;
    private Image pengaturanIconDark;
    private Image pengaturanIconLight;
    private Image notifikasiIconDark;
    private Image notifikasiIconLight;

    // --- Kontrol untuk Elemen di dalam DashboardPane.fxml ---
    // (Ini diakses setelah pane di-load)
    @FXML private Label headerTitle;
    private Label totalPesananLabel;
    private ListView<Order> recentOrdersListView;

    // --- DAO dan Data ---
    private UserDAO userDAO;
    private final ObservableList<Order> orderList = FXCollections.observableArrayList();

    // --- Cache untuk Pane (Kunci Performa) ---
    private final Map<String, Node> panes = new HashMap<>();
    private Node activePane = null;
    private Label activeButton = null;


    @Override
    public void initialize(URL location, ResourceBundle resource) {
        userDAO = new UserDAO();

        imageLoad();

        setupNavigationStyles();

        // Muat halaman dashboard sebagai default saat pertama kali buka
        loadPane("DashboardPane.fxml");
        setActiveButton(btnDashboard);
    }

    private void displayUserInfo() {
        SessionManager session = SessionManager.getInstance();

        if (session.isLoggedIn()) {
            User currentUser = session.getCurrentUser();
            String roleName = getRoleName(currentUser.getIdRole());

            // Update header label (tambahkan ke FXML dulu)
            System.out.println("Welcome: " + currentUser.getNamaLengkap() +
                    " (" + roleName + ")");
        }
    }

    private String getRoleName(int roleId) {
        return switch (roleId) {
            case 1 -> "Administrator";
            case 2 -> "Designer";
            case 3 -> "Operator Produksi";
            case 4 -> "Manager";
            default -> "Unknown";
        };
    }

    private void imageLoad() {
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
     * Mengatur gaya awal dan efek hover untuk tombol navigasi.
     */
    private void setupNavigationStyles() {
        Label[] navButtons = {btnDashboard, btnKelolaPesanan, btnPembayaran, btnManagementUser, btnPengaturan, btnNotifikasi};
        for (Label button : navButtons) {
            button.setOnMouseEntered(e -> {
                if (button != activeButton) {
                    button.getStyleClass().add("hover");
                }
            });
            button.setOnMouseExited(e -> button.getStyleClass().remove("hover"));
        }

        // Style khusus untuk logout
        btnLogout.setOnMouseEntered(e -> btnLogout.getStyleClass().add("logout-hover"));
        btnLogout.setOnMouseExited(e -> btnLogout.getStyleClass().remove("logout-hover"));
    }

    /**
     * Logika utama untuk memuat panel secara dinamis (Lazy Loading).
     * Panel hanya akan dimuat dari FXML satu kali dan disimpan di cache.
     */
    private void loadPane(String fxmlFile) {
        try {
            // Sembunyikan panel yang aktif saat ini
            if (activePane != null) {
                activePane.setVisible(false);
            }

            // Cek apakah panel sudah ada di cache
            if (panes.containsKey(fxmlFile)) {
                activePane = panes.get(fxmlFile);
                activePane.setVisible(true);
            } else {
                // Jika belum, load dari FXML
                String relativePath = "fxml/admin/" + fxmlFile;
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(relativePath));

                if (loader.getLocation() == null) {
                    throw new IOException("Cannot find FXML file: " + relativePath);
                }

                activePane = loader.load();
                panes.put(fxmlFile, activePane);
                contentArea.getChildren().add(activePane);

                // Jika yang di-load adalah dashboard, inisialisasi komponennya
                if ("DashboardPane.fxml".equals(fxmlFile)) {
                    initializeDashboardComponents(loader);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Memuat Halaman");
            alert.setHeaderText("Gagal memuat file: " + fxmlFile);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            // Tampilkan error di UI jika perlu
        }
    }

    /**
     * Mengambil referensi kontrol dari DashboardPane.fxml dan memuat datanya.
     */
    private void initializeDashboardComponents(FXMLLoader loader) {
        // Ambil referensi kontrol dari FXML yang baru di-load
        totalPesananLabel = (Label) loader.getNamespace().get("totalPesananLabel");
        recentOrdersListView = (ListView<Order>) loader.getNamespace().get("recentOrdersListView");

        // Konfigurasi ListView agar efisien
        setupRecentOrdersListView();

        // Memuat data statistik dan daftar pesanan
        loadDashboardStatistics();
        loadRecentOrders();
    }

    /**
     * Mengatur CellFactory untuk ListView.
     * Ini adalah kunci performa agar ListView hanya merender item yang terlihat.
     */
    private void setupRecentOrdersListView() {
        if(recentOrdersListView != null) {
            recentOrdersListView.setItems(orderList);
            recentOrdersListView.setCellFactory(listView -> new OrderItemCell());
        }
    }


    /**
     * Memuat data statistik (seperti total pesanan) secara asynchronous.
     */
    private void loadDashboardStatistics() {
        Task<Integer> statsTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                Thread.sleep(500); // Hapus ini jika sudah pakai database asli
                return userDAO.getTotalUsers(); // Ganti dengan method yang sesuai
            }
        };

        statsTask.setOnSucceeded(e -> {
            if (totalPesananLabel != null) {
                totalPesananLabel.setText(String.valueOf(statsTask.getValue()));
            }
        });

        statsTask.setOnFailed(e -> {
            if (totalPesananLabel != null) {
                totalPesananLabel.setText("0");
            }
            statsTask.getException().printStackTrace();
        });

        new Thread(statsTask).start();
    }

    /**
     * Memuat data pesanan terbaru (mock data untuk contoh).
     * Ganti dengan data dari database Anda.
     */
    private void loadRecentOrders() {
        // Ini adalah data contoh, ganti dengan data dari database Anda
        orderList.clear();
        orderList.add(new Order("PT. Maju Jaya", "Digital Printing", "Proses Desain", "#3498db"));
        orderList.add(new Order("Toko Berkah", "Sablon", "Produksi", "#f1c40f"));
        orderList.add(new Order("CV. Sukses Mandiri", "Cetak Undangan", "Menunggu Konfirmasi", "#e67e22"));
        orderList.add(new Order("Sekolah Harapan", "Offset Printing", "Selesai", "#2ecc71"));
        orderList.add(new Order("Warung Kopi Senja", "Cetak Stiker", "Dibatalkan", "#e74c3c"));
        orderList.add(new Order("Universitas Teknologi", "Jilid & Fotokopi", "Selesai", "#2ecc71"));
    }

    private void setActiveButton(Label button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
            if (dashboardIcon != null && iconDashboardDark != null) {
                dashboardIcon.setImage(iconDashboardDark);
            }
            if (kelolaPesananIcon != null && kelolaPesananIconDark != null) {
                kelolaPesananIcon.setImage(kelolaPesananIconDark);
            }
            if (pembayaranIcon != null && pembayaranIconDark != null) {
                pembayaranIcon.setImage(pembayaranIconDark);
            }
            if (manajemenUserIcon != null && manajemenUserIconDark != null) {
                manajemenUserIcon.setImage(manajemenUserIconDark);
            }
            if (pengaturanIcon != null && pengaturanIconDark != null) {
                pengaturanIcon.setImage(pengaturanIconDark);
            }
            if (notifikasiIcon != null && notifikasiIconDark != null) {
                notifikasiIcon.setImage(notifikasiIconDark);
            }
        }

        // btnActive - Dashboard
        if (button == btnDashboard && dashboardIcon != null && iconDashboardLight != null) {
            dashboardIcon.setImage(iconDashboardLight);
        }

        // btnActive - Kelola Pesanan
        if (button == btnKelolaPesanan && kelolaPesananIcon != null && kelolaPesananIconLight != null){
            kelolaPesananIcon.setImage(kelolaPesananIconLight);
        }

        // btnActive - Pembayaran
        if (button == btnPembayaran && pembayaranIcon != null && pembayaranIconLight != null){
            pembayaranIcon.setImage(pembayaranIconLight);
        }

        // btnActive - Manajemen User
        if (button == btnManagementUser && manajemenUserIcon != null && manajemenUserIconLight != null){
            manajemenUserIcon.setImage(manajemenUserIconLight);
        }

        // btnActive - Pengaturan
        if (button == btnPengaturan && pengaturanIcon != null && pengaturanIconLight != null){
            pengaturanIcon.setImage(pengaturanIconLight);
        }

        // btnActive - Notifikasi
        if (button == btnNotifikasi && notifikasiIcon != null && notifikasiIconLight != null){
            notifikasiIcon.setImage(notifikasiIconLight);
        }

        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    // --- Event Handlers ---

    @FXML private void handleDashboardClick() {
        loadPane("DashboardPane.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML private void handleKelolaPesananClick() {
        loadPane("KelolaPesananPane.fxml");
        setActiveButton(btnKelolaPesanan);
    }

    @FXML private void handlePembayaranClick() {
        loadPane("SettingsPane.fxml");
        setActiveButton(btnPembayaran);
    }

    @FXML private void handleManajemenUserClick() {
        setActiveButton(btnManagementUser);
    }

    @FXML private void handlePengaturanClick() {
        setActiveButton(btnPengaturan);
    }

    @FXML private void handleNotifikasiClick() {
        setActiveButton(btnNotifikasi);
    }

    @FXML
    private void handleLogoutClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Apakah Anda yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Logout");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                System.out.println("User logged out.");
                System.exit(0);
            }
        });
    }

}
