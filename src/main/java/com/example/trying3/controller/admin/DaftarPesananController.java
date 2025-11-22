package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DaftarPesananController {

    // Statistics Labels
    @FXML private Label totalPesananLabel;
    @FXML private Label pendingPesananLabel;
    @FXML private Label prosesPesananLabel;
    @FXML private Label selesaiPesananLabel;

    // Filter Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private ComboBox<String> filterLayananComboBox;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;

    // Table
    @FXML private TableView<Pesanan> pesananTableView;
    @FXML private TableColumn<Pesanan, String> idColumn;
    @FXML private TableColumn<Pesanan, String> namaPelangganColumn;
    @FXML private TableColumn<Pesanan, String> noTeleponColumn;
    @FXML private TableColumn<Pesanan, String> emailColumn;
    @FXML private TableColumn<Pesanan, String> jenisLayananColumn;
    @FXML private TableColumn<Pesanan, String> jumlahColumn;
    @FXML private TableColumn<Pesanan, String> totalHargaColumn;
    @FXML private TableColumn<Pesanan, String> statusColumn;
    @FXML private TableColumn<Pesanan, String> tanggalColumn;
    @FXML private TableColumn<Pesanan, Void> aksiColumn;

    // Pagination
    @FXML private Label resultCountLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private ComboBox<String> itemsPerPageComboBox;

    private ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private int itemsPerPage = 25;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        System.out.println("✅ DaftarPesananController initialized");

        // Setup Table Columns
        setupTableColumns();

        // Setup Filter Controls
        setupFilterControls();

        // Setup Pagination
        setupPagination();

        // Load Data
        loadJenisLayanan();
        loadStatistics();
        loadPesananData();
    }

    private void setupTableColumns() {
        // ID Column
        idColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getIdPesanan()))
        );

        // Nama Pelanggan Column
        namaPelangganColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNamaPelanggan())
        );

        // No Telepon Column
        noTeleponColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNoTelepon())
        );

        // Email Column
        emailColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getEmail())
        );

        // Jenis Layanan Column
        jenisLayananColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getJenisLayanan())
        );

        // Jumlah Column
        jumlahColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getJumlah()))
        );

        // Total Harga Column with Currency Format
        totalHargaColumn.setCellValueFactory(data -> {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return new SimpleStringProperty(currencyFormat.format(data.getValue().getTotalHarga()));
        });

        // Status Column with Custom Cell
        statusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus())
        );
        statusColumn.setCellFactory(column -> new TableCell<Pesanan, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Apply style based on status
                    switch (item.toLowerCase()) {
                        case "pending":
                            setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;");
                            break;
                        case "proses":
                            setStyle("-fx-background-color: #cce5ff; -fx-text-fill: #004085; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;");
                            break;
                        case "selesai":
                            setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;");
                            break;
                        case "dibatalkan":
                            setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 15;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        // Tanggal Column
        tanggalColumn.setCellValueFactory(data -> {
            LocalDateTime tanggal = data.getValue().getTanggalPesanan();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return new SimpleStringProperty(tanggal.format(formatter));
        });

        // Aksi Column with Buttons
        aksiColumn.setCellFactory(column -> new TableCell<Pesanan, Void>() {
            private final Button editButton = new Button("✏ Edit");
            private final Button deleteButton = new Button("🗑 Hapus");
            private final HBox container = new HBox(8, editButton, deleteButton);

            {
                container.setAlignment(Pos.CENTER);
                
                editButton.getStyleClass().addAll("button-secondary");
                editButton.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                editButton.setOnAction(event -> {
                    Pesanan pesanan = getTableView().getItems().get(getIndex());
                    handleEditPesanan(pesanan);
                });

                deleteButton.getStyleClass().addAll("button-danger");
                deleteButton.setStyle("-fx-font-size: 12px; -fx-padding: 4 10;");
                deleteButton.setOnAction(event -> {
                    Pesanan pesanan = getTableView().getItems().get(getIndex());
                    handleDeletePesanan(pesanan);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupFilterControls() {
        // Search Field Listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            loadPesananData();
        });

        // Filter Status Listener
        filterStatusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            loadPesananData();
        });

        // Filter Layanan Listener
        filterLayananComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            loadPesananData();
        });

        // Refresh Button
        refreshButton.setOnAction(event -> {
            loadStatistics();
            loadPesananData();
            AlertUtil.showSuccess("Berhasil", "Data berhasil di-refresh!");
        });

        // Export Button (placeholder)
        exportButton.setOnAction(event -> {
            AlertUtil.showInfo("Export", "Fitur export akan segera hadir!");
        });
    }

    private void setupPagination() {
        // Items Per Page
        itemsPerPageComboBox.setValue("25");
        itemsPerPageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            itemsPerPage = Integer.parseInt(newValue);
            currentPage = 1;
            loadPesananData();
        });

        // Previous Page Button
        prevPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                currentPage--;
                loadPesananData();
            }
        });

        // Next Page Button
        nextPageButton.setOnAction(event -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadPesananData();
            }
        });
    }

    private void loadJenisLayanan() {
        ObservableList<String> layananList = FXCollections.observableArrayList();
        layananList.add("Semua Layanan");
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nama_layanan FROM jenis_layanan WHERE is_active = 1")) {

            while (rs.next()) {
                layananList.add(rs.getString("nama_layanan"));
            }
            filterLayananComboBox.setItems(layananList);
            filterLayananComboBox.setValue("Semua Layanan");

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data layanan.");
        }
    }

    private void loadStatistics() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Total Pesanan
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM pesanan");
            if (rs.next()) {
                totalPesananLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Pending
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM pesanan WHERE status = 'Pending'");
            if (rs.next()) {
                pendingPesananLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Proses
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM pesanan WHERE status = 'Proses'");
            if (rs.next()) {
                prosesPesananLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Selesai
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM pesanan WHERE status = 'Selesai'");
            if (rs.next()) {
                selesaiPesananLabel.setText(String.valueOf(rs.getInt("total")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat statistik.");
        }
    }

    private void loadPesananData() {
        pesananList.clear();

        StringBuilder query = new StringBuilder("SELECT * FROM pesanan WHERE 1=1");

        // Apply Search Filter
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            query.append(" AND (nama_pelanggan LIKE '%").append(searchText).append("%'");
            query.append(" OR no_telepon LIKE '%").append(searchText).append("%')");
        }

        // Apply Status Filter
        String statusFilter = filterStatusComboBox.getValue();
        if (statusFilter != null && !statusFilter.equals("Semua Status")) {
            query.append(" AND status = '").append(statusFilter).append("'");
        }

        // Apply Layanan Filter
        String layananFilter = filterLayananComboBox.getValue();
        if (layananFilter != null && !layananFilter.equals("Semua Layanan")) {
            query.append(" AND jenis_layanan = '").append(layananFilter).append("'");
        }

        query.append(" ORDER BY tanggal_pesanan DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query.toString())) {

            while (rs.next()) {
                Pesanan pesanan = new Pesanan();
                pesanan.setIdPesanan(rs.getInt("id_pesanan"));
                pesanan.setNamaPelanggan(rs.getString("nama_pelanggan"));
                pesanan.setNoTelepon(rs.getString("no_telepon"));
                pesanan.setEmail(rs.getString("email"));
                pesanan.setJenisLayanan(rs.getString("jenis_layanan"));
                pesanan.setJumlah(rs.getInt("jumlah"));
                pesanan.setTotalHarga(rs.getDouble("total_harga"));
                pesanan.setStatus(rs.getString("status"));
                pesanan.setTanggalPesanan(rs.getTimestamp("tanggal_pesanan").toLocalDateTime());

                pesananList.add(pesanan);
            }

            pesananTableView.setItems(pesananList);
            
            // Update Result Count
            resultCountLabel.setText("(" + pesananList.size() + " pesanan)");

            // Update Pagination
            updatePaginationControls();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data pesanan.");
        }
    }

    private void updatePaginationControls() {
        totalPages = (int) Math.ceil((double) pesananList.size() / itemsPerPage);
        pageInfoLabel.setText(currentPage + " / " + Math.max(1, totalPages));

        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    private void handleEditPesanan(Pesanan pesanan) {
        AlertUtil.showInfo("Edit Pesanan", "Edit pesanan: " + pesanan.getNamaPelanggan() + "\n(Fitur ini akan segera hadir)");
        // TODO: Implement edit functionality
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
                    loadStatistics();
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
}
