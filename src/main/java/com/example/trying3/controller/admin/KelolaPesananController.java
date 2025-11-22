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
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE pesanan SET id_status = 2 WHERE id_pesanan = ?")) {

                pstmt.setInt(1, pesanan.getIdPesanan());
                int result = pstmt.executeUpdate();

                if (result > 0) {
                    AlertUtil.showSuccess("Berhasil",
                            "Pesanan berhasil dikonfirmasi!\nStatus: Menunggu Pembayaran");
                    loadPesananData(); // Refresh list
                } else {
                    AlertUtil.showError("Gagal mengkonfirmasi pesanan.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Error: " + e.getMessage());
            }
        }
    }

    // =====================================================
    // CUSTOM CARD CELL
    // =====================================================

    private class PesananCardCell extends ListCell<Pesanan> {
        private final VBox cardContainer = new VBox();
        private final Label orderIdLabel = new Label();
        private final Label orderDateLabel = new Label();
        private final Label pelangganLabel = new Label();
        private final Label pelangganValue = new Label();
        private final Label phoneValue = new Label();
        private final Label layananLabel = new Label();
        private final Label layananValue = new Label();
        private final Label jumlahValue = new Label();
        private final Label totalLabel = new Label();
        private final Label totalValue = new Label();
        private final Label specLabel = new Label();
        private final Label specText = new Label();
        private final Button editButton = new Button("✏ Edit");
        private final Button deleteButton = new Button("🗑 Hapus");
        private final Button konfirmasiButton = new Button("✅ Konfirmasi");

        public PesananCardCell() {
            setupCard();
        }

        private void setupCard() {
            cardContainer.getStyleClass().add("pesanan-card");
            cardContainer.setSpacing(12);

            orderIdLabel.getStyleClass().add("pesanan-order-id");
            orderDateLabel.getStyleClass().add("pesanan-order-date");
            pelangganLabel.getStyleClass().add("pesanan-label");
            pelangganValue.getStyleClass().add("pesanan-value");
            phoneValue.getStyleClass().add("pesanan-phone");
            layananLabel.getStyleClass().add("pesanan-label");
            layananValue.getStyleClass().add("pesanan-value");
            jumlahValue.getStyleClass().add("pesanan-value");
            totalLabel.getStyleClass().add("pesanan-label");
            totalValue.getStyleClass().add("pesanan-total");
            specLabel.getStyleClass().add("pesanan-spec-label");
            specText.getStyleClass().add("pesanan-spec-text");

            editButton.getStyleClass().addAll("button-secondary", "pesanan-action-button");
            deleteButton.getStyleClass().addAll("button-danger", "pesanan-action-button");
            konfirmasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
        }

        @Override
        protected void updateItem(Pesanan pesanan, boolean empty) {
            super.updateItem(pesanan, empty);

            if (empty || pesanan == null) {
                setGraphic(null);
                return;
            }

            // Format data
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

            // Header
            orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()));
            orderDateLabel.setText(pesanan.getTanggalPesanan().format(dateFormatter));

            VBox headerBox = new VBox(2, orderIdLabel, orderDateLabel);
            headerBox.getStyleClass().add("pesanan-card-header");

            // Info Grid
            GridPane infoGrid = new GridPane();
            infoGrid.getStyleClass().add("pesanan-info-grid");
            infoGrid.setHgap(30);
            infoGrid.setVgap(12);

            pelangganLabel.setText("Pelanggan:");
            pelangganValue.setText(pesanan.getNamaPelanggan());
            phoneValue.setText(pesanan.getNoTelepon());
            VBox pelangganBox = new VBox(3, pelangganLabel, pelangganValue, phoneValue);

            layananLabel.setText("Layanan:");
            layananValue.setText(pesanan.getJenisLayanan());
            jumlahValue.setText(pesanan.getJumlah() + " pcs");
            VBox layananBox = new VBox(3, layananLabel, layananValue, jumlahValue);

            totalLabel.setText("Total:");
            totalValue.setText(currencyFormat.format(pesanan.getTotalHarga()));
            VBox totalBox = new VBox(3, totalLabel, totalValue);

            infoGrid.add(pelangganBox, 0, 0);
            infoGrid.add(layananBox, 1, 0);
            infoGrid.add(totalBox, 2, 0);

            // Spesifikasi
            VBox specBox = new VBox(5);
            if (pesanan.getSpesifikasi() != null && !pesanan.getSpesifikasi().isEmpty()) {
                specLabel.setText("Spesifikasi:");
                specText.setText(pesanan.getSpesifikasi());
                specText.setWrapText(true);
                specText.setMaxWidth(Double.MAX_VALUE);
                specBox.getChildren().addAll(specLabel, specText);
            }

            // Separator
            Region separator = new Region();
            separator.getStyleClass().add("pesanan-separator");
            separator.setPrefHeight(1);

            // Status Badge
            Label statusBadge = new Label(pesanan.getStatus());
            statusBadge.getStyleClass().add("status-badge");
            String statusLower = pesanan.getStatus().toLowerCase().replace(" ", "-");
            statusBadge.getStyleClass().add("status-" + statusLower);

            // Actions
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_RIGHT);

            if (pesanan.getStatus().equalsIgnoreCase("Baru Dibuat")) {
                actionBox.getChildren().addAll(konfirmasiButton, editButton, deleteButton);
            } else {
                actionBox.getChildren().addAll(editButton, deleteButton);
            }

            HBox bottomBox = new HBox(15);
            bottomBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(actionBox, Priority.ALWAYS);
            bottomBox.getChildren().addAll(statusBadge, actionBox);

            // Button actions
            konfirmasiButton.setOnAction(e -> handleKonfirmasiPesanan(pesanan));
            editButton.setOnAction(e -> handleEditPesanan(pesanan));
            deleteButton.setOnAction(e -> handleDeletePesanan(pesanan));

            // Assemble card
            cardContainer.getChildren().clear();
            cardContainer.getChildren().addAll(
                    headerBox,
                    infoGrid,
                    specBox,
                    separator,
                    bottomBox
            );

            setGraphic(cardContainer);
        }
    }
}
