package com.example.trying3.controller.management;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller untuk halaman Laporan.
 * Menampilkan ringkasan bisnis dan menyediakan fitur ekspor ke Excel (CSV) dan PDF.
 */
public class LaporanController {

    @FXML private ComboBox<String> comboPeriode;
    @FXML private ComboBox<String> comboJenis;
    @FXML private Button btnExcel;
    @FXML private Button btnPDF;
    @FXML private Label txtTotalPesanan;
    @FXML private Label txtPesananSelesai;
    @FXML private Label txtSelesaiPersen;
    @FXML private Label txtPendapatan;
    @FXML private Label txtTertunda;
    @FXML private VBox layananContainer;
    @FXML private VBox activityContainer;

    private PesananDAO pesananDAO;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        pesananDAO = new PesananDAO();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        comboPeriode.setItems(FXCollections.observableArrayList("Harian", "Mingguan", "Bulanan", "Tahunan"));
        comboPeriode.getSelectionModel().select("Bulanan");

        comboJenis.setItems(FXCollections.observableArrayList("Semua Laporan"));
        comboJenis.getSelectionModel().selectFirst();

        comboPeriode.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        btnExcel.setOnAction(e -> exportToExcel());
        btnPDF.setOnAction(e -> exportToPDF());

        refreshData();
    }

    private void refreshData() {
        String periode = comboPeriode.getValue();
        loadSummaryData(periode);
        loadLayananData(periode);
        loadActivityData();
    }

    private void loadSummaryData(String periode) {
        double[] stats = pesananDAO.getLaporanSummary(periode);
        int total = (int) stats[0];
        int selesai = (int) stats[1];
        int tertunda = (int) stats[2];
        double pendapatan = stats[3];

        txtTotalPesanan.setText(String.valueOf(total));
        txtPesananSelesai.setText(String.valueOf(selesai));
        txtTertunda.setText(String.valueOf(tertunda));
        txtPendapatan.setText(currencyFormat.format(pendapatan));

        if (total > 0) {
            int persen = (selesai * 100) / total;
            txtSelesaiPersen.setText("Tingkat penyelesaian " + persen + "%");
        } else {
            txtSelesaiPersen.setText("Belum ada data");
        }
    }

    private void loadLayananData(String periode) {
        layananContainer.getChildren().clear();
        List<String[]> dataLayanan = pesananDAO.getAnalisaLayanan(periode);

        if (dataLayanan.isEmpty()) {
            Label empty = new Label("Tidak ada data transaksi.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            layananContainer.getChildren().add(empty);
            return;
        }

        for (String[] row : dataLayanan) {
            String nama = (row[0] == null) ? "Lainnya" : row[0];
            String jumlah = row[1];
            double uang = Double.parseDouble(row[2]);

            layananContainer.getChildren().add(makeLayananRow(nama, jumlah + " pesanan", currencyFormat.format(uang)));
        }
    }

    private void loadActivityData() {
        activityContainer.getChildren().clear();
        List<Pesanan> recentOrders = pesananDAO.getAktivitasTerbaru();

        for (Pesanan p : recentOrders) {
            String title = "PO-" + p.getIdPesanan() + " - " + p.getNamaPelanggan();
            String tgl = p.getTanggalPesanan() != null ? p.getTanggalPesanan().toLocalDate().toString() : "-";
            String subtitle = p.getJenisLayanan() + " • " + tgl;
            String price = currencyFormat.format(p.getTotalBiaya());

            activityContainer.getChildren().add(makeActivityRow(title, subtitle, price, p.getStatus()));
        }
    }

    private void exportToExcel() {
        String periode = comboPeriode.getValue();
        List<Pesanan> exportList = pesananDAO.getPesananForExport(periode);

        if (exportList.isEmpty()) {
            showAlert("Info", "Tidak ada data untuk diekspor pada periode ini.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Laporan Excel (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("Laporan_" + periode + "_" + System.currentTimeMillis() + ".csv");

        File file = fileChooser.showSaveDialog(btnExcel.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID Pesanan,Tanggal,Pelanggan,No Telepon,Layanan,Status,Total Harga\n");

                for (Pesanan p : exportList) {
                    String tgl = p.getTanggalPesanan() != null ? p.getTanggalPesanan().format(DateTimeFormatter.ISO_LOCAL_DATE) : "-";
                    String nama = p.getNamaPelanggan().replace(",", " ");
                    String layanan = p.getJenisLayanan().replace(",", " ");

                    writer.write(String.format("%d,%s,%s,%s,%s,%s,%.0f\n",
                            p.getIdPesanan(), tgl, nama, p.getNoTelepon(), layanan, p.getStatus(), p.getTotalBiaya()));
                }

                showAlert("Sukses", "Laporan berhasil diekspor ke Excel (CSV)!");

            } catch (IOException ex) {
                showAlert("Error", "Gagal menyimpan file: " + ex.getMessage());
            }
        }
    }

    private void exportToPDF() {
        String periode = comboPeriode.getValue();
        List<Pesanan> exportList = pesananDAO.getPesananForExport(periode);

        if (exportList.isEmpty()) {
            showAlert("Info", "Tidak ada data untuk dicetak.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean proceed = job.showPrintDialog(btnPDF.getScene().getWindow());
            if (proceed) {
                VBox reportPage = createPDFLayout(periode, exportList);

                boolean success = job.printPage(reportPage);
                if (success) {
                    job.endJob();
                    showAlert("Sukses", "Laporan berhasil dikirim ke PDF/Printer.");
                } else {
                    showAlert("Error", "Gagal mencetak laporan.");
                }
            }
        } else {
            showAlert("Error", "Tidak ada printer/driver PDF terdeteksi.");
        }
    }

    /**
     * Membuat layout halaman laporan untuk dicetak.
     */
    private VBox createPDFLayout(String periode, List<Pesanan> data) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(40));
        layout.setPrefWidth(595);
        layout.setStyle("-fx-background-color: white;");

        Label title = new Label("LAPORAN KINERJA PERCETAKAN");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        Label subtitle = new Label("Periode: " + periode + " | Dicetak: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        GridPane table = new GridPane();
        table.setHgap(10);
        table.setVgap(5);
        table.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 10;");

        addCell(table, 0, 0, "ID", true);
        addCell(table, 1, 0, "TANGGAL", true);
        addCell(table, 2, 0, "PELANGGAN", true);
        addCell(table, 3, 0, "LAYANAN", true);
        addCell(table, 4, 0, "STATUS", true);
        addCell(table, 5, 0, "TOTAL", true);

        int row = 1;
        double totalOmset = 0;
        int limit = Math.min(data.size(), 20);

        for (int i = 0; i < limit; i++) {
            Pesanan p = data.get(i);
            String tgl = p.getTanggalPesanan() != null ? p.getTanggalPesanan().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "-";

            addCell(table, 0, row, String.valueOf(p.getIdPesanan()), false);
            addCell(table, 1, row, tgl, false);
            addCell(table, 2, row, p.getNamaPelanggan(), false);
            addCell(table, 3, row, p.getJenisLayanan(), false);
            addCell(table, 4, row, p.getStatus(), false);
            addCell(table, 5, row, currencyFormat.format(p.getTotalBiaya()), false);

            totalOmset += p.getTotalBiaya();
            row++;
        }

        Label footer = new Label("Total Omset Halaman Ini: " + currencyFormat.format(totalOmset));
        footer.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        if (data.size() > 20) {
            Label note = new Label("Catatan: Menampilkan 20 transaksi terbaru. Gunakan Excel untuk data lengkap.");
            note.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");
            layout.getChildren().addAll(title, subtitle, new Separator(), table, footer, note);
        } else {
            layout.getChildren().addAll(title, subtitle, new Separator(), table, footer);
        }

        return layout;
    }

    private void addCell(GridPane grid, int col, int row, String text, boolean isHeader) {
        Label label = new Label(text);
        if (isHeader) {
            label.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            label.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0;");
        } else {
            label.setFont(Font.font("Arial", 10));
        }
        grid.add(label, col, row);
    }

    private Node makeLayananRow(String name, String count, String revenue) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setStyle("-fx-padding: 8 0; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

        VBox info = new VBox(2);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        Label lblCount = new Label(count);
        lblCount.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        info.getChildren().addAll(lblName, lblCount);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblRevenue = new Label(revenue);
        lblRevenue.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        row.getChildren().addAll(info, spacer, lblRevenue);
        return row;
    }

    private Node makeActivityRow(String title, String subtitle, String price, String status) {
        VBox box = new VBox(6);
        box.setStyle("-fx-padding: 14 0; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

        HBox top = new HBox();
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblPrice = new Label(price);
        lblPrice.setStyle("-fx-font-weight: bold;");
        top.getChildren().addAll(lblTitle, spacer, lblPrice);

        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label lblStatus = new Label(status);
        lblStatus.setStyle(getStatusStyle(status));
        bottom.getChildren().addAll(lblSub, spacer2, lblStatus);

        box.getChildren().addAll(top, bottom);
        return box;
    }

    private String getStatusStyle(String status) {
        String base = "-fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold; ";
        if (status.equalsIgnoreCase("Selesai")) return base + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
        else if (status.equalsIgnoreCase("Dibatalkan")) return base + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
        else return base + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}