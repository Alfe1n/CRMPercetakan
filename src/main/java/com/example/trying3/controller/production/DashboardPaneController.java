package com.example.trying3.controller.production;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
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
import java.util.List;
import java.util.ResourceBundle;

public class DashboardPaneController implements Initializable {

    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox queueContainer;

    private PesananDAO pesananDAO;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pesananDAO = new PesananDAO();
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Mengambil data menggunakan method yang sama dengan ProduksiController
        List<Pesanan> allOrders = pesananDAO.getPesananForProduction();

        // 1. Update Statistik (Stat Boxes) [cite: 21, 26, 31]
        long waiting = allOrders.stream().filter(o -> o.getStatus().equalsIgnoreCase("Antrian Produksi")).count();
        long inProgress = allOrders.stream().filter(o -> o.getStatus().equalsIgnoreCase("Sedang Diproduksi")).count();
        // Sesuai logic ProduksiController: Siap Dikirim dianggap selesai produksi
        long completed = allOrders.stream().filter(o -> o.getStatus().equalsIgnoreCase("Siap Dikirim") || o.getStatus().equalsIgnoreCase("Selesai")).count();

        if (waitingLabel != null) waitingLabel.setText(String.valueOf(waiting));
        if (inProgressLabel != null) inProgressLabel.setText(String.valueOf(inProgress));
        if (completedLabel != null) completedLabel.setText(String.valueOf(completed));

        // 2. Render Daftar Antrian [cite: 33]
        renderQueue(allOrders);
    }

    private void renderQueue(List<Pesanan> orders) {
        if (queueContainer == null) return;
        queueContainer.getChildren().clear();

        // Filter hanya yang belum selesai (Antrian & Sedang Diproduksi)
        List<Pesanan> activeOrders = orders.stream()
                .filter(o -> !o.getStatus().equalsIgnoreCase("Siap Dikirim") && !o.getStatus().equalsIgnoreCase("Selesai"))
                .toList();

        if (activeOrders.isEmpty()) {
            Label empty = new Label("Tidak ada antrian produksi aktif.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 10;");
            queueContainer.getChildren().add(empty);
            return;
        }

        for (Pesanan order : activeOrders) {
            queueContainer.getChildren().add(createSimpleOrderCard(order));
        }
    }

    // Membuat kartu pesanan sesuai tampilan image_cc6f69.png
    private VBox createSimpleOrderCard(Pesanan p) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #eee; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20;");

        // Header: ID & Pelanggan | Status Pill
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label("PO-" + p.getIdPesanan());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label lblCust = new Label(p.getNamaPelanggan());
        lblCust.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(lblId, lblCust);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(p.getStatus());
        String pillStyle = "-fx-background-radius: 15; -fx-padding: 5 12; -fx-font-weight: bold; -fx-font-size: 10px;";
        if (p.getStatus().equalsIgnoreCase("Sedang Diproduksi")) {
            lblStatus.setStyle(pillStyle + "-fx-background-color: #222; -fx-text-fill: white;");
        } else {
            lblStatus.setStyle(pillStyle + "-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7;");
        }

        header.getChildren().addAll(titleBox, spacer, lblStatus);

        // Details: Layanan, Jumlah, Spesifikasi
        VBox details = new VBox(5);
        details.getChildren().add(createDetailRow("Layanan: ", p.getJenisLayanan()));
        details.getChildren().add(createDetailRow("Jumlah: ", p.getJumlah() + " pcs"));
        details.getChildren().add(createDetailRow("Spesifikasi: ", p.getSpesifikasi()));

        card.getChildren().addAll(header, details);
        return card;
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(5);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #555;");
        Label v = new Label(value != null ? value : "-");
        v.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
        row.getChildren().addAll(l, v);
        return row;
    }
}