package com.example.trying3.controller.management;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class LaporanController {

    @FXML private ComboBox<String> comboPeriode;
    @FXML private ComboBox<String> comboJenis;

    @FXML private Button btnExcel;
    @FXML private Button btnPDF;

    // Semua field ini sekarang cocok dengan <Label> di FXML
    @FXML private Label txtTotalPesanan;
    @FXML private Label txtPesananSelesai;
    @FXML private Label txtSelesaiPersen;
    @FXML private Label txtPendapatan;
    @FXML private Label txtTertunda;

    @FXML private VBox layananContainer;
    @FXML private VBox activityContainer;

    @FXML
    public void initialize() {
        comboPeriode.setItems(FXCollections.observableArrayList(
                "Harian", "Mingguan", "Bulanan", "Tahunan"
        ));
        comboPeriode.getSelectionModel().select("Mingguan");

        comboJenis.setItems(FXCollections.observableArrayList(
                "Ringkasan", "Pendapatan", "Pesanan"
        ));
        comboJenis.getSelectionModel().select("Ringkasan");
        // -------------------------------------------------------------

        loadSummaryDummy();
        loadLayananDummy();
        loadActivityDummy();
    }

    private void loadSummaryDummy() {
        txtTotalPesanan.setText("4");
        txtPesananSelesai.setText("1");
        txtSelesaiPersen.setText("Tingkat penyelesaian 25%");
        txtPendapatan.setText("Rp 3.750.000");
        txtTertunda.setText("1");
    }

    private void loadLayananDummy() {
        layananContainer.getChildren().clear();
        layananContainer.getChildren().add(makeRow("Pencetakan Digital", "Rp 750.000"));
        layananContainer.getChildren().add(makeRow("Cetak Offset", "Rp 2.500.000"));
        layananContainer.getChildren().add(makeRow("Sablon", "Rp 500.000"));
        layananContainer.getChildren().add(makeRow("Cetak Undangan", "Rp 0"));
    }

    private Node makeRow(String service, String revenue) {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setStyle("-fx-padding: 10 0; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
        Label name = new Label(service);
        Label amount = new Label(revenue);
        amount.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        row.getChildren().addAll(name, new Pane(), amount);
        HBox.setHgrow(row.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        return row;
    }

    private void loadActivityDummy() {
        activityContainer.getChildren().clear();
        activityContainer.getChildren().add(makeActivity("ORD-001 - PT. Maju Jaya", "Pencetakan Digital - 2024-01-15", "Rp 750.000", "Proses Desain"));
        activityContainer.getChildren().add(makeActivity("ORD-002 - Toko Berkah", "Sablon - 2024-01-14", "Rp 500.000", "Proses Produksi"));
        activityContainer.getChildren().add(makeActivity("ORD-003 - CV. Sukses Mandiri", "Cetak Undangan - 2024-01-16", "Rp 1.200.000", "Menunggu Konfirmasi"));
        activityContainer.getChildren().add(makeActivity("ORD-004 - Sekolah Harapan", "Cetak Offset - 2024-01-10", "Rp 2.500.000", "Selesai"));
    }

    private VBox makeActivity(String title, String subtitle, String price, String status) {
        VBox box = new VBox();
        box.setSpacing(5);
        box.setStyle("-fx-padding: 15 0; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        Label l1 = new Label(title);
        l1.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label l2 = new Label(subtitle);
        l2.setStyle("-fx-text-fill: #777;");

        Label l3 = new Label(price + "  |  " + status);
        l3.setStyle("-fx-font-weight: bold; -fx-text-fill: #444;");

        box.getChildren().addAll(l1, l2, l3);
        return box;
    }
}