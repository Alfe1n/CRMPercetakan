package com.example.trying3.controller.admin;

import com.example.trying3.model.Pesanan;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PembayaranRiwayatCell extends ListCell<Pesanan> {
    
    // FIELDS
    private final VBox cardContainer = new VBox();
    private final Label orderIdLabel = new Label();
    private final Label metodeLabel = new Label();
    private final Label statusBadge = new Label("Terverifikasi");
    private final Label layananValue = new Label();
    private final Label jumlahValue = new Label();
    private final Label totalValue = new Label();

    private final HBox headerBox = new HBox();
    private final GridPane infoGrid = new GridPane();
    
    // CONSTRUCTOR
    public PembayaranRiwayatCell() {
        setupCard();
    }
    
    // SETUP - Dipanggil SEKALI di constructor
    private void setupCard() {
        cardContainer.getStyleClass().add("pesanan-card");
        cardContainer.setSpacing(12);

        // ===== STYLING =====
        orderIdLabel.getStyleClass().add("pesanan-order-id");
        metodeLabel.getStyleClass().add("pesanan-order-date");
        statusBadge.getStyleClass().addAll("status-badge", "status-selesai");

        layananValue.getStyleClass().add("pesanan-value");
        jumlahValue.getStyleClass().add("pesanan-value");
        totalValue.getStyleClass().add("pesanan-total");

        // ===== BUILD HEADER =====
        VBox leftHeader = new VBox(2, orderIdLabel, metodeLabel);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(leftHeader, statusBadge);

        // ===== BUILD INFO GRID =====
        infoGrid.setHgap(30);
        infoGrid.setVgap(8);
        infoGrid.setPadding(new Insets(10, 0, 0, 0));

        Label layananLabel = new Label("Layanan:");
        layananLabel.getStyleClass().add("pesanan-label");
        Label jumlahLabel = new Label("Jumlah:");
        jumlahLabel.getStyleClass().add("pesanan-label");
        Label totalLabel = new Label("Total:");
        totalLabel.getStyleClass().add("pesanan-label");

        infoGrid.add(new VBox(3, layananLabel, layananValue), 0, 0);
        infoGrid.add(new VBox(3, jumlahLabel, jumlahValue), 1, 0);
        infoGrid.add(new VBox(3, totalLabel, totalValue), 2, 0);

        // ===== ASSEMBLE CARD - SEKALI SAJA =====
        cardContainer.getChildren().addAll(headerBox, infoGrid);
    }
    
    // UPDATE ITEM - Dipanggil setiap kali data berubah
    @Override
    protected void updateItem(Pesanan pesanan, boolean empty) {
        super.updateItem(pesanan, empty);

        if (empty || pesanan == null) {
            setGraphic(null);
            return;
        }

        // hanya update data, tidak perlu rebuild UI
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        // Format: "ORD-001 - Nama Pelanggan"
        orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()) + 
                           " - " + pesanan.getNamaPelanggan());

        // Format: "Metode Pembayaran • Tanggal Verifikasi"
        String tanggalText = pesanan.getSpesifikasi() + " • " +
                (pesanan.getUpdatedAt() != null ? 
                 pesanan.getUpdatedAt().format(dateFormatter) : 
                 "");
        metodeLabel.setText(tanggalText);

        layananValue.setText(pesanan.getJenisLayanan());
        jumlahValue.setText(pesanan.getJumlah() + " pcs");
        totalValue.setText(currencyFormat.format(pesanan.getTotalHarga()));

        setGraphic(cardContainer);
    }
}
