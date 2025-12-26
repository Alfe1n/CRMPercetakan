package com.example.trying3.controller.admin;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Notifikasi;
import com.example.trying3.util.AlertUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Custom ListCell untuk menampilkan notifikasi dalam format kartu
 * Termasuk tombol aksi untuk "Siap Dikirim" -> "Tandai Selesai"
 */
public class NotifikasiCell extends ListCell<Notifikasi> {

    private final HBox cardContainer;
    private final VBox iconBox;
    private final Label iconLabel;
    private final VBox contentBox;
    private final Label judulLabel;
    private final Label pesanLabel;
    private final HBox metaBox;
    private final Label nomorPesananLabel;
    private final Label sumberLabel;
    private final VBox rightBox;
    private final Label waktuLabel;
    private final Label tipeBadge;
    private final Button btnSelesai;

    private final PesananDAO pesananDAO;
    private Consumer<Notifikasi> onSelesaiCallback;

    public NotifikasiCell() {
        this(null);
    }

    public NotifikasiCell(Consumer<Notifikasi> onSelesaiCallback) {
        this.onSelesaiCallback = onSelesaiCallback;
        this.pesananDAO = new PesananDAO();

        // Card container
        cardContainer = new HBox(15);
        cardContainer.getStyleClass().add("notifikasi-card");
        cardContainer.setPadding(new Insets(15));
        cardContainer.setAlignment(Pos.TOP_LEFT);

        // Icon
        iconBox = new VBox();
        iconBox.setAlignment(Pos.TOP_CENTER);
        iconBox.setPrefWidth(50);

        iconLabel = new Label();
        iconLabel.setStyle("-fx-font-size: 24px; -fx-padding: 10;");
        iconBox.getChildren().add(iconLabel);

        // Content
        contentBox = new VBox(5);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        judulLabel = new Label();
        judulLabel.getStyleClass().add("notifikasi-judul");
        judulLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        judulLabel.setWrapText(true);

        pesanLabel = new Label();
        pesanLabel.getStyleClass().add("notifikasi-pesan");
        pesanLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        pesanLabel.setWrapText(true);

        metaBox = new HBox(10);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        nomorPesananLabel = new Label();
        nomorPesananLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label dotSeparator = new Label("•");
        dotSeparator.setStyle("-fx-text-fill: #999;");

        sumberLabel = new Label();
        sumberLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        metaBox.getChildren().addAll(nomorPesananLabel, dotSeparator, sumberLabel);

        contentBox.getChildren().addAll(judulLabel, pesanLabel, metaBox);

        // Right side (waktu & badge & tombol)
        rightBox = new VBox(8);
        rightBox.setAlignment(Pos.TOP_RIGHT);
        rightBox.setMinWidth(120);

        waktuLabel = new Label();
        waktuLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        tipeBadge = new Label();
        tipeBadge.setStyle("-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Tombol Tandai Selesai (hanya muncul untuk Siap Dikirim)
        btnSelesai = new Button("✓ Selesai");
        btnSelesai.setStyle(
                "-fx-background-color: #27ae60; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;"
        );
        btnSelesai.setVisible(false);
        btnSelesai.setManaged(false);

        rightBox.getChildren().addAll(waktuLabel, tipeBadge, btnSelesai);

        cardContainer.getChildren().addAll(iconBox, contentBox, rightBox);

        // Set padding untuk cell
        setPadding(new Insets(5, 10, 5, 10));
    }

    /**
     * Set callback yang akan dipanggil ketika tombol Selesai diklik
     */
    public void setOnSelesaiCallback(Consumer<Notifikasi> callback) {
        this.onSelesaiCallback = callback;
    }

    @Override
    protected void updateItem(Notifikasi notifikasi, boolean empty) {
        super.updateItem(notifikasi, empty);

        if (empty || notifikasi == null) {
            setText(null);
            setGraphic(null);
        } else {
            // Set icon berdasarkan tipe
            iconLabel.setText(notifikasi.getIcon());
            iconLabel.setStyle("-fx-font-size: 24px; -fx-padding: 10; " +
                    "-fx-background-color: " + notifikasi.getBadgeColor() + "20; " +
                    "-fx-background-radius: 25;");

            // Set content
            judulLabel.setText(notifikasi.getJudul());
            pesanLabel.setText(notifikasi.getPesan());
            nomorPesananLabel.setText(notifikasi.getNomorPesanan());
            sumberLabel.setText(notifikasi.getSumberDivisi());

            // Set waktu
            waktuLabel.setText(notifikasi.getWaktuRelatif());

            // Set badge
            tipeBadge.setText(notifikasi.getTipeLabel());
            tipeBadge.setStyle("-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-background-color: " + notifikasi.getBadgeColor() + "; " +
                    "-fx-text-fill: white;");

            // Tampilkan tombol "Tandai Selesai" hanya untuk tipe SIAP_DIKIRIM
            boolean isSiapDikirim = Notifikasi.TIPE_SIAP_DIKIRIM.equals(notifikasi.getTipe());
            btnSelesai.setVisible(isSiapDikirim);
            btnSelesai.setManaged(isSiapDikirim);

            if (isSiapDikirim) {
                btnSelesai.setOnAction(e -> handleTandaiSelesai(notifikasi));
            }

            // Apply different style if unread
            if (!notifikasi.isSudahDibaca()) {
                cardContainer.setStyle("-fx-background-color: #f0f8ff; -fx-background-radius: 8; -fx-border-color: #d0e0f0; -fx-border-radius: 8;");
            } else {
                cardContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
            }

            setGraphic(cardContainer);
        }
    }

    /**
     * Handler untuk tombol "Tandai Selesai"
     * Mengubah status pesanan dari "Siap Dikirim" ke "Selesai"
     */
    private void handleTandaiSelesai(Notifikasi notifikasi) {
        // Konfirmasi dulu
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Pesanan Selesai");
        confirm.setHeaderText("Tandai Pesanan Selesai?");
        confirm.setContentText(
                "Pesanan: " + notifikasi.getNomorPesanan() + "\n" +
                        "Pelanggan: " + notifikasi.getNamaPelanggan() + "\n\n" +
                        "Pastikan barang sudah dikirim/diambil oleh pelanggan."
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update status ke "Selesai"
                int idPesanan = notifikasi.getIdPesanan();
                boolean success = pesananDAO.updateStatus(idPesanan, "Selesai");

                if (success) {
                    AlertUtil.showInfo("Berhasil",
                            "Pesanan " + notifikasi.getNomorPesanan() + " telah ditandai SELESAI.");

                    // Panggil callback untuk refresh list
                    if (onSelesaiCallback != null) {
                        onSelesaiCallback.accept(notifikasi);
                    }
                } else {
                    AlertUtil.showError("Gagal", "Gagal mengubah status pesanan. Silakan coba lagi.");
                }
            }
        });
    }
}