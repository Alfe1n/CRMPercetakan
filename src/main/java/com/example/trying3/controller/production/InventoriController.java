package com.example.trying3.controller.production;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InventoriController implements Initializable {

    @FXML
    private FlowPane inventoryContainer;

    private List<InventoryItem> items = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        renderInventory();
    }

    private void loadDummyData() {
        // Data Dummy dengan berbagai kondisi stok
        items.add(new InventoryItem("Kertas A4 80gr", 50, "Rim", 10, "Kertas"));
        items.add(new InventoryItem("Tinta Cyan (C)", 2, "Botol", 5, "Tinta")); // Kritis
        items.add(new InventoryItem("Tinta Magenta (M)", 6, "Botol", 5, "Tinta")); // Menipis
        items.add(new InventoryItem("Tinta Yellow (Y)", 8, "Botol", 5, "Tinta"));
        items.add(new InventoryItem("Tinta Black (K)", 25, "Botol", 5, "Tinta"));
        items.add(new InventoryItem("Kaos Polos Hitam L", 120, "Pcs", 20, "Kaos"));
        items.add(new InventoryItem("Kaos Polos Putih L", 15, "Pcs", 20, "Kaos")); // Kritis
        items.add(new InventoryItem("Banner Flexi 280gr", 4, "Roll", 2, "Banner"));
    }

    private void renderInventory() {
        inventoryContainer.getChildren().clear();

        for (InventoryItem item : items) {
            inventoryContainer.getChildren().add(createItemCard(item));
        }
    }

    private VBox createItemCard(InventoryItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("stat-box"); // Menggunakan style stat-box agar terlihat seperti kartu
        card.setPrefWidth(220); // Lebar tetap agar rapi di grid
        card.setSpacing(10);
        card.setPadding(new Insets(20));

        // 1. Kategori & Nama
        Label lblCategory = new Label(item.getCategory());
        lblCategory.getStyleClass().add("stat-subtitle");

        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        lblName.setWrapText(true);

        // 2. Jumlah Stok (Besar)
        Label lblQty = new Label(item.getQuantity() + " " + item.getUnit());
        lblQty.getStyleClass().add("stat-value");

        // 3. Status Pill (Logika Warna)
        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("status-pill");

        if (item.getQuantity() <= item.getMinThreshold()) {
            lblStatus.setText("Stok Kritis");
            // Merah untuk kritis
            lblStatus.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #d32f2f;");
        } else if (item.getQuantity() <= item.getMinThreshold() + 5) {
            lblStatus.setText("Menipis");
            // Oranye untuk warning
            lblStatus.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #ef6c00;");
        } else {
            lblStatus.setText("Aman");
            // Hijau/Default untuk aman
            lblStatus.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;");
        }

        // Spacer agar status pill ada di bawah
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Menambahkan elemen ke kartu
        card.getChildren().addAll(lblCategory, lblName, lblQty, spacer, lblStatus);
        return card;
    }

    @FXML
    private void handleRestock() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fitur");
        alert.setHeaderText(null);
        alert.setContentText("Fitur pemesanan bahan baku akan segera tersedia.");
        alert.showAndWait();
    }

    // --- INNER CLASS MODEL ---
    public static class InventoryItem {
        private String name;
        private int quantity;
        private String unit;
        private int minThreshold; // Batas minimal sebelum dianggap kritis
        private String category;

        public InventoryItem(String name, int quantity, String unit, int minThreshold, String category) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.minThreshold = minThreshold;
            this.category = category;
        }

        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public int getMinThreshold() { return minThreshold; }
        public String getCategory() { return category; }
    }
}