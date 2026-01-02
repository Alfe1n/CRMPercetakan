package com.example.trying3.controller.admin;

import com.example.trying3.model.Pesanan;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Custom ListCell untuk menampilkan riwayat pembayaran yang sudah terverifikasi
 */
public class PembayaranRiwayatCell extends ListCell<Pesanan> {

    private final VBox cardContainer = new VBox();
    private final Label orderIdLabel = new Label();
    private final Label metodeLabel = new Label();
    private final Label statusBadge = new Label("Terverifikasi");
    private final Label layananValue = new Label();
    private final Label jumlahValue = new Label();
    private final Label totalValue = new Label();

    private final HBox headerBox = new HBox();
    private final GridPane infoGrid = new GridPane();

    public PembayaranRiwayatCell() {
        setupCard();
    }

    /**
     * Konfigurasi UI components (dipanggil sekali saat konstruksi)
     */
    private void setupCard() {
        cardContainer.getStyleClass().add("pesanan-card");
        cardContainer.setSpacing(12);

        orderIdLabel.getStyleClass().add("pesanan-order-id");
        metodeLabel.getStyleClass().add("pesanan-order-date");
        statusBadge.getStyleClass().addAll("status-badge", "status-selesai");

        layananValue.getStyleClass().add("pesanan-value");
        jumlahValue.getStyleClass().add("pesanan-value");
        totalValue.getStyleClass().add("pesanan-total");

        VBox leftHeader = new VBox(2, orderIdLabel, metodeLabel);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(leftHeader, statusBadge);

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

        cardContainer.getChildren().addAll(headerBox, infoGrid);
    }

    @Override
    protected void updateItem(Pesanan pesanan, boolean empty) {
        super.updateItem(pesanan, empty);

        if (empty || pesanan == null) {
            setGraphic(null);
            return;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("id", "ID"));

        orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()) +
                " - " + pesanan.getNamaPelanggan());

        String tanggalText = pesanan.getSpesifikasi() + " • " +
                (pesanan.getUpdatedAt() != null ? pesanan.getUpdatedAt().format(dateFormatter) : "");
        metodeLabel.setText(tanggalText);

        layananValue.setText(pesanan.getJenisLayanan());
        jumlahValue.setText(pesanan.getJumlah() + " pcs");
        totalValue.setText(currencyFormat.format(pesanan.getTotalBiaya()));

        setGraphic(cardContainer);
    }
}
