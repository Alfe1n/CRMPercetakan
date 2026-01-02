package com.example.trying3.controller.admin;

import com.example.trying3.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Custom ListCell untuk menampilkan user dalam bentuk kartu
 * Mendukung edit (reset password), toggle status, dan hapus
 */
public class UserListCell extends ListCell<User> {

    private final Consumer<User> onEdit;
    private final Consumer<User> onDelete;
    private final Consumer<User> onToggleStatus;

    private final HBox container;
    private final VBox infoBox;
    private final Label nameLabel;
    private final Label emailLabel;
    private final Label dateLabel;
    private final Label statusBadge;
    private final Label roleBadge;
    private final Button btnEdit;
    private final Button btnToggleStatus;
    private final Button btnHapus;

    public UserListCell(Consumer<User> onEdit,
                        Consumer<User> onDelete,
                        Consumer<User> onToggleStatus) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onToggleStatus = onToggleStatus;

        container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(15);
        container.setPadding(new Insets(15, 20, 15, 20));
        container.getStyleClass().add("user-list-item");

        infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        nameLabel = new Label();
        nameLabel.getStyleClass().add("user-name");

        emailLabel = new Label();
        emailLabel.getStyleClass().add("user-email");

        dateLabel = new Label();
        dateLabel.getStyleClass().add("user-date");

        infoBox.getChildren().addAll(nameLabel, emailLabel, dateLabel);

        statusBadge = new Label();
        statusBadge.getStyleClass().add("status-badge");

        roleBadge = new Label();
        roleBadge.getStyleClass().add("role-badge");

        btnEdit = new Button("✏ Edit");
        btnEdit.getStyleClass().addAll("button", "button-primary", "button-small");

        btnToggleStatus = new Button("Nonaktifkan");
        btnToggleStatus.getStyleClass().addAll("button", "button-secondary", "button-small");

        btnHapus = new Button("🗑");
        btnHapus.getStyleClass().addAll("button", "button-icon-danger");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.getChildren().addAll(statusBadge, roleBadge, btnEdit, btnToggleStatus, btnHapus);

        container.getChildren().addAll(infoBox, actionBox);
    }

    @Override
    protected void updateItem(User user, boolean empty) {
        super.updateItem(user, empty);

        if (empty || user == null) {
            setGraphic(null);
            return;
        }

        nameLabel.setText(user.getNamaLengkap());
        emailLabel.setText(user.getEmail());

        if (user.getCreatedAt() != null) {
            dateLabel.setText("Dibuat: " + user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            dateLabel.setText("Dibuat: -");
        }

        boolean isActive = user.isActive();
        statusBadge.setText(isActive ? "Aktif" : "Nonaktif");
        statusBadge.getStyleClass().removeAll("status-aktif", "status-nonaktif");
        statusBadge.getStyleClass().add(isActive ? "status-aktif" : "status-nonaktif");

        btnToggleStatus.setText(isActive ? "Nonaktifkan" : "Aktifkan");

        String roleName = getRoleNameFromId(user.getIdRole());
        roleBadge.setText(roleName);
        roleBadge.getStyleClass().removeAll("role-admin", "role-desain", "role-produksi", "role-manajemen");

        switch (user.getIdRole()) {
            case 1 -> roleBadge.getStyleClass().add("role-admin");
            case 2 -> roleBadge.getStyleClass().add("role-desain");
            case 3 -> roleBadge.getStyleClass().add("role-produksi");
            case 4 -> roleBadge.getStyleClass().add("role-manajemen");
        }

        btnEdit.setOnAction(e -> {
            if (onEdit != null) {
                onEdit.accept(user);
            }
        });

        btnToggleStatus.setOnAction(e -> {
            if (onToggleStatus != null) {
                onToggleStatus.accept(user);
            }
        });

        btnHapus.setOnAction(e -> {
            if (onDelete != null) {
                onDelete.accept(user);
            }
        });

        setGraphic(container);
    }

    /**
     * Konversi ID role ke nama role
     */
    private String getRoleNameFromId(int roleId) {
        return switch (roleId) {
            case 1 -> "Administrator";
            case 2 -> "Tim Desain";
            case 3 -> "Tim Produksi";
            case 4 -> "Manajemen";
            default -> "Unknown";
        };
    }
}