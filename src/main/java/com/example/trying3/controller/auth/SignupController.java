package com.example.trying3.controller.auth;

import com.example.trying3.MainApplication;
import com.example.trying3.model.User;
import com.example.trying3.service.UserService;
import com.example.trying3.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller untuk halaman registrasi user baru (untuk testing).
 * Menangani pembuatan akun user dengan berbagai role.
 */
public class SignupController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtNamaLengkap;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cmbRole;

    private final UserService userService;

    public SignupController() {
        this.userService = new UserService();
    }

    @FXML
    public void initialize() {
        System.out.println("SignupController initialized");

        cmbRole.setItems(FXCollections.observableArrayList(
                "Administrator",
                "Designer",
                "Operator Produksi",
                "Manager"
        ));
        cmbRole.getSelectionModel().select(0);
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        System.out.println("🔄 Memproses signup...");

        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String namaLengkap = txtNamaLengkap.getText();
        String email = txtEmail.getText();
        String roleStr = cmbRole.getValue();

        if (!validateInput(username, password, namaLengkap, email, roleStr)) {
            return;
        }

        int roleId = getRoleId(roleStr);

        System.out.println("📝 Data user:");
        System.out.println("   Username: " + username);
        System.out.println("   Password Length: " + password.length());
        System.out.println("   Nama: " + namaLengkap);
        System.out.println("   Email: " + email);
        System.out.println("   Role: " + roleStr + " (ID: " + roleId + ")");

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPasswordHash(password);
        newUser.setNamaLengkap(namaLengkap.trim());
        newUser.setEmail(email.trim());
        newUser.setIdRole(roleId);
        newUser.setActive(true);

        try {
            System.out.println("💾 Menyimpan user ke database...");
            User createdUser = userService.createUser(newUser);

            if (createdUser != null) {
                System.out.println("✅ User berhasil dibuat!");

                AlertUtil.showSuccess(
                        "Berhasil!",
                        "Akun berhasil dibuat!\n\n" +
                                "Username: " + username + "\n" +
                                "Password: " + password + "\n" +
                                "Role: " + roleStr + "\n\n" +
                                "Silakan login dengan kredensial tersebut."
                );

                handleBackToLogin(event);
            } else {
                System.err.println("❌ Gagal membuat user (return null)");
                AlertUtil.showError("Error", "Gagal membuat akun. Silakan coba lagi.");
            }
        } catch (Exception e) {
            System.err.println("❌ Exception saat membuat akun: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                AlertUtil.showError("Error", "Username atau email sudah terdaftar!");
            } else {
                AlertUtil.showError("Error", "Gagal membuat akun: " + errorMsg);
            }
        }
    }

    /**
     * Validasi input form registrasi.
     *
     * @return true jika semua input valid, false jika ada yang tidak valid
     */
    private boolean validateInput(String username, String password, String namaLengkap,
                                   String email, String roleStr) {
        if (username == null || username.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Username tidak boleh kosong!");
            txtUsername.requestFocus();
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Password tidak boleh kosong!");
            txtPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            AlertUtil.showWarning("Peringatan", "Password minimal 6 karakter!");
            txtPassword.requestFocus();
            return false;
        }

        if (namaLengkap == null || namaLengkap.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Nama lengkap tidak boleh kosong!");
            txtNamaLengkap.requestFocus();
            return false;
        }

        if (email == null || email.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Email tidak boleh kosong!");
            txtEmail.requestFocus();
            return false;
        }

        if (roleStr == null) {
            AlertUtil.showWarning("Peringatan", "Pilih role terlebih dahulu!");
            return false;
        }

        return true;
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            System.out.println("🔙 Kembali ke login...");
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("fxml/auth/Login.fxml")
            );
            Scene loginScene = new Scene(loader.load(), 1920, 1080);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("CRM Percetakan - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Gagal kembali ke halaman login");
        }
    }

    private int getRoleId(String roleName) {
        return switch (roleName) {
            case "Administrator" -> 1;
            case "Designer" -> 2;
            case "Operator Produksi" -> 3;
            case "Manager" -> 4;
            default -> 1;
        };
    }
}