package com.example.trying3.controller.production;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProduksiController implements Initializable {

    // --- FXML ELEMENTS ---
    // Statistik
    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;

    // Container untuk menampung kartu pesanan
    @FXML private VBox orderListContainer;

    // Form Kendala (Popup/Bagian Bawah)
    @FXML private VBox kendalaFormContainer;
    @FXML private Label lblKendalaTitle;
    @FXML private TextArea txtKendala;

    // --- DATA LOCAL ---
    private List<ProductionOrder> orderList = new ArrayList<>();
    private ProductionOrder selectedOrderForIssue; // Order yang sedang dipilih untuk lapor kendala

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        refreshDashboard();
    }

    // 1. LOAD DATA DUMMY
    private void loadDummyData() {
        orderList.add(new ProductionOrder("ORD-002", "Toko Berkah", "2024-01-14", "Sablon", 50, "Kaos cotton combed 30s, sablon plastisol", "design_toko_berkah.ai", "Sedang Diproses"));
        orderList.add(new ProductionOrder("ORD-003", "PT Maju Jaya", "2024-01-15", "Digital Printing", 100, "Banner Flexi 280gr, Finishing Mata Ayam", "banner_maju.pdf", "Menunggu"));
        orderList.add(new ProductionOrder("ORD-004", "CV. Sejahtera", "2024-01-16", "Offset", 1000, "Brosur A4 Art Paper 150gsm", "brosur_final.pdf", "Selesai"));
    }

    // 2. REFRESH TAMPILAN (Statistik & List)
    private void refreshDashboard() {
        updateStatistics();
        renderOrderList();
    }

    private void updateStatistics() {
        long waiting = orderList.stream().filter(o -> o.getStatus().equals("Menunggu")).count();
        long inProgress = orderList.stream().filter(o -> o.getStatus().equals("Sedang Diproses")).count();
        long completed = orderList.stream().filter(o -> o.getStatus().equals("Selesai")).count();

        waitingLabel.setText(String.valueOf(waiting));
        inProgressLabel.setText(String.valueOf(inProgress));
        completedLabel.setText(String.valueOf(completed));
    }

    private void renderOrderList() {
        orderListContainer.getChildren().clear();

        // Filter: Tampilkan yang belum selesai (Menunggu & Sedang Diproses)
        // Agar dashboard fokus pada pekerjaan aktif.
        List<ProductionOrder> activeOrders = orderList.stream()
                .filter(o -> !o.getStatus().equals("Selesai"))
                .collect(Collectors.toList());

        if (activeOrders.isEmpty()) {
            Label emptyLabel = new Label("Tidak ada antrian produksi saat ini.");
            emptyLabel.getStyleClass().add("muted");
            orderListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (ProductionOrder order : activeOrders) {
            orderListContainer.getChildren().add(createOrderCard(order));
        }
    }

    // 3. MEMBUAT UI KARTU SECARA PROGRAMMATIC (Sesuai CSS)
    private VBox createOrderCard(ProductionOrder order) {
        VBox card = new VBox();
        card.getStyleClass().add("order-card");
        card.setSpacing(12);
        card.setPadding(new Insets(16));

        // Header Baris (ID + Spacer + Status Pill)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblId = new Label(order.getId());
        lblId.getStyleClass().add("order-id");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.getStatus());
        lblStatus.getStyleClass().add("status-pill");

        // Kustomisasi warna pill jika status "Menunggu"
        if(order.getStatus().equals("Menunggu")) {
            lblStatus.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333;");
        }

        header.getChildren().addAll(lblId, spacer, lblStatus);

        // Subheader (Customer Info)
        Label lblCustomer = new Label(order.getCustomerName() + " - " + order.getDate());
        lblCustomer.getStyleClass().add("muted");

        // Garis Pemisah
        Separator sep = new Separator();

        // Detail Pesanan
        VBox detailsBox = new VBox(8);
        detailsBox.getChildren().addAll(
                createDetailRow("Layanan:", order.getService()),
                createDetailRow("Jumlah:", order.getQuantity() + " pcs"),
                createDetailRow("Spesifikasi:", order.getSpecs()),
                createDetailRow("File Desain:", order.getDesignFile())
        );

        // Tombol Aksi
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button btnFinish = new Button("Selesai Produksi");
        btnFinish.getStyleClass().add("btn-primary");
        btnFinish.setOnAction(e -> handleFinishProduction(order));

        // Logika ganti teks tombol berdasarkan status
        if (order.getStatus().equals("Menunggu")) {
            btnFinish.setText("Mulai Produksi");
            btnFinish.setOnAction(e -> handleStartProduction(order));
        }

        Button btnIssue = new Button("Laporkan Kendala");
        btnIssue.getStyleClass().add("btn-outline");
        btnIssue.setOnAction(e -> showKendalaForm(order));

        Button btnPrint = new Button("Print Job Sheet");
        btnPrint.getStyleClass().add("btn-outline");
        btnPrint.setOnAction(e -> System.out.println("Printing job sheet for " + order.getId()));

        actions.getChildren().addAll(btnFinish, btnIssue, btnPrint);

        // Gabungkan semua ke dalam kartu
        card.getChildren().addAll(header, lblCustomer, sep, detailsBox, actions);
        return card;
    }

    // Helper untuk membuat baris detail (Label Judul + Isi)
    private VBox createDetailRow(String title, String value) {
        VBox row = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("field-title");
        Label lblValue = new Label(value);
        lblValue.setWrapText(true);
        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    // --- EVENT HANDLERS ---

    private void handleStartProduction(ProductionOrder order) {
        order.setStatus("Sedang Diproses");
        System.out.println("Order " + order.getId() + " dimulai.");
        refreshDashboard();
    }

    private void handleFinishProduction(ProductionOrder order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Tandai pesanan " + order.getId() + " sebagai Selesai?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                order.setStatus("Selesai");
                System.out.println("Order " + order.getId() + " selesai.");
                refreshDashboard();
            }
        });
    }

    private void showKendalaForm(ProductionOrder order) {
        this.selectedOrderForIssue = order;
        lblKendalaTitle.setText("Laporkan Kendala - " + order.getId());
        txtKendala.clear();
        kendalaFormContainer.setVisible(true);
        kendalaFormContainer.setManaged(true);
    }

    @FXML
    private void submitKendala() {
        if (selectedOrderForIssue != null && !txtKendala.getText().isEmpty()) {
            String issue = txtKendala.getText();
            System.out.println("Kendala dilaporkan untuk " + selectedOrderForIssue.getId() + ": " + issue);

            Alert info = new Alert(Alert.AlertType.INFORMATION, "Laporan kendala terkirim.");
            info.show();

            cancelKendala();
        } else {
            Alert warn = new Alert(Alert.AlertType.WARNING, "Mohon isi deskripsi kendala.");
            warn.show();
        }
    }

    @FXML
    private void cancelKendala() {
        kendalaFormContainer.setVisible(false);
        kendalaFormContainer.setManaged(false);
        selectedOrderForIssue = null;
        txtKendala.clear();
    }

    // ==============================================================
    // INNER CLASS: MODEL DATA (ProductionOrder)
    // Disimpan di dalam Controller agar menjadi satu file saja.
    // ==============================================================
    public static class ProductionOrder {
        private String id;
        private String customerName;
        private String date;
        private String service;
        private int quantity;
        private String specs;
        private String designFile;
        private String status;

        public ProductionOrder(String id, String customerName, String date, String service, int quantity, String specs, String designFile, String status) {
            this.id = id;
            this.customerName = customerName;
            this.date = date;
            this.service = service;
            this.quantity = quantity;
            this.specs = specs;
            this.designFile = designFile;
            this.status = status;
        }

        public String getId() { return id; }
        public String getCustomerName() { return customerName; }
        public String getDate() { return date; }
        public String getService() { return service; }
        public int getQuantity() { return quantity; }
        public String getSpecs() { return specs; }
        public String getDesignFile() { return designFile; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}