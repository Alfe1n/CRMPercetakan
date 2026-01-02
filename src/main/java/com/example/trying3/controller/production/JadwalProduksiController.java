package com.example.trying3.controller.production;

import com.example.trying3.config.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller untuk halaman Jadwal Produksi.
 * Menampilkan daftar jadwal produksi yang sedang aktif.
 */
public class JadwalProduksiController implements Initializable {

    @FXML private VBox scheduleContainer;
    @FXML private Label lblDateRange;

    private List<ScheduleItem> scheduleList = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDataFromDatabase();
        renderSchedule();
    }

    private void loadDataFromDatabase() {
        scheduleList.clear();

        String query = """
                    SELECT
                        p.nomor_pesanan,
                        jl.nama_layanan,
                        pl.nama AS nama_pelanggan,
                        dp.jumlah,
                        pr.status_produksi
                    FROM produksi pr
                    JOIN pesanan p ON pr.id_pesanan = p.id_pesanan
                    JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
                    JOIN detail_pesanan dp ON p.id_pesanan = dp.id_pesanan
                    JOIN jenis_layanan jl ON dp.id_layanan = jl.id_layanan
                    WHERE pr.status_produksi IN ('antrian', 'proses', 'terkendala')
                    ORDER BY pr.tanggal_mulai ASC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String statusDisplay = formatStatus(rs.getString("status_produksi"));

                scheduleList.add(new ScheduleItem(
                        rs.getString("nomor_pesanan"),
                        rs.getString("nama_layanan"),
                        rs.getString("nama_pelanggan"),
                        rs.getInt("jumlah"),
                        statusDisplay));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatStatus(String statusDb) {
        return switch (statusDb.toLowerCase()) {
            case "antrian" -> "Antrian Produksi";
            case "proses" -> "Sedang Diproduksi";
            case "terkendala" -> "Terkendala";
            case "selesai" -> "Selesai Produksi";
            default -> statusDb;
        };
    }

    private void renderSchedule() {
        scheduleContainer.getChildren().clear();

        if (scheduleList.isEmpty()) {
            Label emptyLabel = new Label("Tidak ada jadwal produksi aktif saat ini.");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-padding: 20; -fx-font-style: italic;");
            scheduleContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < scheduleList.size(); i++) {
            ScheduleItem item = scheduleList.get(i);
            HBox row = createScheduleRow(item);
            row.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0; -fx-padding: 15 0;");
            scheduleContainer.getChildren().add(row);
        }
    }

    private HBox createScheduleRow(ScheduleItem item) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        VBox leftContent = new VBox(5);
        Label titleLabel = new Label(item.getOrderId() + " - " + item.getServiceType());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");

        Label subtitleLabel = new Label(item.getCustomerName() + " • " + item.getQuantity() + " pcs");
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        leftContent.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(item.getStatus());
        String statusStyle = "-fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px; ";

        if (item.getStatus().equals("Sedang Diproduksi")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #222; -fx-text-fill: white;");
        } else if (item.getStatus().equals("Terkendala")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #ffebee; -fx-text-fill: #c62828;");
        } else {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #f0f0f0; -fx-text-fill: #666;");
        }

        row.getChildren().addAll(leftContent, spacer, statusLabel);
        return row;
    }

    /**
     * Model untuk item jadwal produksi.
     */
    public static class ScheduleItem {
        private String orderId, serviceType, customerName, status;
        private int quantity;

        public ScheduleItem(String orderId, String serviceType, String customerName, int quantity, String status) {
            this.orderId = orderId;
            this.serviceType = serviceType;
            this.customerName = customerName;
            this.quantity = quantity;
            this.status = status;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getServiceType() {
            return serviceType;
        }

        public String getCustomerName() {
            return customerName;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getStatus() {
            return status;
        }
    }
}