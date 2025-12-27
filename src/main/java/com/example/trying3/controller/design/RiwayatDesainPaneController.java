package com.example.trying3.controller.design;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.util.AlertUtil;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RiwayatDesainPaneController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button btnEkspor;
    @FXML private VBox riwayatContainer;
    @FXML private Label emptyLabel;

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
     * Load data riwayat desain dari database
     * JOIN ke tabel pelanggan untuk mendapatkan nama dan email
     */
    private void loadDataFromDatabase() {
        masterData.clear();

        // FIX: JOIN ke tabel pelanggan untuk mendapatkan nama dan email
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
                        totalBiaya
                ));
            }

            System.out.println("✅ Loaded " + masterData.size() + " riwayat desain dari database");

        } catch (SQLException e) {
            System.err.println("❌ Error loading riwayat desain: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String mapStatusDesain(String statusDesain) {
        if (statusDesain == null) return "Unknown";
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
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = formatter.format(amount);
        return formatted.replace(",00", "").replace("Rp", "Rp ");
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getSelectionModel().getSelectedItem();

        ObservableList<RiwayatDesain> filteredList = masterData.stream()
                .filter(riwayat -> {
                    boolean matchesSearch = (
                            riwayat.getIdDesain().toLowerCase().contains(searchText) ||
                                    riwayat.getNamaPelanggan().toLowerCase().contains(searchText) ||
                                    riwayat.getEmail().toLowerCase().contains(searchText) ||
                                    riwayat.getJenisLayanan().toLowerCase().contains(searchText) ||
                                    riwayat.getDesigner().toLowerCase().contains(searchText)
                    );

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
        card.setStyle("-fx-background-color: white; -fx-padding: 15 20; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        card.setAlignment(Pos.CENTER_LEFT);

        // 1. Kolom: ID Desain & Nama Pelanggan
        VBox pelangganBox = new VBox(3);
        Label idLabel = new Label(riwayat.getIdDesain());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 13px;");
        Label namaLabel = new Label(riwayat.getNamaPelanggan());
        namaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        namaLabel.setWrapText(true);
        namaLabel.setMaxWidth(150);
        pelangganBox.getChildren().addAll(idLabel, namaLabel);
        pelangganBox.setPrefWidth(160);

        // 2. Kolom: Email
        VBox emailBox = new VBox(3);
        Label emailTitle = new Label("Email");
        emailTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label emailValue = new Label(riwayat.getEmail());
        emailValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        emailValue.setWrapText(true);
        emailValue.setMaxWidth(150);
        emailBox.getChildren().addAll(emailTitle, emailValue);
        emailBox.setPrefWidth(160);

        // 3. Kolom: Jenis Layanan
        VBox layananBox = new VBox(3);
        Label layananTitle = new Label("Jenis Layanan");
        layananTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label layananValue = new Label(riwayat.getJenisLayanan());
        layananValue.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layananValue.setWrapText(true);
        layananValue.setMaxWidth(120);
        layananBox.getChildren().addAll(layananTitle, layananValue);
        layananBox.setPrefWidth(130);

        // 4. Kolom: Desainer
        VBox designerBox = new VBox(3);
        Label designerTitle = new Label("Desainer");
        designerTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label designerName = new Label(riwayat.getDesigner());
        designerName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        designerBox.getChildren().addAll(designerTitle, designerName);
        designerBox.setPrefWidth(80);

        // 5. Kolom: Tanggal Selesai
        VBox dateBox = new VBox(3);
        Label dateTitle = new Label("Selesai Pada");
        dateTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label dateValue = new Label(riwayat.getTanggalSelesai());
        dateValue.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(dateTitle, dateValue);
        dateBox.setPrefWidth(90);

        // 6. Kolom: Status Badge
        VBox statusBox = new VBox(3);
        Label statusTitle = new Label("Status");
        statusTitle.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        Label statusBadge = new Label(riwayat.getStatus());
        statusBadge.setStyle(getStatusStyle(riwayat.getStatus()));
        statusBox.getChildren().addAll(statusTitle, statusBadge);
        statusBox.setPrefWidth(90);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 7. Kolom: Harga & Button
        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label hargaLabel = new Label(riwayat.getHarga());
        hargaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 14px;");

        Button btnDetail = new Button("Lihat Detail");
        btnDetail.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;");
        btnDetail.setOnAction(e -> showDetail(riwayat));

        rightBox.getChildren().addAll(hargaLabel, btnDetail);

        card.getChildren().addAll(pelangganBox, emailBox, layananBox, designerBox, dateBox, statusBox, spacer, rightBox);

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
                        "Total Biaya: " + riwayat.getHarga()
        );
        alert.showAndWait();
    }

    // =====================================================
    // FITUR EKSPOR KE EXCEL
    // =====================================================

    @FXML
    private void handleEkspor() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getSelectionModel().getSelectedItem();

        ObservableList<RiwayatDesain> dataToExport = masterData.stream()
                .filter(riwayat -> {
                    boolean matchesSearch = (
                            riwayat.getIdDesain().toLowerCase().contains(searchText) ||
                                    riwayat.getNamaPelanggan().toLowerCase().contains(searchText) ||
                                    riwayat.getEmail().toLowerCase().contains(searchText) ||
                                    riwayat.getJenisLayanan().toLowerCase().contains(searchText) ||
                                    riwayat.getDesigner().toLowerCase().contains(searchText)
                    );
                    boolean matchesStatus = true;
                    if (selectedStatus != null && !selectedStatus.equals("Semua")) {
                        matchesStatus = riwayat.getStatus().equalsIgnoreCase(selectedStatus);
                    }
                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        if (dataToExport.isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Tidak ada data untuk diekspor.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan File Excel");
        fileChooser.setInitialFileName("Riwayat_Desain_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        Stage stage = (Stage) riwayatContainer.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            exportToExcel(dataToExport, file);
        }
    }

    private void exportToExcel(ObservableList<RiwayatDesain> data, File file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Riwayat Desain");

            // ========== STYLES ==========
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("\"Rp\" #,##0"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle statusSelesaiStyle = createStatusStyle(workbook, dataStyle, IndexedColors.LIGHT_GREEN);
            CellStyle statusDitolakStyle = createStatusStyle(workbook, dataStyle, IndexedColors.CORAL);
            CellStyle statusLainnyaStyle = createStatusStyle(workbook, dataStyle, IndexedColors.LIGHT_YELLOW);

            // ========== TITLE ==========
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("LAPORAN RIWAYAT DESAIN");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            Row subtitleRow = sheet.createRow(1);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Diekspor pada: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"))));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            // ========== HEADER ==========
            Row headerRow = sheet.createRow(3);
            String[] headers = {"No", "ID Desain", "Nama Pelanggan", "Email", "Jenis Layanan", "Desainer", "Tanggal Selesai", "Status", "Total Biaya"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ========== DATA ==========
            int rowNum = 4;
            int no = 1;
            double grandTotal = 0;

            for (RiwayatDesain riwayat : data) {
                Row row = sheet.createRow(rowNum++);

                Cell cellNo = row.createCell(0);
                cellNo.setCellValue(no++);
                cellNo.setCellStyle(dataStyle);

                Cell cellId = row.createCell(1);
                cellId.setCellValue(riwayat.getIdDesain());
                cellId.setCellStyle(dataStyle);

                Cell cellNama = row.createCell(2);
                cellNama.setCellValue(riwayat.getNamaPelanggan());
                cellNama.setCellStyle(dataStyle);

                Cell cellEmail = row.createCell(3);
                cellEmail.setCellValue(riwayat.getEmail());
                cellEmail.setCellStyle(dataStyle);

                Cell cellLayanan = row.createCell(4);
                cellLayanan.setCellValue(riwayat.getJenisLayanan());
                cellLayanan.setCellStyle(dataStyle);

                Cell cellDesigner = row.createCell(5);
                cellDesigner.setCellValue(riwayat.getDesigner());
                cellDesigner.setCellStyle(dataStyle);

                Cell cellTanggal = row.createCell(6);
                cellTanggal.setCellValue(riwayat.getTanggalSelesai());
                cellTanggal.setCellStyle(dataStyle);

                Cell cellStatus = row.createCell(7);
                cellStatus.setCellValue(riwayat.getStatus());
                switch (riwayat.getStatus().toLowerCase()) {
                    case "selesai", "disetujui" -> cellStatus.setCellStyle(statusSelesaiStyle);
                    case "ditolak" -> cellStatus.setCellStyle(statusDitolakStyle);
                    default -> cellStatus.setCellStyle(statusLainnyaStyle);
                }

                Cell cellBiaya = row.createCell(8);
                cellBiaya.setCellValue(riwayat.getTotalBiayaNumeric());
                cellBiaya.setCellStyle(currencyStyle);

                grandTotal += riwayat.getTotalBiayaNumeric();
            }

            // ========== GRAND TOTAL ==========
            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabelCell = totalRow.createCell(7);
            totalLabelCell.setCellValue("GRAND TOTAL:");
            CellStyle totalLabelStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLabelCell.setCellStyle(totalLabelStyle);

            Cell totalValueCell = totalRow.createCell(8);
            totalValueCell.setCellValue(grandTotal);
            CellStyle totalValueStyle = workbook.createCellStyle();
            totalValueStyle.cloneStyleFrom(currencyStyle);
            totalValueStyle.setFont(totalFont);
            totalValueCell.setCellStyle(totalValueStyle);

            // ========== AUTO SIZE COLUMNS ==========
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 500);
            }

            sheet.setColumnWidth(2, Math.max(sheet.getColumnWidth(2), 5000)); // Nama Pelanggan
            sheet.setColumnWidth(3, Math.max(sheet.getColumnWidth(3), 6000)); // Email
            sheet.setColumnWidth(4, Math.max(sheet.getColumnWidth(4), 5000)); // Jenis Layanan
            sheet.setColumnWidth(8, Math.max(sheet.getColumnWidth(8), 4000)); // Total Biaya

            // ========== SAVE FILE ==========
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            AlertUtil.showInfo("Berhasil", "Data berhasil diekspor ke:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Gagal mengekspor data: " + e.getMessage());
        }
    }

    private CellStyle createStatusStyle(Workbook workbook, CellStyle baseStyle, IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // =====================================================
    // Inner class untuk model data Riwayat Desain
    // =====================================================
    public static class RiwayatDesain {
        private final String idDesain;
        private final String namaPelanggan;
        private final String email;
        private final String jenisLayanan;
        private final String designer;
        private final String status;
        private final String tanggalSelesai;
        private final String harga;
        private final double totalBiayaNumeric;

        public RiwayatDesain(String idDesain, String namaPelanggan, String email, String jenisLayanan,
                             String designer, String status, String tanggalSelesai, String harga, double totalBiayaNumeric) {
            this.idDesain = idDesain;
            this.namaPelanggan = namaPelanggan;
            this.email = email;
            this.jenisLayanan = jenisLayanan;
            this.designer = designer;
            this.status = status;
            this.tanggalSelesai = tanggalSelesai;
            this.harga = harga;
            this.totalBiayaNumeric = totalBiayaNumeric;
        }

        public String getIdDesain() { return idDesain; }
        public String getIdPesanan() { return idDesain; } // Alias
        public String getNamaPelanggan() { return namaPelanggan; }
        public String getEmail() { return email; }
        public String getJenisLayanan() { return jenisLayanan; }
        public String getDesigner() { return designer; }
        public String getStatus() { return status; }
        public String getTanggalSelesai() { return tanggalSelesai; }
        public String getHarga() { return harga; }
        public String getTotal() { return harga; }
        public double getTotalBiayaNumeric() { return totalBiayaNumeric; }
    }
}