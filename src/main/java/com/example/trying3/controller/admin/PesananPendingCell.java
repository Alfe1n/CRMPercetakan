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
 * Custom ListCell untuk pesanan yang menunggu verifikasi pembayaran
 * Menggunakan callback pattern untuk komunikasi dengan controller
 */
public class PesananPendingCell extends ListCell<Pesanan> {

    /**
     * Interface callback untuk komunikasi dengan controller
     */
    public interface PembayaranCallback {
        void onVerifikasi(Pesanan pesanan, String nominal, String metode);
        void onTolak(Pesanan pesanan);
        void loadMetodePembayaran(ComboBox<String> comboBox);
    }

    private final PembayaranCallback callback;

    private final VBox cardContainer = new VBox();
    private final Label orderIdLabel = new Label();
    private final Label orderDateLabel = new Label();
    private final Label statusBadge = new Label("Menunggu Pembayaran");
    private final Label pelangganValue = new Label();
    private final Label layananValue = new Label();
    private final Label jumlahValue = new Label();
    private final Label totalValue = new Label();
    private final TextField nominalField = new TextField();
    private final ComboBox<String> metodeComboBox = new ComboBox<>();
    private final Button verifikasiButton = new Button("✓ Verifikasi");
    private final Button tolakButton = new Button("⊗ Tolak");

    private final HBox headerBox = new HBox();
    private final GridPane infoGrid = new GridPane();
    private final GridPane formGrid = new GridPane();
    private final HBox actionBox = new HBox(10, verifikasiButton, tolakButton);

    public PesananPendingCell(PembayaranCallback callback) {
        this.callback = callback;
        setupCard();
    }

    /**
     * Konfigurasi UI components (dipanggil sekali saat konstruksi)
     */
    private void setupCard() {
        cardContainer.getStyleClass().add("pesanan-card");
        cardContainer.setSpacing(15);

        orderIdLabel.getStyleClass().add("pesanan-order-id");
        orderDateLabel.getStyleClass().add("pesanan-order-date");
        statusBadge.getStyleClass().addAll("status-badge", "status-menunggu-pembayaran");

        pelangganValue.getStyleClass().add("pesanan-value");
        layananValue.getStyleClass().add("pesanan-value");
        jumlahValue.getStyleClass().add("pesanan-value");
        totalValue.getStyleClass().add("pesanan-total");

        nominalField.getStyleClass().add("text-field");
        nominalField.setPromptText("Masukkan nominal");

        nominalField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                nominalField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        metodeComboBox.getStyleClass().add("combo-box");
        metodeComboBox.setPromptText("Pilih metode");

        if (callback != null) {
            callback.loadMetodePembayaran(metodeComboBox);
        }

        verifikasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
        tolakButton.getStyleClass().addAll("button-danger", "pesanan-action-button");
        verifikasiButton.setDisable(true);
        tolakButton.setDisable(true);

        VBox leftHeader = new VBox(2, orderIdLabel, orderDateLabel);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(leftHeader, statusBadge);

        infoGrid.setHgap(30);
        infoGrid.setVgap(12);
        infoGrid.setPadding(new Insets(10, 0, 10, 0));

        Label pelangganLabel = new Label("Pelanggan:");
        pelangganLabel.getStyleClass().add("pesanan-label");
        Label layananLabel = new Label("Layanan:");
        layananLabel.getStyleClass().add("pesanan-label");
        Label totalLabel = new Label("Total Tagihan:");
        totalLabel.getStyleClass().add("pesanan-label");

        infoGrid.add(new VBox(3, pelangganLabel, pelangganValue), 0, 0);
        infoGrid.add(new VBox(3, layananLabel, layananValue, jumlahValue), 1, 0);
        infoGrid.add(new VBox(3, totalLabel, totalValue), 2, 0);

        Region separator = new Region();
        separator.getStyleClass().add("pesanan-separator");
        separator.setPrefHeight(1);

        Label inputLabel = new Label("Input Pembayaran:");
        inputLabel.getStyleClass().add("form-label");

        formGrid.setHgap(15);
        formGrid.setVgap(10);

        Label nominalLabel = new Label("Nominal (Rp):");
        nominalLabel.getStyleClass().add("form-label");
        Label metodeLabel = new Label("Metode Pembayaran:");
        metodeLabel.getStyleClass().add("form-label");

        formGrid.add(nominalLabel, 0, 0);
        formGrid.add(nominalField, 1, 0);
        formGrid.add(metodeLabel, 0, 1);
        formGrid.add(metodeComboBox, 1, 1);

        actionBox.setAlignment(Pos.CENTER_RIGHT);

        cardContainer.getChildren().addAll(
                headerBox,
                infoGrid,
                separator,
                inputLabel,
                formGrid,
                actionBox);
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

        orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()));
        orderDateLabel.setText(pesanan.getTanggalPesanan().format(dateFormatter));
        pelangganValue.setText(pesanan.getNamaPelanggan());
        layananValue.setText(pesanan.getJenisLayanan());
        jumlahValue.setText(pesanan.getJumlah() + " pcs");
        totalValue.setText(currencyFormat.format(pesanan.getTotalBiaya()));

        nominalField.setText(String.valueOf((int) pesanan.getTotalBiaya()));
        metodeComboBox.setValue(null);

        verifikasiButton.setOnAction(e -> {
            if (callback != null) {
                callback.onVerifikasi(pesanan, nominalField.getText(), metodeComboBox.getValue());
            }
        });

        tolakButton.setOnAction(e -> {
            if (callback != null) {
                callback.onTolak(pesanan);
            }
        });

        nominalField.textProperty().addListener((obs, old, newVal) -> {
            boolean hasData = !newVal.trim().isEmpty() && metodeComboBox.getValue() != null;
            verifikasiButton.setDisable(!hasData);
            tolakButton.setDisable(!hasData);
        });

        metodeComboBox.valueProperty().addListener((obs, old, newVal) -> {
            boolean hasData = !nominalField.getText().trim().isEmpty() && newVal != null;
            verifikasiButton.setDisable(!hasData);
            tolakButton.setDisable(!hasData);
        });

        setGraphic(cardContainer);
    }
}
