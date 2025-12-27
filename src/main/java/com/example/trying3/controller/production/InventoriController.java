package com.example.trying3.controller.production;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
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
        // Data Dummy sesuai gambar image_cc120a.png
        items.add(new InventoryItem("Kertas A4 80gsm", "50", "rim", "Normal"));
        items.add(new InventoryItem("Tinta Cyan", "5", "botol", "Rendah"));
        items.add(new InventoryItem("Kertas Art Paper 150gsm", "25", "rim", "Normal"));
        items.add(new InventoryItem("Tinta Magenta", "2", "botol", "Kritis"));
    }

    private void renderInventory() {
        inventoryContainer.getChildren().clear();
        for (InventoryItem item : items) {
            inventoryContainer.getChildren().add(createItemCard(item));
        }
    }

    private VBox createItemCard(InventoryItem item) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        card.setPrefWidth(280);

        // 1. Nama Bahan
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        // 2. Jumlah (Angka Besar)
        Label lblQty = new Label(item.getQuantity());
        lblQty.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        // 3. Satuan
        Label lblUnit = new Label(item.getUnit());
        lblUnit.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        // 4. Status Pill
        Label lblStatus = new Label(item.getStatus());
        applyStatusStyle(lblStatus, item.getStatus());

        card.getChildren().addAll(lblName, lblQty, lblUnit, lblStatus);
        VBox.setMargin(lblStatus, new Insets(10, 0, 0, 0));

        return card;
    }

    private void applyStatusStyle(Label label, String status) {
        String baseStyle = "-fx-padding: 4 15; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 10px;";

        switch (status) {
            case "Normal":
                label.setStyle(baseStyle + "-fx-background-color: #222; -fx-text-fill: white;");
                break;
            case "Rendah":
                label.setStyle(baseStyle + "-fx-background-color: #f0f0f0; -fx-text-fill: #666;");
                break;
            case "Kritis":
                label.setStyle(baseStyle + "-fx-background-color: #ff4d4d; -fx-text-fill: white;");
                break;
        }
    }

    public static class InventoryItem {
        private String name, quantity, unit, status;

        public InventoryItem(String name, String quantity, String unit, String status) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.status = status;
        }

        public String getName() { return name; }
        public String getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public String getStatus() { return status; }
    }
}