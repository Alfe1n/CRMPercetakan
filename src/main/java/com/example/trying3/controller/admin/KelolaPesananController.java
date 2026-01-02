package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import com.example.trying3.service.PembayaranService;
import com.example.trying3.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class KelolaPesananController {

    // FXML Components - Panel Navigation
    @FXML private VBox daftarPesananPanel;
    @FXML private VBox formTambahPanel;
    @FXML private VBox formEditPanel;

    // FXML Components - Daftar Pesanan
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Button refreshButton;
    @FXML private Button btnTambahPesanan;
    @FXML private ListView<Pesanan> pesananListView;

    // FXML Components - Form Tambah Pesanan
    @FXML private TextField namaPelangganField;
    @FXML private TextField noTeleponField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> jenisLayananComboBox;
    @FXML private TextField jumlahField;
    @FXML private TextField totalHargaField;
    @FXML private TextArea alamatField;
    @FXML private TextArea spesifikasiArea;
    @FXML private Button simpanPesananButton;
    @FXML private Button batalTambahButton;

    // FXML Components - Form Edit Pesanan
    @FXML private Label editIdPesananLabel;
    @FXML private Label editSubtitleLabel;
    @FXML private TextField editNamaPelangganField;
    @FXML private TextField editNoTeleponField;
    @FXML private TextField editEmailField;
    @FXML private TextArea editAlamatField;
    @FXML private ComboBox<String> editJenisLayananComboBox;
    @FXML private TextField editJumlahField;
    @FXML private TextField editTotalHargaField;
    @FXML private TextArea editSpesifikasiArea;
    @FXML private Button updatePesananButton;
    @FXML private Button batalEditButton;

    // Data sources and services
    private PesananDAO pesananDAO;
    private PembayaranService pembayaranService;
    private final ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private Pesanan currentEditPesanan;

    @FXML
    public void initialize() {
        pesananDAO = new PesananDAO();
        pembayaranService = new PembayaranService();
        setupDaftarPesanan();
        setupFormTambah();
        setupFormEdit();
        loadJenisLayanan();
        loadPesananData();
        showDaftarPanel();
    }

    /**
     * Menampilkan panel daftar pesanan
     */
    private void showDaftarPanel() {
        daftarPesananPanel.setVisible(true);
        daftarPesananPanel.setManaged(true);
        formTambahPanel.setVisible(false);
        formTambahPanel.setManaged(false);
        formEditPanel.setVisible(false);
        formEditPanel.setManaged(false);
    }

    /**
     * Menampilkan panel form tambah pesanan
     */
    private void showTambahPanel() {
        daftarPesananPanel.setVisible(false);
        daftarPesananPanel.setManaged(false);
        formTambahPanel.setVisible(true);
        formTambahPanel.setManaged(true);
        formEditPanel.setVisible(false);
        formEditPanel.setManaged(false);
        clearFormTambah();
    }

    /**
     * Menampilkan panel form edit pesanan
     */
    private void showEditPanel() {
        daftarPesananPanel.setVisible(false);
        daftarPesananPanel.setManaged(false);
        formTambahPanel.setVisible(false);
        formTambahPanel.setManaged(false);
        formEditPanel.setVisible(true);
        formEditPanel.setManaged(true);
    }

    /**
     * Konfigurasi ListView dan filters untuk daftar pesanan
     */
    private void setupDaftarPesanan() {
        pesananListView.setCellFactory(param -> new PesananCardCell(
                this::handleEditPesanan,
                this::handleDeletePesanan,
                this::handleKonfirmasiPesanan,
                this::handleUbahStatus));

        pesananListView.setItems(pesananList);

        setupFilters();
        btnTambahPesanan.setOnAction(e -> showTambahPanel());
    }

    /**
     * Konfigurasi filter status dan pencarian
     */
    private void setupFilters() {
        ObservableList<String> statusOptions = FXCollections.observableArrayList();
        statusOptions.add("Semua Status");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_status FROM status_pesanan ORDER BY id_status")) {
            while (rs.next()) {
                statusOptions.add(rs.getString("nama_status"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        filterStatusComboBox.setItems(statusOptions);
        filterStatusComboBox.setValue("Semua Status");

        searchField.textProperty().addListener((obs, old, newVal) -> loadPesananData());
        filterStatusComboBox.valueProperty().addListener((obs, old, newVal) -> loadPesananData());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            filterStatusComboBox.setValue("Semua Status");
            loadPesananData();
        });
    }

    /**
     * Handler perubahan status pesanan dari dropdown
     */
    private void handleUbahStatus(Pesanan pesanan, String statusBaru) {
        String statusLama = pesanan.getStatus();

        boolean confirmed = AlertUtil.showConfirmation(
                "Ubah Status Pesanan",
                "Ubah status pesanan dari " + pesanan.getNamaPelanggan() +
                        "\ndari '" + statusLama + "' ke '" + statusBaru + "'?");

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), statusBaru);
        }
    }

    /**
     * Memuat data pesanan dari database dengan filter
     */
    private void loadPesananData() {
        pesananListView.getSelectionModel().clearSelection();
        pesananList.clear();

        StringBuilder query = new StringBuilder(
                "SELECT p.*, " +
                        "pel.nama, pel.no_telepon, pel.email, pel.alamat, " +
                        "sp.nama_status, " +
                        "dp.id_layanan, dp.jumlah, dp.subtotal, dp.spesifikasi " +
                        "FROM pesanan p " +
                        "JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan " +
                        "JOIN status_pesanan sp ON p.id_status = sp.id_status " +
                        "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                        "WHERE 1=1");

        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            query.append(" AND (pel.nama LIKE '%").append(searchText).append("%'");
            query.append(" OR pel.no_telepon LIKE '%").append(searchText).append("%')");
        }

        String statusFilter = filterStatusComboBox.getValue();
        if (statusFilter != null && !statusFilter.equals("Semua Status")) {
            query.append(" AND sp.nama_status = '").append(statusFilter).append("'");
        }

        query.append(" ORDER BY p.tanggal_pesanan DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.toString())) {

            while (rs.next()) {
                Pesanan pesanan = new Pesanan();
                pesanan.setIdPesanan(rs.getInt("id_pesanan"));
                pesanan.setNamaPelanggan(rs.getString("nama"));
                pesanan.setNoTelepon(rs.getString("no_telepon"));
                pesanan.setEmail(rs.getString("email"));
                pesanan.setAlamat(rs.getString("alamat"));
                pesanan.setStatus(rs.getString("nama_status"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalBiaya(rs.getDouble("total_biaya"));
                pesanan.setSpesifikasi(rs.getString("spesifikasi"));
                pesanan.setTanggalPesanan(rs.getTimestamp("tanggal_pesanan").toLocalDateTime());

                int idLayanan = rs.getInt("id_layanan");
                pesanan.setJenisLayanan(getLayananName(idLayanan));

                pesananList.add(pesanan);
            }

            resultCountLabel.setText("(" + pesananList.size() + " pesanan)");

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data pesanan.");
        }
    }

    /**
     * Konfigurasi form tambah pesanan
     */
    private void setupFormTambah() {
        simpanPesananButton.setOnAction(e -> simpanPesanan());
        batalTambahButton.setOnAction(e -> {
            clearFormTambah();
            showDaftarPanel();
        });

        forceNumericInput(jumlahField);
        forceNumericInput(totalHargaField);
        forceNumericInput(noTeleponField);
    }

    /**
     * Memuat daftar layanan dari database ke ComboBox
     */
    private void loadJenisLayanan() {
        ObservableList<String> layananList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_layanan FROM jenis_layanan WHERE is_active = 1")) {

            while (rs.next()) {
                layananList.add(rs.getString("nama_layanan"));
            }

            jenisLayananComboBox.setItems(layananList);
            editJenisLayananComboBox.setItems(layananList);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data layanan.");
        }
    }

    /**
     * Proses simpan pesanan baru ke database
     */
    private void simpanPesanan() {
        String namaPelanggan = namaPelangganField.getText().trim();
        String noTelepon = noTeleponField.getText().trim();
        String email = emailField.getText().trim();
        String alamat = alamatField.getText().trim();
        String jenisLayanan = jenisLayananComboBox.getValue();
        String jumlahStr = jumlahField.getText().trim();
        String totalHargaStr = totalHargaField.getText().trim();
        String spesifikasi = spesifikasiArea.getText().trim();

        // Validasi input kosong
        if (namaPelanggan.isEmpty() || noTelepon.isEmpty() || alamat.isEmpty() || jenisLayanan == null ||
                jumlahStr.isEmpty() || totalHargaStr.isEmpty()) {
            AlertUtil.showWarning("Data Tidak Lengkap", "Mohon isi semua field wajib (bertanda *).");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            double totalHarga = Double.parseDouble(totalHargaStr);

            boolean isSuccess = pesananDAO.createPesanan(
                    namaPelanggan, noTelepon, email, alamat, jenisLayanan, jumlah, totalHarga, spesifikasi);

            if (isSuccess) {
                AlertUtil.showSuccess("Berhasil", "Pesanan berhasil dibuat!");
                clearFormTambah();
                showDaftarPanel();
                loadPesananData();
            } else {
                AlertUtil.showError("Gagal menyimpan pesanan ke database.");
            }

        } catch (NumberFormatException e) {
            AlertUtil.showError("Format Salah", "Jumlah dan Harga harus berupa angka valid.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error: " + e.getMessage());
        }
    }

    private void clearFormTambah() {
        namaPelangganField.clear();
        noTeleponField.clear();
        emailField.clear();
        alamatField.clear();
        jenisLayananComboBox.getSelectionModel().clearSelection();
        jumlahField.clear();
        totalHargaField.clear();
        spesifikasiArea.clear();
    }

    /**
     * Konfigurasi form edit pesanan
     */
    private void setupFormEdit() {
        updatePesananButton.setOnAction(e -> updatePesanan());
        batalEditButton.setOnAction(e -> {
            clearFormEdit();
            showDaftarPanel();
        });

        forceNumericInput(editJumlahField);
        forceNumericInput(editTotalHargaField);
        forceNumericInput(editNoTeleponField);
    }

    /**
     * Memuat data pesanan ke form edit
     */
    private void loadDataToEditForm(Pesanan pesanan) {
        currentEditPesanan = pesanan;

        editIdPesananLabel.setText(String.valueOf(pesanan.getIdPesanan()));
        editSubtitleLabel.setText("Edit pesanan #" + pesanan.getIdPesanan() + " - " + pesanan.getNamaPelanggan());

        editNamaPelangganField.setText(pesanan.getNamaPelanggan());
        editNoTeleponField.setText(pesanan.getNoTelepon());
        editEmailField.setText(pesanan.getEmail() != null ? pesanan.getEmail() : "");
        editAlamatField.setText(pesanan.getAlamat() != null ? pesanan.getAlamat() : "");
        editJenisLayananComboBox.setValue(pesanan.getJenisLayanan());
        editJumlahField.setText(String.valueOf(pesanan.getJumlah()));
        editTotalHargaField.setText(String.valueOf((int) pesanan.getTotalBiaya()));
        editSpesifikasiArea.setText(pesanan.getSpesifikasi() != null ? pesanan.getSpesifikasi() : "");

        showEditPanel();
    }

    /**
     * Proses update pesanan ke database (3 tabel: pelanggan, pesanan, detail_pesanan)
     */
    private void updatePesanan() {
        if (currentEditPesanan == null) {
            AlertUtil.showError("Error", "Tidak ada pesanan yang dipilih untuk di-update.");
            return;
        }

        String namaPelanggan = editNamaPelangganField.getText().trim();
        String noTelepon = editNoTeleponField.getText().trim();
        String email = editEmailField.getText().trim();
        String alamat = editAlamatField.getText().trim();
        String jenisLayanan = editJenisLayananComboBox.getValue();
        String jumlahStr = editJumlahField.getText().trim();
        String totalHargaStr = editTotalHargaField.getText().trim();
        String spesifikasi = editSpesifikasiArea.getText().trim();

        if (namaPelanggan.isEmpty() || alamat.isEmpty() || noTelepon.isEmpty() || jenisLayanan == null ||
                jumlahStr.isEmpty() || totalHargaStr.isEmpty()) {
            AlertUtil.showWarning("Data Tidak Lengkap", "Mohon isi semua field wajib.");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            double totalHarga = Double.parseDouble(totalHargaStr);
            int idPesanan = currentEditPesanan.getIdPesanan();

            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try {
                String updatePelangganSql = "UPDATE pelanggan SET nama = ?, no_telepon = ?, email = ?, alamat = ? " +
                        "WHERE id_pelanggan = (SELECT id_pelanggan FROM pesanan WHERE id_pesanan = ?)";
                PreparedStatement psPelanggan = conn.prepareStatement(updatePelangganSql);
                psPelanggan.setString(1, namaPelanggan);
                psPelanggan.setString(2, noTelepon);
                psPelanggan.setString(3, email);
                psPelanggan.setString(4, alamat);
                psPelanggan.setInt(5, idPesanan);
                psPelanggan.executeUpdate();

                String updatePesananSql = "UPDATE pesanan SET total_biaya = ?, catatan = ? WHERE id_pesanan = ?";
                PreparedStatement psPesanan = conn.prepareStatement(updatePesananSql);
                psPesanan.setDouble(1, totalHarga);
                psPesanan.setString(2, spesifikasi);
                psPesanan.setInt(3, idPesanan);
                psPesanan.executeUpdate();

                int idLayanan = getLayananIdByName(conn, jenisLayanan);
                double hargaSatuan = totalHarga / jumlah;

                String updateDetailSql = "UPDATE detail_pesanan SET id_layanan = ?, jumlah = ?, " +
                        "harga_satuan = ?, subtotal = ?, spesifikasi = ? WHERE id_pesanan = ?";
                PreparedStatement psDetail = conn.prepareStatement(updateDetailSql);
                psDetail.setInt(1, idLayanan);
                psDetail.setInt(2, jumlah);
                psDetail.setDouble(3, hargaSatuan);
                psDetail.setDouble(4, totalHarga);
                psDetail.setString(5, spesifikasi);
                psDetail.setInt(6, idPesanan);
                psDetail.executeUpdate();

                conn.commit();

                AlertUtil.showSuccess("Berhasil", "Pesanan berhasil diupdate!");
                clearFormEdit();
                showDaftarPanel();
                loadPesananData();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }

        } catch (NumberFormatException e) {
            AlertUtil.showError("Format Salah", "Jumlah dan Harga harus berupa angka valid.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error: " + e.getMessage());
        }
    }

    private void clearFormEdit() {
        currentEditPesanan = null;
        editNamaPelangganField.clear();
        editNoTeleponField.clear();
        editEmailField.clear();
        editAlamatField.clear();
        editJenisLayananComboBox.getSelectionModel().clearSelection();
        editJumlahField.clear();
        editTotalHargaField.clear();
        editSpesifikasiArea.clear();
    }

    /**
     * Memaksa TextField hanya menerima input angka
     */
    private void forceNumericInput(TextField tf) {
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tf.setText(newValue.replaceAll("\\D", ""));
            }
        });
    }

    /**
     * Mengambil nama layanan berdasarkan ID dari database
     */
    private String getLayananName(int idLayanan) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn
                     .prepareStatement("SELECT nama_layanan FROM jenis_layanan WHERE id_layanan = ?")) {
            pstmt.setInt(1, idLayanan);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nama_layanan");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Mengambil ID layanan berdasarkan nama
     */
    private int getLayananIdByName(Connection conn, String namaLayanan) throws Exception {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("id_layanan");
        }
        return 1;
    }

    /**
     * Handler untuk tombol edit pesanan dari card
     */
    private void handleEditPesanan(Pesanan pesanan) {
        loadDataToEditForm(pesanan);
    }

    /**
     * Handler untuk tombol hapus pesanan dari card
     */
    private void handleDeletePesanan(Pesanan pesanan) {
        boolean confirmed = AlertUtil.showConfirmation(
                "Hapus Pesanan",
                "Apakah Anda yakin ingin menghapus pesanan dari " + pesanan.getNamaPelanggan() + "?");

        if (confirmed) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM pesanan WHERE id_pesanan = ?")) {

                pstmt.setInt(1, pesanan.getIdPesanan());
                int result = pstmt.executeUpdate();

                if (result > 0) {
                    AlertUtil.showSuccess("Berhasil", "Pesanan berhasil dihapus!");
                    loadPesananData();
                } else {
                    AlertUtil.showError("Gagal menghapus pesanan.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler untuk tombol konfirmasi pesanan (ubah status ke Menunggu Pembayaran)
     */
    private void handleKonfirmasiPesanan(Pesanan pesanan) {
        boolean confirmed = AlertUtil.showConfirmation(
                "Konfirmasi Pesanan",
                "Konfirmasi pesanan dari " + pesanan.getNamaPelanggan() +
                        "?\n\nStatus akan diubah ke 'Menunggu Pembayaran'.");

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), "Menunggu Pembayaran");
        }
    }

    /**
     * Update status pesanan di database
     */
    private void updateStatusPesanan(int idPesanan, String namaStatus) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE pesanan SET id_status = (SELECT id_status FROM status_pesanan WHERE nama_status = ?) " +
                             "WHERE id_pesanan = ?")) {

            pstmt.setString(1, namaStatus);
            pstmt.setInt(2, idPesanan);
            int result = pstmt.executeUpdate();

            if (result > 0) {
                AlertUtil.showSuccess("Berhasil", "Status pesanan berhasil diubah ke: " + namaStatus);
                loadPesananData();
            } else {
                AlertUtil.showError("Gagal mengubah status pesanan.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error: " + e.getMessage());
        }
    }
}
