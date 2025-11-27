package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
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
import java.util.Optional;

// Controller utama untuk halaman Kelola Pesanan Admin
public class KelolaPesananController {

    // Komponen UI Panel (untuk switch view antara List, Tambah, Edit)
    @FXML private VBox daftarPesananPanel;
    @FXML private VBox formTambahPanel;
    @FXML private VBox formEditPanel;

    // Komponen UI Daftar Pesanan
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Button refreshButton;
    @FXML private Button btnTambahPesanan;
    @FXML private ListView<Pesanan> pesananListView;

    // Komponen UI Form Tambah Pesanan
    @FXML private TextField namaPelangganField;
    @FXML private TextField noTeleponField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> jenisLayananComboBox;
    @FXML private TextField jumlahField;
    @FXML private TextField totalHargaField;
    @FXML private TextArea spesifikasiArea;
    @FXML private Button simpanPesananButton;
    @FXML private Button batalTambahButton;

    // Komponen UI Form Edit Pesanan
    @FXML private Label editIdPesananLabel;
    @FXML private Label editSubtitleLabel;
    @FXML private TextField editNamaPelangganField;
    @FXML private TextField editNoTeleponField;
    @FXML private TextField editEmailField;
    @FXML private ComboBox<String> editJenisLayananComboBox;
    @FXML private TextField editJumlahField;
    @FXML private TextField editTotalHargaField;
    @FXML private TextArea editSpesifikasiArea;
    @FXML private Button updatePesananButton;
    @FXML private Button batalEditButton;

    // Variabel Data
    private PesananDAO pesananDAO;
    private final ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private Pesanan currentEditPesanan; // Menyimpan pesanan yang sedang diedit

    // Method ini jalan otomatis saat halaman dibuka pertama kali
    @FXML
    public void initialize() {
        pesananDAO = new PesananDAO();

        setupDaftarPesanan();   // Siapkan list view
        setupFormTambah();      // Siapkan tombol di form tambah
        setupFormEdit();        // Siapkan tombol di form edit

        loadJenisLayanan();     // Ambil data layanan dari DB ke dropdown
        loadPesananData();      // Ambil data pesanan dari DB

        showDaftarPanel();      // Tampilkan panel daftar pesanan di awal
    }

    // --- BAGIAN NAVIGASI PANEL ---
    // Fungsi-fungsi ini untuk ganti tampilan (hide/show panel)

    private void showDaftarPanel() {
        daftarPesananPanel.setVisible(true);
        daftarPesananPanel.setManaged(true);
        formTambahPanel.setVisible(false);
        formTambahPanel.setManaged(false);
        formEditPanel.setVisible(false);
        formEditPanel.setManaged(false);
    }

    private void showTambahPanel() {
        daftarPesananPanel.setVisible(false);
        daftarPesananPanel.setManaged(false);
        formTambahPanel.setVisible(true);
        formTambahPanel.setManaged(true);
        formEditPanel.setVisible(false);
        formEditPanel.setManaged(false);
        clearFormTambah(); // Bersihkan form sebelum dipakai
    }

    private void showEditPanel() {
        daftarPesananPanel.setVisible(false);
        daftarPesananPanel.setManaged(false);
        formTambahPanel.setVisible(false);
        formTambahPanel.setManaged(false);
        formEditPanel.setVisible(true);
        formEditPanel.setManaged(true);
    }

    // --- PENGATURAN LIST VIEW ---

    private void setupDaftarPesanan() {
        // Mengatur tampilan sel list menggunakan class PesananCardCell
        // Kita mengirimkan fungsi-fungsi (handleEdit, handleDelete, dll) ke dalam cell
        pesananListView.setCellFactory(param -> new PesananCardCell(
                this::handleEditPesanan,       
                this::handleDeletePesanan,     
                this::handleKonfirmasiPesanan, 
                this::handleUbahStatus         
        ));

        pesananListView.setItems(pesananList);

        setupFilters();
        btnTambahPesanan.setOnAction(e -> showTambahPanel());
    }

