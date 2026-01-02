package com.example.trying3.controller.admin;

import com.example.trying3.dao.NotifikasiDAO;
import com.example.trying3.model.Notifikasi;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller untuk halaman Notifikasi Admin
 * Menampilkan notifikasi dari Tim Desain dan Tim Produksi
 */
public class NotifikasiController implements Initializable {

    @FXML private Label totalNotifikasiLabel;
    @FXML private Label revisiDesainLabel;
    @FXML private Label kendalaProduksiLabel;
    @FXML private Label siapDikirimLabel;

    @FXML private Label notifikasiCountLabel;
    @FXML private Label notifikasiSubtitle;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ListView<Notifikasi> notifikasiListView;
    @FXML private VBox emptyStateBox;

    private NotifikasiDAO notifikasiDAO;
    private ObservableList<Notifikasi> notifikasiList;
    private List<Notifikasi> allNotifikasiList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        notifikasiDAO = new NotifikasiDAO();
        notifikasiList = FXCollections.observableArrayList();
        allNotifikasiList = new ArrayList<>();
        setupListView();
        setupFilter();
        loadData();
    }

    /**
     * Konfigurasi ListView dengan custom cell factory
     */
    private void setupListView() {
        notifikasiListView.setCellFactory(listView -> {
            NotifikasiCell cell = new NotifikasiCell(notifikasi -> {
                loadData();
            });
            return cell;
        });

        notifikasiListView.setItems(notifikasiList);

        notifikasiListView.setOnMouseClicked(event -> {
            Notifikasi selected = notifikasiListView.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                handleNotifikasiClick(selected);
            }
        });
    }

    /**
     * Konfigurasi filter dropdown
     */
    private void setupFilter() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Semua Notifikasi",
                "Revisi Desain",
                "Kendala Produksi",
                "Pesanan Siap Dikirim"
        ));
        filterComboBox.setValue("Semua Notifikasi");
        filterComboBox.setOnAction(e -> applyFilter());
    }

    /**
     * Memuat data notifikasi dan statistik dari database
     */
    private void loadData() {
        System.out.println("🔄 Loading notifikasi data...");

        int revisiCount = notifikasiDAO.getRevisiDesainCount();
        int kendalaCount = notifikasiDAO.getKendalaProduksiCount();
        int siapDikirimCount = notifikasiDAO.getSiapDikirimCount();
        int totalCount = revisiCount + kendalaCount + siapDikirimCount;

        System.out.println("📊 Stats - Revisi: " + revisiCount + ", Kendala: " + kendalaCount + ", Siap Dikirim: " + siapDikirimCount);

        if (totalNotifikasiLabel != null) totalNotifikasiLabel.setText(String.valueOf(totalCount));
        if (revisiDesainLabel != null) revisiDesainLabel.setText(String.valueOf(revisiCount));
        if (kendalaProduksiLabel != null) kendalaProduksiLabel.setText(String.valueOf(kendalaCount));
        if (siapDikirimLabel != null) siapDikirimLabel.setText(String.valueOf(siapDikirimCount));

        allNotifikasiList = notifikasiDAO.getAllNotifikasi();
        System.out.println("📋 Loaded " + allNotifikasiList.size() + " notifikasi from DAO");

        applyFilter();
    }

    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refresh button clicked");
        loadData();
    }

    /**
     * Menerapkan filter pada daftar notifikasi
     */
    private void applyFilter() {
        String filter = filterComboBox.getValue();
        System.out.println("🔍 Applying filter: " + filter);

        List<Notifikasi> filtered;

        if (filter == null || "Semua Notifikasi".equals(filter)) {
            filtered = new ArrayList<>(allNotifikasiList);
        } else {
            String tipeFilter = switch (filter) {
                case "Revisi Desain" -> Notifikasi.TIPE_REVISI_DESAIN;
                case "Kendala Produksi" -> Notifikasi.TIPE_KENDALA_PRODUKSI;
                case "Pesanan Siap Dikirim" -> Notifikasi.TIPE_SIAP_DIKIRIM;
                default -> null;
            };

            if (tipeFilter != null) {
                final String finalTipeFilter = tipeFilter;
                filtered = allNotifikasiList.stream()
                        .filter(n -> {
                            boolean match = finalTipeFilter.equals(n.getTipe());
                            System.out.println("  - " + n.getJudul() + " (tipe: " + n.getTipe() + ") -> match: " + match);
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

    /**
     * Update informasi jumlah notifikasi
     */
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

    /**
     * Menampilkan atau menyembunyikan empty state
     */
    private void showEmptyState(boolean show) {
        System.out.println("👁 showEmptyState: " + show);
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(show);
            emptyStateBox.setManaged(show);
        }
        notifikasiListView.setVisible(!show);
        notifikasiListView.setManaged(!show);
    }

    /**
     * Handler untuk double-click notifikasi (menampilkan detail)
     */
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