package com.example.trying3.controller.design;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.RiwayatDesain;
import com.example.trying3.util.RiwayatDesainExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller untuk halaman Riwayat Desain.
 * Menampilkan daftar riwayat desain yang telah selesai atau ditolak dengan fitur filter dan ekspor.
 */

public class RiwayatDesainPaneController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Button btnEkspor;
    @FXML
    private VBox riwayatContainer;
    @FXML
    private Label emptyLabel;

    private ObservableList<RiwayatDesain> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDataFromDatabase();
        renderRiwayat(masterData);

        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldStatus, newStatus) -> applyFilters());

        if (btnEkspor != null) {
            btnEkspor.setOnAction(e -> handleEkspor());
        }
    }

    /**
     * Load data riwayat desain dari database dengan JOIN ke tabel pelanggan.
     */
    private void loadDataFromDatabase() {
        masterData.clear();

        String sql = """
                    SELECT
                        d.id_desain,
                        p.nomor_pesanan,
                        pel.nama AS nama_pelanggan,
                        pel.email AS email_pelanggan,
                        jl.nama_layanan AS jenis_layanan,
                        u.nama_lengkap AS designer,
                        sd.nama_status AS status_desain,
                        d.tanggal_disetujui,
                        p.total_biaya
                    FROM desain d
                    JOIN pesanan p ON d.id_pesanan = p.id_pesanan
                    JOIN pelanggan pel ON p.id_pelanggan = pel.id_pelanggan
                    JOIN user u ON d.id_designer = u.id_user
                    JOIN status_desain sd ON d.id_status_desain = sd.id_status_desain
                    LEFT JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                    LEFT JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                    WHERE d.id_status_desain IN (5, 6)
                    ORDER BY d.tanggal_disetujui DESC, d.id_desain DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String idDesain = "DES-" + String.format("%03d", rs.getInt("id_desain"));
                String namaPelanggan = rs.getString("nama_pelanggan");
                String email = rs.getString("email_pelanggan");
                String jenisLayanan = rs.getString("jenis_layanan");
                String designer = rs.getString("designer");
                String statusDesain = rs.getString("status_desain");
                String displayStatus = mapStatusDesain(statusDesain);

                Timestamp tanggalDisetujui = rs.getTimestamp("tanggal_disetujui");
                String tanggal = tanggalDisetujui != null
                        ? tanggalDisetujui.toLocalDateTime().toLocalDate().toString()
                        : "-";

                double totalBiaya = rs.getDouble("total_biaya");
                String harga = formatRupiah(totalBiaya);

                masterData.add(new RiwayatDesain(
                        idDesain,
                        namaPelanggan != null ? namaPelanggan : "-",
                        email != null ? email : "-",
                        jenisLayanan != null ? jenisLayanan : "-",
                        designer,
                        displayStatus,
                        tanggal,
                        harga,
                        totalBiaya));
            }

            System.out.println("✅ Loaded " + masterData.size() + " riwayat desain dari database");

        } catch (SQLException e) {
            System.err.println("❌ Error loading riwayat desain: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String mapStatusDesain(String statusDesain) {
        if (statusDesain == null)
            return "Unknown";
        return switch (statusDesain) {
            case "Disetujui (ACC)" -> "Disetujui";
            case "Ditolak" -> "Ditolak";
            case "Dalam Pengerjaan" -> "Dalam Proses";
            case "Menunggu Approval" -> "Menunggu";
            case "Perlu Revisi" -> "Revisi";
            default -> "Selesai";
        };
    }

    private String formatRupiah(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.of("id", "ID"));
        String formatted = formatter.format(amount);
        return formatted.replace(",00", "").replace("Rp", "Rp ");
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getSelectionModel().getSelectedItem();

        ObservableList<RiwayatDesain> filteredList = masterData.stream()
                .filter(riwayat -> {
                    boolean matchesSearch = (riwayat.getIdDesain().toLowerCase().contains(searchText) ||
                            riwayat.getNamaPelanggan().toLowerCase().contains(searchText) ||
                            riwayat.getEmail().toLowerCase().contains(searchText) ||
                            riwayat.getJenisLayanan().toLowerCase().contains(searchText) ||
                            riwayat.getDesigner().toLowerCase().contains(searchText));

                    boolean matchesStatus = true;
                    if (selectedStatus != null && !selectedStatus.equals("Semua")) {
                        matchesStatus = riwayat.getStatus().equalsIgnoreCase(selectedStatus);
                    }

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        renderRiwayat(filteredList);
    }

    private void renderRiwayat(ObservableList<RiwayatDesain> list) {
        riwayatContainer.getChildren().clear();

        if (list.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
            }
            return;
        }

        if (emptyLabel != null) {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
        }

        for (RiwayatDesain riwayat : list) {
            riwayatContainer.getChildren().add(createRiwayatCard(riwayat));
        }
    }

    private HBox createRiwayatCard(RiwayatDesain riwayat) {
        HBox card = new HBox(15);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15 20; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox pelangganBox = new VBox(3);
        Label idLabel = new Label(riwayat.getIdDesain());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 13px;");
        Label namaLabel = new Label(riwayat.getNamaPelanggan());
        namaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        namaLabel.setWrapText(true);
        namaLabel.setMaxWidth(150);
        pelangganBox.getChildren().addAll(idLabel, namaLabel);
        pelangganBox.setPrefWidth(160);

        VBox emailBox = new VBox(3);
        Label emailTitle = new Label("Email");
        emailTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label emailValue = new Label(riwayat.getEmail());
        emailValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        emailValue.setWrapText(true);
        emailValue.setMaxWidth(150);
        emailBox.getChildren().addAll(emailTitle, emailValue);
        emailBox.setPrefWidth(160);

        VBox layananBox = new VBox(3);
        Label layananTitle = new Label("Jenis Layanan");
        layananTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label layananValue = new Label(riwayat.getJenisLayanan());
        layananValue.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layananValue.setWrapText(true);
        layananValue.setMaxWidth(120);
        layananBox.getChildren().addAll(layananTitle, layananValue);
        layananBox.setPrefWidth(130);

        VBox designerBox = new VBox(3);
        Label designerTitle = new Label("Desainer");
        designerTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label designerName = new Label(riwayat.getDesigner());
        designerName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        designerBox.getChildren().addAll(designerTitle, designerName);
        designerBox.setPrefWidth(80);

        VBox dateBox = new VBox(3);
        Label dateTitle = new Label("Selesai Pada");
        dateTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label dateValue = new Label(riwayat.getTanggalSelesai());
        dateValue.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(dateTitle, dateValue);
        dateBox.setPrefWidth(90);

        VBox statusBox = new VBox(3);
        Label statusTitle = new Label("Status");
        statusTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label statusBadge = new Label(riwayat.getStatus());
        statusBadge.setStyle(getStatusStyle(riwayat.getStatus()));
        statusBox.getChildren().addAll(statusTitle, statusBadge);
        statusBox.setPrefWidth(90);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label hargaLabel = new Label(riwayat.getHarga());
        hargaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 14px;");

        Button btnDetail = new Button("Lihat Detail");
        btnDetail.setStyle(
                "-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
        btnDetail.setOnAction(e -> showDetail(riwayat));

        rightBox.getChildren().addAll(hargaLabel, btnDetail);

        card.getChildren().addAll(pelangganBox, emailBox, layananBox, designerBox, dateBox, statusBox, spacer,
                rightBox);

        return card;
    }

    private String getStatusStyle(String status) {
        String baseStyle = "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        return switch (status.toLowerCase()) {
            case "selesai", "disetujui" -> baseStyle + "-fx-background-color: #d4edda; -fx-text-fill: #155724;";
            case "ditolak" -> baseStyle + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
            case "revisi" -> baseStyle + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;";
            default -> baseStyle + "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41;";
        };
    }

    private void showDetail(RiwayatDesain riwayat) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Riwayat Desain");
        alert.setHeaderText(riwayat.getIdDesain() + " - " + riwayat.getNamaPelanggan());
        alert.setContentText(
                "Email: " + riwayat.getEmail() + "\n" +
                        "Jenis Layanan: " + riwayat.getJenisLayanan() + "\n" +
                        "Desainer: " + riwayat.getDesigner() + "\n" +
                        "Status: " + riwayat.getStatus() + "\n" +
                        "Tanggal Selesai: " + riwayat.getTanggalSelesai() + "\n" +
                        "Total Biaya: " + riwayat.getHarga());
        alert.showAndWait();
    }

    @FXML
    private void handleEkspor() {
        ObservableList<RiwayatDesain> dataToExport = getFilteredData();
        Stage stage = (Stage) riwayatContainer.getScene().getWindow();
        RiwayatDesainExporter.exportWithDialog(dataToExport, stage);
    }

    /**
     * Mendapatkan data yang sudah difilter berdasarkan search dan status.
     */
    private ObservableList<RiwayatDesain> getFilteredData() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getSelectionModel().getSelectedItem();

        return masterData.stream()
                .filter(riwayat -> {
                    boolean matchesSearch = (riwayat.getIdDesain().toLowerCase().contains(searchText) ||
                            riwayat.getNamaPelanggan().toLowerCase().contains(searchText) ||
                            riwayat.getEmail().toLowerCase().contains(searchText) ||
                            riwayat.getJenisLayanan().toLowerCase().contains(searchText) ||
                            riwayat.getDesigner().toLowerCase().contains(searchText));
                    boolean matchesStatus = true;
                    if (selectedStatus != null && !selectedStatus.equals("Semua")) {
                        matchesStatus = riwayat.getStatus().equalsIgnoreCase(selectedStatus);
                    }
                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
}