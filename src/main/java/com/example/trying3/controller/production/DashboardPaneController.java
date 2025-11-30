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

public class DashboardPaneController implements Initializable {

    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox queueContainer;

    private List<DashboardOrder> orders = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        updateStatistics();
        renderQueue();
    }

    private void loadDummyData() {
        // Data simulasi (Sama dengan modul lain agar konsisten)
        orders.add(new DashboardOrder("ORD-002", "Toko Berkah", "Sablon", 50, "Kaos cotton combed 30s, sablon plastisol", "Sedang Diproduksi"));
        orders.add(new DashboardOrder("ORD-003", "PT Maju Jaya", "Digital Printing", 100, "Banner Flexi 280gr", "Menunggu Produksi"));
        orders.add(new DashboardOrder("ORD-004", "CV. Sejahtera", "Offset", 1000, "Brosur A4 Art Paper", "Selesai Produksi"));
    }

    private void updateStatistics() {
        long waiting = orders.stream().filter(o -> o.status.equals("Menunggu Produksi")).count();
        long inProgress = orders.stream().filter(o -> o.status.equals("Sedang Diproduksi")).count();
        long completed = orders.stream().filter(o -> o.status.equals("Selesai Produksi")).count();

        waitingLabel.setText(String.valueOf(waiting));
        inProgressLabel.setText(String.valueOf(inProgress));
        completedLabel.setText(String.valueOf(completed));
    }

    private void renderQueue() {
        queueContainer.getChildren().clear();

        // Tampilkan semua order (atau filter hanya yang aktif jika diinginkan)
        for (DashboardOrder order : orders) {
            // Jangan tampilkan yang sudah selesai di antrian dashboard (opsional)
            if (!order.status.equals("Selesai Produksi")) {
                queueContainer.getChildren().add(createOrderCard(order));
            }
        }

        if (queueContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("Tidak ada antrian aktif.");
            emptyLabel.getStyleClass().add("muted");
            queueContainer.getChildren().add(emptyLabel);
        }
    }

    // Membuat Kartu Pesanan (Versi Summary Dashboard)
    private VBox createOrderCard(DashboardOrder order) {
        VBox card = new VBox();
        // Styling card internal (mirip dengan order-card di modul produksi)
        card.setStyle("-fx-background-color: white; -fx-border-color: #eeeeee; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20;");
        card.setSpacing(10);

        // --- Header: ID & Customer (Kiri) --- Status Pill (Kanan) ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label(order.id);
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");

        Label lblCustomer = new Label(order.customer);
        lblCustomer.getStyleClass().add("muted");

        titleBox.getChildren().addAll(lblId, lblCustomer);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.status);
        lblStatus.getStyleClass().add("status-pill");

        // Logic Warna Status Pill
        if (order.status.equals("Menunggu Produksi")) {
            lblStatus.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #666; -fx-background-radius: 15; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            // Default Hitam (Sedang Diproduksi)
            lblStatus.setStyle("-fx-background-color: #222; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        header.getChildren().addAll(titleBox, spacer, lblStatus);

        // --- Details Section ---
        VBox detailsBox = new VBox(5);
        detailsBox.getChildren().add(createDetailLabel("Layanan: ", order.service));
        detailsBox.getChildren().add(createDetailLabel("Jumlah: ", order.quantity + " pcs"));
        detailsBox.getChildren().add(createDetailLabel("Spesifikasi: ", order.specs));

        // Gabungkan
        card.getChildren().addAll(header, detailsBox);
        return card;
    }

    // Helper untuk membuat teks detail yang tebal judulnya
    private HBox createDetailLabel(String label, String value) {
        HBox row = new HBox(5);
        Label lblTitle = new Label(label);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");

        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    // Inner Class Model
    public static class DashboardOrder {
        String id, customer, service, specs, status;
        int quantity;

        public DashboardOrder(String id, String customer, String service, int quantity, String specs, String status) {
            this.id = id;
            this.customer = customer;
            this.service = service;
            this.quantity = quantity;
            this.specs = specs;
            this.status = status;
        }
    }
}