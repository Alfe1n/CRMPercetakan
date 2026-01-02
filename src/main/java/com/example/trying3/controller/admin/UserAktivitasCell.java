package com.example.trying3.controller.admin;

import com.example.trying3.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Custom ListCell untuk menampilkan aktivitas user di dashboard
 */
public class UserAktivitasCell extends ListCell<User> {

    private final HBox container;
    private final VBox infoBox;
    private final Label roleNameLabel;
    private final Label emailLabel;
    private final Label roleBadgeLabel;

    public UserAktivitasCell() {
        container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(15);
        container.setPadding(new Insets(15, 20, 15, 20));
        container.getStyleClass().add("dashboard-user-item");

        infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        roleNameLabel = new Label();
        roleNameLabel.getStyleClass().add("dashboard-user-role-name");

        emailLabel = new Label();
        emailLabel.getStyleClass().add("dashboard-user-email");

        infoBox.getChildren().addAll(roleNameLabel, emailLabel);

        roleBadgeLabel = new Label();
        roleBadgeLabel.getStyleClass().add("dashboard-role-badge");

        container.getChildren().addAll(infoBox, roleBadgeLabel);
    }

    @Override
    protected void updateItem(User user, boolean empty) {
        super.updateItem(user, empty);

        if (empty || user == null) {
            setGraphic(null);
            setText(null);
        } else {
            String roleDisplayName = getRoleDisplayName(user.getIdRole());
            roleNameLabel.setText(roleDisplayName);

            emailLabel.setText(user.getEmail());

            String roleBadgeText = getRoleBadgeText(user.getIdRole());
            roleBadgeLabel.setText(roleBadgeText);

            roleBadgeLabel.getStyleClass().removeIf(style -> style.startsWith("role-badge-"));
            roleBadgeLabel.getStyleClass().add("dashboard-role-badge");

            String roleStyleClass = getRoleStyleClass(user.getIdRole());
            roleBadgeLabel.getStyleClass().add(roleStyleClass);

            setGraphic(container);
        }
    }

    /**
     * Konversi ID role ke nama lengkap
     */
    private String getRoleDisplayName(int idRole) {
        return switch (idRole) {
            case 1 -> "Administrator";
            case 2 -> "Tim Desain";
            case 3 -> "Manajemen";
            case 4 -> "Tim Produksi";
            default -> "User";
        };
    }

    /**
     * Konversi ID role ke teks badge singkat
     */
    private String getRoleBadgeText(int idRole) {
        return switch (idRole) {
            case 1 -> "Admin";
            case 2 -> "Desain";
            case 3 -> "Manajemen";
            case 4 -> "Produksi";
            default -> "User";
        };
    }

    /**
     * Mapping ID role ke CSS class
     */
    private String getRoleStyleClass(int idRole) {
        return switch (idRole) {
            case 1 -> "role-badge-admin";
            case 2 -> "role-badge-desain";
            case 3 -> "role-badge-manajemen";
            case 4 -> "role-badge-produksi";
            default -> "role-badge-default";
        };
    }
}