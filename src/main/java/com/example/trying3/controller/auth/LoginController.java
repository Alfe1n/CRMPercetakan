package com.example.trying3.controller.auth;

import com.example.trying3.MainApplication;
import com.example.trying3.model.User;
import com.example.trying3.service.AuthService;
import com.example.trying3.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;

/**
 * Controller untuk halaman login aplikasi CRM Percetakan.
 * Menangani autentikasi pengguna dan redirect ke dashboard sesuai role.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    @FXML
    private void handleLoginButton(ActionEvent e) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Username tidak boleh kosong!");
            txtUsername.requestFocus();
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            AlertUtil.showWarning("Peringatan", "Password tidak boleh kosong!");
            txtPassword.requestFocus();
            return;
        }

        User user = authService.login(username, password);

        if (user == null) {
            handleFailedLogin();
            return;
        }

        System.out.println("✅ Login berhasil! User: " + user.getNamaLengkap());
        System.out.println("   Role ID: " + user.getIdRole());
        System.out.println("   Is Active: " + user.isActive());

        redirectToDashboard(e, user);
    }

    /**
     * Menangani kasus login gagal dengan menampilkan pesan error yang sesuai.
     */
    private void handleFailedLogin() {
        User failedUser = authService.getCurrentUser();

        if (failedUser != null) {
            if (!failedUser.isActive()) {
                AlertUtil.showAccountInactive();
            } else if (failedUser.isLocked()) {
                Duration duration = Duration.between(
                        java.time.LocalDateTime.now(),
                        failedUser.getLockedUntil()
                );
                int minutes = (int) duration.toMinutes();
                AlertUtil.showAccountLocked(minutes);
            }
        } else {
            AlertUtil.showLoginError();
        }

        txtPassword.clear();
        txtUsername.requestFocus();
    }

    /**
     * Redirect user ke dashboard yang sesuai dengan role-nya.
     *
     * @param e Event dari button login
     * @param user User yang berhasil login
     */
    private void redirectToDashboard(ActionEvent e, User user){
        try {
            String fxmlPath = switch (user.getIdRole()) {
                case 1 -> "fxml/admin/Dashboard.fxml";
                case 2 -> "fxml/designer/DashboardDesign.fxml";
                case 3 -> "fxml/production/DashboardProduction.fxml";
                case 4 -> "fxml/manajemen/DashboardManagement.fxml";
                default -> "fxml/admin/Dashboard.fxml";
            };

            FXMLLoader fxmlLoader = new FXMLLoader(
                    MainApplication.class.getResource(fxmlPath)
            );
            Scene dashboardScene = new Scene(fxmlLoader.load(), 1920, 1080);

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(dashboardScene);
            stage.setTitle("CRM Percetakan - Dashboard");
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ Dashboard loaded successfully!");
        } catch (IOException ex) {
            System.err.println("❌ Error loading dashboard!");
            ex.printStackTrace();
            AlertUtil.showError("Error", "Gagal memuat dashboard: " + ex.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        AlertUtil.showInfo("Forgot Password",
                "Fitur reset password belum tersedia.\n" +
                        "Silakan hubungi administrator untuk reset password.");
    }

    @FXML
    private void handleGoToSignup(javafx.scene.input.MouseEvent event) {
        try {
            System.out.println("🔄 Navigasi ke halaman signup...");
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("fxml/auth/Signup.fxml")
            );
            Scene signupScene = new Scene(loader.load(), 1920, 1080);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(signupScene);
            stage.setTitle("CRM Percetakan - Buat Akun Testing");
            stage.show();
        } catch (Exception e) {
            System.err.println("❌ Error navigasi ke signup: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Error", "Gagal membuka halaman signup: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        System.out.println("LoginController initialized");

        if (txtUsername != null) {
            txtUsername.requestFocus();
        }

        if (txtPassword != null) {
            txtPassword.setOnAction(this::handleLoginButton);
        }
    }
}
