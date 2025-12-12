package com.example.trying3.controller.design;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TemplateDesainPaneController implements Initializable {

    @FXML private FlowPane templateContainer;

    private List<DesignTemplate> templates = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDummyData();
        renderTemplates();
    }

    private void loadDummyData() {
        // Data sesuai gambar referensi
        templates.add(new DesignTemplate("Brosur A4", "Template siap pakai"));
        templates.add(new DesignTemplate("Kartu Nama", "Template siap pakai"));
        templates.add(new DesignTemplate("Banner", "Template siap pakai"));
        templates.add(new DesignTemplate("Undangan", "Template siap pakai"));
        templates.add(new DesignTemplate("Poster", "Template siap pakai"));
        templates.add(new DesignTemplate("Flyer", "Template siap pakai"));
    }

    private void renderTemplates() {
        templateContainer.getChildren().clear();

        for (DesignTemplate item : templates) {
            templateContainer.getChildren().add(createTemplateCard(item));
        }
    }

    private VBox createTemplateCard(DesignTemplate item) {
        // 1. Container Kartu (Border tipis, rounded)
        VBox card = new VBox();
        card.setPrefWidth(280); // Lebar fix agar rapi
        card.setSpacing(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #eeeeee; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.02), 5, 0, 0, 1);");

        // 2. Placeholder Gambar (Kotak Abu-abu dengan Icon di tengah)
        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setPrefHeight(160);
        imagePlaceholder.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 6;");

        // Icon Palet (SVG Sederhana)
        SVGPath icon = new SVGPath();
        icon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1.41 16.09V20h-2.67v-1.93c-1.71-.36-3.16-1.54-3.92-3.11l1.73-.72c.43.9 1.25 1.57 2.19 1.77V14h2.67v2.1c1.71.36 3.16 1.54 3.92 3.11l-1.73.72c-.43-.9-1.25-1.57-2.19-1.77z");
        // (Isi SVG path bisa diganti icon paint palette yang lebih akurat jika perlu, ini contoh icon generic)
        icon.setStyle("-fx-fill: #b0b8c4;");
        icon.setScaleX(2);
        icon.setScaleY(2);

        imagePlaceholder.getChildren().add(icon);

        // 3. Info Teks
        VBox infoBox = new VBox(2);
        Label lblTitle = new Label(item.title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #222;");

        Label lblDesc = new Label(item.description);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(lblTitle, lblDesc);

        // Gabungkan
        card.getChildren().addAll(imagePlaceholder, infoBox);

        // Efek Hover sederhana
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-border-color: #d0d0d0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-border-color: #eeeeee; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.02), 5, 0, 0, 1);"));

        return card;
    }

    // Inner Model
    public static class DesignTemplate {
        String title;
        String description;

        public DesignTemplate(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}