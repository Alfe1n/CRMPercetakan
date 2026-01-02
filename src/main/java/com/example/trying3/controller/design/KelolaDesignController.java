package com.example.trying3.controller.design;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.DesainInfo;
import com.example.trying3.model.Pesanan;
import com.example.trying3.util.AlertUtil;
import com.example.trying3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller untuk halaman Kelola Desain.
 * Menangani upload desain, approval, dan revisi dari pelanggan.
 */
public class KelolaDesignController implements Initializable {

    @FXML private VBox ordersContainer;
    @FXML private Label lblCountWaiting;
    @FXML private Label lblCountRevision;
    @FXML private Label lblCountApproved;
    @FXML private StackPane revisiPopupContainer;
    @FXML private TextArea txtRevisi;

    private PesananDAO pesananDAO;
    private Pesanan currentRevisiOrder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pesananDAO = new PesananDAO();

        if (revisiPopupContainer != null) {
            revisiPopupContainer.setVisible(false);
            revisiPopupContainer.setManaged(false);
        }

        loadData();
    }

    private void loadData() {
        if (ordersContainer != null) {
            ordersContainer.getChildren().clear();
        }

        List<Pesanan> orders = pesananDAO.getPesananForDesignTeam();

        int countMenunggu = 0;
        int countRevisi = 0;
        int countDisetujui = 0;

        for (Pesanan p : orders) {
            String status = p.getStatus();

            if (status == null) continue;

            if (status.equals("Menunggu Desain") || status.equals("Pembayaran Verified")) {
                countMenunggu++;
            } else if (status.equals("Desain Direvisi")) {
                countRevisi++;
            } else if (status.equals("Desain Disetujui") || status.equals("Antrian Produksi")) {
                countDisetujui++;
            }

            if (ordersContainer != null) {
                ordersContainer.getChildren().add(createOrderCard(p));
            }
        }

        if (lblCountWaiting != null) lblCountWaiting.setText(String.valueOf(countMenunggu));
        if (lblCountRevision != null) lblCountRevision.setText(String.valueOf(countRevisi));
        if (lblCountApproved != null) lblCountApproved.setText(String.valueOf(countDisetujui));
    }

    private VBox createOrderCard(Pesanan p) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);

        Label lblDate = new Label(p.getTanggalPesanan() != null ? p.getTanggalPesanan().toLocalDate().toString() : "-");
        lblDate.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

        String nomorPesanan = p.getNomorPesanan();
        Label lblNomor = new Label(nomorPesanan != null ? nomorPesanan : "-");
        lblNomor.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #333;");

        titleBox.getChildren().addAll(lblDate, lblNomor);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String statusText = p.getStatus() != null ? p.getStatus() : "Unknown";
        Label lblStatus = new Label(statusText);
        if (statusText.contains("Revisi")) {
            lblStatus.setStyle(
                    "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 5 10; -fx-background-radius: 15;");
        } else if (statusText.contains("Verified")) {
            lblStatus.setStyle(
                    "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-padding: 5 10; -fx-background-radius: 15;");
        } else {
            lblStatus.setStyle(
                    "-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-padding: 5 10; -fx-background-radius: 15;");
        }

        header.getChildren().addAll(titleBox, spacer, lblStatus);

        String namaPelanggan = p.getNamaPelanggan();
        Label lblCustomer = new Label(namaPelanggan != null && !namaPelanggan.isEmpty() ? namaPelanggan : "Pelanggan");
        lblCustomer.setStyle("-fx-text-fill: #333; -fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(5);
        grid.setPadding(new Insets(10, 0, 0, 0));

        addDetailRow(grid, 0, "Layanan:", p.getJenisLayanan() != null ? p.getJenisLayanan() : "-");
        addDetailRow(grid, 1, "Jumlah:", p.getJumlah() + " pcs");
        addDetailRow(grid, 2, "Spesifikasi:", p.getSpesifikasi() != null ? p.getSpesifikasi() : "-");

        HBox fileBox = new HBox(5);
        String filePath = p.getFileDesainPath();
        if (filePath != null && !filePath.isEmpty()) {
            Label lblFile = new Label("File: " + new File(filePath).getName());
            lblFile.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 11px;");
            fileBox.getChildren().add(lblFile);
        } else {
            Label lblFile = new Label("Belum ada file desain");
            lblFile.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 11px;");
            fileBox.getChildren().add(lblFile);
        }

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnUpload = new Button("Upload Desain");
        btnUpload.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-cursor: hand;");
        btnUpload.setOnAction(e -> handleUpload(p));

        Button btnApprove = new Button("Setujui Desain");
        btnApprove.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white; -fx-cursor: hand;");
        if (filePath == null || filePath.isEmpty()) {
            btnApprove.setDisable(true);
            btnApprove.setTooltip(new Tooltip("Upload desain dulu sebelum approve"));
        }
        btnApprove.setOnAction(e -> handleApprove(p));

        Button btnRevisi = new Button("Perlu Revisi");
        btnRevisi.setStyle(
                "-fx-background-color: white; -fx-border-color: #ff9800; -fx-text-fill: #ff9800; -fx-cursor: hand;");
        btnRevisi.setOnAction(e -> handleOpenRevisiPopup(p));

        actions.getChildren().addAll(btnUpload, btnApprove, btnRevisi);

        card.getChildren().addAll(header, lblCustomer, grid, fileBox, actions);

        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
        v.setWrapText(true);

        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }


    private void handleUpload(Pesanan p) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File Desain");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.ai", "*.psd"));
        Stage stage = (Stage) ordersContainer.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                File destDir = new File("design_storage");
                if (!destDir.exists())
                    destDir.mkdirs();
                String newFileName = "DESAIN_" + p.getNomorPesanan() + "_" + System.currentTimeMillis() + "_"
                        + selectedFile.getName();
                File destFile = new File(destDir, newFileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                int idDesigner = SessionManager.getInstance().getCurrentUserId();
                if (idDesigner == -1)
                    idDesigner = 1;

                boolean success = pesananDAO.simpanDesainDenganRevisi(p.getIdPesanan(), destFile.getPath(), idDesigner);

                if (success) {
                    String currentStatus = p.getStatus();
                    if ("Desain Direvisi".equals(currentStatus) || "Menunggu Desain".equals(currentStatus)) {
                        pesananDAO.updateStatus(p.getIdPesanan(), "Menunggu Desain");
                    }

                    loadData();

                    DesainInfo info = pesananDAO.getDesainInfo(p.getIdPesanan());
                    String message = "File berhasil diupload!";
                    if (info != null && info.getRevisiKe() > 1) {
                        message = "File revisi ke-" + info.getRevisiKe() + " berhasil diupload!";
                    }
                    AlertUtil.showInfo("Sukses", message);
                } else {
                    AlertUtil.showError("Error", "Gagal menyimpan desain ke database.");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                AlertUtil.showError("Error", "Gagal upload file: " + ex.getMessage());
            }
        }
    }

    private void handleApprove(Pesanan p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Apakah pelanggan sudah setuju?\nPesanan akan dikirim ke Tim Produksi.",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                boolean success = pesananDAO.approveDesain(p.getIdPesanan());
                if (success) {
                    loadData();
                    AlertUtil.showInfo("Sukses", "Desain disetujui! Pesanan masuk ke antrian produksi.");
                } else {
                    AlertUtil.showError("Error", "Gagal update database.");
                }
            }
        });
    }


    private void handleOpenRevisiPopup(Pesanan p) {
        this.currentRevisiOrder = p;
        if (txtRevisi != null)
            txtRevisi.clear();
        if (revisiPopupContainer != null) {
            revisiPopupContainer.setVisible(true);
            revisiPopupContainer.setManaged(true);
        }
    }

    @FXML
    private void submitRevisi() {
        if (currentRevisiOrder == null)
            return;

        String alasan = txtRevisi.getText();
        if (alasan == null || alasan.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Mohon isi alasan revisi dari pelanggan.");
            return;
        }

        boolean successStatus = pesananDAO.updateDesignStatus(currentRevisiOrder.getIdPesanan(), "revisi");
        pesananDAO.updateCatatan(currentRevisiOrder.getIdPesanan(), "REVISI: " + alasan);

        if (successStatus) {
            AlertUtil.showInfo("Berhasil", "Status pesanan diubah menjadi Perlu Revisi.");
            loadData();
            cancelRevisi();
        } else {
            AlertUtil.showError("Gagal", "Terjadi kesalahan saat update status.");
        }
    }

    @FXML
    private void cancelRevisi() {
        if (revisiPopupContainer != null) {
            revisiPopupContainer.setVisible(false);
            revisiPopupContainer.setManaged(false);
        }
        currentRevisiOrder = null;
        if (txtRevisi != null)
            txtRevisi.clear();
    }
}