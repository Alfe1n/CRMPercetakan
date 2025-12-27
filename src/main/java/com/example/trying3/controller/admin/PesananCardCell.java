package com.example.trying3.controller.admin;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.model.Pesanan;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PesananCardCell extends ListCell<Pesanan> {

    // Interface untuk mengirim aksi (klik tombol) kembali ke Controller utama
    private final Consumer<Pesanan> onEdit;
    private final Consumer<Pesanan> onDelete;
    private final Consumer<Pesanan> onConfirm;
    private final BiConsumer<Pesanan, String> onStatusChange;

    // Komponen UI untuk tampilan kartu
    private final VBox cardContainer = new VBox();
    private final Label orderIdLabel = new Label();
    private final Label orderDateLabel = new Label();
    private final Label statusBadge = new Label();
    private final HBox headerBox = new HBox();

    private final GridPane infoGrid = new GridPane();
    private final Label pelangganValue = new Label();
    private final Label phoneValue = new Label();
    private final Label layananValue = new Label();
    private final Label jumlahValue = new Label();
    private final Label totalValue = new Label();

    private final VBox specBox = new VBox(5);
    private final Label specText = new Label();

    // Komponen interaktif
    private final ComboBox<String> ubahStatusComboBox = new ComboBox<>();
    private final Button editButton = new Button("✎ Edit");
    private final Button deleteButton = new Button("🗑 Hapus");
    private final Button konfirmasiButton = new Button("✅ Konfirmasi");

    // Constructor: Menerima fungsi aksi dari Controller
    public PesananCardCell(Consumer<Pesanan> onEdit,
                           Consumer<Pesanan> onDelete,
                           Consumer<Pesanan> onConfirm,
                           BiConsumer<Pesanan, String> onStatusChange) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onConfirm = onConfirm;
        this.onStatusChange = onStatusChange;

        setupCard(); // Panggil fungsi untuk menyusun layout visual
    }

    // Mengatur CSS, posisi, dan susunan elemen dalam kartu
    private void setupCard() {
        cardContainer.getStyleClass().add("pesanan-card");
        cardContainer.setSpacing(12);

        // Styling header (ID Pesanan, Tanggal, Status)
        orderIdLabel.getStyleClass().add("pesanan-order-id");
        orderDateLabel.getStyleClass().add("pesanan-order-date");
        statusBadge.getStyleClass().add("status-badge");

        VBox leftHeader = new VBox(2, orderIdLabel, orderDateLabel);
        leftHeader.getStyleClass().add("pesanan-card-header");

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        headerBox.getChildren().addAll(leftHeader, statusBadge);

        // Styling isi informasi (Pelanggan, Layanan, Total)
        pelangganValue.getStyleClass().add("pesanan-value");
        layananValue.getStyleClass().add("pesanan-value");
        jumlahValue.getStyleClass().add("pesanan-value");

        phoneValue.getStyleClass().add("pesanan-phone");
        totalValue.getStyleClass().add("pesanan-total");

        // Menyusun grid informasi
        GridPane infoGrid = new GridPane();
        infoGrid.getStyleClass().add("pesanan-info-grid");
        infoGrid.setHgap(30);
        infoGrid.setVgap(12);

        infoGrid.add(createInfoBox("Pelanggan:", pelangganValue, phoneValue), 0, 0);
        infoGrid.add(createInfoBox("Layanan:", layananValue, jumlahValue), 1, 0);
        infoGrid.add(createInfoBox("Total:", totalValue), 2, 0);

        // Area spesifikasi (catatan pesanan)
        Label specLabel = new Label("Spesifikasi:");
        specLabel.getStyleClass().add("pesanan-spec-label");
        specText.getStyleClass().add("pesanan-spec-text");
        specText.setWrapText(true);

        VBox specBox = new VBox(5);
        specBox.getChildren().addAll(specLabel, specText);

        // Mengatur tampilan teks pada ComboBox status
        ubahStatusComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(ubahStatusComboBox.getPromptText());
                else setText(item);
            }
        });

        // Styling tombol aksi
        konfirmasiButton.getStyleClass().addAll("button-success", "pesanan-action-button");
        editButton.getStyleClass().addAll("button-secondary", "pesanan-action-button");
        deleteButton.getStyleClass().addAll("button-danger", "pesanan-action-button");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getChildren().addAll(konfirmasiButton, editButton, deleteButton);

        HBox bottomBox = new HBox(15);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actionBox, Priority.ALWAYS);
        bottomBox.getChildren().addAll(ubahStatusComboBox, actionBox);

        // Garis pemisah
        Region separator = new Region();
        separator.getStyleClass().add("pesanan-separator");
        separator.setPrefHeight(1);

        cardContainer.getChildren().addAll(headerBox, infoGrid, specBox, separator, bottomBox);
    }

    // Helper untuk membuat kotak info label-value
    private VBox createInfoBox(String label, Label... values) {
        VBox box = new VBox(3);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("pesanan-label");
        box.getChildren().add(lbl);
        for(Label v : values) {
            box.getChildren().add(v);
        }
        return box;
    }

    // Dipanggil otomatis setiap kali data pesanan perlu ditampilkan
    @Override
    protected void updateItem(Pesanan pesanan, boolean empty) {
        super.updateItem(pesanan, empty);

        if (empty || pesanan == null) {
            setGraphic(null);
            return;
        }

        // Mengisi data ke label UI
        orderIdLabel.setText("ORD-" + String.format("%03d", pesanan.getIdPesanan()));
        orderDateLabel.setText(pesanan.getTanggalPesanan().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        pelangganValue.setText(pesanan.getNamaPelanggan());
        phoneValue.setText(pesanan.getNoTelepon());
        layananValue.setText(pesanan.getJenisLayanan());
        jumlahValue.setText(pesanan.getJumlah() + " pcs");
        totalValue.setText("Rp" + String.format("%,.0f", pesanan.getTotalBiaya()));

        statusBadge.setText(pesanan.getStatus());
        statusBadge.getStyleClass().removeIf(styleClass -> styleClass.startsWith("status-"));
        statusBadge.getStyleClass().add("status-badge");
        String statusStyleClass = getStatusStyleClass(pesanan.getStatus());
        statusBadge.getStyleClass().add(statusStyleClass);

        // Hanya tampilkan box spesifikasi jika ada isinya
        if (pesanan.getSpesifikasi() != null && !pesanan.getSpesifikasi().isEmpty()) {
            specText.setText(pesanan.getSpesifikasi());
            specBox.setVisible(true);
            specBox.setManaged(true);
        } else {
            specBox.setVisible(false);
            specBox.setManaged(false);
        }

        // Hanya muncul jika status masih "Baru Dibuat"
        String status = pesanan.getStatus();
        boolean isBaru = status.equalsIgnoreCase("Baru Dibuat");
        konfirmasiButton.setVisible(isBaru);
        konfirmasiButton.setManaged(isBaru);
        editButton.setVisible(isBaru);
        editButton.setManaged(isBaru);
        deleteButton.setVisible(isBaru);
        deleteButton.setManaged(isBaru);

        // Menghubungkan tombol dengan fungsi di Controller
        editButton.setOnAction(e -> onEdit.accept(pesanan));
        deleteButton.setOnAction(e -> onDelete.accept(pesanan));
        konfirmasiButton.setOnAction(e -> onConfirm.accept(pesanan));

        setupComboBox(pesanan);
        setGraphic(cardContainer); // Tampilkan kartu
    }

    /**
     * Menentukan style class berdasarkan status pesanan
     * Mapping status dari database ke CSS class
     */
    private String getStatusStyleClass(String status) {
        if (status == null) return "status-default";

        return switch (status) {
            case "Baru Dibuat" -> "status-baru-dibuat";
            case "Menunggu Pembayaran" -> "status-menunggu-pembayaran";
            case "Pembayaran Verified" -> "status-pembayaran-verified";
            case "Menunggu Desain" -> "status-menunggu-desain";
            case "Desain Direvisi" -> "status-desain-direvisi";
            case "Desain Disetujui" -> "status-desain-disetujui";
            case "Antrian Produksi" -> "status-antrian-produksi";
            case "Sedang Diproduksi" -> "status-sedang-diproduksi";
            case "Produksi Selesai" -> "status-produksi-selesai";
            case "Siap Dikirim" -> "status-siap-dikirim";
            case "Selesai" -> "status-selesai";
            case "Dibatalkan" -> "status-dibatalkan";
            default -> "status-default";
        };
    }

    // Mengatur isi dropdown "Ubah Status"
    private void setupComboBox(Pesanan pesanan) {
        ubahStatusComboBox.setOnAction(null);
        ubahStatusComboBox.setValue(null);
        ubahStatusComboBox.setPromptText("Ubah Status");

        ObservableList<String> statusOptions = FXCollections.observableArrayList();
        // Ambil daftar status dari database agar dinamis
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT nama_status FROM status_pesanan " +
                             "WHERE nama_status IN ('Baru Dibuat', 'Menunggu Pembayaran', 'Pembayaran Verified', 'Dibatalkan') " +
                             "ORDER BY urutan ASC"
             ))  {
            while (rs.next()) statusOptions.add(rs.getString("nama_status"));
        } catch (Exception e) {
            // Fallback jika database error
            statusOptions.addAll("Baru Dibuat", "Menunggu Pembayaran", "Pembayaran Verified", "Dibatalkan");
        }
        ubahStatusComboBox.setItems(statusOptions);

        // Aksi saat status dipilih di dropdown
        ubahStatusComboBox.setOnAction(e -> {
            String statusBaru = ubahStatusComboBox.getValue();
            if (statusBaru == null) return;

            onStatusChange.accept(pesanan, statusBaru); // Kirim ke Controller

            // Reset dropdown setelah dipilih
            Platform.runLater(() -> {
                ubahStatusComboBox.getSelectionModel().clearSelection();
                ubahStatusComboBox.setValue(null);
                ubahStatusComboBox.setPromptText("Ubah Status");
            });
        });
    }
}