    // Setup filter status dan pencarian
    private void setupFilters() {
        ObservableList<String> statusOptions = FXCollections.observableArrayList();
        statusOptions.add("Semua Status");

        // Ambil daftar status dari database
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

        // Auto-refresh saat ngetik search atau ganti filter
        searchField.textProperty().addListener((obs, old, newVal) -> loadPesananData());
        filterStatusComboBox.valueProperty().addListener((obs, old, newVal) -> loadPesananData());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            filterStatusComboBox.setValue("Semua Status");
            loadPesananData();
        });
    }

    // Fungsi yang dipanggil saat status di dropdown diubah
    private void handleUbahStatus(Pesanan pesanan, String statusBaru) { 
        String statusLama = pesanan.getStatus();

        // LOGIKA KHUSUS: Jika memilih "Pembayaran Verified", munculkan popup input uang
        if (statusBaru.equalsIgnoreCase("Pembayaran Verified")) {
            showPembayaranVerifiedDialog(pesanan);
            return;
        }

        // Konfirmasi biasa untuk status lain
        boolean confirmed = AlertUtil.showConfirmation(
                "Ubah Status Pesanan",
                "Ubah status pesanan dari " + pesanan.getNamaPelanggan() +
                        "\ndari '" + statusLama + "' ke '" + statusBaru + "'?"
        );

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), statusBaru);
        }
    }

    // --- BAGIAN QUERY DATABASE (PENTING) ---

    private void loadPesananData() {
        pesananListView.getSelectionModel().clearSelection();
        pesananList.clear();

        // Query JOIN tabel pesanan, pelanggan, status, dan detail_pesanan
        StringBuilder query = new StringBuilder(
                "SELECT p.*, " +
                        "pel.nama, pel.no_telepon, pel.email, " +
                        "sp.nama_status, " +
                        "dp.id_layanan, dp.jumlah, dp.subtotal, dp.spesifikasi " +
                        "FROM pesanan p " +
                        "JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan " +
                        "JOIN status_pesanan sp ON p.id_status = sp.id_status " +
                        "LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan " +
                        "WHERE 1=1"
        );

        // Logika Filter Search
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            query.append(" AND (pel.nama LIKE '%").append(searchText).append("%'");
            query.append(" OR pel.no_telepon LIKE '%").append(searchText).append("%')");
        }

        // Logika Filter Status
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
                // Mapping data dari database ke objek Java
                pesanan.setIdPesanan(rs.getInt("id_pesanan"));
                pesanan.setNamaPelanggan(rs.getString("nama"));
                pesanan.setNoTelepon(rs.getString("no_telepon"));
                pesanan.setEmail(rs.getString("email"));
                pesanan.setStatus(rs.getString("nama_status"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalHarga(rs.getDouble("total_biaya"));
                pesanan.setSpesifikasi(rs.getString("spesifikasi"));
                pesanan.setTanggalPesanan(rs.getTimestamp("tanggal_pesanan").toLocalDateTime());

                // Konversi ID layanan ke nama layanan
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

    // --- BAGIAN FORM TAMBAH PESANAN ---

    private void setupFormTambah() {
        simpanPesananButton.setOnAction(e -> simpanPesanan());
        batalTambahButton.setOnAction(e -> {
            clearFormTambah();
            showDaftarPanel();
        });

        // Mencegah input huruf di kolom angka
        forceNumericInput(jumlahField);
        forceNumericInput(totalHargaField);
    }

    // Load data layanan untuk dropdown
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

    // Simpan pesanan baru ke database
    private void simpanPesanan() {
        // Ambil value dari form
        String namaPelanggan = namaPelangganField.getText().trim();
        String noTelepon = noTeleponField.getText().trim();
        String email = emailField.getText().trim();
        String jenisLayanan = jenisLayananComboBox.getValue();
        String jumlahStr = jumlahField.getText().trim();
        String totalHargaStr = totalHargaField.getText().trim();
        String spesifikasi = spesifikasiArea.getText().trim();

        // Validasi input kosong
        if (namaPelanggan.isEmpty() || noTelepon.isEmpty() || jenisLayanan == null ||
                jumlahStr.isEmpty() || totalHargaStr.isEmpty()) {
            AlertUtil.showWarning("Data Tidak Lengkap", "Mohon isi semua field wajib (bertanda *).");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            double totalHarga = Double.parseDouble(totalHargaStr);

            // Panggil DAO untuk insert ke DB
            boolean isSuccess = pesananDAO.createPesanan(
                    namaPelanggan, noTelepon, email, jenisLayanan, jumlah, totalHarga, spesifikasi
            );

            if (isSuccess) {
                AlertUtil.showSuccess("Berhasil", "Pesanan berhasil dibuat!");
                clearFormTambah();
                showDaftarPanel();
                loadPesananData(); // Refresh list
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
        jenisLayananComboBox.getSelectionModel().clearSelection();
        jumlahField.clear();
        totalHargaField.clear();
        spesifikasiArea.clear();
    }

    // --- BAGIAN EDIT PESANAN ---

    private void setupFormEdit() {
        updatePesananButton.setOnAction(e -> updatePesanan());
        batalEditButton.setOnAction(e -> {
            clearFormEdit();
            showDaftarPanel();
        });

        forceNumericInput(editJumlahField);
        forceNumericInput(editTotalHargaField);
    }

    // Mengisi form edit dengan data pesanan yang dipilih
    private void loadDataToEditForm(Pesanan pesanan) {
        currentEditPesanan = pesanan;

        editIdPesananLabel.setText(String.valueOf(pesanan.getIdPesanan()));
        editSubtitleLabel.setText("Edit pesanan #" + pesanan.getIdPesanan() + " - " + pesanan.getNamaPelanggan());

        editNamaPelangganField.setText(pesanan.getNamaPelanggan());
        editNoTeleponField.setText(pesanan.getNoTelepon());
        editEmailField.setText(pesanan.getEmail() != null ? pesanan.getEmail() : "");
        editJenisLayananComboBox.setValue(pesanan.getJenisLayanan());
        editJumlahField.setText(String.valueOf(pesanan.getJumlah()));
        editTotalHargaField.setText(String.valueOf((int) pesanan.getTotalHarga()));
        editSpesifikasiArea.setText(pesanan.getSpesifikasi() != null ? pesanan.getSpesifikasi() : "");

        showEditPanel();
    }

    // Proses update data ke database
    private void updatePesanan() {
        if (currentEditPesanan == null) {
            AlertUtil.showError("Error", "Tidak ada pesanan yang dipilih untuk di-update.");
            return;
        }

        // Ambil data baru dari form edit
        String namaPelanggan = editNamaPelangganField.getText().trim();
        String noTelepon = editNoTeleponField.getText().trim();
        String email = editEmailField.getText().trim();
        String jenisLayanan = editJenisLayananComboBox.getValue();
        String jumlahStr = editJumlahField.getText().trim();
        String totalHargaStr = editTotalHargaField.getText().trim();
        String spesifikasi = editSpesifikasiArea.getText().trim();

        if (namaPelanggan.isEmpty() || noTelepon.isEmpty() || jenisLayanan == null ||
                jumlahStr.isEmpty() || totalHargaStr.isEmpty()) {
            AlertUtil.showWarning("Data Tidak Lengkap", "Mohon isi semua field wajib.");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            double totalHarga = Double.parseDouble(totalHargaStr);
            int idPesanan = currentEditPesanan.getIdPesanan();

            Connection conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaksi (agar data konsisten di 3 tabel)

            try {
                // 1. Update data Pelanggan
                String updatePelangganSql =
                    "UPDATE pelanggan SET nama = ?, no_telepon = ?, email = ? " +
                    "WHERE id_pelanggan = (SELECT id_pelanggan FROM pesanan WHERE id_pesanan = ?)";
                PreparedStatement psPelanggan = conn.prepareStatement(updatePelangganSql);
                psPelanggan.setString(1, namaPelanggan);
                psPelanggan.setString(2, noTelepon);
                psPelanggan.setString(3, email);
                psPelanggan.setInt(4, idPesanan);
                psPelanggan.executeUpdate();

                // 2. Update data Pesanan (Header)
                String updatePesananSql = "UPDATE pesanan SET total_biaya = ?, catatan = ? WHERE id_pesanan = ?";
                PreparedStatement psPesanan = conn.prepareStatement(updatePesananSql);
                psPesanan.setDouble(1, totalHarga);
                psPesanan.setString(2, spesifikasi);
                psPesanan.setInt(3, idPesanan);
                psPesanan.executeUpdate();

                // 3. Update Detail Pesanan
                int idLayanan = getLayananIdByName(conn, jenisLayanan);
                double hargaSatuan = totalHarga / jumlah;

                String updateDetailSql =
                    "UPDATE detail_pesanan SET id_layanan = ?, jumlah = ?, " +
                    "harga_satuan = ?, subtotal = ?, spesifikasi = ? WHERE id_pesanan = ?";
                PreparedStatement psDetail = conn.prepareStatement(updateDetailSql);
                psDetail.setInt(1, idLayanan);
                psDetail.setInt(2, jumlah);
                psDetail.setDouble(3, hargaSatuan);
                psDetail.setDouble(4, totalHarga);
                psDetail.setString(5, spesifikasi);
                psDetail.setInt(6, idPesanan);
                psDetail.executeUpdate();

                conn.commit(); // Simpan perubahan permanen

                AlertUtil.showSuccess("Berhasil", "Pesanan berhasil diupdate!");
                clearFormEdit();
                showDaftarPanel();
                loadPesananData();

            } catch (Exception e) {
                conn.rollback(); // Batalkan jika ada error
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
        editJenisLayananComboBox.getSelectionModel().clearSelection();
        editJumlahField.clear();
        editTotalHargaField.clear();
        editSpesifikasiArea.clear();
    }

    // Helper: Memaksa textfield hanya menerima angka
    private void forceNumericInput(TextField tf) {
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tf.setText(newValue.replaceAll("\\D", ""));
            }
        });
    }

    // Helper: Ambil nama layanan berdasarkan ID
    private String getLayananName(int idLayanan) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT nama_layanan FROM jenis_layanan WHERE id_layanan = ?")) {
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

    // Helper: Ambil ID layanan berdasarkan Nama
    private int getLayananIdByName(Connection conn, String namaLayanan) throws Exception {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_layanan");
        }
        return 1; 
    }

    // --- HANDLER AKSI TOMBOL DI KARTU PESANAN ---

    private void handleEditPesanan(Pesanan pesanan) {
        loadDataToEditForm(pesanan); // Buka form edit
    }

    private void handleDeletePesanan(Pesanan pesanan) {
        boolean confirmed = AlertUtil.showConfirmation(
                "Hapus Pesanan",
                "Apakah Anda yakin ingin menghapus pesanan dari " + pesanan.getNamaPelanggan() + "?"
        );

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

    // Aksi tombol "Konfirmasi" (ubah status ke Menunggu Pembayaran)
    private void handleKonfirmasiPesanan(Pesanan pesanan) {
        boolean confirmed = AlertUtil.showConfirmation(
                "Konfirmasi Pesanan",
                "Konfirmasi pesanan dari " + pesanan.getNamaPelanggan() +
                        "?\n\nStatus akan diubah ke 'Menunggu Pembayaran'."
        );

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), "Menunggu Pembayaran");
        }
    }

    private void handleUbahStatus(Pesanan pesanan, String statusBaru, ComboBox<String> statusComboBox) {
        String statusLama = pesanan.getStatus();
        
        if (statusBaru.equalsIgnoreCase("Pembayaran Verified")) {
            showPembayaranVerifiedDialog(pesanan);
            return;
        }

        boolean confirmed = AlertUtil.showConfirmation(
                "Ubah Status Pesanan",
                "Ubah status pesanan dari " + pesanan.getNamaPelanggan() +
                        "\ndari '" + statusLama + "' ke '" + statusBaru + "'?"
        );

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), statusBaru);
        }
    }

    // Update status pesanan di database
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
                loadPesananData(); // Refresh agar tampilan update
            } else {
                AlertUtil.showError("Gagal mengubah status pesanan.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error: " + e.getMessage());
        }
    }

    // Menampilkan Dialog Khusus untuk "Pembayaran Verified"
    private void showPembayaranVerifiedDialog(Pesanan pesanan) {
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Verifikasi Pembayaran");
        dialog.setHeaderText("Tunggu dulu! Masukkan nominal uang yang diterima dan metode pembayarannya.");

        // Layout Grid untuk form dialog
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20));

        // Input Nominal
        TextField nominalField = new TextField();
        nominalField.setPromptText("Contoh: 2500000");
        Label nominalLabel = new Label("Nominal Diterima (Rp):");
        nominalLabel.getStyleClass().add("form-label");

        // Input Metode Pembayaran
        ComboBox<String> metodeComboBox = new ComboBox<>();
        metodeComboBox.getItems().addAll("Cash", "Transfer Bank", "E-Wallet", "Kartu Kredit/Debit");
        metodeComboBox.setPromptText("Pilih metode pembayaran");
        Label metodeLabel = new Label("Metode Pembayaran:");
        metodeLabel.getStyleClass().add("form-label");

        // Info Pesanan
        Label infoLabel = new Label(
                "Pesanan: " + pesanan.getNamaPelanggan() +
                        "\nTotal Tagihan: Rp" + String.format("%,.2f", pesanan.getTotalHarga())
        );
        infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        grid.add(infoLabel, 0, 0, 2, 1);
        grid.add(nominalLabel, 0, 1);
        grid.add(nominalField, 1, 1);
        grid.add(metodeLabel, 0, 2);
        grid.add(metodeComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Tombol Simpan & Batal
        ButtonType simpanButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        ButtonType batalButtonType = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(simpanButtonType, batalButtonType);

        // Validasi: Tombol simpan mati kalau input kosong
        javafx.scene.Node simpanButton = dialog.getDialogPane().lookupButton(simpanButtonType);
        simpanButton.setDisable(true);

        nominalField.textProperty().addListener((obs, old, newVal) -> simpanButton.setDisable(newVal.trim().isEmpty() || metodeComboBox.getValue() == null));

        metodeComboBox.valueProperty().addListener((obs, old, newVal) -> simpanButton.setDisable(nominalField.getText().trim().isEmpty() || newVal == null));

        // Paksa nominal hanya angka
        nominalField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                nominalField.setText(newVal.replaceAll("\\D", ""));
            }
        });

        // Tampilkan dialog dan tunggu respon
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == simpanButtonType) {
            String nominal = nominalField.getText();
            String metode = metodeComboBox.getValue();
            // TODO: Di sini nanti akan ada logika INSERT ke tabel pembayaran
            AlertUtil.showInfo("Info",
                    "FITUR INI AKAN DIIMPLEMENTASIKAN NANTI\n\n" +
                            "Data yang akan disimpan:\n" +
                            "Nominal: Rp" + nominal + "\n" +
                            "Metode: " + metode + "\n\n" +
                            "Untuk saat ini, status tidak akan berubah.");
        }
    }
}
