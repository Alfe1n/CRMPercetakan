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
 * Custom ListCell untuk menampilkan pesanan yang menunggu pembayaran.
 * Menggunakan callback pattern untuk komunikasi dengan controller.
 * OPTIMIZED: Components dibuat sekali di constructor, hanya data yang di-update.
 */
public class PesananPendingCell extends ListCell<Pesanan> {
    
    // CALLBACK INTERFACE - Untuk komunikasi dengan Controller
    public interface PembayaranCallback {
        /**
         * Dipanggil ketika tombol Verifikasi diklik
         */
        void onVerifikasi(Pesanan pesanan, String nominal, String metode);
        
        /**
         * Dipanggil ketika tombol Tolak diklik
         */
        void onTolak(Pesanan pesanan);
        
        /**
         * Meminta controller untuk load data metode pembayaran
         */
        void loadMetodePembayaran(ComboBox<String> comboBox);
    }
    
    // FIELDS
    private final PembayaranCallback callback;
    
    // Components - CREATE SEKALI saat konstruktor
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

    // Container components
    private final HBox headerBox = new HBox();
    private final GridPane infoGrid = new GridPane();
    private final GridPane formGrid = new GridPane();
    private final HBox actionBox = new HBox(10, verifikasiButton, tolakButton);
    
    // CONSTRUCTOR
    public PesananPendingCell(PembayaranCallback callback) {
        this.callback = callback;
        setupCard();
    }
    
    // SETUP - Dipanggil SEKALI di constructor
    private void setupCard() {
        cardContainer.getStyleClass().add("pesanan-card");
        cardContainer.setSpacing(15);

        // styling
        orderIdLabel.getStyleClass().add("pesanan-order-id");
        orderDateLabel.getStyleClass().add("pesanan-order-date");
        statusBadge.getStyleClass().addAll("status-badge", "status-menunggu-pembayaran");

        pelangganValue.getStyleClass().add("pesanan-value");
        layananValue.getStyleClass().add("pesanan-value");
        jumlahValue.getStyleClass().add("pesanan-value");
        totalValue.getStyleClass().add("pesanan-total");

        // nomial text field
        nominalField.getStyleClass().add("text-field");
        nominalField.setPromptText("Masukkan nominal");

        // numeric only validation
        nominalField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                nominalField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // combo box metode pembayaran
        metodeComboBox.getStyleClass().add("combo-box");
        metodeComboBox.setPromptText("Pilih metode");
        
        // Load metode pembayaran via callback
        if (callback != null) {
            callback.loadMetodePembayaran(metodeComboBox);
        }

        // buttons
        verifikasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
        tolakButton.getStyleClass().addAll("button-danger", "pesanan-action-button");
        verifikasiButton.setDisable(true);
        tolakButton.setDisable(true);

        // build header
        VBox leftHeader = new VBox(2, orderIdLabel, orderDateLabel);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(leftHeader, statusBadge);

        // build info grid
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

        // separator
        Region separator = new Region();
        separator.getStyleClass().add("pesanan-separator");
        separator.setPrefHeight(1);

        // form input pembayaran
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

        // action buttons
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // assemble card
        cardContainer.getChildren().addAll(
                headerBox,
                infoGrid,
                separator,
                inputLabel,
                formGrid,
                actionBox
        );
    }
    
    // UPDATE ITEM - Dipanggil setiap kali data berubah
    @Override
    protected void updateItem(Pesanan pesanan, boolean empty) {
        super.updateItem(pesanan, empty);

        if (empty || pesanan == null) {
            setGraphic(null);
            return;
        }

        // ===== HANYA UPDATE DATA, TIDAK REBUILD UI =====
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        // Update text labels
        orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()));
        orderDateLabel.setText(pesanan.getTanggalPesanan().format(dateFormatter));
        pelangganValue.setText(pesanan.getNamaPelanggan());
        layananValue.setText(pesanan.getJenisLayanan());
        jumlahValue.setText(pesanan.getJumlah() + " pcs");
        totalValue.setText(currencyFormat.format(pesanan.getTotalHarga()));

        // Set default values for input
        nominalField.setText(String.valueOf((int) pesanan.getTotalHarga()));
        metodeComboBox.setValue(null);

        // ===== BUTTON ACTIONS - Gunakan CALLBACK =====
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

        // ===== ENABLE/DISABLE BUTTONS =====
        // Buttons hanya enabled jika nominal DAN metode sudah diisi
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

        // Set graphic
        setGraphic(cardContainer);
    }
}
