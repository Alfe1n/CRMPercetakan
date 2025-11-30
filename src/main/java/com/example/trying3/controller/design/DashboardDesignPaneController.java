package com.example.trying3.controller.design;

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

public class DashboardDesignPaneController implements Initializable {

    @FXML private Label waitingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private VBox queueContainer;

    private List<DesignOrder> orders = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        updateStatistics();
        renderQueue();
    }

    private void loadDummyData() {
        // Data Dummy
        orders.add(new DesignOrder("DES-101", "Kopi Kenangan", "Rebranding Logo", "Menunggu Design", "Logo vector, style minimalis"));
        orders.add(new DesignOrder("DES-102", "Universitas X", "Banner Wisuda", "Sedang Didesain", "Ukuran 3x1m, foto terlampir"));
        orders.add(new DesignOrder("DES-103", "Warung Bu Siti", "Daftar Menu", "Selesai Design", "A4 Laminasi, file siap cetak"));
        orders.add(new DesignOrder("DES-104", "Event Organizer", "ID Card Panitia", "Menunggu Design", "Nama & Foto menyusul"));
    }

    private void updateStatistics() {
        long waiting = orders.stream().filter(o -> o.status.equals("Menunggu Design")).count();
        long inProgress = orders.stream().filter(o -> o.status.equals("Sedang Didesain")).count();
        long completed = orders.stream().filter(o -> o.status.equals("Selesai Design")).count();

        if(waitingLabel != null) waitingLabel.setText(String.valueOf(waiting));
        if(inProgressLabel != null) inProgressLabel.setText(String.valueOf(inProgress));
        if(completedLabel != null) completedLabel.setText(String.valueOf(completed));
    }

    private void renderQueue() {
        if(queueContainer == null) return;

        queueContainer.getChildren().clear();

        for (DesignOrder order : orders) {
            if (!order.status.equals("Selesai Design")) {
                queueContainer.getChildren().add(createOrderCard(order));
            }
        }

        if (queueContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("Tidak ada antrian desain aktif.");
            emptyLabel.getStyleClass().add("muted");
            queueContainer.getChildren().add(emptyLabel);
        }
    }

    private VBox createOrderCard(DesignOrder order) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-border-color: #eeeeee; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20;");
        card.setSpacing(10);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblId = new Label(order.id);
        lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");
        Label lblClient = new Label(order.client);
        lblClient.getStyleClass().add("muted");
        titleBox.getChildren().addAll(lblId, lblClient);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(order.status);
        lblStatus.getStyleClass().add("status-pill");

        if (order.status.equals("Menunggu Design")) {
            lblStatus.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-background-radius: 15; -fx-padding: 5 12; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else if (order.status.equals("Sedang Didesain")) {
            lblStatus.setStyle("-fx-background-color: #cce5ff; -fx-text-fill: #004085; -fx-background-radius: 15; -fx-padding: 5 12; -fx-font-weight: bold; -fx-font-size: 11px;");
        }

        header.getChildren().addAll(titleBox, spacer, lblStatus);

        VBox detailsBox = new VBox(5);
        detailsBox.getChildren().add(createDetailLabel("Project: ", order.project));
        detailsBox.getChildren().add(createDetailLabel("Brief: ", order.brief));

        card.getChildren().addAll(header, detailsBox);
        return card;
    }

    private HBox createDetailLabel(String label, String value) {
        HBox row = new HBox(5);
        Label lblTitle = new Label(label);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 12px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
        row.getChildren().addAll(lblTitle, lblValue);
        return row;
    }

    public static class DesignOrder {
        String id, client, project, status, brief;

        public DesignOrder(String id, String client, String project, String status, String brief) {
            this.id = id;
            this.client = client;
            this.project = project;
            this.status = status;
            this.brief = brief;
        }
    }
}