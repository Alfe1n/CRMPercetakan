package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class KelolaPesananController {

    // ========== PANEL CONTAINERS ==========
    @FXML private VBox daftarPesananPanel;
    @FXML private VBox formTambahPanel;
    @FXML private VBox formEditPanel;

    // ========== DAFTAR PESANAN COMPONENTS ==========
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private Button refreshButton;
    @FXML private Button btnTambahPesanan;
    @FXML private ListView<Pesanan> pesananListView;

    // ========== FORM TAMBAH COMPONENTS ==========
    @FXML private Button btnBackFromTambah;
    @FXML private TextField namaPelangganField;
    @FXML private TextField noTeleponField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> jenisLayananComboBox;
    @FXML private TextField jumlahField;
    @FXML private TextField totalHargaField;
    @FXML private TextArea spesifikasiArea;
    @FXML private Button simpanPesananButton;
    @FXML private Button batalTambahButton;

    // ========== FORM EDIT COMPONENTS ==========
    @FXML private Button btnBackFromEdit;
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

    // ========== DATA ==========
    private PesananDAO pesananDAO;
    private ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private Pesanan currentEditPesanan; // For tracking which pesanan is being edited

    @FXML
    public void initialize() {
        System.out.println("✅ KelolaPesananController initialized");

        pesananDAO = new PesananDAO();

        // Setup Components
        setupDaftarPesanan();
        setupFormTambah();
        setupFormEdit();

        // Load Initial Data
        loadJenisLayanan();
        loadPesananData();

        // Show Daftar Panel by Default
        showDaftarPanel();
    }

    // =====================================================
    // PANEL SWITCHING METHODS
    // =====================================================

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
        
        clearFormTambah();
    }

    private void showEditPanel() {
        daftarPesananPanel.setVisible(false);
        daftarPesananPanel.setManaged(false);
        
        formTambahPanel.setVisible(false);
        formTambahPanel.setManaged(false);
        
        formEditPanel.setVisible(true);
        formEditPanel.setManaged(true);
    }

    // =====================================================
    // DAFTAR PESANAN SETUP
    // =====================================================

    private void setupDaftarPesanan() {
        // ListView
        pesananListView.setCellFactory(param -> new PesananCardCell());
        pesananListView.setItems(pesananList);

        // Filters
        setupFilters();

        // Tombol Tambah
        btnTambahPesanan.setOnAction(e -> showTambahPanel());
    }

    private void setupFilters() {
        // Load status options
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

        // Listeners
        searchField.textProperty().addListener((obs, old, newVal) -> loadPesananData());
        filterStatusComboBox.valueProperty().addListener((obs, old, newVal) -> loadPesananData());

        refreshButton.setOnAction(e -> {
            searchField.clear();
            filterStatusComboBox.setValue("Semua Status");
            loadPesananData();
        });
    }

    private void loadPesananData() {
        pesananListView.getSelectionModel().clearSelection();
        pesananList.clear();

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

        // Apply Search Filter
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            query.append(" AND (pel.nama LIKE '%").append(searchText).append("%'");
            query.append(" OR pel.no_telepon LIKE '%").append(searchText).append("%')");
        }

        // Apply Status Filter
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
                pesanan.setStatus(rs.getString("nama_status"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalHarga(rs.getDouble("total_biaya"));
                pesanan.setSpesifikasi(rs.getString("spesifikasi"));
                pesanan.setTanggalPesanan(rs.getTimestamp("tanggal_pesanan").toLocalDateTime());

                // Get Layanan Name
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

    // =====================================================
    // FORM TAMBAH SETUP
    // =====================================================

    private void setupFormTambah() {
        btnBackFromTambah.setOnAction(e -> {
            showDaftarPanel();
            loadPesananData(); // Refresh data
        });

        simpanPesananButton.setOnAction(e -> simpanPesanan());
        batalTambahButton.setOnAction(e -> {
            clearFormTambah();
            showDaftarPanel();
        });

        // Numeric validation
        forceNumericInput(jumlahField);
        forceNumericInput(totalHargaField);
    }

    private void loadJenisLayanan() {
        ObservableList<String> layananList = FXCollections.observableArrayList();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_layanan FROM jenis_layanan WHERE is_active = 1")) {

            while (rs.next()) {
                layananList.add(rs.getString("nama_layanan"));
            }
            
            jenisLayananComboBox.setItems(layananList);
            editJenisLayananComboBox.setItems(layananList); // For edit form too

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data layanan.");
        }
    }

    private void simpanPesanan() {
        // Validation
        String namaPelanggan = namaPelangganField.getText().trim();
        String noTelepon = noTeleponField.getText().trim();
        String email = emailField.getText().trim();
        String jenisLayanan = jenisLayananComboBox.getValue();
        String jumlahStr = jumlahField.getText().trim();
        String totalHargaStr = totalHargaField.getText().trim();
        String spesifikasi = spesifikasiArea.getText().trim();

        if (namaPelanggan.isEmpty() || noTelepon.isEmpty() || jenisLayanan == null ||
                jumlahStr.isEmpty() || totalHargaStr.isEmpty()) {
            AlertUtil.showWarning("Data Tidak Lengkap", "Mohon isi semua field wajib (bertanda *).");
            return;
        }

        try {
            int jumlah = Integer.parseInt(jumlahStr);
            double totalHarga = Double.parseDouble(totalHargaStr);

            boolean isSuccess = pesananDAO.createPesanan(
                    namaPelanggan, noTelepon, email, jenisLayanan, jumlah, totalHarga, spesifikasi
            );

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
        jenisLayananComboBox.getSelectionModel().clearSelection();
        jumlahField.clear();
        totalHargaField.clear();
        spesifikasiArea.clear();
    }

    // =====================================================
    // FORM EDIT SETUP
    // =====================================================

    private void setupFormEdit() {
        btnBackFromEdit.setOnAction(e -> {
            showDaftarPanel();
            loadPesananData();
        });

        updatePesananButton.setOnAction(e -> updatePesanan());
        batalEditButton.setOnAction(e -> {
            clearFormEdit();
            showDaftarPanel();
        });

        // Numeric validation
        forceNumericInput(editJumlahField);
        forceNumericInput(editTotalHargaField);
    }

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

    private void updatePesanan() {
        if (currentEditPesanan == null) {
            AlertUtil.showError("Error", "Tidak ada pesanan yang dipilih untuk di-update.");
            return;
        }

        // Validation
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
            conn.setAutoCommit(false);

            try {
                // 1. Update Pelanggan
                String updatePelangganSql = 
                    "UPDATE pelanggan SET nama = ?, no_telepon = ?, email = ? " +
                    "WHERE id_pelanggan = (SELECT id_pelanggan FROM pesanan WHERE id_pesanan = ?)";
                PreparedStatement psPelanggan = conn.prepareStatement(updatePelangganSql);
                psPelanggan.setString(1, namaPelanggan);
                psPelanggan.setString(2, noTelepon);
                psPelanggan.setString(3, email);
                psPelanggan.setInt(4, idPesanan);
                psPelanggan.executeUpdate();

                // 2. Update Pesanan
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
        editJenisLayananComboBox.getSelectionModel().clearSelection();
        editJumlahField.clear();
        editTotalHargaField.clear();
        editSpesifikasiArea.clear();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private void forceNumericInput(TextField tf) {
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tf.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

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

    private int getLayananIdByName(Connection conn, String namaLayanan) throws Exception {
        String sql = "SELECT id_layanan FROM jenis_layanan WHERE nama_layanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaLayanan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_layanan");
        }
        return 1; // Default
    }

    // =====================================================
    // CARD ACTIONS
    // =====================================================

    private void handleEditPesanan(Pesanan pesanan) {
        loadDataToEditForm(pesanan);
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

        // SPECIAL CASE: Pembayaran Verified
        if (statusBaru.equalsIgnoreCase("Pembayaran Verified")) {
            showPembayaranVerifiedDialog(pesanan);
            return;
        }

        // Konfirmasi perubahan status
        boolean confirmed = AlertUtil.showConfirmation(
                "Ubah Status Pesanan",
                "Ubah status pesanan dari " + pesanan.getNamaPelanggan() +
                        "\ndari '" + statusLama + "' ke '" + statusBaru + "'?"
        );

        if (confirmed) {
            updateStatusPesanan(pesanan.getIdPesanan(), statusBaru);
        }
    }

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
                loadPesananData(); // Refresh
            } else {
                AlertUtil.showError("Gagal mengubah status pesanan.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error: " + e.getMessage());
        }
    }

    private void showPembayaranVerifiedDialog(Pesanan pesanan) {
        // Create custom dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Verifikasi Pembayaran");
        dialog.setHeaderText("Tunggu dulu! Masukkan nominal uang yang diterima dan metode pembayarannya.");

        // Dialog content
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20));

        // Nominal field
        TextField nominalField = new TextField();
        nominalField.setPromptText("Contoh: 2500000");
        Label nominalLabel = new Label("Nominal Diterima (Rp):");
        nominalLabel.getStyleClass().add("form-label");

        // Metode pembayaran dropdown
        ComboBox<String> metodeComboBox = new ComboBox<>();
        metodeComboBox.getItems().addAll("Cash", "Transfer Bank", "E-Wallet", "Kartu Kredit/Debit");
        metodeComboBox.setPromptText("Pilih metode pembayaran");
        Label metodeLabel = new Label("Metode Pembayaran:");
        metodeLabel.getStyleClass().add("form-label");

        // Info pesanan
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

        // Buttons
        ButtonType simpanButtonType = new ButtonType("Simpan", ButtonBar.ButtonData.OK_DONE);
        ButtonType batalButtonType = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(simpanButtonType, batalButtonType);

        // Validation
        javafx.scene.Node simpanButton = dialog.getDialogPane().lookupButton(simpanButtonType);
        simpanButton.setDisable(true);

        // Enable button only when fields are filled
        nominalField.textProperty().addListener((obs, old, newVal) -> {
            simpanButton.setDisable(newVal.trim().isEmpty() || metodeComboBox.getValue() == null);
        });

        metodeComboBox.valueProperty().addListener((obs, old, newVal) -> {
            simpanButton.setDisable(nominalField.getText().trim().isEmpty() || newVal == null);
        });

        // Numeric validation for nominal
        nominalField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                nominalField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // Show dialog
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == simpanButtonType) {
            String nominal = nominalField.getText();
            String metode = metodeComboBox.getValue();

            AlertUtil.showInfo("Info",
                    "FITUR INI AKAN DIIMPLEMENTASIKAN NANTI\n\n" +
                            "Data yang akan disimpan:\n" +
                            "Nominal: Rp" + nominal + "\n" +
                            "Metode: " + metode + "\n\n" +
                            "Untuk saat ini, status tidak akan berubah.");

            // NANTI: Simpan ke tabel pembayaran, lalu update status
            // savePaymentData(pesanan.getIdPesanan(), nominal, metode);
            // updateStatusPesanan(pesanan.getIdPesanan(), "Pembayaran Verified");
        }
    }

    // =====================================================
    // CUSTOM CARD CELL
    // =====================================================

    private class PesananCardCell extends ListCell<Pesanan> {
        private final VBox cardContainer = new VBox();

        // Header Components
        private final Label orderIdLabel = new Label();
        private final Label orderDateLabel = new Label();
        private final Label statusBadge = new Label();
        private final HBox headerBox = new HBox(); // Wadah header

        // Info Grid Components
        private final GridPane infoGrid = new GridPane();
        private final Label pelangganLabel = new Label();
        private final Label pelangganValue = new Label();
        private final Label phoneValue = new Label();
        private final Label layananLabel = new Label();
        private final Label layananValue = new Label();
        private final Label jumlahValue = new Label();
        private final Label totalLabel = new Label();
        private final Label totalValue = new Label();

        // Spesifikasi Components
        private final VBox specBox = new VBox(5);
        private final Label specText = new Label();
        private final Label specLabel = new Label();

        // Action Buttons
        private final ComboBox<String> ubahStatusComboBox = new ComboBox<>();
        private final Button editButton = new Button("✏ Edit");
        private final Button deleteButton = new Button("🗑 Hapus");
        private final Button konfirmasiButton = new Button("✅ Konfirmasi");
        private final HBox actionBox = new HBox(10);

        public PesananCardCell() {
            setupCard();
        }

        private void setupCard() {
            cardContainer.getStyleClass().add("pesanan-card");
            cardContainer.setSpacing(12);

            // --- SETUP HEADER ---
            orderIdLabel.getStyleClass().add("pesanan-order-id");     // Biar Bold & Warnanya pas
            orderDateLabel.getStyleClass().add("pesanan-order-date"); // Biar tanggalnya rapi

            VBox leftHeader = new VBox(2, orderIdLabel, orderDateLabel);
            leftHeader.getStyleClass().add("pesanan-card-header");

            statusBadge.getStyleClass().add("status-badge");

            headerBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(leftHeader, Priority.ALWAYS);
            headerBox.getChildren().addAll(leftHeader, statusBadge);

            // --- SETUP GRID INFO ---
            infoGrid.getStyleClass().add("pesanan-info-grid");
            infoGrid.setHgap(30);
            infoGrid.setVgap(12);

            // Helper method untuk bikin VBox kecil biar rapi
            infoGrid.add(createInfoBox("Pelanggan:", pelangganValue, phoneValue), 0, 0);
            infoGrid.add(createInfoBox("Layanan:", layananValue, jumlahValue), 1, 0);
            infoGrid.add(createInfoBox("Total:", totalValue), 2, 0);

            // --- SETUP SPEC ---
            Label specLabel = new Label("Spesifikasi:");
            specLabel.getStyleClass().add("pesanan-spec-label");
            specText.getStyleClass().add("pesanan-spec-text");
            specText.setWrapText(true);
            specBox.getChildren().addAll(specLabel, specText);

            // --- SETUP BOTTOM ACTIONS ---
            // Jurus Paksa ButtonCell (dari chat sebelumnya)
            ubahStatusComboBox.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) setText(ubahStatusComboBox.getPromptText());
                    else setText(item);
                }
            });

            // Action Box
            konfirmasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
            editButton.getStyleClass().addAll("button-secondary", "pesanan-action-button");
            deleteButton.getStyleClass().addAll("button-danger", "pesanan-action-button");

            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.getChildren().addAll(konfirmasiButton, editButton, deleteButton); // Pasang semua tombol

            HBox bottomBox = new HBox(15);
            bottomBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(actionBox, Priority.ALWAYS);
            bottomBox.getChildren().addAll(ubahStatusComboBox, actionBox);

            Region separator = new Region();
            separator.getStyleClass().add("pesanan-separator");
            separator.setPrefHeight(1);

            cardContainer.getChildren().addAll(headerBox, infoGrid, specBox, separator, bottomBox);
