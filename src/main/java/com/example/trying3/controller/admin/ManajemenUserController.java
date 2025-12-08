package com.example.trying3.controller.admin;

import com.example.trying3.dao.UserDAO;
import com.example.trying3.model.User;
import com.example.trying3.util.PasswordUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManajemenUserController implements Initializable {

    // === Pane Navigation ===
    @FXML private VBox listUserPane;
    @FXML private VBox formTambahUserPane;
    @FXML private VBox formResetPasswordPane;
    @FXML private Button btnBackToList;
    @FXML private Button btnBatalReset;
    @FXML private Button btnSimpanUser;
    @FXML private Button btnBatalForm;
    @FXML private Button btnBackFromReset;
    @FXML private Button btnResetPassword;

    // === List User Components ===
    @FXML private ListView<User> userListView;
    @FXML private Button btnTambahUser;

    // === Form Tambah User Components ===
    @FXML private TextField txtNamaLengkap;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtKonfirmasiPassword;
    @FXML private ComboBox<String> cmbRole;

    // === Error Labels Form Tambah ===
    @FXML private Label lblNamaError;
    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblKonfirmasiPasswordError;
    @FXML private Label lblRoleError;

    // === Form Reset Password Components ===
    @FXML private Label lblResetNama;
    @FXML private Label lblResetUsername;
    @FXML private Label lblResetEmail;
    @FXML private Label lblResetRole;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;
    @FXML private Label lblNewPasswordError;
    @FXML private Label lblConfirmNewPasswordError;

    // === Data ===
    private UserDAO userDAO;
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser;

    // === Role Options ===
    private final ObservableList<String> roleOptions = FXCollections.observableArrayList(
            "Administrator", "Tim Desain", "Tim Produksi", "Manajemen"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userDAO = new UserDAO();

        // Setup ComboBoxes
        setupComboBoxes();

        // Setup ListView
        setupUserListView();

        // Load initial data
        loadUserData();

        // Show list pane by default
        showListPane();
    }

    private void setupComboBoxes() {
        if (cmbRole != null) cmbRole.setItems(roleOptions);
    }

    private void setupUserListView() {
        userListView.setItems(userList);

        // Menggunakan UserListCell dengan 4 Consumer (tambah onEdit untuk reset password)
        userListView.setCellFactory(listView -> new UserListCell(
                this::openResetPasswordForm,  // onEdit - untuk reset password
                this::handleDeleteUser,       // onDelete
                this::handleToggleStatus      // onToggleStatus
        ));

        // Placeholder jika list kosong
        userListView.setPlaceholder(new Label("Belum ada data pengguna"));
    }

    private void loadUserData() {
        userList.clear();

        try {
            userList.addAll(userDAO.getAllUsers());
            System.out.println("✅ Loaded " + userList.size() + " users");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Gagal memuat data user: " + e.getMessage());
        }
    }

    // ==================== NAVIGATION METHODS ====================

    private void showListPane() {
        listUserPane.setVisible(true);
        formTambahUserPane.setVisible(false);
        formResetPasswordPane.setVisible(false);
    }

    private void showTambahPane() {
        listUserPane.setVisible(false);
        formTambahUserPane.setVisible(true);
        formResetPasswordPane.setVisible(false);
        clearTambahForm();
    }

    private void showResetPasswordPane() {
        listUserPane.setVisible(false);
        formTambahUserPane.setVisible(false);
        formResetPasswordPane.setVisible(true);
        clearResetPasswordForm();
    }

    // ==================== EVENT HANDLERS ====================

    @FXML
    private void handleTambahUserClick() {
        showTambahPane();
    }

    @FXML
    private void handleBackToListClick() {
        showListPane();
        selectedUser = null;
    }

    @FXML
    private void handleSimpanUserClick() {
        if (validateTambahForm()) {
            try {
                User newUser = new User();
                newUser.setNamaLengkap(txtNamaLengkap.getText().trim());
                newUser.setUsername(txtUsername.getText().trim());
                newUser.setEmail(txtEmail.getText().trim());

                // Hash password menggunakan PasswordUtil
                String hashedPassword = PasswordUtil.hashPassword(txtPassword.getText());
                newUser.setPasswordHash(hashedPassword);

                newUser.setIdRole(getRoleIdFromName(cmbRole.getValue()));
                newUser.setActive(true); // Default aktif

                // Save to database
                boolean success = userDAO.insertUser(newUser);

                if (success) {
                    showSuccessAlert("User berhasil ditambahkan!");
                    loadUserData();
                    showListPane();
                } else {
                    showErrorAlert("Gagal menambahkan user. Silakan coba lagi.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Terjadi kesalahan: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBatalFormClick() {
        showListPane();
    }

    @FXML
    private void handleResetPasswordClick() {
        if (validateResetPasswordForm() && selectedUser != null) {
            try {
                // Hash password baru
                String hashedPassword = PasswordUtil.hashPassword(txtNewPassword.getText());

                // Update password di database
                boolean success = userDAO.updatePassword(selectedUser.getIdUser(), hashedPassword);

                if (success) {
                    showSuccessAlert("Password berhasil direset untuk user: " + selectedUser.getNamaLengkap());
                    loadUserData();
                    showListPane();
                } else {
                    showErrorAlert("Gagal mereset password. Silakan coba lagi.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Terjadi kesalahan: " + e.getMessage());
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Membuka form reset password untuk user yang dipilih (dipanggil dari UserListCell)
     */
    private void openResetPasswordForm(User user) {
        selectedUser = user;
        populateResetPasswordForm(user);
        showResetPasswordPane();
    }

    private void populateResetPasswordForm(User user) {
        if (lblResetNama != null) lblResetNama.setText(user.getNamaLengkap());
        if (lblResetUsername != null) lblResetUsername.setText(user.getUsername());
        if (lblResetEmail != null) lblResetEmail.setText(user.getEmail());
        if (lblResetRole != null) lblResetRole.setText(getRoleNameFromId(user.getIdRole()));
    }

    /**
     * Menghapus user (dipanggil dari UserListCell)
     */
    private void handleDeleteUser(User user) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Hapus");
        confirmAlert.setHeaderText("Hapus User");
        confirmAlert.setContentText("Apakah Anda yakin ingin menghapus user '" + user.getNamaLengkap() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Soft delete: set is_active = false
                user.setActive(false);
                boolean success = userDAO.updateUser(user);

                if (success) {
                    showSuccessAlert("User berhasil dihapus!");
                    loadUserData();
                } else {
                    showErrorAlert("Gagal menghapus user.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Terjadi kesalahan: " + e.getMessage());
            }
        }
    }

    /**
     * Toggle status aktif/nonaktif user (dipanggil dari UserListCell)
     */
    private void handleToggleStatus(User user) {
        String action = user.isActive() ? "menonaktifkan" : "mengaktifkan";

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi");
        confirmAlert.setHeaderText("Ubah Status User");
        confirmAlert.setContentText("Apakah Anda yakin ingin " + action + " user '" + user.getNamaLengkap() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                user.setActive(!user.isActive());
                boolean success = userDAO.updateUser(user);

                if (success) {
                    showSuccessAlert("Status user berhasil diubah!");
                    loadUserData();
                } else {
                    showErrorAlert("Gagal mengubah status user.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Terjadi kesalahan: " + e.getMessage());
            }
        }
    }

    private void clearTambahForm() {
        if (txtNamaLengkap != null) txtNamaLengkap.clear();
        if (txtUsername != null) txtUsername.clear();
        if (txtEmail != null) txtEmail.clear();
        if (txtPassword != null) txtPassword.clear();
        if (txtKonfirmasiPassword != null) txtKonfirmasiPassword.clear();
        if (cmbRole != null) cmbRole.getSelectionModel().clearSelection();
        clearAllErrors();
    }

    private void clearResetPasswordForm() {
        if (txtNewPassword != null) txtNewPassword.clear();
        if (txtConfirmNewPassword != null) txtConfirmNewPassword.clear();
        if (lblNewPasswordError != null) lblNewPasswordError.setVisible(false);
        if (lblConfirmNewPasswordError != null) lblConfirmNewPasswordError.setVisible(false);
    }

    private void clearAllErrors() {
        if (lblNamaError != null) lblNamaError.setVisible(false);
        if (lblUsernameError != null) lblUsernameError.setVisible(false);
        if (lblEmailError != null) lblEmailError.setVisible(false);
        if (lblPasswordError != null) lblPasswordError.setVisible(false);
        if (lblKonfirmasiPasswordError != null) lblKonfirmasiPasswordError.setVisible(false);
        if (lblRoleError != null) lblRoleError.setVisible(false);
        if (lblNewPasswordError != null) lblNewPasswordError.setVisible(false);
        if (lblConfirmNewPasswordError != null) lblConfirmNewPasswordError.setVisible(false);
    }

    private boolean validateTambahForm() {
        boolean isValid = true;
        clearAllErrors();

        // Validate Nama
        if (txtNamaLengkap.getText().trim().isEmpty()) {
            lblNamaError.setText("Nama lengkap harus diisi");
            lblNamaError.setVisible(true);
            isValid = false;
        }

        // Validate Username
        if (txtUsername.getText().trim().isEmpty()) {
            lblUsernameError.setText("Username harus diisi");
            lblUsernameError.setVisible(true);
            isValid = false;
        }

        // Validate Email
        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            lblEmailError.setText("Email harus diisi");
            lblEmailError.setVisible(true);
            isValid = false;
        } else if (!isValidEmail(email)) {
            lblEmailError.setText("Format email tidak valid");
            lblEmailError.setVisible(true);
            isValid = false;
        }

        // Validate Password
        if (txtPassword.getText().isEmpty()) {
            lblPasswordError.setText("Password harus diisi");
            lblPasswordError.setVisible(true);
            isValid = false;
        } else if (txtPassword.getText().length() < 6) {
            lblPasswordError.setText("Password minimal 6 karakter");
            lblPasswordError.setVisible(true);
            isValid = false;
        }

        // Validate Konfirmasi Password
        if (!txtPassword.getText().equals(txtKonfirmasiPassword.getText())) {
            lblKonfirmasiPasswordError.setText("Konfirmasi password tidak cocok");
            lblKonfirmasiPasswordError.setVisible(true);
            isValid = false;
        }

        // Validate Role
        if (cmbRole.getValue() == null) {
            lblRoleError.setText("Pilih role untuk user");
            lblRoleError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private boolean validateResetPasswordForm() {
        boolean isValid = true;
        clearAllErrors();

        // Validate New Password
        if (txtNewPassword.getText().isEmpty()) {
            lblNewPasswordError.setText("Password baru harus diisi");
            lblNewPasswordError.setVisible(true);
            isValid = false;
        } else if (txtNewPassword.getText().length() < 6) {
            lblNewPasswordError.setText("Password minimal 6 karakter");
            lblNewPasswordError.setVisible(true);
            isValid = false;
        }

        // Validate Confirm Password
        if (!txtNewPassword.getText().equals(txtConfirmNewPassword.getText())) {
            lblConfirmNewPasswordError.setText("Konfirmasi password tidak cocok");
            lblConfirmNewPasswordError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    private int getRoleIdFromName(String roleName) {
        return switch (roleName) {
            case "Administrator" -> 1;
            case "Tim Desain" -> 2;
            case "Tim Produksi" -> 3;
            case "Manajemen" -> 4;
            default -> 0;
        };
    }

    private String getRoleNameFromId(int roleId) {
        return switch (roleId) {
            case 1 -> "Administrator";
            case 2 -> "Tim Desain";
            case 3 -> "Tim Produksi";
            case 4 -> "Manajemen";
            default -> "Unknown";
        };
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sukses");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}