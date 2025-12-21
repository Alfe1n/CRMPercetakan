package com.example.trying3.controller.admin;

import com.example.trying3.dao.NotifikasiDAO;
import com.example.trying3.model.Notifikasi;
import com.example.trying3.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotifikasiController {

    // Statistics Labels
    @FXML private Label totalNotifikasiLabel;
    @FXML private Label revisiDesainLabel;
    @FXML private Label kendalaProduksiLabel;
    @FXML private Label siapDikirimLabel;

    // Filter and Info
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button refreshButton;
    @FXML private Label notifikasiCountLabel;
    @FXML private Label notifikasiSubtitle;

    // List View
    @FXML private ListView<Notifikasi> notifikasiListView;

    // Empty State
    @FXML private VBox emptyStateBox;

    // Data
    private NotifikasiDAO notifikasiDAO;
    private ObservableList<Notifikasi> notifikasiList = FXCollections.observableArrayList();
    private List<Notifikasi> allNotifikasiList = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("✅ NotifikasiController initialized");

        notifikasiDAO = new NotifikasiDAO();

        setupFilterComboBox();
        setupListView();
        setupButtons();

        // Load data setelah komponen siap
        Platform.runLater(this::loadData);
    }

    // Setup components
    private void setupFilterComboBox() {
        ObservableList<String> filterOptions = FXCollections.observableArrayList(
                "Semua Notifikasi",
                "Revisi Desain",
                "Kendala Produksi",
                "Pesanan Siap Dikirim"
        );
        filterComboBox.setItems(filterOptions);
        filterComboBox.setValue("Semua Notifikasi");

        filterComboBox.setOnAction(e -> applyFilter());
    }

    private void setupListView() {
        notifikasiListView.setCellFactory(param -> new NotifikasiCell());
        notifikasiListView.setItems(notifikasiList);

        // Handle click on notification
        notifikasiListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Notifikasi selected = notifikasiListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleNotifikasiClick(selected);
                }
            }
        });
    }

    private void setupButtons() {
        refreshButton.setOnAction(e -> loadData());
    }

    // Data loading
    private void loadData() {
        System.out.println("📥 Loading data...");
        loadStatistics();
        loadNotifikasi();
    }

    private void loadStatistics() {
        try {
            // Total
            int total = notifikasiDAO.getTotalNotifikasiCount();
            totalNotifikasiLabel.setText(String.valueOf(total));
            System.out.println("📊 Total notifikasi count: " + total);

            // Per tipe
            int revisi = notifikasiDAO.getRevisiDesainCount();
            int kendala = notifikasiDAO.getKendalaProduksiCount();
            int siap = notifikasiDAO.getSiapDikirimCount();

            revisiDesainLabel.setText(String.valueOf(revisi));
            kendalaProduksiLabel.setText(String.valueOf(kendala));
            siapDikirimLabel.setText(String.valueOf(siap));

            System.out.println("📊 Revisi: " + revisi + ", Kendala: " + kendala + ", Siap: " + siap);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat statistik notifikasi.");
        }
    }

    private void loadNotifikasi() {
        try {
            System.out.println("📥 Fetching all notifikasi from DAO...");

            List<Notifikasi> data = notifikasiDAO.getAllNotifikasi();

            System.out.println("📥 Data received: " + (data != null ? data.size() : "null") + " items");

            // Debug: print each item
            if (data != null) {
                for (Notifikasi n : data) {
                    System.out.println("   - " + n.getTipe() + ": " + n.getJudul() + " (" + n.getNomorPesanan() + ")");
                }
            }

            allNotifikasiList.clear();
            if (data != null) {
                allNotifikasiList.addAll(data);
            }

            System.out.println("📥 allNotifikasiList size: " + allNotifikasiList.size());

            applyFilter();

        } catch (Exception e) {
            System.err.println("❌ Error loading notifikasi:");
            e.printStackTrace();
            AlertUtil.showError("Gagal memuat data notifikasi: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String selectedFilter = filterComboBox.getValue();
        System.out.println("🔍 Applying filter: " + selectedFilter);
        System.out.println("🔍 allNotifikasiList size before filter: " + allNotifikasiList.size());

        List<Notifikasi> filtered;

        if (selectedFilter == null || "Semua Notifikasi".equals(selectedFilter)) {
            filtered = new ArrayList<>(allNotifikasiList);
        } else {
            String tipe = switch (selectedFilter) {
                case "Revisi Desain" -> Notifikasi.TIPE_REVISI_DESAIN;
                case "Kendala Produksi" -> Notifikasi.TIPE_KENDALA_PRODUKSI;
                case "Pesanan Siap Dikirim" -> Notifikasi.TIPE_SIAP_DIKIRIM;
                default -> null;
            };

            System.out.println("🔍 Filtering by tipe: " + tipe);

            if (tipe != null) {
                final String finalTipe = tipe;
                filtered = allNotifikasiList.stream()
                        .filter(n -> {
                            boolean match = finalTipe.equals(n.getTipe());
                            System.out.println("   Checking: " + n.getTipe() + " == " + finalTipe + " ? " + match);
                            return match;
                        })
                        .collect(Collectors.toList());
            } else {
                filtered = new ArrayList<>(allNotifikasiList);
            }
        }

        System.out.println("🔍 Filtered result: " + filtered.size() + " items");

        notifikasiList.clear();
        notifikasiList.addAll(filtered);

        System.out.println("🔍 notifikasiList size after filter: " + notifikasiList.size());

        updateListInfo();
    }

    private void updateListInfo() {
        int count = notifikasiList.size();
        notifikasiCountLabel.setText("(" + count + " notifikasi)");
        System.out.println("📋 Updating list info, count: " + count);

        if (count == 0) {
            notifikasiSubtitle.setText("Tidak ada notifikasi baru");
            showEmptyState(true);
        } else {
            notifikasiSubtitle.setText(count + " notifikasi memerlukan perhatian");
            showEmptyState(false);
        }
    }

    private void showEmptyState(boolean show) {
        System.out.println("👁 showEmptyState: " + show);
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(show);
            emptyStateBox.setManaged(show);
        }
        notifikasiListView.setVisible(!show);
        notifikasiListView.setManaged(!show);
    }

    // Event handler untuk klik notifikasi
    private void handleNotifikasiClick(Notifikasi notifikasi) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Notifikasi");
        alert.setHeaderText(notifikasi.getJudul());

        StringBuilder content = new StringBuilder();
        content.append("Pesanan: ").append(notifikasi.getNomorPesanan()).append("\n");
        content.append("Pelanggan: ").append(notifikasi.getNamaPelanggan()).append("\n");
        content.append("Sumber: ").append(notifikasi.getSumberDivisi()).append("\n");
        content.append("Waktu: ").append(notifikasi.getFormattedDateTime()).append("\n\n");
        content.append("Detail:\n").append(notifikasi.getPesan());

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    @FXML
    private void handleFilterRevisiDesain() {
        filterComboBox.setValue("Revisi Desain");
    }

    @FXML
    private void handleFilterKendalaProduksi() {
        filterComboBox.setValue("Kendala Produksi");
    }

    @FXML
    private void handleFilterSiapDikirim() {
        filterComboBox.setValue("Pesanan Siap Dikirim");
    }

    @FXML
    private void handleFilterSemua() {
        filterComboBox.setValue("Semua Notifikasi");
    }
}