//
//            orderIdLabel.getStyleClass().add("pesanan-order-id");
//            orderDateLabel.getStyleClass().add("pesanan-order-date");
//            pelangganLabel.getStyleClass().add("pesanan-label");
//            pelangganValue.getStyleClass().add("pesanan-value");
//            phoneValue.getStyleClass().add("pesanan-phone");
//            layananLabel.getStyleClass().add("pesanan-label");
//            layananValue.getStyleClass().add("pesanan-value");
//            jumlahValue.getStyleClass().add("pesanan-value");
//            totalLabel.getStyleClass().add("pesanan-label");
//            totalValue.getStyleClass().add("pesanan-total");
//            specLabel.getStyleClass().add("pesanan-spec-label");
//            specText.getStyleClass().add("pesanan-spec-text");
//
//            editButton.getStyleClass().addAll("button-secondary", "pesanan-action-button");
//            deleteButton.getStyleClass().addAll("button-danger", "pesanan-action-button");
//            konfirmasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
        }

        private VBox createInfoBox(String label, Label... values) {
            VBox box = new VBox(3);
            Label lbl = new Label(label);
            lbl.getStyleClass().add("pesanan-label");
            box.getChildren().add(lbl);
            for(Label v : values) {
                v.getStyleClass().add("pesanan-value"); // atau class yang sesuai
                box.getChildren().add(v);
            }
            return box;
        }

        @Override
        protected void updateItem(Pesanan pesanan, boolean empty) {
            super.updateItem(pesanan, empty);

            if (empty || pesanan == null) {
                setGraphic(null);
                return;
            }

            // 1. Text Updates
            orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()));
            orderDateLabel.setText(pesanan.getTanggalPesanan().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            pelangganValue.setText(pesanan.getNamaPelanggan());
            phoneValue.setText(pesanan.getNoTelepon());
            layananValue.setText(pesanan.getJenisLayanan());
            jumlahValue.setText(pesanan.getJumlah() + " pcs");
            totalValue.setText("Rp" + String.format("%,.0f", pesanan.getTotalHarga())); // Simplified format

            // 2. Status Badge Style
            statusBadge.setText(pesanan.getStatus());
            String targetStyle = "status-" + pesanan.getStatus().toLowerCase().replace(" ", "-");
            // LOGIKA PENTING: Cek dulu, apakah badge SUDAH punya style tersebut?
            if (!statusBadge.getStyleClass().contains(targetStyle)) {
                // Hanya jika BELUM punya, kita lakukan hapus & ganti
                // (Ini mencegah redraw yang tidak perlu saat diklik)

                statusBadge.getStyleClass().removeAll(
                        "status-baru-dibuat",
                        "status-menunggu-pembayaran",
                        "status-pembayaran-verified", // Pastikan semua kemungkinan status ada di sini
                        "status-selesai",
                        "status-dibatalkan"
                );

                statusBadge.getStyleClass().add(targetStyle);
            }

            // 3. Spesifikasi (Show/Hide Logic)
            if (pesanan.getSpesifikasi() != null && !pesanan.getSpesifikasi().isEmpty()) {
                specText.setText(pesanan.getSpesifikasi());
                specBox.setVisible(true);
                specBox.setManaged(true);
            } else {
                specBox.setVisible(false);
                specBox.setManaged(false); // Biar tidak makan tempat kalau kosong
            }

            // 4. Tombol Konfirmasi (Show/Hide Logic)
            boolean isBaru = pesanan.getStatus().equalsIgnoreCase("Baru Dibuat");
            konfirmasiButton.setVisible(isBaru);
            konfirmasiButton.setManaged(isBaru); // Kalau hidden, dia tidak makan tempat

            // 5. Event Handlers tombol
            konfirmasiButton.setOnAction(e -> handleKonfirmasiPesanan(pesanan));
            editButton.setOnAction(e -> handleEditPesanan(pesanan));
            deleteButton.setOnAction(e -> handleDeletePesanan(pesanan));

            // 6. Setup Ubah Status ComboBox
            ubahStatusComboBox.setOnAction(null); // Putus listener dulu biar gak trigger
            ubahStatusComboBox.setValue(null);
            ubahStatusComboBox.setPromptText("Ubah Status");

            ObservableList<String> statusOptions = FXCollections.observableArrayList();
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT nama_status FROM status_pesanan WHERE nama_status IN " +
                                 "('Baru Dibuat', 'Menunggu Pembayaran', 'Pembayaran Verified', 'Dibatalkan') " +
                                 "ORDER BY id_status")) {

                while (rs.next()) {
                    statusOptions.add(rs.getString("nama_status"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                statusOptions.addAll("Baru Dibuat", "Menunggu Pembayaran",
                        "Pembayaran Verified", "Dibatalkan");
            }

            ubahStatusComboBox.setItems(statusOptions);

            ubahStatusComboBox.setOnAction(e -> {
                String statusBaru = ubahStatusComboBox.getValue();
                if (statusBaru == null) {
                    return;
                }

                handleUbahStatus(pesanan, statusBaru, ubahStatusComboBox);

                javafx.application.Platform.runLater(() -> {
                    ubahStatusComboBox.setValue(null);
                    ubahStatusComboBox.setPromptText("Ubah Status");
                });
            });

            // FINAL SET
            setGraphic(cardContainer);
        }
    }
}
