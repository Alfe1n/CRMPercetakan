package com.example.trying3.controller.design;

import com.example.trying3.dao.PesananDAO;
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

public class KelolaDesignController implements Initializable {

    // --- FXML ELEMENTS ---
    @FXML private VBox ordersContainer;

    // Statistik Labels
    @FXML private Label lblCountWaiting;
    @FXML private Label lblCountRevision;
    @FXML private Label lblCountApproved;

    // --- POPUP REVISI ELEMENTS (Sesuai FXML) ---
    @FXML private VBox revisiPopupContainer;
    @FXML private TextArea txtRevisi;

    private PesananDAO pesananDAO;

    // Variable bantuan untuk menyimpan pesanan mana yang sedang direvisi
    private Pesanan currentRevisiOrder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pesananDAO = new PesananDAO();

        // Pastikan popup tersembunyi saat awal load
        if(revisiPopupContainer != null) {
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
            if (status.contains("Menunggu") || status.contains("Verified")) countMenunggu++;
            else if (status.contains("Revisi")) countRevisi++;
            else if (status.contains("Disetujui") || status.contains("Antrian")) countDisetujui++;

            if (ordersContainer != null) {
                ordersContainer.getChildren().add(createOrderCard(p));
            }
        }

        if(lblCountWaiting != null) lblCountWaiting.setText(String.valueOf(countMenunggu));
        if(lblCountRevision != null) lblCountRevision.setText(String.valueOf(countRevisi));
        if(lblCountApproved != null) lblCountApproved.setText(String.valueOf(countDisetujui));
    }

    private VBox createOrderCard(Pesanan p) {
        VBox card = new VBox(10);
        // Menggunakan styling CSS yang sama dengan Produksi (Card Style)
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPadding(new Insets(15));

        // --- Header ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label("PO-" + p.getNomorPesanan());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label lblDate = new Label(p.getTanggalPesanan() != null ? p.getTanggalPesanan().toLocalDate().toString() : "-");
        lblDate.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(lblId, lblDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(p.getStatus());
        lblStatus.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-padding: 5 10; -fx-background-radius: 15;");
        if (p.getStatus().contains("Revisi")) {
            lblStatus.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 5 10; -fx-background-radius: 15;");
        }
        header.getChildren().addAll(titleBox, spacer, lblStatus);

        // --- Info Customer ---
        Label lblCust = new Label(p.getNamaPelanggan());
        lblCust.setStyle("-fx-text-fill: #333; -fx-font-weight: bold; -fx-font-size: 12px;");

        // --- Detail Grid ---
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(5);

        addDetailRow(grid, 0, "Layanan:", p.getJenisLayanan());
        addDetailRow(grid, 1, "Jumlah:", p.getJumlah() + " pcs");
        addDetailRow(grid, 2, "Spesifikasi:", p.getSpesifikasi());

        // File Link
        HBox fileBox = new HBox(5);
        if (p.getFileDesainPath() != null && !p.getFileDesainPath().isEmpty()) {
            Label lblFile = new Label("File: " + new File(p.getFileDesainPath()).getName());
            lblFile.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 11px;");
            fileBox.getChildren().add(lblFile);
        } else {
            Label lblFile = new Label("Belum ada file desain");
            lblFile.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 11px;");
            fileBox.getChildren().add(lblFile);
        }

        // --- Tombol Aksi ---
        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnUpload = new Button("Upload Desain");
        btnUpload.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-cursor: hand;");
        btnUpload.setOnAction(e -> handleUpload(p));

        Button btnApprove = new Button("Setujui Desain");
        btnApprove.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white; -fx-cursor: hand;");
        if (p.getFileDesainPath() == null || p.getFileDesainPath().isEmpty()) {
            btnApprove.setDisable(true);
            btnApprove.setTooltip(new Tooltip("Upload desain dulu sebelum approve"));
        }
        btnApprove.setOnAction(e -> handleApprove(p));

        Button btnRevisi = new Button("Perlu Revisi");
        btnRevisi.setStyle("-fx-background-color: white; -fx-border-color: #ff9800; -fx-text-fill: #ff9800; -fx-cursor: hand;");
        btnRevisi.setOnAction(e -> handleOpenRevisiPopup(p));

        actions.getChildren().addAll(btnUpload, btnApprove, btnRevisi);
        card.getChildren().addAll(header, lblCust, grid, fileBox, actions);
        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label); l.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");
        Label v = new Label(value); v.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;"); v.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    // --- LOGIC ACTIONS ---

    private void handleUpload(Pesanan p) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File Desain");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.ai", "*.psd")
        );
        Stage stage = (Stage) ordersContainer.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                File destDir = new File("design_storage");
                if (!destDir.exists()) destDir.mkdirs();
                String newFileName = "DESAIN_" + p.getNomorPesanan() + "_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                File destFile = new File(destDir, newFileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                int idDesigner = SessionManager.getInstance().getCurrentUserId();
                if (idDesigner == -1) idDesigner = 1;

                boolean success = pesananDAO.simpanDesain(p.getIdPesanan(), destFile.getPath(), idDesigner);
                if (success) {
                    pesananDAO.updateStatus(p.getIdPesanan(), "Menunggu Desain");
                    loadData();
                    AlertUtil.showInfo("Sukses", "File berhasil diupload!");
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

    // =======================================================
    // LOGIC POPUP REVISI (Menghubungkan dengan FXML)
    // =======================================================

    // 1. Membuka Popup saat tombol "Perlu Revisi" diklik di kartu
    private void handleOpenRevisiPopup(Pesanan p) {
        this.currentRevisiOrder = p; // Simpan pesanan mana yang sedang direvisi
        txtRevisi.clear(); // Kosongkan text area
        revisiPopupContainer.setVisible(true); // Tampilkan popup
        revisiPopupContainer.setManaged(true); // Atur layout agar terlihat
    }

    // 2. Button Action: "Kirim Revisi" (dipanggil dari FXML onAction="#submitRevisi")
    @FXML
    private void submitRevisi() {
        if (currentRevisiOrder == null) return;

        String alasan = txtRevisi.getText();
        if (alasan == null || alasan.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Mohon isi alasan revisi dari pelanggan.");
            return;
        }

        // Update status ke "Desain Direvisi"
        boolean successStatus = pesananDAO.updateDesignStatus(currentRevisiOrder.getIdPesanan(), "revisi");

        // (Opsional) Update catatan alasan revisi ke database
        // Jika DAO kamu punya method updateCatatan, gunakan ini:
        pesananDAO.updateCatatan(currentRevisiOrder.getIdPesanan(), "REVISI: " + alasan);

        if (successStatus) {
            AlertUtil.showInfo("Berhasil", "Status pesanan diubah menjadi Perlu Revisi.");
            loadData();   // Refresh data
            cancelRevisi(); // Tutup popup
        } else {
            AlertUtil.showError("Gagal", "Terjadi kesalahan saat update status.");
        }
    }

    // 3. Button Action: "Batal" (dipanggil dari FXML onAction="#cancelRevisi")
    @FXML
    private void cancelRevisi() {
        revisiPopupContainer.setVisible(false);
        revisiPopupContainer.setManaged(false);
        currentRevisiOrder = null;
        txtRevisi.clear();
    }
}