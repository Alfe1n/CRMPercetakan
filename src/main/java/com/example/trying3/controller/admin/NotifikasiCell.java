package com.example.trying3.controller.admin;

import com.example.trying3.model.Notifikasi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class NotifikasiCell extends ListCell<Notifikasi> {

    // Main container
    private final HBox cardContainer;

    // Left side - Icon
    private final Label iconLabel;

    // Center - Content
    private final VBox contentBox;
    private final Label judulLabel;
    private final Label pesanLabel;
    private final HBox metaBox;
    private final Label nomorPesananLabel;
    private final Label sumberLabel;

    // Right side - Time and Badge
    private final VBox rightBox;
    private final Label waktuLabel;
    private final Label tipeBadge;

    public NotifikasiCell() {
        // Initialize components
        iconLabel = new Label();
        iconLabel.getStyleClass().add("notifikasi-icon");
        iconLabel.setMinWidth(50);
        iconLabel.setMinHeight(50);
        iconLabel.setAlignment(Pos.CENTER);

        // Judul
        judulLabel = new Label();
        judulLabel.getStyleClass().add("notifikasi-judul");
        judulLabel.setWrapText(true);

        // Pesan
        pesanLabel = new Label();
        pesanLabel.getStyleClass().add("notifikasi-pesan");
        pesanLabel.setWrapText(true);
        pesanLabel.setMaxWidth(500);

        // Nomor pesanan
        nomorPesananLabel = new Label();
        nomorPesananLabel.getStyleClass().addAll("notifikasi-meta", "text-bold");

        // Sumber divisi
        sumberLabel = new Label();
        sumberLabel.getStyleClass().add("notifikasi-meta");

        // Meta box
        metaBox = new HBox(10);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.getChildren().addAll(nomorPesananLabel, createSeparator(), sumberLabel);

        // Content box
        contentBox = new VBox(5);
        contentBox.getChildren().addAll(judulLabel, pesanLabel, metaBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Waktu
        waktuLabel = new Label();
        waktuLabel.getStyleClass().add("notifikasi-waktu");

        // Tipe badge
        tipeBadge = new Label();
        tipeBadge.getStyleClass().add("notifikasi-badge");

        // Right box
        rightBox = new VBox(8);
        rightBox.setAlignment(Pos.TOP_RIGHT);
        rightBox.setMinWidth(120);
        rightBox.getChildren().addAll(waktuLabel, tipeBadge);

        // Main container
        cardContainer = new HBox(15);
        cardContainer.getStyleClass().add("notifikasi-card");
        cardContainer.setAlignment(Pos.CENTER_LEFT);
        cardContainer.setPadding(new Insets(15, 20, 15, 20));
        cardContainer.getChildren().addAll(iconLabel, contentBox, rightBox);
    }

    private Label createSeparator() {
        Label sep = new Label("•");
        sep.getStyleClass().add("notifikasi-separator");
        return sep;
    }

    @Override
    protected void updateItem(Notifikasi notifikasi, boolean empty) {
        super.updateItem(notifikasi, empty);

        if (empty || notifikasi == null) {
            setText(null);
            setGraphic(null);
        } else {
            // Set icon berdasarkan tipe
            iconLabel.setText(notifikasi.getIcon());
            iconLabel.setStyle("-fx-background-color: " + notifikasi.getBadgeColor() + "20; " +
                    "-fx-background-radius: 25;");

            // Set content
            judulLabel.setText(notifikasi.getJudul());
            pesanLabel.setText(notifikasi.getPesan());
            nomorPesananLabel.setText(notifikasi.getNomorPesanan());
            sumberLabel.setText(notifikasi.getSumberDivisi());

            // Set waktu
            waktuLabel.setText(notifikasi.getWaktuRelatif());

            // Set badge
            tipeBadge.setText(notifikasi.getTipeLabel());
            tipeBadge.setStyle("-fx-background-color: " + notifikasi.getBadgeColor() + "; " +
                    "-fx-text-fill: white;");

            // Apply different style if unread
            if (!notifikasi.isSudahDibaca()) {
                cardContainer.getStyleClass().remove("notifikasi-card-read");
                cardContainer.getStyleClass().add("notifikasi-card-unread");
            } else {
                cardContainer.getStyleClass().remove("notifikasi-card-unread");
                cardContainer.getStyleClass().add("notifikasi-card-read");
            }

            setGraphic(cardContainer);
        }
    }
}