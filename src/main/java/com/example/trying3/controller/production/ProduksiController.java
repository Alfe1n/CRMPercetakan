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
import java.io.FileWriter; // Import untuk tulis file TXT
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
        long completed = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Selesai")).count();

        if (waitingLabel != null) waitingLabel.setText(String.valueOf(waiting));
        if (inProgressLabel != null) inProgressLabel.setText(String.valueOf(inProgress));
        if (completedLabel != null) completedLabel.setText(String.valueOf(completed));

        renderOrderList();
    }

    private void renderOrderList() {
        if (orderListContainer != null) {
            orderListContainer.getChildren().clear();

            List<ProductionOrder> activeOrders = orderList.stream()
                    .filter(o -> !o.getStatus().equalsIgnoreCase("Selesai"))
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
        btnIssue.getStyleClass().add("button-secondary");
        btnIssue.setOnAction(e -> showKendalaForm(order));

        // [UPDATE] Tombol Print ke TXT
        Button btnPrint = new Button("🖨️ Print Job Sheet (TXT)");
        btnPrint.getStyleClass().add("button-secondary");
        btnPrint.setOnAction(e -> generateTxtJobSheet(order));

        actions.getChildren().addAll(btnMain, btnIssue, btnPrint);

        card.getChildren().addAll(header, grid, fileBox, actions);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");

        Label v = new Label(value);
        v.getStyleClass().add("field-value");
        v.setWrapText(true);

        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    // =====================================================================
    // FITUR PRINT KE TXT (NOTEPAD)
    // =====================================================================
    private void generateTxtJobSheet(ProductionOrder order) {
        // 1. Siapkan konten teks
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("               CRM PERCETAKAN - JOB SHEET                   \n");
        sb.append("============================================================\n");
        sb.append("Tanggal Cetak : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).append("\n");
        sb.append("Nomor Pesanan : PO-").append(order.getId()).append("\n");
        sb.append("------------------------------------------------------------\n\n");

        sb.append("DATA PELANGGAN:\n");
        sb.append("Nama          : ").append(order.getCustomerName()).append("\n");
        sb.append("Tanggal Masuk : ").append(order.getDate()).append("\n\n");

        sb.append("DETAIL PESANAN:\n");
        sb.append("Layanan       : ").append(order.getService()).append("\n");
        sb.append("Jumlah        : ").append(order.getQuantity()).append(" pcs\n");
        sb.append("Spesifikasi   :\n").append(order.getSpecs()).append("\n\n");

        if (!order.getCatatan().isEmpty()) {
            sb.append("CATATAN KENDALA:\n");
            sb.append(order.getCatatan()).append("\n\n");
        }

        sb.append("------------------------------------------------------------\n");
        sb.append("\n\n");
        sb.append("   Admin / CS                       Operator Produksi\n");
        sb.append("\n\n");
        sb.append(" (______________)                 (________________)\n");
        sb.append("============================================================\n");

        // 2. Buat file TXT
        try {
            // Buat folder khusus 'print_jobs' biar rapi
            File dir = new File("print_jobs");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "JobSheet_PO-" + order.getId() + ".txt";
            File file = new File(dir, fileName);

            // Tulis konten ke file
            FileWriter writer = new FileWriter(file);
            writer.write(sb.toString());
            writer.close();

            // 3. Buka file otomatis dengan Notepad
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

    private void handleFinishProduction(ProductionOrder order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Selesaikan pesanan ini?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                pesananDAO.updateStatus(Integer.parseInt(order.getId()), "Selesai");
                loadDataFromDatabase(); refreshDashboard();
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

        // 2. Ambil ID Pesanan dari String (Misal: "ORD-015" -> ambil 15)
        int pesananId = 0;
        try {
            String displayId = selectedOrderForIssue.getId(); // Format: "ORD-015"
            // Ambil angka setelah strip terakhir
            String numberOnly = displayId.substring(displayId.lastIndexOf("-") + 1);
            pesananId = Integer.parseInt(numberOnly);
        } catch (Exception e) {
            showAlert("Error Data", "Format ID Pesanan tidak valid: " + selectedOrderForIssue.getId());
            return;
        }

        // 3. Simpan ke Database (Tabel 'produksi' & 'kendala_produksi')
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Biar aman (Transaksi)

            // A. Cek apakah sudah ada data di tabel 'produksi' untuk pesanan ini?
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

            // B. Jika belum ada di tabel produksi, buat dulu (Insert otomatis)
            if (idProduksi == 0) {
                // Query dengan DUA tanda tanya (?, ?)
                String sqlInsertProd = "INSERT INTO produksi (id_pesanan, id_operator, tanggal_mulai, status_produksi) VALUES (?, ?, NOW(), 'terkendala')";

                try (PreparedStatement psProd = conn.prepareStatement(sqlInsertProd, Statement.RETURN_GENERATED_KEYS)) {

                    // Parameter 1: ID Pesanan (Sudah ada)
                    psProd.setInt(1, pesananId);

                    // --- TAMBAHKAN BAGIAN INI (Solusi Error) ---
                    // Ambil ID User yang sedang login
                    int currentUserId = com.example.trying3.util.SessionManager.getInstance().getCurrentUserId();

                    // Parameter 2: ID Operator (Ini yang tadinya lupa diisi)
                    psProd.setInt(2, currentUserId);
                    // -------------------------------------------

                    psProd.executeUpdate();

                    try (ResultSet rsKey = psProd.getGeneratedKeys()) {
                        if (rsKey.next()) idProduksi = rsKey.getInt(1);
                    }
                }
            } else {
                // Jika sudah ada, update statusnya jadi 'terkendala'
                String sqlUpdateStatus = "UPDATE produksi SET status_produksi = 'terkendala' WHERE id_produksi = ?";
                try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdateStatus)) {
                    psUpd.setInt(1, idProduksi);
                    psUpd.executeUpdate();
                }
            }

            // C. Masukkan laporan ke tabel 'kendala_produksi' (Inilah yang dibaca Admin!)
            String sqlKendala = "INSERT INTO kendala_produksi (id_produksi, deskripsi, status, tanggal_lapor, dilaporkan_oleh) VALUES (?, ?, 'open', NOW(), ?)";

            try (PreparedStatement psKendala = conn.prepareStatement(sqlKendala)) {
                psKendala.setInt(1, idProduksi);
                psKendala.setString(2, kendala);

                // --- TAMBAHAN BARU ---
                // Ambil ID User yang login (misal: Alvin)
                int currentUserId = com.example.trying3.util.SessionManager.getInstance().getCurrentUserId();
                psKendala.setInt(3, currentUserId);
                // ---------------------

                psKendala.executeUpdate();
            }

            conn.commit();

            showAlert("Berhasil", "Kendala berhasil dilaporkan ke Admin.");

            // Tutup form dan reset text
            kendalaFormContainer.setVisible(false);
            kendalaFormContainer.setManaged(false);
            txtKendala.clear();
            selectedOrderForIssue = null;

            // Refresh tampilan list agar status berubah (opsional)
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