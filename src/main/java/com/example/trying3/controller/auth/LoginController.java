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

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    @FXML
    private void handleLoginButton(ActionEvent e) {
        // Get input
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Validasi input kosong
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

        // Proses login
        User user = authService.login(username, password);

        if (user == null) {
            // Login gagal - cek alasan
            User failedUser = authService.getCurrentUser();

            // Tampilkan pesan error yang sesuai
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

            // Clear password field
            txtPassword.clear();
            txtUsername.requestFocus();
            return;
        }

        // Login berhasil
        System.out.println("✅ Login berhasil! User: " + user.getNamaLengkap());
        System.out.println("   Role ID: " + user.getIdRole());
        System.out.println("   Is Active: " + user.isActive());

        // Redirect ke dashboard sesuai role
        redirectToDashboard(e, user);
    }

    private void redirectToDashboard(ActionEvent e, User user){
        try {
            String fxmlPath;

            // Tentukan dashboard berdasarkan role
            switch (user.getIdRole()) {
                case 1: // Admin
                    fxmlPath = "fxml/admin/Dashboard.fxml";
                    break;
                case 2: // Designer
                    fxmlPath = "fxml/designer/DashboardDesignPane.fxml"; // Belum ada, pakai admin dulu
                    break;
                case 3: // Production
                    fxmlPath = "fxml/production/DashboardProduction.fxml"; // Belum ada, pakai admin dulu
                    break;
                case 4: // Manager
                    fxmlPath = "fxml/manajemen/DashboardManagement.fxml"; // Belum ada, pakai admin dulu
                    break;
                default:
                    fxmlPath = "fxml/admin/Dashboard.fxml";
            }

            // Load dashboard scene
            FXMLLoader fxmlLoader = new FXMLLoader(
                    MainApplication.class.getResource(fxmlPath)
            );
            Scene dashboardScene = new Scene(fxmlLoader.load(), 1920, 1080);

            // Dapatkan stage saat ini
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

            // Ganti scene ke dashboard
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

        // Set focus ke username field
        if (txtUsername != null) {
            txtUsername.requestFocus();
        }

        // Enter key pada password field = login
        if (txtPassword != null) {
            txtPassword.setOnAction(this::handleLoginButton);
        }
    }
}
