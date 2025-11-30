package com.example.trying3.controller.design;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class KelolaDesignController implements Initializable {

    @FXML private Label lblCountWaiting;
    @FXML private Label lblCountRevision;
    @FXML private Label lblCountApproved;
    @FXML private VBox ordersContainer;

    private List<DetailedOrder> orderList = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        refreshUI();
    }

    private void loadDummyData() {
        // Data Dummy
        orderList.add(new DetailedOrder(
                "ORD-001", "PT. Maju Jaya", "2024-01-15",
                "Digital Printing", "1000 pcs",
                "Brosur A4, kertas art paper 150gsm, full color",
                "Desain logo harus menggunakan warna biru corporate",
                "Menunggu Desain"
        ));

        orderList.add(new DetailedOrder(
                "ORD-005", "Toko Sebelah", "2024-11-30",
                "Banner Outdoor", "2 pcs",
                "Ukuran 3x1 meter, finishing mata ayam",
                "-",
                "Perlu Revisi"
        ));
    }

    private void refreshUI() {
        // Update Statistik
        long waiting = orderList.stream().filter(o -> o.status.equals("Menunggu Desain")).count();
        long revision = orderList.stream().filter(o -> o.status.equals("Perlu Revisi")).count();
        long approved = orderList.stream().filter(o -> o.status.equals("Disetujui")).count();

        if (lblCountWaiting != null) lblCountWaiting.setText(String.valueOf(waiting));
        if (lblCountRevision != null) lblCountRevision.setText(String.valueOf(revision));
        if (lblCountApproved != null) lblCountApproved.setText(String.valueOf(approved));

        // Render List
        if (ordersContainer != null) {
            ordersContainer.getChildren().clear();
            for (DetailedOrder order : orderList) {
                ordersContainer.getChildren().add(createOrderCard(order));
            }
        }
    }

    private VBox createOrderCard(DetailedOrder order) {
        VBox card = new VBox();
        card.getStyleClass().add("order-detail-card");
        card.setSpacing(15);

        // --- Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox identityBox = new VBox(2);
        Label lblId = new Label(order.id);
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111;");
        Label lblClient = new Label(order.client);
        lblClient.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label lblDate = new Label(order.date);
        lblDate.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        identityBox.getChildren().addAll(lblId, lblClient, lblDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.status);
        lblStatus.getStyleClass().add("status-pill");

        if (order.status.equals("Menunggu Desain")) {
            lblStatus.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151;");
        } else if (order.status.equals("Perlu Revisi")) {
            lblStatus.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626;");
        }

        header.getChildren().addAll(identityBox, spacer, lblStatus);

        // --- Detail Info ---
        VBox details = new VBox(8);
        details.getChildren().add(createRow("Layanan: ", order.service));
        details.getChildren().add(createRow("Jumlah: ", order.qty));
        details.getChildren().add(createRow("Spesifikasi: ", order.specs));
        details.getChildren().add(createRow("Catatan: ", order.notes));

        // --- Actions ---
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnUpload = new Button("↑ Upload Desain");
        btnUpload.getStyleClass().addAll("action-button-base", "btn-outline");

        Button btnApprove = new Button("✓ Setujui Desain");
        btnApprove.getStyleClass().addAll("action-button-base", "btn-filled");

        Button btnRevise = new Button("✎ Perlu Revisi");
        btnRevise.getStyleClass().addAll("action-button-base", "btn-outline");

        actions.getChildren().addAll(btnUpload, btnApprove, btnRevise);

        card.getChildren().addAll(header, details, actions);
        return card;
    }

    private HBox createRow(String labelText, String valueText) {
        HBox row = new HBox(5);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        Label val = new Label(valueText);
        val.getStyleClass().add("field-value");
        val.setWrapText(true);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    public static class DetailedOrder {
        String id, client, date, service, qty, specs, notes, status;

        public DetailedOrder(String id, String client, String date, String service, String qty, String specs, String notes, String status) {
            this.id = id;
            this.client = client;
            this.date = date;
            this.service = service;
            this.qty = qty;
            this.specs = specs;
            this.notes = notes;
            this.status = status;
        }
    }
}