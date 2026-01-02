package com.example.trying3.controller.admin;

import com.example.trying3.dao.DashboardStatsDAO;
import com.example.trying3.model.Pesanan;
import com.example.trying3.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardPaneController implements Initializable {

    // FXML Components - Statistics Labels
    @FXML private Label totalPesananLabel;
    @FXML private Label totalPesananSubtitle;
    @FXML private Label pesananSelesaiLabel;
    @FXML private Label pesananSelesaiSubtitle;
    @FXML private Label pembayaranPendingLabel;
    @FXML private Label pembayaranPendingSubtitle;
    @FXML private Label totalPendapatanLabel;
    @FXML private Label totalPendapatanSubtitle;

    // FXML Components - List Views
    @FXML private ListView<Pesanan> pesananTerbaruListView;
    @FXML private ListView<User> aktivitasUserListView;

    // Data sources
    private DashboardStatsDAO dashboardDAO;
    private final ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardDAO = new DashboardStatsDAO();
        setupPesananListView();
        setupUserListView();
        loadAllData();
    }

    /**
     * Konfigurasi ListView untuk pesanan terbaru dengan custom cell
     */
    private void setupPesananListView() {
        if (pesananTerbaruListView != null) {
            pesananTerbaruListView.setItems(pesananList);
            pesananTerbaruListView.setCellFactory(lv -> new PesananTerbaruCell());
            pesananTerbaruListView.setFixedCellSize(70);
        }
    }

    /**
     * Konfigurasi ListView untuk aktivitas user dengan custom cell
     */
    private void setupUserListView() {
        if (aktivitasUserListView != null) {
            aktivitasUserListView.setItems(userList);
            aktivitasUserListView.setCellFactory(lv -> new UserAktivitasCell());
            aktivitasUserListView.setFixedCellSize(70);
        }
    }

    /**
     * Load semua data dashboard secara asynchronous
     */
    public void loadAllData() {
        loadStatistics();
        loadPesananTerbaru();
        loadAktivitasUser();
    }

    /**
     * Memuat statistik dashboard dari database
     * Data: Total Pesanan, Pesanan Selesai, Pembayaran Pending, Total Pendapatan
     */
    private void loadStatistics() {
        Task<Void> statsTask = new Task<>() {
            private int totalPesanan;
            private int pesananSelesai;
            private String persenSelesai;
            private int pembayaranPending;
            private String totalPendapatan;

            @Override
            protected Void call() {
                totalPesanan = dashboardDAO.getTotalPesanan();
                pesananSelesai = dashboardDAO.getPesananSelesai();
                persenSelesai = dashboardDAO.getPersentasePesananSelesai();
                pembayaranPending = dashboardDAO.getPembayaranPending();
                totalPendapatan = dashboardDAO.getFormattedTotalPendapatan();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (totalPesananLabel != null) {
                        totalPesananLabel.setText(String.valueOf(totalPesanan));
                    }
                    if (totalPesananSubtitle != null) {
                        totalPesananSubtitle.setText("Semua pesanan");
                    }

                    if (pesananSelesaiLabel != null) {
                        pesananSelesaiLabel.setText(String.valueOf(pesananSelesai));
                    }
                    if (pesananSelesaiSubtitle != null) {
                        pesananSelesaiSubtitle.setText(persenSelesai);
                    }

                    if (pembayaranPendingLabel != null) {
                        pembayaranPendingLabel.setText(String.valueOf(pembayaranPending));
                    }
                    if (pembayaranPendingSubtitle != null) {
                        pembayaranPendingSubtitle.setText("Perlu verifikasi");
                    }

                    if (totalPendapatanLabel != null) {
                        totalPendapatanLabel.setText(totalPendapatan);
                    }
                    if (totalPendapatanSubtitle != null) {
                        totalPendapatanSubtitle.setText("Pembayaran terverifikasi");
                    }
                });
            }

            @Override
            protected void failed() {
                getException().printStackTrace();
                Platform.runLater(() -> {
                    if (totalPesananLabel != null) totalPesananLabel.setText("0");
                    if (pesananSelesaiLabel != null) pesananSelesaiLabel.setText("0");
                    if (pembayaranPendingLabel != null) pembayaranPendingLabel.setText("0");
                    if (totalPendapatanLabel != null) totalPendapatanLabel.setText("Rp 0");
                });
            }
        };

        new Thread(statsTask).start();
    }

    /**
     * Memuat 5 pesanan terbaru dari database
     */
    private void loadPesananTerbaru() {
        Task<List<Pesanan>> task = new Task<>() {
            @Override
            protected List<Pesanan> call() {
                return dashboardDAO.getPesananTerbaru(5);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            pesananList.clear();
            pesananList.addAll(task.getValue());
        }));

        task.setOnFailed(e -> task.getException().printStackTrace());

        new Thread(task).start();
    }

    /**
     * Memuat daftar user aktif dari database
     */
    private void loadAktivitasUser() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return dashboardDAO.getActiveUsers();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            userList.clear();
            userList.addAll(task.getValue());
        }));

        task.setOnFailed(e -> task.getException().printStackTrace());

        new Thread(task).start();
    }

    /**
     * Refresh semua data dashboard
     * Dipanggil dari DashboardAdminController saat navigasi ke halaman ini
     */
    public void refresh() {
        loadAllData();
    }
}