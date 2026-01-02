package com.example.trying3.controller.design;

import com.example.trying3.dao.PesananDAO;
import com.example.trying3.model.Pesanan;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller untuk pane utama Dashboard Design.
 * Menampilkan statistik dan antrian desain yang perlu dikerjakan.
 */
public class DashboardDesignPaneController implements Initializable {

    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox queueContainer;

    private PesananDAO pesananDAO;
    private List<Pesanan> orders = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pesananDAO = new PesananDAO();
        loadData();
    }

    private void loadData() {
        orders = pesananDAO.getPesananForDesignTeam();
        updateStatistics();
        renderQueue();
    }

    private void updateStatistics() {
        long waiting = orders.stream()
                .filter(o -> "Menunggu Desain".equals(o.getStatus()))
                .count();
        long inProgress = orders.stream()
                .filter(o -> "Desain Direvisi".equals(o.getStatus()))
                .count();
        long completed = orders.stream()
                .filter(o -> "Desain Disetujui".equals(o.getStatus()))
                .count();

        if (waitingLabel != null) waitingLabel.setText(String.valueOf(waiting));
        if (inProgressLabel != null) inProgressLabel.setText(String.valueOf(inProgress));
        if (completedLabel != null) completedLabel.setText(String.valueOf(completed));
    }

    /**
     * Render daftar antrian desain yang masih aktif.
     * Mengurutkan berdasarkan prioritas (Desain Direvisi > Menunggu Desain).
     */
    private void renderQueue() {
        if (queueContainer == null) return;

        queueContainer.getChildren().clear();

        List<Pesanan> activeOrders = orders.stream()
                .filter(o -> !"Desain Disetujui".equals(o.getStatus()))
                .sorted((a, b) -> {
                    int priorityA = getPriority(a.getStatus());
                    int priorityB = getPriority(b.getStatus());
                    return Integer.compare(priorityA, priorityB);
                })
                .toList();

        if (activeOrders.isEmpty()) {
            Label emptyLabel = new Label("Tidak ada antrian desain aktif. 🎉");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            queueContainer.getChildren().add(emptyLabel);
        } else {
            for (Pesanan order : activeOrders) {
                queueContainer.getChildren().add(createOrderCard(order));
            }
        }
    }

    private int getPriority(String status) {
        return switch (status) {
            case "Desain Direvisi" -> 1;
            case "Menunggu Desain" -> 2;
            default -> 3;
        };
    }
    private VBox createOrderCard(Pesanan order) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-border-color: #eeeeee; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20;");
        card.setSpacing(10);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label(order.getDisplayId());
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");

        Label lblClient = new Label(order.getNamaPelanggan());
        lblClient.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        titleBox.getChildren().addAll(lblId, lblClient);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.getStatus());
        lblStatus.setStyle(getStatusStyle(order.getStatus()));

        header.getChildren().addAll(titleBox, spacer, lblStatus);

        VBox detailsBox = new VBox(5);
        detailsBox.getChildren().add(createDetailLabel("Project: ", order.getJenisLayanan()));
        detailsBox.getChildren().add(createDetailLabel("Brief: ", order.getSpesifikasi()));

        card.getChildren().addAll(header, detailsBox);
        return card;
    }

    private String getStatusStyle(String status) {
        String baseStyle = "-fx-background-radius: 15; -fx-padding: 5 12; -fx-font-weight: bold; -fx-font-size: 11px;";

        return switch (status) {
            case "Menunggu Desain" -> baseStyle + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;";
            case "Desain Direvisi" -> baseStyle + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
            case "Desain Disetujui" -> baseStyle + "-fx-background-color: #d4edda; -fx-text-fill: #155724;";
            default -> baseStyle + "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41;";
        };
    }


    private HBox createDetailLabel(String label, String value) {
        HBox row = new HBox(5);
        Label lblTitle = new Label(label);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");

        Label lblValue = new Label(value != null ? value : "-");
        lblValue.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
        lblValue.setWrapText(true);

        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    public void refresh() {
        Platform.runLater(this::loadData);
    }
}