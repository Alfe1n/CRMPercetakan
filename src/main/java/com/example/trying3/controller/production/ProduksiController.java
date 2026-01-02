package com.example.trying3.controller.production;

import com.example.trying3.config.DatabaseConnection;
import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import com.example.trying3.model.ProductionOrder;
import com.example.trying3.util.AlertUtil;
import com.example.trying3.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller untuk halaman Produksi.
 * Menampilkan daftar pesanan yang perlu diproduksi dan menangani aksi produksi.
 */
public class ProduksiController implements Initializable {

    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox orderListContainer;
    @FXML private StackPane kendalaPopupContainer;
    @FXML private Label lblKendalaTitle;
    @FXML private TextArea txtKendala;

    private List<ProductionOrder> orderList = new ArrayList<>();
    private ProductionOrder selectedOrderForIssue;
    private PesananDAO pesananDAO;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pesananDAO = new PesananDAO();

        if (kendalaPopupContainer != null) {
            kendalaPopupContainer.setVisible(false);
            kendalaPopupContainer.setManaged(false);
        }

        loadDataFromDatabase();
        refreshDashboard();
    }

    private void loadDataFromDatabase() {
        orderList.clear();
        List<Pesanan> dbOrders = pesananDAO.getPesananForProduction();

        for (Pesanan p : dbOrders) {
            String fileDesain = (p.getFileDesainPath() != null) ? p.getFileDesainPath() : "";
            String namaPelanggan = (p.getNamaPelanggan() != null) ? p.getNamaPelanggan() : "Unknown";
            String tgl = (p.getTanggalPesanan() != null) ? p.getTanggalPesanan().toLocalDate().toString() : "-";
            String catatan = (p.getCatatan() != null) ? p.getCatatan() : "";

            orderList.add(new ProductionOrder(
                    String.valueOf(p.getIdPesanan()), namaPelanggan, tgl,
                    p.getJenisLayanan(), p.getJumlah(), p.getSpesifikasi(),
                    fileDesain, p.getStatus(), catatan));
        }
    }

    private void refreshDashboard() {
        long waiting = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Antrian Produksi")).count();
        long inProgress = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Sedang Diproduksi")).count();
        long completed = orderList.stream().filter(o -> o.getStatus().equalsIgnoreCase("Siap Dikirim") ||
                o.getStatus().equalsIgnoreCase("Selesai")).count();

        if (waitingLabel != null)
            waitingLabel.setText(String.valueOf(waiting));
        if (inProgressLabel != null)
            inProgressLabel.setText(String.valueOf(inProgress));
        if (completedLabel != null)
            completedLabel.setText(String.valueOf(completed));

        renderOrderList();
    }

    private void renderOrderList() {
        if (orderListContainer != null) {
            orderListContainer.getChildren().clear();

            List<ProductionOrder> activeOrders = orderList.stream()
                    .filter(o -> !o.getStatus().equalsIgnoreCase("Siap Dikirim") &&
                            !o.getStatus().equalsIgnoreCase("Selesai"))
                    .collect(Collectors.toList());

            if (activeOrders.isEmpty()) {
                Label emptyLabel = new Label("Tidak ada antrian produksi saat ini.");
                emptyLabel.getStyleClass().add("muted");
                emptyLabel.setPadding(new Insets(10));
                orderListContainer.getChildren().add(emptyLabel);
                return;
            }

            for (ProductionOrder order : activeOrders) {
                orderListContainer.getChildren().add(createOrderCard(order));
            }
        }
    }

    private VBox createOrderCard(ProductionOrder order) {
        VBox card = new VBox(15);
        card.getStyleClass().add("order-detail-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label("PO-" + order.getId());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1f1f1f;");
        Label lblCust = new Label(order.getCustomerName() + " • " + order.getDate());
        lblCust.getStyleClass().add("muted");
        titleBox.getChildren().addAll(lblId, lblCust);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.getStatus());
        lblStatus.getStyleClass().add("status-badge");
        if (order.getStatus().equalsIgnoreCase("Antrian Produksi")) {
            lblStatus.getStyleClass().add("status-antrian-produksi");
        } else if (order.getStatus().equalsIgnoreCase("Sedang Diproduksi")) {
            lblStatus.getStyleClass().add("status-sedang-diproduksi");
        } else {
            lblStatus.getStyleClass().add("status-produksi-selesai");
        }
        header.getChildren().addAll(titleBox, spacer, lblStatus);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        addDetailRow(grid, 0, "Layanan", order.getService());
        addDetailRow(grid, 1, "Jumlah", order.getQuantity() + " pcs");
        addDetailRow(grid, 2, "Spesifikasi", order.getSpecs());

        if (!order.getCatatan().isEmpty()) {
            Label l = new Label("Catatan");
            l.getStyleClass().add("field-label");
            l.setStyle("-fx-text-fill: #e74c3c;");

            Label v = new Label(order.getCatatan());
            v.getStyleClass().add("field-value");
            v.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            v.setWrapText(true);
            grid.add(l, 0, 3);
            grid.add(v, 1, 3);
        }

        HBox fileBox = new HBox(5);
        if (!order.getFileDesainPath().isEmpty()) {
            Label lblFile = new Label("📂 File Desain: " + new File(order.getFileDesainPath()).getName());
            lblFile.setStyle(
                    "-fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 12px; -fx-cursor: hand; -fx-underline: true;");
            lblFile.setOnMouseClicked(e -> openFile(order.getFileDesainPath()));
            fileBox.getChildren().add(lblFile);
        } else {
            Label lblFile = new Label("⚠️ Belum ada file desain");
            lblFile.getStyleClass().add("muted");
            lblFile.setStyle("-fx-font-style: italic;");
            fileBox.getChildren().add(lblFile);
        }

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnMain = new Button();
        btnMain.getStyleClass().add("button-primary");

        if (order.getStatus().equalsIgnoreCase("Antrian Produksi")) {
            btnMain.setText("Mulai Produksi");
            btnMain.setOnAction(e -> handleStartProduction(order));
        } else if (order.getStatus().equalsIgnoreCase("Sedang Diproduksi")) {
            btnMain.setText("Selesai Produksi");
            btnMain.setOnAction(e -> handleSelesaiProduksi(order));
        }

        Button btnIssue = new Button("Laporkan Kendala");
        btnIssue.getStyleClass().add("button-secondary");
        btnIssue.setStyle("-fx-border-color: #e74c3c; -fx-text-fill: #e74c3c;");
        btnIssue.setOnAction(e -> showKendalaPopup(order));

        actions.getChildren().addAll(btnMain, btnIssue);
        card.getChildren().addAll(header, grid, fileBox, actions);

        return card;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        Label v = new Label(value != null ? value : "-");
        v.getStyleClass().add("field-value");
        v.setWrapText(true);
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists())
                Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleStartProduction(ProductionOrder order) {
        int pesananId = Integer.parseInt(order.getId());

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int idProduksi = 0;
            String sqlCek = "SELECT id_produksi FROM produksi WHERE id_pesanan = ?";
            try (PreparedStatement psCek = conn.prepareStatement(sqlCek)) {
                psCek.setInt(1, pesananId);
                try (ResultSet rs = psCek.executeQuery()) {
                    if (rs.next()) {
                        idProduksi = rs.getInt("id_produksi");
                    }
                }
            }

            if (idProduksi == 0) {
                String sqlInsert = """
                            INSERT INTO produksi (id_pesanan, id_operator, tanggal_mulai, status_produksi, progres_persen)
                            VALUES (?, ?, NOW(), 'proses', 0)
                        """;
                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    psInsert.setInt(1, pesananId);
                    int currentUserId = SessionManager.getInstance().getCurrentUserId();
                    psInsert.setInt(2, currentUserId);
                    psInsert.executeUpdate();

                    try (ResultSet rsKey = psInsert.getGeneratedKeys()) {
                        if (rsKey.next()) {
                            idProduksi = rsKey.getInt(1);
                            System.out.println("✅ Inserted produksi record, id_produksi: " + idProduksi);
                        }
                    }
                }
            } else {
                String sqlUpdate = "UPDATE produksi SET status_produksi = 'proses', tanggal_mulai = NOW() WHERE id_produksi = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                    psUpdate.setInt(1, idProduksi);
                    psUpdate.executeUpdate();
                    System.out.println("✅ Updated existing produksi record, id_produksi: " + idProduksi);
                }
            }

            String sqlUpdatePesanan = """
                        UPDATE pesanan
                        SET id_status = (SELECT id_status FROM status_pesanan WHERE nama_status = 'Sedang Diproduksi'),
                            updated_at = NOW()
                        WHERE id_pesanan = ?
                    """;
            try (PreparedStatement psUpdatePesanan = conn.prepareStatement(sqlUpdatePesanan)) {
                psUpdatePesanan.setInt(1, pesananId);
                psUpdatePesanan.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Produksi dimulai untuk pesanan ID: " + pesananId);
            AlertUtil.showInfo("Berhasil", "Produksi dimulai!");

        } catch (Exception e) {
            if (conn != null)
                try {
                    conn.rollback();
                } catch (Exception ex) {
                }
            e.printStackTrace();
            AlertUtil.showError("Error", "Gagal memulai produksi: " + e.getMessage());
            return;
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (Exception ex) {
                }
        }

        loadDataFromDatabase();
        refreshDashboard();
    }

    private void handleSelesaiProduksi(ProductionOrder order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Tandai produksi sebagai selesai?\nSemua kendala terkait akan di-resolve.",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                boolean success = pesananDAO.selesaikanProduksi(Integer.parseInt(order.getId()));

                if (success) {
                    AlertUtil.showInfo("Berhasil", "Produksi selesai! Pesanan siap dikirim.");
                    loadDataFromDatabase();
                    refreshDashboard();
                } else {
                    AlertUtil.showError("Gagal", "Terjadi kesalahan saat update status.");
                }
            }
        });
    }

    /**
     * Menampilkan popup kendala untuk melaporkan masalah produksi.
     */
    private void showKendalaPopup(ProductionOrder order) {
        this.selectedOrderForIssue = order;

        if (lblKendalaTitle != null) {
            lblKendalaTitle.setText("Kendala PO-" + order.getId());
        }
        if (txtKendala != null) {
            txtKendala.clear();
        }

        if (kendalaPopupContainer != null) {
            kendalaPopupContainer.setVisible(true);
            kendalaPopupContainer.setManaged(true);
        }
    }

    @FXML
    private void submitKendala() {
        if (selectedOrderForIssue == null || txtKendala == null)
            return;

        String deskripsi = txtKendala.getText();
        if (deskripsi == null || deskripsi.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Mohon isi deskripsi kendala.");
            return;
        }

        int idPesanan = Integer.parseInt(selectedOrderForIssue.getId());
        int idUser = SessionManager.getInstance().getCurrentUserId();
        if (idUser == -1)
            idUser = 1;

        boolean success = pesananDAO.simpanKendalaProduksi(idPesanan, deskripsi, idUser);

        if (success) {
            AlertUtil.showInfo("Berhasil", "Kendala berhasil dilaporkan ke Admin.");
            loadDataFromDatabase();
            refreshDashboard();
            cancelKendala();
        } else {
            AlertUtil.showError("Gagal", "Terjadi kesalahan saat menyimpan kendala.");
        }
    }

    @FXML
    private void cancelKendala() {
        if (kendalaPopupContainer != null) {
            kendalaPopupContainer.setVisible(false);
            kendalaPopupContainer.setManaged(false);
        }
        selectedOrderForIssue = null;
        if (txtKendala != null) {
            txtKendala.clear();
        }
    }
}