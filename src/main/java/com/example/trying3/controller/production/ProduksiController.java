package com.example.trying3.controller.production;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProduksiController implements Initializable {

    // --- FXML ELEMENTS ---
    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox orderListContainer;

    @FXML private VBox kendalaFormContainer;
    @FXML private Label lblKendalaTitle;
    @FXML private TextArea txtKendala;

    // --- DATA ---
    private List<ProductionOrder> orderList = new ArrayList<>();
    private ProductionOrder selectedOrderForIssue;
    private PesananDAO pesananDAO;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pesananDAO = new PesananDAO();
        if(kendalaFormContainer != null) {
            kendalaFormContainer.setVisible(false);
            kendalaFormContainer.setManaged(false);
        }
        loadDataFromDatabase();
        refreshDashboard();
    }

    private void loadDataFromDatabase() {
        orderList.clear();
        List<Pesanan> dbOrders = pesananDAO.getPesananForProduction();

        for (Pesanan p : dbOrders) {
            String fileDesain = (p.getFileDesainPath() != null) ? p.getFileDesainPath() : "";
            String namaPelanggan = (p.getNamaPelanggan() != null) ? p.getNamaPelanggan() : "Unknown";
            String tgl = (p.getTanggalPesanan() != null) ? p.getTanggalPesanan().toLocalDate().toString() : "-";
            String catatan = (p.getCatatan() != null) ? p.getCatatan() : "";

            orderList.add(new ProductionOrder(
                    String.valueOf(p.getIdPesanan()), namaPelanggan, tgl,
                    p.getJenisLayanan(), p.getJumlah(), p.getSpesifikasi(),
                    fileDesain, p.getStatus(), catatan
            ));
        }
    }

    private void refreshDashboard() {
        long waiting = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Antrian Produksi")).count();
        long inProgress = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Sedang Diproduksi")).count();
        // FIX: Hitung yang sudah Siap Dikirim juga sebagai "completed" untuk statistik
        long completed = orderList.stream().filter(o ->
                o.getStatus().equalsIgnoreCase("Siap Dikirim") ||
                        o.getStatus().equalsIgnoreCase("Selesai")
        ).count();

        if (waitingLabel != null) waitingLabel.setText(String.valueOf(waiting));
        if (inProgressLabel != null) inProgressLabel.setText(String.valueOf(inProgress));
        if (completedLabel != null) completedLabel.setText(String.valueOf(completed));

        renderOrderList();
    }

    private void renderOrderList() {
        if (orderListContainer != null) {
            orderListContainer.getChildren().clear();

            // FIX: Filter untuk tampilkan yang belum Siap Dikirim/Selesai
            List<ProductionOrder> activeOrders = orderList.stream()
                    .filter(o -> !o.getStatus().equalsIgnoreCase("Siap Dikirim") &&
                            !o.getStatus().equalsIgnoreCase("Selesai"))
                    .collect(Collectors.toList());

            if (activeOrders.isEmpty()) {
                Label emptyLabel = new Label("Tidak ada antrian produksi saat ini.");
                emptyLabel.getStyleClass().add("muted");
                emptyLabel.setPadding(new Insets(10));
                orderListContainer.getChildren().add(emptyLabel);
                return;
            }

            for (ProductionOrder order : activeOrders) {
                orderListContainer.getChildren().add(createOrderCard(order));
            }
        }
    }

    // --- UI CARD (Style CSS Designer) ---
    private VBox createOrderCard(ProductionOrder order) {
        VBox card = new VBox(15);
        card.getStyleClass().add("order-detail-card");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label("PO-" + order.getId());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1f1f1f;");
        Label lblCust = new Label(order.getCustomerName() + " • " + order.getDate());
        lblCust.getStyleClass().add("muted");
        titleBox.getChildren().addAll(lblId, lblCust);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Pill
        Label lblStatus = new Label(order.getStatus());
        lblStatus.getStyleClass().add("status-badge");
        if (order.getStatus().equalsIgnoreCase("Antrian Produksi")) {
            lblStatus.getStyleClass().add("status-antrian-produksi");
        } else if (order.getStatus().equalsIgnoreCase("Sedang Diproduksi")) {
            lblStatus.getStyleClass().add("status-sedang-diproduksi");
        } else {
            lblStatus.getStyleClass().add("status-produksi-selesai");
        }
        header.getChildren().addAll(titleBox, spacer, lblStatus);

        // Details
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        addDetailRow(grid, 0, "Layanan", order.getService());
        addDetailRow(grid, 1, "Jumlah", order.getQuantity() + " pcs");
        addDetailRow(grid, 2, "Spesifikasi", order.getSpecs());

        if (!order.getCatatan().isEmpty()) {
            Label l = new Label("Catatan");
            l.getStyleClass().add("field-label");
            l.setStyle("-fx-text-fill: #e74c3c;");

            Label v = new Label(order.getCatatan());
            v.getStyleClass().add("field-value");
            v.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            v.setWrapText(true);
            grid.add(l, 0, 3); grid.add(v, 1, 3);
        }

        // File Link
        HBox fileBox = new HBox(5);
        if (!order.getFileDesainPath().isEmpty()) {
            Label lblFile = new Label("📂 File Desain: " + new File(order.getFileDesainPath()).getName());
            lblFile.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: true;");
            lblFile.setOnMouseClicked(e -> openFile(order.getFileDesainPath()));
            fileBox.getChildren().add(lblFile);
        } else {
            Label lblFile = new Label("⚠️ Belum ada file desain");
            lblFile.getStyleClass().add("muted");
            lblFile.setStyle("-fx-font-style: italic;");
            fileBox.getChildren().add(lblFile);
        }

        // Action Buttons
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnMain = new Button();
        btnMain.getStyleClass().add("button-primary");

        if (order.getStatus().equalsIgnoreCase("Antrian Produksi")) {
            btnMain.setText("Mulai Produksi");
            btnMain.setOnAction(e -> handleStartProduction(order));
        } else {
            btnMain.setText("✅ Selesai Produksi");
            btnMain.setOnAction(e -> handleFinishProduction(order));
        }

        Button btnIssue = new Button("⚠️ Laporkan Kendala");
        btnIssue.getStyleClass().add("button-danger");
        btnIssue.setOnAction(e -> showKendalaForm(order));

        Button btnJobSheet = new Button("📋 Cetak Job Sheet");
        btnJobSheet.getStyleClass().add("button-secondary");
        btnJobSheet.setOnAction(e -> generateJobSheet(order));

        actions.getChildren().addAll(btnMain, btnIssue, btnJobSheet);

        card.getChildren().addAll(header, grid, fileBox, actions);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        Label v = new Label(value != null ? value : "-");
        v.getStyleClass().add("field-value");
        v.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private void generateJobSheet(ProductionOrder order) {
        try {
            // Buat folder jika belum ada
            File dir = new File("job_sheets");
            if (!dir.exists()) dir.mkdirs();

            // Nama file unik
            String fileName = "JobSheet_PO-" + order.getId() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            File file = new File(dir, fileName);

            // Tulis isi Job Sheet
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("============================================\n");
                writer.write("              JOB SHEET PRODUKSI            \n");
                writer.write("============================================\n\n");
                writer.write("No. Pesanan   : PO-" + order.getId() + "\n");
                writer.write("Pelanggan     : " + order.getCustomerName() + "\n");
                writer.write("Tanggal Order : " + order.getDate() + "\n");
                writer.write("--------------------------------------------\n");
                writer.write("Layanan       : " + order.getService() + "\n");
                writer.write("Jumlah        : " + order.getQuantity() + " pcs\n");
                writer.write("Spesifikasi   : " + order.getSpecs() + "\n");
                if (!order.getCatatan().isEmpty()) {
                    writer.write("Catatan       : " + order.getCatatan() + "\n");
                }
                writer.write("--------------------------------------------\n");
                writer.write("File Desain   : " + (order.getFileDesainPath().isEmpty() ? "Belum ada" : order.getFileDesainPath()) + "\n");
                writer.write("\n============================================\n");
                writer.write("Dicetak pada  : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("============================================\n");
            }

            // Buka file otomatis
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Info", "File berhasil dibuat: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuat Job Sheet: " + e.getMessage());
        }
    }

    // --- UTILS ---
    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) Desktop.getDesktop().open(file);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleStartProduction(ProductionOrder order) {
        pesananDAO.updateStatus(Integer.parseInt(order.getId()), "Sedang Diproduksi");
        loadDataFromDatabase(); refreshDashboard();
    }

    /**
     * FIX: Selesai Produksi -> Status "Siap Dikirim" (bukan "Selesai")
     * Status "Selesai" akan diubah oleh Admin setelah barang dikirim/diambil
     */
    private void handleFinishProduction(ProductionOrder order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Selesaikan produksi pesanan ini?\n\nStatus akan berubah menjadi 'Siap Dikirim' dan Admin akan mendapat notifikasi.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Konfirmasi Selesai Produksi");
        alert.setHeaderText("PO-" + order.getId() + " - " + order.getCustomerName());

        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                // FIX: Update ke "Siap Dikirim" bukan "Selesai"
                pesananDAO.updateStatus(Integer.parseInt(order.getId()), "Siap Dikirim");
                showAlert("Berhasil", "Produksi selesai! Pesanan siap untuk dikirim.\nAdmin akan mendapat notifikasi.");
                loadDataFromDatabase();
                refreshDashboard();
            }
        });
    }

    private void showKendalaForm(ProductionOrder order) {
        this.selectedOrderForIssue = order;
        if(lblKendalaTitle != null) lblKendalaTitle.setText("Kendala PO-" + order.getId());
        if(txtKendala != null) txtKendala.clear();
        if(kendalaFormContainer != null) {
            kendalaFormContainer.setVisible(true);
            kendalaFormContainer.setManaged(true);
        }
    }

    @FXML
    private void submitKendala() {
        // 1. Validasi Input
        String kendala = txtKendala.getText();
        if (kendala == null || kendala.trim().isEmpty() || selectedOrderForIssue == null) {
            showAlert("Peringatan", "Mohon isi deskripsi kendala dan pilih pesanan.");
            return;
        }

        // 2. Ambil ID Pesanan dari String
        int pesananId = 0;
        try {
            String displayId = selectedOrderForIssue.getId();
            String numberOnly = displayId.substring(displayId.lastIndexOf("-") + 1);
            pesananId = Integer.parseInt(numberOnly);
        } catch (Exception e) {
            // Jika tidak ada strip, coba parse langsung
            try {
                pesananId = Integer.parseInt(selectedOrderForIssue.getId());
            } catch (Exception ex) {
                showAlert("Error Data", "Format ID Pesanan tidak valid: " + selectedOrderForIssue.getId());
                return;
            }
        }

        // 3. Simpan ke Database
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // A. Cek apakah sudah ada data di tabel 'produksi'
            int idProduksi = 0;
            String sqlCek = "SELECT id_produksi FROM produksi WHERE id_pesanan = ?";
            try (PreparedStatement psCek = conn.prepareStatement(sqlCek)) {
                psCek.setInt(1, pesananId);
                try (ResultSet rs = psCek.executeQuery()) {
                    if (rs.next()) {
                        idProduksi = rs.getInt("id_produksi");
                    }
                }
            }

            // B. Jika belum ada di tabel produksi, buat dulu
            if (idProduksi == 0) {
                String sqlInsertProd = "INSERT INTO produksi (id_pesanan, id_operator, tanggal_mulai, status_produksi) VALUES (?, ?, NOW(), 'terkendala')";
                try (PreparedStatement psProd = conn.prepareStatement(sqlInsertProd, Statement.RETURN_GENERATED_KEYS)) {
                    psProd.setInt(1, pesananId);
                    int currentUserId = com.example.trying3.util.SessionManager.getInstance().getCurrentUserId();
                    psProd.setInt(2, currentUserId);
                    psProd.executeUpdate();
                    try (ResultSet rsKey = psProd.getGeneratedKeys()) {
                        if (rsKey.next()) idProduksi = rsKey.getInt(1);
                    }
                }
            } else {
                String sqlUpdateStatus = "UPDATE produksi SET status_produksi = 'terkendala' WHERE id_produksi = ?";
                try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdateStatus)) {
                    psUpd.setInt(1, idProduksi);
                    psUpd.executeUpdate();
                }
            }

            // C. Masukkan laporan ke tabel 'kendala_produksi'
            String sqlKendala = "INSERT INTO kendala_produksi (id_produksi, deskripsi, status, tanggal_lapor, dilaporkan_oleh) VALUES (?, ?, 'open', NOW(), ?)";
            try (PreparedStatement psKendala = conn.prepareStatement(sqlKendala)) {
                psKendala.setInt(1, idProduksi);
                psKendala.setString(2, kendala);
                int currentUserId = com.example.trying3.util.SessionManager.getInstance().getCurrentUserId();
                psKendala.setInt(3, currentUserId);
                psKendala.executeUpdate();
            }

            conn.commit();
            showAlert("Berhasil", "Kendala berhasil dilaporkan ke Admin.");

            // Tutup form dan reset
            kendalaFormContainer.setVisible(false);
            kendalaFormContainer.setManaged(false);
            txtKendala.clear();
            selectedOrderForIssue = null;

            // Refresh tampilan
            initialize(null, null);

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            showAlert("Gagal Database", "Error: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception ex) {}
        }
    }

    @FXML private void cancelKendala() {
        if(kendalaFormContainer != null) {
            kendalaFormContainer.setVisible(false);
            kendalaFormContainer.setManaged(false);
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(content);
        a.show();
    }

    public static class ProductionOrder {
        private String id, customerName, date, service;
        private int quantity;
        private String specs, fileDesainPath, status, catatan;
        public ProductionOrder(String id, String customerName, String date, String service, int quantity, String specs, String fileDesainPath, String status, String catatan) {
            this.id = id; this.customerName = customerName; this.date = date; this.service = service;
            this.quantity = quantity; this.specs = specs; this.fileDesainPath = fileDesainPath;
            this.status = status; this.catatan = catatan;
        }
        public String getId() { return id; }
        public String getCustomerName() { return customerName; }
        public String getDate() { return date; }
        public String getService() { return service; }
        public int getQuantity() { return quantity; }
        public String getSpecs() { return specs; }
        public String getFileDesainPath() { return fileDesainPath; }
        public String getStatus() { return status; }
        public String getCatatan() { return catatan; }
    }
}