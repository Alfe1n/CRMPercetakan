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

    // FXML BINDINGS - STATISTIK
    @FXML private Label totalPesananLabel;
    @FXML private Label totalPesananSubtitle;

    @FXML private Label pesananSelesaiLabel;
    @FXML private Label pesananSelesaiSubtitle;

    @FXML private Label pembayaranPendingLabel;
    @FXML private Label pembayaranPendingSubtitle;

    @FXML private Label totalPendapatanLabel;
    @FXML private Label totalPendapatanSubtitle;

    // FXML BINDINGS - LIST VIEWS
    @FXML private ListView<Pesanan> pesananTerbaruListView;
    @FXML private ListView<User> aktivitasUserListView;

    // DATA
    private DashboardStatsDAO dashboardDAO;
    private final ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    // INITIALIZATION
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardDAO = new DashboardStatsDAO();

        // Setup ListViews
        setupPesananListView();
        setupUserListView();

        // Load semua data
        loadAllData();
    }

    /**
     * Setup ListView untuk pesanan terbaru dengan custom cell
     */
    private void setupPesananListView() {
        if (pesananTerbaruListView != null) {
            pesananTerbaruListView.setItems(pesananList);
            pesananTerbaruListView.setCellFactory(lv -> new PesananTerbaruCell());
            pesananTerbaruListView.setFixedCellSize(70); // Fixed height untuk konsistensi
        }
    }

    /**
     * Setup ListView untuk aktivitas user dengan custom cell
     */
    private void setupUserListView() {
        if (aktivitasUserListView != null) {
            aktivitasUserListView.setItems(userList);
            aktivitasUserListView.setCellFactory(lv -> new UserAktivitasCell());
            aktivitasUserListView.setFixedCellSize(70);
        }
    }

    // DATA LOADING
    /**
     * Load semua data dashboard secara asynchronous
     */
    public void loadAllData() {
        loadStatistics();
        loadPesananTerbaru();
        loadAktivitasUser();
    }

    /**
     * Load statistik dashboard (Total Pesanan, Selesai, Pending, Pendapatan)
     */
    private void loadStatistics() {
        Task<Void> statsTask = new Task<>() {
            private int totalPesanan;
            private int pesananSelesai;
            private String persenSelesai;
            private int pembayaranPending;
            private String totalPendapatan;

            @Override
            protected Void call() throws Exception {
                // Ambil semua statistik dari database
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
                    // Update Total Pesanan
                    if (totalPesananLabel != null) {
                        totalPesananLabel.setText(String.valueOf(totalPesanan));
                    }
                    if (totalPesananSubtitle != null) {
                        totalPesananSubtitle.setText("Semua pesanan");
                    }

                    // Update Pesanan Selesai
                    if (pesananSelesaiLabel != null) {
                        pesananSelesaiLabel.setText(String.valueOf(pesananSelesai));
                    }
                    if (pesananSelesaiSubtitle != null) {
                        pesananSelesaiSubtitle.setText(persenSelesai);
                    }

                    // Update Pembayaran Pending
                    if (pembayaranPendingLabel != null) {
                        pembayaranPendingLabel.setText(String.valueOf(pembayaranPending));
                    }
                    if (pembayaranPendingSubtitle != null) {
                        pembayaranPendingSubtitle.setText("Perlu verifikasi");
                    }

                    // Update Total Pendapatan
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
                    // Set default values jika gagal
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
     * Load pesanan terbaru (limit 5)
     */
    private void loadPesananTerbaru() {
        Task<List<Pesanan>> task = new Task<>() {
            @Override
            protected List<Pesanan> call() throws Exception {
                return dashboardDAO.getPesananTerbaru(5);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                pesananList.clear();
                pesananList.addAll(task.getValue());
            });
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    /**
     * Load aktivitas user (semua user aktif)
     */
    private void loadAktivitasUser() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return dashboardDAO.getActiveUsers();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                userList.clear();
                userList.addAll(task.getValue());
            });
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    // PUBLIC METHODS (untuk dipanggil dari DashboardAdminController)
    /**
     * Refresh semua data dashboard
     * Bisa dipanggil ketika ada perubahan data
     */
    public void refresh() {
        loadAllData();
    }
}