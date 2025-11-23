package com.example.trying3.controller.production;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class JadwalProduksiController implements Initializable {

    @FXML
    private VBox scheduleContainer;

    // List untuk menampung data dummy
    private List<ScheduleItem> scheduleList = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        renderSchedule();
    }

    private void loadDummyData() {
        // Data dummy sesuai konteks gambar referensi
        scheduleList.add(new ScheduleItem("ORD-002", "Sablon", "Toko Berkah", 50, "Sedang Diproduksi"));
        scheduleList.add(new ScheduleItem("ORD-005", "Offset", "Universitas Jenderal Achmad Yani", 1000, "Menunggu Bahan"));
        scheduleList.add(new ScheduleItem("ORD-006", "Digital Printing", "Cafe Kopi Senja", 25, "Siap Produksi"));
        scheduleList.add(new ScheduleItem("ORD-007", "Cetak Undangan", "Pernikahan Budi & Ani", 300, "Dalam Antrian"));
    }

    private void renderSchedule() {
        scheduleContainer.getChildren().clear();

        if (scheduleList.isEmpty()) {
            Label emptyLabel = new Label("Belum ada jadwal produksi minggu ini.");
            emptyLabel.getStyleClass().add("muted");
            scheduleContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < scheduleList.size(); i++) {
            ScheduleItem item = scheduleList.get(i);
            HBox row = createScheduleRow(item);

            // Tambahkan garis pemisah (border bawah) kecuali untuk item terakhir
            if (i < scheduleList.size() - 1) {
                row.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0;");
            }

            scheduleContainer.getChildren().add(row);
        }
    }

    private HBox createScheduleRow(ScheduleItem item) {
        HBox row = new HBox();
        row.setPadding(new Insets(15, 0, 15, 0)); // Padding atas bawah
        row.setAlignment(Pos.CENTER_LEFT);

        // --- BAGIAN KIRI (Teks Utama) ---
        VBox leftContent = new VBox(5); // Spacing antar baris teks

        // Judul: "ORD-002 - Sablon"
        Label titleLabel = new Label(item.getOrderId() + " - " + item.getServiceType());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");

        // Subjudul: "Toko Berkah - 50 pcs"
        Label subtitleLabel = new Label(item.getCustomerName() + " - " + item.getQuantity() + " pcs");
        subtitleLabel.getStyleClass().add("muted"); // Menggunakan class .muted dari production.css

        leftContent.getChildren().addAll(titleLabel, subtitleLabel);

        // --- SPACER (Agar status terdorong ke kanan) ---
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- BAGIAN KANAN (Status Pill) ---
        Label statusLabel = new Label(item.getStatus());
        statusLabel.getStyleClass().add("status-pill");

        // Kustomisasi warna status sederhana
        if (item.getStatus().contains("Menunggu") || item.getStatus().contains("Antrian")) {
            statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666;");
        } else if (item.getStatus().equals("Sedang Diproduksi")) {
            // Default style hitam (sesuai css .status-pill)
        }

        // Gabungkan ke dalam Row
        row.getChildren().addAll(leftContent, spacer, statusLabel);

        return row;
    }

    // --- INNER CLASS: MODEL DATA ---
    public static class ScheduleItem {
        private String orderId;
        private String serviceType;
        private String customerName;
        private int quantity;
        private String status;

        public ScheduleItem(String orderId, String serviceType, String customerName, int quantity, String status) {
            this.orderId = orderId;
            this.serviceType = serviceType;
            this.customerName = customerName;
            this.quantity = quantity;
            this.status = status;
        }

        public String getOrderId() { return orderId; }
        public String getServiceType() { return serviceType; }
        public String getCustomerName() { return customerName; }
        public int getQuantity() { return quantity; }
        public String getStatus() { return status; }
    }
}