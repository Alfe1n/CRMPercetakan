package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.AlertUtil;
import com.example.trying3.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller untuk halaman Kelola Pembayaran
 * Menggunakan PesananPendingCell.java (file terpisah) dengan callback pattern
 */
public class KelolaPembayaranController {

    // ========== STATISTICS ==========
    @FXML
    private Label menungguVerifikasiLabel;
    @FXML
    private Label terverifikasiLabel;
    @FXML
    private Label totalPendapatanLabel;

    // ========== PENDING LIST ==========
    @FXML
    private Label pendingCountLabel;
    @FXML
    private Label pendingSubtitle;
    @FXML
    private ListView<Pesanan> pendingListView;
    @FXML
    private Button refreshButton;

    // ========== RIWAYAT LIST ==========
    @FXML
    private ListView<Pesanan> riwayatListView;

    // ========== DATA ==========
    private ObservableList<Pesanan> pendingList = FXCollections.observableArrayList();
    private ObservableList<Pesanan> riwayatList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("✅ KelolaPembayaranController initialized");

        setupLists();
        setupButtons();
        Platform.runLater(() -> loadData());
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        if (pendingListView.getParent() != null) {
            pendingListView.getParent().visibleProperty().addListener((obs, wasVisible, isNowVisible) -> {
                if (isNowVisible) {
                    System.out.println("🔄 Pane visible, refreshing data...");
                    loadData();
                }
            });
        }
    }

    // =====================================================
    // SETUP METHODS
    // =====================================================

    private void setupLists() {
        // Setup Pending ListView - Menggunakan file terpisah dengan CALLBACK
        pendingListView.setCellFactory(param -> new PesananPendingCell(
            new PesananPendingCell.PembayaranCallback() {
                @Override
                public void onVerifikasi(Pesanan pesanan, String nominal, String metode) {
                    handleVerifikasi(pesanan, nominal, metode);
                }

                @Override
                public void onTolak(Pesanan pesanan) {
                    handleTolak(pesanan);
                }

                @Override
                public void loadMetodePembayaran(ComboBox<String> comboBox) {
                    KelolaPembayaranController.this.loadMetodePembayaran(comboBox);
                }
            }
        ));
        pendingListView.setItems(pendingList);

        // Setup Riwayat ListView
        riwayatListView.setCellFactory(param -> new PembayaranRiwayatCell());
        riwayatListView.setItems(riwayatList);
    }

    private void setupButtons() {
        refreshButton.setOnAction(e -> loadData());
    }

    // =====================================================
    // DATA LOADING
    // =====================================================

    private void loadData() {
        loadStatistics();
        loadPendingPesanan();
        loadRiwayatPembayaran();
    }

    private void loadStatistics() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Menunggu Verifikasi (pesanan dengan status id = 2)
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) as total FROM pesanan WHERE id_status = 2"
            );
            if (rs.next()) {
                menungguVerifikasiLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Terverifikasi (pembayaran yang sudah verified)
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) as total FROM pembayaran WHERE status_pembayaran = 'verified'"
            );
            if (rs.next()) {
                terverifikasiLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Total Pendapatan
            rs = stmt.executeQuery(
                    "SELECT COALESCE(SUM(jumlah), 0) as total FROM pembayaran WHERE status_pembayaran = 'verified'"
            );
            if (rs.next()) {
                double total = rs.getDouble("total");
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                totalPendapatanLabel.setText(currencyFormat.format(total));
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat statistik pembayaran.");
        }
    }

    private void loadPendingPesanan() {
        pendingList.clear();

        // Load pesanan dengan status Menunggu Pembayaran (id_status = 2)
        String query =
                "SELECT p.*, pl.nama as nama_pelanggan, " +
                        "dp.jumlah, dp.subtotal, jl.nama_layanan " +
                        "FROM pesanan p " +
                        "JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan " +
                        "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                        "LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                        "WHERE p.id_status = 2 " +
                        "ORDER BY p.tanggal_pesanan DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Pesanan pesanan = new Pesanan();
                pesanan.setIdPesanan(rs.getInt("id_pesanan"));
                pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));
                pesanan.setJenisLayanan(rs.getString("nama_layanan"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalBiaya(rs.getDouble("total_biaya"));
                pesanan.setTanggalPesanan(rs.getTimestamp("tanggal_pesanan").toLocalDateTime());

                pendingList.add(pesanan);
            }

            pendingCountLabel.setText("(" + pendingList.size() + " pesanan)");
            pendingSubtitle.setText(pendingList.size() + " pembayaran menunggu verifikasi");

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data pesanan pending.");
        }
    }

    private void loadRiwayatPembayaran() {
        riwayatList.clear();

        // Load pembayaran yang sudah verified
        String query =
                "SELECT pb.*, p.nomor_pesanan, pl.nama as nama_pelanggan, " +
                        "mp.nama_metode, dp.jumlah, jl.nama_layanan " +
                        "FROM pembayaran pb " +
                        "JOIN pesanan p ON pb.id_pesanan = p.id_pesanan " +
                        "JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan " +
                        "JOIN metode_pembayaran mp ON pb.id_metode = mp.id_metode " +
                        "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                        "LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan " +
                        "WHERE pb.status_pembayaran = 'verified' " +
                        "ORDER BY pb.tanggal_verifikasi DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Pesanan pesanan = new Pesanan();
                pesanan.setIdPesanan(rs.getInt("id_pesanan"));
                pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));
                pesanan.setJenisLayanan(rs.getString("nama_layanan"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalBiaya(rs.getDouble("jumlah")); // jumlah dari pembayaran
                pesanan.setSpesifikasi(rs.getString("nama_metode")); // Temp use for metode

                Timestamp verifikasi = rs.getTimestamp("tanggal_verifikasi");
                if (verifikasi != null) {
                    pesanan.setUpdatedAt(verifikasi.toLocalDateTime());
                }

                riwayatList.add(pesanan);
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat riwayat pembayaran.");
        }
    }

    // =====================================================
    // PAYMENT ACTIONS - Dipanggil dari Cell via Callback
    // =====================================================

    private void handleVerifikasi(Pesanan pesanan, String nominalStr, String metode) {
        if (nominalStr == null || nominalStr.trim().isEmpty() || metode == null) {
            AlertUtil.showWarning("Peringatan", "Mohon isi nominal dan pilih metode pembayaran.");
            return;
        }

        try {
            double nominal = Double.parseDouble(nominalStr);

            // Konfirmasi
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Konfirmasi Verifikasi");
            alert.setHeaderText("Verifikasi pembayaran pesanan ORD-" + String.format("%03d", pesanan.getIdPesanan()));
            alert.setContentText(
                    "Pelanggan: " + pesanan.getNamaPelanggan() + "\n" +
                            "Nominal: Rp " + String.format("%,.0f", nominal) + "\n" +
                            "Metode: " + metode + "\n\n" +
                            "Yakin ingin memverifikasi pembayaran ini?"
            );

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                verifikasiPembayaran(pesanan.getIdPesanan(), nominal, metode);
            }

        } catch (NumberFormatException e) {
            AlertUtil.showError("Nominal tidak valid.");
        }
    }

    private void handleTolak(Pesanan pesanan) {
        // Konfirmasi
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Penolakan");
        alert.setHeaderText("Tolak pembayaran pesanan ORD-" + String.format("%03d", pesanan.getIdPesanan()));
        alert.setContentText(
                "Pelanggan: " + pesanan.getNamaPelanggan() + "\n\n" +
                        "Yakin ingin menolak pembayaran ini?\n" +
                        "Status pesanan akan dikembalikan ke 'Pending'."
        );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            tolakPembayaran(pesanan.getIdPesanan());
        }
    }

    // =====================================================
    // DATABASE OPERATIONS
    // =====================================================

    private void verifikasiPembayaran(int idPesanan, double nominal, String namaMetode) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Get metode ID
            int idMetode = getMetodeIdByName(conn, namaMetode);

            // Get admin ID
            int idAdmin = SessionManager.getInstance().getCurrentUser().getIdUser();

            // 1. Insert into pembayaran
            String insertPembayaran =
                    "INSERT INTO pembayaran (id_pesanan, id_metode, jumlah, tanggal_pembayaran, " +
                            "status_pembayaran, verified_by, tanggal_verifikasi) " +
                            "VALUES (?, ?, ?, NOW(), 'verified', ?, NOW())";

            try (PreparedStatement ps = conn.prepareStatement(insertPembayaran)) {
                ps.setInt(1, idPesanan);
                ps.setInt(2, idMetode);
                ps.setDouble(3, nominal);
                ps.setInt(4, idAdmin);
                ps.executeUpdate();
            }

            // 2. Update status pesanan menjadi Sedang Dikerjakan (id_status = 3)
            String updatePesanan = "UPDATE pesanan SET id_status = 3, updated_at = NOW() WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(updatePesanan)) {
                ps.setInt(1, idPesanan);
                ps.executeUpdate();
            }

            conn.commit();

            AlertUtil.showSuccess("Sukses","Pembayaran berhasil diverifikasi!\nPesanan akan diproses.");
            loadData(); // Refresh

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            AlertUtil.showError("Gagal memverifikasi pembayaran: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tolakPembayaran(int idPesanan) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Update status pesanan kembali ke Pending (id_status = 1)
            String updatePesanan = "UPDATE pesanan SET id_status = 1, updated_at = NOW() WHERE id_pesanan = ?";
            try (PreparedStatement ps = conn.prepareStatement(updatePesanan)) {
                ps.setInt(1, idPesanan);
                ps.executeUpdate();
            }

            conn.commit();

            AlertUtil.showInfo("Info", "Pembayaran ditolak.\nStatus pesanan dikembalikan ke Pending.");
            loadData(); // Refresh

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            AlertUtil.showError("Gagal menolak pembayaran: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private void loadMetodePembayaran(ComboBox<String> comboBox) {
        ObservableList<String> metodeList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT nama_metode FROM metode_pembayaran WHERE is_active = 1")) {

            while (rs.next()) {
                metodeList.add(rs.getString("nama_metode"));
            }
            comboBox.setItems(metodeList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getMetodeIdByName(Connection conn, String namaMetode) throws SQLException {
        String sql = "SELECT id_metode FROM metode_pembayaran WHERE nama_metode = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaMetode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_metode");
        }
        return 1; // Default
    }
}
