package com.example.trying3.controller.design;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RiwayatDesainPaneController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox riwayatContainer;
    @FXML private Label emptyLabel;

    // Data Sumber
    private final ObservableList<RiwayatDesain> masterData = FXCollections.observableArrayList(
            new RiwayatDesain("DES-098", "Revisi Logo Kopi", "Budi", "Selesai", "2024-12-05", "Rp 500.000"),
            new RiwayatDesain("DES-097", "Kartu Nama Direksi", "Andi", "Disetujui", "2024-12-03", "Rp 350.000"),
            new RiwayatDesain("DES-096", "Flyer Promo Akhir Tahun", "Budi", "Ditolak", "2024-12-01", "Rp 200.000"),
            new RiwayatDesain("DES-095", "Desain Kemasan Baru", "Santi", "Selesai", "2024-11-28", "Rp 800.000"),
            new RiwayatDesain("DES-094", "Brosur Digital", "Rani", "Selesai", "2024-11-25", "Rp 400.000"),
            new RiwayatDesain("DES-093", "Desain Undangan", "Andi", "Ditolak", "2024-11-20", "Rp 150.000")
    );


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Panggil fungsi untuk mengisi container
        renderRiwayat(masterData);

        // Setup Listeners
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldStatus, newStatus) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getSelectionModel().getSelectedItem();

        // Filter data berdasarkan kriteria
        ObservableList<RiwayatDesain> filteredList = masterData.stream()
                .filter(riwayat -> {
                    boolean matchesSearch = (riwayat.getIdPesanan().toLowerCase().contains(searchText) ||
                            riwayat.getNamaProject().toLowerCase().contains(searchText) ||
                            riwayat.getDesigner().toLowerCase().contains(searchText));

                    boolean matchesStatus = true;
                    if (selectedStatus != null && !selectedStatus.equals("Semua")) {
                        matchesStatus = riwayat.getStatus().equals(selectedStatus);
                    }

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        renderRiwayat(filteredList);
    }

    private void renderRiwayat(ObservableList<RiwayatDesain> list) {
        riwayatContainer.getChildren().clear();

        if (list.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (RiwayatDesain riwayat : list) {
            riwayatContainer.getChildren().add(createRiwayatCard(riwayat));
        }
    }

    // Fungsi yang membuat satu kartu riwayat (sesuai gambar)
    private HBox createRiwayatCard(RiwayatDesain riwayat) {
        HBox card = new HBox(20);
        card.setStyle("-fx-background-color: white; -fx-padding: 15 20; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");

        // 1. Proyek dan ID (VBox kiri)
        VBox projectBox = new VBox(5);
        Label idLabel = new Label(riwayat.getIdPesanan());
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        Label projectLabel = new Label(riwayat.getNamaProject());
        projectLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

        projectBox.getChildren().addAll(idLabel, projectLabel);
        projectBox.setPrefWidth(250); // Lebar tetap untuk kolom kiri

        // 2. Desainer (VBox tengah kiri)
        VBox designerBox = new VBox(5);
        Label designerTitle = new Label("Desainer");
        designerTitle.getStyleClass().add("text-muted");
        Label designerName = new Label(riwayat.getDesigner());
        designerName.setStyle("-fx-font-weight: bold;");
        designerBox.getChildren().addAll(designerTitle, designerName);
        designerBox.setPrefWidth(120);

        // 3. Tanggal Selesai (VBox tengah kanan)
        VBox dateBox = new VBox(5);
        Label dateTitle = new Label("Selesai Pada");
        dateTitle.getStyleClass().add("text-muted");
        Label dateValue = new Label(riwayat.getTanggalSelesai());
        dateValue.setStyle("-fx-font-weight: bold;");
        dateBox.getChildren().addAll(dateTitle, dateValue);
        dateBox.setPrefWidth(120);

        // 4. Status (VBox status)
        VBox statusBox = new VBox(5);
        Label statusTitle = new Label("Status");
        statusTitle.getStyleClass().add("text-muted");

        Label statusBadge = new Label(riwayat.getStatus());
        statusBadge.getStyleClass().add(getStatusStyleClass(riwayat.getStatus()));
        statusBadge.setStyle("-fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");

        statusBox.getChildren().addAll(statusTitle, statusBadge);
        statusBox.setPrefWidth(100);

        // 5. Total dan Aksi (VBox kanan)
        VBox actionBox = new VBox(5);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label totalLabel = new Label(riwayat.getTotal());
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2ecc71;");

        Button detailButton = new Button("Lihat Detail");
        detailButton.getStyleClass().add("pesanan-action-button"); // Dari designer.css
        detailButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #555;");
        detailButton.setOnAction(e -> {
            System.out.println("Melihat detail untuk " + riwayat.getIdPesanan());
            // Tambahkan kode untuk membuka popup detail di sini
        });

        actionBox.getChildren().addAll(totalLabel, detailButton);


        // Menambahkan spacer (Region) untuk memastikan tata letak terpisah
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        card.getChildren().addAll(projectBox, designerBox, dateBox, spacer1, statusBox, spacer2, actionBox);
        HBox.setHgrow(projectBox, Priority.NEVER);
        HBox.setHgrow(designerBox, Priority.NEVER);
        HBox.setHgrow(dateBox, Priority.NEVER);
        HBox.setHgrow(actionBox, Priority.NEVER);

        return card;
    }

    private String getStatusStyleClass(String status) {
        return switch (status) {
            case "Selesai", "Disetujui" -> "status-success";
            case "Ditolak" -> "status-danger";
            default -> "status-info"; // Fallback
        };
    }

    // Inner Class untuk Model Data Riwayat (diperbarui dengan field Total)
    public static class RiwayatDesain {
        private final String idPesanan;
        private final String namaProject;
        private final String designer;
        private final String status;
        private final String tanggalSelesai;
        private final String total;

        public RiwayatDesain(String idPesanan, String namaProject, String designer, String status, String tanggalSelesai, String total) {
            this.idPesanan = idPesanan;
            this.namaProject = namaProject;
            this.designer = designer;
            this.status = status;
            this.tanggalSelesai = tanggalSelesai;
            this.total = total;
        }

        // Getter wajib
        public String getIdPesanan() { return idPesanan; }
        public String getNamaProject() { return namaProject; }
        public String getDesigner() { return designer; }
        public String getStatus() { return status; }
        public String getTanggalSelesai() { return tanggalSelesai; }
        public String getTotal() { return total; }
    }
}