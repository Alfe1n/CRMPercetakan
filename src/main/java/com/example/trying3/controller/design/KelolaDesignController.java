package com.example.trying3.controller.design;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.AlertUtil;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class KelolaDesignController implements Initializable {

    @FXML private Label lblCountWaiting;
    @FXML private Label lblCountRevision;
    @FXML private Label lblCountApproved;
    @FXML private VBox ordersContainer;

    private PesananDAO pesananDAO;
    private List<Pesanan> orderList = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pesananDAO = new PesananDAO();
        loadData();
    }

    private void loadData() {
        orderList = pesananDAO.getPesananForDesignTeam();
        refreshUI();
    }

    // Perbarui UI berdasarkan data terbaru
    private void refreshUI() {
        updateStatistics();
        renderOrderCards();
    }

    // Perbarui statistik di dashboard
    private void updateStatistics() {
        long waiting = orderList.stream()
                .filter(o -> "Menunggu Desain".equals(o.getStatus()) || "Pembayaran Verified".equals(o.getStatus()))
                .count();
        long revision = orderList.stream()
                .filter(o -> "Desain Direvisi".equals(o.getStatus()))
                .count();
        long approved = orderList.stream()
                .filter(o -> "Desain Disetujui".equals(o.getStatus()))
                .count();

        if (lblCountWaiting != null) lblCountWaiting.setText(String.valueOf(waiting));
        if (lblCountRevision != null) lblCountRevision.setText(String.valueOf(revision));
        if (lblCountApproved != null) lblCountApproved.setText(String.valueOf(approved));
    }


    // Render kartu pesanan di antrian desain
    private void renderOrderCards() {
        if (ordersContainer == null) return;

        ordersContainer.getChildren().clear();

        // Filter hanya yang belum disetujui untuk ditampilkan di antrian
        List<Pesanan> antrianList = orderList.stream()
                .filter(o -> !"Desain Disetujui".equals(o.getStatus()))
                .toList();

        if (antrianList.isEmpty()) {
            Label emptyLabel = new Label("Tidak ada pesanan dalam antrian desain.");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
            ordersContainer.getChildren().add(emptyLabel);
        } else {
            for (Pesanan order : antrianList) {
                ordersContainer.getChildren().add(createOrderCard(order));
            }
        }
    }


    // Buat kartu pesanan individual
    private VBox createOrderCard(Pesanan order) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-border-color: #e8e8e8; -fx-border-width: 1; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 20;");
        card.setSpacing(15);

        // === HEADER ===
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox identityBox = new VBox(2);
        Label lblId = new Label(order.getDisplayId());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111;");

        Label lblClient = new Label(order.getNamaPelanggan());
        lblClient.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        Label lblDate = new Label(order.getFormattedDate());
        lblDate.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        identityBox.getChildren().addAll(lblId, lblClient, lblDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = createStatusLabel(order.getStatus());

        header.getChildren().addAll(identityBox, spacer, lblStatus);

        // === DETAIL INFO ===
        VBox details = new VBox(8);
        details.getChildren().add(createDetailRow("Layanan:", order.getJenisLayanan()));
        details.getChildren().add(createDetailRow("Jumlah:", order.getFormattedJumlah()));
        details.getChildren().add(createDetailRow("Spesifikasi:",
                order.getSpesifikasi() != null ? order.getSpesifikasi() : "-"));
        details.getChildren().add(createDetailRow("Catatan:",
                order.getCatatan() != null ? order.getCatatan() : "-"));

        // === ACTION BUTTONS ===
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnUpload = new Button("↑ Upload Desain");
        styleButton(btnUpload, false);
        btnUpload.setOnAction(e -> handleUploadDesign(order));

        Button btnApprove = new Button("✓ Setujui Desain");
        styleButton(btnApprove, true);
        btnApprove.setOnAction(e -> handleApproveDesign(order));

        Button btnRevise = new Button("✎ Perlu Revisi");
        styleButton(btnRevise, false);
        btnRevise.setOnAction(e -> handleRequestRevision(order));

        // Tampilkan tombol berdasarkan status
        if ("Menunggu Desain".equals(order.getStatus()) || "Pembayaran Verified".equals(order.getStatus())) {
            actions.getChildren().addAll(btnUpload, btnApprove, btnRevise);
        } else if ("Desain Direvisi".equals(order.getStatus())) {
            actions.getChildren().addAll(btnUpload, btnApprove);
        }

        card.getChildren().addAll(header, details, actions);
        return card;
    }

    // Buat label status dengan gaya dinamis
    private Label createStatusLabel(String status) {
        Label lblStatus = new Label(status);
        lblStatus.setStyle(getStatusStyle(status));
        return lblStatus;
    }

    // Tentukan gaya status berdasarkan nilai status
    private String getStatusStyle(String status) {
        String baseStyle = "-fx-padding: 6 14; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 11px;";

        return switch (status) {
            case "Menunggu Desain" -> baseStyle + "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
            case "Desain Direvisi" -> baseStyle + "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626;";
            case "Desain Disetujui" -> baseStyle + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            default -> baseStyle + "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
        };
    }

    // Buat baris detail informasi pesanan
    private HBox createDetailRow(String labelText, String valueText) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");
        lbl.setMinWidth(80);

        Label val = new Label(valueText != null ? valueText : "-");
        val.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
        val.setWrapText(true);

        row.getChildren().addAll(lbl, val);
        return row;
    }

    // Gaya tombol konsisten
    private void styleButton(Button btn, boolean isPrimary) {
        String baseStyle = "-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 8 16; " +
                "-fx-background-radius: 6; -fx-cursor: hand;";

        if (isPrimary) {
            btn.setStyle(baseStyle + "-fx-background-color: #1f1f1f; -fx-text-fill: white;");
            btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-background-color: #333; -fx-text-fill: white;"));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle + "-fx-background-color: #1f1f1f; -fx-text-fill: white;"));
        } else {
            btn.setStyle(baseStyle + "-fx-background-color: white; -fx-text-fill: #333; " +
                    "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 6;");
            btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-background-color: #f5f5f5; -fx-text-fill: #333; " +
                    "-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 6;"));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle + "-fx-background-color: white; -fx-text-fill: #333; " +
                    "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 6;"));
        }
    }

    // Tangani upload desain
    private void handleUploadDesign(Pesanan order) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File Desain");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Design Files", "*.psd", "*.ai", "*.cdr"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(ordersContainer.getScene().getWindow());

        if (selectedFile != null) {
            AlertUtil.showInfo("Upload Berhasil",
                    "File desain untuk " + order.getDisplayId() + " berhasil diupload.\n" +
                            "File: " + selectedFile.getName());

            // Refresh data
            loadData();
        }
    }

    // Tangani approval desain
    private void handleApproveDesign(Pesanan order) {
        boolean confirm = AlertUtil.showConfirmation("Konfirmasi Approval",
                "Apakah Anda yakin ingin menyetujui desain untuk pesanan " + order.getDisplayId() + "?\n" +
                        "Pesanan akan dilanjutkan ke tahap produksi.");

        if (confirm) {
            boolean success = pesananDAO.updateDesignStatus(order.getIdPesanan(), "approve");

            if (success) {
                AlertUtil.showInfo("Desain Disetujui",
                        "Desain untuk " + order.getDisplayId() + " telah disetujui.\n" +
                                "Pesanan siap untuk produksi.");
                loadData();
            } else {
                AlertUtil.showError("Gagal", "Gagal mengupdate status pesanan. Silakan coba lagi.");
            }
        }
    }

    // Tangani permintaan revisi desain
    private void handleRequestRevision(Pesanan order) {
        boolean confirm = AlertUtil.showConfirmation("Konfirmasi Revisi",
                "Apakah Anda yakin desain untuk pesanan " + order.getDisplayId() + " perlu direvisi?");

        if (confirm) {
            boolean success = pesananDAO.updateDesignStatus(order.getIdPesanan(), "revisi");

            if (success) {
                AlertUtil.showInfo("Status Diubah",
                        "Pesanan " + order.getDisplayId() + " ditandai perlu revisi.");
                loadData();
            } else {
                AlertUtil.showError("Gagal", "Gagal mengupdate status pesanan. Silakan coba lagi.");
            }
        }
    }

    public void refresh() {
        Platform.runLater(this::loadData);
    }
}