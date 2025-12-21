package com.example.trying3.controller.admin;

import com.example.trying3.model.Pesanan;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PesananTerbaruCell extends ListCell<Pesanan> {

    private final HBox container;
    private final VBox infoBox;
    private final Label namaPelangganLabel;
    private final Label jenisLayananLabel;
    private final Label statusLabel;

    public PesananTerbaruCell() {
        // Container utama
        container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(15);
        container.setPadding(new Insets(15, 20, 15, 20));
        container.getStyleClass().add("dashboard-pesanan-item");

        // Info box (nama + jenis layanan)
        infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        namaPelangganLabel = new Label();
        namaPelangganLabel.getStyleClass().add("dashboard-pesanan-nama");

        jenisLayananLabel = new Label();
        jenisLayananLabel.getStyleClass().add("dashboard-pesanan-layanan");

        infoBox.getChildren().addAll(namaPelangganLabel, jenisLayananLabel);

        // Status badge
        statusLabel = new Label();
        statusLabel.getStyleClass().add("dashboard-status-badge");

        container.getChildren().addAll(infoBox, statusLabel);
    }

    @Override
    protected void updateItem(Pesanan pesanan, boolean empty) {
        super.updateItem(pesanan, empty);

        if (empty || pesanan == null) {
            setGraphic(null);
            setText(null);
        } else {
            // Set data
            namaPelangganLabel.setText(pesanan.getNamaPelanggan());
            jenisLayananLabel.setText(pesanan.getJenisLayanan() != null ?
                    pesanan.getJenisLayanan() : "Tidak ada layanan");

            // Set status dengan styling
            String status = pesanan.getStatus();
            statusLabel.setText(status);

            // Reset style classes
            statusLabel.getStyleClass().removeIf(style -> style.startsWith("status-"));
            statusLabel.getStyleClass().add("dashboard-status-badge");

            // Apply status-specific style
            String statusStyleClass = getStatusStyleClass(status);
            statusLabel.getStyleClass().add(statusStyleClass);

            setGraphic(container);
        }
    }

    /**
     * Menentukan style class berdasarkan status pesanan
     */
    private String getStatusStyleClass(String status) {
        if (status == null) return "status-default";

        return switch (status.toLowerCase()) {
            case "baru dibuat" -> "status-baru";
            case "menunggu pembayaran" -> "status-menunggu-pembayaran";
            case "pembayaran verified" -> "status-pembayaran-verified";
            case "menunggu desain" -> "status-menunggu-desain";
            case "desain direvisi" -> "status-desain-revisi";
            case "desain disetujui" -> "status-desain-disetujui";
            case "antrian produksi" -> "status-antrian-produksi";
            case "sedang diproduksi" -> "status-sedang-produksi";
            case "produksi selesai" -> "status-produksi-selesai";
            case "siap dikirim" -> "status-siap-kirim";
            case "selesai" -> "status-selesai";
            case "dibatalkan" -> "status-dibatalkan";
            default -> "status-default";
        };
    }
}